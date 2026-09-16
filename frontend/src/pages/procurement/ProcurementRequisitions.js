import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { inventoryApi, procurementApi } from '../../api';
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
import { Plus, Trash2 } from 'lucide-react';
import { toast } from 'react-toastify';

const STATUSES = ['DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED', 'CONVERTED'];
const PRIORITIES = ['LOW', 'NORMAL', 'HIGH', 'URGENT'];

const emptyLine = () => ({ inventoryItemId: '', quantityRequested: '', estimatedUnitPrice: '', notes: '' });

const ProcurementRequisitions = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ title: '', justification: '', neededBy: '', priority: 'NORMAL', lines: [emptyLine()] });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [reqRes, itemRes] = await Promise.all([
        procurementApi.getRequisitions(statusFilter !== 'all' ? { status: statusFilter } : undefined),
        inventoryApi.getItems().catch(() => ({ data: { data: [] } })),
      ]);
      setRows(reqRes.data?.data || []);
      setItems(itemRes.data?.data || []);
    } catch {
      setError('Failed to load requisitions.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => { load(); }, [load]);

  const create = async (e) => {
    e.preventDefault();
    const lines = form.lines.filter((l) => l.inventoryItemId && l.quantityRequested);
    if (!lines.length) { toast.error('Add at least one line'); return; }
    try {
      await procurementApi.createRequisition({
        title: form.title,
        justification: form.justification || undefined,
        neededBy: form.neededBy || undefined,
        priority: form.priority,
        items: lines.map((l) => ({
          inventoryItemId: Number(l.inventoryItemId),
          quantityRequested: Number(l.quantityRequested),
          estimatedUnitPrice: l.estimatedUnitPrice ? Number(l.estimatedUnitPrice) : undefined,
          notes: l.notes || undefined,
        })),
      });
      toast.success('Requisition created');
      setOpen(false);
      setForm({ title: '', justification: '', neededBy: '', priority: 'NORMAL', lines: [emptyLine()] });
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Create failed');
    }
  };

  const filtered = useMemo(() => rows.filter((r) => {
    if (!search.trim()) return true;
    const q = search.trim().toLowerCase();
    return `${r.requisitionNumber || ''} ${r.title || ''} ${r.requestedByName || ''}`.toLowerCase().includes(q);
  }), [rows, search]);

  const cols = [
    {
      accessorKey: 'requisitionNumber',
      header: 'Req #',
      cell: ({ row }) => (
        <button type="button" className="font-mono font-bold text-primary" onClick={() => navigate(`/procurement/requisitions/${row.original.id}`)}>
          {row.original.requisitionNumber}
        </button>
      ),
    },
    { accessorKey: 'title', header: 'Title' },
    { accessorKey: 'priority', header: 'Priority' },
    { accessorKey: 'requestedByName', header: 'Requested By', cell: ({ row }) => row.original.requestedByName || '—' },
    { accessorKey: 'neededBy', header: 'Needed By', cell: ({ row }) => formatDate(row.original.neededBy) },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
  ];

  return (
    <EnterprisePage
      title="Requisitions"
      subtitle="Internal purchase requests awaiting review and conversion"
      breadcrumbs={['Procurement', 'Requisitions']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={hasPermission(PERMISSIONS.CREATE_PROCUREMENT) && (
        <Button size="sm" onClick={() => setOpen(true)}><Plus className="h-4 w-4 mr-1" /> New Requisition</Button>
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
        <p className="text-sm text-slate-500 py-10 text-center">No requisitions found.</p>
      ) : (
        <DataTable columns={cols} data={filtered} />
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>Create Requisition</DialogTitle></DialogHeader>
          <form onSubmit={create} className="space-y-3">
            <div className="space-y-1"><Label>Title</Label><Input required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></div>
            <div className="space-y-1"><Label>Justification</Label><Textarea value={form.justification} onChange={(e) => setForm({ ...form, justification: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Needed By</Label><Input type="date" value={form.neededBy} onChange={(e) => setForm({ ...form, neededBy: e.target.value })} /></div>
              <div className="space-y-1">
                <Label>Priority</Label>
                <Select value={form.priority} onValueChange={(v) => setForm({ ...form, priority: v })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>{PRIORITIES.map((p) => <SelectItem key={p} value={p}>{p}</SelectItem>)}</SelectContent>
                </Select>
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex justify-between items-center">
                <Label>Lines</Label>
                <Button type="button" size="sm" variant="outline" onClick={() => setForm({ ...form, lines: [...form.lines, emptyLine()] })}>
                  <Plus className="h-3 w-3 mr-1" /> Line
                </Button>
              </div>
              {form.lines.map((line, idx) => (
                <div key={idx} className="grid grid-cols-12 gap-2 items-end border rounded-lg p-2">
                  <div className="col-span-5 space-y-1">
                    <Label className="text-xs">Item</Label>
                    <Select value={line.inventoryItemId} onValueChange={(v) => {
                      const lines = [...form.lines];
                      lines[idx] = { ...lines[idx], inventoryItemId: v };
                      setForm({ ...form, lines });
                    }}>
                      <SelectTrigger><SelectValue placeholder="Select item" /></SelectTrigger>
                      <SelectContent>
                        {items.map((it) => (
                          <SelectItem key={it.id} value={String(it.id)}>{it.itemCode || it.code || ''} — {it.name}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="col-span-2 space-y-1">
                    <Label className="text-xs">Qty</Label>
                    <Input type="number" min="0.01" step="any" value={line.quantityRequested} onChange={(e) => {
                      const lines = [...form.lines];
                      lines[idx] = { ...lines[idx], quantityRequested: e.target.value };
                      setForm({ ...form, lines });
                    }} />
                  </div>
                  <div className="col-span-3 space-y-1">
                    <Label className="text-xs">Est. Unit Price</Label>
                    <Input type="number" min="0" step="any" value={line.estimatedUnitPrice} onChange={(e) => {
                      const lines = [...form.lines];
                      lines[idx] = { ...lines[idx], estimatedUnitPrice: e.target.value };
                      setForm({ ...form, lines });
                    }} />
                  </div>
                  <div className="col-span-2 flex justify-end">
                    <Button type="button" size="icon" variant="ghost" disabled={form.lines.length === 1} onClick={() => {
                      setForm({ ...form, lines: form.lines.filter((_, i) => i !== idx) });
                    }}><Trash2 className="h-4 w-4 text-rose-600" /></Button>
                  </div>
                </div>
              ))}
            </div>
            <DialogFooter><Button type="submit">Create</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default ProcurementRequisitions;
