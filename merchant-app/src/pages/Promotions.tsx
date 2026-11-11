import React, { useState, useMemo } from 'react';
import { Tag, Search, Plus, Edit2, Trash2, Ban, Clock, ChevronLeft, ChevronRight, Bookmark } from 'lucide-react';
import { useCollection } from '../lib/firebase-hooks';
import { Modal } from '../components/Modal';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../contexts/ToastContext';
import { trackEvent } from '../lib/analytics';

export default function Promotions() {
  const { user, isRetailerUser, isAdminUser } = useAuth();
  const { showToast } = useToast();
  const { data: promotions, loading: promosLoading, add: addPromo, update: updatePromo, remove: removePromo } = useCollection<any>('promotions');
  const { data: coupons, loading: couponsLoading, add: addCoupon, update: updateCoupon, remove: removeCoupon } = useCollection<any>('coupons');
  const loading = promosLoading || couponsLoading;

  const [searchQuery, setSearchQuery] = useState('');
  const [isAdding, setIsAdding] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [newPromo, setNewPromo] = useState({
    title: '', code: '', description: '', discountInfo: '', validFrom: '', validTo: '', type: 'coupon', status: 'published'
  });

  const [currentPage, setCurrentPage] = useState(1);
  const ITEMS_PER_PAGE = 20;

  const allOffers = useMemo(() => {
    const p = promotions.map((x: any) => ({ ...x, type: 'offer' }));
    const c = coupons.map((x: any) => ({ ...x, type: 'coupon', validTo: x.expiry, expiry: undefined }));
    return [...p, ...c].sort((a, b) => b.createdAt - a.createdAt);
  }, [promotions, coupons]);

  const filteredPromotions = useMemo(() => {
    return allOffers.filter((p: any) => 
      (isAdminUser || p.retailerId === user.retailerId) &&
      (p.title?.toLowerCase().includes(searchQuery.toLowerCase()) || 
      p.code?.toLowerCase().includes(searchQuery.toLowerCase()))
    );
  }, [allOffers, user, isAdminUser, searchQuery]);

  const paginatedPromotions = useMemo(() => {
    const startIdx = (currentPage - 1) * ITEMS_PER_PAGE;
    return filteredPromotions.slice(startIdx, startIdx + ITEMS_PER_PAGE);
  }, [filteredPromotions, currentPage]);

  const totalPages = Math.ceil(filteredPromotions.length / ITEMS_PER_PAGE);

  React.useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery]);

  const resetForm = () => {
    setNewPromo({ title: '', code: '', description: '', discountInfo: '', validFrom: '', validTo: '', type: 'coupon', status: 'published' });
    setEditingId(null);
  };

  const handleEditClick = (promo: any) => {
    setNewPromo({
      title: promo.title || '',
      code: promo.code || '',
      description: promo.description || '',
      discountInfo: promo.discountInfo || promo.discount || '',
      validFrom: promo.validFrom || '',
      validTo: promo.validTo || promo.expiry || '',
      type: promo.type || 'coupon',
      status: promo.status || 'published'
    });
    setEditingId(promo.id);
    setIsEditing(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const isCoupon = newPromo.type === 'coupon';
      const payload: any = {
        retailerId: isRetailerUser ? user.retailerId : newPromo.retailerId || '',
        title: newPromo.title,
        discountInfo: newPromo.discountInfo,
        status: newPromo.status
      };
      
      if (isCoupon) {
        payload.code = newPromo.code;
        payload.expiry = newPromo.validTo;
      } else {
        payload.description = newPromo.description;
        payload.validFrom = newPromo.validFrom;
        payload.validTo = newPromo.validTo;
      }

      if (isEditing && editingId) {
        payload.updatedAt = new Date().toISOString();
        if (isCoupon) await updateCoupon(editingId, payload);
        else await updatePromo(editingId, payload);
        showToast('Promotion updated successfully', 'success');
      } else {
        payload.usageCount = 0;
        payload.createdAt = new Date().toISOString();
        payload.updatedAt = new Date().toISOString();
        if (isCoupon) await addCoupon(payload);
        else await addPromo(payload);
        showToast('Promotion added successfully', 'success');
      }
      setIsAdding(false);
      setIsEditing(false);
      resetForm();
    } catch (e) {
      showToast('Failed to save promotion', 'error');
    }
  };

  const handleStatusChange = async (promo: any, newStatus: string) => {
    try {
      if (promo.type === 'coupon') await updateCoupon(promo.id, { status: newStatus });
      else await updatePromo(promo.id, { status: newStatus });
      showToast(`Status updated to ${newStatus}`, 'success');
    } catch (e) {
      showToast('Failed to update status', 'error');
    }
  };

  const handleDelete = async (promo: any) => {
    if (window.confirm(`Are you sure you want to delete promotion "${promo.title}"?`)) {
      try {
        if (promo.type === 'coupon') await removeCoupon(promo.id);
        else await removePromo(promo.id);
        showToast('Promotion deleted', 'success');
      } catch (e) {
        showToast('Failed to delete', 'error');
      }
    }
  };

  const handleSimulateSave = async (promo: any) => {
    try {
      const incrementVal = (promo.usageCount || 0) + 1;
      if (promo.type === 'coupon') {
        await updateCoupon(promo.id, { usageCount: incrementVal });
        await trackEvent('coupon_save', { couponId: promo.id, title: promo.title, retailerId: promo.retailerId || '' });
        showToast(`Saved coupon: ${promo.title} (metrics logged)`, 'success');
      } else {
        await updatePromo(promo.id, { usageCount: incrementVal });
        await trackEvent('promotion_click', { promoId: promo.id, title: promo.title, retailerId: promo.retailerId || '' });
        showToast(`Clicked promo: ${promo.title} (metrics logged)`, 'success');
      }
    } catch (e) {
      showToast('Simulation failed. Check Firestore context.', 'error');
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full">
      <div className="mb-6 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Promotions & Coupons</h1>
          <p className="text-slate-500 mt-1 text-sm">Manage discount codes and special offers.</p>
        </div>
        <button onClick={() => setIsAdding(true)} className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors font-semibold text-sm">
          <Plus className="w-4 h-4" />
          Create Promotion
        </button>
      </div>

      <div className="bg-white border border-slate-200 rounded-3xl shadow-sm overflow-hidden flex-1 flex flex-col">
        <div className="p-4 border-b border-slate-100">
           <div className="relative w-full max-w-md">
             <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 w-4 h-4" />
             <input 
               type="text" 
               value={searchQuery}
               onChange={(e) => setSearchQuery(e.target.value)}
               placeholder="Search promotions by title or code..." 
               className="w-full pl-9 pr-4 py-2 bg-slate-50 border-none rounded-full focus:ring-2 focus:ring-indigo-500 text-sm outline-none"
             />
           </div>
        </div>

        <div className="overflow-x-auto flex-1">
          {loading ? (
             <div className="p-12 text-center text-slate-500 font-medium animate-pulse">Loading promotions...</div>
          ) : filteredPromotions.length === 0 ? (
             <div className="p-12 text-center flex flex-col items-center justify-center h-full">
               <Tag className="w-12 h-12 text-slate-300 mb-4" />
               <h3 className="text-lg font-bold text-slate-900 mb-1">No promotions found</h3>
               <p className="text-sm text-slate-500">Create a coupon or promo code to share with users.</p>
             </div>
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/50 border-b border-slate-200">
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase">Promotion</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase">Code</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase">Discount</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase">Status</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase">Usage</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {paginatedPromotions.map((promo: any) => (
                  <tr key={promo.id} className="hover:bg-slate-50 transition-colors">
                    <td className="py-4 px-6">
                       <div className="font-semibold text-slate-900">{promo.title}</div>
                       <div className="text-[10px] text-slate-500 uppercase tracking-widest">{promo.type}</div>
                    </td>
                    <td className="py-4 px-6 font-mono text-xs font-bold text-indigo-600">
                       <span className="bg-indigo-50 px-2 py-1 rounded inline-block">{promo.code || '-'}</span>
                    </td>
                    <td className="py-4 px-6 text-sm font-bold text-green-600">{promo.discountInfo}</td>
                    <td className="py-4 px-6">
                       <span className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-widest ${
                         promo.status === 'published' ? 'bg-green-100 text-green-700' :
                         promo.status === 'disabled' ? 'bg-slate-200 text-slate-600' :
                         promo.status === 'expired' ? 'bg-orange-100 text-orange-700' : ''
                       }`}>
                         {promo.status || 'published'}
                       </span>
                    </td>
                    <td className="py-4 px-6 text-sm text-slate-600 font-semibold">{promo.usageCount || 0}</td>
                    <td className="py-4 px-6 text-right">
                       <div className="flex items-center justify-end gap-1">
                          {promo.status === 'published' && (
                            <>
                              <button onClick={() => handleStatusChange(promo, 'disabled')} className="text-slate-400 hover:text-slate-700 p-1.5 rounded-lg" title="Disable">
                                <Ban className="w-4 h-4" />
                              </button>
                              <button onClick={() => handleStatusChange(promo, 'expired')} className="text-slate-400 hover:text-orange-600 p-1.5 rounded-lg" title="Mark Expired">
                                <Clock className="w-4 h-4" />
                              </button>
                            </>
                          )}
                          {promo.status === 'published' && (
                            <button onClick={() => handleSimulateSave(promo)} className="text-slate-400 hover:text-green-600 p-1.5 hover:bg-green-50 rounded-lg transition-colors" title={promo.type === 'coupon' ? "Simulate Coupon Save" : "Simulate Promotion Click"}>
                              <Bookmark className="w-4 h-4" />
                            </button>
                          )}
                          <button onClick={() => handleEditClick(promo)} className="text-slate-400 hover:text-indigo-600 p-1.5 rounded-lg">
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button onClick={() => handleDelete(promo)} className="text-slate-400 hover:text-red-600 p-1.5 rounded-lg">
                            <Trash2 className="w-4 h-4" />
                          </button>
                       </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {totalPages > 1 && (
          <div className="px-6 py-4 border-t border-slate-200 bg-slate-50/50 flex justify-between items-center shrink-0">
            <span className="text-xs text-slate-500 font-medium">
              Showing {(currentPage - 1) * ITEMS_PER_PAGE + 1} - {Math.min(currentPage * ITEMS_PER_PAGE, filteredPromotions.length)} of {filteredPromotions.length}
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

      <Modal isOpen={isAdding || isEditing} onClose={() => { setIsAdding(false); setIsEditing(false); resetForm(); }} title={isEditing ? "Edit Promotion" : "Create Promotion"}>
        <form onSubmit={handleSave} className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-slate-900 mb-1">Title</label>
            <input type="text" required value={newPromo.title} onChange={e => setNewPromo({...newPromo, title: e.target.value})} className="w-full px-4 py-2 border border-slate-200 rounded-xl" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-slate-900 mb-1">Promo Code</label>
              <input type="text" value={newPromo.code} onChange={e => setNewPromo({...newPromo, code: e.target.value})} className="w-full px-4 py-2 border border-slate-200 rounded-xl" />
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-900 mb-1">Discount Amount</label>
              <input type="text" value={newPromo.discountInfo} onChange={e => setNewPromo({...newPromo, discountInfo: e.target.value})} placeholder="e.g. 20% OFF" className="w-full px-4 py-2 border border-slate-200 rounded-xl" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
             <div>
              <label className="block text-sm font-semibold text-slate-900 mb-1">Type</label>
              <select value={newPromo.type} onChange={e => setNewPromo({...newPromo, type: e.target.value})} className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-white">
                <option value="coupon">Coupon</option>
                <option value="offer">General Offer</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-900 mb-1">Status</label>
              <select value={newPromo.status} onChange={e => setNewPromo({...newPromo, status: e.target.value})} className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-white">
                <option value="published">Published</option>
                <option value="disabled">Disabled</option>
                <option value="expired">Expired</option>
              </select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-slate-900 mb-1">Start Date</label>
              <input type="date" value={newPromo.validFrom} onChange={e => setNewPromo({...newPromo, validFrom: e.target.value})} className="w-full px-4 py-2 border border-slate-200 rounded-xl" />
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-900 mb-1">Expiry Date</label>
              <input type="date" value={newPromo.validTo} onChange={e => setNewPromo({...newPromo, validTo: e.target.value})} className="w-full px-4 py-2 border border-slate-200 rounded-xl" />
            </div>
          </div>
          <div className="pt-4 flex items-center justify-end gap-3">
             <button type="button" onClick={() => { setIsAdding(false); setIsEditing(false); resetForm(); }} className="px-4 py-2 font-bold text-slate-600 hover:bg-slate-100 rounded-lg">Cancel</button>
             <button type="submit" className="px-6 py-2 font-bold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700">{isEditing ? "Save Changes" : "Create"}</button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
