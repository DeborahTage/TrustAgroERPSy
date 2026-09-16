import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { procurementApi } from '../../api';
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
import { formatDate } from '../../utils';
import { Plus } from 'lucide-react';
import { toast } from 'react-toastify';

const STATUSES = ['DRAFT', 'POSTED', 'CANCELLED'];

const ProcurementGoodsReceipts = () => {
  const { hasPermission } = useAuth();
  const [searchParams] = useSearchParams();
  const preselectedPoId = searchParams.get('poId') || '';
  const [rows, setRows] = useState([]);
  const [pos, setPos] = useState([]);
  const [selectedPo, setSelectedPo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState('all');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ purchaseOrderId: preselectedPoId, receiptDate: '', notes: '', lines: [] });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [grnRes, poRes] = await Promise.all([
        procurementApi.getGoodsReceipts(statusFilter !== 'all' ? { status: statusFilter } : undefined),
        procurementApi.getPurchaseOrders().catch(() => ({ data: { data: [] } })),
      ]);
      setRows(grnRes.data?.data || []);
      const receivable = (poRes.data?.data || []).filter((p) =>
        ['SENT', 'PARTIALLY_RECEIVED', 'APPROVED'].includes(p.status));
      setPos(receivable);
    } catch {
      setError('Failed to load goods receipts.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    if (preselectedPoId && hasPermission(PERMISSIONS.RECEIVE_PROCUREMENT)) {
      setOpen(true);
      setForm((f) => ({ ...f, purchaseOrderId: preselectedPoId }));
    }
  }, [preselectedPoId, hasPermission]);

  useEffect(() => {
    const loadPo = async () => {
      if (!form.purchaseOrderId) {
        setSelectedPo(null);
        setForm((f) => ({ ...f, lines: [] }));
        return;
      }
      try {
        const res = await procurementApi.getPurchaseOrder(form.purchaseOrderId);
        const po = res.data.data;
        setSelectedPo(po);
        setForm((f) => ({
          ...f,
          lines: (po.items || []).map((line) => {
            const remaining = Math.max(0, (line.quantityOrdered || 0) - (line.quantityReceived || 0));
            return {
              purchaseOrderItemId: line.id,
              itemName: line.itemName,
              remaining,
              receivedQty: remaining > 0 ? String(remaining) : '0',
              rejectedQty: '0',
              batchNumber: '',
              expiryDate: '',
              unitCost: line.unitPrice != null ? String(line.unitPrice) : '',
            };
          }),
        }));
      } catch {
        toast.error('Failed to load PO lines');
      }
    };
    loadPo();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [form.purchaseOrderId]);

  const create = async (e) => {
    e.preventDefault();
    const lines = form.lines
      .filter((l) => Number(l.receivedQty) > 0)
      .map((l) => ({
        purchaseOrderItemId: l.purchaseOrderItemId,
        receivedQty: Number(l.receivedQty),
        rejectedQty: l.rejectedQty ? Number(l.rejectedQty) : undefined,
        batchNumber: l.batchNumber || undefined,
        expiryDate: l.expiryDate || undefined,
        unitCost: l.unitCost ? Number(l.unitCost) : undefined,
      }));
    if (!lines.length) { toast.error('Enter received quantities'); return; }
    try {
      const res = await procurementApi.createGoodsReceipt({
        purchaseOrderId: Number(form.purchaseOrderId),
        receiptDate: form.receiptDate || undefined,
        notes: form.notes || undefined,
        items: lines,
      });
      setOpen(false);
      const created = res.data?.data;
      if (created?.id) {
        try {
          await procurementApi.postGoodsReceipt(created.id);
          toast.success('GRN posted — stock updated');
        } catch (postErr) {
          toast.success('Goods receipt created as draft');
          toast.error(postErr.response?.data?.message || 'Post failed — post from the list');
        }
      } else {
        toast.success('Goods receipt created');
      }
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Create failed');
    }
  };

  const act = async (fn, msg) => {
    try {
      await fn();
      toast.success(msg);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    }
  };

  const cols = [
    { accessorKey: 'grnNumber', header: 'GRN #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.grnNumber}</span> },
    { accessorKey: 'poNumber', header: 'PO #' },
    { accessorKey: 'supplierName', header: 'Supplier' },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { accessorKey: 'receiptDate', header: 'Date', cell: ({ row }) => formatDate(row.original.receiptDate) },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => {
        const g = row.original;
        return (
          <div className="flex gap-1 justify-end">
            {hasPermission(PERMISSIONS.RECEIVE_PROCUREMENT) && g.status === 'DRAFT' && (
              <Button size="sm" onClick={() => act(() => procurementApi.postGoodsReceipt(g.id), 'GRN posted — stock updated')}>Post</Button>
            )}
            {(hasPermission(PERMISSIONS.CANCEL_PROCUREMENT) || hasPermission(PERMISSIONS.RECEIVE_PROCUREMENT)) && g.status === 'DRAFT' && (
              <Button size="sm" variant="outline" onClick={() => act(() => procurementApi.cancelGoodsReceipt(g.id), 'Cancelled')}>Cancel</Button>
            )}
          </div>
        );
      },
    },
  ];

  const receivablePos = useMemo(() => pos, [pos]);

  return (
    <EnterprisePage
      title="Goods Receipts"
      subtitle="Receive against open purchase orders and post stock"
      breadcrumbs={['Procurement', 'Goods Receipts']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={hasPermission(PERMISSIONS.RECEIVE_PROCUREMENT) && (
        <Button size="sm" onClick={() => setOpen(true)}><Plus className="h-4 w-4 mr-1" /> Receive from PO</Button>
      )}
    >
      <div className="mb-4">
        <Select value={statusFilter} onValueChange={setStatusFilter}>
          <SelectTrigger className="w-48"><SelectValue placeholder="Status" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All statuses</SelectItem>
            {STATUSES.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
          </SelectContent>
        </Select>
      </div>
      {rows.length === 0 && !loading ? (
        <p className="text-sm text-slate-500 py-10 text-center">No goods receipts found.</p>
      ) : (
        <DataTable columns={cols} data={rows} />
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>Create Goods Receipt</DialogTitle></DialogHeader>
          <form onSubmit={create} className="space-y-3">
            <div className="space-y-1">
              <Label>Purchase Order</Label>
              <Select value={form.purchaseOrderId} onValueChange={(v) => setForm({ ...form, purchaseOrderId: v })}>
                <SelectTrigger><SelectValue placeholder="Select PO" /></SelectTrigger>
                <SelectContent>
                  {receivablePos.map((p) => (
                    <SelectItem key={p.id} value={String(p.id)}>{p.poNumber} — {p.supplierName}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1"><Label>Receipt Date</Label><Input type="date" value={form.receiptDate} onChange={(e) => setForm({ ...form, receiptDate: e.target.value })} /></div>
            {selectedPo && (
              <div className="space-y-2">
                <Label>Lines (remaining qty shown)</Label>
                {form.lines.map((line, idx) => (
                  <div key={line.purchaseOrderItemId} className="border rounded-lg p-3 space-y-2">
                    <div className="flex justify-between text-sm">
                      <span className="font-semibold">{line.itemName}</span>
                      <span className="font-mono text-slate-500">{line.remaining} remaining</span>
                    </div>
                    <div className="grid grid-cols-3 gap-2">
                      <div className="space-y-1">
                        <Label className="text-xs">Received</Label>
                        <Input type="number" min="0" step="any" max={line.remaining} value={line.receivedQty} onChange={(e) => {
                          const lines = [...form.lines];
                          lines[idx] = { ...lines[idx], receivedQty: e.target.value };
                          setForm({ ...form, lines });
                        }} />
                      </div>
                      <div className="space-y-1">
                        <Label className="text-xs">Rejected</Label>
                        <Input type="number" min="0" step="any" value={line.rejectedQty} onChange={(e) => {
                          const lines = [...form.lines];
                          lines[idx] = { ...lines[idx], rejectedQty: e.target.value };
                          setForm({ ...form, lines });
                        }} />
                      </div>
                      <div className="space-y-1">
                        <Label className="text-xs">Batch</Label>
                        <Input value={line.batchNumber} onChange={(e) => {
                          const lines = [...form.lines];
                          lines[idx] = { ...lines[idx], batchNumber: e.target.value };
                          setForm({ ...form, lines });
                        }} />
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
            <DialogFooter><Button type="submit" disabled={!form.purchaseOrderId}>Create & Post</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default ProcurementGoodsReceipts;
