import React from 'react';
import { cn } from '../../lib/utils';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';

const StatsCard = ({
  title,
  value,
  description,
  icon: Icon,
  trend,
  trendValue,
  className,
  iconColor = "text-primary",
  bgColor = "bg-primary/10"
}) => {
  return (
    <div className={cn("erp-stat-card group", className)}>
      <div className="flex items-start justify-between gap-3">
        <div className="space-y-2 flex-1 min-w-0">
          <p className="text-xs font-medium text-muted-foreground">{title}</p>
          <div className="flex items-baseline gap-2 flex-wrap">
            <h3 className="text-2xl font-bold text-foreground tracking-tight sm:text-3xl">{value}</h3>
            {trend && (
              <span className={cn(
                "text-[11px] font-semibold px-1.5 py-0.5 rounded-md inline-flex items-center gap-0.5",
                trend === 'up' ? 'text-emerald-700 bg-emerald-50 dark:bg-emerald-950/40 dark:text-emerald-400' :
                  trend === 'down' ? 'text-rose-700 bg-rose-50 dark:bg-rose-950/40 dark:text-rose-400' :
                    'text-muted-foreground bg-muted'
              )}>
                {trend === 'up' ? <TrendingUp className="h-3 w-3" /> : trend === 'down' ? <TrendingDown className="h-3 w-3" /> : <Minus className="h-3 w-3" />}
                {trendValue}%
              </span>
            )}
          </div>
          {description && (
            <p className="text-xs text-muted-foreground leading-relaxed">{description}</p>
          )}
        </div>

        {Icon && (
          <div className={cn(
            "h-11 w-11 rounded-xl flex items-center justify-center transition-transform duration-200 group-hover:scale-105 shrink-0",
            bgColor
          )}>
            <Icon className={cn("h-5 w-5", iconColor)} />
          </div>
        )}
      </div>
    </div>
  );
};

export default StatsCard;
