import React, { useCallback, useEffect, useMemo, useState } from 'react';
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
import { formatDate } from '../../utils';
import { Plus } from 'lucide-react';
import { toast } from 'react-toastify';

const ATTENDANCE_STATUSES = ['PRESENT', 'ABSENT', 'LATE', 'HALF_DAY', 'ON_LEAVE', 'HOLIDAY', 'REST_DAY'];

const today = () => new Date().toISOString().slice(0, 10);
const monthStart = () => `${new Date().toISOString().slice(0, 7)}-01`;

const emptyForm = {
  employeeId: '', attendanceDate: today(), checkIn: '', checkOut: '', status: 'PRESENT', notes: '',
};

const HrAttendance = () => {
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [from, setFrom] = useState(monthStart());
  const [to, setTo] = useState(today());
  const [employeeFilter, setEmployeeFilter] = useState('all');
  const [deptFilter, setDeptFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');
  const [open, setOpen] = useState(false);
  const [editId, setEditId] = useState(null);
  const [form, setForm] = useState(emptyForm);

  const canWrite = hasPermission(PERMISSIONS.HR_ATTENDANCE);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [attRes, empRes, deptRes] = await Promise.all([
        hrApi.getAttendance({
          employeeId: employeeFilter !== 'all' ? employeeFilter : undefined,
          from: from || undefined,
          to: to || undefined,
        }),
        hrApi.getEmployees({ status: 'ACTIVE' }).catch(() => ({ data: { data: [] } })),
        hrApi.getDepartments({ activeOnly: true }).catch(() => ({ data: { data: [] } })),
      ]);
      setRows(attRes.data.data || []);
      setEmployees(empRes.data?.data || []);
      setDepartments(deptRes.data?.data || []);
    } catch {
      setError('Failed to load attendance.');
    } finally {
      setLoading(false);
    }
  }, [employeeFilter, from, to]);

  useEffect(() => { load(); }, [load]);

  const submit = async (e) => {
    e.preventDefault();
    const payload = {
      employeeId: Number(form.employeeId),
      attendanceDate: form.attendanceDate,
      checkIn: form.checkIn || undefined,
      checkOut: form.checkOut || undefined,
      status: form.status,
      notes: form.notes || undefined,
    };
    try {
      if (editId) {
        await hrApi.updateAttendance(editId, payload);
        toast.success('Attendance updated');
      } else {
        await hrApi.recordAttendance(payload);
        toast.success('Attendance recorded');
      }
      setOpen(false);
      setEditId(null);
      setForm(emptyForm);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Save failed');
    }
  };

  const filtered = useMemo(() => rows.filter((r) => {
    if (statusFilter !== 'all' && r.status !== statusFilter) return false;
    if (deptFilter !== 'all') {
      const dept = departments.find((d) => String(d.id) === deptFilter);
      if (!dept || r.departmentName !== dept.name) return false;
    }
    return true;
  }), [rows, statusFilter, deptFilter, departments]);

  const cols = [
    { accessorKey: 'attendanceDate', header: 'Date', cell: ({ row }) => formatDate(row.original.attendanceDate) },
    { accessorKey: 'employeeNumber', header: 'Employee #', cell: ({ row }) => <span className="font-mono">{row.original.employeeNumber}</span> },
    { accessorKey: 'employeeName', header: 'Employee' },
    { accessorKey: 'departmentName', header: 'Department', cell: ({ row }) => row.original.departmentName || '—' },
    { accessorKey: 'checkIn', header: 'Check In', cell: ({ row }) => row.original.checkIn || '—' },
    { accessorKey: 'checkOut', header: 'Check Out', cell: ({ row }) => row.original.checkOut || '—' },
    { accessorKey: 'hoursWorked', header: 'Hours', cell: ({ row }) => <span className="font-mono">{row.original.hoursWorked ?? '—'}</span> },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge>,
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => canWrite && (
        <div className="flex justify-end">
          <Button size="sm" variant="outline" onClick={() => {
            const a = row.original;
            setEditId(a.id);
            setForm({
              employeeId: String(a.employeeId),
              attendanceDate: a.attendanceDate || today(),
              checkIn: a.checkIn || '',
              checkOut: a.checkOut || '',
              status: a.status || 'PRESENT',
              notes: a.notes || '',
            });
            setOpen(true);
          }}>Edit</Button>
        </div>
      ),
    },
  ];

  return (
    <EnterprisePage
      title="Attendance"
      subtitle="Daily attendance records with check-in and check-out times"
      breadcrumbs={['HR', 'Attendance']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={canWrite && (
        <Button size="sm" onClick={() => { setEditId(null); setForm(emptyForm); setOpen(true); }}>
          <Plus className="h-4 w-4 mr-1" /> Record Attendance
        </Button>
      )}
    >
      <div className="flex flex-wrap gap-3 mb-4">
        <div className="space-y-1">
          <Label className="text-xs">From</Label>
          <Input type="date" className="w-40" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div className="space-y-1">
          <Label className="text-xs">To</Label>
          <Input type="date" className="w-40" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
        <div className="space-y-1">
          <Label className="text-xs">Employee</Label>
          <Select value={employeeFilter} onValueChange={setEmployeeFilter}>
            <SelectTrigger className="w-56"><SelectValue placeholder="Employee" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All employees</SelectItem>
              {employees.map((emp) => (
                <SelectItem key={emp.id} value={String(emp.id)}>{emp.employeeNumber} · {emp.fullName}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1">
          <Label className="text-xs">Department</Label>
          <Select value={deptFilter} onValueChange={setDeptFilter}>
            <SelectTrigger className="w-48"><SelectValue placeholder="Department" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All departments</SelectItem>
              {departments.map((d) => <SelectItem key={d.id} value={String(d.id)}>{d.name}</SelectItem>)}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1">
          <Label className="text-xs">Status</Label>
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="w-44"><SelectValue placeholder="Status" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All statuses</SelectItem>
              {ATTENDANCE_STATUSES.map((s) => <SelectItem key={s} value={s}>{s.replace(/_/g, ' ')}</SelectItem>)}
            </SelectContent>
          </Select>
        </div>
      </div>

      <DataTable columns={cols} data={filtered} emptyMessage="No attendance records for this range" />

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader><DialogTitle>{editId ? 'Edit Attendance' : 'Record Attendance'}</DialogTitle></DialogHeader>
          <form onSubmit={submit} className="space-y-3">
            <div className="space-y-1">
              <Label>Employee</Label>
              <Select value={form.employeeId} onValueChange={(v) => setForm({ ...form, employeeId: v })} disabled={!!editId}>
                <SelectTrigger><SelectValue placeholder="Select employee" /></SelectTrigger>
                <SelectContent>
                  {employees.map((emp) => (
                    <SelectItem key={emp.id} value={String(emp.id)}>{emp.employeeNumber} · {emp.fullName}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Date</Label><Input type="date" required value={form.attendanceDate} onChange={(e) => setForm({ ...form, attendanceDate: e.target.value })} /></div>
              <div className="space-y-1">
                <Label>Status</Label>
                <Select value={form.status} onValueChange={(v) => setForm({ ...form, status: v })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>{ATTENDANCE_STATUSES.map((s) => <SelectItem key={s} value={s}>{s.replace(/_/g, ' ')}</SelectItem>)}</SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Check In</Label><Input type="time" value={form.checkIn} onChange={(e) => setForm({ ...form, checkIn: e.target.value })} /></div>
              <div className="space-y-1"><Label>Check Out</Label><Input type="time" value={form.checkOut} onChange={(e) => setForm({ ...form, checkOut: e.target.value })} /></div>
            </div>
            <div className="space-y-1"><Label>Notes</Label><Textarea value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></div>
            <DialogFooter><Button type="submit" disabled={!form.employeeId || !form.attendanceDate}>{editId ? 'Save' : 'Record'}</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default HrAttendance;
