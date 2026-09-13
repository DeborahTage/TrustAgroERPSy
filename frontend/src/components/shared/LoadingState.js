import React from 'react';
import { cn } from '../../lib/utils';
import { Skeleton } from '../ui/skeleton';

const LoadingState = ({ count = 5, message, className }) => {
  return (
    <div className={cn("space-y-4", className)}>
      {message && <p className="text-sm text-slate-500 text-center">{message}</p>}
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="flex items-center space-x-4 p-4 border border-border/60 rounded-xl bg-card">
          <Skeleton className="h-12 w-12 rounded-lg" />
          <div className="space-y-2 flex-1">
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-3 w-1/2" />
          </div>
          <Skeleton className="h-8 w-24" />
        </div>
      ))}
    </div>
  );
};

export default LoadingState;
