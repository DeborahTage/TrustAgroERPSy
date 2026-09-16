import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { supplierApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../../components/ui/dialog';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { Plus, Star } from 'lucide-react';
import { toast } from 'react-toastify';

const emptyForm = {
  name: '', contactPerson: '', email: '', phone: '', address: '', category: '', paymentTerms: 'NET_30', rating: 4,
};

const ProcurementSuppliers = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await supplierApi.getAll();
      const list = Array.isArray(res.data) ? res.data : res.data?.data || [];
      setRows(list);
    } catch {
      setError('Failed to load suppliers.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const create = async (e) => {
    e.preventDefault();
    try {
      await supplierApi.create({
        ...form,
        rating: Number(form.rating) || 4,
      });
      toast.success('Supplier created');
      setOpen(false);
      setForm(emptyForm);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Create failed');
    }
  };

  const filtered = useMemo(() => rows.filter((s) => {
    if (!search.trim()) return true;
    const q = search.trim().toLowerCase();
    return `${s.supplierCode || ''} ${s.name || ''} ${s.email || ''} ${s.category || ''}`.toLowerCase().includes(q);
  }), [rows, search]);

  const cols = [
    {
      accessorKey: 'supplierCode',
      header: 'Code',
      cell: ({ row }) => <span className="font-mono font-bold text-primary">{row.original.supplierCode || '—'}</span>,
    },
    { accessorKey: 'name', header: 'Name' },
    { accessorKey: 'contactPerson', header: 'Contact', cell: ({ row }) => row.original.contactPerson || '—' },
    { accessorKey: 'phone', header: 'Phone', cell: ({ row }) => row.original.phone || '—' },
    {
      accessorKey: 'category',
      header: 'Category',
      cell: ({ row }) => <Badge variant="outline">{row.original.category || 'GENERAL'}</Badge>,
    },
    {
      accessorKey: 'rating',
      header: 'Rating',
      cell: ({ row }) => (
        <span className="inline-flex items-center gap-1">
          <Star className="h-3 w-3 fill-amber-400 text-amber-400" />
          {row.original.rating ?? '—'}
        </span>
      ),
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <Button size="sm" variant="outline" onClick={() => navigate(`/procurement/suppliers/${row.original.id}`)}>
          Open
        </Button>
      ),
    },
  ];

  const canManage = hasPermission(PERMISSIONS.CREATE_PROCUREMENT) || hasPermission(PERMISSIONS.EDIT_PROCUREMENT)
    || hasPermission(PERMISSIONS.CREATE_INVENTORY_ITEMS);

  return (
    <EnterprisePage
      title="Procurement Suppliers"
      subtitle="Vendor master used across purchase orders and AP"
      breadcrumbs={['Procurement', 'Suppliers']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={canManage && (
        <Button size="sm" onClick={() => setOpen(true)}><Plus className="h-4 w-4 mr-1" /> Add Supplier</Button>
      )}
    >
      <div className="mb-4">
        <Input className="max-w-xs" placeholder="Search code, name…" value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>
      {filtered.length === 0 && !loading ? (
        <p className="text-sm text-slate-500 py-10 text-center">No suppliers found.</p>
      ) : (
        <DataTable columns={cols} data={filtered} />
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader><DialogTitle>Add Supplier</DialogTitle></DialogHeader>
          <form onSubmit={create} className="space-y-3">
            <div className="space-y-1"><Label>Name</Label><Input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Contact</Label><Input value={form.contactPerson} onChange={(e) => setForm({ ...form, contactPerson: e.target.value })} /></div>
              <div className="space-y-1"><Label>Phone</Label><Input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></div>
            </div>
            <div className="space-y-1"><Label>Email</Label><Input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Category</Label><Input value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} placeholder="FEED, DRUGS…" /></div>
              <div className="space-y-1"><Label>Payment Terms</Label><Input value={form.paymentTerms} onChange={(e) => setForm({ ...form, paymentTerms: e.target.value })} /></div>
            </div>
            <DialogFooter><Button type="submit">Create</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default ProcurementSuppliers;
