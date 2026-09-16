import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { consultancyApi } from '../../api';
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
import { formatCurrency } from '../../utils';
import { Plus } from 'lucide-react';
import { toast } from 'react-toastify';

const CATEGORIES = [
  'FARM_ASSESSMENT', 'POULTRY_SETUP', 'LIVESTOCK', 'FEED_NUTRITION', 'BIOSECURITY',
  'FARM_MANAGEMENT', 'PRODUCTIVITY', 'ONSITE_ADVISORY', 'BUSINESS_ADVISORY', 'OTHER',
];

const emptyForm = {
  serviceCode: '', name: '', category: 'FARM_ASSESSMENT', defaultPrice: '', currency: 'ETB', active: true,
};

const ConsultancyServices = () => {
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await consultancyApi.getServices({ activeOnly: false });
      setRows(res.data.data || []);
    } catch {
      setError('Failed to load services.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const create = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await consultancyApi.createService({
        ...form,
        defaultPrice: parseFloat(form.defaultPrice) || 0,
        serviceCode: form.serviceCode || undefined,
      });
      toast.success('Service created');
      setOpen(false);
      setForm(emptyForm);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Create failed');
    } finally {
      setSaving(false);
    }
  };

  const filtered = useMemo(() => rows.filter((r) => {
    if (!search.trim()) return true;
    const q = search.trim().toLowerCase();
    return `${r.serviceCode || ''} ${r.name || ''} ${r.category || ''}`.toLowerCase().includes(q);
  }), [rows, search]);

  const cols = [
    { accessorKey: 'serviceCode', header: 'Code', cell: ({ row }) => <span className="font-mono font-bold">{row.original.serviceCode}</span> },
    { accessorKey: 'name', header: 'Name' },
    { accessorKey: 'category', header: 'Category' },
    { accessorKey: 'defaultPrice', header: 'Price', cell: ({ row }) => formatCurrency(row.original.defaultPrice) },
    { accessorKey: 'currency', header: 'Currency' },
    {
      accessorKey: 'active',
      header: 'Status',
      cell: ({ row }) => <Badge variant="outline">{row.original.active ? 'ACTIVE' : 'INACTIVE'}</Badge>,
    },
  ];

  return (
    <EnterprisePage
      title="Consultancy Services"
      subtitle="Catalog of advisory and on-site consultancy offerings"
      breadcrumbs={['Consultancy', 'Services']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={hasPermission(PERMISSIONS.MANAGE_CONSULTANCY) && (
        <Button size="sm" onClick={() => setOpen(true)}><Plus className="h-4 w-4 mr-1" /> New Service</Button>
      )}
    >
      <div className="mb-4">
        <Input className="max-w-xs" placeholder="Search code / name…" value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>
      {filtered.length === 0 && !loading ? (
        <p className="text-sm text-slate-500 py-10 text-center">No services yet.</p>
      ) : (
        <DataTable columns={cols} data={filtered} />
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader><DialogTitle>Create Service</DialogTitle></DialogHeader>
          <form onSubmit={create} className="space-y-3">
            <div className="space-y-1"><Label>Service Code</Label><Input value={form.serviceCode} onChange={(e) => setForm({ ...form, serviceCode: e.target.value })} placeholder="Optional" /></div>
            <div className="space-y-1"><Label>Name</Label><Input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
            <div className="space-y-1">
              <Label>Category</Label>
              <Select value={form.category} onValueChange={(v) => setForm({ ...form, category: v })}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  {CATEGORIES.map((c) => <SelectItem key={c} value={c}>{c}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Default Price</Label><Input type="number" step="0.01" required value={form.defaultPrice} onChange={(e) => setForm({ ...form, defaultPrice: e.target.value })} /></div>
              <div className="space-y-1"><Label>Currency</Label><Input value={form.currency} onChange={(e) => setForm({ ...form, currency: e.target.value })} /></div>
            </div>
            <DialogFooter>
              <Button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Create'}</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default ConsultancyServices;
