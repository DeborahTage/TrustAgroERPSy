import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { feedApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Badge } from '../../components/ui/badge';
import { Progress } from '../../components/ui/progress';
import { Database, AlertTriangle } from 'lucide-react';
import { cn } from '../../lib/utils';

const SiloDashboard = () => {
  const [silos, setSilos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchSilos = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await feedApi.getSilos();
      setSilos(res.data.data || []);
    } catch (e) {
      setError('Failed to load silo data.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchSilos(); }, [fetchSilos]);

  const stats = useMemo(() => {
    const totalCapacity = silos.reduce((s, x) => s + (x.capacityKg || 0), 0);
    const totalLevel = silos.reduce((s, x) => s + (x.currentLevelKg || 0), 0);
    const low = silos.filter((s) => s.capacityKg > 0 && (s.currentLevelKg / s.capacityKg) < 0.2).length;
    return [
      { title: 'Active Silos', value: silos.length, icon: Database, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
      { title: 'Total Capacity', value: `${totalCapacity.toLocaleString()} kg`, icon: Database, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
      { title: 'Current Stock', value: `${totalLevel.toLocaleString()} kg`, icon: Database, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
      { title: 'Low Level Alerts', value: low, icon: AlertTriangle, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    ];
  }, [silos]);

  const getLevelColor = (level, capacity) => {
    const percent = capacity > 0 ? (level / capacity) * 100 : 0;
    if (percent < 20) return 'text-rose-600';
    if (percent < 40) return 'text-amber-600';
    return 'text-emerald-600';
  };

  return (
    <EnterprisePage
      title="Feed Silo Monitoring"
      subtitle="Real-time feed inventory levels across farm storage units"
      breadcrumbs={['Farm', 'Feed', 'Silos']}
      loading={loading}
      error={error}
      onRetry={fetchSilos}
      onRefresh={fetchSilos}
      stats={stats}
      empty={!silos.length ? { title: 'No silos configured', description: 'Silos appear when feed deliveries are logged to the system.' } : null}
    >
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {silos.map((silo) => {
          const pct = silo.capacityKg > 0 ? (silo.currentLevelKg / silo.capacityKg) * 100 : 0;
          return (
            <Card key={silo.id} className="border-none shadow-sm rounded-2xl">
              <CardHeader className="pb-2">
                <div className="flex justify-between items-start">
                  <CardTitle className="text-lg font-bold flex items-center gap-2">
                    <Database className="h-5 w-5 text-primary" /> {silo.name}
                  </CardTitle>
                  <Badge variant="outline">{silo.farmName || 'Farm'}</Badge>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className={cn('text-3xl font-black', getLevelColor(silo.currentLevelKg, silo.capacityKg))}>
                  {(silo.currentLevelKg || 0).toLocaleString()} <span className="text-sm font-medium text-slate-400">/ {(silo.capacityKg || 0).toLocaleString()} kg</span>
                </div>
                <Progress value={pct} className="h-3" />
                <p className="text-xs text-slate-500">{pct.toFixed(0)}% capacity · {silo.feedType || 'Mixed feed'}</p>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </EnterprisePage>
  );
};

export default SiloDashboard;
