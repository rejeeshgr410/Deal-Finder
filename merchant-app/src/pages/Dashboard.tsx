import React, { useMemo, useState } from 'react';
import { FileText, ScanLine, Store, Tag, Package, BarChart2, Download, Calendar, Info } from 'lucide-react';
import { useCollection } from '../lib/firebase-hooks';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { where } from 'firebase/firestore';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

export default function Dashboard() {
  const navigate = useNavigate();
  const { user, isAdminUser, isRetailerUser } = useAuth();
  const tenantFilter = isRetailerUser && user.retailerId ? [where('retailerId', '==', user.retailerId)] : [];
  const analyticsFilter = isRetailerUser && user.retailerId ? [where('tenantId', '==', user.retailerId)] : [];
  const filterKey = `${user.role || ''}:${user.retailerId || ''}`;
  const { data: retailers, loading: loadingRetailers } = useCollection<any>('retailers', {
    constraints: isRetailerUser ? [where('status', '==', 'published')] : [],
    enabled: Boolean(user.role),
    filterKey
  });
  const { data: allFlyers, loading: loadingFlyers } = useCollection<any>('flyers', {
    constraints: tenantFilter,
    enabled: Boolean(user.role),
    filterKey
  });
  const { data: allProducts, loading: loadingProducts } = useCollection<any>('products', {
    constraints: tenantFilter,
    enabled: Boolean(user.role),
    filterKey
  });
  const { data: allPromotions, loading: loadingPromotions } = useCollection<any>('promotions', {
    constraints: tenantFilter,
    enabled: Boolean(user.role),
    filterKey
  });
  const { data: rawAnalytics } = useCollection<any>('analytics', {
    constraints: analyticsFilter,
    enabled: Boolean(user.role),
    filterKey
  });

  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  // Admin Data
  const activeRetailersCount = retailers.filter((r: any) => r.status === 'published' || r.status === 'Active').length;
  const pendingFlyers = allFlyers.filter((f: any) => ['processing', 'pending', 'pending_review', 'pending_admin_review', 'draft', 'ocr_review', 'review_required'].includes(f.status));
  const pendingOcrCount = pendingFlyers.length;

  // Retailer Data
  const retailerFlyers = isRetailerUser ? allFlyers.filter((f: any) => f.retailerId === user.retailerId) : [];
  const retailerProducts = isRetailerUser ? allProducts.filter((p: any) => p.retailerId === user.retailerId) : [];
  const retailerPromotions = isRetailerUser ? allPromotions.filter((p: any) => p.retailerId === user.retailerId) : [];

  const verifiedEvents = useMemo(() => {
    const startMs = startDate ? new Date(startDate).getTime() : 0;
    const endMs = endDate ? new Date(`${endDate}T23:59:59`).getTime() : Number.MAX_SAFE_INTEGER;
    return rawAnalytics.filter((e: any) => {
      if (e.isDemo || e.environment === 'demo') return false;
      const ts = Number(e.occurredAt || e.timestamp || 0);
      return ts >= startMs && ts <= endMs;
    });
  }, [rawAnalytics, startDate, endDate]);

  const measuredViewsCount = useMemo(
    () => verifiedEvents.filter((e: any) => (e.eventName || e.type) === 'flyer_view').length,
    [verifiedEvents]
  );

  const filteredPerformanceData = useMemo(() => {
    const buckets: Record<string, { name: string; views: number; clicks: number }> = {};
    verifiedEvents.forEach((e: any) => {
      const ts = Number(e.occurredAt || e.timestamp || 0);
      if (!ts) return;
      const dayKey = new Date(ts).toISOString().slice(0, 10);
      if (!buckets[dayKey]) buckets[dayKey] = { name: dayKey, views: 0, clicks: 0 };
      const name = e.eventName || e.type;
      if (name === 'flyer_view') buckets[dayKey].views += 1;
      if (name === 'product_click') buckets[dayKey].clicks += 1;
    });
    return Object.values(buckets).sort((a, b) => a.name.localeCompare(b.name));
  }, [verifiedEvents]);

  const adminStats = [
    { label: 'Total Retailers', value: loadingRetailers ? '-' : retailers.length.toString(), trend: 'Live', isPositive: true },
    { label: 'Published Retailers', value: loadingRetailers ? '-' : activeRetailersCount.toString(), trend: 'Live', isPositive: true },
    { label: 'Total Flyers', value: loadingFlyers ? '-' : allFlyers.length.toString(), trend: 'Live', isPositive: true },
    { label: 'Pending Review', value: loadingFlyers ? '-' : pendingOcrCount.toString(), trend: 'Live', isPositive: pendingOcrCount === 0 },
  ];

  const retailerStats = [
    { label: 'My Flyers', value: loadingFlyers ? '-' : retailerFlyers.length.toString(), trend: 'Live', isPositive: true },
    { label: 'My Products', value: loadingProducts ? '-' : retailerProducts.length.toString(), trend: 'Live', isPositive: true },
    { label: 'Active Promotions', value: loadingPromotions ? '-' : retailerPromotions.length.toString(), trend: 'Live', isPositive: true },
    { label: 'Verified Flyer Views', value: measuredViewsCount.toString(), trend: 'Measured', isPositive: true },
  ];

  const stats = isAdminUser ? adminStats : retailerStats;
  const displayFlyers = isAdminUser ? allFlyers : retailerFlyers;

  const isDateInRange = (dateStr: string) => {
    if (!dateStr) return true;
    if (!startDate && !endDate) return true;
    let d = new Date(dateStr);
    if (isNaN(d.getTime())) {
      const match = dateStr.match(/\d{4}-\d{2}-\d{2}/);
      if (match) d = new Date(match[0]);
      else return true;
    }
    const start = startDate ? new Date(startDate) : null;
    const end = endDate ? new Date(endDate) : null;
    if (start && d < start) return false;
    if (end) {
      const endDay = new Date(end);
      endDay.setHours(23, 59, 59, 999);
      if (d > endDay) return false;
    }
    return true;
  };

  const handleExportCSV = () => {
    let csvContent = "data:text/csv;charset=utf-8,";
    if (isRetailerUser) {
      csvContent += "Verified Engagement Report\n";
      csvContent += `Period: ${startDate || 'All Time'} to ${endDate || 'All Time'}\n\n`;
      csvContent += "Date,Verified Flyer Views,Product Clicks\n";
      filteredPerformanceData.forEach(row => {
        csvContent += `${row.name},${row.views},${row.clicks}\n`;
      });
      csvContent += "\nMy Flyers Overview\n";
      csvContent += "Title,Status,Date\n";
      retailerFlyers.filter((f: any) => isDateInRange(f.uploadDate || f.validFrom)).forEach((f: any) => {
        csvContent += `"${f.title}","${f.status}","${f.date || f.validFrom || '-'}"\n`;
      });
    } else {
      csvContent += "Admin Overview\n";
      csvContent += `Period: ${startDate || 'All Time'} to ${endDate || 'All Time'}\n\n`;
      csvContent += "Metric,Value\n";
      adminStats.forEach(stat => {
        csvContent += `"${stat.label}","${stat.value}"\n`;
      });
      csvContent += "\nAll Retailers\n";
      csvContent += "Name,Status,Branches\n";
      retailers.forEach((r: any) => {
        csvContent += `"${r.name || ''}","${r.status || ''}","${r.branches || 0}"\n`;
      });
      csvContent += "\nPending Review Flyers\n";
      csvContent += "Title,Retailer,Status\n";
      pendingFlyers.filter((f: any) => isDateInRange(f.uploadDate || f.validFrom)).forEach((f: any) => {
        csvContent += `"${f.title || ''}","${f.retailerId || ''}","${f.status || ''}"\n`;
      });
    }

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `analytics_report_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="flex-1 flex flex-col h-full">
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <h2 className="text-lg font-semibold text-slate-900">{isAdminUser ? 'Operations Command Center' : 'Retailer Dashboard'}</h2>
          <div className="px-2 py-1 bg-green-100 text-green-700 text-[10px] font-bold rounded uppercase tracking-wide">Real-time Active</div>
        </div>
        <div className="flex items-center gap-2 sm:gap-4 flex-wrap">
          <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-xl border border-slate-200">
            <Calendar className="w-4 h-4 text-slate-400" />
            <input 
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="text-xs sm:text-sm text-slate-600 bg-transparent outline-none cursor-pointer"
              title="Start Date"
            />
            <span className="text-slate-300">-</span>
            <input 
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="text-xs sm:text-sm text-slate-600 bg-transparent outline-none cursor-pointer"
              title="End Date"
            />
          </div>
          <button 
            onClick={handleExportCSV}
            className="flex items-center gap-2 px-3 py-1.5 sm:px-4 sm:py-2 bg-indigo-50 text-indigo-700 font-bold text-xs sm:text-sm rounded-xl hover:bg-indigo-100 transition-colors"
          >
            <Download className="w-4 h-4" /> Export Report
          </button>
        </div>
      </div>

      {/* Bento Grid Content */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 auto-rows-min">
        {/* Metric Cards */}
        {stats.map((stat, i) => (
          <div key={i} className="bg-white rounded-2xl border border-slate-200 p-5 flex flex-col justify-between min-h-[120px]">
            <span className="text-xs text-slate-500 font-medium uppercase">{stat.label}</span>
            <div className="flex items-end justify-between mt-auto pt-4">
              <span className={`text-3xl font-bold ${!stat.isPositive && stat.label === 'Pending OCR' ? 'text-orange-600' : 'text-slate-900'}`}>{stat.value}</span>
              <span className={`text-xs font-bold mb-1 ${stat.isPositive ? 'text-green-600' : 'text-orange-500'}`}>
                {stat.trend}
              </span>
            </div>
          </div>
        ))}

        {/* Recent Activity */}
        <div className={`sm:col-span-2 lg:col-span-3 lg:row-span-2 bg-white rounded-3xl border border-slate-200 overflow-hidden flex flex-col min-h-[400px]`}>
          <div className="p-5 border-b border-slate-100 flex justify-between items-center sm:min-h-[64px]">
            <h3 className="font-bold text-slate-900">Recent Flyers</h3>
            <button onClick={() => navigate('/flyers')} className="text-sm sm:text-xs text-indigo-600 font-bold hover:underline py-2">View All</button>
          </div>
          <div className="flex-1 overflow-y-auto">
            {loadingFlyers ? (
               <div className="p-8 text-center text-slate-500 font-medium animate-pulse">Loading activity...</div>
            ) : displayFlyers.length === 0 ? (
               <div className="p-8 text-center text-slate-500">No recent activity</div>
            ) : displayFlyers.slice(0, 5).map((flyer: any) => (
              <div key={flyer.id} className="flex sm:items-center flex-col sm:flex-row gap-3 sm:gap-4 p-4 sm:p-5 hover:bg-slate-50 transition-colors border-b border-slate-50 last:border-0">
                <div className="flex items-center gap-3 w-full sm:w-auto">
                  <div className="w-10 h-10 rounded-xl bg-indigo-50 flex items-center justify-center text-indigo-600 font-bold text-xs shrink-0">
                    <FileText className="w-5 h-5" />
                  </div>
                  <div className="flex-1 min-w-0 sm:hidden">
                    <p className="text-sm font-semibold text-slate-900 truncate">{flyer.title}</p>
                    <p className="text-[11px] text-slate-500 truncate">{flyer.retailerId || 'Generic Retailer'} • Validity: {flyer.date}</p>
                  </div>
                </div>
                <div className="hidden sm:block flex-1 min-w-0 md:min-w-[200px]">
                  <p className="text-sm font-semibold text-slate-900 truncate">{flyer.title}</p>
                  <p className="text-[11px] text-slate-500 truncate">{isAdminUser ? (flyer.retailerId || 'Generic Retailer') : `Validity: ${flyer.date}`}</p>
                </div>
                <div className="flex items-center gap-3 shrink-0 justify-between sm:justify-end w-full sm:w-auto ml-12 sm:ml-0">
                  <span className={`px-2 py-1 text-[10px] rounded-md font-bold uppercase tracking-wide inline-block ${
                    flyer.status === 'live' || flyer.status === 'published' ? 'bg-green-100 text-green-700' : 'bg-orange-100 text-orange-700'
                  }`}>
                    {flyer.status}
                  </span>
                  {isAdminUser && (
                    <button onClick={() => navigate(`/ocr/${flyer.id}`)} className="px-4 py-2 sm:px-3 sm:py-1.5 bg-slate-900 text-white text-xs sm:text-[10px] rounded-full sm:rounded-lg font-bold hover:bg-slate-800 transition-colors shadow-sm">Review</button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Quick Actions (Action Center) */}
        <div className="sm:col-span-2 lg:col-span-1 lg:row-span-2 bg-indigo-900 rounded-3xl p-5 border border-indigo-800/50 sm:p-6 text-white relative overflow-hidden flex flex-col justify-between sm:justify-start">
          <div className="relative z-10 flex-1 flex flex-col">
            <h3 className="text-lg font-semibold mb-1">Quick Actions</h3>
            <p className="text-xs text-indigo-200 mb-6">Manage platform data</p>
            
            <div className="grid grid-cols-2 sm:grid-cols-1 gap-3 sm:space-y-0 mt-auto sm:mt-0 pointer-events-auto">
              <button onClick={() => navigate('/flyers')} className="w-full flex sm:justify-start justify-center flex-col sm:flex-row items-center gap-2 sm:gap-3 px-3 py-4 sm:px-4 sm:py-3.5 bg-indigo-800/50 border border-indigo-700 text-white rounded-2xl hover:bg-indigo-800 transition-colors font-medium text-xs sm:text-sm shadow-sm active:scale-95">
                <FileText className="w-5 h-5 sm:w-4 sm:h-4 text-indigo-300" />
                <span>Upload Flyer</span>
              </button>
              {isAdminUser ? (
                <>
                  <button onClick={() => navigate('/retailers')} className="w-full flex sm:justify-start justify-center flex-col sm:flex-row items-center gap-2 sm:gap-3 px-3 py-4 sm:px-4 sm:py-3.5 bg-indigo-800/50 border border-indigo-700 text-white rounded-2xl hover:bg-indigo-800 transition-colors font-medium text-xs sm:text-sm shadow-sm active:scale-95">
                    <Store className="w-5 h-5 sm:w-4 sm:h-4 text-indigo-300" />
                    <span>Add Retailer</span>
                  </button>
                  <button onClick={() => navigate('/ocr')} className="w-full col-span-2 sm:col-span-1 flex sm:justify-start justify-center flex-col sm:flex-row items-center gap-2 sm:gap-3 px-3 py-4 sm:px-4 sm:py-3.5 bg-indigo-800/50 border border-indigo-700 text-white rounded-2xl hover:bg-indigo-800 transition-colors font-medium text-xs sm:text-sm shadow-sm active:scale-95">
                    <ScanLine className="w-5 h-5 sm:w-4 sm:h-4 text-indigo-300" />
                    <span>OCR Queue</span>
                  </button>
                </>
              ) : (
                <>
                  <button onClick={() => navigate('/products')} className="w-full flex sm:justify-start justify-center flex-col sm:flex-row items-center gap-2 sm:gap-3 px-3 py-4 sm:px-4 sm:py-3.5 bg-indigo-800/50 border border-indigo-700 text-white rounded-2xl hover:bg-indigo-800 transition-colors font-medium text-xs sm:text-sm shadow-sm active:scale-95">
                    <Package className="w-5 h-5 sm:w-4 sm:h-4 text-indigo-300" />
                    <span>My Products</span>
                  </button>
                  <button onClick={() => navigate('/promotions')} className="w-full col-span-2 sm:col-span-1 flex sm:justify-start justify-center flex-col sm:flex-row items-center gap-2 sm:gap-3 px-3 py-4 sm:px-4 sm:py-3.5 bg-indigo-800/50 border border-indigo-700 text-white rounded-2xl hover:bg-indigo-800 transition-colors font-medium text-xs sm:text-sm shadow-sm active:scale-95">
                    <Tag className="w-5 h-5 sm:w-4 sm:h-4 text-indigo-300" />
                    <span>New Promo</span>
                  </button>
                </>
              )}
            </div>
          </div>
          <div className="absolute -right-20 -bottom-20 sm:-top-20 w-64 h-64 bg-indigo-600 rounded-full blur-[80px] opacity-30 pointer-events-none"></div>
        </div>

        {/* Verified Engagement Metrics (Retailer Only) */}
        {isRetailerUser && (
          <div className="sm:col-span-2 lg:col-span-4 bg-white rounded-3xl border border-slate-200 overflow-hidden flex flex-col p-5 min-h-[340px] mt-2">
            <div className="flex justify-between items-center mb-6">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-green-50 flex items-center justify-center text-green-600">
                  <BarChart2 className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-slate-900">Verified Engagement Metrics</h3>
                  <p className="text-xs text-slate-500">Daily measured flyer views and product clicks from backend analytics</p>
                </div>
              </div>
            </div>
            {filteredPerformanceData.length === 0 ? (
              <div className="flex-1 flex flex-col items-center justify-center text-center text-slate-500 py-10">
                <Info className="w-8 h-8 text-slate-300 mb-2" />
                <p className="font-semibold text-slate-700">No verified data yet.</p>
                <p className="text-xs mt-1">Measured engagement events will populate this chart automatically.</p>
              </div>
            ) : (
              <div className="flex-1 w-full h-[260px]">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={filteredPerformanceData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                    <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: '#64748b', fontSize: 12 }} dy={10} />
                    <YAxis allowDecimals={false} axisLine={false} tickLine={false} tick={{ fill: '#64748b', fontSize: 12 }} dx={-10} />
                    <Tooltip contentStyle={{ borderRadius: '16px', border: '1px solid #e2e8f0' }} />
                    <Bar name="Verified Flyer Views" dataKey="views" fill="#4f46e5" radius={[6, 6, 0, 0]} />
                    <Bar name="Product Clicks" dataKey="clicks" fill="#f59e0b" radius={[6, 6, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
