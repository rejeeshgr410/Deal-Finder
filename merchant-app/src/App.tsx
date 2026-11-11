/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Flyers from './pages/Flyers';
import OcrCenter from './pages/OcrCenter';
import OcrReview from './pages/OcrReview';
import Products from './pages/Products';
import Promotions from './pages/Promotions';
import Retailers from './pages/Retailers';
import RetailerAccount from './pages/RetailerAccount';
import RetailerBranches from './pages/RetailerBranches';
import Users from './pages/Users';
import AuditLogs from './pages/AuditLogs';
import Notifications from './pages/Notifications';
import AdminAnalytics from './pages/AdminAnalytics';
import RetailerAnalytics from './pages/RetailerAnalytics';
import OcrAnalytics from './pages/OcrAnalytics';
import Login from './pages/Login';
import ConsumerPortal from './pages/ConsumerPortal';
import { AuthProvider, useAuth } from './contexts/AuthContext';

import { ToastProvider } from './contexts/ToastContext';

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) {
    return <div className="min-h-screen flex items-center justify-center bg-slate-50 font-bold text-slate-500 animate-pulse">Loading Deal Finder UAE...</div>;
  }
  if (!user.role) {
    return <Navigate to="/login" replace />;
  }
  if (user.role === 'pending' || user.role === 'unauthorized') {
    if (window.location.pathname.endsWith('/consumer')) {
      return <>{children}</>;
    }
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50 p-6 text-center">
        <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-200 max-w-md w-full">
          <h2 className="text-2xl font-bold text-slate-900 mb-2">Access Pending</h2>
          <p className="text-slate-500 mb-6">Your account is currently pending approval. Please contact a system administrator to assign a role to your account.</p>
          <div className="h-px bg-slate-150 my-4"></div>
          <p className="text-xs text-slate-400 mb-4">While you wait for B2B authorization, you can still experience the consumer side of Deal Finder UAE below.</p>
          <a
            href="/consumer"
            className="inline-block w-full py-3 px-4 bg-indigo-600 text-white rounded-xl font-bold text-sm hover:bg-indigo-700 transition-colors shadow-md shadow-indigo-600/10"
          >
            Explore B2C Consumer App &rarr;
          </a>
        </div>
      </div>
    );
  }
  return <>{children}</>;
}

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/" element={<PrivateRoute><Layout /></PrivateRoute>}>
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<Dashboard />} />
              <Route path="flyers" element={<Flyers />} />
              <Route path="ocr" element={<OcrCenter />} />
              <Route path="ocr/:jobId" element={<OcrReview />} />
              <Route path="products" element={<Products />} />
              <Route path="promotions" element={<Promotions />} />
              <Route path="retailers" element={<Retailers />} />
              <Route path="users" element={<Users />} />
              <Route path="audit-logs" element={<AuditLogs />} />
              <Route path="notifications" element={<Notifications />} />
              <Route path="admin-analytics" element={<AdminAnalytics />} />
              <Route path="retailer-analytics" element={<RetailerAnalytics />} />
              <Route path="ocr-analytics" element={<OcrAnalytics />} />
              <Route path="account" element={<RetailerAccount />} />
              <Route path="account/branches" element={<RetailerBranches />} />
              <Route path="consumer" element={<ConsumerPortal />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  );
}
