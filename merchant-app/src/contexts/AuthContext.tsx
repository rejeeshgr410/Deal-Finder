import React, { createContext, useContext, useState, useEffect } from 'react';
import { auth, db } from '../firebase';
import { GoogleAuthProvider, signInWithCredential, signInWithPopup, signOut, onAuthStateChanged, User, signInWithEmailAndPassword } from 'firebase/auth';
import { doc, getDoc, setDoc } from 'firebase/firestore';
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
        // Fetch user profile from Firestore
        const userRef = doc(db, 'users', firebaseUser.uid);
        const userSnap = await getDoc(userRef);
        
        let role: Role = 'pending'; // Default to pending if new
        let retailerId: string | null = null;
        
        if (firebaseUser.email === '***REDACTED_EMAIL***') {
          role = 'super_admin';
        }

        // Get role from custom claims or firestore document
        if (userSnap.exists()) {
          const data = userSnap.data();
          role = data.role || role;
          retailerId = data.retailerId || null;
          
          if (firebaseUser.email === '***REDACTED_EMAIL***' && role !== 'super_admin') {
             role = 'super_admin';
             await setDoc(userRef, { role: 'super_admin' }, { merge: true });
          }
        } else {
          // Auto-provision basic profile
          await setDoc(userRef, {
            email: firebaseUser.email,
            role: role,
            createdAt: Date.now()
          });
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
