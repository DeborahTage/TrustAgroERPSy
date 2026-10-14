import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Activity, AlertCircle, Calendar, ClipboardList,
  HeartPulse, ShieldCheck, Stethoscope, Pill,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../../components/ui/table';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { vetApi } from '../../api';

const VeterinaryDashboard = () => {
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [prescriptions, setPrescriptions] = useState([]);
  const [vaccinations, setVaccinations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchAll = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [statsRes, rxRes, vacRes] = await Promise.all([
        vetApi.getDashboardStats(),
        vetApi.getPrescriptions(),
        vetApi.getVaccinations(),
      ]);
      setData(statsRes.data?.data || {});
      setPrescriptions((rxRes.data?.data || []).slice(0, 10));
      setVaccinations(vacRes.data?.data || []);
    } catch {
      setError('Failed to load veterinary dashboard.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchAll(); }, [fetchAll]);

  const today = new Date().toISOString().split('T')[0];
  const todayVaccinations = vaccinations.filter((v) => v.scheduledDate === today);
  const mortalityTrend = (data?.mortalityTrend || []).map((p) => ({
    day: p.date,
    rate: p.mortality,
  }));
  const recentCases = data?.recentDiseaseCases || [];

  const stats = data ? [
    { title: 'Active Disease Cases', value: data.activeDiseaseCases || 0, icon: Activity, iconColor: 'text-rose-600 dark:text-rose-400', bgColor: 'bg-rose-500/10 dark:bg-rose-500/20', description: 'Open pathologies' },
    { title: 'Vaccinations Due (7d)', value: data.vaccinationsDueThisWeek || 0, icon: Calendar, iconColor: 'text-emerald-600 dark:text-emerald-400', bgColor: 'bg-emerald-500/10 dark:bg-emerald-500/20', description: 'Scheduled this week' },
    { title: 'Mortality Rate (7d)', value: `${data.mortalityRate7Day || 0}%`, icon: HeartPulse, iconColor: 'text-primary', bgColor: 'bg-primary/10 dark:bg-primary/20', description: 'Rolling average' },
    { title: 'Drug Stock Alerts', value: data.drugStockAlerts || 0, icon: ShieldCheck, iconColor: 'text-amber-600 dark:text-amber-400', bgColor: 'bg-amber-500/10 dark:bg-amber-500/20', description: 'Below minimum stock' },
  ] : [];

  return (
    <EnterprisePage
      title="Veterinary Command Center"
      subtitle="Real-time biological health monitoring and outbreak management"
      breadcrumbs={['Veterinary', 'Dashboard']}
      loading={loading}
      error={error}
      onRetry={fetchAll}
      onRefresh={fetchAll}
      stats={stats}
      actions={
        <>
          <Button variant="outline" onClick={() => navigate('/veterinary')}>
            <Stethoscope className="w-4 h-4 mr-2" /> Clinical Hub
          </Button>
          <Button className="bg-rose-600 hover:bg-rose-700 text-white" onClick={() => navigate('/veterinary/health-reports/new')}>
            <AlertCircle className="w-4 h-4 mr-2" /> Report Outbreak
          </Button>
        </>
      }
    >
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-2 erp-card">
          <CardHeader>
            <CardTitle>Mortality Trend (30 days)</CardTitle>
            <CardDescription>Daily mortality from farm records</CardDescription>
          </CardHeader>
          <CardContent className="h-[280px]">
            {mortalityTrend.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={mortalityTrend}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="currentColor" className="stroke-border/40" />
                  <XAxis dataKey="day" tick={{ fontSize: 11, fill: 'currentColor' }} className="text-muted-foreground" />
                  <YAxis tick={{ fontSize: 11, fill: 'currentColor' }} className="text-muted-foreground" />
                  <Tooltip contentStyle={{ borderRadius: '1rem', border: '1px solid hsl(var(--border))', backgroundColor: 'hsl(var(--card))', color: 'hsl(var(--foreground))' }} />
                  <Area type="monotone" dataKey="rate" stroke="#6366f1" fill="#6366f1" fillOpacity={0.2} />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-sm text-muted-foreground text-center py-16">No mortality data in the selected period.</p>
            )}
          </CardContent>
        </Card>

        <Card className="erp-card">
          <CardHeader>
            <CardTitle className="text-sm flex items-center gap-2">
              <AlertCircle className="w-4 h-4 text-rose-500" /> Active Outbreaks
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {recentCases.length > 0 ? recentCases.map((c) => (
              <div key={c.id} className="flex justify-between items-center p-3 rounded-lg border border-border bg-card">
                <div>
                  <p className="font-semibold text-sm text-foreground">{c.suspectedDisease || c.diseaseName || 'Case'}</p>
                  <p className="text-xs text-muted-foreground">Flock: {c.batchCode || c.flockId}</p>
                </div>
                <Badge className={c.severity === 'HIGH' ? 'bg-rose-500/20 text-rose-600 dark:text-rose-400 border-none' : 'bg-amber-500/20 text-amber-600 dark:text-amber-400 border-none'}>
                  {c.severity || 'ACTIVE'}
                </Badge>
              </div>
            )) : (
              <p className="text-sm text-emerald-600 dark:text-emerald-400 text-center py-6">No active disease cases</p>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="erp-card">
          <CardHeader className="flex flex-row items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <ClipboardList className="h-5 w-5 text-primary" /> Prescription Queue
              </CardTitle>
              <CardDescription>Recent medical orders</CardDescription>
            </div>
            <Button size="sm" variant="outline" onClick={() => navigate('/veterinary/prescriptions/new')}>New Rx</Button>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Drug</TableHead>
                  <TableHead>Qty</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {prescriptions.length > 0 ? prescriptions.map((rx) => (
                  <TableRow key={rx.id}>
                    <TableCell className="font-medium text-foreground">{rx.drugName}</TableCell>
                    <TableCell>{rx.quantity}</TableCell>
                    <TableCell><Badge variant="outline">{rx.status}</Badge></TableCell>
                  </TableRow>
                )) : (
                  <TableRow><TableCell colSpan={3} className="text-center text-muted-foreground">No prescriptions yet</TableCell></TableRow>
                )}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        <Card className="erp-card">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Calendar className="h-5 w-5 text-emerald-600" /> Today&apos;s Vaccinations
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {todayVaccinations.length > 0 ? todayVaccinations.map((v) => (
              <div key={v.id} className="flex justify-between items-center p-3 rounded-lg bg-muted/40 border border-border">
                <div>
                  <p className="font-semibold text-sm text-foreground">{v.vaccineName}</p>
                  <p className="text-xs text-muted-foreground">{v.batchCode || v.farmName}</p>
                </div>
                <Badge>{v.status}</Badge>
              </div>
            )) : (
              <p className="text-sm text-muted-foreground text-center py-6">No vaccinations scheduled for today</p>
            )}
            <Button variant="link" className="w-full" onClick={() => navigate('/vet/vaccinations')}>View full calendar</Button>
          </CardContent>
        </Card>
      </div>

      {(data?.alerts || []).length > 0 && (
        <Card className="rounded-2xl border border-amber-500/30 bg-amber-500/10">
          <CardHeader><CardTitle className="text-amber-800 dark:text-amber-300">System Alerts</CardTitle></CardHeader>
          <CardContent className="space-y-2">
            {data.alerts.map((a, i) => (
              <div key={i} className="flex items-start gap-2 text-sm text-amber-900 dark:text-amber-200">
                <Pill className="h-4 w-4 mt-0.5" />
                <div><strong>{a.title}</strong> — {a.description}</div>
              </div>
            ))}
          </CardContent>
        </Card>
      )}
    </EnterprisePage>
  );
};

export default VeterinaryDashboard;
