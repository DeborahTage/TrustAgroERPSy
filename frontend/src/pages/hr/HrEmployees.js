import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { hrApi } from '../../api';
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
import { formatCurrency, formatDate } from '../../utils';
import { Plus } from 'lucide-react';
import { toast } from 'react-toastify';

const EMPLOYMENT_STATUSES = ['ACTIVE', 'ON_LEAVE', 'SUSPENDED', 'TERMINATED', 'RESIGNED', 'INACTIVE'];
const EMPLOYMENT_TYPES = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'TEMPORARY', 'INTERN'];

const emptyForm = {
  firstName: '', middleName: '', lastName: '', phone: '', email: '', address: '',
  hireDate: '', employmentType: 'FULL_TIME', departmentId: '', positionId: '', managerId: '',
  basicSalary: '', currency: 'ETB', bankName: '', bankAccountRef: '', emergencyContact: '', notes: '',
};

const HrEmployees = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [positions, setPositions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [deptFilter, setDeptFilter] = useState('all');
  const [positionFilter, setPositionFilter] = useState('all');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);

  const canSeeSalary = hasPermission(PERMISSIONS.HR_PAYROLL);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [empRes, deptRes, posRes] = await Promise.all([
        hrApi.getEmployees(),
        hrApi.getDepartments({ activeOnly: true }).catch(() => ({ data: { data: [] } })),
        hrApi.getPositions({ activeOnly: true }).catch(() => ({ data: { data: [] } })),
      ]);
      setRows(empRes.data.data || []);
      setDepartments(deptRes.data?.data || []);
      setPositions(posRes.data?.data || []);
    } catch {
      setError('Failed to load employees.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const create = async (e) => {
    e.preventDefault();
    try {
      await hrApi.createEmployee({
        firstName: form.firstName,
        middleName: form.middleName || undefined,
        lastName: form.lastName,
        phone: form.phone || undefined,
        email: form.email || undefined,
        address: form.address || undefined,
        hireDate: form.hireDate || undefined,
        employmentType: form.employmentType,
        departmentId: form.departmentId ? Number(form.departmentId) : undefined,
        positionId: form.positionId ? Number(form.positionId) : undefined,
        managerId: form.managerId ? Number(form.managerId) : undefined,
        basicSalary: form.basicSalary ? parseFloat(form.basicSalary) : undefined,
        currency: form.currency || undefined,
        bankName: form.bankName || undefined,
        bankAccountRef: form.bankAccountRef || undefined,
        emergencyContact: form.emergencyContact || undefined,
        notes: form.notes || undefined,
      });
      toast.success('Employee created');
      setOpen(false);
      setForm(emptyForm);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Create failed');
    }
  };

  const filtered = useMemo(() => rows.filter((r) => {
    if (statusFilter !== 'all' && r.employmentStatus !== statusFilter) return false;
    if (deptFilter !== 'all' && String(r.departmentId) !== deptFilter) return false;
    if (positionFilter !== 'all' && String(r.positionId) !== positionFilter) return false;
    if (search.trim()) {
      const q = search.trim().toLowerCase();
      const hay = `${r.employeeNumber || ''} ${r.fullName || ''} ${r.phone || ''} ${r.email || ''}`.toLowerCase();
      if (!hay.includes(q)) return false;
    }
    return true;
  }), [rows, statusFilter, deptFilter, positionFilter, search]);

  const cols = [
    {
      accessorKey: 'employeeNumber',
      header: 'Employee #',
      cell: ({ row }) => <span className="font-mono font-bold">{row.original.employeeNumber}</span>,
    },
    { accessorKey: 'fullName', header: 'Name' },
    { accessorKey: 'departmentName', header: 'Department', cell: ({ row }) => row.original.departmentName || '—' },
    { accessorKey: 'positionName', header: 'Position', cell: ({ row }) => row.original.positionName || '—' },
    { accessorKey: 'employmentType', header: 'Type', cell: ({ row }) => (row.original.employmentType || '—').replace(/_/g, ' ') },
    { accessorKey: 'hireDate', header: 'Hired', cell: ({ row }) => formatDate(row.original.hireDate) },
    ...(canSeeSalary ? [{
      accessorKey: 'basicSalary',
      header: 'Basic Salary',
      cell: ({ row }) => <span className="font-mono">{row.original.basicSalary != null ? formatCurrency(row.original.basicSalary) : '—'}</span>,
    }] : []),
    {
      accessorKey: 'employmentStatus',
      header: 'Status',
      cell: ({ row }) => <Badge variant="outline">{row.original.employmentStatus}</Badge>,
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <div className="flex justify-end">
          <Button size="sm" variant="outline" onClick={() => navigate(`/hr/employees/${row.original.id}`)}>Open</Button>
        </div>
      ),
    },
  ];

  return (
    <EnterprisePage
      title="Employee Directory"
      subtitle="Employee records, org assignment, and employment status"
      breadcrumbs={['HR', 'Employees']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={hasPermission(PERMISSIONS.CREATE_HR) && (
        <Button size="sm" onClick={() => setOpen(true)}><Plus className="h-4 w-4 mr-1" /> New Employee</Button>
      )}
    >
      <div className="flex flex-wrap gap-3 mb-4">
        <Input className="max-w-xs" placeholder="Search name, number, phone…" value={search} onChange={(e) => setSearch(e.target.value)} />
        <Select value={statusFilter} onValueChange={setStatusFilter}>
          <SelectTrigger className="w-44"><SelectValue placeholder="Status" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All statuses</SelectItem>
            {EMPLOYMENT_STATUSES.map((s) => <SelectItem key={s} value={s}>{s.replace(/_/g, ' ')}</SelectItem>)}
          </SelectContent>
        </Select>
        <Select value={deptFilter} onValueChange={setDeptFilter}>
          <SelectTrigger className="w-48"><SelectValue placeholder="Department" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All departments</SelectItem>
            {departments.map((d) => <SelectItem key={d.id} value={String(d.id)}>{d.name}</SelectItem>)}
          </SelectContent>
        </Select>
        <Select value={positionFilter} onValueChange={setPositionFilter}>
          <SelectTrigger className="w-48"><SelectValue placeholder="Position" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All positions</SelectItem>
            {positions.map((p) => <SelectItem key={p.id} value={String(p.id)}>{p.name}</SelectItem>)}
          </SelectContent>
        </Select>
      </div>

      <DataTable columns={cols} data={filtered} searchable={false} emptyMessage="No employees found" />

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>New Employee</DialogTitle></DialogHeader>
          <form onSubmit={create} className="space-y-3">
            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-1"><Label>First Name</Label><Input required value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} /></div>
              <div className="space-y-1"><Label>Middle Name</Label><Input value={form.middleName} onChange={(e) => setForm({ ...form, middleName: e.target.value })} /></div>
              <div className="space-y-1"><Label>Last Name</Label><Input required value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} /></div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Phone</Label><Input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></div>
              <div className="space-y-1"><Label>Email</Label><Input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></div>
            </div>
            <div className="space-y-1"><Label>Address</Label><Input value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Hire Date</Label><Input type="date" value={form.hireDate} onChange={(e) => setForm({ ...form, hireDate: e.target.value })} /></div>
              <div className="space-y-1">
                <Label>Employment Type</Label>
                <Select value={form.employmentType} onValueChange={(v) => setForm({ ...form, employmentType: v })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>{EMPLOYMENT_TYPES.map((t) => <SelectItem key={t} value={t}>{t.replace(/_/g, ' ')}</SelectItem>)}</SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label>Department</Label>
                <Select value={form.departmentId || 'none'} onValueChange={(v) => setForm({ ...form, departmentId: v === 'none' ? '' : v })}>
                  <SelectTrigger><SelectValue placeholder="Optional" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">None</SelectItem>
                    {departments.map((d) => <SelectItem key={d.id} value={String(d.id)}>{d.name}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1">
                <Label>Position</Label>
                <Select value={form.positionId || 'none'} onValueChange={(v) => setForm({ ...form, positionId: v === 'none' ? '' : v })}>
                  <SelectTrigger><SelectValue placeholder="Optional" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">None</SelectItem>
                    {positions.map((p) => <SelectItem key={p.id} value={String(p.id)}>{p.name}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label>Manager</Label>
                <Select value={form.managerId || 'none'} onValueChange={(v) => setForm({ ...form, managerId: v === 'none' ? '' : v })}>
                  <SelectTrigger><SelectValue placeholder="Optional" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">None</SelectItem>
                    {rows.map((r) => <SelectItem key={r.id} value={String(r.id)}>{r.fullName}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1"><Label>Emergency Contact</Label><Input value={form.emergencyContact} onChange={(e) => setForm({ ...form, emergencyContact: e.target.value })} /></div>
            </div>
            {canSeeSalary && (
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1"><Label>Basic Salary</Label><Input type="number" step="0.01" value={form.basicSalary} onChange={(e) => setForm({ ...form, basicSalary: e.target.value })} /></div>
                <div className="space-y-1"><Label>Currency</Label><Input value={form.currency} onChange={(e) => setForm({ ...form, currency: e.target.value })} /></div>
              </div>
            )}
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Bank Name</Label><Input value={form.bankName} onChange={(e) => setForm({ ...form, bankName: e.target.value })} /></div>
              <div className="space-y-1"><Label>Bank Account Ref</Label><Input value={form.bankAccountRef} onChange={(e) => setForm({ ...form, bankAccountRef: e.target.value })} /></div>
            </div>
            <div className="space-y-1"><Label>Notes</Label><Textarea value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></div>
            <DialogFooter>
              <Button type="submit" disabled={!form.firstName || !form.lastName}>Create</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default HrEmployees;
