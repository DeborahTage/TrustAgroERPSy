import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { crmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import { Button } from '../../components/ui/button';
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

const STAGES = ['PROSPECTING', 'QUALIFICATION', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST'];
const emptyForm = {
  clientId: '', title: '', description: '', estimatedValue: '', currency: 'ETB', probability: '50',
  expectedCloseDate: '', priority: 'MEDIUM', notes: '',
};

const CrmOpportunities = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [stageFilter, setStageFilter] = useState('ALL');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = stageFilter !== 'ALL' ? { stage: stageFilter } : undefined;
      const [oppRes, clientRes] = await Promise.all([
        crmApi.getOpportunities(params),
        crmApi.getClients().catch(() => ({ data: { data: [] } })),
      ]);
      setRows(oppRes.data.data || []);
      setClients(clientRes.data?.data || []);
    } catch {
      setError('Failed to load opportunities.');
    } finally {
      setLoading(false);
    }
  }, [stageFilter]);

  useEffect(() => { load(); }, [load]);

  const create = async (e) => {
    e.preventDefault();
    try {
      await crmApi.createOpportunity({
        clientId: Number(form.clientId),
        title: form.title,
        description: form.description || undefined,
        estimatedValue: form.estimatedValue ? parseFloat(form.estimatedValue) : undefined,
        currency: form.currency || 'ETB',
        probability: form.probability ? parseInt(form.probability, 10) : undefined,
        expectedCloseDate: form.expectedCloseDate || undefined,
        priority: form.priority,
        notes: form.notes || undefined,
      });
      toast.success('Opportunity created');
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
    return `${r.opportunityNumber || ''} ${r.title || ''} ${r.clientName || ''}`.toLowerCase().includes(q);
  }), [rows, search]);

  const cols = [
    { accessorKey: 'opportunityNumber', header: 'Opp #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.opportunityNumber}</span> },
    { accessorKey: 'title', header: 'Title' },
    { accessorKey: 'clientName', header: 'Customer' },
    { accessorKey: 'estimatedValue', header: 'Value', cell: ({ row }) => formatCurrency(row.original.estimatedValue) },
    { accessorKey: 'expectedCloseDate', header: 'Close', cell: ({ row }) => formatDate(row.original.expectedCloseDate) },
    { accessorKey: 'stage', header: 'Stage', cell: ({ row }) => <StatusBadge status={row.original.stage} /> },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <Button size="sm" variant="outline" onClick={() => navigate(`/crm/opportunities/${row.original.id}`)}>Open</Button>
      ),
    },
  ];

  return (
    <EnterprisePage
      title="Opportunities"
      subtitle="Sales opportunities by stage and expected value"
      breadcrumbs={['CRM', 'Opportunities']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={hasPermission(PERMISSIONS.CRM_OPPORTUNITY) && (
        <Button size="sm" onClick={() => setOpen(true)}><Plus className="h-4 w-4 mr-1" /> New Opportunity</Button>
      )}
    >
      <div className="mb-4 flex flex-wrap gap-3">
        <Input className="max-w-xs" placeholder="Search…" value={search} onChange={(e) => setSearch(e.target.value)} />
        <Select value={stageFilter} onValueChange={setStageFilter}>
          <SelectTrigger className="w-48"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All stages</SelectItem>
            {STAGES.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
          </SelectContent>
        </Select>
      </div>
      {filtered.length === 0 && !loading ? (
        <p className="text-sm text-slate-500 py-10 text-center">No opportunities yet.</p>
      ) : (
        <DataTable columns={cols} data={filtered} />
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>Create Opportunity</DialogTitle></DialogHeader>
          <form onSubmit={create} className="space-y-3">
            <div className="space-y-1">
              <Label>Customer</Label>
              <Select value={form.clientId} onValueChange={(v) => setForm({ ...form, clientId: v })}>
                <SelectTrigger><SelectValue placeholder="Select client" /></SelectTrigger>
                <SelectContent>
                  {clients.map((c) => (
                    <SelectItem key={c.id} value={String(c.id)}>{c.clientName || c.name || `#${c.id}`}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1"><Label>Title</Label><Input required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></div>
            <div className="space-y-1"><Label>Description</Label><Textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Estimated Value</Label><Input type="number" step="0.01" value={form.estimatedValue} onChange={(e) => setForm({ ...form, estimatedValue: e.target.value })} /></div>
              <div className="space-y-1"><Label>Probability %</Label><Input type="number" value={form.probability} onChange={(e) => setForm({ ...form, probability: e.target.value })} /></div>
            </div>
            <div className="space-y-1"><Label>Expected Close</Label><Input type="date" value={form.expectedCloseDate} onChange={(e) => setForm({ ...form, expectedCloseDate: e.target.value })} /></div>
            <DialogFooter><Button type="submit" disabled={!form.clientId}>Create</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default CrmOpportunities;
