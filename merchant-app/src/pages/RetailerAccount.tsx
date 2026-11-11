import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useCollection } from '../lib/firebase-hooks';
import { Store, Mail, KeyRound, MapPin, Save, User } from 'lucide-react';

export default function RetailerAccount() {
  const { user, isRetailerUser } = useAuth();
  const { data: retailers, update, loading } = useCollection<any>('retailers');
  const [formData, setFormData] = useState({ name: '', email: '', branchesCount: 0 });
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState('');

  const retailerData = retailers.find((r: any) => r.id === user.retailerId);

  useEffect(() => {
    if (retailerData) {
      setFormData({
        name: retailerData.name || '',
        email: retailerData.email || '',
        branchesCount: retailerData.branchesCount || retailerData.branches || 0
      });
    }
  }, [retailerData]);

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
         <div className="animate-pulse text-slate-500 font-medium">Loading account details...</div>
       </div>
    );
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user.retailerId) return;
    setIsSaving(true);
    setMessage('');
    try {
      await update(user.retailerId, formData);
      setMessage('Account details updated successfully.');
    } catch (err) {
      setMessage('Failed to update account details.');
    }
    setIsSaving(false);
  };

  return (
    <div className="flex-1 flex flex-col h-full max-w-3xl mx-auto w-full">
      <div className="mb-8">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">Manage Account</h1>
        <p className="text-slate-500 mt-1 text-sm">Update your store profile and login credentials.</p>
      </div>

      <div className="bg-white border border-slate-200 rounded-3xl shadow-sm overflow-hidden flex-1 p-6 sm:p-8">
        <form onSubmit={handleSave} className="space-y-6">
          
          <div className="flex items-center gap-4 pb-6 border-b border-slate-100">
             <div className="w-16 h-16 bg-indigo-50 text-indigo-600 rounded-2xl flex items-center justify-center shrink-0">
               <Store className="w-8 h-8" />
             </div>
             <div>
                <h2 className="text-xl font-bold text-slate-900">{formData.name || 'Store Name'}</h2>
                <div className="flex items-center gap-2 mt-1">
                  <span className="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-widest bg-green-100 text-green-700">Active Partner</span>
                </div>
             </div>
          </div>

          {message && (
             <div className={`p-4 rounded-xl text-sm font-semibold ${message.includes('success') ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'}`}>
               {message}
             </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-4">
              <h3 className="text-sm font-bold uppercase tracking-widest text-slate-400">Store Details</h3>
              
              <div>
                <label className="block text-sm font-semibold text-slate-900 mb-1 flex items-center gap-2">
                  <Store className="w-4 h-4 text-slate-400" /> Store Name
                </label>
                <input 
                  type="text" 
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                  className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-slate-900 mb-1 flex items-center gap-2">
                  <MapPin className="w-4 h-4 text-slate-400" /> Number of Branches
                </label>
                <div className="flex flex-col sm:flex-row gap-3">
                  <input 
                    type="number" 
                    min="0"
                    value={formData.branchesCount}
                    onChange={(e) => setFormData({...formData, branchesCount: parseInt(e.target.value) || 0})}
                    className="flex-1 px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
                  />
                  <Link to="/account/branches" className="w-full sm:w-auto px-4 py-2 bg-indigo-50 text-indigo-700 font-bold rounded-xl hover:bg-indigo-100 transition-colors flex justify-center items-center">
                    Manage Branches
                  </Link>
                </div>
              </div>
            </div>

            <div className="space-y-4 md:border-l border-slate-100 md:pl-6">
              <h3 className="text-sm font-bold uppercase tracking-widest text-slate-400">Login Credentials</h3>
              
              <div>
                <label className="block text-sm font-semibold text-slate-900 mb-1 flex items-center gap-2">
                  <Mail className="w-4 h-4 text-slate-400" /> Email Address
                </label>
                <input 
                  type="email" 
                  value={formData.email}
                  onChange={(e) => setFormData({...formData, email: e.target.value})}
                  className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 transition-colors"
                />
              </div>
            </div>
          </div>

          <div className="pt-6 border-t border-slate-100 flex justify-end">
            <button 
              type="submit" 
              disabled={isSaving}
              className="flex items-center gap-2 px-6 py-2.5 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors font-bold shadow-sm disabled:opacity-50"
            >
              <Save className="w-4 h-4" />
              {isSaving ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
