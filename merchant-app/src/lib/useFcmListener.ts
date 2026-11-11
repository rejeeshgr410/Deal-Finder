import { useEffect, useRef, useState } from 'react';
import { db, auth } from '../firebase';
import { collection, query, where, onSnapshot, doc, setDoc, updateDoc, getDocs } from 'firebase/firestore';
import { useToast } from '../contexts/ToastContext';

export interface FcmTokenRegistration {
  id: string; // Token string (e.g., 'fcm_token_usr_XYZ')
  userId: string;
  email: string | null;
  emirate: string;
  followedRetailers: string[];
  deviceType: string;
  status: 'active' | 'inactive';
  updatedAt: number;
}

export interface NotificationDelivery {
  id: string;
  notificationId: string;
  tokenId: string;
  userId: string;
  email: string | null;
  status: 'sent' | 'delivered' | 'opened' | 'failed';
  sentAt: number;
  deliveredAt: number | null;
  openedAt: number | null;
}

export function useFcmListener() {
  const { showToast } = useToast();
  const [fcmEnabled, setFcmEnabled] = useState(false);
  const [selectedEmirate, setSelectedEmirate] = useState('Dubai');
  const [followedRetailers, setFollowedRetailers] = useState<string[]>([]);
  const [tokenStr, setTokenStr] = useState<string>('');
  const [unreadDeliveries, setUnreadDeliveries] = useState<any[]>([]);

  const listenerUnsubRef = useRef<() => void>();

  // Determine current browser Token ID persistently
  useEffect(() => {
    const storedToken = localStorage.getItem('df_fcm_sim_token');
    if (storedToken) {
      setTokenStr(storedToken);
    } else {
      const generated = `fcm_token_usr_${Math.random().toString(36).substring(2, 10)}`;
      localStorage.setItem('df_fcm_sim_token', generated);
      setTokenStr(generated);
    }

    const storedEmirate = localStorage.getItem('df_fcm_emirate');
    if (storedEmirate) setSelectedEmirate(storedEmirate);

    const storedRetailers = localStorage.getItem('df_fcm_retailers');
    if (storedRetailers) {
      try {
        setFollowedRetailers(JSON.parse(storedRetailers));
      } catch (e) {
        // Fallback
      }
    }

    const storedEnabled = localStorage.getItem('df_fcm_enabled');
    if (storedEnabled === 'true') setFcmEnabled(true);
  }, []);

  // Update Profile & Register Token in Firestore
  const updateProfileAndToken = async (
    enabled: boolean,
    emirate: string,
    retailersList: string[]
  ) => {
    localStorage.setItem('df_fcm_enabled', String(enabled));
    localStorage.setItem('df_fcm_emirate', emirate);
    localStorage.setItem('df_fcm_retailers', JSON.stringify(retailersList));

    setFcmEnabled(enabled);
    setSelectedEmirate(emirate);
    setFollowedRetailers(retailersList);

    const currentUser = auth.currentUser;
    if (!currentUser || !tokenStr) return;

    const tokenDocRef = doc(db, 'fcmTokens', tokenStr);

    if (enabled) {
      const payload: FcmTokenRegistration = {
        id: tokenStr,
        userId: currentUser.uid,
        email: currentUser.email,
        emirate,
        followedRetailers: retailersList,
        deviceType: navigator.userAgent.toLowerCase().includes('mobile') ? 'Mobile Web Browser' : 'Desktop Web Browser',
        status: 'active',
        updatedAt: Date.now()
      };
      await setDoc(tokenDocRef, payload, { merge: true });
    } else {
      await setDoc(tokenDocRef, { status: 'inactive', updatedAt: Date.now() }, { merge: true });
    }
  };

  // Sync token automatically on login/load if already enabled
  useEffect(() => {
    const handleAutoSync = async () => {
      const currentUser = auth.currentUser;
      if (currentUser && tokenStr && fcmEnabled) {
        await updateProfileAndToken(true, selectedEmirate, followedRetailers);
      }
    };
    handleAutoSync();
  }, [auth.currentUser, tokenStr]);

  // Request browser Notification Permission
  const requestPermission = async () => {
    if (!('Notification' in window)) {
      showToast('Notifications not supported by this browser', 'info');
      return false;
    }
    
    try {
      const permission = await Notification.requestPermission();
      if (permission === 'granted') {
        showToast('Notification access granted!', 'success');
        return true;
      } else {
        showToast('Notification permission denied', 'error');
        return false;
      }
    } catch (e) {
      console.warn('Native notification request exception', e);
      return false;
    }
  };

  // Listen to incoming Delivery Documents
  useEffect(() => {
    const currentUser = auth.currentUser;
    if (!currentUser || !tokenStr || !fcmEnabled) {
      if (listenerUnsubRef.current) {
        listenerUnsubRef.current();
        listenerUnsubRef.current = undefined;
      }
      setUnreadDeliveries([]);
      return;
    }

    const q = query(
      collection(db, 'notificationDeliveries'),
      where('tokenId', '==', tokenStr)
    );

    const unsubscribe = onSnapshot(q, (snapshot) => {
      const list: any[] = [];
      snapshot.forEach((docSnap) => {
        const item = { id: docSnap.id, ...docSnap.data() } as any;
        list.push(item);

        // Active notification handling logic!
        if (item.status === 'sent') {
          // 1. Instantly process visual delivery alerting
          // Get original notification document details
          const fetchAndNotify = async () => {
            try {
              // Mark as delivered in DB
              const docRef = doc(db, 'notificationDeliveries', item.id);
              await updateDoc(docRef, {
                status: 'delivered',
                deliveredAt: Date.now()
              });

              // Slide-in Toast and trigger native browser notification popup
              const noteSnap = await getDocs(
                query(collection(db, 'notifications'), where('id', '==', item.notificationId))
              );
              
              let title = 'FCM Simulation Message';
              let body = 'You have a new alert!';
              if (!noteSnap.empty) {
                const noteData = noteSnap.docs[0].data();
                title = noteData.title;
                body = noteData.message;
              }

              showToast(`📢 ${title}: ${body}`, 'info');

              if ('Notification' in window && Notification.permission === 'granted') {
                new Notification(title, {
                  body: body,
                  icon: '/icon-192.png'
                });
              }
            } catch (err) {
              console.error('Error handling notification receipt:', err);
              // Set to failed if delivery updating crashed
              const docRef = doc(db, 'notificationDeliveries', item.id);
              await updateDoc(docRef, { status: 'failed' });
            }
          };
          fetchAndNotify();
        }
      });

      // Keep only active unread deliveries
      setUnreadDeliveries(list);
    });

    listenerUnsubRef.current = unsubscribe;

    return () => {
      if (unsubscribe) unsubscribe();
    };
  }, [auth.currentUser, tokenStr, fcmEnabled]);

  // Mark all deliveries for this simulator as opened/read
  const markAllAsOpened = async () => {
    for (const del of unreadDeliveries) {
      if (del.status === 'delivered') {
        const docRef = doc(db, 'notificationDeliveries', del.id);
        await updateDoc(docRef, {
          status: 'opened',
          openedAt: Date.now()
        });
      }
    }
  };

  return {
    fcmEnabled,
    selectedEmirate,
    followedRetailers,
    tokenStr,
    unreadDeliveries,
    requestPermission,
    updateProfileAndToken,
    markAllAsOpened
  };
}
