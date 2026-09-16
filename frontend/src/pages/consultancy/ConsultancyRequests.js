import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { consultancyApi, crmApi, userApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../../components/ui/dialog';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatDate } from '../../utils';
import { Plus } from 'lucide-react';
import { toast } from 'react-toastify';

const STATUSES = ['NEW', 'QUALIFYING', 'ASSIGNED', 'PROPOSAL_REQUIRED', 'CONVERTED', 'REJECTED', 'CANCELLED'];
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

const emptyForm = {
  customerId: '', subject: '', description: '', serviceId: '', requestedDate: '', preferredLocation: '', priority: 'MEDIUM', notes: '',
};

const ConsultancyRequests = () => {
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [clients, setClients] = useState([]);
  const [services, setServices] = useState([]);
  const [consultants, setConsultants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [open, setOpen] = useState(false);
  const [assignOpen, setAssignOpen] = useState(false);
  const [assignId, setAssignId] = useState(null);
  const [consultantId, setConsultantId] = useState('');
  const [form, setForm] = useState(emptyForm);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [reqRes, clientRes, svcRes, userRes] = await Promise.all([
        consultancyApi.getRequests(),
        crmApi.getClients().catch(() => ({ data: { data: [] } })),
        consultancyApi.getServices({ activeOnly: true }).catch(() => ({ data: { data: [] } })),
        userApi.getAll().catch(() => ({ data: { data: [] } })),
      ]);
      setRows(reqRes.data.data || []);
      setClients(clientRes.data?.data || []);
      setServices(svcRes.data?.data || []);
      const users = userRes.data?.data || userRes.data || [];
      setConsultants((Array.isArray(users) ? users : []).filter((u) => {
        const role = u.role || u.roles?.[0];
        return role === 'CONSULTANT' || role === 'CRM_MANAGER' || role === 'ADMIN';
      }));
    } catch {
      setError('Failed to load requests.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const act = async (fn, msg) => {
    try {
      await fn();
      toast.success(msg);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    }
  };

  const create = async (e) => {
    e.preventDefault();
    try {
      await consultancyApi.createRequest({
        customerId: Number(form.customerId),
        subject: form.subject,
        description: form.description || undefined,
        serviceId: form.serviceId ? Number(form.serviceId) : undefined,
        requestedDate: form.requestedDate || undefined,
        preferredLocation: form.preferredLocation || undefined,
        priority: form.priority,
        notes: form.notes || undefined,
      });
      toast.success('Request created');
      setOpen(false);
      setForm(emptyForm);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Create failed');
    }
  };

  const submitAssign = async (e) => {
    e.preventDefault();
    await act(() => consultancyApi.assignRequest(assignId, { consultantId: Number(consultantId), asLead: true }), 'Assigned');
    setAssignOpen(false);
    setConsultantId('');
  };

  const filtered = useMemo(() => rows.filter((r) => {
    if (statusFilter !== 'all' && r.status !== statusFilter) return false;
    if (search.trim()) {
      const q = search.trim().toLowerCase();
      const hay = `${r.requestNumber || ''} ${r.subject || ''} ${r.customerName || ''}`.toLowerCase();
      if (!hay.includes(q)) return false;
    }
    return true;
  }), [rows, statusFilter, search]);

  const cols = [
    { accessorKey: 'requestNumber', header: 'Request #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.requestNumber}</span> },
    { accessorKey: 'subject', header: 'Subject' },
    { accessorKey: 'customerName', header: 'Customer' },
    { accessorKey: 'serviceName', header: 'Service', cell: ({ row }) => row.original.serviceName || '—' },
    { accessorKey: 'priority', header: 'Priority' },
    { accessorKey: 'requestedDate', header: 'Requested', cell: ({ row }) => formatDate(row.original.requestedDate) },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge>,
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => {
        const r = row.original;
        const openStatuses = ['NEW', 'QUALIFYING', 'ASSIGNED', 'PROPOSAL_REQUIRED'];
        return (
          <div className="flex flex-wrap gap-1 justify-end">
            {hasPermission(PERMISSIONS.ASSIGN_CONSULTANCY) && openStatuses.includes(r.status) && (
              <Button size="sm" variant="outline" onClick={() => { setAssignId(r.id); setAssignOpen(true); }}>Assign</Button>
            )}
            {(hasPermission(PERMISSIONS.EDIT_CONSULTANCY) || hasPermission(PERMISSIONS.CANCEL_CONSULTANCY)) && openStatuses.includes(r.status) && (
              <Button size="sm" variant="destructive" onClick={() => {
                if (!window.confirm('Reject this request?')) return;
                act(() => consultancyApi.rejectRequest(r.id, { completionNotes: 'Rejected' }), 'Rejected');
              }}>Reject</Button>
            )}
            {hasPermission(PERMISSIONS.MANAGE_CONSULTANCY) && openStatuses.includes(r.status) && (
              <Button size="sm" variant="outline" onClick={() => act(() => consultancyApi.convertRequestToProposal(r.id), 'Converted to proposal')}>To Proposal</Button>
            )}
            {hasPermission(PERMISSIONS.MANAGE_CONSULTANCY) && ['ASSIGNED', 'PROPOSAL_REQUIRED'].includes(r.status) && (
              <Button size="sm" variant="outline" onClick={() => act(() => consultancyApi.convertRequestToEngagement(r.id), 'Converted to engagement')}>To Engagement</Button>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <EnterprisePage
      title="Consultancy Requests"
      subtitle="Inbound advisory requests from CRM customers"
      breadcrumbs={['Consultancy', 'Requests']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={hasPermission(PERMISSIONS.MANAGE_CONSULTANCY) && (
        <Button size="sm" onClick={() => setOpen(true)}><Plus className="h-4 w-4 mr-1" /> New Request</Button>
      )}
    >
      <div className="flex flex-wrap gap-3 mb-4">
        <Input className="max-w-xs" placeholder="Search…" value={search} onChange={(e) => setSearch(e.target.value)} />
        <Select value={statusFilter} onValueChange={setStatusFilter}>
          <SelectTrigger className="w-48"><SelectValue placeholder="Status" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All statuses</SelectItem>
            {STATUSES.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
          </SelectContent>
        </Select>
      </div>
      {filtered.length === 0 && !loading ? (
        <p className="text-sm text-slate-500 py-10 text-center">No requests found.</p>
      ) : (
        <DataTable columns={cols} data={filtered} />
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>Create Request</DialogTitle></DialogHeader>
          <form onSubmit={create} className="space-y-3">
            <div className="space-y-1">
              <Label>Customer</Label>
              <Select value={form.customerId} onValueChange={(v) => setForm({ ...form, customerId: v })}>
                <SelectTrigger><SelectValue placeholder="Select client" /></SelectTrigger>
                <SelectContent>
                  {clients.map((c) => (
                    <SelectItem key={c.id} value={String(c.id)}>{c.name || c.fullName || c.companyName || `#${c.id}`}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1"><Label>Subject</Label><Input required value={form.subject} onChange={(e) => setForm({ ...form, subject: e.target.value })} /></div>
            <div className="space-y-1"><Label>Description</Label><Textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
            <div className="space-y-1">
              <Label>Service</Label>
              <Select value={form.serviceId || 'none'} onValueChange={(v) => setForm({ ...form, serviceId: v === 'none' ? '' : v })}>
                <SelectTrigger><SelectValue placeholder="Optional" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">None</SelectItem>
                  {services.map((s) => <SelectItem key={s.id} value={String(s.id)}>{s.name}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Requested Date</Label><Input type="date" value={form.requestedDate} onChange={(e) => setForm({ ...form, requestedDate: e.target.value })} /></div>
              <div className="space-y-1">
                <Label>Priority</Label>
                <Select value={form.priority} onValueChange={(v) => setForm({ ...form, priority: v })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>{PRIORITIES.map((p) => <SelectItem key={p} value={p}>{p}</SelectItem>)}</SelectContent>
                </Select>
              </div>
            </div>
            <div className="space-y-1"><Label>Preferred Location</Label><Input value={form.preferredLocation} onChange={(e) => setForm({ ...form, preferredLocation: e.target.value })} /></div>
            <DialogFooter><Button type="submit" disabled={!form.customerId}>Create</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={assignOpen} onOpenChange={setAssignOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Assign Consultant</DialogTitle></DialogHeader>
          <form onSubmit={submitAssign} className="space-y-3">
            <div className="space-y-1">
              <Label>Consultant</Label>
              <Select value={consultantId} onValueChange={setConsultantId}>
                <SelectTrigger><SelectValue placeholder="Select user" /></SelectTrigger>
                <SelectContent>
                  {consultants.map((u) => (
                    <SelectItem key={u.id} value={String(u.id)}>{u.fullName || u.name || u.username || `#${u.id}`}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <DialogFooter><Button type="submit" disabled={!consultantId}>Assign</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default ConsultancyRequests;
