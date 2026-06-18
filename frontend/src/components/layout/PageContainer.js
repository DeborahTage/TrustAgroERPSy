import React from 'react';
import { cn } from '../../lib/utils';

const PageContainer = ({ children, className, scrollable = true }) => {
    return (
        <div className={cn(
            "flex flex-col h-full space-y-6 animate-in fade-in duration-500",
            scrollable ? "overflow-y-auto" : "overflow-hidden",
            className
        )}>
            {children}
        </div>
    );
};

export default PageContainer;
