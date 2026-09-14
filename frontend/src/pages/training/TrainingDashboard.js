import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { trainingApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { formatCurrency, formatDate } from '../../utils';
import { GraduationCap, Users, Calendar, Award, AlertCircle, DollarSign } from 'lucide-react';

const TrainingDashboard = () => {
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await trainingApi.getDashboard();
      setData(res.data.data);
    } catch {
      setError('Failed to load training dashboard.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const stats = data ? [
    { title: 'Active Programs', value: data.activePrograms || 0, icon: GraduationCap, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Confirmed Participants', value: data.confirmedParticipants || 0, icon: Users, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: 'Unpaid / Partial', value: data.unpaidRegistrations || 0, icon: AlertCircle, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: 'Training Revenue', value: formatCurrency(data.revenue || 0), icon: DollarSign, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: 'Completed', value: data.completedTrainings || 0, icon: Calendar, iconColor: 'text-indigo-600', bgColor: 'bg-indigo-50' },
    { title: 'Certificates Issued', value: data.certificatesIssued || 0, icon: Award, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
  ] : [];

  return (
    <EnterprisePage
      title="Training & Workshops"
      subtitle="Programs, cohorts, registrations, attendance, and certificates — live ledger data"
      breadcrumbs={['Training']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      stats={stats}
      actions={
        <>
          <Button variant="outline" size="sm" onClick={() => navigate('/training/programs')}>Programs</Button>
          <Button size="sm" onClick={() => navigate('/training/registrations')}>Registrations</Button>
        </>
      }
    >
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Upcoming Sessions</CardTitle>
            <CardDescription>From persisted session schedules</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            {(data?.upcomingSessions || []).length === 0 ? (
              <p className="text-sm text-slate-500 py-6 text-center">No upcoming sessions.</p>
            ) : (data.upcomingSessions || []).map((s) => (
              <div key={s.id} className="flex justify-between items-center border rounded-lg p-3">
                <div>
                  <p className="font-semibold text-sm">{s.sessionCode}</p>
                  <p className="text-xs text-slate-500">{s.programTitle} · {formatDate(s.startDate)}</p>
                </div>
                <div className="text-right">
                  <Badge variant="outline">{s.confirmedCount}/{s.capacity}</Badge>
                  <p className="text-[10px] text-slate-400 mt-1">{s.remainingCapacity} seats left</p>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>

        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Collections Snapshot</CardTitle>
            <CardDescription>Revenue from Finance INCOME (source TRAINING)</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <div className="flex justify-between border-b py-2"><span>Recognized revenue</span><span className="font-mono font-bold">{formatCurrency(data?.revenue || 0)}</span></div>
            <div className="flex justify-between border-b py-2"><span>Paid collected (registrations)</span><span className="font-mono">{formatCurrency(data?.paidCollected || 0)}</span></div>
            <div className="flex justify-between border-b py-2"><span>Outstanding AR</span><span className="font-mono text-amber-700">{formatCurrency(data?.outstandingAmount || 0)}</span></div>
            <p className="text-[11px] text-slate-400 pt-2">Cash receipts settle AR and are not counted again as revenue.</p>
          </CardContent>
        </Card>
      </div>
    </EnterprisePage>
  );
};

export default TrainingDashboard;
