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
        return 'bg-emerald-50 border-emerald-100';
      case 'warning':
        return 'bg-amber-50 border-amber-100';
      case 'critical':
      case 'error':
        return 'bg-rose-50 border-rose-100';
      case 'vet':
      case 'treatment':
        return 'bg-blue-50 border-blue-100';
      case 'inventory':
      case 'stock':
      case 'stock_in':
      case 'stock_out':
        return 'bg-primary/10 border-primary/20';
      default:
        return 'bg-slate-50 border-slate-100';
    }
  };

  if (!activities || activities.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-gray-400 bg-gray-50/50 rounded-2xl border border-dashed border-gray-200">
        <Clock className="h-10 w-10 mb-3 opacity-20" />
        <p className="text-sm font-bold tracking-tight">No activity logs found</p>
        <p className="text-xs mt-1">Pending operations will appear here.</p>
      </div>
    );
  }

  return (
    <div className={cn("relative space-y-8 before:absolute before:inset-y-0 before:left-4 before:w-0.5 before:bg-gradient-to-b before:from-gray-100 before:via-gray-200 before:to-gray-50", className)}>
      {activities.map((activity, index) => (
        <div key={index} className="relative pl-12 group">
          <div className={cn(
            "absolute left-0 p-2 rounded-xl border-2 transition-all duration-300 group-hover:scale-110 z-10 bg-white",
            getBadgeColor(activity.type)
          )}>
            {getIcon(activity.type)}
          </div>
          <div className="flex flex-col gap-2 p-1">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <h4 className="text-sm font-extrabold text-gray-900 group-hover:text-emerald-600 transition-colors uppercase tracking-tight">{activity.title}</h4>
              <span className="text-[10px] font-bold text-gray-400 flex items-center gap-1.5 bg-gray-50 px-2 py-0.5 rounded-full border border-gray-100">
                <Clock className="h-3 w-3" />
                {activity.timestamp}
              </span>
            </div>
            <p className="text-sm text-gray-500 leading-snug font-medium">
              {activity.description}
            </p>
            {activity.meta && (
              <div className="mt-1 flex flex-wrap gap-2">
                {Object.entries(activity.meta).map(([key, value]) => (
                  <div key={key} className="inline-flex items-center text-[9px] uppercase tracking-widest font-black bg-white text-gray-400 border border-gray-100 px-2 py-1 rounded-lg group-hover:border-emerald-100 group-hover:text-emerald-700 transition-all">
                    <span className="opacity-50 mr-1.5">{key}</span>
                    <ArrowRight className="h-2 w-2 mr-1.5 opacity-30" />
                    <span className="text-gray-600 font-bold">{value}</span>
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
