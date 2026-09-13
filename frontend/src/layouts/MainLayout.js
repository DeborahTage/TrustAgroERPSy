import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import TopBar from './TopBar';
import { Menu } from 'lucide-react';
import { Button } from '../components/ui/button';
import { cn } from '../lib/utils';

const MainLayout = () => {
  const [showSidebar, setShowSidebar] = useState(false);
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div className="flex h-screen overflow-hidden bg-background">
      <Sidebar
        show={showSidebar}
        collapsed={collapsed}
        onToggleCollapse={() => setCollapsed((v) => !v)}
        onHide={() => setShowSidebar(false)}
      />

      <div
        className={cn(
          'flex flex-1 flex-col min-w-0 transition-[margin] duration-300',
          collapsed ? 'lg:ml-[4.5rem]' : 'lg:ml-64'
        )}
      >
        <header className="sticky top-0 z-30 flex h-16 shrink-0 items-center border-b border-border/60 bg-card/80 backdrop-blur-xl px-4 lg:px-6">
          <div className="flex w-full items-center gap-3">
            <Button
              variant="ghost"
              size="icon"
              className="lg:hidden h-9 w-9 text-muted-foreground"
              onClick={() => setShowSidebar(true)}
            >
              <Menu className="h-5 w-5" />
            </Button>
            <TopBar />
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-4 lg:p-6 xl:p-8">
          <div className="mx-auto max-w-[1600px] min-h-full animate-fade-in">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};

export default MainLayout;
