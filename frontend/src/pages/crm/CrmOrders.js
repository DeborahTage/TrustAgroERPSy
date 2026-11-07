import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { crmApi, inventoryApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import { Button } from '../../components/ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from '../../components/ui/dialog';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatCurrency, formatDate } from '../../utils';
import { MASTER_CATALOG_ITEMS } from '../../utils/masterCatalog';
import { Plus, ShoppingCart, Clock, CheckCircle, Truck, UserPlus, Package } from 'lucide-react';
import { toast } from 'react-toastify';

const emptyForm = {
  clientId: '',
  orderDate: new Date().toISOString().slice(0, 10),
  expectedDeliveryDate: '',
  currency: 'ETB',
  deliveryAddress: '',
  contactNumber: '',
  notes: '',
  selectedInventoryItemId: '',
  productName: '',
  quantity: '1',
  unit: 'PIECE',
  unitPrice: '',
};

const CrmOrders = () => {
  const navigate = useNavigate();
  const { hasPermission, hasRole } = useAuth();
  const [orders, setOrders] = useState([]);
  const [clients, setClients] = useState([]);
  const [inventoryItems, setInventoryItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [creating, setCreating] = useState(false);

  const canCreateOrder =
    hasPermission(PERMISSIONS.CRM_ORDER) ||
    hasPermission(PERMISSIONS.CREATE_CRM) ||
    hasPermission(PERMISSIONS.MANAGE_CRM) ||
    hasRole('ADMIN', 'GENERAL_MANAGER', 'CRM_MANAGER');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [oRes, cRes, iRes] = await Promise.all([
        crmApi.getOrders(),
        crmApi.getClients().catch(() => ({ data: { data: [] } })),
        inventoryApi.getItems().catch(() => ({ data: [] })),
      ]);
      setOrders(oRes.data?.data || oRes.data || []);
      setClients(cRes.data?.data || cRes.data || []);
      const itemsList = Array.isArray(iRes.data) ? iRes.data : (iRes.data?.data || []);
      setInventoryItems(itemsList.length > 0 ? itemsList : MASTER_CATALOG_ITEMS);
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

  const handleProductSelect = (itemId) => {
    if (itemId === 'CUSTOM') {
      setForm((prev) => ({
        ...prev,
        selectedInventoryItemId: 'CUSTOM',
        productName: '',
        unit: 'PIECE',
        unitPrice: '',
      }));
      return;
    }
    const item = inventoryItems.find((i) => String(i.id) === String(itemId));
    if (item) {
      const price = item.retailPrice != null
        ? String(item.retailPrice)
        : (item.sellingPrice != null ? String(item.sellingPrice) : (item.avgUnitCost != null ? String(item.avgUnitCost) : ''));
      setForm((prev) => ({
        ...prev,
        selectedInventoryItemId: itemId,
        productName: item.itemName,
        unit: item.unit || item.unitString || 'PIECE',
        unitPrice: price,
      }));
    }
  };

  const create = async (e) => {
    e.preventDefault();
    if (!form.clientId) {
      toast.error('Please select a customer for the order.');
      return;
    }
    if (!form.productName.trim()) {
      toast.error('Please select or specify a product name.');
      return;
    }
    const qty = parseFloat(form.quantity);
    if (!qty || qty <= 0) {
      toast.error('Please enter a valid quantity greater than 0.');
      return;
    }
    const price = parseFloat(form.unitPrice);
    if (price == null || isNaN(price) || price < 0) {
      toast.error('Please enter a valid unit price.');
      return;
    }

    setCreating(true);
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
          quantity: qty,
          unit: form.unit || 'PIECE',
          unitPrice: price,
        }],
      });
      toast.success('Order created successfully');
      setOpen(false);
      setForm(emptyForm);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Create failed');
    } finally {
      setCreating(false);
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
    <>
      <EnterprisePage
        title="Customer Orders"
        subtitle="Confirm, fulfill, invoice, and collect customer payments"
        breadcrumbs={['CRM', 'Orders']}
        loading={loading}
        error={error}
        onRetry={load}
        onRefresh={load}
        stats={kpiCards}
        actions={canCreateOrder && (
          <Button size="sm" onClick={() => setOpen(true)} className="bg-primary hover:bg-primary/90 text-primary-foreground font-semibold">
            <Plus className="h-4 w-4 mr-1.5" /> New Order
          </Button>
        )}
        empty={!loading && orders.length === 0 ? {
          title: 'No customer orders yet',
          description: 'Create a new order directly or accept an approved client quotation to begin.',
          action: () => setOpen(true),
          actionLabel: 'Create First Order',
        } : null}
      >
        {orders.length > 0 && (
          <>
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
              <Input className="max-w-xs" placeholder="Search orders…" value={search} onChange={(e) => setSearch(e.target.value)} />
              <p className="text-sm text-slate-500">
                Pipeline value: <span className="font-bold text-primary">{formatCurrency(stats.revenue)}</span>
              </p>
            </div>
            <DataTable columns={cols} data={filtered} />
          </>
        )}
      </EnterprisePage>

      {/* Create Order Modal — kept outside EnterprisePage so empty states don't unmount it */}
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="text-xl font-bold">Create New Customer Order</DialogTitle>
            <DialogDescription>
              Record an order with designated customer, order lines, pricing, and delivery schedule.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={create} className="space-y-4 pt-2">
            {/* Customer selector */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <Label htmlFor="clientSelect" className="font-semibold text-sm">Customer *</Label>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="h-7 text-xs text-primary font-semibold hover:text-primary/80 px-2"
                  onClick={() => { setOpen(false); navigate('/crm/clients/new'); }}
                >
                  <UserPlus className="h-3.5 w-3.5 mr-1" /> Add New Client
                </Button>
              </div>

              {clients.length === 0 ? (
                <div className="rounded-xl border border-amber-200 bg-amber-50 dark:bg-amber-950/30 p-3 text-xs text-amber-800 dark:text-amber-200 flex items-center justify-between">
                  <span>No clients registered yet. Please register a client first.</span>
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    className="h-7 text-xs border-amber-300 bg-white dark:bg-amber-900"
                    onClick={() => { setOpen(false); navigate('/crm/clients/new'); }}
                  >
                    + Add Client
                  </Button>
                </div>
              ) : (
                <Select
                  value={form.clientId}
                  onValueChange={(v) => {
                    const selClient = clients.find((c) => String(c.id) === String(v));
                    setForm((prev) => ({
                      ...prev,
                      clientId: v,
                      contactNumber: selClient?.phone || prev.contactNumber,
                      deliveryAddress: selClient?.location || selClient?.address || prev.deliveryAddress,
                    }));
                  }}
                >
                  <SelectTrigger id="clientSelect"><SelectValue placeholder="Select client account..." /></SelectTrigger>
                  <SelectContent>
                    {clients.map((c) => (
                      <SelectItem key={c.id} value={String(c.id)}>
                        {c.clientName || c.name} {c.phone ? `(${c.phone})` : ''} — {c.location || c.city || 'No location'}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            </div>

            {/* Product selection from catalog with custom fallback */}
            <div className="space-y-2 rounded-xl border border-border/80 p-3.5 bg-muted/20">
              <div className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-muted-foreground">
                <Package className="h-3.5 w-3.5 text-primary" /> Product & Pricing
              </div>

              <div className="space-y-1">
                <Label className="text-xs">Select from Inventory Catalog</Label>
                <Select
                  value={form.selectedInventoryItemId}
                  onValueChange={handleProductSelect}
                >
                  <SelectTrigger><SelectValue placeholder="Pick catalog item (e.g. Layer Feed, Vaccine)..." /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="CUSTOM">-- Custom / Other Product --</SelectItem>
                    {inventoryItems.map((it) => (
                      <SelectItem key={it.id} value={String(it.id)}>
                        {it.itemName} ({it.unit || 'PCS'}) — {it.retailPrice || it.sellingPrice ? `${Number(it.retailPrice || it.sellingPrice).toLocaleString()} ETB` : ''}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1">
                <Label className="text-xs">Product Name *</Label>
                <Input
                  required
                  placeholder="e.g. LAYER FEED or Live Broilers"
                  value={form.productName}
                  onChange={(e) => setForm({ ...form, productName: e.target.value })}
                />
              </div>

              <div className="grid grid-cols-3 gap-3 pt-1">
                <div className="space-y-1">
                  <Label className="text-xs">Quantity *</Label>
                  <Input
                    type="number"
                    step="any"
                    min="0.01"
                    required
                    value={form.quantity}
                    onChange={(e) => setForm({ ...form, quantity: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <Label className="text-xs">Unit</Label>
                  <Input
                    placeholder="Unit"
                    value={form.unit}
                    onChange={(e) => setForm({ ...form, unit: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <Label className="text-xs">Unit Price (ETB) *</Label>
                  <Input
                    type="number"
                    step="any"
                    min="0"
                    required
                    placeholder="0.00"
                    value={form.unitPrice}
                    onChange={(e) => setForm({ ...form, unitPrice: e.target.value })}
                  />
                </div>
              </div>

              {form.quantity && form.unitPrice && (
                <div className="text-right text-xs font-semibold text-primary pt-1">
                  Line Total: {formatCurrency(Number(form.quantity) * Number(form.unitPrice))}
                </div>
              )}
            </div>

            {/* Date & Contact fields */}
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label className="text-xs">Order Date</Label>
                <Input
                  type="date"
                  value={form.orderDate}
                  onChange={(e) => setForm({ ...form, orderDate: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label className="text-xs">Expected Delivery</Label>
                <Input
                  type="date"
                  value={form.expectedDeliveryDate}
                  onChange={(e) => setForm({ ...form, expectedDeliveryDate: e.target.value })}
                />
              </div>
            </div>

            <div className="space-y-1">
              <Label className="text-xs">Contact Number</Label>
              <Input
                placeholder="Phone number for delivery"
                value={form.contactNumber}
                onChange={(e) => setForm({ ...form, contactNumber: e.target.value })}
              />
            </div>

            <div className="space-y-1">
              <Label className="text-xs">Delivery Address</Label>
              <Textarea
                placeholder="Delivery destination address / farm location"
                value={form.deliveryAddress}
                onChange={(e) => setForm({ ...form, deliveryAddress: e.target.value })}
                rows={2}
              />
            </div>

            <DialogFooter className="pt-2">
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={!form.clientId || creating} className="bg-primary hover:bg-primary/90 text-primary-foreground font-semibold">
                {creating ? 'Creating...' : 'Create Order'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </>
  );
};

export default CrmOrders;
