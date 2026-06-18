import React, { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { dashboardApi } from '../../api';
import { formatCurrency } from '../../utils';
import StatsCard from '../../components/shared/StatsCard';
import AlertCard from '../../components/shared/AlertCard';
import PageContainer from '../../components/layout/PageContainer';
import PageHeader from '../../components/shared/PageHeader';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import {
  Building2,
  Users,
  Activity,
  AlertTriangle,
  DollarSign,
  UserCheck,
  TrendingUp,
  Package,
  HeartPulse,
  ShoppingCart,
  Clock
} from 'lucide-react';
import { cn } from '../../lib/utils';

const AdminDashboard = () => {
  const { user } = useAuth();
  const [data, setData] = useState({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const res = await dashboardApi.admin();
        setData(res.data.data || {});
      } catch (e) {
        console.error('Failed to fetch dashboard data:', e);
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
  }, []);

  const kpiCards = [
    {
      title: 'Total Farms',
      value: data.totalFarms || 0,
      icon: Building2,
      iconColor: 'text-indigo-600',
      bgColor: 'bg-indigo-50',
      trend: 'up',
      trendValue: '12'
    },
    {
      title: 'Total Users',
      value: data.totalUsers || 0,
      icon: Users,
      iconColor: 'text-blue-600',
      bgColor: 'bg-blue-50',
      trend: 'up',
      trendValue: '5'
    },
    {
      title: 'Active Disease Cases',
      value: data.activeDiseaseCases || 0,
      icon: Activity,
      iconColor: 'text-rose-600',
      bgColor: 'bg-rose-50',
      trend: 'down',
      trendValue: '8'
    },
    {
      title: 'Inventory Alerts',
      value: data.inventoryAlerts || 0,
      icon: AlertTriangle,
      iconColor: 'text-amber-600',
      bgColor: 'bg-amber-50',
      trend: 'up',
      trendValue: '3'
    },
    {
      title: 'Revenue Overview',
      value: formatCurrency(data.totalRevenue || 0),
      icon: DollarSign,
      iconColor: 'text-emerald-600',
      bgColor: 'bg-emerald-50',
      trend: 'up',
      trendValue: '15'
    },
    {
      title: 'Active Clients',
      value: data.activeClients || 0,
      icon: UserCheck,
      iconColor: 'text-indigo-600',
      bgColor: 'bg-indigo-50',
      trend: 'up',
      trendValue: '10'
    }
  ];

  const alerts = [
    { title: 'Low Stock Alert', description: '15 items are below minimum stock level', type: 'critical', icon: Package, timestamp: '2 hours ago' },
    { title: 'Expiring Drugs', description: '8 pharmaceutical items expiring within 30 days', type: 'warning', icon: Clock, timestamp: '5 hours ago' },
    { title: 'High Mortality Rate', description: 'Farm A showing 12% mortality rate this week', type: 'critical', icon: Activity, timestamp: '1 day ago' },
  ];

  const recentActivities = [
    { title: 'New Farm Record Created', description: 'Daily record submitted for Farm B - Flock 3', type: 'info', icon: TrendingUp, timestamp: '30 minutes ago' },
    { title: 'Stock Movement', description: 'Stock in: 500 units of Animal Feed', type: 'success', icon: Package, timestamp: '1 hour ago' },
    { title: 'Pharmacy Sale', description: 'Sale #1234 - $450.00 completed', type: 'success', icon: ShoppingCart, timestamp: '3 hours ago' }
  ];

  if (loading) {
    return (
      <PageContainer>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="h-32 bg-gray-100 rounded-2xl animate-pulse" />
          ))}
        </div>
      </PageContainer>
    );
  }

  return (
    <PageContainer>
      <PageHeader
        title="Admin Dashboard"
        subtitle={`Welcome back, ${user?.fullName || 'Admin'}. Here's what's happening today.`}
        breadcrumbs={['Home', 'Admin', 'Dashboard']}
      />

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {kpiCards.map((card, index) => (
          <StatsCard key={index} {...card} />
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-2 border-none shadow-sm bg-white rounded-2xl overflow-hidden">
          <CardHeader className="border-b border-gray-50 px-6 py-4">
            <CardTitle className="flex items-center gap-2 text-base font-black uppercase tracking-tight text-gray-900">
              <Activity className="h-4.5 w-4.5 text-indigo-600" />
              Recent Activity
            </CardTitle>
          </CardHeader>
          <CardContent className="p-6">
            <div className="space-y-4">
              {recentActivities.map((activity, index) => (
                <AlertCard key={index} {...activity} />
              ))}
            </div>
          </CardContent>
        </Card>

        <Card className="border-none shadow-sm bg-white rounded-2xl overflow-hidden">
          <CardHeader className="border-b border-gray-100 px-6 py-4">
            <CardTitle className="flex items-center gap-2 text-base font-black uppercase tracking-tight text-gray-900">
              <AlertTriangle className="h-4.5 w-4.5 text-rose-500" />
              System Alerts
            </CardTitle>
          </CardHeader>
          <CardContent className="p-6">
            <div className="space-y-4">
              {alerts.map((alert, index) => (
                <AlertCard key={index} {...alert} />
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 pb-8">
        <Card className="border-none shadow-sm bg-white rounded-2xl p-6 h-80 flex items-center justify-center group overflow-hidden">
          <div className="absolute top-0 left-0 w-1 h-full bg-indigo-600 opacity-0 group-hover:opacity-100 transition-opacity" />
          <div className="text-center">
            <div className="h-16 w-16 bg-indigo-50 rounded-2xl flex items-center justify-center mx-auto mb-4 group-hover:scale-110 transition-transform">
              <TrendingUp className="h-8 w-8 text-indigo-600" />
            </div>
            <h3 className="text-lg font-black text-gray-900 uppercase">Farm Performance</h3>
            <p className="text-xs text-gray-400 font-bold tracking-widest mt-1">Live Analytics Pending Integration</p>
          </div>
        </Card>

        <Card className="border-none shadow-sm bg-white rounded-2xl p-6 h-80 flex items-center justify-center group overflow-hidden">
          <div className="absolute top-0 left-0 w-1 h-full bg-emerald-600 opacity-0 group-hover:opacity-100 transition-opacity" />
          <div className="text-center">
            <div className="h-16 w-16 bg-emerald-50 rounded-2xl flex items-center justify-center mx-auto mb-4 group-hover:scale-110 transition-transform">
              <DollarSign className="h-8 w-8 text-emerald-600" />
            </div>
            <h3 className="text-lg font-black text-gray-900 uppercase">Revenue Trends</h3>
            <p className="text-xs text-gray-400 font-bold tracking-widest mt-1">Real-time Finance Sync Active</p>
          </div>
        </Card>
      </div>
    </PageContainer>
  );
};

export default AdminDashboard;
