import React, { useState } from 'react';
import { Store, ShieldCheck, Mail, Lock, AlertCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export default function Login() {
  const { loginWithGoogle, loginWithEmail, user } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  React.useEffect(() => {
    if (user.role) {
      navigate('/dashboard');
    }
  }, [user, navigate]);

  const formatLoginError = (err: any, fallback: string) => {
    const code = err?.code || '';
    const msg = String(err?.message || '');
    if (code === 'auth/network-request-failed' || msg.includes('auth/network-request-failed')) {
      return 'Network unavailable. Check your internet connection and try again.';
    }
    if (code === 'auth/invalid-credential' || code === 'auth/wrong-password' || code === 'auth/user-not-found' || msg.includes('auth/invalid-credential')) {
      return 'Invalid email or password.';
    }
    if (code === 'auth/too-many-requests' || msg.includes('auth/too-many-requests')) {
      return 'Too many failed attempts. Please wait a moment and try again.';
    }
    if (msg.toLowerCase().includes('cancel')) {
      return 'Sign-in was cancelled.';
    }
    return msg || fallback;
  };

  const handleEmailLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password) {
      setError('Please enter both email and password.');
      return;
    }
    setError(null);
    setLoading(true);
    try {
      await loginWithEmail(email.trim(), password);
    } catch (err: any) {
      setError(formatLoginError(err, 'Login failed. Please check credentials.'));
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = async () => {
    setError(null);
    setLoading(true);
    try {
      await loginWithGoogle();
    } catch (err: any) {
      setError(formatLoginError(err, 'Google sign in failed.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center items-center p-4">
      <div className="w-full max-w-md bg-white rounded-3xl shadow-xl overflow-hidden border border-slate-200">
        <div className="p-8 text-center bg-indigo-600 text-white">
          <div className="w-16 h-16 bg-white/20 rounded-2xl flex items-center justify-center mx-auto mb-4 backdrop-blur-sm">
            <Store className="w-8 h-8 font-bold" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight">Deal Finder UAE</h1>
          <p className="text-indigo-200 text-sm mt-1">Platform Management Portal</p>
        </div>

        <div className="p-8">
          {error && (
            <div className="mb-4 p-3 rounded-xl bg-red-50 border border-red-200 flex items-start gap-2 text-xs text-red-600">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleEmailLogin} className="space-y-4 mb-6">
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">Email</label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" />
                <input
                  type="email"
                  required
                  placeholder="admin@dealfinder.ae"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-800"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">Password</label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" />
                <input
                  type="password"
                  required
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-800"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 px-4 bg-slate-900 text-white rounded-xl font-bold text-sm hover:bg-slate-800 transition-colors shadow-md active:scale-[0.98] disabled:opacity-50"
            >
              {loading ? 'Signing in...' : 'Sign in with Email'}
            </button>
          </form>

          <div className="relative flex py-2 items-center mb-6">
            <div className="flex-grow border-t border-slate-200"></div>
            <span className="flex-shrink mx-4 text-xs text-slate-400 font-medium">or</span>
            <div className="flex-grow border-t border-slate-200"></div>
          </div>

          <button
            onClick={handleGoogleLogin}
            disabled={loading}
            className="w-full py-3.5 px-4 bg-indigo-600 text-white rounded-xl font-bold text-sm hover:bg-indigo-700 transition-colors shadow-md shadow-indigo-600/20 active:scale-[0.98] flex items-center justify-center gap-2 disabled:opacity-50"
          >
            Sign in with Google
          </button>
          
          <div className="mt-8 flex flex-col items-center justify-center gap-2 text-xs text-slate-400 font-medium">
            <div className="flex items-center gap-2"><ShieldCheck className="w-4 h-4" /> Secure Business Portal</div>
            <p className="text-center mt-2 max-w-[250px]">
              Access requires authorization. New identities will be provisioned with limited scope by default.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
