import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { trainingApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../../components/ui/dialog';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatCurrency, formatDate } from '../../utils';
import { toast } from 'react-toastify';

const TrainingProgramDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [program, setProgram] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [sessionOpen, setSessionOpen] = useState(false);
  const [sessionForm, setSessionForm] = useState({ sessionCode: '', startDate: '', endDate: '', venue: '', capacity: 30, trainerName: '' });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [p, s] = await Promise.all([
        trainingApi.getProgram(id),
        trainingApi.getSessions({ programId: id }),
      ]);
      setProgram(p.data.data);
      setSessions(s.data.data || []);
    } catch {
      setError('Failed to load program.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const act = async (fn, ok) => {
    try {
      await fn();
      toast.success(ok);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    }
  };

  const createSession = async (e) => {
    e.preventDefault();
    try {
      await trainingApi.createSession({
        programId: Number(id),
        sessionCode: sessionForm.sessionCode || undefined,
        startDate: sessionForm.startDate,
        endDate: sessionForm.endDate || sessionForm.startDate,
        venue: sessionForm.venue,
        capacity: parseInt(sessionForm.capacity, 10),
        trainerName: sessionForm.trainerName || undefined,
      });
      toast.success('Session created');
      setSessionOpen(false);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create session');
    }
  };

  if (!program && !loading) {
    return <EnterprisePage title="Program" error={error || 'Not found'} breadcrumbs={['Training']} />;
  }

  return (
    <EnterprisePage
      title={program?.title || 'Program'}
      subtitle={`${program?.programCode || ''} · ${program?.status || ''}`}
      breadcrumbs={['Training', 'Programs', program?.programCode]}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={
        <div className="flex flex-wrap gap-2">
          {hasPermission(PERMISSIONS.PUBLISH_TRAINING) && program?.status === 'DRAFT' && (
            <Button size="sm" onClick={() => act(() => trainingApi.publishProgram(id), 'Published')}>Publish</Button>
          )}
          {hasPermission(PERMISSIONS.PUBLISH_TRAINING) && program?.status === 'PUBLISHED' && (
            <Button size="sm" onClick={() => act(() => trainingApi.openRegistration(id), 'Registration opened')}>Open Registration</Button>
          )}
          {hasPermission(PERMISSIONS.PUBLISH_TRAINING) && program?.status === 'REGISTRATION_OPEN' && (
            <Button size="sm" variant="outline" onClick={() => act(() => trainingApi.closeRegistration(id), 'Registration closed')}>Close Registration</Button>
          )}
          {hasPermission(PERMISSIONS.MANAGE_TRAINING) && (
            <Button size="sm" variant="outline" onClick={() => setSessionOpen(true)}>Add Session</Button>
          )}
        </div>
      }
    >
      {program && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <Card className="lg:col-span-1 border-none shadow-sm">
            <CardHeader><CardTitle className="text-base">Program Details</CardTitle></CardHeader>
            <CardContent className="space-y-2 text-sm">
              <div className="flex justify-between"><span>Category</span><span>{program.category}</span></div>
              <div className="flex justify-between"><span>Mode</span><span>{program.deliveryMode}</span></div>
              <div className="flex justify-between"><span>Price</span><span className="font-mono">{formatCurrency(program.price)}</span></div>
              <div className="flex justify-between"><span>Trainer</span><span>{program.trainerName || '—'}</span></div>
              <div className="flex justify-between"><span>Location</span><span>{program.location || '—'}</span></div>
              <div className="flex justify-between"><span>Start</span><span>{formatDate(program.startDate) || '—'}</span></div>
              <div className="flex justify-between"><span>Deadline</span><span>{formatDate(program.registrationDeadline) || '—'}</span></div>
              <Badge className="mt-2">{program.status}</Badge>
              {program.description && <p className="text-xs text-slate-500 pt-3 border-t">{program.description}</p>}
            </CardContent>
          </Card>

          <Card className="lg:col-span-2 border-none shadow-sm">
            <CardHeader><CardTitle className="text-base">Sessions / Cohorts</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              {sessions.length === 0 ? (
                <p className="text-sm text-slate-500 text-center py-8">No sessions yet.</p>
              ) : sessions.map((s) => (
                <div key={s.id} className="flex items-center justify-between border rounded-lg p-3">
                  <div>
                    <p className="font-mono font-bold text-sm">{s.sessionCode}</p>
                    <p className="text-xs text-slate-500">{formatDate(s.startDate)} → {formatDate(s.endDate)} · {s.venue || '—'}</p>
                  </div>
                  <div className="text-right space-y-1">
                    <Badge variant="outline">{s.confirmedCount}/{s.capacity} confirmed</Badge>
                    <div>
                      <Button size="sm" variant="ghost" onClick={() => navigate(`/training/registrations?sessionId=${s.id}`)}>
                        Registrations
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>
      )}

      <Dialog open={sessionOpen} onOpenChange={setSessionOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>New Session / Cohort</DialogTitle></DialogHeader>
          <form onSubmit={createSession} className="space-y-3">
            <div className="space-y-1"><Label>Session code (optional)</Label><Input value={sessionForm.sessionCode} onChange={(e) => setSessionForm({ ...sessionForm, sessionCode: e.target.value })} placeholder="APFM-SEP-2026" /></div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Start</Label><Input type="date" required value={sessionForm.startDate} onChange={(e) => setSessionForm({ ...sessionForm, startDate: e.target.value })} /></div>
              <div className="space-y-1"><Label>End</Label><Input type="date" value={sessionForm.endDate} onChange={(e) => setSessionForm({ ...sessionForm, endDate: e.target.value })} /></div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Capacity</Label><Input type="number" required value={sessionForm.capacity} onChange={(e) => setSessionForm({ ...sessionForm, capacity: e.target.value })} /></div>
              <div className="space-y-1"><Label>Venue</Label><Input value={sessionForm.venue} onChange={(e) => setSessionForm({ ...sessionForm, venue: e.target.value })} /></div>
            </div>
            <DialogFooter><Button type="submit">Create Session</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default TrainingProgramDetail;
