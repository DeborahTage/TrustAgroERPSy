import React, { useCallback, useEffect, useState } from 'react';
import { internalDeliveryApi, farmDocumentApi, inventoryApi, userApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Input } from '../../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatDate } from '../../utils';

const STATUS_STYLES = {
  DRAFT: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700',
  SUBMITTED: 'bg-blue-100 text-blue-700 dark:bg-blue-950/50 dark:text-blue-300 dark:border-blue-800/40',
  APPROVED: 'bg-indigo-100 text-indigo-700 dark:bg-indigo-950/50 dark:text-indigo-300 dark:border-indigo-800/40',
  ISSUED: 'bg-amber-100 text-amber-700 dark:bg-amber-950/50 dark:text-amber-300 dark:border-amber-800/40',
  RECEIVED: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/50 dark:text-emerald-300 dark:border-emerald-800/40',
  CANCELLED: 'bg-rose-100 text-rose-700 dark:bg-rose-950/50 dark:text-rose-300 dark:border-rose-800/40',
};

const InternalDeliveryPage = () => {
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [items, setItems] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const [form, setForm] = useState({
    inventoryItemId: '',
    quantity: '',
    unit: 'KG',
    sourceLocation: 'MAIN_WAREHOUSE',
    destinationLocation: 'FARM_STORE',
    delivererId: '',
    receiverId: '',
    purpose: 'Farm restock',
    notes: '',
    deliveryDate: new Date().toISOString().slice(0, 10),
  });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [delRes, itemRes, userRes] = await Promise.all([
        internalDeliveryApi.list(),
        inventoryApi.getItems(),
        userApi.getAll().catch(() => ({ data: { data: [] } })),
      ]);
      setRows(delRes.data?.data || []);
      setItems(itemRes.data?.data || []);
      setUsers(userRes.data?.data || userRes.data || []);
    } catch {
      setError('Failed to load internal deliveries.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const run = async (id, fn) => {
    setBusyId(id);
    try {
      await fn();
      await load();
    } catch (e) {
      setError(e?.response?.data?.message || 'Action failed');
    } finally {
      setBusyId(null);
    }
  };

  const create = async () => {
    setError(null);
    try {
      await internalDeliveryApi.create({
        ...form,
        inventoryItemId: Number(form.inventoryItemId),
        quantity: Number(form.quantity),
        delivererId: Number(form.delivererId),
        receiverId: Number(form.receiverId),
      });
      await load();
    } catch (e) {
      setError(e?.response?.data?.message || 'Create failed');
    }
  };

  const openDoc = async (id) => {
    const res = await farmDocumentApi.html('INTERNAL_DELIVERY', id);
    const w = window.open('', '_blank');
    if (w) {
      w.document.write(typeof res.data === 'string' ? res.data : (res.data?.data || ''));
      w.document.close();
    }
  };

  const cols = [
    { accessorKey: 'receiptNumber', header: 'Receipt #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.receiptNumber}</span> },
    { accessorKey: 'deliveryDate', header: 'Date', cell: ({ row }) => formatDate(row.original.deliveryDate) },
    { accessorKey: 'inventoryItemName', header: 'Item' },
    { accessorKey: 'quantity', header: 'Qty' },
    { accessorKey: 'sourceLocation', header: 'From' },
    { accessorKey: 'destinationLocation', header: 'To' },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ row }) => <Badge className={`${STATUS_STYLES[row.original.status] || ''} shadow-none`}>{row.original.status}</Badge>,
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => {
        const r = row.original;
        const busy = busyId === r.id;
        return (
          <div className="flex flex-wrap gap-1">
            {r.status === 'DRAFT' && hasPermission(PERMISSIONS.EDIT_FARMS) && (
              <Button size="sm" disabled={busy} onClick={() => run(r.id, () => internalDeliveryApi.submit(r.id))}>Submit</Button>
            )}
            {r.status === 'SUBMITTED' && hasPermission(PERMISSIONS.EDIT_FARMS) && (
              <Button size="sm" disabled={busy} onClick={() => run(r.id, () => internalDeliveryApi.approve(r.id))}>Approve</Button>
            )}
            {r.status === 'APPROVED' && hasPermission(PERMISSIONS.STOCK_TRANSFER) && (
              <Button size="sm" disabled={busy} onClick={() => run(r.id, () => internalDeliveryApi.issue(r.id))}>Issue</Button>
            )}
              {r.status === 'ISSUED' && hasPermission(PERMISSIONS.EDIT_FARMS) && (
              <Button size="sm" disabled={busy} onClick={() => run(r.id, () => internalDeliveryApi.receive(r.id))}>Receive</Button>
            )}
            <Button size="sm" variant="ghost" onClick={() => openDoc(r.id)}>Print</Button>
          </div>
        );
      },
    },
  ];

  return (
    <EnterprisePage
      title="Internal Delivery Receipts"
      description="Warehouse → farm store transfers via InventoryService (no revenue)"
      loading={loading}
      error={error}
      onRetry={load}
    >
      {hasPermission(PERMISSIONS.CREATE_FARMS) && (
        <div className="mb-6 grid gap-2 md:grid-cols-4 border border-border/70 p-4 rounded-xl bg-card">
          <Select value={String(form.inventoryItemId)} onValueChange={(v) => setForm((f) => ({ ...f, inventoryItemId: v }))}>
            <SelectTrigger><SelectValue placeholder="Item" /></SelectTrigger>
            <SelectContent>
              {items.map((i) => <SelectItem key={i.id} value={String(i.id)}>{i.itemName || i.name}</SelectItem>)}
            </SelectContent>
          </Select>
          <Input type="number" placeholder="Quantity" value={form.quantity} onChange={(e) => setForm((f) => ({ ...f, quantity: e.target.value }))} />
          <Input placeholder="From" value={form.sourceLocation} onChange={(e) => setForm((f) => ({ ...f, sourceLocation: e.target.value }))} />
          <Input placeholder="To" value={form.destinationLocation} onChange={(e) => setForm((f) => ({ ...f, destinationLocation: e.target.value }))} />
          <Select value={String(form.delivererId)} onValueChange={(v) => setForm((f) => ({ ...f, delivererId: v }))}>
            <SelectTrigger><SelectValue placeholder="Deliverer" /></SelectTrigger>
            <SelectContent>
              {users.map((u) => <SelectItem key={u.id} value={String(u.id)}>{u.fullName || u.email}</SelectItem>)}
            </SelectContent>
          </Select>
          <Select value={String(form.receiverId)} onValueChange={(v) => setForm((f) => ({ ...f, receiverId: v }))}>
            <SelectTrigger><SelectValue placeholder="Receiver" /></SelectTrigger>
            <SelectContent>
              {users.map((u) => <SelectItem key={u.id} value={String(u.id)}>{u.fullName || u.email}</SelectItem>)}
            </SelectContent>
          </Select>
          <Input placeholder="Purpose" value={form.purpose} onChange={(e) => setForm((f) => ({ ...f, purpose: e.target.value }))} />
          <Button onClick={create}>Create Draft</Button>
        </div>
      )}
      <DataTable columns={cols} data={rows} />
    </EnterprisePage>
  );
};

export default InternalDeliveryPage;
