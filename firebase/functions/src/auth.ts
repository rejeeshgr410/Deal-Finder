import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { z } from 'zod';
import { getAdminAuth, getNamedDb } from './config';

export const ALL_ROLES = [
  'super_admin',
  'admin',
  'moderator',
  'data_entry',
  'retailer_owner',
  'retailer_staff',
  'pending',
  'consumer',
  'user',
  'unauthorized'
] as const;

export type AppRole = (typeof ALL_ROLES)[number];

export const AssignRoleInputSchema = z.object({
  targetUid: z.string().min(1).max(128),
  role: z.enum(ALL_ROLES),
  retailerId: z.string().min(1).max(128).nullable().optional(),
  isSuspended: z.boolean().optional()
});

export function canCallerAssignRole(
  callerRole: string | undefined,
  existingTargetRole: string | undefined,
  requestedRole: AppRole
): { allowed: boolean; reason?: string } {
  if (callerRole !== 'super_admin' && callerRole !== 'admin') {
    return { allowed: false, reason: 'Only admin or super_admin may assign roles.' };
  }
  if (callerRole === 'admin') {
    if (requestedRole === 'super_admin') {
      return { allowed: false, reason: 'Only super_admin may grant super_admin.' };
    }
    if (existingTargetRole === 'super_admin') {
      return { allowed: false, reason: 'Admins cannot modify a super_admin account.' };
    }
  }
  return { allowed: true };
}

export function requiresRetailerId(role: AppRole): boolean {
  return role === 'retailer_owner' || role === 'retailer_staff';
}

async function resolveCallerRole(uid: string, tokenRole: unknown): Promise<{ role: string; isSuspended: boolean }> {
  const db = getNamedDb();
  const snap = await db.collection('users').doc(uid).get();
  const data = snap.exists ? snap.data() : undefined;
  const isSuspended = data?.isSuspended === true;
  const role =
    typeof tokenRole === 'string' && tokenRole.length > 0
      ? tokenRole
      : (typeof data?.role === 'string' ? data.role : 'consumer');
  return { role, isSuspended };
}

export const assignUserRole = onCall({ cors: true }, async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Authentication required.');
  }

  const parsed = AssignRoleInputSchema.safeParse(request.data);
  if (!parsed.success) {
    throw new HttpsError('invalid-argument', 'Invalid role assignment payload.');
  }

  const { targetUid, role, retailerId, isSuspended } = parsed.data;
  if (requiresRetailerId(role) && !retailerId) {
    throw new HttpsError('invalid-argument', 'retailerId is required for retailer roles.');
  }

  const caller = await resolveCallerRole(request.auth.uid, request.auth.token.role);
  if (caller.isSuspended) {
    throw new HttpsError('permission-denied', 'Suspended callers cannot manage roles.');
  }

  const db = getNamedDb();
  const targetRef = db.collection('users').doc(targetUid);
  const targetSnap = await targetRef.get();
  const existingRole = targetSnap.exists ? (targetSnap.data()?.role as string | undefined) : undefined;

  const check = canCallerAssignRole(caller.role, existingRole, role);
  if (!check.allowed) {
    throw new HttpsError('permission-denied', check.reason ?? 'Permission denied.');
  }

  const normalizedRetailerId = requiresRetailerId(role) ? (retailerId ?? null) : null;
  const now = Date.now();

  await targetRef.set(
    {
      role,
      retailerId: normalizedRetailerId,
      ...(typeof isSuspended === 'boolean' ? { isSuspended } : {}),
      customClaimsSyncedAt: now,
      updatedAt: now
    },
    { merge: true }
  );

  await getAdminAuth().setCustomUserClaims(targetUid, {
    role,
    retailerId: normalizedRetailerId,
    isSuspended: typeof isSuspended === 'boolean' ? isSuspended : (targetSnap.data()?.isSuspended === true)
  });

  await db.collection('auditLogs').add({
    collection: 'users',
    action: 'ASSIGN_ROLE',
    documentId: targetUid,
    details: JSON.stringify({
      previousRole: existingRole ?? null,
      newRole: role,
      retailerId: normalizedRetailerId,
      isSuspended: typeof isSuspended === 'boolean' ? isSuspended : undefined
    }),
    user: request.auth.token.email ?? request.auth.uid,
    actorUid: request.auth.uid,
    timestamp: now
  });

  return {
    ok: true,
    targetUid,
    role,
    retailerId: normalizedRetailerId
  };
});

export const syncMyClaims = onCall({ cors: true }, async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Authentication required.');
  }

  const uid = request.auth.uid;
  const db = getNamedDb();
  const userRef = db.collection('users').doc(uid);
  const snap = await userRef.get();
  const now = Date.now();

  let role: string = 'pending';
  let retailerId: string | null = null;
  let isSuspended = false;

  if (snap.exists) {
    const data = snap.data() ?? {};
    role = typeof data.role === 'string' && data.role.length > 0 ? data.role : 'consumer';
    retailerId = typeof data.retailerId === 'string' && data.retailerId.length > 0 ? data.retailerId : null;
    isSuspended = data.isSuspended === true;
  } else {
    // Safe default provisioning; never assigns admin/super_admin from email or client payload.
    const defaultRole = request.data?.clientType === 'merchant_portal' ? 'pending' : 'consumer';
    role = defaultRole;
    await userRef.set({
      id: uid,
      email: request.auth.token.email ?? '',
      role: defaultRole,
      retailerId: null,
      isSuspended: false,
      createdAt: now,
      updatedAt: now,
      customClaimsSyncedAt: now
    });
  }

  await getAdminAuth().setCustomUserClaims(uid, {
    role,
    retailerId,
    isSuspended
  });

  return {
    role,
    retailerId,
    isSuspended
  };
});
