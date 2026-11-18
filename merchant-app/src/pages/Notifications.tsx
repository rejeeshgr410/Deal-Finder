import React, { useState, useMemo } from 'react';
import { Bell, Send, AlertCircle, ShieldAlert, ChevronLeft, ChevronRight, RefreshCw, Info, CheckCircle2, XCircle } from 'lucide-react';
import { httpsCallable } from 'firebase/functions';
import { useCollection } from '../lib/firebase-hooks';
import { useAuth } from '../contexts/AuthContext';
import { functions } from '../firebase';

export default function Notifications() {
  const { data: notifications, loading } = useCollection<any>('notifications');
  const { data: retailers } = useCollection<any>('retailers');
  const { isAdminUser, isRetailerUser, user } = useAuth();

  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [targetType, setTargetType] = useState<'broadcast' | 'emirate' | 'retailer_followers'>(
    isRetailerUser && !isAdminUser ? 'retailer_followers' : 'broadcast'
  );
  const [targetEmirate, setTargetEmirate] = useState('');
  const [targetRetailer, setTargetRetailer] = useState(user.retailerId || '');
  const [alertType, setAlertType] = useState('general');
  const [isSending, setIsSending] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [dispatchSummary, setDispatchSummary] = useState<string | null>(null);

  const [currentPage, setCurrentPage] = useState(1);
  const ITEMS_PER_PAGE = 5;

  const paginatedNotifications = useMemo(() => {
    const sorted = [...notifications].sort((a: any, b: any) => (b.timestamp || 0) - (a.timestamp || 0));
    const startIdx = (currentPage - 1) * ITEMS_PER_PAGE;
    return sorted.slice(startIdx, startIdx + ITEMS_PER_PAGE);
  }, [notifications, currentPage]);

  const totalPages = Math.max(1, Math.ceil(notifications.length / ITEMS_PER_PAGE));

  const handleSendNotification = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isAdminUser && !isRetailerUser) return;

    setIsSending(true);
    setErrorMessage(null);
    setDispatchSummary(null);
    try {
      const dispatchCampaign = httpsCallable<any, any>(functions, 'sendNotificationCampaign');
      const result = await dispatchCampaign({
        title,
        message,
        targetType,
        targetEmirate: targetType === 'emirate' ? targetEmirate : null,
        targetRetailerId: targetType === 'retailer_followers' ? (isAdminUser ? targetRetailer : user.retailerId) : null,
        alertType,
      });

      const data = result.data || {};
      setTitle('');
      setMessage('');
      setDispatchSummary(
        `Campaign queued & dispatched to FCM: ${data.acceptedByProvider ?? 0} accepted by provider, ${data.providerRejected ?? 0} rejected, ${data.optedOutSkipped ?? 0} opted-out skipped. (Provider acceptance is not proof of device delivery.)`
      );
    } catch (err: any) {
      console.error(err);
      setErrorMessage(err?.message || 'Trusted backend FCM dispatch failed. Verify Cloud Functions deployment and permissions.');
    } finally {
      setIsSending(false);
    }
  };

  if (!isAdminUser && !isRetailerUser) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center h-full min-h-[400px]">
        <ShieldAlert className="w-16 h-16 text-slate-300 mb-4" />
        <h2 className="text-xl font-bold text-slate-900 font-sans">Access Restricted</h2>
        <p className="text-slate-500 mt-1 max-w-sm text-center text-sm">
          Only administrators or authorized retailer accounts can dispatch notifications.
        </p>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col h-full max-w-6xl mx-auto w-full">
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
          <Bell className="w-6 h-6 text-indigo-600" /> Notifications & Alerts
        </h1>
        <p className="text-slate-500 mt-1 text-sm">
          Server-verified push campaigns dispatched via Firebase Admin SDK with opt-out enforcement.
        </p>
      </div>

      <div className="mb-6 p-4 bg-blue-50 border border-blue-200 text-blue-900 rounded-2xl flex items-start gap-3 text-xs">
        <Info className="w-4 h-4 mt-0.5 shrink-0 text-blue-600" />
        <div>
          <span className="font-bold">Notification Truth Model:</span> Campaigns track{' '}
          <code className="font-mono">queued → accepted_by_provider → client_received → opened</code>.
          FCM provider acceptance only confirms Firebase accepted the send request — it is <strong>not</strong> proof of physical-device delivery.
        </div>
      </div>

      {dispatchSummary && (
        <div className="mb-6 p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl flex items-center gap-2 text-sm font-medium">
          <CheckCircle2 className="w-5 h-5 shrink-0 text-emerald-600" />
          <span>{dispatchSummary}</span>
        </div>
      )}

      {errorMessage && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 rounded-2xl flex items-center justify-between shadow-sm">
          <div className="flex items-center gap-2 text-sm font-semibold">
            <AlertCircle className="w-5 h-5 flex-shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button
            onClick={() => setErrorMessage(null)}
            className="px-3 py-1.5 bg-white text-xs border border-red-300 rounded-lg hover:bg-slate-50 transition-colors font-bold flex items-center gap-1 shrink-0"
          >
            <RefreshCw className="w-3.5 h-3.5" /> Dismiss
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-1 border border-slate-200 rounded-3xl p-6 bg-white shadow-sm flex flex-col">
          <h2 className="text-lg font-bold text-slate-900 mb-4 flex items-center gap-2">
            <Send className="w-5 h-5 text-indigo-500" /> Compose Campaign
          </h2>
          <form onSubmit={handleSendNotification} className="flex-1 flex flex-col space-y-4">
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">Alert Type</label>
              <select
                value={alertType}
                onChange={(e) => setAlertType(e.target.value)}
                className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 text-sm"
              >
                <option value="general">General Update (Opt-in Required)</option>
                <option value="flyer_launch">Flyer Launch Alert</option>
                <option value="promotion">Promotion Alert (Marketing Opt-in)</option>
                <option value="coupon">Coupon Alert</option>
                <option value="retailer_update">Retailer Update</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">Target Audience</label>
              <select
                value={targetType}
                onChange={(e) => setTargetType(e.target.value as any)}
                className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 text-sm"
              >
                {isAdminUser && <option value="broadcast">All Opted-in Users (Broadcast)</option>}
                {isAdminUser && <option value="emirate">Specific Emirate</option>}
                <option value="retailer_followers">Retailer Followers</option>
              </select>
            </div>

            {targetType === 'emirate' && isAdminUser && (
              <div className="text-sm">
                <label className="block text-sm font-semibold text-slate-700 mb-1">Select Emirate</label>
                <select
                  value={targetEmirate}
                  onChange={(e) => setTargetEmirate(e.target.value)}
                  className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500"
                  required
                >
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

            {targetType === 'retailer_followers' && isAdminUser && (
              <div className="text-sm">
                <label className="block text-sm font-semibold text-slate-700 mb-1">Select Retailer</label>
                <select
                  value={targetRetailer}
                  onChange={(e) => setTargetRetailer(e.target.value)}
                  className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500"
                  required
                >
                  <option value="" disabled>Select Retailer...</option>
                  {retailers.map((r: any) => (
                    <option key={r.id} value={r.id}>{r.name}</option>
                  ))}
                </select>
              </div>
            )}

            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">Notification Title</label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="e.g. New Verified Weekly Flyer"
                className="w-full px-4 py-2 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 text-sm"
                required
                maxLength={60}
              />
            </div>

            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">Message Body</label>
              <textarea
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                placeholder="Enter the push notification message..."
                className="w-full px-4 py-3 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 min-h-[120px] resize-none text-sm"
                required
                maxLength={150}
              />
              <div className="text-right text-xs text-slate-400 mt-1">{message.length}/150</div>
            </div>

            <div className="mt-auto pt-4">
              <button
                id="noti-send-btn"
                type="submit"
                disabled={isSending}
                className="w-full py-3 bg-indigo-600 text-white rounded-xl font-bold hover:bg-indigo-700 transition-colors disabled:opacity-70 flex items-center justify-center gap-2"
              >
                {isSending ? 'Dispatching via Admin SDK...' : 'Dispatch Campaign'} <Send className="w-4 h-4" />
              </button>
            </div>
          </form>
        </div>

        <div className="lg:col-span-2 border border-slate-200 rounded-3xl bg-white shadow-sm overflow-hidden flex flex-col min-h-[400px]">
          <div className="p-6 border-b border-slate-100 flex items-center justify-between">
            <h2 className="text-lg font-bold text-slate-900">Campaign Dispatch History</h2>
            <div className="px-3 py-1 bg-slate-100 rounded-lg text-xs font-bold text-slate-600">
              {notifications.length} Campaigns
            </div>
          </div>
          <div className="flex-1 overflow-y-auto">
            {loading ? (
              <div className="p-12 text-center text-slate-500 font-medium animate-pulse">Loading dispatch history...</div>
            ) : notifications.length === 0 ? (
              <div className="p-12 text-center text-slate-500 flex flex-col items-center justify-center h-full min-h-[250px]">
                <AlertCircle className="w-12 h-12 text-slate-300 mb-3" />
                <p className="font-medium text-slate-900">No verified campaigns yet</p>
                <p className="text-sm">Server-dispatched notification campaigns will appear here.</p>
              </div>
            ) : (
              <div className="divide-y divide-slate-100">
                {paginatedNotifications.map((note: any) => (
                  <div key={note.id} className="p-6 hover:bg-slate-50 transition-colors">
                    <div className="flex items-start justify-between gap-4 mb-2">
                      <div>
                        <div className="flex items-center gap-2 mb-1">
                          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-indigo-50 text-indigo-700">
                            {note.alertType || 'general'}
                          </span>
                          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-slate-100 text-slate-600">
                            {note.status || 'queued'}
                          </span>
                        </div>
                        <h3 className="font-bold text-slate-900">{note.title}</h3>
                        <p className="text-sm text-slate-600 mt-0.5">{note.message}</p>
                      </div>
                      <span className="text-xs text-slate-400 shrink-0">
                        {note.timestamp ? new Date(note.timestamp).toLocaleString() : '—'}
                      </span>
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-5 gap-2 mt-4 pt-3 border-t border-slate-100 text-xs">
                      <div className="bg-slate-50 rounded-xl p-2">
                        <div className="text-slate-400">Matched</div>
                        <div className="font-bold text-slate-800">{note.matchedTokens ?? 0}</div>
                      </div>
                      <div className="bg-emerald-50 rounded-xl p-2">
                        <div className="text-emerald-700">Provider Accepted</div>
                        <div className="font-bold text-emerald-900">{note.acceptedByProvider ?? 0}</div>
                      </div>
                      <div className="bg-red-50 rounded-xl p-2">
                        <div className="text-red-700 flex items-center gap-1"><XCircle className="w-3 h-3" /> Rejected</div>
                        <div className="font-bold text-red-900">{note.providerRejected ?? 0}</div>
                      </div>
                      <div className="bg-blue-50 rounded-xl p-2">
                        <div className="text-blue-700">Client Received</div>
                        <div className="font-bold text-blue-900">{note.clientReceived ?? 0}</div>
                      </div>
                      <div className="bg-amber-50 rounded-xl p-2">
                        <div className="text-amber-700">Opted-out Skipped</div>
                        <div className="font-bold text-amber-900">{note.optedOutSkipped ?? 0}</div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {notifications.length > ITEMS_PER_PAGE && (
            <div className="p-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
              <span>Page {currentPage} of {totalPages}</span>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                  disabled={currentPage === 1}
                  className="p-1.5 border border-slate-200 rounded-lg disabled:opacity-40"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <button
                  onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                  disabled={currentPage === totalPages}
                  className="p-1.5 border border-slate-200 rounded-lg disabled:opacity-40"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
