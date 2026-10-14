import React from 'react';
import { cn } from '../../lib/utils';
import { Clock, CheckCircle2, AlertCircle, Info, Syringe, Package, Activity, ArrowRight } from 'lucide-react';

const ActivityTimeline = ({ activities = [], className }) => {
  const getIcon = (type) => {
    switch (type?.toLowerCase()) {
      case 'success':
        return <CheckCircle2 className="h-4 w-4 text-emerald-600" />;
      case 'warning':
        return <AlertCircle className="h-4 w-4 text-amber-600" />;
      case 'critical':
      case 'error':
        return <AlertCircle className="h-4 w-4 text-rose-600" />;
      case 'vet':
      case 'treatment':
        return <Syringe className="h-4 w-4 text-blue-600" />;
      case 'inventory':
      case 'stock':
      case 'stock_in':
      case 'stock_out':
        return <Package className="h-4 w-4 text-primary" />;
      case 'disease':
      case 'health':
        return <Activity className="h-4 w-4 text-rose-600" />;
      default:
        return <Info className="h-4 w-4 text-slate-600" />;
    }
  };

  const getBadgeColor = (type) => {
    switch (type?.toLowerCase()) {
      case 'success':
        return 'bg-emerald-50 border-emerald-100 dark:bg-emerald-950/40 dark:border-emerald-800/40';
      case 'warning':
        return 'bg-amber-50 border-amber-100 dark:bg-amber-950/40 dark:border-amber-800/40';
      case 'critical':
      case 'error':
        return 'bg-rose-50 border-rose-100 dark:bg-rose-950/40 dark:border-rose-800/40';
      case 'vet':
      case 'treatment':
        return 'bg-blue-50 border-blue-100 dark:bg-blue-950/40 dark:border-blue-800/40';
      case 'inventory':
      case 'stock':
      case 'stock_in':
      case 'stock_out':
        return 'bg-primary/10 border-primary/20 dark:bg-primary/20 dark:border-primary/30';
      default:
        return 'bg-slate-50 border-slate-100 dark:bg-slate-800/40 dark:border-slate-700/40';
    }
  };

  if (!activities || activities.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-muted-foreground bg-muted/30 rounded-2xl border border-dashed border-border">
        <Clock className="h-10 w-10 mb-3 opacity-20" />
        <p className="text-sm font-bold tracking-tight">No activity logs found</p>
        <p className="text-xs mt-1">Pending operations will appear here.</p>
      </div>
    );
  }

  return (
    <div className={cn("relative space-y-8 before:absolute before:inset-y-0 before:left-4 before:w-0.5 before:bg-gradient-to-b before:from-gray-100 before:via-gray-200 before:to-gray-50 dark:before:from-slate-800 dark:before:via-slate-700 dark:before:to-slate-900", className)}>
      {activities.map((activity, index) => (
        <div key={index} className="relative pl-12 group">
          <div className={cn(
            "absolute left-0 p-2 rounded-xl border-2 transition-all duration-300 group-hover:scale-110 z-10 bg-card",
            getBadgeColor(activity.type)
          )}>
            {getIcon(activity.type)}
          </div>
          <div className="flex flex-col gap-2 p-1">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <h4 className="text-sm font-extrabold text-foreground group-hover:text-emerald-500 transition-colors uppercase tracking-tight">{activity.title}</h4>
              <span className="text-[10px] font-bold text-muted-foreground flex items-center gap-1.5 bg-muted/60 dark:bg-slate-800/60 px-2 py-0.5 rounded-full border border-border">
                <Clock className="h-3 w-3" />
                {activity.timestamp}
              </span>
            </div>
            <p className="text-sm text-muted-foreground leading-snug font-medium">
              {activity.description}
            </p>
            {activity.meta && (
              <div className="mt-1 flex flex-wrap gap-2">
                {Object.entries(activity.meta).map(([key, value]) => (
                  <div key={key} className="inline-flex items-center text-[9px] uppercase tracking-widest font-black bg-card text-muted-foreground border border-border px-2 py-1 rounded-lg group-hover:border-emerald-500/30 group-hover:text-emerald-500 transition-all">
                    <span className="opacity-50 mr-1.5">{key}</span>
                    <ArrowRight className="h-2 w-2 mr-1.5 opacity-30" />
                    <span className="text-foreground/80 font-bold">{value}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      ))}
    </div>
  );
};

export default ActivityTimeline;
