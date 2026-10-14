import React from 'react';
import { cn } from '../../lib/utils';
import { Card, CardContent } from '../ui/card';
import { Badge } from '../ui/badge';

const AlertCard = ({ 
  title, 
  description, 
  type = 'warning',
  icon: Icon,
  timestamp,
  action,
  className 
}) => {
  const typeStyles = {
    critical: {
      bgColor: 'bg-red-500/10 dark:bg-red-500/20',
      iconColor: 'text-red-600 dark:text-red-400',
      badgeColor: 'destructive'
    },
    warning: {
      bgColor: 'bg-yellow-500/10 dark:bg-yellow-500/20',
      iconColor: 'text-yellow-600 dark:text-yellow-400',
      badgeColor: 'warning'
    },
    info: {
      bgColor: 'bg-blue-500/10 dark:bg-blue-500/20',
      iconColor: 'text-blue-600 dark:text-blue-400',
      badgeColor: 'info'
    },
    success: {
      bgColor: 'bg-emerald-500/10 dark:bg-emerald-500/20',
      iconColor: 'text-emerald-600 dark:text-emerald-400',
      badgeColor: 'success'
    }
  };

  const style = typeStyles[type] || typeStyles.warning;

  return (
    <Card className={cn("hover:shadow-md transition-shadow", className)}>
      <CardContent className="p-4">
        <div className="flex items-start gap-3">
          {Icon && (
            <div className={cn("p-2 rounded-lg shrink-0", style.bgColor)}>
              <Icon className={cn("h-5 w-5", style.iconColor)} />
            </div>
          )}
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <h4 className="font-semibold text-foreground truncate">{title}</h4>
              <Badge variant={style.badgeColor} className="shrink-0">{type}</Badge>
            </div>
            <p className="text-sm text-muted-foreground mb-2">{description}</p>
            {timestamp && (
              <p className="text-xs text-muted-foreground/70">{timestamp}</p>
            )}
          </div>
          {action && (
            <button
              onClick={action}
              className="shrink-0 text-sm text-primary hover:underline"
            >
              View
            </button>
          )}
        </div>
      </CardContent>
    </Card>
  );
};

export default AlertCard;
