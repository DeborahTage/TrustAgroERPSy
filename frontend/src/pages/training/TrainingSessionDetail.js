import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { trainingApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatDate } from '../../utils';
import { toast } from 'react-toastify';

const TrainingSessionDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [roster, setRoster] = useState(null);
  const [sessionDate, setSessionDate] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async (date) => {
    setLoading(true);
    setError(null);
    try {
      const params = date ? { date } : {};
      const res = await trainingApi.getSessionRoster(id, params);
      const data = res.data.data;
      setRoster(data);
      if (!date && data.sessionDate) {
        setSessionDate(data.sessionDate);
      }
    } catch {
      setError('Failed to load session roster.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const mark = async (registrationId, status) => {
    try {
      await trainingApi.recordAttendance(registrationId, {
        sessionDate: sessionDate || roster?.sessionDate,
        status,
      });
      toast.success(`Marked ${status}`);
      load(sessionDate || roster?.sessionDate);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Attendance update failed');
    }
  };

  const session = roster?.session;

  return (
    <EnterprisePage
      title={session?.sessionCode || 'Session'}
      subtitle={session ? `${session.programTitle} · ${session.confirmedCount}/${session.capacity} confirmed` : ''}
      breadcrumbs={['Training', 'Sessions', session?.sessionCode]}
      loading={loading}
      error={error}
      onRetry={() => load(sessionDate)}
      onRefresh={() => load(sessionDate)}
      actions={
        <Button size="sm" variant="outline" onClick={() => navigate(`/training/programs/${session?.programId}`)}>
          Program
        </Button>
      }
    >
      {session && (
        <div className="space-y-6">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <Card className="border-none shadow-sm"><CardContent className="pt-4"><p className="text-slate-500 text-xs">Capacity</p><p className="text-xl font-bold">{roster.capacity}</p></CardContent></Card>
            <Card className="border-none shadow-sm"><CardContent className="pt-4"><p className="text-slate-500 text-xs">Confirmed</p><p className="text-xl font-bold">{roster.confirmedCount}</p></CardContent></Card>
            <Card className="border-none shadow-sm"><CardContent className="pt-4"><p className="text-slate-500 text-xs">Remaining</p><p className="text-xl font-bold text-emerald-700">{roster.remainingCapacity}</p></CardContent></Card>
            <Card className="border-none shadow-sm"><CardContent className="pt-4"><p className="text-slate-500 text-xs">Venue</p><p className="font-semibold">{session.venue || '—'}</p></CardContent></Card>
          </div>

          <Card className="border-none shadow-sm">
            <CardHeader className="flex flex-row items-end justify-between gap-4">
              <div>
                <CardTitle className="text-base">Attendance Roster</CardTitle>
                <CardDescription>Participant · Status · Recorded by · Time</CardDescription>
              </div>
              <div className="space-y-1">
                <Label className="text-xs">Session date</Label>
                <Input type="date" value={sessionDate} onChange={(e) => {
                  setSessionDate(e.target.value);
                  load(e.target.value);
                }} />
              </div>
            </CardHeader>
            <CardContent className="space-y-2">
              {(roster.participants || []).length === 0 ? (
                <p className="text-sm text-slate-500 text-center py-8">No confirmed participants.</p>
              ) : (roster.participants || []).map((p) => (
                <div key={p.registrationId} className="flex flex-wrap items-center justify-between gap-3 border rounded-lg p-3">
                  <div>
                    <p className="font-semibold text-sm">{p.participantName}</p>
                    <p className="text-xs text-slate-500 font-mono">{p.registrationNumber}</p>
                    <p className="text-[10px] text-slate-400">
                      {p.recordedBy ? `By ${p.recordedBy}` : 'Not recorded'}
                      {p.checkInTime ? ` · ${new Date(p.checkInTime).toLocaleString()}` : ''}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant="outline">{p.attendanceStatus || '—'}</Badge>
                    <Badge variant="outline">{p.paymentStatus}</Badge>
                    {hasPermission(PERMISSIONS.TRAINING_ATTENDANCE) && (
                      <Select onValueChange={(v) => mark(p.registrationId, v)}>
                        <SelectTrigger className="w-32 h-8"><SelectValue placeholder="Mark" /></SelectTrigger>
                        <SelectContent>
                          {['PRESENT', 'ABSENT', 'EXCUSED'].map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
                        </SelectContent>
                      </Select>
                    )}
                    <Button size="sm" variant="ghost" onClick={() => navigate(`/training/registrations/${p.registrationId}`)}>Open</Button>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>
          <p className="text-xs text-slate-400">Dates: {formatDate(session.startDate)} → {formatDate(session.endDate)}</p>
        </div>
      )}
    </EnterprisePage>
  );
};

export default TrainingSessionDetail;
