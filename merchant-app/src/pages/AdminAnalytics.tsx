import React, { useState, useMemo } from 'react';
import { BarChart2, Users, Eye, MousePointer2, Percent, Info, ShoppingBasket } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { useCollection } from '../lib/firebase-hooks';

export default function AdminAnalytics() {
  const [timeframe, setTimeframe] = useState<'7d' | '30d' | '90d' | 'all'>('7d');
  const { data: rawAnalytics, loading: analyticsLoading } = useCollection<any>('analytics');
  const { data: users, loading: usersLoading } = useCollection<any>('users');
  const { data: retailers } = useCollection<any>('retailers');

  const loading = analyticsLoading || usersLoading;

  const dMetrics = useMemo(() => {
    const now = Date.now();
    const cutoff =
      timeframe === '7d'
        ? now - 7 * 86400000
        : timeframe === '30d'
        ? now - 30 * 86400000
        : timeframe === '90d'
        ? now - 90 * 86400000
        : 0;

    // Exclude demo/sample records from authentic operational totals
    const events = rawAnalytics.filter((e: any) => {
      if (e.isDemo || e.environment === 'demo') return false;
      const ts = Number(e.occurredAt || e.timestamp || 0);
      return cutoff === 0 || ts >= cutoff;
    });

    const getName = (e: any) => e.eventName || e.type;
    const flyerViews = events.filter((e: any) => getName(e) === 'flyer_view').length;
    const productClicks = events.filter((e: any) => getName(e) === 'product_click').length;
    const couponSaves = events.filter((e: any) => getName(e) === 'coupon_save' || getName(e) === 'promotion_click').length;
    const basketComparisons = events.filter((e: any) => getName(e) === 'basket_comparison_completed').length;

    const searchCounts: Record<string, number> = {};
    events
      .filter((e: any) => getName(e) === 'search')
      .forEach((s: any) => {
        const q = (s.details?.query || '').trim();
        if (q) searchCounts[q] = (searchCounts[q] || 0) + 1;
      });

    const searchTerms = Object.entries(searchCounts)
      .map(([term, count]) => ({ term, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 8);

    const pageViews = events.filter((e: any) => getName(e) === 'retailer_page_view');
    const retailerPerformance = retailers
      .map((r: any) => ({
        name: r.name,
        views: pageViews.filter((v: any) => v.tenantId === r.id || v.details?.retailerId === r.id).length,
      }))
      .filter((r) => r.views > 0)
      .sort((a, b) => b.views - a.views)
      .slice(0, 8);

    return {
      totalUsers: users.length,
      flyerViews,
      productClicks,
      couponSaves,
      basketComparisons,
      searchTerms,
      retailerPerformance,
    };
  }, [rawAnalytics, users, retailers, timeframe]);

  return (
    <div className="flex-1 flex flex-col h-full max-w-7xl mx-auto w-full">
      <div className="mb-8 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <BarChart2 className="w-6 h-6 text-indigo-600" /> Platform Analytics
          </h1>
          <p className="text-slate-500 mt-1 text-sm">
            Verified backend event telemetry (PII-stripped, demo events excluded, zero synthetic fallbacks).
          </p>
        </div>
        <div>
          <select
            value={timeframe}
            onChange={(e) => setTimeframe(e.target.value as any)}
            className="px-4 py-2 border border-slate-200 rounded-xl bg-white font-bold text-sm shadow-sm focus:ring-2 focus:ring-indigo-500"
          >
            <option value="7d">Last 7 Days</option>
            <option value="30d">Last 30 Days</option>
            <option value="90d">Last 90 Days</option>
            <option value="all">All Time</option>
          </select>
        </div>
      </div>

      {loading ? (
        <div className="flex-1 flex items-center justify-center py-20">
          <div className="text-slate-500 font-medium animate-pulse">Loading verified backend telemetry...</div>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4 mb-6">
            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1">
                <Users className="w-4 h-4" /> Accounts
              </span>
              <span className="text-3xl font-bold text-slate-900 mt-4">{dMetrics.totalUsers}</span>
            </div>
            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1">
                <Eye className="w-4 h-4" /> Flyer Views
              </span>
              <span className="text-3xl font-bold text-slate-900 mt-4">{dMetrics.flyerViews}</span>
            </div>
            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1">
                <MousePointer2 className="w-4 h-4" /> Product Clicks
              </span>
              <span className="text-3xl font-bold text-slate-900 mt-4">{dMetrics.productClicks}</span>
            </div>
            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1">
                <ShoppingBasket className="w-4 h-4" /> Basket Compares
              </span>
              <span className="text-3xl font-bold text-slate-900 mt-4">{dMetrics.basketComparisons}</span>
            </div>
            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1">
                <Percent className="w-4 h-4" /> Coupon Saves
              </span>
              <span className="text-3xl font-bold text-slate-900 mt-4">{dMetrics.couponSaves}</span>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="bg-white border border-slate-200 p-6 rounded-2xl shadow-sm min-h-[340px] flex flex-col">
              <h3 className="font-bold text-slate-900 mb-4">Measured Search Queries</h3>
              {dMetrics.searchTerms.length === 0 ? (
                <div className="flex-1 flex flex-col items-center justify-center text-center text-slate-500 p-8">
                  <Info className="w-8 h-8 text-slate-300 mb-2" />
                  <p className="font-semibold text-slate-700">No verified data yet.</p>
                  <p className="text-xs mt-1">Measured search events recorded via `trackAnalyticsEvent` will appear here.</p>
                </div>
              ) : (
                <div className="flex-1 min-h-[260px]">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={dMetrics.searchTerms} margin={{ top: 10, right: 20, left: 0, bottom: 20 }}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                      <XAxis dataKey="term" axisLine={false} tickLine={false} tick={{ fill: '#475569', fontSize: 12 }} />
                      <YAxis allowDecimals={false} axisLine={false} tickLine={false} tick={{ fill: '#64748b', fontSize: 12 }} />
                      <Tooltip />
                      <Bar dataKey="count" name="Searches" fill="#4f46e5" radius={[6, 6, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </div>

            <div className="bg-white border border-slate-200 p-6 rounded-2xl shadow-sm min-h-[340px] flex flex-col">
              <h3 className="font-bold text-slate-900 mb-4">Retailer Page Views</h3>
              {dMetrics.retailerPerformance.length === 0 ? (
                <div className="flex-1 flex flex-col items-center justify-center text-center text-slate-500 p-8">
                  <Info className="w-8 h-8 text-slate-300 mb-2" />
                  <p className="font-semibold text-slate-700">No verified data yet.</p>
                  <p className="text-xs mt-1">Retailer engagement populates only from verified `retailer_page_view` events.</p>
                </div>
              ) : (
                <div className="flex-1 min-h-[260px]">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={dMetrics.retailerPerformance} margin={{ top: 10, right: 20, left: 0, bottom: 20 }}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                      <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: '#475569', fontSize: 12 }} />
                      <YAxis allowDecimals={false} axisLine={false} tickLine={false} tick={{ fill: '#64748b', fontSize: 12 }} />
                      <Tooltip />
                      <Bar dataKey="views" name="Page Views" fill="#0ea5e9" radius={[6, 6, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
