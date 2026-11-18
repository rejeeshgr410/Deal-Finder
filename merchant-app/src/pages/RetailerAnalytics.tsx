import React, { useMemo } from 'react';
import { where } from 'firebase/firestore';
import { useAuth } from '../contexts/AuthContext';
import { useCollection } from '../lib/firebase-hooks';
import { BarChart2, Eye, MousePointer2, Percent, Package, Info, Clock } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

export default function RetailerAnalytics() {
  const { isRetailerUser, user } = useAuth();
  const tenantId = user.retailerId || '';

  const { data: products, loading: loadingProducts } = useCollection<any>('products', {
    constraints: tenantId ? [where('retailerId', '==', tenantId)] : [],
    enabled: Boolean(isRetailerUser && tenantId),
    filterKey: `ret-prod:${tenantId}`,
  });

  const { data: flyers, loading: loadingFlyers } = useCollection<any>('flyers', {
    constraints: tenantId ? [where('retailerId', '==', tenantId)] : [],
    enabled: Boolean(isRetailerUser && tenantId),
    filterKey: `ret-flyer:${tenantId}`,
  });

  const { data: analytics, loading: loadingAnalytics } = useCollection<any>('analytics', {
    constraints: tenantId ? [where('tenantId', '==', tenantId)] : [],
    enabled: Boolean(isRetailerUser && tenantId),
    filterKey: `ret-analytics:${tenantId}`,
  });

  const loading = loadingProducts || loadingFlyers || loadingAnalytics;

  const metrics = useMemo(() => {
    const now = Date.now();
    const in48h = now + 48 * 60 * 60 * 1000;
    const prodEvents = analytics.filter((e: any) => !e.isDemo && e.environment !== 'demo');

    const publishedOffers = products.filter((p: any) => p.status === 'published' && (!p.validTo || p.validTo > now));
    const expiringSoon = publishedOffers.filter((p: any) => p.validTo && p.validTo <= in48h);
    const expiredOffers = products.filter((p: any) => p.validTo && p.validTo <= now);

    const flyerViews = prodEvents.filter((e: any) => (e.eventName || e.type) === 'flyer_view').length;
    const productClicks = prodEvents.filter((e: any) => (e.eventName || e.type) === 'product_click').length;
    const couponSaves = prodEvents.filter((e: any) => (e.eventName || e.type) === 'coupon_save').length;

    const viewsByFlyer: Record<string, number> = {};
    prodEvents
      .filter((e: any) => (e.eventName || e.type) === 'flyer_view')
      .forEach((e: any) => {
        const fid = e.recordId || e.details?.flyerId;
        if (fid) viewsByFlyer[fid] = (viewsByFlyer[fid] || 0) + 1;
      });

    const topFlyers = flyers
      .map((f: any) => ({
        name: f.title || f.id,
        views: viewsByFlyer[f.id] || 0,
      }))
      .filter((f) => f.views > 0)
      .sort((a, b) => b.views - a.views)
      .slice(0, 6);

    return {
      publishedCount: publishedOffers.length,
      expiringSoonCount: expiringSoon.length,
      expiredCount: expiredOffers.length,
      flyerViews,
      productClicks,
      couponSaves,
      topFlyers,
    };
  }, [products, flyers, analytics]);

  if (!isRetailerUser) {
    return <div className="p-8 text-center text-slate-500">Retailer Analytics access only.</div>;
  }

  return (
    <div className="flex-1 flex flex-col h-full max-w-6xl mx-auto w-full">
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
          <BarChart2 className="w-6 h-6 text-indigo-600" /> Tenant Operational Analytics
        </h1>
        <p className="text-slate-500 mt-1 text-sm">
          Verified catalog freshness and privacy-safe engagement counts for your store (no estimated revenue attribution).
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1">
            <Package className="w-4 h-4 text-indigo-500" /> Active Published Offers
          </span>
          <div className="mt-4 flex items-end justify-between">
            <span className="text-3xl font-bold text-slate-900">{loading ? '—' : metrics.publishedCount}</span>
            <span className="text-xs text-amber-600 font-semibold flex items-center gap-1">
              <Clock className="w-3 h-3" /> {metrics.expiringSoonCount} expiring &lt;48h
            </span>
          </div>
        </div>

        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1">
            <Eye className="w-4 h-4" /> Verified Flyer Views
          </span>
          <div className="mt-4 flex items-end justify-between">
            <span className="text-3xl font-bold text-slate-900">{loading ? '—' : metrics.flyerViews}</span>
            <span className="text-xs text-slate-400">Measured events</span>
          </div>
        </div>

        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1">
            <MousePointer2 className="w-4 h-4" /> Product Clicks
          </span>
          <div className="mt-4 flex items-end justify-between">
            <span className="text-3xl font-bold text-slate-900">{loading ? '—' : metrics.productClicks}</span>
            <span className="text-xs text-slate-400">Intentional taps</span>
          </div>
        </div>

        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1">
            <Percent className="w-4 h-4" /> Coupon Saves
          </span>
          <div className="mt-4 flex items-end justify-between">
            <span className="text-3xl font-bold text-slate-900">{loading ? '—' : metrics.couponSaves}</span>
            <span className="text-xs text-slate-400">{metrics.expiredCount} expired offers</span>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-sm min-h-[340px] flex flex-col">
        <h3 className="font-bold text-slate-900 mb-4">Measured Flyer Engagement</h3>
        {metrics.topFlyers.length === 0 ? (
          <div className="flex-1 flex flex-col items-center justify-center text-center text-slate-500 p-10">
            <Info className="w-8 h-8 text-slate-300 mb-2" />
            <p className="font-semibold text-slate-700">No verified data yet.</p>
            <p className="text-xs mt-1">
              Flyer engagement bars populate strictly from authenticated <code className="font-mono">flyer_view</code> backend events.
            </p>
          </div>
        ) : (
          <div className="flex-1 min-h-[260px]">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={metrics.topFlyers} margin={{ top: 10, right: 20, left: 0, bottom: 20 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: '#475569', fontSize: 12 }} />
                <YAxis allowDecimals={false} axisLine={false} tickLine={false} tick={{ fill: '#64748b', fontSize: 12 }} />
                <Tooltip />
                <Bar dataKey="views" name="Verified Views" fill="#4f46e5" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>
    </div>
  );
}
