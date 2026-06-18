import React from 'react';
import { cn } from '../../lib/utils';
import { Button } from '../ui/button';
import { Plus, ChevronRight } from 'lucide-react';

const PageHeader = ({
  title,
  subtitle,
  action,
  actionLabel,
  onAction,
  children,
  className,
  breadcrumbs = []
}) => {
  return (
    <div className={cn("flex flex-col gap-6 mb-8", className)}>
      {breadcrumbs.length > 0 && (
        <nav className="flex items-center gap-1.5 text-[10px] font-bold text-gray-400 uppercase tracking-widest">
          {breadcrumbs.map((crumb, i) => (
            <React.Fragment key={i}>
              <span className={cn(i === breadcrumbs.length - 1 ? "text-indigo-600" : "hover:text-gray-600 cursor-pointer transition-colors")}>
                {crumb}
              </span>
              {i < breadcrumbs.length - 1 && <ChevronRight className="h-3 w-3" />}
            </React.Fragment>
          ))}
        </nav>
      )}

      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-3xl font-black tracking-tight text-gray-900 sm:text-4xl">{title}</h1>
          {subtitle && (
            <p className="text-sm text-gray-500 font-medium max-w-2xl">{subtitle}</p>
          )}
        </div>

        <div className="flex items-center gap-3">
          {children}
          {(action || onAction) && (
            <Button
              onClick={onAction || action}
              className="h-11 px-5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl shadow-lg shadow-indigo-200 transition-all active:scale-95"
            >
              <Plus className="mr-2 h-4 w-4 stroke-[3px]" />
              {actionLabel || 'Add New'}
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};

export default PageHeader;
