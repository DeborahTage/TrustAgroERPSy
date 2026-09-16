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
import { formatDate } from '../../utils';
import { Plus } from 'lucide-react';
import { toast } from 'react-toastify';

const emptyForm = { code: '', name: '', description: '', active: true };

const HrDepartments = () => {
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
      const res = await hrApi.getDepartments();
      setRows(res.data.data || []);
    } catch {
      setError('Failed to load departments.');
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
      active: form.active,
    };
    try {
      if (editId) {
        await hrApi.updateDepartment(editId, payload);
        toast.success('Department updated');
      } else {
        await hrApi.createDepartment(payload);
        toast.success('Department created');
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
    { accessorKey: 'name', header: 'Department' },
    { accessorKey: 'description', header: 'Description', cell: ({ row }) => row.original.description || '—' },
    { accessorKey: 'employeeCount', header: 'Employees', cell: ({ row }) => <span className="font-mono">{row.original.employeeCount}</span> },
    { accessorKey: 'createdAt', header: 'Created', cell: ({ row }) => formatDate(row.original.createdAt) },
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
            const d = row.original;
            setEditId(d.id);
            setForm({ code: d.code || '', name: d.name || '', description: d.description || '', active: d.active });
            setOpen(true);
          }}>Edit</Button>
        </div>
      ),
    },
  ];

  return (
    <EnterprisePage
      title="Departments"
      subtitle="Organisational units employees are assigned to"
      breadcrumbs={['HR', 'Departments']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={hasPermission(PERMISSIONS.CREATE_HR) && (
        <Button size="sm" onClick={() => { setEditId(null); setForm(emptyForm); setOpen(true); }}>
          <Plus className="h-4 w-4 mr-1" /> New Department
        </Button>
      )}
    >
      <DataTable columns={cols} data={rows} emptyMessage="No departments configured" />

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader><DialogTitle>{editId ? 'Edit Department' : 'New Department'}</DialogTitle></DialogHeader>
          <form onSubmit={submit} className="space-y-3">
            <div className="space-y-1"><Label>Code</Label><Input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} placeholder="Auto-generated when blank" /></div>
            <div className="space-y-1"><Label>Name</Label><Input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
            <div className="space-y-1"><Label>Description</Label><Textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
            <div className="flex items-center gap-2">
              <input type="checkbox" id="dept-active" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
              <Label htmlFor="dept-active">Active</Label>
            </div>
            <DialogFooter><Button type="submit" disabled={!form.name}>{editId ? 'Save' : 'Create'}</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default HrDepartments;
