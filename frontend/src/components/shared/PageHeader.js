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
    <div className={cn("flex flex-col gap-4 mb-6", className)}>
      {breadcrumbs.length > 0 && (
        <nav className="flex items-center gap-1 text-xs text-muted-foreground">
          {breadcrumbs.map((crumb, i) => (
            <React.Fragment key={i}>
              {i > 0 && <ChevronRight className="h-3 w-3" />}
              <span className={cn(i === breadcrumbs.length - 1 && "font-medium text-foreground")}>
                {crumb}
              </span>
            </React.Fragment>
          ))}
        </nav>
      )}

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">{title}</h1>
          {subtitle && (
            <p className="text-sm text-muted-foreground max-w-2xl">{subtitle}</p>
          )}
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          {children}
          {(action || onAction) && (
            <Button onClick={onAction || action} className="h-10 rounded-xl shadow-sm shadow-primary/15">
              <Plus className="mr-2 h-4 w-4" />
              {actionLabel || 'Add New'}
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};

export default PageHeader;
