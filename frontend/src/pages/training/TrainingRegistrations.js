import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { trainingApi, crmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../../components/ui/dialog';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatCurrency, formatDate } from '../../utils';
import { Plus } from 'lucide-react';
import { toast } from 'react-toastify';

const REG_STATUS = {
  PENDING: 'bg-slate-100 text-slate-700',
  CONFIRMED: 'bg-blue-100 text-blue-700',
  COMPLETED: 'bg-emerald-100 text-emerald-700',
  CANCELLED: 'bg-rose-100 text-rose-700',
};

const TrainingRegistrations = () => {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const sessionId = params.get('sessionId');
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({
    sessionId: sessionId || '',
    clientId: '',
    participantName: '',
    participantPhone: '',
    participantEmail: '',
    organization: '',
    discountAmount: '0',
    applyTax: false,
  });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [regs, sess, crm] = await Promise.all([
        trainingApi.getRegistrations(sessionId ? { sessionId } : {}),
        trainingApi.getSessions(),
        crmApi.getClients().catch(() => ({ data: { data: [] } })),
      ]);
      setRows(regs.data.data || []);
      setSessions(sess.data.data || []);
      setClients(crm.data?.data || []);
    } catch {
      setError('Failed to load registrations.');
    } finally {
      setLoading(false);
    }
  }, [sessionId]);

  useEffect(() => { load(); }, [load]);

  const stats = useMemo(() => [
    { title: 'Total', value: rows.length },
    { title: 'Confirmed+', value: rows.filter((r) => ['CONFIRMED', 'COMPLETED'].includes(r.status)).length },
    { title: 'Outstanding', value: formatCurrency(rows.reduce((s, r) => s + (Number(r.outstandingAmount) || 0), 0)) },
  ], [rows]);

  const submit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        sessionId: Number(form.sessionId),
        clientId: form.clientId ? Number(form.clientId) : null,
        participantName: form.participantName,
        participantPhone: form.participantPhone || null,
        participantEmail: form.participantEmail || null,
        organization: form.organization || null,
        discountAmount: parseFloat(form.discountAmount) || 0,
        applyTax: !!form.applyTax,
      };
      const res = await trainingApi.register(payload);
      toast.success('Registration created');
      setOpen(false);
      navigate(`/training/registrations/${res.data.data.id}`);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Registration failed');
    }
  };

  const cols = [
    { accessorKey: 'registrationNumber', header: 'Reg #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.registrationNumber}</span> },
    { accessorKey: 'participantName', header: 'Participant' },
    { accessorKey: 'sessionCode', header: 'Session' },
    { accessorKey: 'programTitle', header: 'Program' },
    { accessorKey: 'registrationDate', header: 'Date', cell: ({ row }) => formatDate(row.original.registrationDate) },
    { accessorKey: 'totalAmount', header: 'Total', cell: ({ row }) => formatCurrency(row.original.totalAmount) },
    { accessorKey: 'outstandingAmount', header: 'Outstanding', cell: ({ row }) => formatCurrency(row.original.outstandingAmount) },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ row }) => <Badge className={`${REG_STATUS[row.original.status] || 'bg-slate-100'} shadow-none`}>{row.original.status}</Badge>,
    },
    { accessorKey: 'paymentStatus', header: 'Payment' },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <Button size="sm" variant="outline" onClick={() => navigate(`/training/registrations/${row.original.id}`)}>Open</Button>
      ),
    },
  ];

  return (
    <EnterprisePage
      title="Registrations & Participants"
      subtitle="CRM-linked enrollments with invoice and payment status"
      breadcrumbs={['Training', 'Registrations']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      stats={stats}
      actions={hasPermission(PERMISSIONS.REGISTER_TRAINING) && (
        <Button size="sm" onClick={() => setOpen(true)}><Plus className="h-4 w-4 mr-1" /> Register</Button>
      )}
    >
      <DataTable columns={cols} data={rows} />

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>New Registration</DialogTitle></DialogHeader>
          <form onSubmit={submit} className="space-y-3">
            <div className="space-y-1">
              <Label>Session</Label>
              <Select value={String(form.sessionId)} onValueChange={(v) => setForm({ ...form, sessionId: v })}>
                <SelectTrigger><SelectValue placeholder="Select session" /></SelectTrigger>
                <SelectContent>
                  {sessions.map((s) => (
                    <SelectItem key={s.id} value={String(s.id)}>{s.sessionCode} — {s.programTitle} ({s.remainingCapacity} left)</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label>CRM Client (optional)</Label>
              <Select value={form.clientId || 'new'} onValueChange={(v) => {
                if (v === 'new') {
                  setForm({ ...form, clientId: '', participantName: '' });
                } else {
                  const c = clients.find((x) => String(x.id) === v);
                  setForm({
                    ...form,
                    clientId: v,
                    participantName: c?.clientName || form.participantName,
                    participantPhone: c?.phone || form.participantPhone,
                  });
                }
              }}>
                <SelectTrigger><SelectValue placeholder="New or existing" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="new">Create / find by phone</SelectItem>
                  {clients.map((c) => <SelectItem key={c.id} value={String(c.id)}>{c.clientName}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1"><Label>Full name</Label><Input required value={form.participantName} onChange={(e) => setForm({ ...form, participantName: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Phone</Label><Input value={form.participantPhone} onChange={(e) => setForm({ ...form, participantPhone: e.target.value })} /></div>
              <div className="space-y-1"><Label>Email</Label><Input value={form.participantEmail} onChange={(e) => setForm({ ...form, participantEmail: e.target.value })} /></div>
            </div>
            <div className="space-y-1"><Label>Organization</Label><Input value={form.organization} onChange={(e) => setForm({ ...form, organization: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Discount</Label><Input type="number" step="0.01" value={form.discountAmount} onChange={(e) => setForm({ ...form, discountAmount: e.target.value })} /></div>
              <div className="flex items-end gap-2 pb-2">
                <input id="tax" type="checkbox" checked={form.applyTax} onChange={(e) => setForm({ ...form, applyTax: e.target.checked })} />
                <Label htmlFor="tax">Apply 15% VAT</Label>
              </div>
            </div>
            <DialogFooter><Button type="submit">Create Registration</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default TrainingRegistrations;
