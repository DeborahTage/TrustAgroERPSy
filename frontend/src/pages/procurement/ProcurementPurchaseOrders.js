import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { inventoryApi, procurementApi, supplierApi } from '../../api';
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
import { Plus, Trash2 } from 'lucide-react';
import { toast } from 'react-toastify';

const STATUSES = ['DRAFT', 'SUBMITTED', 'APPROVED', 'SENT', 'PARTIALLY_RECEIVED', 'FULLY_RECEIVED', 'CANCELLED', 'CLOSED'];
const emptyLine = () => ({ inventoryItemId: '', quantityOrdered: '', unitPrice: '', notes: '' });

const ProcurementPurchaseOrders = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({
    supplierId: '', orderDate: '', expectedDelivery: '', paymentTerms: 'NET_30', currency: 'ETB', taxAmount: '', notes: '', lines: [emptyLine()],
  });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [poRes, supRes, itemRes] = await Promise.all([
        procurementApi.getPurchaseOrders(statusFilter !== 'all' ? { status: statusFilter } : undefined),
        supplierApi.getAll().catch(() => ({ data: { data: [] } })),
        inventoryApi.getItems().catch(() => ({ data: { data: [] } })),
      ]);
      setRows(poRes.data?.data || []);
      setSuppliers(Array.isArray(supRes.data) ? supRes.data : supRes.data?.data || []);
      setItems(itemRes.data?.data || []);
    } catch {
      setError('Failed to load purchase orders.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => { load(); }, [load]);

  const create = async (e) => {
    e.preventDefault();
    const lines = form.lines.filter((l) => l.inventoryItemId && l.quantityOrdered && l.unitPrice !== '');
    if (!lines.length) { toast.error('Add at least one line'); return; }
    try {
      const res = await procurementApi.createPurchaseOrder({
        supplierId: Number(form.supplierId),
        orderDate: form.orderDate || undefined,
        expectedDelivery: form.expectedDelivery || undefined,
        paymentTerms: form.paymentTerms || undefined,
        currency: form.currency || 'ETB',
        taxAmount: form.taxAmount ? Number(form.taxAmount) : undefined,
        notes: form.notes || undefined,
        items: lines.map((l) => ({
          inventoryItemId: Number(l.inventoryItemId),
          quantityOrdered: Number(l.quantityOrdered),
          unitPrice: Number(l.unitPrice),
          notes: l.notes || undefined,
        })),
      });
      toast.success('Purchase order created');
      setOpen(false);
      const created = res.data?.data;
      if (created?.id) navigate(`/procurement/purchase-orders/${created.id}`);
      else load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Create failed');
    }
  };

  const filtered = useMemo(() => rows.filter((r) => {
    if (!search.trim()) return true;
    const q = search.trim().toLowerCase();
    return `${r.poNumber || ''} ${r.supplierName || ''} ${r.supplierCode || ''}`.toLowerCase().includes(q);
  }), [rows, search]);

  const cols = [
    {
      accessorKey: 'poNumber',
      header: 'PO #',
      cell: ({ row }) => (
        <button type="button" className="font-mono font-bold text-primary" onClick={() => navigate(`/procurement/purchase-orders/${row.original.id}`)}>
          {row.original.poNumber}
        </button>
      ),
    },
    {
      accessorKey: 'supplierName',
      header: 'Supplier',
      cell: ({ row }) => (
        <div>
          <div>{row.original.supplierName}</div>
          {row.original.supplierCode && <div className="text-xs font-mono text-slate-500">{row.original.supplierCode}</div>}
        </div>
      ),
    },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { accessorKey: 'totalAmount', header: 'Total', cell: ({ row }) => formatCurrency(row.original.totalAmount) },
    { accessorKey: 'expectedDelivery', header: 'Expected', cell: ({ row }) => formatDate(row.original.expectedDelivery) },
  ];

  return (
    <EnterprisePage
      title="Purchase Orders"
      subtitle="Supplier orders across submit, approve, send, and receive"
      breadcrumbs={['Procurement', 'Purchase Orders']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={hasPermission(PERMISSIONS.CREATE_PROCUREMENT) && (
        <Button size="sm" onClick={() => setOpen(true)}><Plus className="h-4 w-4 mr-1" /> New PO</Button>
      )}
    >
      <div className="flex flex-wrap gap-3 mb-4">
        <Input className="max-w-xs" placeholder="Search…" value={search} onChange={(e) => setSearch(e.target.value)} />
        <Select value={statusFilter} onValueChange={setStatusFilter}>
          <SelectTrigger className="w-52"><SelectValue placeholder="Status" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All statuses</SelectItem>
            {STATUSES.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
          </SelectContent>
        </Select>
      </div>
      {filtered.length === 0 && !loading ? (
        <p className="text-sm text-slate-500 py-10 text-center">No purchase orders found.</p>
      ) : (
        <DataTable columns={cols} data={filtered} />
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>Create Purchase Order</DialogTitle></DialogHeader>
          <form onSubmit={create} className="space-y-3">
            <div className="space-y-1">
              <Label>Supplier</Label>
              <Select value={form.supplierId} onValueChange={(v) => setForm({ ...form, supplierId: v })}>
                <SelectTrigger><SelectValue placeholder="Select supplier" /></SelectTrigger>
                <SelectContent>
                  {suppliers.map((s) => (
                    <SelectItem key={s.id} value={String(s.id)}>
                      {[s.supplierCode, s.name].filter(Boolean).join(' — ')}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Order Date</Label><Input type="date" value={form.orderDate} onChange={(e) => setForm({ ...form, orderDate: e.target.value })} /></div>
              <div className="space-y-1"><Label>Expected Delivery</Label><Input type="date" value={form.expectedDelivery} onChange={(e) => setForm({ ...form, expectedDelivery: e.target.value })} /></div>
            </div>
            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-1"><Label>Payment Terms</Label><Input value={form.paymentTerms} onChange={(e) => setForm({ ...form, paymentTerms: e.target.value })} /></div>
              <div className="space-y-1"><Label>Currency</Label><Input value={form.currency} onChange={(e) => setForm({ ...form, currency: e.target.value })} /></div>
              <div className="space-y-1"><Label>Tax</Label><Input type="number" min="0" step="any" value={form.taxAmount} onChange={(e) => setForm({ ...form, taxAmount: e.target.value })} /></div>
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
                    <Input type="number" min="0.01" step="any" value={line.quantityOrdered} onChange={(e) => {
                      const lines = [...form.lines];
                      lines[idx] = { ...lines[idx], quantityOrdered: e.target.value };
                      setForm({ ...form, lines });
                    }} />
                  </div>
                  <div className="col-span-3 space-y-1">
                    <Label className="text-xs">Unit Price</Label>
                    <Input type="number" min="0" step="any" value={line.unitPrice} onChange={(e) => {
                      const lines = [...form.lines];
                      lines[idx] = { ...lines[idx], unitPrice: e.target.value };
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
            <DialogFooter><Button type="submit" disabled={!form.supplierId}>Create</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default ProcurementPurchaseOrders;
