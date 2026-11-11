import React, { useState, useMemo } from 'react';
import { Store, Plus, MoreVertical, MapPin, Search, Mail, KeyRound, CheckCircle, XCircle, Ban, Users, ChevronLeft, ChevronRight } from 'lucide-react';
import { useCollection } from '../lib/firebase-hooks';
import { Modal } from '../components/Modal';
import { useToast } from '../contexts/ToastContext';
import { db } from '../firebase';
import { collection, query, where, getDocs, addDoc } from 'firebase/firestore';
import { trackEvent } from '../lib/analytics';

export default function Retailers() {
  const { data: retailers, loading, add, update, remove } = useCollection<any>('retailers');
  const { data: users } = useCollection<any>('users');
  const { add: addNotification } = useCollection<any>('notifications');
  const { showToast } = useToast();
  const [searchQuery, setSearchQuery] = useState('');
  const [isAdding, setIsAdding] = useState(false);
  const [newRetailer, setNewRetailer] = useState({ name: '', logoUrl: '', email: '', branchesCount: 0, status: 'published', emirates: [] as string[] });
  const [actionMenuId, setActionMenuId] = useState<string | null>(null);
  
  const [staffModalRetailerId, setStaffModalRetailerId] = useState<string | null>(null);

  const [currentPage, setCurrentPage] = useState(1);
  const ITEMS_PER_PAGE = 12;

  const filteredRetailers = useMemo(() => {
    return retailers.filter((r: any) => {
      return r.name?.toLowerCase().includes(searchQuery.toLowerCase()) || 
             r.email?.toLowerCase().includes(searchQuery.toLowerCase());
    });
  }, [retailers, searchQuery]);

  const paginatedRetailers = useMemo(() => {
    const startIdx = (currentPage - 1) * ITEMS_PER_PAGE;
    return filteredRetailers.slice(startIdx, startIdx + ITEMS_PER_PAGE);
  }, [filteredRetailers, currentPage]);

  const totalPages = Math.ceil(filteredRetailers.length / ITEMS_PER_PAGE);

  React.useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery]);

  const handleAddRetailer = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await add({ 
        ...newRetailer,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      });
      setIsAdding(false);
      setNewRetailer({ name: '', logoUrl: '', email: '', branchesCount: 0, status: 'published', emirates: [] });
      showToast('Retailer added successfully', 'success');
    } catch (error) {
      console.error("Failed to add retailer", error);
      showToast('Failed to add retailer.', 'error');
    }
  };

  const handleDelete = async (id: string, name: string) => {
    setActionMenuId(null);
    if (window.confirm(`Are you sure you want to delete retailer "${name}"?`)) {
       try {
         await remove(id);
         showToast('Retailer deleted', 'success');
       } catch (err) {
         showToast('Failed to delete retailer', 'error');
       }
    }
  };

  const handleUpdateStatus = async (id: string, newStatus: string, retailerName?: string) => {
    try {
      await update(id, { 
        status: newStatus,
        updatedAt: new Date().toISOString()
      });
      setActionMenuId(null);
      showToast(`Status updated to ${newStatus}`, 'success');
      
      // Send real push notification dispatch to followers!
      const noteRef = await addDoc(collection(db, 'notifications'), {
         title: `Account ${newStatus === 'published' ? 'Approved' : newStatus === 'rejected' ? 'Rejected' : 'Suspended'}`,
         message: `Your retailer account has been ${newStatus}.`,
         targetType: 'retailer_followers',
         targetRetailerId: id,
         alertType: 'retailer_update',
         status: 'sent',
         timestamp: Date.now(),
         createdAt: Date.now(),
         updatedAt: Date.now()
      });

      // Synchronize delivery logs
      const tokensSnap = await getDocs(query(collection(db, 'fcmTokens'), where('status', '==', 'active')));
      const matches: any[] = [];
      tokensSnap.forEach(tDoc => {
        const tokenVal = tDoc.data();
        if (tokenVal.followedRetailers?.includes(id)) {
          matches.push({ id: tDoc.id, ...tokenVal });
        }
      });

      if (matches.length > 0) {
        await Promise.all(matches.map(tok => {
          return addDoc(collection(db, 'notificationDeliveries'), {
            notificationId: noteRef.id,
            tokenId: tok.id,
            userId: tok.userId || 'anonymous',
            email: tok.email || null,
            status: 'sent',
            sentAt: Date.now(),
            deliveredAt: null,
            openedAt: null
          });
        }));
      }
    } catch (e) {
       console.error('Failed to send notification or update status', e);
       showToast('Error updating status', 'error');
    }
  };

  const getStatusColor = (status: string) => {
    switch(status?.toLowerCase()) {
      case 'published': return 'bg-green-100 text-green-700';
      case 'pending': return 'bg-orange-100 text-orange-700';
      case 'suspended': return 'bg-slate-200 text-slate-700';
      case 'rejected': return 'bg-red-100 text-red-700';
      default: return 'bg-green-100 text-green-700';
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full">
      <div className="mb-6 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Retailer Accounts</h1>
          <p className="text-slate-500 mt-1 text-sm">Manage partner retail accounts and branch locations.</p>
        </div>
        <div className="flex items-center gap-4 w-full sm:w-auto">
          <div className="relative flex-1 sm:w-64">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 w-4 h-4" />
            <input 
              type="text" 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search retailers..." 
              className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-xl bg-white focus:ring-2 focus:ring-indigo-500 text-sm outline-none transition-all shadow-sm"
            />
          </div>
          <button onClick={() => setIsAdding(true)} className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors font-semibold text-sm justify-center shadow-sm whitespace-nowrap">
            <Plus className="w-4 h-4" />
            Add Retailer
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex-1 flex items-center justify-center">
          <div className="text-slate-500 font-medium animate-pulse">Loading retailers...</div>
        </div>
      ) : filteredRetailers.length === 0 ? (
        <div className="flex-1 flex flex-col items-center justify-center border-2 border-dashed border-slate-200 rounded-3xl p-12 text-center bg-slate-50/50">
          <Store className="w-12 h-12 text-slate-300 mb-4" />
          <h3 className="text-lg font-bold text-slate-900 mb-1">No retailers yet</h3>
          <p className="text-sm text-slate-500">Get started by adding your first retail partner.</p>
        </div>
      ) : (
        <div className="flex-1 flex flex-col min-h-0">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 overflow-y-auto pb-4">
            {paginatedRetailers.map((retailer: any) => {
              const staffCount = users.filter((u: any) => u.retailerId === retailer.id).length;
              return (
              <div key={retailer.id} className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 hover:shadow-md transition-shadow flex flex-col">
                <div className="flex justify-between items-start">
                  <div className="flex items-center gap-4 flex-1 min-w-0">
                    <div className="w-12 h-12 bg-indigo-50 text-indigo-600 rounded-2xl flex items-center justify-center shrink-0">
                      <Store className="w-6 h-6" />
                    </div>
                    <div className="min-w-0">
                      <h3 className="font-semibold text-slate-900 truncate" title={retailer.name}>{retailer.name}</h3>
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded-md mt-1 inline-block uppercase tracking-wide cursor-default ${getStatusColor(retailer.status)}`}>
                        {retailer.status || 'published'}
                      </span>
                    </div>
                  </div>
                  <div className="relative">
                    <button 
                      onClick={() => setActionMenuId(actionMenuId === retailer.id ? null : retailer.id)} 
                      className="text-slate-400 hover:text-indigo-600 p-1 rounded-lg hover:bg-slate-50 transition-colors shrink-0"
                    >
                      <MoreVertical className="w-5 h-5" />
                    </button>
                    {actionMenuId === retailer.id && (
                      <div className="absolute top-8 right-0 bg-white border border-slate-200 shadow-lg rounded-xl flex flex-col py-1 w-40 z-10 animate-in fade-in zoom-in-95">
                        <button onClick={() => { setStaffModalRetailerId(retailer.id); setActionMenuId(null); }} className="flex items-center gap-2 px-4 py-2 text-sm text-left hover:bg-slate-50 text-slate-700">
                          <Users className="w-4 h-4 text-indigo-600" /> Manage Staff
                        </button>
                        <div className="h-px bg-slate-100 my-1"></div>
                        <button onClick={() => handleUpdateStatus(retailer.id, 'published', retailer.name)} className="flex items-center gap-2 px-4 py-2 text-sm text-left hover:bg-slate-50 text-slate-700">
                          <CheckCircle className="w-4 h-4 text-green-600" /> Approve
                        </button>
                        <button onClick={() => handleUpdateStatus(retailer.id, 'rejected', retailer.name)} className="flex items-center gap-2 px-4 py-2 text-sm text-left hover:bg-slate-50 text-slate-700">
                          <XCircle className="w-4 h-4 text-red-600" /> Reject
                        </button>
                        <button onClick={() => handleUpdateStatus(retailer.id, 'suspended', retailer.name)} className="flex items-center gap-2 px-4 py-2 text-sm text-left hover:bg-slate-50 text-slate-700">
                          <Ban className="w-4 h-4 text-slate-500" /> Suspend
                        </button>
                        <div className="h-px bg-slate-100 my-1"></div>
                        <button onClick={() => handleDelete(retailer.id, retailer.name)} className="flex items-center gap-2 px-4 py-2 text-sm text-left hover:bg-red-50 text-red-600">
                          Delete
                        </button>
                      </div>
                    )}
                  </div>
                </div>
                
                <div className="mt-5 space-y-2">
                  <div className="flex items-center gap-2 text-sm text-slate-600">
                    <Mail className="w-4 h-4 text-slate-400" />
                    <span className="truncate">{retailer.email || 'No email provided'}</span>
                  </div>
                </div>

                <div className="mt-4 grid grid-cols-2 gap-4 border-t border-slate-100 pt-4 flex-1 items-end">
                   <div>
                      <p className="text-[10px] text-slate-400 uppercase tracking-widest font-bold">Branches</p>
                      <p className="text-xl font-bold text-slate-900 mt-1 flex items-center gap-1">
                        <MapPin className="w-4 h-4 text-slate-400" />
                        {retailer.branchesCount || 0}
                      </p>
                   </div>
                   <div>
                      <p className="text-[10px] text-slate-400 uppercase tracking-widest font-bold">Staff</p>
                      <p className="text-xl font-bold text-slate-900 mt-1 flex items-center gap-1">
                        <Users className="w-4 h-4 text-slate-400" />
                        {staffCount}
                      </p>
                   </div>
                </div>
                
                {/* Simulated Visit Page View Action */}
                <button
                  onClick={async () => {
                    await trackEvent('retailer_page_view', { retailerId: retailer.id, name: retailer.name });
                    showToast(`Visited ${retailer.name} page (metrics logged)`, 'success');
                  }}
                  className="mt-4 w-full py-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold text-xs rounded-xl transition-all border border-indigo-100 shadow-sm uppercase tracking-wider"
                >
                  Simulate Page View
                </button>
              </div>
            )})}
          </div>

          {totalPages > 1 && (
            <div className="mt-auto pt-4 px-6 py-4 border-t border-slate-200 flex justify-between items-center shrink-0">
              <span className="text-xs text-slate-500 font-medium">
                Showing {(currentPage - 1) * ITEMS_PER_PAGE + 1} - {Math.min(currentPage * ITEMS_PER_PAGE, filteredRetailers.length)} of {filteredRetailers.length}
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
        </div>
      )}

      <Modal isOpen={!!staffModalRetailerId} onClose={() => setStaffModalRetailerId(null)} title="Manage Retailer Staff">
         <div className="space-y-4 max-h-[60vh] overflow-y-auto">
            <p className="text-xs text-slate-500 mb-4">Users linked to this retailer in the global User Management table.</p>
            {users.filter((u: any) => u.retailerId === staffModalRetailerId).length === 0 ? (
               <div className="p-8 text-center border border-dashed border-slate-200 rounded-xl text-slate-500">
                 No staff assigned to this retailer yet. Go to User Management to assign roles.
               </div>
            ) : (
               <div className="border border-slate-200 rounded-xl overflow-hidden divide-y divide-slate-100">
                 {users.filter((u: any) => u.retailerId === staffModalRetailerId).map((u: any) => (
                   <div key={u.id} className="p-3 bg-white flex items-center justify-between">
                      <div className="flex flex-col">
                        <span className="font-semibold text-sm text-slate-900">{u.email}</span>
                        <span className="text-xs text-slate-500 uppercase tracking-widest">{u.role}</span>
                      </div>
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wide ${u.status === 'suspended' ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>
                        {u.status || 'active'}
                      </span>
                   </div>
                 ))}
               </div>
            )}
            <div className="pt-4 flex justify-end">
               <button onClick={() => setStaffModalRetailerId(null)} className="px-6 py-2 bg-slate-900 text-white rounded-xl font-bold text-sm">Close</button>
            </div>
         </div>
      </Modal>

      <Modal isOpen={isAdding} onClose={() => setIsAdding(false)} title="Add New Retailer">
        <form onSubmit={handleAddRetailer} className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-slate-900 mb-1">Retailer Name</label>
            <input 
              type="text" 
              required
              value={newRetailer.name}
              onChange={(e) => setNewRetailer({...newRetailer, name: e.target.value})}
              className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
              placeholder="e.g. Carrefour"
            />
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-900 mb-1">Login Email</label>
            <input 
              type="email" 
              required
              value={newRetailer.email}
              onChange={(e) => setNewRetailer({...newRetailer, email: e.target.value})}
              className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
              placeholder="e.g. admin@carrefour.com"
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-slate-900 mb-1">Branches Counter</label>
              <input 
                type="number" 
                min="0"
                value={newRetailer.branchesCount}
                onChange={(e) => setNewRetailer({...newRetailer, branchesCount: parseInt(e.target.value) || 0})}
                className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-900 mb-1">Status</label>
              <select 
                value={newRetailer.status}
                onChange={(e) => setNewRetailer({...newRetailer, status: e.target.value})}
                className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
              >
                <option value="published">Published</option>
                <option value="suspended">Suspended</option>
                <option value="pending">Pending</option>
              </select>
            </div>
          </div>
          <div className="pt-4 flex items-center justify-end gap-3">
            <button 
              type="button" 
              onClick={() => setIsAdding(false)}
              className="px-4 py-2 text-sm font-bold text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
            >
              Cancel
            </button>
            <button 
              type="submit" 
              className="px-6 py-2 text-sm font-bold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors"
            >
              Save Retailer
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
