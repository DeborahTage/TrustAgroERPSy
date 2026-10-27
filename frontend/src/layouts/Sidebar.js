import React, { useState } from 'react';
import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import { hasAnyPermissionForUser, hasPermissionForUser } from '../lib/permissions';
import { getRoleMenus } from '../lib/roleMenus';
import { LogOut, Leaf, PanelLeftClose, PanelLeft } from 'lucide-react';
import { cn } from '../lib/utils';
import { ScrollArea } from '../components/ui/scroll-area';

const Sidebar = ({ show, collapsed, onToggleCollapse, onHide }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { logout, user } = useAuth();
  const { t } = useLanguage();
  const [expanded, setExpanded] = useState({});

  const menuSections = getRoleMenus(user?.role);

  const isItemVisible = (item) => {
    if (item.alwaysVisible) return true;
    if (item.permission && !hasPermissionForUser(user, item.permission)) return false;
    if (item.permissions?.length && !hasAnyPermissionForUser(user, item.permissions)) return false;
    return true;
  };

  const handleLogout = () => {
    logout();
    if (onHide) onHide();
    navigate('/login', { replace: true });
  };

  const renderNavItem = (item) => {
    const Icon = item.icon;
    const label = item.labelKey ? t(`sidebar.${item.labelKey}`) : item.label;
    const hasChildren = item.children?.length > 0;
    const isActive = location.pathname === item.path || location.pathname.startsWith(`${item.path}/`);
    const isExpanded = expanded[item.path] || (hasChildren && location.pathname.startsWith(item.path));

    if (hasChildren) {
      return (
        <div key={item.path} className="space-y-0.5">
          <button
            type="button"
            onClick={() => setExpanded((prev) => ({ ...prev, [item.path]: !prev[item.path] }))}
            className={cn('erp-nav-item w-full', isActive && 'text-foreground font-semibold', collapsed && 'justify-center px-2')}
          >
            {Icon && <Icon className="h-[18px] w-[18px] shrink-0" />}
            {!collapsed && (
              <>
                <span className="flex-1 text-left">{label}</span>
                <span className={cn('text-xs transition-transform', isExpanded && 'rotate-90')}>›</span>
              </>
            )}
          </button>
          {!collapsed && isExpanded && (
            <div className="ml-8 space-y-0.5 border-l border-border/60 pl-3">
              {item.children.filter(isItemVisible).map((child) => (
                <NavLink
                  key={child.path}
                  to={child.path}
                  onClick={onHide}
                  className={({ isActive: active }) =>
                    cn(
                      'block rounded-lg px-2.5 py-1.5 text-xs font-medium transition-colors',
                      active ? 'text-primary bg-accent' : 'text-muted-foreground hover:text-foreground hover:bg-accent/60'
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
    }

    return (
      <div key={item.path} title={collapsed ? label : undefined}>
        <NavLink
          to={item.path}
          onClick={onHide}
          className={({ isActive: active }) =>
            cn('erp-nav-item w-full', active && 'erp-nav-active', collapsed && 'justify-center px-2')
          }
        >
          {Icon && <Icon className="h-[18px] w-[18px] shrink-0" />}
          {!collapsed && <span>{label}</span>}
        </NavLink>
      </div>
    );
  };

  const sidebarContent = (
    <div
      className={cn(
        'flex h-full flex-col border-r border-border/60 bg-card transition-all duration-300',
        collapsed ? 'w-[4.5rem]' : 'w-64'
      )}
    >
      <div className={cn('flex h-16 items-center border-b border-border/60', collapsed ? 'justify-center px-2' : 'justify-between px-4')}>
        <button
          type="button"
          onClick={() => (collapsed ? onToggleCollapse?.() : navigate('/dashboard'))}
          className="flex items-center gap-3 group min-w-0"
          title={collapsed ? 'Expand sidebar' : 'Dashboard'}
        >
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-sm shadow-primary/25 transition-transform group-hover:scale-105 dark:bg-primary/20 dark:text-primary dark:border dark:border-primary/30">
            <Leaf className="h-5 w-5" />
          </div>
          {!collapsed && (
            <div className="text-left">
              <div className="text-sm font-bold text-foreground leading-tight">Trust Agro</div>
              <div className="text-[10px] font-medium text-muted-foreground tracking-wide">Enterprise ERP</div>
            </div>
          )}
        </button>

        {!collapsed && (
          <button
            type="button"
            onClick={onToggleCollapse}
            title="Collapse sidebar"
            className="hidden lg:flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted/80 transition-colors"
          >
            <PanelLeftClose className="h-4 w-4" />
          </button>
        )}
      </div>

      <ScrollArea className="flex-1 px-3 py-4">
        <div className="space-y-6">
          {menuSections.map((section) => {
            const visibleItems = section.items.filter(isItemVisible);
            if (visibleItems.length === 0) return null;
            return (
              <div key={section.label} className="space-y-1.5">
                {!collapsed && <p className="erp-section-label">{section.label}</p>}
                <div className="space-y-0.5">{visibleItems.map(renderNavItem)}</div>
              </div>
            );
          })}
        </div>
      </ScrollArea>

      <div className="border-t border-border/60 p-3 space-y-2">
        {collapsed && (
          <button
            type="button"
            onClick={onToggleCollapse}
            title="Expand sidebar"
            className="hidden lg:flex erp-nav-item w-full text-muted-foreground justify-center px-2"
          >
            <PanelLeft className="h-4 w-4" />
          </button>
        )}

        <div className={cn('flex items-center gap-2 rounded-xl bg-muted/50 p-2', collapsed && 'justify-center')}>
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary text-xs font-bold text-primary-foreground dark:bg-primary/20 dark:text-primary dark:border dark:border-primary/25">
            {user?.fullName?.charAt(0).toUpperCase() || 'U'}
          </div>
          {!collapsed && (
            <>
              <div className="min-w-0 flex-1">
                <div className="truncate text-xs font-semibold text-foreground">{user?.fullName || 'User'}</div>
                <div className="truncate text-[10px] font-medium text-muted-foreground">
                  {user?.role?.replace(/_/g, ' ') || 'Role'}
                </div>
              </div>
              <button
                type="button"
                onClick={handleLogout}
                className="flex h-8 w-8 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive"
              >
                <LogOut className="h-4 w-4" />
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );

  return (
    <>
      <div className="fixed left-0 top-0 z-40 hidden h-screen lg:block">{sidebarContent}</div>
      {show && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div className="fixed inset-0 bg-foreground/20 backdrop-blur-sm" onClick={onHide} />
          <div className="fixed left-0 top-0 h-full animate-in slide-in-from-left duration-300">{sidebarContent}</div>
        </div>
      )}
    </>
  );
};

export default Sidebar;
