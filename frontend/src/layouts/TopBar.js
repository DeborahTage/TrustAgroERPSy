import React, { useEffect, useState, useCallback } from 'react';
import { notificationApi } from '../api';
import { useLanguage } from '../context/LanguageContext';
import { useAuth } from '../context/AuthContext';
import { PERMISSIONS } from '../lib/permissions';
import { formatDate } from '../utils';
import { Bell, Search, Globe, Check } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '../components/ui/dialog';
import { Skeleton } from '../components/ui/skeleton';
import { cn } from '../lib/utils';

const NOTIFICATION_POLL_MS = 60000;

const TopBar = () => {
  const { t, language } = useLanguage();
  const { user, hasPermission } = useAuth();
  const canViewNotifications = hasPermission(PERMISSIONS.VIEW_NOTIFICATIONS);
  const [unread, setUnread] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const [loadingNotifications, setLoadingNotifications] = useState(false);

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

  useEffect(() => {
    loadNotifications();
    if (!canViewNotifications) return undefined;
    const interval = setInterval(() => loadNotifications(true), NOTIFICATION_POLL_MS);
    return () => clearInterval(interval);
  }, [loadNotifications, canViewNotifications]);

  return (
    <div className="flex flex-1 items-center justify-between">
      <div className="flex items-center gap-4">
        <div className="hidden md:flex items-center gap-2 px-3 py-1.5 bg-gray-100 rounded-full text-[11px] font-bold text-gray-500 uppercase tracking-wider">
          <Globe className="h-3 w-3" />
          <span>{language === 'am' ? 'Amharic' : 'English'}</span>
        </div>
      </div>

      <div className="flex items-center gap-2">
        {/* Search Placeholder */}
        <div className="hidden sm:flex items-center gap-2 h-9 px-3 bg-gray-100/50 rounded-lg text-gray-400 group cursor-pointer hover:bg-gray-100 transition-all">
          <Search className="h-4 w-4" />
          <span className="text-xs font-medium pr-8">Search anything...</span>
          <span className="text-[10px] font-bold border border-gray-200 px-1 rounded bg-white">⌘K</span>
        </div>

        {canViewNotifications && (
          <Button
            variant="ghost"
            size="icon"
            className="relative h-10 w-10 rounded-xl hover:bg-indigo-50 hover:text-indigo-600 transition-all"
            onClick={() => setShowNotifications(true)}
          >
            <Bell className="h-5 w-5" />
            {unread > 0 && (
              <span className="absolute top-2 right-2 h-2.5 w-2.5 bg-rose-500 border-2 border-white rounded-full animate-pulse" />
            )}
          </Button>
        )}

        {/* User Context */}
        <div className="flex items-center gap-2 pl-2 border-l border-gray-100 ml-2">
          <div className="flex flex-col items-end hidden sm:flex">
            <span className="text-[11px] font-black text-gray-900 leading-tight uppercase">{user?.fullName || 'User'}</span>
            <span className="text-[9px] font-bold text-indigo-500 uppercase tracking-widest">{user?.role?.replace('_', ' ')}</span>
          </div>
          <div className="h-9 w-9 rounded-xl bg-gray-100 flex items-center justify-center font-black text-xs text-gray-500">
            {user?.fullName?.charAt(0).toUpperCase()}
          </div>
        </div>
      </div>

      <Dialog open={showNotifications} onOpenChange={setShowNotifications}>
        <DialogContent className="sm:max-w-md bg-white/95 backdrop-blur-md rounded-2xl border-none shadow-2xl">
          <DialogHeader className="pb-4 border-b border-gray-100">
            <DialogTitle className="flex items-center gap-2 text-gray-900 font-black uppercase tracking-tight text-lg">
              <Bell className="h-5 w-5 text-indigo-600" />
              Notifications
              {unread > 0 && <Badge variant="destructive" className="ml-2 bg-indigo-100 text-indigo-600 hover:bg-indigo-100 border-none font-black">{unread} New</Badge>}
            </DialogTitle>
          </DialogHeader>

          <div className="max-h-[450px] overflow-y-auto pr-1 py-4">
            {loadingNotifications ? (
              <div className="space-y-4">
                {[1, 2, 3].map((i) => (
                  <Skeleton key={i} className="h-20 w-full rounded-xl" />
                ))}
              </div>
            ) : notifications.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 text-center opacity-40">
                <Bell className="h-12 w-12 mb-3" />
                <p className="text-sm font-bold uppercase tracking-widest">Inbox is empty</p>
              </div>
            ) : (
              <div className="space-y-3">
                {notifications.map((notification) => (
                  <div
                    key={notification.id}
                    className={cn(
                      "p-4 rounded-2xl border transition-all cursor-pointer group",
                      notification.read ? 'bg-gray-50/50 border-gray-100' : 'bg-white border-indigo-100 shadow-sm'
                    )}
                  >
                    <div className="flex items-start gap-4">
                      <div className={cn(
                        "h-10 w-10 rounded-xl flex items-center justify-center shrink-0",
                        notification.read ? "bg-gray-100" : "bg-indigo-50"
                      )}>
                        <Bell className={cn("h-5 w-5", notification.read ? "text-gray-400" : "text-indigo-600")} />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="font-bold text-sm text-gray-900 mb-1 group-hover:text-indigo-600 transition-colors">{notification.title}</div>
                        <div className="text-xs text-gray-500 font-medium line-clamp-2 leading-relaxed">{notification.message}</div>
                        <div className="flex items-center gap-2 mt-3">
                          <span className="text-[10px] font-black text-gray-400 uppercase tracking-tighter">{formatDate(notification.createdAt)}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default TopBar;
