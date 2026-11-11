import React, { useMemo, useState } from 'react';
import { useCollection } from '../lib/firebase-hooks';
import { useFcmListener } from '../lib/useFcmListener';
import { Bell, FileText, Tag, CheckCircle, Clock, Settings, ShieldAlert, Wifi, Globe, MapPin, Store } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export function NotificationCenter({ onClose }: { onClose: () => void }) {
  const navigate = useNavigate();
  const { data: flyers } = useCollection<any>('flyers');
  const { data: promotions } = useCollection<any>('promotions');
  const { data: retailers } = useCollection<any>('retailers');

  // FCM listener client simulation hooks!
  const {
    fcmEnabled,
    selectedEmirate,
    followedRetailers,
    tokenStr,
    unreadDeliveries,
    requestPermission,
    updateProfileAndToken,
    markAllAsOpened
  } = useFcmListener();

  const [activeTab, setActiveTab] = useState<'alerts' | 'settings'>('alerts');

  const notifications = useMemo(() => {
    const items: any[] = [];
    
    // Add real persistent notification deliveries!
    unreadDeliveries.forEach((delivery: any) => {
      // Find original notification details
      const relatedNote = delivery.status !== 'sent' ? {
        title: 'Push Alert Received',
        description: `Delivered to subscriber ${delivery.email || 'active session'}`,
        alertType: 'general'
      } : null;
      
      items.push({
        id: `delivery-${delivery.id}`,
        title: relatedNote?.title || 'FCM Notification',
        description: relatedNote?.description || 'Broadcasting...',
        icon: <Bell className="w-5 h-5 text-indigo-600 animate-bounce" />,
        time: delivery.deliveredAt ? new Date(delivery.deliveredAt).toLocaleTimeString() : 'Just now',
        onClick: async () => {
          // If clicked, mark as opened!
          await markAllAsOpened();
          onClose();
        }
      });
    });

    // Add flyer notifications
    flyers.forEach((flyer: any) => {
      if (flyer.status === 'pending') {
         items.push({
           id: `flyer-pending-${flyer.id}`,
           title: 'New Flyer Pending Review',
           description: `${flyer.title} is waiting for OCR approval.`,
           icon: <Clock className="w-5 h-5 text-orange-500" />,
           time: 'Just now',
           onClick: () => { navigate(`/ocr/${flyer.id}`); onClose(); }
         });
      } else if (flyer.status === 'published') {
         items.push({
           id: `flyer-published-${flyer.id}`,
           title: 'Flyer Published',
           description: `${flyer.title} is now live on the consumer app.`,
           icon: <CheckCircle className="w-5 h-5 text-green-500" />,
           time: 'Recent',
           onClick: () => { navigate('/flyers'); onClose(); }
         });
      }
    });

    // Add promotion notifications
    promotions.forEach((promo: any) => {
       items.push({
         id: `promo-${promo.id}`,
         title: 'Promotion Active',
         description: `Code ${promo.code || promo.title} is live.`,
         icon: <Tag className="w-5 h-5 text-indigo-500" />,
         time: 'Recent',
         onClick: () => { navigate('/promotions'); onClose(); }
       });
    });

    return items;
  }, [flyers, promotions, unreadDeliveries, navigate, onClose, markAllAsOpened]);

  const toggleRetailerFollow = async (retailerId: string) => {
    let updated = [...followedRetailers];
    if (updated.includes(retailerId)) {
      updated = updated.filter(id => id !== retailerId);
    } else {
      updated.push(retailerId);
    }
    await updateProfileAndToken(fcmEnabled, selectedEmirate, updated);
  };

  const handleToggleFcmSwitch = async (checked: boolean) => {
    if (checked) {
      const allowed = await requestPermission();
      if (allowed) {
        await updateProfileAndToken(true, selectedEmirate, followedRetailers);
      }
    } else {
      await updateProfileAndToken(false, selectedEmirate, followedRetailers);
    }
  };

  return (
    <div className="absolute top-14 right-2 sm:right-6 w-[calc(100vw-16px)] sm:w-96 bg-white rounded-xl shadow-lg border border-slate-200 overflow-hidden z-50 animate-in fade-in slide-in-from-top-2 max-w-[400px] flex flex-col">
      <div className="p-3 border-b border-slate-100 flex justify-between items-center bg-slate-50 shrink-0">
        <h4 className="font-bold text-sm text-slate-800 flex items-center gap-1.5">
          <Bell className="w-4 h-4 text-indigo-600" /> Dispatch Alerts
        </h4>
        <div className="flex items-center gap-1">
          <button 
            onClick={() => setActiveTab('alerts')}
            className={`px-2 py-1 text-xs font-bold rounded-lg ${activeTab === 'alerts' ? 'bg-indigo-600 text-white' : 'text-slate-600 hover:bg-slate-200'}`}
          >
            Alerts
          </button>
          <button 
            onClick={() => setActiveTab('settings')}
            className={`px-2 py-1 text-xs font-bold rounded-lg flex items-center gap-1 ${activeTab === 'settings' ? 'bg-indigo-600 text-white' : 'text-slate-600 hover:bg-slate-200'}`}
          >
            <Settings className="w-3 h-3" /> FCM Setup
          </button>
        </div>
      </div>

      <div className="max-h-96 overflow-y-auto flex-1">
        {activeTab === 'alerts' ? (
          <div>
            {notifications.length === 0 ? (
              <div className="p-8 flex flex-col items-center justify-center text-center">
                <Bell className="w-8 h-8 text-slate-300 mb-2 animate-pulse" />
                <p className="text-sm font-semibold text-slate-600">No new notifications</p>
                <p className="text-xs text-slate-400 mt-1">You're all caught up!</p>
              </div>
            ) : (
              <ul className="divide-y divide-slate-100">
                {notifications.map((note) => (
                  <li 
                    key={note.id} 
                    className="p-4 hover:bg-slate-50 cursor-pointer transition-colors"
                    onClick={note.onClick}
                  >
                    <div className="flex gap-3">
                      <div className="shrink-0 mt-0.5">{note.icon}</div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-semibold text-slate-900 truncate">{note.title}</p>
                        <p className="text-xs text-slate-500 mt-0.5 line-clamp-2">{note.description}</p>
                        <p className="text-[10px] text-slate-400 mt-1.5">{note.time}</p>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
            
            {unreadDeliveries.length > 0 && (
              <div className="p-3 bg-indigo-50 border-t border-indigo-100 text-center shrink-0">
                <button onClick={markAllAsOpened} className="text-xs text-indigo-700 font-bold hover:underline">
                  Mark all unread deliveries as read
                </button>
              </div>
            )}
          </div>
        ) : (
          <div className="p-4 space-y-4">
            <div className="p-3 bg-slate-50 rounded-xl border border-slate-200">
               <div className="flex items-center justify-between">
                 <div>
                   <p className="text-xs font-bold text-slate-800 flex items-center gap-1">
                     <Wifi className={`w-3.5 h-3.5 ${fcmEnabled ? 'text-green-600 animate-pulse' : 'text-slate-400'}`} /> Push Emulator
                   </p>
                   <p className="text-[10px] text-slate-400 mt-0.5 font-mono">{fcmEnabled ? tokenStr : 'Token disabled'}</p>
                 </div>
                 <label className="relative inline-flex items-center cursor-pointer">
                    <input 
                      type="checkbox" 
                      checked={fcmEnabled} 
                      onChange={(e) => handleToggleFcmSwitch(e.target.checked)}
                      className="sr-only peer" 
                    />
                    <div className="w-9 h-5 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-indigo-600"></div>
                 </label>
               </div>
            </div>

            {fcmEnabled && (
              <div className="space-y-4 animate-in fade-in">
                <div>
                  <label className="block text-xs font-bold text-slate-600 mb-1 flex items-center gap-1">
                    <MapPin className="w-3.5 h-3.5 text-indigo-500" /> Select Sim Emirate
                  </label>
                  <select 
                    value={selectedEmirate}
                    onChange={async (e) => await updateProfileAndToken(true, e.target.value, followedRetailers)}
                    className="w-full px-3 py-1.5 border border-slate-200 rounded-xl text-xs bg-white focus:ring-2 focus:ring-indigo-500"
                  >
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
                  <label className="block text-xs font-bold text-slate-600 mb-1.5 flex items-center gap-1">
                    <Store className="w-3.5 h-3.5 text-indigo-500" /> Follow Retailers
                  </label>
                  <div className="border border-slate-200 rounded-xl p-2 max-h-32 overflow-y-auto space-y-1.5 bg-slate-50/50">
                    {retailers.map((r: any) => (
                      <div key={r.id} className="flex items-center gap-2">
                        <input 
                          type="checkbox" 
                          id={`follow-${r.id}`}
                          checked={followedRetailers.includes(r.id)}
                          onChange={() => toggleRetailerFollow(r.id)}
                          className="rounded text-indigo-600 focus:ring-indigo-500 w-3.5 h-3.5"
                        />
                        <label htmlFor={`follow-${r.id}`} className="text-xs text-slate-600 select-none truncate font-medium cursor-pointer">
                          {r.name}
                        </label>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
            
            <p className="text-[10px] text-slate-400 mt-2 text-center">
              Active subscriber sessions are stored in firestore and targeted in real push broadcasts!
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
