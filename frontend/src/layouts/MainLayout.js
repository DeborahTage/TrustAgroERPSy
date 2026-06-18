import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import TopBar from './TopBar';
import { Menu } from 'lucide-react';
import { Button } from '../components/ui/button';

const MainLayout = () => {
  const [showSidebar, setShowSidebar] = useState(false);

  return (
    <div className="flex h-screen overflow-hidden bg-[#f8fafc]">
      {/* Sidebar - Desktop always visible, Mobile toggleable */}
      <Sidebar show={showSidebar} onHide={() => setShowSidebar(false)} />

      {/* Main Content Area */}
      <div className="flex flex-1 flex-col lg:ml-64 relative">
        {/* Header - Glassmorphism Effect */}
        <header className="flex h-16 shrink-0 items-center justify-between border-b border-gray-100 bg-white/80 backdrop-blur-md px-4 lg:px-8 sticky top-0 z-30">
          <div className="flex items-center gap-4 w-full">
            <Button
              variant="ghost"
              size="icon"
              className="lg:hidden h-10 w-10 text-gray-500 hover:bg-gray-100"
              onClick={() => setShowSidebar(true)}
            >
              <Menu className="h-5 w-5" />
            </Button>
            <TopBar />
          </div>
        </header>

        {/* Global Page Padding and Scroll Area */}
        <main className="flex-1 overflow-y-auto p-4 lg:p-8">
          <div className="max-w-[1600px] mx-auto min-h-full">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};

export default MainLayout;
