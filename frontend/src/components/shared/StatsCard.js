import React from 'react';
import { cn } from '../../lib/utils';
import { Card, CardContent } from '../ui/card';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';

const StatsCard = ({
  title,
  value,
  description,
  icon: Icon,
  trend,
  trendValue,
  className,
  iconColor = "text-indigo-600",
  bgColor = "bg-indigo-50"
}) => {
  return (
    <Card className={cn("group relative overflow-hidden boarder-none transition-all duration-300 hover:shadow-md hover:-translate-y-1 bg-white", className)}>
      <CardContent className="p-6">
        <div className="flex items-start justify-between">
          <div className="space-y-1.5 flex-1">
            <p className="text-[11px] font-bold text-gray-400 uppercase tracking-[0.1em]">{title}</p>
            <div className="flex items-baseline gap-2">
              <h3 className="text-3xl font-black text-gray-900 tracking-tight">{value}</h3>
              {trend && (
                <span className={cn(
                  "text-[10px] font-black px-1.5 py-0.5 rounded-sm flex items-center gap-0.5",
                  trend === 'up' ? 'text-emerald-700 bg-emerald-50' :
                    trend === 'down' ? 'text-rose-700 bg-rose-50' :
                      'text-gray-500 bg-gray-50'
                )}>
                  {trend === 'up' ? <TrendingUp className="h-2.5 w-2.5" /> : trend === 'down' ? <TrendingDown className="h-2.5 w-2.5" /> : <Minus className="h-2.5 w-2.5" />}
                  {trendValue}%
                </span>
              )}
            </div>
            {description && (
              <p className="text-[11px] text-gray-500 font-medium leading-relaxed">{description}</p>
            )}
          </div>

          {Icon && (
            <div className={cn(
              "h-12 w-12 rounded-xl flex items-center justify-center transition-transform duration-300 group-hover:scale-110 shrink-0",
              bgColor
            )}>
              <Icon className={cn("h-6 w-6", iconColor)} />
            </div>
          )}
        </div>

        {/* Subtle decorative element */}
        <div className="absolute -bottom-1 -right-1 opacity-[0.03] group-hover:opacity-[0.06] transition-opacity">
          {Icon && <Icon className="h-16 w-16 rotate-12" />}
        </div>
      </CardContent>
    </Card>
  );
};

export default StatsCard;
