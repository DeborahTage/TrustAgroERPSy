import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { procurementApi, supplierApi } from '../../api';
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

const STATUSES = ['DRAFT', 'SUBMITTED', 'MATCHED', 'MISMATCHED', 'APPROVED', 'PARTIALLY_PAID', 'PAID', 'CANCELLED'];
const PAY_METHODS = ['CASH', 'BANK_TRANSFER', 'MOBILE_MONEY', 'CREDIT'];
const emptyLine = () => ({ inventoryItemId: '', description: '', quantity: '', unitPrice: '', purchaseOrderItemId: '' });

const ProcurementSupplierInvoices = () => {
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [pos, setPos] = useState([]);
  const [selectedPo, setSelectedPo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [open, setOpen] = useState(false);
  const [payOpen, setPayOpen] = useState(false);
  const [payInvoice, setPayInvoice] = useState(null);
  const [payForm, setPayForm] = useState({ amount: '', paymentMethod: 'BANK_TRANSFER', paymentDate: '', notes: '' });
  const [form, setForm] = useState({
    supplierId: '', supplierInvoiceNumber: '', purchaseOrderId: '', invoiceDate: '', dueDate: '', currency: 'ETB', taxAmount: '', notes: '', lines: [emptyLine()],
  });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [invRes, supRes, poRes] = await Promise.all([
        procurementApi.getSupplierInvoices(statusFilter !== 'all' ? { status: statusFilter } : undefined),
        supplierApi.getAll().catch(() => ({ data: { data: [] } })),
        procurementApi.getPurchaseOrders().catch(() => ({ data: { data: [] } })),
      ]);
      setRows(invRes.data?.data || []);
      setSuppliers(Array.isArray(supRes.data) ? supRes.data : supRes.data?.data || []);
      setPos(poRes.data?.data || []);
    } catch {
      setError('Failed to load supplier invoices.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    const loadPo = async () => {
      if (!form.purchaseOrderId) {
        setSelectedPo(null);
        return;
      }
      try {
        const res = await procurementApi.getPurchaseOrder(form.purchaseOrderId);
        const po = res.data.data;
        setSelectedPo(po);
        setForm((f) => ({
          ...f,
          supplierId: String(po.supplierId || f.supplierId),
          lines: (po.items || []).map((line) => ({
            purchaseOrderItemId: String(line.id),
            inventoryItemId: String(line.inventoryItemId),
            description: line.itemName || '',
            quantity: String(line.quantityOrdered || ''),
            unitPrice: line.unitPrice != null ? String(line.unitPrice) : '',
          })),
        }));
      } catch {
        toast.error('Failed to load PO for invoice');
      }
    };
    loadPo();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [form.purchaseOrderId]);

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
    const lines = form.lines.filter((l) => l.quantity && l.unitPrice !== '');
    if (!lines.length) { toast.error('Add invoice lines'); return; }
    try {
      await procurementApi.createSupplierInvoice({
        supplierId: Number(form.supplierId),
        supplierInvoiceNumber: form.supplierInvoiceNumber,
        purchaseOrderId: form.purchaseOrderId ? Number(form.purchaseOrderId) : undefined,
        invoiceDate: form.invoiceDate || undefined,
        dueDate: form.dueDate || undefined,
        currency: form.currency || 'ETB',
        taxAmount: form.taxAmount ? Number(form.taxAmount) : undefined,
        notes: form.notes || undefined,
        items: lines.map((l) => ({
          purchaseOrderItemId: l.purchaseOrderItemId ? Number(l.purchaseOrderItemId) : undefined,
          inventoryItemId: l.inventoryItemId ? Number(l.inventoryItemId) : undefined,
          description: l.description || undefined,
          quantity: Number(l.quantity),
          unitPrice: Number(l.unitPrice),
        })),
      });
      toast.success('Supplier invoice created');
      setOpen(false);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Create failed');
    }
  };

  const submitPay = async (e) => {
    e.preventDefault();
    await act(() => procurementApi.createPayment({
      supplierInvoiceId: payInvoice.id,
      amount: Number(payForm.amount),
      paymentMethod: payForm.paymentMethod,
      paymentDate: payForm.paymentDate || undefined,
      notes: payForm.notes || undefined,
    }), 'Payment recorded');
    setPayOpen(false);
    setPayInvoice(null);
  };

  const filtered = useMemo(() => rows.filter((r) => {
    if (!search.trim()) return true;
    const q = search.trim().toLowerCase();
    return `${r.invoiceNumber || ''} ${r.supplierInvoiceNumber || ''} ${r.supplierName || ''}`.toLowerCase().includes(q);
  }), [rows, search]);

  const cols = [
    { accessorKey: 'invoiceNumber', header: 'Invoice #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.invoiceNumber}</span> },
    { accessorKey: 'supplierName', header: 'Supplier' },
    { accessorKey: 'poNumber', header: 'PO', cell: ({ row }) => row.original.poNumber || '—' },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { accessorKey: 'matchStatus', header: 'Match', cell: ({ row }) => row.original.matchStatus || '—' },
    { accessorKey: 'totalAmount', header: 'Total', cell: ({ row }) => formatCurrency(row.original.totalAmount) },
    { accessorKey: 'outstanding', header: 'Outstanding', cell: ({ row }) => formatCurrency(row.original.outstanding) },
    { accessorKey: 'dueDate', header: 'Due', cell: ({ row }) => formatDate(row.original.dueDate) },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => {
        const inv = row.original;
        return (
          <div className="flex flex-wrap gap-1 justify-end">
            {hasPermission(PERMISSIONS.PROCUREMENT_INVOICE) && inv.status === 'DRAFT' && (
              <Button size="sm" variant="outline" onClick={() => act(() => procurementApi.submitSupplierInvoice(inv.id), 'Submitted')}>Submit</Button>
            )}
            {hasPermission(PERMISSIONS.PROCUREMENT_INVOICE) && ['SUBMITTED', 'MISMATCHED'].includes(inv.status) && (
              <Button size="sm" variant="outline" onClick={() => act(() => procurementApi.matchSupplierInvoice(inv.id), 'Matched')}>Match</Button>
            )}
            {hasPermission(PERMISSIONS.PROCUREMENT_INVOICE) && ['MATCHED', 'SUBMITTED'].includes(inv.status) && (
              <Button size="sm" onClick={() => act(() => procurementApi.approveSupplierInvoice(inv.id), 'Approved')}>Approve</Button>
            )}
            {hasPermission(PERMISSIONS.PROCUREMENT_PAYMENT) && ['APPROVED', 'PARTIALLY_PAID'].includes(inv.status) && Number(inv.outstanding) > 0 && (
              <Button size="sm" variant="outline" onClick={() => {
                setPayInvoice(inv);
                setPayForm({ amount: String(inv.outstanding || ''), paymentMethod: 'BANK_TRANSFER', paymentDate: '', notes: '' });
                setPayOpen(true);
              }}>Pay</Button>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <EnterprisePage
      title="Supplier Invoices"
      subtitle="3-way match, approve, and record supplier payments"
      breadcrumbs={['Procurement', 'Supplier Invoices']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={hasPermission(PERMISSIONS.PROCUREMENT_INVOICE) && (
        <Button size="sm" onClick={() => setOpen(true)}><Plus className="h-4 w-4 mr-1" /> New Invoice</Button>
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
        <p className="text-sm text-slate-500 py-10 text-center">No invoices found.</p>
      ) : (
        <DataTable columns={cols} data={filtered} />
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>Create Supplier Invoice</DialogTitle></DialogHeader>
          <form onSubmit={create} className="space-y-3">
            <div className="space-y-1">
              <Label>Link PO (optional)</Label>
              <Select value={form.purchaseOrderId || 'none'} onValueChange={(v) => setForm({ ...form, purchaseOrderId: v === 'none' ? '' : v })}>
                <SelectTrigger><SelectValue placeholder="Optional PO" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">None</SelectItem>
                  {pos.map((p) => <SelectItem key={p.id} value={String(p.id)}>{p.poNumber} — {p.supplierName}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label>Supplier</Label>
              <Select value={form.supplierId} onValueChange={(v) => setForm({ ...form, supplierId: v })}>
                <SelectTrigger><SelectValue placeholder="Select supplier" /></SelectTrigger>
                <SelectContent>
                  {suppliers.map((s) => (
                    <SelectItem key={s.id} value={String(s.id)}>{[s.supplierCode, s.name].filter(Boolean).join(' — ')}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1"><Label>Supplier Invoice #</Label><Input required value={form.supplierInvoiceNumber} onChange={(e) => setForm({ ...form, supplierInvoiceNumber: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Invoice Date</Label><Input type="date" value={form.invoiceDate} onChange={(e) => setForm({ ...form, invoiceDate: e.target.value })} /></div>
              <div className="space-y-1"><Label>Due Date</Label><Input type="date" value={form.dueDate} onChange={(e) => setForm({ ...form, dueDate: e.target.value })} /></div>
            </div>
            <div className="space-y-2">
              <div className="flex justify-between items-center">
                <Label>Lines {selectedPo ? '(from PO)' : ''}</Label>
                {!selectedPo && (
                  <Button type="button" size="sm" variant="outline" onClick={() => setForm({ ...form, lines: [...form.lines, emptyLine()] })}>
                    <Plus className="h-3 w-3 mr-1" /> Line
                  </Button>
                )}
              </div>
              {form.lines.map((line, idx) => (
                <div key={idx} className="grid grid-cols-12 gap-2 items-end border rounded-lg p-2">
                  <div className="col-span-5 space-y-1">
                    <Label className="text-xs">Description</Label>
                    <Input value={line.description} onChange={(e) => {
                      const lines = [...form.lines];
                      lines[idx] = { ...lines[idx], description: e.target.value };
                      setForm({ ...form, lines });
                    }} />
                  </div>
                  <div className="col-span-3 space-y-1">
                    <Label className="text-xs">Qty</Label>
                    <Input type="number" min="0.01" step="any" value={line.quantity} onChange={(e) => {
                      const lines = [...form.lines];
                      lines[idx] = { ...lines[idx], quantity: e.target.value };
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
                  <div className="col-span-1 flex justify-end">
                    {!selectedPo && (
                      <Button type="button" size="icon" variant="ghost" disabled={form.lines.length === 1} onClick={() => {
                        setForm({ ...form, lines: form.lines.filter((_, i) => i !== idx) });
                      }}><Trash2 className="h-4 w-4 text-rose-600" /></Button>
                    )}
                  </div>
                </div>
              ))}
            </div>
            <DialogFooter><Button type="submit" disabled={!form.supplierId}>Create</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={payOpen} onOpenChange={setPayOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Record Payment — {payInvoice?.invoiceNumber}</DialogTitle></DialogHeader>
          <form onSubmit={submitPay} className="space-y-3">
            <div className="space-y-1"><Label>Amount</Label><Input type="number" min="0.01" step="any" required value={payForm.amount} onChange={(e) => setPayForm({ ...payForm, amount: e.target.value })} /></div>
            <div className="space-y-1">
              <Label>Method</Label>
              <Select value={payForm.paymentMethod} onValueChange={(v) => setPayForm({ ...payForm, paymentMethod: v })}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>{PAY_METHODS.map((m) => <SelectItem key={m} value={m}>{m}</SelectItem>)}</SelectContent>
              </Select>
            </div>
            <div className="space-y-1"><Label>Payment Date</Label><Input type="date" value={payForm.paymentDate} onChange={(e) => setPayForm({ ...payForm, paymentDate: e.target.value })} /></div>
            <DialogFooter><Button type="submit">Record Payment</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default ProcurementSupplierInvoices;
