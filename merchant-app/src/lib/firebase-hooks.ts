import { useState, useEffect } from 'react';
import {
  collection,
  query,
  onSnapshot,
  addDoc,
  updateDoc,
  deleteDoc,
  doc,
  QueryConstraint
} from 'firebase/firestore';
import { db, auth } from '../firebase';

const logAction = async (collectionName: string, action: string, details: any, documentId?: string) => {
  if (collectionName === 'auditLogs') return;
  const currentUser = auth.currentUser;
  if (!currentUser) return;
  try {
    await addDoc(collection(db, 'auditLogs'), {
      collection: collectionName,
      action,
      documentId: documentId || 'Unknown',
      details: JSON.stringify(details).substring(0, 500),
      user: currentUser.email || currentUser.uid,
      actorUid: currentUser.uid,
      timestamp: Date.now()
    });
  } catch (error) {
    console.error('Audit log failed', error);
  }
};

export interface UseCollectionOptions {
  constraints?: QueryConstraint[];
  enabled?: boolean;
  filterKey?: string;
}

export function useCollection<T>(collectionName: string, options: UseCollectionOptions = {}) {
  const [data, setData] = useState<T[]>([]);
  const [loading, setLoading] = useState(true);
  const enabled = options.enabled ?? true;
  const filterKey = options.filterKey ?? '';

  useEffect(() => {
    if (!enabled) {
      setData([]);
      setLoading(false);
      return;
    }
    const constraints = options.constraints ?? [];
    const q = constraints.length > 0
      ? query(collection(db, collectionName), ...constraints)
      : query(collection(db, collectionName));

    const unsubscribe = onSnapshot(
      q,
      (snapshot) => {
        setData(snapshot.docs.map((d) => ({ id: d.id, ...d.data() } as T)));
        setLoading(false);
      },
      (error) => {
        console.error('Error fetching collection:', error);
        setLoading(false);
      }
    );

    return () => unsubscribe();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [collectionName, enabled, filterKey]);

  const add = async (item: any) => {
    const now = Date.now();
    const docRef = await addDoc(collection(db, collectionName), {
      ...item,
      createdAt: now,
      updatedAt: now
    });
    await logAction(collectionName, 'CREATE', item, docRef.id);
    return docRef;
  };

  const update = async (id: string, item: any) => {
    const docRef = doc(db, collectionName, id);
    await updateDoc(docRef, {
      ...item,
      updatedAt: Date.now()
    });

    let actionLabel = 'UPDATE';
    if (item.status === 'approved' || item.status === 'published') actionLabel = 'APPROVE / PUBLISH';
    if (item.status === 'rejected') actionLabel = 'REJECT';
    if (item.status === 'suspended') actionLabel = 'SUSPEND';

    await logAction(collectionName, actionLabel, item, id);
  };

  const remove = async (id: string) => {
    await deleteDoc(doc(db, collectionName, id));
    await logAction(collectionName, 'DELETE', { ref: id }, id);
  };

  return { data, loading, add, update, remove };
}
