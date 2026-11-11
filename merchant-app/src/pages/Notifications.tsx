import React, { useState, useMemo } from 'react';
import { Bell, Send, Users, MapPin, Tag, Store, FileText, AlertCircle, ShieldAlert, ChevronLeft, ChevronRight, RefreshCw, CheckCircle, Info } from 'lucide-react';
import { useCollection } from '../lib/firebase-hooks';
import { useAuth } from '../contexts/AuthContext';
import { db } from '../firebase';
import { collection, query, where, getDocs, addDoc } from 'firebase/firestore';

export default function Notifications() {
  const { data: notifications, add, loading } = useCollection<any>('notifications');
  const { data: retailers } = useCollection<any>('retailers');
  const { data: deliveries } = useCollection<any>('notificationDeliveries');

  const { isAdminUser } = useAuth();
  
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [targetType, setTargetType] = useState('broadcast');
  const [targetEmirate, setTargetEmirate] = useState('');
  const [targetRetailer, setTargetRetailer] = useState('');
  const [alertType, setAlertType] = useState('general');
  const [isSending, setIsSending] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Pagination for notification history
  const [currentPage, setCurrentPage] = useState(1);
  const ITEMS_PER_PAGE = 5;

  const paginatedNotifications = useMemo(() => {
    const sorted = [...notifications].sort((a: any, b: any) => b.timestamp - a.timestamp);
    const startIdx = (currentPage - 1) * ITEMS_PER_PAGE;
    return sorted.slice(startIdx, startIdx + ITEMS_PER_PAGE);
  }, [notifications, currentPage]);

  const totalPages = Math.ceil(notifications.length / ITEMS_PER_PAGE);

  const handleSendNotification = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isAdminUser) return;
    
    setIsSending(true);
    setErrorMessage(null);
    try {
      // 1. Store Notification Document in Firestore
      const noteRef = await add({
        title,
        message,
        targetType,
        targetEmirate: targetType === 'emirate' ? targetEmirate : null,
        targetRetailerId: targetType === 'retailer_followers' ? targetRetailer : null,
        alertType,
        status: 'sent',
        timestamp: Date.now()
      });

      // 2. Fetch active simulator tokens from Firestore
      const tokensSnap = await getDocs(query(collection(db, 'fcmTokens'), where('status', '==', 'active')));
      const matches: any[] = [];

      tokensSnap.forEach(tokDoc => {
        const tokData = tokDoc.data();
        let isMatch = false;

        if (targetType === 'broadcast') {
          isMatch = true;
        } else if (targetType === 'emirate' && tokData.emirate === targetEmirate) {
          isMatch = true;
        } else if (targetType === 'retailer_followers' && tokData.followedRetailers?.includes(targetRetailer)) {
          isMatch = true;
        }

        if (isMatch) {
          matches.push({ id: tokDoc.id, ...tokData });
        }
      });

      // 3. Save matching FCM delivery logs for verification
      if (matches.length > 0) {
        await Promise.all(matches.map(async (tok) => {
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

      setTitle('');
      setMessage('');
      setTargetType('broadcast');
      setAlertType('general');
      alert(`Message broadcast successfully. Linked ${matches.length} delivery records!`);
    } catch (err: any) {
      console.error(err);
      setErrorMessage('FCM dispatch failed. Verify Firestore security rules and network state.');
    } finally {
      setIsSending(false);
    }
  };

  const calculateTelemetry = (notificationId: string) => {
    const related = deliveries.filter((d: any) => d.notificationId === notificationId);
    
    const sent = related.length;
    const delivered = related.filter((d: any) => d.status === 'delivered' || d.status === 'opened').length;
    const opened = related.filter((d: any) => d.status === 'opened').length;
    const failed = related.filter((d: any) => d.status === 'failed').length;

    return { sent, delivered, opened, failed };
  };

  if (!isAdminUser) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center h-full min-h-[400px]">
         <ShieldAlert className="w-16 h-16 text-slate-300 mb-4" />
         <h2 className="text-xl font-bold text-slate-900 font-sans">Access Restricted</h2>
         <p className="text-slate-500 mt-1 max-w-sm text-center text-sm">Only system administrators can broadcast standard push notifications.</p>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col h-full max-w-6xl mx-auto w-full">
      <div className="mb-8">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
          <Bell className="w-6 h-6 text-indigo-600" /> Notifications & Alerts
        </h1>
        <p className="text-slate-500 mt-1 text-sm">Send push notifications to users based on emirate, followers, or globally.</p>
      </div>

      {errorMessage && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 rounded-2xl flex items-center justify-between shadow-sm animate-in fade-in">
          <div className="flex items-center gap-2 text-sm font-semibold">
            <AlertCircle className="w-5 h-5 flex-shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button onClick={() => setErrorMessage(null)} className="px-3 py-1.5 bg-white text-xs border border-red-300 rounded-lg hover:bg-slate-50 transition-colors font-bold flex items-center gap-1 shrink-0">
            <RefreshCw className="w-3.5 h-3.5" /> Retry
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Composer Form */}
        <div className="lg:col-span-1 border border-slate-200 rounded-3xl p-6 bg-white shadow-sm flex flex-col">
          <h2 className="text-lg font-bold text-slate-900 mb-4 flex items-center gap-2">
            <Send className="w-5 h-5 text-indigo-500" /> Compose Message
          </h2>
          <form onSubmit={handleSendNotification} className="flex-1 flex flex-col space-y-4">
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">Alert Type</label>
              <select value={alertType} onChange={(e) => setAlertType(e.target.value)} className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 text-sm">
                <option value="general">General Broadcast</option>
                <option value="flyer_launch">Flyer Launch Alert</option>
                <option value="promotion">Promotion Alert</option>
                <option value="coupon">Coupon Alert</option>
                <option value="retailer_update">Retailer Update</option>
              </select>
            </div>
            
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">Target Audience</label>
              <select value={targetType} onChange={(e) => setTargetType(e.target.value)} className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 text-sm">
                <option value="broadcast">All Users (Broadcast)</option>
                <option value="emirate">Specific Emirate</option>
                <option value="retailer_followers">Retailer Followers</option>
              </select>
            </div>

            {targetType === 'emirate' && (
              <div className="animate-in fade-in slide-in-from-top-1 text-sm">
                <label className="block text-sm font-semibold text-slate-700 mb-1" id="label-emirate-select">Select Emirate</label>
                <select id="emirate-select" value={targetEmirate} onChange={(e) => setTargetEmirate(e.target.value)} className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500" required>
                  <option value="" disabled>Select Emirate...</option>
                  <option value="Abu Dhabi">Abu Dhabi</option>
                  <option value="Dubai">Dubai</option>
                  <option value="Sharjah">Sharjah</option>
                  <option value="Ajman">Ajman</option>
                  <option value="Umm Al Quwain">Umm Al Quwain</option>
                  <option value="Ras Al Khaimah">Ras Al Khaimah</option>
                  <option value="Fujairah">Fujairah</option>
                </select>
              </div>
            )}

            {targetType === 'retailer_followers' && (
              <div className="animate-in fade-in slide-in-from-top-1 text-sm">
                <label className="block text-sm font-semibold text-slate-700 mb-1" id="label-retailer-select">Select Retailer</label>
                <select id="retailer-select" value={targetRetailer} onChange={(e) => setTargetRetailer(e.target.value)} className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500" required>
                  <option value="" disabled>Select Retailer...</option>
                  {retailers.map((r: any) => (
                    <option key={r.id} value={r.id}>{r.name}</option>
                  ))}
                </select>
              </div>
            )}

            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">Notification Title</label>
              <input type="text" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="e.g. Weekend Super Sale!" className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 text-sm" required maxLength={60} />
            </div>

            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">Message Body</label>
              <textarea value={message} onChange={(e) => setMessage(e.target.value)} placeholder="Enter the push notification message..." className="w-full px-4 py-3 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 min-h-[120px] resize-none text-sm" required maxLength={150} />
              <div className="text-right text-xs text-slate-400 mt-1">{message.length}/150</div>
            </div>

            <div className="mt-auto pt-6">
               <button id="noti-send-btn" type="submit" disabled={isSending} className="w-full py-3 bg-indigo-600 text-white rounded-xl font-bold hover:bg-indigo-700 transition-colors disabled:opacity-70 flex items-center justify-center gap-2">
                 {isSending ? 'Sending...' : 'Send Notification'} <Send className="w-4 h-4" />
               </button>
            </div>
          </form>
        </div>

        {/* Dispatch Log Card */}
        <div className="lg:col-span-2 border border-slate-200 rounded-3xl bg-white shadow-sm overflow-hidden flex flex-col min-h-[400px]">
          <div className="p-6 border-b border-slate-100 flex items-center justify-between">
            <h2 className="text-lg font-bold text-slate-900">Notification History</h2>
            <div className="px-3 py-1 bg-slate-100 rounded-lg text-xs font-bold text-slate-600">
               {notifications.length} Sent
            </div>
          </div>
          <div className="flex-1 overflow-y-auto p-0">
             {loading ? (
                <div className="p-12 text-center text-slate-500 font-medium animate-pulse">Loading broadcast history...</div>
             ) : notifications.length === 0 ? (
                <div className="p-12 text-center text-slate-500 flex flex-col items-center justify-center h-full min-h-[250px]">
                   <AlertCircle className="w-12 h-12 text-slate-300 mb-3" />
                   <p className="font-medium text-slate-900">No Notifications Sent</p>
                   <p className="text-sm">Broadcast history will appear here.</p>
                </div>
             ) : (
                <div className="divide-y divide-slate-100">
                   {paginatedNotifications.map((note: any) => {
                     const tel = calculateTelemetry(note.id);
                     return (
                      <div key={note.id} className="p-6 hover:bg-slate-50 transition-colors">
                        <div className="flex items-start justify-between gap-4">
                           <div className="flex-1">
                              <div className="flex items-center gap-2 mb-2">
                                 <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-widest ${
                                    note.alertType === 'flyer_launch' ? 'bg-blue-100 text-blue-700' :
                                    note.alertType === 'promotion' ? 'bg-green-100 text-green-700' :
                                    note.alertType === 'coupon' ? 'bg-orange-100 text-orange-700' :
                                    'bg-slate-200 text-slate-700'
                                 }`}>
                                    {note.alertType?.replace('_', ' ') || 'general'}
                                 </span>
                                 <span className="text-xs text-slate-400 font-medium whitespace-nowrap">
                                    {new Date(note.timestamp).toLocaleString()}
                                 </span>
                              </div>
                              <h3 className="font-bold text-slate-900 mb-1">{note.title}</h3>
                              <p className="text-sm text-slate-600 mb-4">{note.message}</p>
                              
                              {/* Rich delivery tracking report details */}
                              <div className="grid grid-cols-4 gap-2 max-w-sm mb-4 border border-slate-100 bg-slate-50 p-2.5 rounded-xl font-mono text-[10px] text-center">
                                <div>
                                  <div className="text-slate-400 uppercase tracking-widest font-bold">Sent</div>
                                  <div className="font-bold text-slate-800 text-xs mt-0.5">{tel.sent}</div>
                                </div>
                                <div>
                                  <div className="text-slate-400 uppercase tracking-widest font-bold">Delivered</div>
                                  <div className="font-bold text-green-600 text-xs mt-0.5">{tel.delivered}</div>
                                </div>
                                <div>
                                  <div className="text-slate-400 uppercase tracking-widest font-bold font-semibold">Opened</div>
                                  <div className="font-bold text-indigo-600 text-xs mt-0.5">{tel.opened}</div>
                                </div>
                                <div>
                                  <div className="text-slate-400 uppercase tracking-widest font-bold">Failed</div>
                                  <div className="font-bold text-red-500 text-xs mt-0.5">{tel.failed}</div>
                                </div>
                              </div>

                              <div className="flex items-center gap-3 text-xs text-slate-500 font-medium">
                                 <div className="flex items-center gap-1 bg-slate-100 px-2 py-1 rounded-md">
                                    {note.targetType === 'broadcast' ? <Users className="w-3 h-3" /> : 
                                     note.targetType === 'emirate' ? <MapPin className="w-3 h-3" /> : 
                                     <Store className="w-3 h-3" />}
                                    {note.targetType === 'broadcast' ? 'All Users' :
                                     note.targetType === 'emirate' ? note.targetEmirate :
                                     'Retailer Followers'}
                                 </div>
                              </div>
                           </div>
                           <div className="text-right shrink-0">
                               <div className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Status</div>
                               <div className="text-sm font-bold text-green-600 flex items-center gap-1 justify-end">
                                  <CheckCircle className="w-4 h-4" /> Dispatched
                                </div>
                           </div>
                        </div>
                      </div>
                     );
                   })}
                </div>
             )}
          </div>

          {totalPages > 1 && (
            <div className="px-6 py-4 border-t border-slate-200 bg-slate-50/50 flex justify-between items-center shrink-0">
              <span className="text-xs text-slate-500 font-medium">
                Showing {(currentPage - 1) * ITEMS_PER_PAGE + 1} - {Math.min(currentPage * ITEMS_PER_PAGE, notifications.length)} of {notifications.length}
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
      </div>
    </div>
  );
}
