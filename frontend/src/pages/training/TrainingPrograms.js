import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { trainingApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../../components/ui/dialog';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatCurrency } from '../../utils';
import { Plus } from 'lucide-react';
import { toast } from 'react-toastify';

const STATUS = {
  DRAFT: 'bg-slate-100 text-slate-700',
  PUBLISHED: 'bg-blue-100 text-blue-700',
  REGISTRATION_OPEN: 'bg-emerald-100 text-emerald-700',
  REGISTRATION_CLOSED: 'bg-amber-100 text-amber-700',
  IN_PROGRESS: 'bg-indigo-100 text-indigo-700',
  COMPLETED: 'bg-emerald-200 text-emerald-800',
  CANCELLED: 'bg-rose-100 text-rose-700',
};

const emptyForm = {
  title: '', description: '', category: 'AGRICULTURAL', deliveryMode: 'ONSITE',
  trainerName: '', location: '', defaultCapacity: 30, price: '', durationHours: '',
  objectives: '', startDate: '', endDate: '', registrationDeadline: '',
};

const TrainingPrograms = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [statusFilter, setStatusFilter] = useState('all');
  const [categoryFilter, setCategoryFilter] = useState('all');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await trainingApi.getPrograms();
      setRows(res.data.data || []);
    } catch {
      setError('Failed to load programs.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const create = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        ...form,
        price: parseFloat(form.price) || 0,
        defaultCapacity: form.defaultCapacity ? parseInt(form.defaultCapacity, 10) : null,
        durationHours: form.durationHours ? parseInt(form.durationHours, 10) : null,
        startDate: form.startDate || null,
        endDate: form.endDate || null,
        registrationDeadline: form.registrationDeadline || null,
      };
      const res = await trainingApi.createProgram(payload);
      toast.success('Program created');
      setOpen(false);
      setForm(emptyForm);
      navigate(`/training/programs/${res.data.data.id}`);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Create failed');
    } finally {
      setSaving(false);
    }
  };

  const cols = [
    { accessorKey: 'programCode', header: 'Code', cell: ({ row }) => <span className="font-mono font-bold">{row.original.programCode}</span> },
    { accessorKey: 'title', header: 'Title' },
    { accessorKey: 'category', header: 'Category' },
    { accessorKey: 'deliveryMode', header: 'Mode' },
    { accessorKey: 'price', header: 'Price', cell: ({ row }) => formatCurrency(row.original.price) },
    { accessorKey: 'sessionCount', header: 'Sessions' },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ row }) => <Badge className={`${STATUS[row.original.status] || 'bg-slate-100'} shadow-none`}>{row.original.status}</Badge>,
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <Button size="sm" variant="outline" onClick={() => navigate(`/training/programs/${row.original.id}`)}>Open</Button>
      ),
    },
  ];

  const filtered = useMemo(() => rows.filter((r) => {
    if (statusFilter !== 'all' && r.status !== statusFilter) return false;
    if (categoryFilter !== 'all' && r.category !== categoryFilter) return false;
    if (search.trim()) {
      const q = search.trim().toLowerCase();
      const hay = `${r.programCode || ''} ${r.title || ''} ${r.trainerName || ''}`.toLowerCase();
      if (!hay.includes(q)) return false;
    }
    return true;
  }), [rows, statusFilter, categoryFilter, search]);

  return (
    <EnterprisePage
      title="Training Programs"
      subtitle="Catalog of professional, agricultural, and veterinary training offerings"
      breadcrumbs={['Training', 'Programs']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={hasPermission(PERMISSIONS.MANAGE_TRAINING) && (
        <Button size="sm" onClick={() => setOpen(true)}><Plus className="h-4 w-4 mr-1" /> New Program</Button>
      )}
    >
      <div className="flex flex-wrap gap-3 mb-4">
        <Input className="max-w-xs" placeholder="Search code / title…" value={search} onChange={(e) => setSearch(e.target.value)} />
        <Select value={statusFilter} onValueChange={setStatusFilter}>
          <SelectTrigger className="w-48"><SelectValue placeholder="Status" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All statuses</SelectItem>
            {Object.keys(STATUS).map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
          </SelectContent>
        </Select>
        <Select value={categoryFilter} onValueChange={setCategoryFilter}>
          <SelectTrigger className="w-48"><SelectValue placeholder="Category" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All categories</SelectItem>
            {['PROFESSIONAL', 'AGRICULTURAL', 'VETERINARY', 'WORKSHOP', 'TECHNICAL', 'OTHER'].map((c) => (
              <SelectItem key={c} value={c}>{c}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <DataTable columns={cols} data={filtered} />

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>Create Training Program</DialogTitle></DialogHeader>
          <form onSubmit={create} className="space-y-3">
            <div className="space-y-1"><Label>Title</Label><Input required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label>Category</Label>
                <Select value={form.category} onValueChange={(v) => setForm({ ...form, category: v })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {['PROFESSIONAL', 'AGRICULTURAL', 'VETERINARY', 'WORKSHOP', 'TECHNICAL', 'OTHER'].map((c) => (
                      <SelectItem key={c} value={c}>{c}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1">
                <Label>Delivery</Label>
                <Select value={form.deliveryMode} onValueChange={(v) => setForm({ ...form, deliveryMode: v })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {['ONSITE', 'ONLINE', 'HYBRID'].map((c) => <SelectItem key={c} value={c}>{c}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Price (ETB)</Label><Input type="number" step="0.01" required value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} /></div>
              <div className="space-y-1"><Label>Default capacity</Label><Input type="number" value={form.defaultCapacity} onChange={(e) => setForm({ ...form, defaultCapacity: e.target.value })} /></div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Trainer</Label><Input value={form.trainerName} onChange={(e) => setForm({ ...form, trainerName: e.target.value })} /></div>
              <div className="space-y-1"><Label>Location</Label><Input value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} /></div>
            </div>
            <div className="space-y-1"><Label>Description</Label><Textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} rows={3} /></div>
            <div className="grid grid-cols-3 gap-2">
              <div className="space-y-1"><Label>Start</Label><Input type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></div>
              <div className="space-y-1"><Label>End</Label><Input type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} /></div>
              <div className="space-y-1"><Label>Reg. deadline</Label><Input type="date" value={form.registrationDeadline} onChange={(e) => setForm({ ...form, registrationDeadline: e.target.value })} /></div>
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

export default TrainingPrograms;
