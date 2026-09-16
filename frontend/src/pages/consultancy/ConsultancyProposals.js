import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { consultancyApi, crmApi } from '../../api';
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
import { formatCurrency, formatDate } from '../../utils';
import { Plus } from 'lucide-react';
import { toast } from 'react-toastify';

const emptyForm = {
  customerId: '', requestId: '', serviceId: '', title: '', scope: '', proposedAmount: '', currency: 'ETB', validUntil: '', notes: '',
};

const ConsultancyProposals = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [clients, setClients] = useState([]);
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [propRes, clientRes, svcRes] = await Promise.all([
        consultancyApi.getProposals(),
        crmApi.getClients().catch(() => ({ data: { data: [] } })),
        consultancyApi.getServices({ activeOnly: true }).catch(() => ({ data: { data: [] } })),
      ]);
      setRows(propRes.data.data || []);
      setClients(clientRes.data?.data || []);
      setServices(svcRes.data?.data || []);
    } catch {
      setError('Failed to load proposals.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const act = async (fn, msg) => {
    try {
      const res = await fn();
      toast.success(msg);
      if (res?.data?.data?.convertedEngagementId) {
        navigate(`/consultancy/engagements/${res.data.data.convertedEngagementId}`);
      } else {
        load();
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    }
  };

  const create = async (e) => {
    e.preventDefault();
    try {
      await consultancyApi.createProposal({
        customerId: Number(form.customerId),
        requestId: form.requestId ? Number(form.requestId) : undefined,
        serviceId: form.serviceId ? Number(form.serviceId) : undefined,
        title: form.title,
        scope: form.scope || undefined,
        proposedAmount: parseFloat(form.proposedAmount) || 0,
        currency: form.currency || 'ETB',
        validUntil: form.validUntil || undefined,
        notes: form.notes || undefined,
      });
      toast.success('Proposal created');
      setOpen(false);
      setForm(emptyForm);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Create failed');
    }
  };

  const filtered = useMemo(() => rows.filter((r) => {
    if (!search.trim()) return true;
    const q = search.trim().toLowerCase();
    return `${r.proposalNumber || ''} ${r.title || ''} ${r.customerName || ''}`.toLowerCase().includes(q);
  }), [rows, search]);

  const cols = [
    { accessorKey: 'proposalNumber', header: 'Proposal #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.proposalNumber}</span> },
    { accessorKey: 'title', header: 'Title' },
    { accessorKey: 'customerName', header: 'Customer' },
    { accessorKey: 'proposedAmount', header: 'Amount', cell: ({ row }) => formatCurrency(row.original.proposedAmount) },
    { accessorKey: 'validUntil', header: 'Valid Until', cell: ({ row }) => formatDate(row.original.validUntil) },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge>,
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => {
        const p = row.original;
        return (
          <div className="flex flex-wrap gap-1 justify-end">
            {hasPermission(PERMISSIONS.EDIT_CONSULTANCY) && p.status === 'DRAFT' && (
              <Button size="sm" variant="outline" onClick={() => act(() => consultancyApi.sendProposal(p.id), 'Sent')}>Send</Button>
            )}
            {(hasPermission(PERMISSIONS.APPROVE_CONSULTANCY) || hasPermission(PERMISSIONS.EDIT_CONSULTANCY)) && p.status === 'SENT' && (
              <Button size="sm" onClick={() => act(() => consultancyApi.acceptProposal(p.id), 'Accepted')}>Accept</Button>
            )}
            {hasPermission(PERMISSIONS.EDIT_CONSULTANCY) && ['DRAFT', 'SENT'].includes(p.status) && (
              <Button size="sm" variant="destructive" onClick={() => {
                if (!window.confirm('Reject this proposal?')) return;
                act(() => consultancyApi.rejectProposal(p.id, { completionNotes: 'Rejected' }), 'Rejected');
              }}>Reject</Button>
            )}
            {p.convertedEngagementId && (
              <Button size="sm" variant="outline" onClick={() => navigate(`/consultancy/engagements/${p.convertedEngagementId}`)}>Engagement</Button>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <EnterprisePage
      title="Consultancy Proposals"
      subtitle="Quoted scope and amounts for advisory work"
      breadcrumbs={['Consultancy', 'Proposals']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={hasPermission(PERMISSIONS.MANAGE_CONSULTANCY) && (
        <Button size="sm" onClick={() => setOpen(true)}><Plus className="h-4 w-4 mr-1" /> New Proposal</Button>
      )}
    >
      <div className="mb-4">
        <Input className="max-w-xs" placeholder="Search…" value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>
      {filtered.length === 0 && !loading ? (
        <p className="text-sm text-slate-500 py-10 text-center">No proposals yet.</p>
      ) : (
        <DataTable columns={cols} data={filtered} />
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>Create Proposal</DialogTitle></DialogHeader>
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
            <div className="space-y-1"><Label>Title</Label><Input required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></div>
            <div className="space-y-1"><Label>Scope</Label><Textarea value={form.scope} onChange={(e) => setForm({ ...form, scope: e.target.value })} /></div>
            <div className="space-y-1">
              <Label>Service</Label>
              <Select value={form.serviceId || 'none'} onValueChange={(v) => setForm({ ...form, serviceId: v === 'none' ? '' : v })}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">None</SelectItem>
                  {services.map((s) => <SelectItem key={s.id} value={String(s.id)}>{s.name}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Proposed Amount</Label><Input type="number" step="0.01" required value={form.proposedAmount} onChange={(e) => setForm({ ...form, proposedAmount: e.target.value })} /></div>
              <div className="space-y-1"><Label>Currency</Label><Input value={form.currency} onChange={(e) => setForm({ ...form, currency: e.target.value })} /></div>
            </div>
            <div className="space-y-1"><Label>Valid Until</Label><Input type="date" value={form.validUntil} onChange={(e) => setForm({ ...form, validUntil: e.target.value })} /></div>
            <DialogFooter><Button type="submit" disabled={!form.customerId}>Create</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default ConsultancyProposals;
