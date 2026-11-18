import React, { createContext, useContext, useState, useEffect } from 'react';
import { auth, db, functions } from '../firebase';
import { GoogleAuthProvider, signInWithCredential, signInWithPopup, signOut, onAuthStateChanged, User, signInWithEmailAndPassword } from 'firebase/auth';
import { doc, getDoc, setDoc } from 'firebase/firestore';
import { httpsCallable } from 'firebase/functions';
import { Capacitor } from '@capacitor/core';
import { SocialLogin } from '@capgo/capacitor-social-login';

const GOOGLE_WEB_CLIENT_ID = '102162589559-qu5k7vo8r5kb3pq2ijcc6p3h9q7ugsm2.apps.googleusercontent.com';

export type Role = 'super_admin' | 'admin' | 'moderator' | 'data_entry' | 'retailer_owner' | 'retailer_staff' | 'pending' | 'unauthorized' | null;

interface UserInfo {
  role: Role;
  email: string | null;
  retailerId?: string | null;
  uid?: string | null;
}

interface AuthContextType {
  user: UserInfo;
  loading: boolean;
  loginWithGoogle: () => Promise<void>;
  loginWithEmail: (email: string, pass: string) => Promise<void>;
  logout: () => Promise<void>;
  isAdminUser: boolean;
  isRetailerUser: boolean;
  isModeratorTeam: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserInfo>({ role: null, email: null });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser: User | null) => {
      if (firebaseUser) {
        let role: Role = 'pending';
        let retailerId: string | null = null;

        try {
          // Sync server-verified custom claims from trusted backend if available
          const syncClaims = httpsCallable<{ clientType: string }, { role: string; retailerId: string | null }>(
            functions,
            'syncMyClaims'
          );
          const result = await syncClaims({ clientType: 'merchant_portal' });
          await firebaseUser.getIdToken(true);
          role = (result.data?.role as Role) || 'pending';
          retailerId = result.data?.retailerId ?? null;
        } catch {
          // Fallback to server-controlled Firestore profile + existing token claims
          const tokenResult = await firebaseUser.getIdTokenResult();
          const userRef = doc(db, 'users', firebaseUser.uid);
          const userSnap = await getDoc(userRef);

          if (userSnap.exists()) {
            const data = userSnap.data();
            role = (tokenResult.claims.role as Role) || (data.role as Role) || 'pending';
            retailerId = (tokenResult.claims.retailerId as string | null) || data.retailerId || null;
          } else {
            // Safe default provisioning only; never elevate by email or client state
            role = 'pending';
            await setDoc(userRef, {
              email: firebaseUser.email ?? '',
              role: 'pending',
              isSuspended: false,
              createdAt: Date.now(),
              updatedAt: Date.now()
            });
          }
        }

        setUser({ role, email: firebaseUser.email, retailerId, uid: firebaseUser.uid });
      } else {
        setUser({ role: null, email: null });
      }
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const loginWithGoogle = async () => {
    try {
      if (Capacitor.isNativePlatform()) {
        await SocialLogin.initialize({
          google: {
            webClientId: GOOGLE_WEB_CLIENT_ID,
            mode: 'online',
          },
        });
        const res = await SocialLogin.login({
          provider: 'google',
          options: {},
        });
        if (res.result.responseType === 'online' && res.result.idToken) {
          const credential = GoogleAuthProvider.credential(res.result.idToken);
          await signInWithCredential(auth, credential);
          return;
        }
        throw new Error('Google Sign-In did not return an ID token');
      }
      const provider = new GoogleAuthProvider();
      await signInWithPopup(auth, provider);
    } catch (error) {
      console.error("Error signing in with Google", error);
      throw error;
    }
  };

  const loginWithEmail = async (email: string, pass: string) => {
    try {
      await signInWithEmailAndPassword(auth, email, pass);
    } catch (error) {
      console.error("Error signing in with email", error);
      throw error;
    }
  };

  const logout = async () => {
    try {
      await signOut(auth);
    } catch (error) {
      console.error("Error signing out", error);
    }
  };

  const isAdminUser = user.role === 'super_admin' || user.role === 'admin';
  const isRetailerUser = user.role === 'retailer_owner' || user.role === 'retailer_staff';
  const isModeratorTeam = ['super_admin', 'admin', 'moderator', 'data_entry'].includes(user.role as string);

  return (
    <AuthContext.Provider value={{ user, loading, loginWithGoogle, loginWithEmail, logout, isAdminUser, isRetailerUser, isModeratorTeam }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
