import { useState, useEffect } from 'react';
import { collection, query, onSnapshot, addDoc, updateDoc, deleteDoc, doc } from 'firebase/firestore';
import { db, auth } from '../firebase';

const logAction = async (collectionName: string, action: string, details: any, documentId?: string) => {
  if (collectionName === 'auditLogs') return; // Prevent infinite loop
  try {
    const userEmail = auth.currentUser?.email || 'system';
    await addDoc(collection(db, 'auditLogs'), {
      collection: collectionName,
      action,
      documentId: documentId || 'Unknown',
      details: JSON.stringify(details).substring(0, 500), // truncate for safety
      user: userEmail,
      timestamp: Date.now()
    });
  } catch (error) {
    console.error("Audit log failed", error);
  }
};

export function useCollection<T>(collectionName: string) {
  const [data, setData] = useState<T[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const q = query(collection(db, collectionName));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      setData(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as T)));
      setLoading(false);
    }, (error) => {
      console.error("Error fetching collection:", error);
      setLoading(false);
    });

    return () => unsubscribe();
  }, [collectionName]);

  const add = async (item: any) => {
    try {
      const now = Date.now();
      const docRef = await addDoc(collection(db, collectionName), {
        ...item,
        createdAt: now,
        updatedAt: now
      });
      await logAction(collectionName, 'CREATE', item, docRef.id);
      return docRef;
    } catch (error) {
      console.error("Error adding document:", error);
      throw error;
    }
  };

  const update = async (id: string, item: any) => {
    try {
      const docRef = doc(db, collectionName, id);
      await updateDoc(docRef, {
        ...item,
        updatedAt: Date.now()
      });
      
      // Determine more specific action based on status changes if any
      let actionLabel = 'UPDATE';
      if (item.status === 'approved' || item.status === 'published') actionLabel = 'APPROVE / PUBLISH';
      if (item.status === 'rejected') actionLabel = 'REJECT';
      if (item.status === 'suspended') actionLabel = 'SUSPEND';
      
      await logAction(collectionName, actionLabel, item, id);
    } catch (error) {
      console.error("Error updating document:", error);
      throw error;
    }
  };

  const remove = async (id: string) => {
    try {
      await deleteDoc(doc(db, collectionName, id));
      await logAction(collectionName, 'DELETE', { ref: id }, id);
    } catch (error) {
      console.error("Error deleting document:", error);
      throw error;
    }
  };

  return { data, loading, add, update, remove };
}
