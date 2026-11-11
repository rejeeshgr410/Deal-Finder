import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { LayoutDashboard, FileText, ScanLine, Package, Store, Bell, Users, Settings, LogOut, Search, Tag, Building2, User as UserIcon, ShieldCheck, BarChart2, ShoppingCart } from 'lucide-react';
import { useState } from 'react';
import { NotificationCenter } from './NotificationCenter';
import { useAuth } from '../contexts/AuthContext';

export default function Layout() {
  const [sidebarOpen, setSidebarOpen] = useState(window.innerWidth > 768);
  const [showNotifications, setShowNotifications] = useState(false);
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const { user, logout, isAdminUser, isModeratorTeam, isRetailerUser } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navItems = isModeratorTeam ? [
    { icon: LayoutDashboard, label: 'Dashboard', path: '/dashboard' },
    { icon: ShoppingCart, label: 'Consumer App (B2C)', path: '/consumer' },
    ...(isAdminUser ? [
      { icon: BarChart2, label: 'Platform Analytics', path: '/admin-analytics' },
      { icon: ScanLine, label: 'OCR Analytics', path: '/ocr-analytics' }
    ] : []),
    ...(user.role !== 'data_entry' ? [
      { icon: FileText, label: 'Flyer Management', path: '/flyers' }
    ] : []),
    { icon: ScanLine, label: 'OCR Center', path: '/ocr' },
    { icon: Package, label: 'Products', path: '/products' },
    ...(user.role !== 'data_entry' ? [
      { icon: Tag, label: 'Promotions', path: '/promotions' },
      { icon: Store, label: 'Retailers', path: '/retailers' }
    ] : []),
    ...(isAdminUser ? [
      { icon: Bell, label: 'Notifications', path: '/notifications' },
      { icon: Users, label: 'Users', path: '/users' },
      { icon: ShieldCheck, label: 'Audit Logs', path: '/audit-logs' }
    ] : [])
  ] : [
    { icon: LayoutDashboard, label: 'Dashboard', path: '/dashboard' },
    { icon: ShoppingCart, label: 'Consumer App (B2C)', path: '/consumer' },
    { icon: BarChart2, label: 'Analytics', path: '/retailer-analytics' },
    { icon: FileText, label: 'My Flyers', path: '/flyers' },
    { icon: Package, label: 'My Products', path: '/products' },
    { icon: Tag, label: 'My Promotions', path: '/promotions' },
    { icon: UserIcon, label: 'Manage Account', path: '/account' },
  ];

  const userInitials = isAdminUser ? 'AD' : (user.email ? user.email.substring(0, 2).toUpperCase() : 'RT');
  const userRoleStr = isAdminUser ? 'Admin User' : (isModeratorTeam ? 'Moderator' : 'Retailer Partner');

  return (
    <div className="h-screen bg-slate-50 flex text-slate-900 font-sans overflow-hidden">
      {/* Mobile Overlay */}
      {sidebarOpen && (
        <div 
          className="fixed inset-0 bg-slate-900/50 z-40 md:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside 
        className={`fixed md:relative z-50 bg-white border-r border-slate-200 transition-all duration-300 h-full flex flex-col
        ${sidebarOpen ? 'translate-x-0 w-64' : '-translate-x-full md:translate-x-0 md:w-20'}
        `}
      >
        <div className="h-16 flex items-center justify-between md:justify-center border-b border-slate-100 px-4">
          <div className="flex items-center gap-3 overflow-hidden whitespace-nowrap">
            <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center text-white font-bold shrink-0">
              D
            </div>
            {(sidebarOpen || window.innerWidth > 768) && (
               <span className={`font-bold text-lg tracking-tight text-slate-900 ${!sidebarOpen && 'md:hidden'}`}>DF-UAE <span className="text-indigo-600">Biz</span></span>
            )}
          </div>
          <button className="md:hidden p-1 text-slate-500" onClick={() => setSidebarOpen(false)}>
             <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>
        
        <nav className="flex-1 overflow-y-auto p-4 flex flex-col gap-1">
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              onClick={() => {
                if (window.innerWidth < 768) setSidebarOpen(false);
              }}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2 rounded-lg transition-colors whitespace-nowrap text-sm ${
                  isActive 
                    ? 'bg-indigo-50 text-indigo-700 font-medium' 
                    : 'text-slate-500 hover:bg-slate-50 font-medium'
                }`
              }
            >
              <item.icon className="h-4 w-4 shrink-0" />
              <span className={!sidebarOpen ? 'md:hidden' : ''}>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        
        <div className="p-4 border-t border-slate-100">
          <div className={`bg-slate-900 rounded-xl p-4 text-white transition-all duration-300 overflow-hidden ${sidebarOpen ? 'block' : 'hidden'}`}>
            <p className="text-[10px] uppercase tracking-wider opacity-60 mb-1">Current Role</p>
            <p className="text-sm font-semibold">{user.role}</p>
            <p className="text-xs opacity-80 mt-2">{userRoleStr}</p>
          </div>
           {!sidebarOpen && (
             <button onClick={handleLogout} className="hidden md:flex items-center justify-center w-full px-2 py-2 text-slate-500 hover:text-slate-700 rounded-lg transition-colors" title="Sign Out">
               <LogOut className="h-5 w-5 shrink-0" />
             </button>
           )}
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Header */}
        <header className="h-16 bg-white border-b border-slate-200 px-4 md:px-6 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-4 flex-1 min-w-0">
            <button 
              onClick={() => setSidebarOpen(!sidebarOpen)}
              className="p-1 -ml-2 rounded-md hover:bg-slate-50 text-slate-500 shrink-0"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" /></svg>
            </button>
            <div className="max-w-md w-full relative hidden sm:block">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400 pointer-events-none" />
              <input 
                type="text" 
                placeholder="Search flyers, products, or retailers..." 
                className="w-full pl-9 pr-4 py-1.5 bg-slate-100 border-none rounded-full focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all text-xs"
              />
            </div>
          </div>
          <div className="flex items-center gap-4 md:gap-6 shrink-0 relative">
             <button 
               onClick={() => { setShowNotifications(!showNotifications); setShowProfileMenu(false); }}
               className="relative p-1 hover:text-indigo-600 text-slate-500 transition-colors focus:outline-none"
             >
               <Bell className="h-4 w-4" />
               <span className="absolute top-0.5 right-0.5 w-1.5 h-1.5 bg-red-500 rounded-full"></span>
             </button>
             
             {showNotifications && (
               <>
                 <div className="fixed inset-0 z-40" onClick={() => setShowNotifications(false)}></div>
                 <NotificationCenter onClose={() => setShowNotifications(false)} />
               </>
             )}

             <div 
               className="flex items-center gap-3 pl-4 border-l border-slate-200 cursor-pointer hover:opacity-80 transition-opacity"
               onClick={() => { setShowProfileMenu(!showProfileMenu); setShowNotifications(false); }}
             >
                <div className="text-right hidden md:block">
                  <div className="text-xs font-bold text-slate-900 leading-tight">{userRoleStr}</div>
                  <div className="text-[10px] text-slate-500 leading-tight">{user.role}</div>
                </div>
                <div className="w-8 h-8 md:w-9 md:h-9 rounded-full bg-slate-200 flex items-center justify-center font-bold text-sm text-slate-600">
                  {userInitials}
                </div>
             </div>
             
             {showProfileMenu && (
               <>
                 <div className="fixed inset-0 z-40" onClick={() => setShowProfileMenu(false)}></div>
                 <div className="absolute top-14 right-2 sm:right-6 w-56 bg-white rounded-xl shadow-lg border border-slate-200 overflow-hidden z-50 animate-in fade-in slide-in-from-top-2">
                   <div className="p-4 border-b border-slate-100 flex items-center gap-3 bg-slate-50/50">
                     <div className="w-10 h-10 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold text-sm shrink-0">{userInitials}</div>
                     <div className="min-w-0">
                       <p className="font-bold text-sm text-slate-800 uppercase tracking-tight truncate">{userRoleStr}</p>
                       <p className="text-xs text-slate-500 truncate">{user.email || 'user@example.com'}</p>
                     </div>
                   </div>
                   <div className="p-2 space-y-1">
                     {isRetailerUser && (
                       <button onClick={() => { navigate('/account'); setShowProfileMenu(false); }} className="w-full text-left px-3 py-2 text-sm text-slate-700 hover:bg-slate-50 rounded-lg flex items-center gap-2 font-medium">
                         <Settings className="w-4 h-4 text-slate-400" /> Manage Account
                       </button>
                     )}
                     <div className="h-px bg-slate-100 my-1"></div>
                     <button onClick={handleLogout} className="w-full text-left px-3 py-2 text-sm text-red-600 hover:bg-red-50 rounded-lg flex items-center gap-2 font-medium transition-colors">
                       <LogOut className="w-4 h-4" /> Sign Out
                     </button>
                   </div>
                 </div>
               </>
             )}
          </div>
        </header>

        {/* Page Content */}
        <div className="flex-1 overflow-auto p-4 md:p-6">
          <div className="max-w-7xl mx-auto h-full">
            <Outlet />
          </div>
        </div>
      </main>
    </div>
  );
}
