import React from 'react';
import { cn } from '../../lib/utils';
import { Card, CardContent } from '../ui/card';
import { Inbox } from 'lucide-react';

const EmptyState = ({ 
  title = "No data found", 
  description = "There are no items to display at this time.",
  icon: Icon = Inbox,
  action,
  actionLabel,
  className 
}) => {
  return (
    <Card className={cn("border-dashed border-border/60 bg-muted/20", className)}>
      <CardContent className="flex flex-col items-center justify-center py-14">
        <div className="p-4 bg-muted rounded-2xl mb-4">
          <Icon className="h-8 w-8 text-muted-foreground" />
        </div>
        <h3 className="text-base font-semibold text-foreground mb-1">{title}</h3>
        <p className="text-sm text-muted-foreground text-center max-w-sm mb-4">{description}</p>
        {action && (
          <button
            onClick={action}
            className="px-4 py-2 bg-primary text-primary-foreground rounded-xl hover:bg-primary/90 transition-colors text-sm font-medium"
          >
            {actionLabel || 'Add New'}
          </button>
        )}
      </CardContent>
    </Card>
  );
};

export default EmptyState;
