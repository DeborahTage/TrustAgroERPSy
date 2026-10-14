import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { farmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Badge } from '../../components/ui/badge';
import { Progress } from '../../components/ui/progress';
import { Home, Thermometer, Droplets, Wind, Users, Zap } from 'lucide-react';
import { cn } from '../../lib/utils';

const BarnDashboard = () => {
  const [barns, setBarns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchBarns = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await farmApi.getBarns();
      setBarns(res.data.data || []);
    } catch (e) {
      setError('Failed to load barn data.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchBarns();
    const interval = setInterval(() => fetchBarns(), 30000);
    return () => clearInterval(interval);
  }, [fetchBarns]);

  const getStatusColor = (barn) => {
    if (barn.currentAmmonia != null && barn.currentAmmonia > 25) return 'bg-rose-500';
    if (barn.currentTemp != null && (barn.currentTemp > 35 || barn.currentTemp < 20)) return 'bg-amber-500';
    if (barn.currentTemp == null && barn.currentHumidity == null) return 'bg-slate-400';
    return 'bg-emerald-500';
  };

  const stats = useMemo(() => [
    { title: 'Total Barns', value: barns.length, icon: Home, iconColor: 'text-primary', bgColor: 'bg-primary/10 dark:bg-primary/20' },
    { title: 'Total Capacity', value: barns.reduce((s, b) => s + (b.capacity || 0), 0).toLocaleString(), icon: Users, iconColor: 'text-blue-600 dark:text-blue-400', bgColor: 'bg-blue-500/10 dark:bg-blue-500/20', description: 'bird slots' },
    { title: 'Occupied', value: barns.reduce((s, b) => s + (b.currentBirdCount || 0), 0).toLocaleString(), icon: Users, iconColor: 'text-emerald-600 dark:text-emerald-400', bgColor: 'bg-emerald-500/10 dark:bg-emerald-500/20', description: 'current Chickens' },
    { title: 'Alerts', value: barns.filter((b) => b.currentAmmonia > 25 || (b.currentTemp != null && (b.currentTemp > 35 || b.currentTemp < 20))).length, icon: Wind, iconColor: 'text-rose-600 dark:text-rose-400', bgColor: 'bg-rose-500/10 dark:bg-rose-500/20' },
  ], [barns]);

  const fmt = (val, suffix = '') => (val != null ? `${val}${suffix}` : '—');

  return (
    <EnterprisePage
      title="Barn Control Center"
      subtitle="Environmental monitoring and occupancy from IoT sensors and flock assignments"
      breadcrumbs={['Farm', 'Barn Dashboard']}
      loading={loading}
      error={error}
      onRetry={fetchBarns}
      onRefresh={fetchBarns}
      stats={stats}
      empty={!barns.length ? { title: 'No barns configured', description: 'Barns are linked to farms. Add barns via farm setup to enable monitoring.' } : null}
    >
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {barns.map((barn) => {
          const occupancy = barn.capacity > 0 ? ((barn.currentBirdCount || 0) / barn.capacity) * 100 : 0;
          return (
            <Card key={barn.id} className="erp-card overflow-hidden">
              <div className={cn('h-2 w-full', getStatusColor(barn))} />
              <CardHeader className="pb-2">
                <div className="flex justify-between items-start">
                  <div>
                    <CardTitle className="text-lg font-bold flex items-center gap-2">
                      <Home className="h-5 w-5 text-primary" /> {barn.name}
                    </CardTitle>
                    <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground mt-1">{barn.barnType || 'STANDARD'}</p>
                  </div>
                  <Badge variant="outline">{(barn.capacity || 0) - (barn.currentBirdCount || 0)} free</Badge>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-3">
                  <div className="bg-orange-500/10 dark:bg-orange-500/20 p-3 rounded-xl border border-orange-500/20">
                    <div className="flex items-center gap-1 text-orange-600 dark:text-orange-400 text-[10px] font-bold uppercase"><Thermometer className="h-3 w-3" /> Temp</div>
                    <div className="text-xl font-bold text-orange-700 dark:text-orange-300">{fmt(barn.currentTemp, '°C')}</div>
                  </div>
                  <div className="bg-blue-500/10 dark:bg-blue-500/20 p-3 rounded-xl border border-blue-500/20">
                    <div className="flex items-center gap-1 text-blue-600 dark:text-blue-400 text-[10px] font-bold uppercase"><Droplets className="h-3 w-3" /> Humidity</div>
                    <div className="text-xl font-bold text-blue-700 dark:text-blue-300">{fmt(barn.currentHumidity, '%')}</div>
                  </div>
                  <div className="bg-rose-500/10 dark:bg-rose-500/20 p-3 rounded-xl border border-rose-500/20">
                    <div className="flex items-center gap-1 text-rose-600 dark:text-rose-400 text-[10px] font-bold uppercase"><Wind className="h-3 w-3" /> NH₃</div>
                    <div className="text-xl font-bold text-rose-700 dark:text-rose-300">{fmt(barn.currentAmmonia, ' ppm')}</div>
                  </div>
                  <div className="bg-primary/10 dark:bg-primary/20 p-3 rounded-xl border border-primary/20">
                    <div className="flex items-center gap-1 text-primary text-[10px] font-bold uppercase"><Zap className="h-3 w-3" /> CO₂</div>
                    <div className="text-xl font-bold text-primary">{fmt(barn.currentCO2, ' ppm')}</div>
                  </div>
                </div>
                <div className="space-y-1">
                  <div className="flex justify-between text-[10px] font-bold text-slate-400 uppercase">
                    <span>Occupancy</span>
                    <span>{occupancy.toFixed(0)}%</span>
                  </div>
                  <Progress value={occupancy} className="h-2" />
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </EnterprisePage>
  );
};

export default BarnDashboard;
