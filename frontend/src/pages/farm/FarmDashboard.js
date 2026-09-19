import React, { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { farmApi } from '../../api';
import StatsCard from '../../components/shared/StatsCard';
import AlertCard from '../../components/shared/AlertCard';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { 
  Building2, 
  Users, 
  Activity, 
  TrendingUp, 
  AlertTriangle,
  MapPin,
  BarChart3
} from 'lucide-react';

const FarmDashboard = () => {
  const { user } = useAuth();
  const [data, setData] = useState({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const res = await farmApi.getAll();
        const farms = res.data.data || [];
        
        // Calculate analytics
        const totalFarms = farms.length;
        const activeFarms = farms.filter(f => f.status === 'ACTIVE').length;
        const totalCapacity = farms.reduce((sum, f) => sum + (Number(f.capacity) || 0), 0);
        const activeFlocks = farms.reduce((sum, f) => sum + (f.activeFlocks || 0), 0);
        
        setData({
          totalFarms,
          activeFarms,
          totalCapacity,
          activeFlocks,
          farms
        });
      } catch (e) {
        console.error('Failed to fetch farm dashboard data:', e);
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
      iconColor: 'text-blue-600',
      bgColor: 'bg-blue-50',
      trend: 'up',
      trendValue: '+2 new this month'
    },
    {
      title: 'Active Farms',
      value: data.activeFarms || 0,
      icon: Activity,
      iconColor: 'text-green-600',
      bgColor: 'bg-green-50',
      trend: 'stable',
      trendValue: 'All operational'
    },
    {
      title: 'Total Capacity',
      value: `${data.totalCapacity || 0} Chickens`,
      icon: Users,
      iconColor: 'text-purple-600',
      bgColor: 'bg-purple-50',
      trend: 'up',
      trendValue: '+15% from last quarter'
    },
    {
      title: 'Active Flocks',
      value: data.activeFlocks || 0,
      icon: TrendingUp,
      iconColor: 'text-orange-600',
      bgColor: 'bg-orange-50',
      trend: 'up',
      trendValue: '+5 new flocks'
    }
  ];

  const alerts = [
    {
      title: 'Farm Capacity Warning',
      description: 'Farm A is at 95% capacity',
      type: 'warning',
      icon: AlertTriangle,
      timestamp: '2 hours ago'
    },
    {
      title: 'New Farm Setup',
      description: 'Farm C setup completed',
      type: 'success',
      icon: Building2,
      timestamp: '1 day ago'
    }
  ];

  const recentActivities = [
    {
      title: 'Farm Created',
      description: 'New farm "Green Valley" added to system',
      type: 'info',
      icon: Building2,
      timestamp: '30 minutes ago'
    },
    {
      title: 'Capacity Updated',
      description: 'Farm B capacity increased to 10,000 Chickens',
      type: 'success',
      icon: TrendingUp,
      timestamp: '2 hours ago'
    },
    {
      title: 'Manager Assigned',
      description: 'John Doe assigned as Farm Manager for Farm A',
      type: 'info',
      icon: Users,
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
        <h1 className="text-3xl font-bold tracking-tight text-gray-900">Farm Management</h1>
        <p className="text-gray-500 mt-1">
          Manage farms, monitor capacity, and track farm performance.
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
        <Card>
          <CardHeader>
            <CardTitle>Farm Capacity Distribution</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64 flex items-center justify-center text-gray-400">
              <div className="text-center">
                <BarChart3 className="h-12 w-12 mx-auto mb-2" />
                <p>Capacity distribution chart</p>
                <p className="text-sm">(Recharts integration available)</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Farm Performance Overview</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64 flex items-center justify-center text-gray-400">
              <div className="text-center">
                <TrendingUp className="h-12 w-12 mx-auto mb-2" />
                <p>Performance metrics chart</p>
                <p className="text-sm">(Recharts integration available)</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default FarmDashboard;
