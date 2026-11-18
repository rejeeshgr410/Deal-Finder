import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useCollection } from '../lib/firebase-hooks';
import { useToast } from '../contexts/ToastContext';
import { validateBranchCoordinates } from '../lib/offerContract';
import { MapPin, Plus, Edit2, Trash2, Phone, Map, Building2, Store } from 'lucide-react';

export default function RetailerBranches() {
  const { user, isRetailerUser } = useAuth();
  const { data: branches, add, update, remove, loading } = useCollection<any>('branches');
  const { showToast } = useToast();
  
  const [isAdding, setIsAdding] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  
  const [formData, setFormData] = useState({
    name: '',
    emirate: '',
    address: '',
    phone: '',
    latitude: '',
    longitude: '',
    openingHours: ''
  });

  const myBranches = branches.filter((b: any) => b.retailerId === user.retailerId);

  const resetForm = () => {
    setFormData({ name: '', emirate: '', address: '', phone: '', latitude: '', longitude: '', openingHours: '' });
    setEditingId(null);
    setIsAdding(false);
  };

  const handleEdit = (branch: any) => {
    setFormData({
      name: branch.name || '',
      emirate: branch.emirate || '',
      address: branch.address || '',
      phone: branch.phone || '',
      latitude: branch.latitude || '',
      longitude: branch.longitude || '',
      openingHours: branch.openingHours || ''
    });
    setEditingId(branch.id);
    setIsAdding(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user.retailerId) return;

    const hasCoordsInput = String(formData.latitude).trim() !== '' || String(formData.longitude).trim() !== '';
    let coordPayload: { latitude: number | null; longitude: number | null; coordsVerified: boolean; coordSource: string } = {
      latitude: null,
      longitude: null,
      coordsVerified: false,
      coordSource: 'unverified'
    };

    if (hasCoordsInput) {
      const check = validateBranchCoordinates(formData.latitude, formData.longitude, 'merchant_entered');
      if (!check.valid) {
        showToast(check.errors[0] || 'Invalid UAE branch coordinates.', 'error');
        return;
      }
      coordPayload = {
        latitude: check.latitude,
        longitude: check.longitude,
        coordsVerified: check.coordsVerified,
        coordSource: 'merchant_entered'
      };
    }

    const payload = {
      ...formData,
      ...coordPayload
    };

    try {
      if (editingId) {
        await update(editingId, {
          ...payload,
          updatedAt: new Date().toISOString()
        });
        showToast('Branch updated successfully', 'success');
      } else {
        await add({ 
          ...payload, 
          retailerId: user.retailerId,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        });
        showToast('Branch added successfully', 'success');
      }
      resetForm();
    } catch (error) {
      console.error("Failed to save branch", error);
      showToast('Failed to save branch details.', 'error');
    }
  };

  const handleDelete = async (id: string, name: string) => {
    if (window.confirm(`Are you sure you want to delete branch "${name}"?`)) {
      try {
        await remove(id);
        showToast('Branch deleted successfully', 'success');
      } catch (err) {
        showToast('Failed to delete branch.', 'error');
      }
    }
  };

  if (!isRetailerUser) {
    return (
       <div className="flex-1 flex flex-col items-center justify-center h-full">
         <p className="text-slate-500">Only retailers can access this page.</p>
       </div>
    );
  }

  if (loading) {
    return (
       <div className="flex-1 flex flex-col items-center justify-center h-full">
         <div className="animate-pulse text-slate-500 font-medium">Loading branches...</div>
       </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col h-full max-w-4xl mx-auto w-full">
      <div className="flex justify-between items-end mb-8">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <Store className="w-6 h-6 text-indigo-600" /> My Branches
          </h1>
          <p className="text-slate-500 mt-1 text-sm">Manage the details and locations of your store branches.</p>
        </div>
        {!isAdding && (
          <button 
            onClick={() => setIsAdding(true)}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors font-bold shadow-sm"
          >
            <Plus className="w-4 h-4" /> Add Branch
          </button>
        )}
      </div>

      {isAdding && (
        <div className="bg-white border border-slate-200 rounded-3xl shadow-sm p-6 mb-8 relative">
          <h2 className="text-lg font-bold text-slate-900 mb-6">{editingId ? 'Edit Branch' : 'Add New Branch'}</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1">Branch Outlet Name/Number</label>
                <input 
                  type="text" 
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                  className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
                  placeholder="e.g. Branch #1 or Downtown Store"
                />
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1">Emirate</label>
                <select 
                  required
                  value={formData.emirate}
                  onChange={(e) => setFormData({...formData, emirate: e.target.value})}
                  className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
                >
                  <option value="" disabled>Select Emirate</option>
                  <option value="Abu Dhabi">Abu Dhabi</option>
                  <option value="Dubai">Dubai</option>
                  <option value="Sharjah">Sharjah</option>
                  <option value="Ajman">Ajman</option>
                  <option value="Umm Al Quwain">Umm Al Quwain</option>
                  <option value="Ras Al Khaimah">Ras Al Khaimah</option>
                  <option value="Fujairah">Fujairah</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1">Address / Area</label>
                <input 
                  type="text" 
                  required
                  value={formData.address}
                  onChange={(e) => setFormData({...formData, address: e.target.value})}
                  className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
                  placeholder="e.g. Downtown"
                />
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1">Phone Number</label>
                <input 
                  type="text" 
                  required
                  value={formData.phone}
                  onChange={(e) => setFormData({...formData, phone: e.target.value})}
                  className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
                  placeholder="e.g. +1 234 567 890"
                />
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1">Opening Hours</label>
                <input 
                  type="text" 
                  value={formData.openingHours}
                  onChange={(e) => setFormData({...formData, openingHours: e.target.value})}
                  className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
                  placeholder="e.g. 9 AM - 10 PM"
                />
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1">Latitude</label>
                <input 
                  type="text" 
                  value={formData.latitude}
                  onChange={(e) => setFormData({...formData, latitude: e.target.value})}
                  className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
                  placeholder="e.g. 25.2048"
                />
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1">Longitude</label>
                <input 
                  type="text" 
                  value={formData.longitude}
                  onChange={(e) => setFormData({...formData, longitude: e.target.value})}
                  className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
                  placeholder="e.g. 55.2708"
                />
              </div>
            </div>
            
            <div className="flex justify-end gap-3 pt-4 border-t border-slate-100 mt-6">
              <button 
                type="button" 
                onClick={resetForm}
                className="px-5 py-2 text-slate-600 font-bold hover:bg-slate-100 rounded-xl transition-colors"
              >
                Cancel
              </button>
              <button 
                type="submit" 
                className="px-5 py-2 bg-indigo-600 text-white font-bold rounded-xl hover:bg-indigo-700 transition-colors shadow-sm"
              >
                {editingId ? 'Update Branch' : 'Save Branch'}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="space-y-4">
        {myBranches.length === 0 ? (
          <div className="text-center py-16 bg-slate-50 border border-slate-200 border-dashed rounded-3xl">
            <Building2 className="w-12 h-12 text-slate-300 mx-auto mb-3" />
            <h3 className="text-lg font-bold text-slate-900 mb-1">No branches added yet</h3>
            <p className="text-slate-500 text-sm">Add your store locations so users can find you.</p>
          </div>
        ) : (
          myBranches.map((branch: any) => (
            <div key={branch.id} className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm flex flex-col gap-4 transition-all hover:border-indigo-200 hover:shadow-md">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="flex items-start gap-4">
                   <div className="w-12 h-12 bg-indigo-50 text-indigo-600 rounded-xl flex items-center justify-center shrink-0">
                     <MapPin className="w-6 h-6" />
                   </div>
                   <div>
                     <h3 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                       {branch.name}
                     </h3>
                     <div className="text-sm text-slate-500 mt-1 flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-4">
                       <span className="flex items-center gap-1"><Map className="w-4 h-4" /> {branch.address}, {branch.emirate}</span>
                       <span className="flex items-center gap-1"><Phone className="w-4 h-4" /> {branch.phone}</span>
                     </div>
                     <div className="mt-2 text-xs font-semibold flex items-center gap-4">
                       {branch.openingHours && (
                           <span className="text-indigo-600 font-medium bg-indigo-50 px-2 py-0.5 rounded-full">{branch.openingHours}</span>
                       )}
                       {branch.latitude && branch.longitude && (
                         <a href={`https://maps.google.com/?q=${branch.latitude},${branch.longitude}`} target="_blank" rel="noopener noreferrer" className="text-slate-500 hover:text-slate-700 underline">
                           Open Map Location
                         </a>
                       )}
                     </div>
                   </div>
                </div>
                <div className="flex items-center gap-2 self-end md:self-stretch md:items-center pt-4 md:pt-0 border-t md:border-0 border-slate-100 w-full md:w-auto justify-end">
                  <button 
                    onClick={() => handleEdit(branch)}
                    className="p-2 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors flex items-center gap-1 text-sm font-bold"
                  >
                    <Edit2 className="w-4 h-4" /> Edit
                  </button>
                  <button 
                    onClick={() => {
                      handleDelete(branch.id, branch.name);
                    }}
                    className="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors flex items-center gap-1 text-sm font-bold"
                  >
                    <Trash2 className="w-4 h-4" /> Delete
                  </button>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
