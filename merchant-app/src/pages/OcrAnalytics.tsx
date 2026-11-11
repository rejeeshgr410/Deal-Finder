import React from 'react';
import { ScanLine, Target, AlertTriangle, Zap, CheckCircle2 } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line, AreaChart, Area } from 'recharts';

const accuracyData = [
  { week: 'W1', accuracy: 82, manualCorrections: 450 },
  { week: 'W2', accuracy: 85, manualCorrections: 380 },
  { week: 'W3', accuracy: 86, manualCorrections: 350 },
  { week: 'W4', accuracy: 89, manualCorrections: 290 },
  { week: 'W5', accuracy: 92, manualCorrections: 210 },
  { week: 'W6', accuracy: 94, manualCorrections: 150 },
];

const errorTypes = [
  { type: 'Price Mismatch', count: 450 },
  { type: 'Incorrect Product Name', count: 320 },
  { type: 'Missed Barcode', count: 280 },
  { type: 'Invalid Category', count: 150 },
  { type: 'Unit/Size Error', count: 120 },
];

export default function OcrAnalytics() {
  return (
    <div className="flex-1 flex flex-col h-full max-w-6xl mx-auto w-full">
      <div className="mb-8">
         <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <ScanLine className="w-6 h-6 text-indigo-600" /> OCR Accuracy Dashboard
         </h1>
         <p className="text-slate-500 mt-1 text-sm">Monitor data extraction precision and human-in-the-loop correction rates.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
         <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between h-32">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1"><Target className="w-4 h-4"/> Base Accuracy</span>
            <div className="mt-4 flex items-end justify-between">
               <span className="text-3xl font-bold text-slate-900">94.2%</span>
               <span className="text-xs font-bold text-green-600 bg-green-50 px-2 py-1 rounded">+2.1%</span>
            </div>
         </div>
         <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between h-32">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1"><AlertTriangle className="w-4 h-4 text-orange-500"/> Correction Rate</span>
            <div className="mt-4 flex items-end justify-between">
               <span className="text-3xl font-bold text-slate-900">5.8%</span>
               <span className="text-xs font-bold text-green-600 bg-green-50 px-2 py-1 rounded">-1.5%</span>
            </div>
         </div>
         <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between h-32">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1"><Zap className="w-4 h-4 text-yellow-500"/> Scans Processed</span>
            <div className="mt-4 flex items-end justify-between">
               <span className="text-3xl font-bold text-slate-900">12.4k</span>
            </div>
         </div>
         <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between h-32">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1"><CheckCircle2 className="w-4 h-4 text-blue-500"/> Auto-Published</span>
            <div className="mt-4 flex items-end justify-between">
               <span className="text-3xl font-bold text-slate-900">88.5%</span>
               <span className="text-xs font-bold text-green-600 bg-green-50 px-2 py-1 rounded">+3.4%</span>
            </div>
         </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
         <div className="bg-white border border-slate-200 p-6 rounded-3xl shadow-sm h-[400px]">
            <h3 className="font-bold text-slate-900 mb-6 font-semibold">Accuracy Over Time (Last 6 Weeks)</h3>
            <ResponsiveContainer width="100%" height="100%">
               <AreaChart data={accuracyData}>
                  <defs>
                     <linearGradient id="colorAcc" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#10b981" stopOpacity={0.2}/>
                        <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                     </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis dataKey="week" axisLine={false} tickLine={false} tick={{fill: '#64748b', fontSize: 12}} dy={10} />
                  <YAxis domain={[75, 100]} axisLine={false} tickLine={false} tick={{fill: '#64748b', fontSize: 12}} dx={-10} />
                  <Tooltip contentStyle={{ borderRadius: '16px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }} />
                  <Area type="monotone" dataKey="accuracy" name="OCR Accuracy %" stroke="#10b981" strokeWidth={3} fillOpacity={1} fill="url(#colorAcc)" />
               </AreaChart>
            </ResponsiveContainer>
         </div>

         <div className="bg-white border border-slate-200 p-6 rounded-3xl shadow-sm h-[400px] flex flex-col">
            <h3 className="font-bold text-slate-900 mb-6 font-semibold">Manual Corrections Trend</h3>
            <ResponsiveContainer width="100%" height="80%">
               <LineChart data={accuracyData}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis dataKey="week" axisLine={false} tickLine={false} tick={{fill: '#64748b', fontSize: 12}} dy={10} />
                  <YAxis axisLine={false} tickLine={false} tick={{fill: '#64748b', fontSize: 12}} dx={-10} />
                  <Tooltip contentStyle={{ borderRadius: '16px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }} />
                  <Line type="monotone" dataKey="manualCorrections" name="Corrections Made" stroke="#f59e0b" strokeWidth={3} dot={{r: 4}} activeDot={{r: 6}} />
               </LineChart>
            </ResponsiveContainer>
         </div>

         <div className="bg-white border border-slate-200 p-6 rounded-3xl shadow-sm h-[350px] lg:col-span-2">
            <h3 className="font-bold text-slate-900 mb-6 font-semibold">Most Common OCR Error Types</h3>
            <ResponsiveContainer width="100%" height="100%">
               <BarChart data={errorTypes} margin={{ top: 0, right: 30, left: 0, bottom: 20 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis dataKey="type" axisLine={false} tickLine={false} tick={{fill: '#475569', fontSize: 12, fontWeight: 600}} dy={10} />
                  <YAxis type="number" axisLine={false} tickLine={false} tick={{fill: '#64748b', fontSize: 12}} />
                  <Tooltip cursor={{fill: '#f8fafc'}} contentStyle={{ borderRadius: '12px', border: '1px solid #e2e8f0', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }} />
                  <Bar dataKey="count" fill="#ef4444" radius={[4, 4, 0, 0]} barSize={40} name="Error Instances" />
               </BarChart>
            </ResponsiveContainer>
         </div>
      </div>
    </div>
  );
}
