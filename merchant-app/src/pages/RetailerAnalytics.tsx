import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import { BarChart2, Eye, MousePointer2, Percent, TrendingUp } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line, AreaChart, Area } from 'recharts';

export default function RetailerAnalytics() {
  const { isRetailerUser, user } = useAuth();

  const performanceData = [
    { name: 'Jan', views: 400, searches: 240, clicks: 140 },
    { name: 'Feb', views: 300, searches: 139, clicks: 221 },
    { name: 'Mar', views: 500, searches: 480, clicks: 229 },
    { name: 'Apr', views: 478, searches: 390, clicks: 200 },
    { name: 'May', views: 589, searches: 480, clicks: 218 },
    { name: 'Jun', views: 439, searches: 380, clicks: 250 },
  ];

  if (!isRetailerUser) {
    return <div className="p-8 text-center text-slate-500">Retailer Analytics access only.</div>;
  }

  return (
    <div className="flex-1 flex flex-col h-full max-w-6xl mx-auto w-full">
      <div className="mb-8 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <BarChart2 className="w-6 h-6 text-indigo-600" /> Analytics & Performance
          </h1>
          <p className="text-slate-500 mt-1 text-sm">Track your flyers, product engagement, and promotion metrics.</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
         <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1"><Eye className="w-4 h-4"/> Flyer Views</span>
            <div className="mt-4 flex items-end justify-between">
               <span className="text-3xl font-bold text-slate-900">45.2k</span>
               <span className="text-xs font-bold text-green-600 bg-green-50 px-2 py-1 rounded">+12.5%</span>
            </div>
         </div>
         <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1"><MousePointer2 className="w-4 h-4"/> Product Clicks</span>
            <div className="mt-4 flex items-end justify-between">
               <span className="text-3xl font-bold text-slate-900">12.1k</span>
               <span className="text-xs font-bold text-green-600 bg-green-50 px-2 py-1 rounded">+8.1%</span>
            </div>
         </div>
         <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1"><TrendingUp className="w-4 h-4"/> Search Impressions</span>
            <div className="mt-4 flex items-end justify-between">
               <span className="text-3xl font-bold text-slate-900">89k</span>
               <span className="text-xs font-bold text-green-600 bg-green-50 px-2 py-1 rounded">+15.3%</span>
            </div>
         </div>
         <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1"><Percent className="w-4 h-4"/> Coupon Saves</span>
            <div className="mt-4 flex items-end justify-between">
               <span className="text-3xl font-bold text-slate-900">4.5k</span>
               <span className="text-xs font-bold text-green-600 bg-green-50 px-2 py-1 rounded">+22.1%</span>
            </div>
         </div>
      </div>

      <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-sm mb-6 h-[400px]">
         <h3 className="font-bold text-slate-900 mb-6">Engagement Trends</h3>
         <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={performanceData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
               <defs>
                  <linearGradient id="colorViewsR" x1="0" y1="0" x2="0" y2="1">
                     <stop offset="5%" stopColor="#4f46e5" stopOpacity={0.2}/>
                     <stop offset="95%" stopColor="#4f46e5" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorSearchesR" x1="0" y1="0" x2="0" y2="1">
                     <stop offset="5%" stopColor="#0ea5e9" stopOpacity={0.2}/>
                     <stop offset="95%" stopColor="#0ea5e9" stopOpacity={0}/>
                  </linearGradient>
               </defs>
               <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
               <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{fill: '#64748b', fontSize: 12}} dy={10} />
               <YAxis axisLine={false} tickLine={false} tick={{fill: '#64748b', fontSize: 12}} dx={-10} />
               <Tooltip contentStyle={{ borderRadius: '16px', border: '1px solid #e2e8f0', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }} />
               <Area type="monotone" name="Flyer Views" dataKey="views" stroke="#4f46e5" strokeWidth={3} fillOpacity={1} fill="url(#colorViewsR)" />
               <Area type="monotone" name="Search Impressions" dataKey="searches" stroke="#0ea5e9" strokeWidth={3} fillOpacity={1} fill="url(#colorSearchesR)" />
               <Line type="monotone" name="Click-Throughs" dataKey="clicks" stroke="#f59e0b" strokeWidth={3} dot={{r: 4}} activeDot={{r: 6}} />
            </AreaChart>
         </ResponsiveContainer>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
         <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-sm h-[350px]">
            <h3 className="font-bold text-slate-900 mb-6">Top Performing Flyers</h3>
            <div className="space-y-4">
               {[
                  { name: 'Weekend Super Deals', views: 12500, clicks: 4200 },
                  { name: 'Fresh Produce Special', views: 8200, clicks: 2100 },
                  { name: 'Electronics Sale', views: 7600, clicks: 3500 },
                  { name: 'Back to School', views: 5400, clicks: 1200 },
               ].map((f, i) => (
                  <div key={i} className="flex flex-col border-b border-slate-100 pb-3 last:border-0 last:pb-0">
                     <span className="font-semibold text-slate-900 mb-2">{f.name}</span>
                     <div className="flex gap-4">
                        <div className="flex-1 bg-slate-100 rounded-full h-2 overflow-hidden flex items-center">
                           <div className="bg-indigo-500 h-full rounded-full" style={{ width: `${(f.views / 15000) * 100}%`}}></div>
                        </div>
                        <span className="text-xs font-bold text-slate-500 w-24 text-right">{f.views.toLocaleString()} Views</span>
                     </div>
                  </div>
               ))}
            </div>
         </div>
      </div>
    </div>
  );
}
