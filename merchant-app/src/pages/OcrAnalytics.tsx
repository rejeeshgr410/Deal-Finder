import React, { useMemo } from 'react';
import { ScanLine, Target, AlertTriangle, Zap, CheckCircle2, Clock, Info } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { useCollection } from '../lib/firebase-hooks';

export default function OcrAnalytics() {
  const { data: jobs, loading: jobsLoading } = useCollection<any>('extractionJobs');
  const { data: analytics, loading: analyticsLoading } = useCollection<any>('analytics');

  const loading = jobsLoading || analyticsLoading;

  const metrics = useMemo(() => {
    const prodJobs = jobs.filter((j: any) => !j.isDemo && j.environment !== 'demo');
    const prodEvents = analytics.filter((e: any) => !e.isDemo && e.environment !== 'demo');

    const totalJobs = prodJobs.length;
    const completedJobs = prodJobs.filter((j: any) => j.status === 'completed' || j.status === 'published');
    const failedJobs = prodJobs.filter((j: any) => j.status === 'failed');

    const extractedItems = prodJobs.reduce((sum: number, j: any) => sum + (Number(j.extractedCount) || 0), 0);
    const approvedItems = prodJobs.reduce((sum: number, j: any) => sum + (Number(j.approvedCount) || 0), 0);
    const rejectedItems = prodJobs.reduce((sum: number, j: any) => sum + (Number(j.rejectedCount) || 0), 0);

    // Median minutes between upload (createdAt) and approval (publishedAt)
    const durationsMin = prodJobs
      .filter((j: any) => typeof j.createdAt === 'number' && typeof j.publishedAt === 'number' && j.publishedAt >= j.createdAt)
      .map((j: any) => (j.publishedAt - j.createdAt) / 60000)
      .sort((a: number, b: number) => a - b);

    const medianMinutes =
      durationsMin.length === 0
        ? null
        : durationsMin.length % 2 === 1
        ? durationsMin[Math.floor(durationsMin.length / 2)]
        : (durationsMin[durationsMin.length / 2 - 1] + durationsMin[durationsMin.length / 2]) / 2;

    // Count manual corrections by field from verified analytics events
    const correctionEvents = prodEvents.filter((e: any) => (e.eventName || e.type) === 'manual_correction');
    const fieldCounts: Record<string, number> = {};
    correctionEvents.forEach((e: any) => {
      const field = e.details?.field || 'Unspecified field';
      fieldCounts[field] = (fieldCounts[field] || 0) + 1;
    });
    const correctionChart = Object.entries(fieldCounts)
      .map(([type, count]) => ({ type, count }))
      .sort((a, b) => b.count - a.count);

    // Group failed job error reasons
    const errorReasons: Record<string, number> = {};
    failedJobs.forEach((j: any) => {
      const reason = j.errorReason || j.errorMessage || 'Unknown failure';
      errorReasons[reason] = (errorReasons[reason] || 0) + 1;
    });
    const errorChart = Object.entries(errorReasons)
      .map(([reason, count]) => ({ reason, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 6);

    return {
      totalJobs,
      completedCount: completedJobs.length,
      failedCount: failedJobs.length,
      extractedItems,
      approvedItems,
      rejectedItems,
      medianMinutes,
      correctionTotal: correctionEvents.length,
      correctionChart,
      errorChart,
    };
  }, [jobs, analytics]);

  return (
    <div className="flex-1 flex flex-col h-full max-w-6xl mx-auto w-full">
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
          <ScanLine className="w-6 h-6 text-indigo-600" /> Extraction & Review Operations
        </h1>
        <p className="text-slate-500 mt-1 text-sm">
          Measured operational metrics from backend extraction jobs and reviewer corrections (demo records excluded).
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between h-32">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1">
            <Zap className="w-4 h-4 text-indigo-500" /> Jobs Processed
          </span>
          <div className="mt-4 flex items-end justify-between">
            <span className="text-3xl font-bold text-slate-900">{loading ? '—' : metrics.totalJobs}</span>
            <span className="text-xs text-slate-500">{metrics.failedCount} failed</span>
          </div>
        </div>

        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between h-32">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1">
            <CheckCircle2 className="w-4 h-4 text-emerald-500" /> Offers Approved
          </span>
          <div className="mt-4 flex items-end justify-between">
            <span className="text-3xl font-bold text-slate-900">{loading ? '—' : metrics.approvedItems}</span>
            <span className="text-xs text-slate-500">
              {metrics.extractedItems} extracted / {metrics.rejectedItems} rejected
            </span>
          </div>
        </div>

        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between h-32">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1">
            <Clock className="w-4 h-4 text-blue-500" /> Median Review Time
          </span>
          <div className="mt-4 flex items-end justify-between">
            <span className="text-3xl font-bold text-slate-900">
              {loading ? '—' : metrics.medianMinutes !== null ? `${metrics.medianMinutes.toFixed(1)}m` : 'N/A'}
            </span>
            <span className="text-xs text-slate-400">Upload → Approval</span>
          </div>
        </div>

        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between h-32">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-widest flex items-center gap-1">
            <AlertTriangle className="w-4 h-4 text-orange-500" /> Manual Corrections
          </span>
          <div className="mt-4 flex items-end justify-between">
            <span className="text-3xl font-bold text-slate-900">{loading ? '—' : metrics.correctionTotal}</span>
            <span className="text-xs text-slate-400">Verified reviewer edits</span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white border border-slate-200 p-6 rounded-3xl shadow-sm min-h-[340px] flex flex-col">
          <h3 className="font-bold text-slate-900 mb-4 flex items-center gap-2">
            <Target className="w-4 h-4 text-indigo-500" /> Corrected Fields Breakdown
          </h3>
          {metrics.correctionChart.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center text-center text-slate-500 p-8">
              <Info className="w-8 h-8 text-slate-300 mb-2" />
              <p className="font-semibold text-slate-700">No verified data yet.</p>
              <p className="text-xs mt-1">Reviewer edits recorded via `manual_correction` events will appear here.</p>
            </div>
          ) : (
            <div className="flex-1 min-h-[260px]">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={metrics.correctionChart} margin={{ top: 0, right: 20, left: 0, bottom: 20 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis dataKey="type" axisLine={false} tickLine={false} tick={{ fill: '#475569', fontSize: 12 }} />
                  <YAxis allowDecimals={false} axisLine={false} tickLine={false} tick={{ fill: '#64748b', fontSize: 12 }} />
                  <Tooltip />
                  <Bar dataKey="count" fill="#f59e0b" radius={[6, 6, 0, 0]} name="Corrections" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        <div className="bg-white border border-slate-200 p-6 rounded-3xl shadow-sm min-h-[340px] flex flex-col">
          <h3 className="font-bold text-slate-900 mb-4 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-red-500" /> Failed Job Reasons
          </h3>
          {metrics.errorChart.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center text-center text-slate-500 p-8">
              <CheckCircle2 className="w-8 h-8 text-emerald-300 mb-2" />
              <p className="font-semibold text-slate-700">No failed extraction jobs recorded.</p>
              <p className="text-xs mt-1">Job failures and retry diagnostics will appear here when encountered.</p>
            </div>
          ) : (
            <div className="flex-1 min-h-[260px]">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={metrics.errorChart} margin={{ top: 0, right: 20, left: 0, bottom: 20 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis dataKey="reason" axisLine={false} tickLine={false} tick={{ fill: '#475569', fontSize: 11 }} />
                  <YAxis allowDecimals={false} axisLine={false} tickLine={false} tick={{ fill: '#64748b', fontSize: 12 }} />
                  <Tooltip />
                  <Bar dataKey="count" fill="#ef4444" radius={[6, 6, 0, 0]} name="Failed Jobs" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
