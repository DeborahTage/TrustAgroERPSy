import React, { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { flockApi } from '../../api';
import StatsCard from '../../components/shared/StatsCard';
import AlertCard from '../../components/shared/AlertCard';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
  Cell
} from 'recharts';
import PageHeader from '../../components/shared/PageHeader';
import {
  Layers,
  TrendingDown,
  TrendingUp,
  Activity,
  AlertTriangle,
  HeartPulse,
  BarChart3,
  Scale,
  Calendar,
  ChevronRight
} from 'lucide-react';
import { cn } from '../../lib/utils';

const FlockDashboard = () => {
  const { user } = useAuth();
  const [data, setData] = useState({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const res = await flockApi.getAll();
        const flocks = res.data.data || [];

        // Calculate analytics
        const totalFlocks = flocks.length;
        const activeFlocks = flocks.filter(f => f.status === 'ACTIVE').length;
        const totalChickens = flocks.reduce((sum, f) => sum + (Number(f.initialBirdCount) || 0), 0);
        const avgMortality = 2.5; // This would come from actual data

        setData({
          totalFlocks,
          activeFlocks,
          totalChickens,
          avgMortality,
          flocks
        });
      } catch (e) {
        console.error('Failed to fetch flock dashboard data:', e);
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
  }, []);

  const kpiCards = [
    {
      title: 'Total Flocks',
      value: data.totalFlocks || 0,
      icon: Layers,
      iconColor: 'text-blue-600',
      bgColor: 'bg-blue-50',
      trend: 'up',
      trendValue: '+3 new this month'
    },
    {
      title: 'Active Flocks',
      value: data.activeFlocks || 0,
      icon: Activity,
      iconColor: 'text-green-600',
      bgColor: 'bg-green-50',
      trend: 'stable',
      trendValue: 'All operational'
    },
    {
      title: 'Total Chickens',
      value: `${(data.totalChickens || 0).toLocaleString()}`,
      icon: HeartPulse,
      iconColor: 'text-purple-600',
      bgColor: 'bg-purple-50',
      trend: 'up',
      trendValue: '+8% from last month'
    },
    {
      title: 'Avg Mortality',
      value: `${data.avgMortality || 0}%`,
      icon: TrendingDown,
      iconColor: data.avgMortality > 3 ? 'text-red-600' : 'text-green-600',
      bgColor: data.avgMortality > 3 ? 'bg-red-50' : 'bg-green-50',
      trend: data.avgMortality > 3 ? 'up' : 'down',
      trendValue: data.avgMortality > 3 ? 'Above target' : 'Within target'
    }
  ];

  const alerts = [
    {
      title: 'High Mortality Alert',
      description: 'Flock B-003 showing 5.2% mortality rate',
      type: 'critical',
      icon: AlertTriangle,
      timestamp: '1 hour ago'
    },
    {
      title: 'Growth Rate Warning',
      description: 'Flock A-001 below expected growth rate',
      type: 'warning',
      icon: TrendingDown,
      timestamp: '3 hours ago'
    }
  ];

  const recentActivities = [
    {
      title: 'New Flock Created',
      description: 'Flock C-005 added with 5,000 broiler Chickens',
      type: 'info',
      icon: Layers,
      timestamp: '30 minutes ago'
    },
    {
      title: 'Vaccination Completed',
      description: 'Flock B-002 received Newcastle vaccination',
      type: 'success',
      icon: HeartPulse,
      timestamp: '2 hours ago'
    },
    {
      title: 'Weight Check',
      description: 'Flock A-001 average weight: 1.8kg',
      type: 'info',
      icon: Scale,
      timestamp: '1 day ago'
    }
  ];

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {Array.from({ length: 4 }).map((_, i) => (
            <Card key={i} className="h-32">
              <CardContent className="p-6">
                <div className="animate-pulse space-y-4">
                  <div className="h-4 bg-gray-200 rounded w-1/3" />
                  <div className="h-8 bg-gray-200 rounded w-1/2" />
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-3xl font-bold tracking-tight text-gray-900">Flock Management</h1>
        <p className="text-gray-500 mt-1">
          Monitor flock performance, mortality trends, and growth analytics.
        </p>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {kpiCards.map((card, index) => (
          <StatsCard key={index} {...card} />
        ))}
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Activity Feed */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Activity className="h-5 w-5" />
              Recent Activity
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {recentActivities.map((activity, index) => (
                <AlertCard key={index} {...activity} />
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Alert Center */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <AlertTriangle className="h-5 w-5" />
              Alerts
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {alerts.map((alert, index) => (
                <AlertCard key={index} {...alert} />
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="border-none shadow-xl rounded-3xl overflow-hidden bg-white">
          <CardHeader>
            <CardTitle className="text-lg font-black text-gray-900 tracking-tight flex items-center gap-2">
              <TrendingUp className="h-5 w-5 text-emerald-500" />
              Growth Performance
            </CardTitle>
            <p className="text-[10px] uppercase font-bold tracking-widest text-gray-400">Actual Weight vs. Breed Standard</p>
          </CardHeader>
          <CardContent>
            <div className="h-72 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={[
                  { day: 0, actual: 42, standard: 42 },
                  { day: 7, actual: 180, standard: 190 },
                  { day: 14, actual: 450, standard: 480 },
                  { day: 21, actual: 920, standard: 950 },
                  { day: 28, actual: 1550, standard: 1580 },
                ]}>
                  <defs>
                    <linearGradient id="colorActual" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#10b981" stopOpacity={0.1} />
                      <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis dataKey="day" axisLine={false} tickLine={false} tick={{ fill: '#94a3b8', fontSize: 10 }} label={{ value: 'Day', position: 'insideBottom', offset: -5 }} />
                  <YAxis hide />
                  <Tooltip contentStyle={{ borderRadius: '16px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }} />
                  <Area type="monotone" dataKey="actual" stroke="#10b981" strokeWidth={3} fillOpacity={1} fill="url(#colorActual)" />
                  <Area type="monotone" dataKey="standard" stroke="#94a3b8" strokeWidth={2} strokeDasharray="5 5" fill="none" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        <Card className="border-none shadow-xl rounded-3xl overflow-hidden bg-white">
          <CardHeader>
            <CardTitle className="text-lg font-black text-gray-900 tracking-tight flex items-center gap-2">
              <Scale className="h-5 w-5 text-primary" />
              Feed Efficiency (FCR)
            </CardTitle>
            <p className="text-[10px] uppercase font-bold tracking-widest text-gray-400">Feed Conversion Ratio Trend</p>
          </CardHeader>
          <CardContent>
            <div className="h-72 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={[
                  { week: 'Wk 1', fcr: 1.1 },
                  { week: 'Wk 2', fcr: 1.3 },
                  { week: 'Wk 3', fcr: 1.45 },
                  { week: 'Wk 4', fcr: 1.6 },
                ]}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis dataKey="week" axisLine={false} tickLine={false} tick={{ fill: '#94a3b8', fontSize: 10 }} />
                  <YAxis hide />
                  <Tooltip cursor={{ fill: '#f8fafc' }} contentStyle={{ borderRadius: '16px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }} />
                  <Bar dataKey="fcr" fill="#6366f1" radius={[10, 10, 0, 0]} maxBarSize={40}>
                    {[1.1, 1.3, 1.45, 1.6].map((entry, index) => (
                      <Cell key={index} fill={entry > 1.7 ? '#f43f5e' : '#6366f1'} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default FlockDashboard;
