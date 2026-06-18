import React, { useState } from 'react';
import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import { PATH_MODULE_MAP, FORM_ROUTE_PERMISSIONS } from '../lib/routeAccess';
import { hasPermission } from '../lib/permissions';
import {
  LayoutDashboard,
  Users,
  Building2,
  Layers,
  FileText,
  Package,
  HeartPulse,
  Pill,
  DollarSign,
  Users2,
  BarChart3,
  Settings,
  LogOut,
  ChevronDown,
  GraduationCap
} from 'lucide-react';
import { cn } from '../lib/utils';
import { ScrollArea } from '../components/ui/scroll-area';

const menuItems = [
  { path: '/dashboard', labelKey: 'dashboard', icon: LayoutDashboard },
  { path: '/users', labelKey: 'users', icon: Users },
  { path: '/farms', labelKey: 'farms', icon: Building2 },
  { path: '/flocks', labelKey: 'flocks', icon: Layers },
  { path: '/daily-records', labelKey: 'dailyRecords', icon: FileText },
  {
    path: '/inventory', labelKey: 'inventory', icon: Package, children: [
      { path: '/inventory/dashboard', label: 'Dashboard' },
      { path: '/inventory/requisitions', label: 'Requisitions' },
      { path: '/inventory/stock-in', label: 'Receiving (GRN)' },
      { path: '/inventory/stock-out', label: 'Issuance' },
      { path: '/inventory/scan', label: 'Barcode Scan' },
      { path: '/inventory/po', label: 'Purchase Orders' },
      { path: '/inventory/forecast', label: 'Forecasting' },
      { path: '/inventory/reports', label: 'Reports' },
    ]
  },
  { path: '/veterinary', labelKey: 'veterinary', icon: HeartPulse },
  {
    path: '/pharmacy', labelKey: 'pharmacy', icon: Pill, children: [
      { path: '/pharmacy/overview', label: 'Overview' },
      { path: '/pharmacy/process-rx', label: 'Process Rx' },
      { path: '/pharmacy/pos', label: 'POS Sale' },
      { path: '/pharmacy/customers', label: 'Patients' },
      { path: '/pharmacy/reports', label: 'Reports' }
    ]
  },
  {
    path: '/finance', labelKey: 'finance', icon: DollarSign, children: [
      { path: '/finance/dashboard', label: 'Dashboard' },
      { path: '/finance/coa', label: 'Chart of Accounts' },
      { path: '/finance/invoices', label: 'Invoice Center' },
      { path: '/finance/wallets', label: 'Digital Wallets' },
      { path: '/finance/reconciliation', label: 'Cash Audit' },
      { path: '/finance/reports', label: 'Report Center' },
      { path: '/finance/flock-cogs', label: 'Flock COGS' },
    ]
  },
  { path: '/training', labelKey: 'training', icon: GraduationCap },
  { path: '/crm', labelKey: 'crm', icon: Users2 },
  { path: '/reports', labelKey: 'reports', icon: BarChart3 },
  { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
];

const Sidebar = ({ show, onHide }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { canAccessModule, logout, user } = useAuth();
  const { t } = useLanguage();
  const [expanded, setExpanded] = useState({});

  const toggleExpand = (path) => {
    setExpanded(prev => ({ ...prev, [path]: !prev[path] }));
  };

  const visibleItems = menuItems.filter((item) => {
    if (user?.role === 'PHARMACY_SALES') {
      return item.path === '/pharmacy' || item.alwaysVisible || item.path === '/dashboard';
    }
    if (item.alwaysVisible) return true;
    const module = PATH_MODULE_MAP[item.path];
    return canAccessModule(module);
  }).map(item => {
    if (!item.children) return item;
    const visibleChildren = item.children.filter(child => {
      const requiredPermission = FORM_ROUTE_PERMISSIONS[child.path];
      if (!requiredPermission) return true;
      return hasPermission(user?.role, requiredPermission);
    });
    return { ...item, children: visibleChildren.length > 0 ? visibleChildren : null };
  });

  const handleLogout = () => {
    logout();
    if (onHide) onHide();
    navigate('/login', { replace: true });
  };

  const sidebarContent = (
    <div className="flex h-full w-64 flex-col bg-[#0f172a] text-slate-300">
      {/* Brand */}
      <div className="flex h-20 items-center px-6 border-b border-slate-800/50">
        <div className="flex items-center gap-3 group cursor-pointer" onClick={() => navigate('/dashboard')}>
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-600 shadow-lg shadow-indigo-500/20 group-hover:scale-110 transition-transform">
            <Building2 className="h-6 w-6 text-white" />
          </div>
          <div>
            <div className="text-sm font-black text-white leading-tight tracking-tight uppercase">Trust Agro</div>
            <div className="text-[10px] text-indigo-400 font-bold uppercase tracking-widest">ERP System</div>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <ScrollArea className="flex-1 px-3 py-6">
        <div className="space-y-1">
          {visibleItems.map((item) => {
            const Icon = item.icon;
            const isExpanded = expanded[item.path] || location.pathname.startsWith(item.path);
            const hasChildren = !!item.children;

            return (
              <div key={item.path} className="space-y-1">
                {hasChildren ? (
                  <button
                    onClick={() => toggleExpand(item.path)}
                    className={cn(
                      'flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-bold transition-all group',
                      location.pathname.startsWith(item.path)
                        ? 'text-white'
                        : 'text-slate-400 hover:bg-slate-800/50 hover:text-white'
                    )}
                  >
                    <Icon className={cn("h-5 w-5 transition-colors", location.pathname.startsWith(item.path) ? "text-indigo-400" : "group-hover:text-white")} />
                    <span className="flex-1 text-left">{t(`sidebar.${item.labelKey}`)}</span>
                    <ChevronDown className={cn("h-3.5 w-3.5 transition-transform duration-300", isExpanded && "rotate-180")} />
                  </button>
                ) : (
                  <NavLink
                    to={item.path}
                    onClick={onHide}
                    className={({ isActive }) =>
                      cn(
                        'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-bold transition-all group',
                        isActive
                          ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/20'
                          : 'text-slate-400 hover:bg-slate-800/50 hover:text-white'
                      )
                    }
                  >
                    <Icon className="h-5 w-5" />
                    <span>{t(`sidebar.${item.labelKey}`)}</span>
                  </NavLink>
                )}

                {hasChildren && isExpanded && (
                  <div className="ml-9 space-y-1 animate-in slide-in-from-top-2 duration-300">
                    {item.children.map(child => (
                      <NavLink
                        key={child.path}
                        to={child.path}
                        onClick={onHide}
                        className={({ isActive }) =>
                          cn(
                            'block rounded-lg px-3 py-1.5 text-[13px] font-bold transition-all',
                            isActive
                              ? 'text-white'
                              : 'text-slate-500 hover:text-white'
                          )
                        }
                      >
                        {child.label}
                      </NavLink>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </ScrollArea>

      {/* User info */}
      <div className="border-t border-slate-800/50 p-4">
        <div className="flex items-center gap-3 p-2 rounded-xl bg-slate-800/30">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-indigo-600 transition-transform hover:scale-105">
            <span className="text-sm font-black text-white">
              {user?.fullName?.charAt(0).toUpperCase() || 'U'}
            </span>
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-xs font-black text-white truncate uppercase tracking-tighter">
              {user?.fullName || 'User'}
            </div>
            <div className="text-[10px] text-indigo-400 font-bold truncate uppercase tracking-widest">
              {user?.role?.replace('_', ' ') || 'Role'}
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="h-8 w-8 flex items-center justify-center rounded-lg text-slate-500 hover:bg-rose-500/10 hover:text-rose-500 transition-all"
          >
            <LogOut className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );

  return (
    <>
      <div className="hidden lg:flex fixed left-0 top-0 z-50 h-screen">
        {sidebarContent}
      </div>

      {show && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div className="fixed inset-0 bg-[#0f172a]/80 backdrop-blur-sm" onClick={onHide} />
          <div className="fixed left-0 top-0 h-full w-64 animate-in slide-in-from-left duration-300">
            {sidebarContent}
          </div>
        </div>
      )}
    </>
  );
};

export default Sidebar;
