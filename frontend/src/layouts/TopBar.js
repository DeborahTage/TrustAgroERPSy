import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { useLocation } from 'react-router-dom';
import { notificationApi } from '../api';
import { useLanguage } from '../context/LanguageContext';
import { useAuth } from '../context/AuthContext';
import { PERMISSIONS } from '../lib/permissions';
import { formatDate } from '../utils';
import {
  Bell,
  Search,
  Globe,
  ChevronRight,
  Sun,
  Moon,
  Plus,
  CheckCheck,
} from 'lucide-react';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '../components/ui/dialog';
import { Skeleton } from '../components/ui/skeleton';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../components/ui/dropdown-menu';
import { cn } from '../lib/utils';

const NOTIFICATION_POLL_MS = 60000;

const ROUTE_LABELS = {
  dashboard: 'Dashboard',
  users: 'Users',
  farms: 'Farms',
  flocks: 'Flocks',
  'daily-records': 'Daily Records',
  inventory: 'Inventory',
  veterinary: 'Veterinary',
  pharmacy: 'Pharmacy',
  finance: 'Finance',
  training: 'Training',
  crm: 'CRM',
  reports: 'Reports',
  settings: 'Settings',
};

const TopBar = () => {
  const location = useLocation();
  const { language } = useLanguage();
  const { user, hasPermission } = useAuth();
  const canViewNotifications = hasPermission(PERMISSIONS.VIEW_NOTIFICATIONS);
  const [unread, setUnread] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const [loadingNotifications, setLoadingNotifications] = useState(false);
  const [darkMode, setDarkMode] = useState(() => document.documentElement.classList.contains('dark'));

  const breadcrumbs = useMemo(() => {
    const segments = location.pathname.split('/').filter(Boolean);
    if (segments.length === 0) return ['Dashboard'];
    return segments.map((seg) => ROUTE_LABELS[seg] || seg.replace(/-/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()));
  }, [location.pathname]);

  const loadNotifications = useCallback(async (silent = false) => {
    if (!canViewNotifications) return;
    if (!silent) setLoadingNotifications(true);
    try {
      const res = await notificationApi.getAll();
      const rows = res.data.data || [];
      setNotifications(rows);
      setUnread(rows.filter((row) => !row.read).length);
    } catch {
      setNotifications([]);
      setUnread(0);
    } finally {
      if (!silent) setLoadingNotifications(false);
    }
  }, [canViewNotifications]);

  const handleNotificationClick = async (notification) => {
    if (!notification.read) {
      try {
        await notificationApi.markRead(notification.id);
        setNotifications((prev) =>
          prev.map((n) => (n.id === notification.id ? { ...n, read: true } : n))
        );
        setUnread((c) => Math.max(0, c - 1));
      } catch {
        // ignore
      }
    }
  };

  const markAllRead = async () => {
    const unreadItems = notifications.filter((n) => !n.read);
    await Promise.all(unreadItems.map((n) => notificationApi.markRead(n.id).catch(() => null)));
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    setUnread(0);
  };

  const toggleTheme = () => {
    const next = !darkMode;
    setDarkMode(next);
    document.documentElement.classList.toggle('dark', next);
    localStorage.setItem('trust-theme', next ? 'dark' : 'light');
  };

  useEffect(() => {
    const saved = localStorage.getItem('trust-theme');
    if (saved === 'dark') {
      document.documentElement.classList.add('dark');
      setDarkMode(true);
    }
  }, []);

  useEffect(() => {
    loadNotifications();
    if (!canViewNotifications) return undefined;
    const interval = setInterval(() => loadNotifications(true), NOTIFICATION_POLL_MS);
    return () => clearInterval(interval);
  }, [loadNotifications, canViewNotifications]);

  const groupedNotifications = useMemo(() => {
    const groups = {};
    notifications.forEach((n) => {
      const key = n.module || n.type || 'General';
      if (!groups[key]) groups[key] = [];
      groups[key].push(n);
    });
    return groups;
  }, [notifications]);

  return (
    <>
      <div className="flex flex-1 items-center gap-4 min-w-0">
        <nav className="hidden xl:flex items-center gap-1 text-xs text-muted-foreground shrink-0">
          {breadcrumbs.map((crumb, i) => (
            <React.Fragment key={`${crumb}-${i}`}>
              {i > 0 && <ChevronRight className="h-3 w-3" />}
              <span className={cn(i === breadcrumbs.length - 1 && 'font-semibold text-foreground')}>
                {crumb}
              </span>
            </React.Fragment>
          ))}
        </nav>

        <div className="hidden md:flex flex-1 max-w-xl items-center gap-2 h-9 px-3 rounded-xl border border-border/60 bg-muted/40 text-muted-foreground transition-colors hover:bg-muted/60 cursor-pointer">
          <Search className="h-4 w-4 shrink-0" />
          <span className="text-sm flex-1 truncate">Search modules, records, actions…</span>
          <kbd className="hidden lg:inline text-[10px] font-medium border border-border rounded px-1.5 py-0.5 bg-card text-muted-foreground">
            Ctrl+K
          </kbd>
        </div>
      </div>

      <div className="flex items-center gap-1.5 sm:gap-2 shrink-0">
        <Button variant="ghost" size="icon" className="hidden sm:flex h-9 w-9 text-muted-foreground" title="Quick action">
          <Plus className="h-4 w-4" />
        </Button>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="sm" className="hidden sm:flex h-9 gap-1.5 text-muted-foreground px-2.5">
              <Globe className="h-4 w-4" />
              <span className="text-xs font-medium">{language === 'am' ? 'አማ' : 'EN'}</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-40">
            <DropdownMenuLabel>Language</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem className="font-medium">English</DropdownMenuItem>
            <DropdownMenuItem disabled>Amharic (soon)</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>

        <Button
          variant="ghost"
          size="icon"
          className="h-9 w-9 text-muted-foreground"
          onClick={toggleTheme}
          title="Toggle theme"
        >
          {darkMode ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
        </Button>

        {canViewNotifications && (
          <Button
            variant="ghost"
            size="icon"
            className="relative h-9 w-9 text-muted-foreground"
            onClick={() => setShowNotifications(true)}
          >
            <Bell className="h-4 w-4" />
            {unread > 0 && (
              <span className="absolute top-1.5 right-1.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[9px] font-bold text-destructive-foreground">
                {unread > 9 ? '9+' : unread}
              </span>
            )}
          </Button>
        )}

        <div className="hidden sm:flex items-center gap-2 pl-2 ml-1 border-l border-border/60">
          <div className="text-right hidden lg:block">
            <p className="text-xs font-semibold text-foreground leading-tight truncate max-w-[120px]">
              {user?.fullName || 'User'}
            </p>
            <Badge variant="secondary" className="mt-0.5 h-4 px-1.5 text-[9px] font-semibold uppercase tracking-wide">
              {user?.role?.replace(/_/g, ' ') || 'Role'}
            </Badge>
          </div>
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 text-xs font-bold text-primary">
            {user?.fullName?.charAt(0).toUpperCase()}
          </div>
        </div>
      </div>

      <Dialog open={showNotifications} onOpenChange={setShowNotifications}>
        <DialogContent className="sm:max-w-md rounded-2xl">
          <DialogHeader className="flex flex-row items-center justify-between space-y-0 pb-4 border-b border-border">
            <DialogTitle className="flex items-center gap-2 text-base font-semibold">
              <Bell className="h-4 w-4 text-primary" />
              Notifications
              {unread > 0 && (
                <Badge className="bg-primary/10 text-primary hover:bg-primary/10 border-0">
                  {unread} new
                </Badge>
              )}
            </DialogTitle>
            {unread > 0 && (
              <Button variant="ghost" size="sm" className="h-8 text-xs" onClick={markAllRead}>
                <CheckCheck className="h-3.5 w-3.5 mr-1" />
                Mark all read
              </Button>
            )}
          </DialogHeader>

          <div className="max-h-[420px] overflow-y-auto py-2">
            {loadingNotifications ? (
              <div className="space-y-3">
                {[1, 2, 3].map((i) => (
                  <Skeleton key={i} className="h-16 w-full rounded-xl" />
                ))}
              </div>
            ) : notifications.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 text-center text-muted-foreground">
                <Bell className="h-10 w-10 mb-2 opacity-40" />
                <p className="text-sm font-medium">No notifications yet</p>
              </div>
            ) : (
              <div className="space-y-4">
                {Object.entries(groupedNotifications).map(([module, items]) => (
                  <div key={module}>
                    <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mb-2 px-1">
                      {module}
                    </p>
                    <div className="space-y-2">
                      {items.map((notification) => (
                        <button
                          key={notification.id}
                          type="button"
                          onClick={() => handleNotificationClick(notification)}
                          className={cn(
                            'w-full text-left p-3 rounded-xl border transition-all',
                            notification.read
                              ? 'bg-muted/30 border-border/50'
                              : 'bg-card border-primary/20 shadow-soft'
                          )}
                        >
                          <p className="text-sm font-medium text-foreground mb-0.5">{notification.title}</p>
                          <p className="text-xs text-muted-foreground line-clamp-2">{notification.message}</p>
                          <p className="text-[10px] text-muted-foreground mt-2">{formatDate(notification.createdAt)}</p>
                        </button>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
};

export default TopBar;
