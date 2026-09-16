import React, { useCallback, useEffect, useState } from 'react';
import { hrApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../../components/ui/dialog';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { Plus } from 'lucide-react';
import { toast } from 'react-toastify';

const emptyForm = {
  code: '', name: '', description: '', balanceControlled: true, defaultAnnualEntitlement: '', paid: true, active: true,
};

const HrLeaveTypes = () => {
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [open, setOpen] = useState(false);
  const [editId, setEditId] = useState(null);
  const [form, setForm] = useState(emptyForm);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await hrApi.getLeaveTypes({ activeOnly: false });
      setRows(res.data.data || []);
    } catch {
      setError('Failed to load leave types.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const submit = async (e) => {
    e.preventDefault();
    const payload = {
      code: form.code || undefined,
      name: form.name,
      description: form.description || undefined,
      balanceControlled: form.balanceControlled,
      defaultAnnualEntitlement: form.defaultAnnualEntitlement ? parseFloat(form.defaultAnnualEntitlement) : undefined,
      paid: form.paid,
      active: form.active,
    };
    try {
      if (editId) {
        await hrApi.updateLeaveType(editId, payload);
        toast.success('Leave type updated');
      } else {
        await hrApi.createLeaveType(payload);
        toast.success('Leave type created');
      }
      setOpen(false);
      setEditId(null);
      setForm(emptyForm);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Save failed');
    }
  };

  const cols = [
    { accessorKey: 'code', header: 'Code', cell: ({ row }) => <span className="font-mono font-bold">{row.original.code || '—'}</span> },
    { accessorKey: 'name', header: 'Leave Type' },
    { accessorKey: 'description', header: 'Description', cell: ({ row }) => row.original.description || '—' },
    {
      accessorKey: 'defaultAnnualEntitlement',
      header: 'Annual Days',
      cell: ({ row }) => <span className="font-mono">{row.original.defaultAnnualEntitlement ?? '—'}</span>,
    },
    {
      accessorKey: 'balanceControlled',
      header: 'Balance Controlled',
      cell: ({ row }) => (row.original.balanceControlled ? 'Yes' : 'No'),
    },
    { accessorKey: 'paid', header: 'Paid', cell: ({ row }) => (row.original.paid ? 'Paid' : 'Unpaid') },
    {
      accessorKey: 'active',
      header: 'Status',
      cell: ({ row }) => <Badge variant="outline">{row.original.active ? 'ACTIVE' : 'INACTIVE'}</Badge>,
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => hasPermission(PERMISSIONS.EDIT_HR) && (
        <div className="flex justify-end">
          <Button size="sm" variant="outline" onClick={() => {
            const t = row.original;
            setEditId(t.id);
            setForm({
              code: t.code || '',
              name: t.name || '',
              description: t.description || '',
              balanceControlled: t.balanceControlled,
              defaultAnnualEntitlement: t.defaultAnnualEntitlement != null ? String(t.defaultAnnualEntitlement) : '',
              paid: t.paid,
              active: t.active,
            });
            setOpen(true);
          }}>Edit</Button>
        </div>
      ),
    },
  ];

  return (
    <EnterprisePage
      title="Leave Types"
      subtitle="Entitlement rules that drive leave balances"
      breadcrumbs={['HR', 'Leave Types']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={hasPermission(PERMISSIONS.CREATE_HR) && (
        <Button size="sm" onClick={() => { setEditId(null); setForm(emptyForm); setOpen(true); }}>
          <Plus className="h-4 w-4 mr-1" /> New Leave Type
        </Button>
      )}
    >
      <DataTable columns={cols} data={rows} emptyMessage="No leave types configured" />

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>{editId ? 'Edit Leave Type' : 'New Leave Type'}</DialogTitle></DialogHeader>
          <form onSubmit={submit} className="space-y-3">
            <div className="space-y-1"><Label>Code</Label><Input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} placeholder="Auto-generated when blank" /></div>
            <div className="space-y-1"><Label>Name</Label><Input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
            <div className="space-y-1"><Label>Description</Label><Textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
            <div className="space-y-1">
              <Label>Default Annual Entitlement (days)</Label>
              <Input type="number" step="0.5" value={form.defaultAnnualEntitlement} onChange={(e) => setForm({ ...form, defaultAnnualEntitlement: e.target.value })} />
            </div>
            <div className="flex flex-wrap gap-6">
              <div className="flex items-center gap-2">
                <input type="checkbox" id="lt-balance" checked={form.balanceControlled} onChange={(e) => setForm({ ...form, balanceControlled: e.target.checked })} />
                <Label htmlFor="lt-balance">Balance controlled</Label>
              </div>
              <div className="flex items-center gap-2">
                <input type="checkbox" id="lt-paid" checked={form.paid} onChange={(e) => setForm({ ...form, paid: e.target.checked })} />
                <Label htmlFor="lt-paid">Paid leave</Label>
              </div>
              <div className="flex items-center gap-2">
                <input type="checkbox" id="lt-active" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
                <Label htmlFor="lt-active">Active</Label>
              </div>
            </div>
            <DialogFooter><Button type="submit" disabled={!form.name}>{editId ? 'Save' : 'Create'}</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default HrLeaveTypes;
