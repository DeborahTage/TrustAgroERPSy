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

const emptyForm = {
  clientId: '', opportunityId: '', quotationType: 'PRODUCT', currency: 'ETB',
  issueDate: '', expiryDate: '', discountAmount: '', taxAmount: '', notes: '',
  itemDescription: '', itemQty: '1', itemUnit: 'unit', itemPrice: '',
};

const CrmQuotations = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = statusFilter !== 'ALL' ? { status: statusFilter } : undefined;
      const [qRes, cRes] = await Promise.all([
        crmApi.getQuotations(params),
        crmApi.getClients().catch(() => ({ data: { data: [] } })),
      ]);
      setRows(qRes.data.data || []);
      setClients(cRes.data?.data || []);
    } catch {
      setError('Failed to load quotations.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

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
      await crmApi.createQuotation({
        clientId: Number(form.clientId),
        opportunityId: form.opportunityId ? Number(form.opportunityId) : undefined,
        quotationType: form.quotationType,
        currency: form.currency || 'ETB',
        issueDate: form.issueDate || undefined,
        expiryDate: form.expiryDate || undefined,
        discountAmount: form.discountAmount ? parseFloat(form.discountAmount) : undefined,
        taxAmount: form.taxAmount ? parseFloat(form.taxAmount) : undefined,
        notes: form.notes || undefined,
        items: [{
          description: form.itemDescription,
          quantity: parseFloat(form.itemQty) || 1,
          unit: form.itemUnit || 'unit',
          unitPrice: parseFloat(form.itemPrice) || 0,
        }],
      });
      toast.success('Quotation created');
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
    return `${r.quotationNumber || ''} ${r.clientName || ''}`.toLowerCase().includes(q);
  }), [rows, search]);

  const cols = [
    { accessorKey: 'quotationNumber', header: 'Quote #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.quotationNumber}</span> },
    { accessorKey: 'clientName', header: 'Customer' },
    { accessorKey: 'totalAmount', header: 'Total', cell: ({ row }) => formatCurrency(row.original.totalAmount) },
    { accessorKey: 'expiryDate', header: 'Expiry', cell: ({ row }) => formatDate(row.original.expiryDate) },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <StatusBadge status={row.original.status} /> },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => {
        const q = row.original;
        return (
          <div className="flex flex-wrap gap-1 justify-end">
            <Button size="sm" variant="outline" onClick={() => navigate(`/crm/quotations/${q.id}`)}>Open</Button>
            {hasPermission(PERMISSIONS.CRM_QUOTATION) && q.status === 'DRAFT' && (
              <Button size="sm" variant="outline" onClick={() => act(() => crmApi.submitQuotation(q.id), 'Submitted')}>Submit</Button>
            )}
            {hasPermission(PERMISSIONS.CRM_QUOTATION_APPROVE) && q.status === 'SUBMITTED' && (
              <Button size="sm" onClick={() => act(() => crmApi.approveQuotation(q.id), 'Approved')}>Approve</Button>
            )}
            {hasPermission(PERMISSIONS.CRM_QUOTATION) && q.status === 'APPROVED' && (
              <Button size="sm" onClick={() => act(() => crmApi.sendQuotation(q.id), 'Sent')}>Send</Button>
            )}
            {hasPermission(PERMISSIONS.CRM_QUOTATION) && q.status === 'SENT' && (
              <Button size="sm" onClick={() => act(() => crmApi.acceptQuotation(q.id), 'Accepted')}>Accept</Button>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <EnterprisePage
      title="Quotations"
      subtitle="Commercial quotations — submit, approve, send, and accept"
      breadcrumbs={['CRM', 'Quotations']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={hasPermission(PERMISSIONS.CRM_QUOTATION) && (
        <Button size="sm" onClick={() => setOpen(true)}><Plus className="h-4 w-4 mr-1" /> New Quotation</Button>
      )}
    >
      <div className="mb-4 flex flex-wrap gap-3">
        <Input className="max-w-xs" placeholder="Search…" value={search} onChange={(e) => setSearch(e.target.value)} />
        <Select value={statusFilter} onValueChange={setStatusFilter}>
          <SelectTrigger className="w-44"><SelectValue /></SelectTrigger>
          <SelectContent>
            {['ALL', 'DRAFT', 'SUBMITTED', 'APPROVED', 'SENT', 'ACCEPTED', 'REJECTED', 'CANCELLED'].map((s) => (
              <SelectItem key={s} value={s}>{s === 'ALL' ? 'All statuses' : s}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      {filtered.length === 0 && !loading ? (
        <p className="text-sm text-slate-500 py-10 text-center">No quotations yet.</p>
      ) : (
        <DataTable columns={cols} data={filtered} />
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>Create Quotation</DialogTitle></DialogHeader>
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
            <div className="space-y-1"><Label>Line Description</Label><Input required value={form.itemDescription} onChange={(e) => setForm({ ...form, itemDescription: e.target.value })} /></div>
            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-1"><Label>Qty</Label><Input type="number" step="0.01" required value={form.itemQty} onChange={(e) => setForm({ ...form, itemQty: e.target.value })} /></div>
              <div className="space-y-1"><Label>Unit</Label><Input value={form.itemUnit} onChange={(e) => setForm({ ...form, itemUnit: e.target.value })} /></div>
              <div className="space-y-1"><Label>Unit Price</Label><Input type="number" step="0.01" required value={form.itemPrice} onChange={(e) => setForm({ ...form, itemPrice: e.target.value })} /></div>
            </div>
            <div className="space-y-1"><Label>Notes</Label><Textarea value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></div>
            <DialogFooter><Button type="submit" disabled={!form.clientId}>Create</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default CrmQuotations;
