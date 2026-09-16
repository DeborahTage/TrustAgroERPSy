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

const LEAVE_STATUSES = ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'];

const emptyForm = { employeeId: '', leaveTypeId: '', startDate: '', endDate: '', reason: '' };

const HrLeaveRequests = () => {
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [leaveTypes, setLeaveTypes] = useState([]);
  const [balances, setBalances] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [employeeFilter, setEmployeeFilter] = useState('all');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [rejectId, setRejectId] = useState(null);
  const [rejectReason, setRejectReason] = useState('');

  const canCreate = hasPermission(PERMISSIONS.HR_LEAVE);
  const canDecide = hasPermission(PERMISSIONS.HR_LEAVE_APPROVE);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [reqRes, empRes, typeRes] = await Promise.all([
        hrApi.getLeaveRequests({
          employeeId: employeeFilter !== 'all' ? employeeFilter : undefined,
          status: statusFilter !== 'all' ? statusFilter : undefined,
        }),
        hrApi.getEmployees({ status: 'ACTIVE' }).catch(() => ({ data: { data: [] } })),
        hrApi.getLeaveTypes({ activeOnly: true }).catch(() => ({ data: { data: [] } })),
      ]);
      setRows(reqRes.data.data || []);
      setEmployees(empRes.data?.data || []);
      setLeaveTypes(typeRes.data?.data || []);
    } catch {
      setError('Failed to load leave requests.');
    } finally {
      setLoading(false);
    }
  }, [employeeFilter, statusFilter]);

  useEffect(() => { load(); }, [load]);

  const loadBalances = useCallback(async (employeeId) => {
    if (!employeeId) {
      setBalances([]);
      return;
    }
    try {
      const res = await hrApi.getEmployeeLeaveBalances(employeeId);
      setBalances(res.data?.data || []);
    } catch {
      setBalances([]);
    }
  }, []);

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
    await act(() => hrApi.createLeaveRequest({
      employeeId: Number(form.employeeId),
      leaveTypeId: Number(form.leaveTypeId),
      startDate: form.startDate,
      endDate: form.endDate,
      reason: form.reason || undefined,
    }), 'Leave request submitted');
    setOpen(false);
    setForm(emptyForm);
    setBalances([]);
  };

  const submitReject = async (e) => {
    e.preventDefault();
    await act(() => hrApi.rejectLeaveRequest(rejectId, { reason: rejectReason || undefined }), 'Leave rejected');
    setRejectId(null);
    setRejectReason('');
  };

  const filtered = useMemo(() => rows.filter((r) => {
    if (!search.trim()) return true;
    const q = search.trim().toLowerCase();
    const hay = `${r.requestNumber || ''} ${r.employeeName || ''} ${r.employeeNumber || ''} ${r.leaveTypeName || ''}`.toLowerCase();
    return hay.includes(q);
  }), [rows, search]);

  const cols = [
    { accessorKey: 'requestNumber', header: 'Request #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.requestNumber}</span> },
    { accessorKey: 'employeeName', header: 'Employee' },
    { accessorKey: 'departmentName', header: 'Department', cell: ({ row }) => row.original.departmentName || '—' },
    { accessorKey: 'leaveTypeName', header: 'Leave Type' },
    { accessorKey: 'startDate', header: 'From', cell: ({ row }) => formatDate(row.original.startDate) },
    { accessorKey: 'endDate', header: 'To', cell: ({ row }) => formatDate(row.original.endDate) },
    { accessorKey: 'totalDays', header: 'Days', cell: ({ row }) => <span className="font-mono">{row.original.totalDays}</span> },
    { accessorKey: 'paid', header: 'Paid', cell: ({ row }) => (row.original.paid ? 'Paid' : 'Unpaid') },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge>,
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => {
        const r = row.original;
        return (
          <div className="flex flex-wrap gap-1 justify-end">
            {canDecide && r.status === 'PENDING' && (
              <Button size="sm" onClick={() => {
                if (!window.confirm(`Approve ${r.totalDays} day(s) of ${r.leaveTypeName} for ${r.employeeName}?`)) return;
                act(() => hrApi.approveLeaveRequest(r.id), 'Leave approved');
              }}>Approve</Button>
            )}
            {canDecide && r.status === 'PENDING' && (
              <Button size="sm" variant="destructive" onClick={() => { setRejectId(r.id); setRejectReason(''); }}>Reject</Button>
            )}
            {(canCreate || canDecide) && ['PENDING', 'APPROVED'].includes(r.status) && (
              <Button size="sm" variant="outline" onClick={() => {
                if (!window.confirm('Cancel this leave request?')) return;
                act(() => hrApi.cancelLeaveRequest(r.id), 'Leave cancelled');
              }}>Cancel</Button>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <EnterprisePage
      title="Leave Requests"
      subtitle="Submit, approve, and track employee leave against balances"
      breadcrumbs={['HR', 'Leave']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={canCreate && (
        <Button size="sm" onClick={() => { setForm(emptyForm); setBalances([]); setOpen(true); }}>
          <Plus className="h-4 w-4 mr-1" /> New Request
        </Button>
      )}
    >
      <div className="flex flex-wrap gap-3 mb-4">
        <Input className="max-w-xs" placeholder="Search request, employee…" value={search} onChange={(e) => setSearch(e.target.value)} />
        <Select value={statusFilter} onValueChange={setStatusFilter}>
          <SelectTrigger className="w-44"><SelectValue placeholder="Status" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All statuses</SelectItem>
            {LEAVE_STATUSES.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
          </SelectContent>
        </Select>
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

      <DataTable columns={cols} data={filtered} searchable={false} emptyMessage="No leave requests found" />

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>New Leave Request</DialogTitle></DialogHeader>
          <form onSubmit={create} className="space-y-3">
            <div className="space-y-1">
              <Label>Employee</Label>
              <Select value={form.employeeId} onValueChange={(v) => { setForm({ ...form, employeeId: v }); loadBalances(v); }}>
                <SelectTrigger><SelectValue placeholder="Select employee" /></SelectTrigger>
                <SelectContent>
                  {employees.map((emp) => (
                    <SelectItem key={emp.id} value={String(emp.id)}>{emp.employeeNumber} · {emp.fullName}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label>Leave Type</Label>
              <Select value={form.leaveTypeId} onValueChange={(v) => setForm({ ...form, leaveTypeId: v })}>
                <SelectTrigger><SelectValue placeholder="Select leave type" /></SelectTrigger>
                <SelectContent>
                  {leaveTypes.map((t) => <SelectItem key={t.id} value={String(t.id)}>{t.name}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            {balances.length > 0 && (
              <div className="rounded-lg border p-3 space-y-1 text-xs">
                <p className="font-semibold text-slate-500 uppercase">Balances</p>
                {balances.map((b) => (
                  <div key={b.id} className="flex justify-between">
                    <span>{b.leaveTypeName}</span>
                    <span className="font-mono">{b.remainingDays} of {b.entitlement} days left</span>
                  </div>
                ))}
              </div>
            )}
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Start Date</Label><Input type="date" required value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></div>
              <div className="space-y-1"><Label>End Date</Label><Input type="date" required value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} /></div>
            </div>
            <div className="space-y-1"><Label>Reason</Label><Textarea value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} /></div>
            <DialogFooter>
              <Button type="submit" disabled={!form.employeeId || !form.leaveTypeId || !form.startDate || !form.endDate}>Submit</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={!!rejectId} onOpenChange={(v) => { if (!v) setRejectId(null); }}>
        <DialogContent>
          <DialogHeader><DialogTitle>Reject Leave Request</DialogTitle></DialogHeader>
          <form onSubmit={submitReject} className="space-y-3">
            <div className="space-y-1"><Label>Reason</Label><Textarea required value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} /></div>
            <DialogFooter><Button type="submit" variant="destructive" disabled={!rejectReason}>Reject</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default HrLeaveRequests;
