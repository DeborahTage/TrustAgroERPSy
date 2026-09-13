import React, { useCallback, useEffect, useState } from 'react';
import { supplierApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '../../components/ui/dialog';
import { Badge } from '../../components/ui/badge';
import { Plus, FileText, Send, PackageCheck } from 'lucide-react';
import { toast } from 'react-toastify';
import api from '../../api/axios';

const statusColors = {
  Draft: 'bg-slate-100 text-slate-700',
  Sent: 'bg-blue-100 text-blue-700',
  Partial: 'bg-amber-100 text-amber-700',
  Received: 'bg-emerald-100 text-emerald-700',
  Closed: 'bg-slate-100 text-slate-500',
  Cancelled: 'bg-rose-100 text-rose-700',
};

export default function PurchaseOrders() {
  const [pos, setPos] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ poNumber: '', supplierId: '' });
  const [saving, setSaving] = useState(false);

  const fetchPOs = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const r = await api.get('/api/inventory/purchase-orders');
      setPos(Array.isArray(r.data) ? r.data : r.data?.data || []);
    } catch {
      setError('Failed to load purchase orders.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchPOs();
    supplierApi.getAll().then((r) => setSuppliers(r.data.data || [])).catch(() => {});
  }, [fetchPOs]);

  const createPO = async (e) => {
    e.preventDefault();
    if (!form.supplierId) { toast.error('Select a supplier'); return; }
    setSaving(true);
    try {
      await api.post('/api/inventory/purchase-orders', {
        poNumber: form.poNumber || `PO-${Date.now()}`,
        supplier: { id: parseInt(form.supplierId, 10) },
      });
      toast.success('Purchase order created');
      setShowCreate(false);
      setForm({ poNumber: '', supplierId: '' });
      fetchPOs();
    } catch {
      toast.error('Failed to create PO');
    } finally {
      setSaving(false);
    }
  };

  const sendPO = async (id) => {
    try {
      await api.put(`/api/inventory/purchase-orders/${id}/send`);
      toast.success('PO sent to supplier');
      fetchPOs();
    } catch { toast.error('Failed to send PO'); }
  };

  const receivePO = async (id) => {
    try {
      await api.put(`/api/inventory/purchase-orders/${id}/receive`);
      toast.success('PO received — stock updated');
      fetchPOs();
    } catch { toast.error('Failed to receive PO'); }
  };

  const columns = [
    { accessorKey: 'poNumber', header: 'PO Number', cell: ({ row }) => <span className="font-mono text-sm font-semibold text-primary">{row.original.poNumber}</span> },
    { accessorKey: 'supplier', header: 'Supplier', cell: ({ row }) => row.original.supplier?.name || '—' },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <Badge className={statusColors[row.original.status] || 'bg-slate-100'}>{row.original.status}</Badge> },
    { accessorKey: 'totalAmount', header: 'Total', cell: ({ row }) => row.original.totalAmount ? `${row.original.totalAmount.toFixed(2)} ETB` : '—' },
    { accessorKey: 'expectedDelivery', header: 'Expected Delivery', cell: ({ row }) => row.original.expectedDelivery || '—' },
    {
      id: 'actions',
      header: 'Actions',
      cell: ({ row }) => {
        const po = row.original;
        return (
          <div className="flex gap-2">
            {po.status === 'Draft' && (
              <Button size="sm" variant="outline" onClick={() => sendPO(po.id)}><Send className="h-3 w-3 mr-1" />Send</Button>
            )}
            {po.status === 'Sent' && (
              <Button size="sm" className="bg-emerald-600 hover:bg-emerald-700" onClick={() => receivePO(po.id)}><PackageCheck className="h-3 w-3 mr-1" />Receive</Button>
            )}
            {po.status === 'Received' && <span className="text-emerald-600 text-xs font-semibold">Received</span>}
          </div>
        );
      },
    },
  ];

  const stats = [
    { title: 'Total POs', value: pos.length, icon: FileText, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Draft', value: pos.filter(p => p.status === 'Draft').length, icon: FileText, iconColor: 'text-slate-600', bgColor: 'bg-slate-50' },
    { title: 'In Transit', value: pos.filter(p => p.status === 'Sent').length, icon: Send, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: 'Received', value: pos.filter(p => p.status === 'Received').length, icon: PackageCheck, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
  ];

  return (
    <EnterprisePage
      title="Purchase Orders"
      subtitle="Create, send, and receive supplier purchase orders"
      breadcrumbs={['Inventory', 'Purchase Orders']}
      loading={loading}
      error={error}
      onRetry={fetchPOs}
      onRefresh={fetchPOs}
      stats={stats}
      exportData={pos}
      exportColumns={columns.filter(c => c.id !== 'actions')}
      exportFilename="purchase_orders"
      actions={
        <Button className="bg-primary hover:bg-primary/90" onClick={() => setShowCreate(true)}>
          <Plus className="h-4 w-4 mr-2" /> Create PO
        </Button>
      }
      empty={!pos.length ? { title: 'No purchase orders', description: 'Create your first PO to start procurement.' } : null}
    >
      <DataTable columns={columns} data={pos} loading={loading} pageSize={10} />

      <Dialog open={showCreate} onOpenChange={setShowCreate}>
        <DialogContent className="rounded-2xl max-w-md">
          <DialogHeader><DialogTitle>New Purchase Order</DialogTitle></DialogHeader>
          <form onSubmit={createPO} className="space-y-4">
            <div className="space-y-2">
              <Label>PO Number (optional)</Label>
              <Input placeholder="Auto-generated if blank" value={form.poNumber} onChange={(e) => setForm({ ...form, poNumber: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label>Supplier *</Label>
              <select className="w-full border rounded-lg h-10 px-3" value={form.supplierId} onChange={(e) => setForm({ ...form, supplierId: e.target.value })} required>
                <option value="">Select supplier...</option>
                {suppliers.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
            <div className="flex gap-3">
              <Button type="button" variant="outline" className="flex-1" onClick={() => setShowCreate(false)}>Cancel</Button>
              <Button type="submit" className="flex-1 bg-primary hover:bg-primary/90" disabled={saving}>{saving ? 'Creating...' : 'Create'}</Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
}
