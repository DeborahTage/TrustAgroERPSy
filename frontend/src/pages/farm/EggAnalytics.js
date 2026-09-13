import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { dailyRecordApi, flockApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend,
} from 'recharts';
import { Activity, Egg, AlertTriangle, TrendingUp } from 'lucide-react';
import { formatDate } from '../../utils';

const COLORS = ['#10b981', '#f43f5e'];

const EggAnalytics = () => {
  const { id: flockId } = useParams();
  const [flock, setFlock] = useState(null);
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchData = useCallback(async () => {
    if (!flockId) return;
    setLoading(true);
    setError(null);
    try {
      const [flockRes, recordsRes] = await Promise.all([
        flockApi.getById(flockId),
        dailyRecordApi.getAll({ flockId }),
      ]);
      setFlock(flockRes.data.data);
      setRecords(recordsRes.data.data || []);
    } catch (e) {
      setError('Failed to load egg production data.');
    } finally {
      setLoading(false);
    }
  }, [flockId]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const hdpTrend = useMemo(() =>
    [...records]
      .filter((r) => r.eggProduction != null && r.openingBirdCount > 0)
      .sort((a, b) => new Date(a.date) - new Date(b.date))
      .map((r) => ({
        date: formatDate(r.date),
        hdp: Number(((r.eggProduction / r.openingBirdCount) * 100).toFixed(1)),
        eggs: r.eggProduction,
      })),
  [records]);

  const totals = useMemo(() => {
    const totalEggs = records.reduce((s, r) => s + (Number(r.eggProduction) || 0), 0);
    const totalDamaged = records.reduce((s, r) => s + (Number(r.damagedEggs) || 0), 0);
    const totalGood = totalEggs - totalDamaged;
    const latest = hdpTrend[hdpTrend.length - 1];
    const brokenRate = totalEggs > 0 ? ((totalDamaged / totalEggs) * 100).toFixed(1) : '0';
    return { totalEggs, totalGood, totalDamaged, latestHdp: latest?.hdp, brokenRate };
  }, [records, hdpTrend]);

  const gradeData = useMemo(() => {
    if (totals.totalEggs === 0) return [];
    return [
      { name: 'Marketable', value: totals.totalGood, color: COLORS[0] },
      { name: 'Damaged', value: totals.totalDamaged, color: COLORS[1] },
    ].filter((d) => d.value > 0);
  }, [totals]);

  const stats = useMemo(() => [
    { title: 'Hen-Day Production %', value: totals.latestHdp != null ? `${totals.latestHdp}%` : '—', icon: Activity, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50', description: 'Latest daily record' },
    { title: 'Total Eggs Logged', value: totals.totalEggs.toLocaleString(), icon: Egg, iconColor: 'text-orange-600', bgColor: 'bg-orange-50' },
    { title: 'Broken Rate', value: `${totals.brokenRate}%`, icon: AlertTriangle, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    { title: 'Current Flock', value: flock?.currentBirdCount?.toLocaleString() || '—', icon: TrendingUp, iconColor: 'text-primary', bgColor: 'bg-primary/10', description: flock?.batchCode },
  ], [totals, flock]);

  return (
    <EnterprisePage
      title="Egg Production Analytics"
      subtitle={flock ? `Flock ${flock.batchCode} — production from daily farm records` : 'Flock production analysis'}
      breadcrumbs={['Farm', 'Flocks', flock?.batchCode || 'Analytics']}
      loading={loading}
      error={error}
      onRetry={fetchData}
      onRefresh={fetchData}
      stats={stats}
      empty={!records.length ? { title: 'No production data', description: 'Daily farm records with egg counts will populate this dashboard automatically.' } : null}
    >
      {records.length > 0 && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <Card className="lg:col-span-2 border-none shadow-sm">
            <CardHeader>
              <CardTitle className="text-base font-bold">Hen-Day Production % Trend</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="h-72">
                {hdpTrend.length === 0 ? (
                  <p className="text-sm text-slate-500 text-center py-16">No HDP data — records need opening bird count and egg production.</p>
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={hdpTrend}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} />
                      <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                      <YAxis domain={[0, 100]} tick={{ fontSize: 11 }} />
                      <Tooltip />
                      <Line type="monotone" dataKey="hdp" stroke="#10b981" strokeWidth={3} dot={{ r: 4 }} />
                    </LineChart>
                  </ResponsiveContainer>
                )}
              </div>
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader>
              <CardTitle className="text-base font-bold">Egg Quality Split</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="h-56">
                {gradeData.length === 0 ? (
                  <p className="text-sm text-slate-500 text-center py-12">No egg data yet.</p>
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie data={gradeData} dataKey="value" nameKey="name" innerRadius={50} outerRadius={70} paddingAngle={4}>
                        {gradeData.map((entry, i) => (
                          <Cell key={i} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </EnterprisePage>
  );
};

export default EggAnalytics;
