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
import { Plus, ShoppingCart, Clock, CheckCircle, Truck } from 'lucide-react';
import { toast } from 'react-toastify';

const emptyForm = {
  clientId: '', orderDate: '', expectedDeliveryDate: '', currency: 'ETB',
  deliveryAddress: '', contactNumber: '', notes: '',
  productName: '', quantity: '1', unit: 'unit', unitPrice: '',
};

const CrmOrders = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [orders, setOrders] = useState([]);
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [oRes, cRes] = await Promise.all([
        crmApi.getOrders(),
        crmApi.getClients().catch(() => ({ data: { data: [] } })),
      ]);
      setOrders(oRes.data.data || []);
      setClients(cRes.data?.data || []);
    } catch {
      setError('Failed to load customer orders.');
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
      await crmApi.createOrder({
        clientId: Number(form.clientId),
        orderDate: form.orderDate || undefined,
        expectedDeliveryDate: form.expectedDeliveryDate || undefined,
        currency: form.currency || 'ETB',
        deliveryAddress: form.deliveryAddress || undefined,
        contactNumber: form.contactNumber || undefined,
        notes: form.notes || undefined,
        items: [{
          productName: form.productName,
          quantity: parseFloat(form.quantity) || 1,
          unit: form.unit || 'unit',
          unitPrice: parseFloat(form.unitPrice) || 0,
        }],
      });
      toast.success('Order created');
      setOpen(false);
      setForm(emptyForm);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Create failed');
    }
  };

  const stats = useMemo(() => {
    const draft = orders.filter((o) => o.status === 'DRAFT').length;
    const confirmed = orders.filter((o) => o.status === 'CONFIRMED').length;
    const fulfilled = orders.filter((o) => o.status === 'FULFILLED' || o.status === 'PARTIALLY_FULFILLED').length;
    const revenue = orders.reduce((sum, o) => sum + (Number(o.totalAmount) || 0), 0);
    return { draft, confirmed, fulfilled, revenue };
  }, [orders]);

  const kpiCards = [
    { title: 'Total Orders', value: orders.length, icon: ShoppingCart, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Draft', value: stats.draft, icon: Clock, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: 'Confirmed', value: stats.confirmed, icon: CheckCircle, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: 'Fulfilled', value: stats.fulfilled, icon: Truck, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
  ];

  const filtered = useMemo(() => orders.filter((r) => {
    if (!search.trim()) return true;
    const q = search.trim().toLowerCase();
    return `${r.orderNumber || ''} ${r.clientName || ''} ${r.status || ''}`.toLowerCase().includes(q);
  }), [orders, search]);

  const cols = [
    { accessorKey: 'orderNumber', header: 'Order #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.orderNumber}</span> },
    { accessorKey: 'clientName', header: 'Customer' },
    { accessorKey: 'orderDate', header: 'Date', cell: ({ row }) => formatDate(row.original.orderDate) },
    { accessorKey: 'totalAmount', header: 'Total', cell: ({ row }) => formatCurrency(row.original.totalAmount) },
    { accessorKey: 'outstandingAmount', header: 'Outstanding', cell: ({ row }) => formatCurrency(row.original.outstandingAmount) },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <StatusBadge status={row.original.status} /> },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => {
        const o = row.original;
        return (
          <div className="flex flex-wrap gap-1 justify-end">
            <Button size="sm" variant="outline" onClick={() => navigate(`/crm/orders/${o.id}`)}>Open</Button>
            {hasPermission(PERMISSIONS.CRM_ORDER_CONFIRM) && o.status === 'DRAFT' && (
              <Button size="sm" onClick={() => act(() => crmApi.confirmOrder(o.id), 'Confirmed')}>Confirm</Button>
            )}
            {hasPermission(PERMISSIONS.CRM_ORDER_INVOICE) && !o.invoiceId && o.status !== 'DRAFT' && o.status !== 'CANCELLED' && (
              <Button size="sm" variant="outline" onClick={() => act(() => crmApi.invoiceOrder(o.id), 'Invoice created')}>Invoice</Button>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <EnterprisePage
      title="Customer Orders"
      subtitle="Confirm, fulfill, invoice, and collect payments"
      breadcrumbs={['CRM', 'Orders']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      stats={kpiCards}
      actions={hasPermission(PERMISSIONS.CRM_ORDER) && (
        <Button size="sm" onClick={() => setOpen(true)}><Plus className="h-4 w-4 mr-1" /> New Order</Button>
      )}
      empty={!loading && orders.length === 0 ? { title: 'No orders yet', description: 'Create an order or accept a quotation to begin.' } : null}
    >
      {orders.length > 0 && (
        <>
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <Input className="max-w-xs" placeholder="Search…" value={search} onChange={(e) => setSearch(e.target.value)} />
            <p className="text-sm text-slate-500">
              Pipeline value: <span className="font-bold text-primary">{formatCurrency(stats.revenue)}</span>
            </p>
          </div>
          <DataTable columns={cols} data={filtered} />
        </>
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>Create Order</DialogTitle></DialogHeader>
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
            <div className="space-y-1"><Label>Product</Label><Input required value={form.productName} onChange={(e) => setForm({ ...form, productName: e.target.value })} /></div>
            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-1"><Label>Qty</Label><Input type="number" step="0.01" required value={form.quantity} onChange={(e) => setForm({ ...form, quantity: e.target.value })} /></div>
              <div className="space-y-1"><Label>Unit</Label><Input value={form.unit} onChange={(e) => setForm({ ...form, unit: e.target.value })} /></div>
              <div className="space-y-1"><Label>Unit Price</Label><Input type="number" step="0.01" required value={form.unitPrice} onChange={(e) => setForm({ ...form, unitPrice: e.target.value })} /></div>
            </div>
            <div className="space-y-1"><Label>Delivery Address</Label><Textarea value={form.deliveryAddress} onChange={(e) => setForm({ ...form, deliveryAddress: e.target.value })} /></div>
            <DialogFooter><Button type="submit" disabled={!form.clientId}>Create</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default CrmOrders;
