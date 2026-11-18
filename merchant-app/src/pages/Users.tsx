import React, { useState, useMemo } from 'react';
import { useCollection } from '../lib/firebase-hooks';
import { Modal } from '../components/Modal';
import { Users as UsersIcon, Search, MoreVertical, Edit2, ShieldAlert, Trash2, ShieldCheck, Mail, Activity, Ban, ChevronLeft, ChevronRight } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../contexts/ToastContext';
import { httpsCallable } from 'firebase/functions';
import { functions } from '../firebase';

export default function Users() {
  const { data: users, remove, loading } = useCollection<any>('users');
  const { data: retailers } = useCollection<any>('retailers');
  const [searchQuery, setSearchQuery] = useState('');
  const { user: currentUser } = useAuth();
  const { showToast } = useToast();
  
  const [editingUser, setEditingUser] = useState<any>(null);
  const [formData, setFormData] = useState({
    role: '',
    status: 'active',
    retailerId: ''
  });

  const [currentPage, setCurrentPage] = useState(1);
  const ITEMS_PER_PAGE = 20;

  const filteredUsers = useMemo(() => {
    return users.filter((u: any) => {
      const searchTerms = [u.email, u.role].filter(Boolean).map(s => s.toLowerCase());
      return searchTerms.some(term => term.includes(searchQuery.toLowerCase()));
    });
  }, [users, searchQuery]);

  const paginatedUsers = useMemo(() => {
    const startIdx = (currentPage - 1) * ITEMS_PER_PAGE;
    return filteredUsers.slice(startIdx, startIdx + ITEMS_PER_PAGE);
  }, [filteredUsers, currentPage]);

  const totalPages = Math.ceil(filteredUsers.length / ITEMS_PER_PAGE);

  React.useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery]);

  const handleEditClick = (u: any) => {
    setEditingUser(u);
    setFormData({
      role: u.role || 'retailer_owner',
      status: u.status || 'active',
      retailerId: u.retailerId || ''
    });
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingUser) return;

    try {
      const assignRole = httpsCallable(functions, 'assignUserRole');
      await assignRole({
        targetUid: editingUser.id,
        role: formData.role,
        retailerId: formData.retailerId || null,
        isSuspended: formData.status === 'suspended'
      });
      showToast('User role and claims updated via trusted backend', 'success');
    } catch (err: any) {
      showToast(err?.message || 'Error updating user role via backend', 'error');
    }
    setEditingUser(null);
  };

  const handleDelete = async (userId: string) => {
    if (window.confirm('Are you sure you want to delete this user? They will lose access to the system.')) {
      try {
        await remove(userId);
        showToast('User deleted', 'success');
      } catch (err) {
        showToast('Error deleting user', 'error');
      }
    }
  };

  const getRetailerName = (id: string) => {
    const r = retailers.find((ret: any) => ret.id === id);
    return r ? r.name : 'Unknown Retailer';
  };

  return (
    <div className="flex-1 flex flex-col h-full max-w-6xl mx-auto w-full">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <UsersIcon className="w-6 h-6 text-indigo-600" /> User Management
          </h1>
          <p className="text-slate-500 mt-1 text-sm">Manage system roles, permissions, and account access.</p>
        </div>
        <div className="relative w-full sm:w-64">
           <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 w-4 h-4" />
           <input 
             type="text" 
             value={searchQuery}
             onChange={(e) => setSearchQuery(e.target.value)}
             placeholder="Search users..." 
             className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-xl bg-white focus:ring-2 focus:ring-indigo-500 text-sm outline-none shadow-sm"
           />
        </div>
      </div>

      {loading ? (
         <div className="flex-1 flex items-center justify-center h-64">
           <div className="text-slate-500 font-medium animate-pulse">Loading users...</div>
         </div>
      ) : (
        <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden flex-1">
          <table className="w-full text-left border-collapse">
             <thead>
               <tr className="bg-slate-50/80 border-b border-slate-200">
                 <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">User</th>
                 <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Role & Access</th>
                 <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Status</th>
                 <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider text-right">Actions</th>
               </tr>
             </thead>
             <tbody className="divide-y divide-slate-100">
                {paginatedUsers.map((u: any) => (
                  <tr key={u.id} className="hover:bg-slate-50 transition-colors">
                     <td className="py-4 px-6">
                        <div className="flex items-center gap-3">
                           <div className="w-10 h-10 bg-slate-100 text-slate-600 rounded-full flex items-center justify-center font-bold text-sm shrink-0 uppercase">
                              {u.email ? u.email.substring(0, 2) : 'US'}
                           </div>
                           <div className="min-w-0">
                              <div className="font-semibold text-slate-900 truncate">{u.email}</div>
                              <div className="text-xs text-slate-500 flex items-center gap-1 mt-0.5">
                                <Activity className="w-3 h-3" />
                                {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : 'Unknown date'}
                              </div>
                           </div>
                        </div>
                     </td>
                     <td className="py-4 px-6">
                        <div className="flex flex-col items-start gap-1">
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-widest bg-indigo-50 text-indigo-700 border border-indigo-100">
                            {u.role.replace(/_/g, ' ')}
                          </span>
                          {(u.role === 'retailer_owner' || u.role === 'retailer_staff') && u.retailerId && (
                             <span className="text-xs text-slate-500 truncate mt-1">
                               @ {getRetailerName(u.retailerId)}
                             </span>
                          )}
                        </div>
                     </td>
                     <td className="py-4 px-6">
                        {u.status === 'suspended' ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wide bg-red-100 text-red-700">
                            <Ban className="w-3 h-3" /> Suspended
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wide bg-green-100 text-green-700">
                            <ShieldCheck className="w-3 h-3" /> Active
                          </span>
                        )}
                     </td>
                     <td className="py-4 px-6 text-right">
                        <div className="flex items-center justify-end gap-2">
                           <button onClick={() => handleEditClick(u)} className="p-2 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors flex items-center gap-1 text-sm font-bold">
                             <Edit2 className="w-4 h-4" /> Edit
                           </button>
                           {currentUser.uid !== u.id && (
                             <button onClick={() => handleDelete(u.id)} className="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors flex items-center gap-1 text-sm font-bold">
                               <Trash2 className="w-4 h-4" /> Delete
                             </button>
                           )}
                        </div>
                     </td>
                  </tr>
                ))}
                {filteredUsers.length === 0 && (
                  <tr>
                    <td colSpan={4} className="py-12 text-center text-slate-500">No users found matching your search.</td>
                  </tr>
                )}
             </tbody>
          </table>
        </div>
      )}

      {totalPages > 1 && !loading && (
        <div className="mt-4 px-6 py-4 border border-slate-200 bg-white rounded-2xl flex justify-between items-center shrink-0 shadow-sm">
          <span className="text-xs text-slate-500 font-medium">
            Showing {(currentPage - 1) * ITEMS_PER_PAGE + 1} - {Math.min(currentPage * ITEMS_PER_PAGE, filteredUsers.length)} of {filteredUsers.length}
          </span>
          <div className="flex items-center gap-2">
            <button 
              disabled={currentPage === 1}
              onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
              className="p-1 border border-slate-200 bg-white rounded-lg text-slate-600 hover:bg-slate-50 disabled:opacity-50 transition-colors"
              title="Previous Page"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            <div className="text-xs font-bold text-slate-700 px-2">{currentPage} / {totalPages}</div>
            <button 
              disabled={currentPage === totalPages}
              onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
              className="p-1 border border-slate-200 bg-white rounded-lg text-slate-600 hover:bg-slate-50 disabled:opacity-50 transition-colors"
              title="Next Page"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          </div>
        </div>
      )}

      <Modal isOpen={!!editingUser} onClose={() => setEditingUser(null)} title="Edit User">
        <form onSubmit={handleUpdate} className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-slate-900 mb-1">User Email</label>
            <input type="text" disabled value={editingUser?.email || ''} className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-100 text-slate-500 cursor-not-allowed" />
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-900 mb-1">Role</label>
            <select 
              value={formData.role}
              onChange={(e) => setFormData({...formData, role: e.target.value})}
              className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
            >
              <option value="super_admin">Super Admin</option>
              <option value="admin">Admin</option>
              <option value="moderator">Moderator</option>
              <option value="data_entry">Data Entry</option>
              <option value="retailer_owner">Retailer Owner</option>
              <option value="retailer_staff">Retailer Staff</option>
              <option value="pending">Pending</option>
              <option value="unauthorized">Unauthorized</option>
            </select>
          </div>
          {(formData.role === 'retailer_owner' || formData.role === 'retailer_staff') && (
            <div>
              <label className="block text-sm font-semibold text-slate-900 mb-1">Assign to Retailer</label>
              <select 
                value={formData.retailerId || ''}
                onChange={(e) => setFormData({...formData, retailerId: e.target.value})}
                className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
              >
                <option value="" disabled>Select Retailer</option>
                {retailers.map((r: any) => (
                  <option key={r.id} value={r.id}>{r.name}</option>
                ))}
              </select>
            </div>
          )}
          <div>
            <label className="block text-sm font-semibold text-slate-900 mb-1">Account Status</label>
            <select 
              value={formData.status}
              onChange={(e) => setFormData({...formData, status: e.target.value})}
              className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
            >
              <option value="active">Active</option>
              <option value="suspended">Suspended</option>
            </select>
          </div>
          <div className="pt-4 flex items-center justify-end gap-3">
             <button type="button" onClick={() => setEditingUser(null)} className="px-4 py-2 text-sm font-bold text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">Cancel</button>
             <button type="submit" className="px-6 py-2 text-sm font-bold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors">Update User</button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
