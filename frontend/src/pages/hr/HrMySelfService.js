import React, { useCallback, useEffect, useState } from 'react';
import { hrApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../../components/ui/dialog';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '../../components/ui/tabs';
import { formatCurrency, formatDate } from '../../utils';
import { Plus } from 'lucide-react';
import { toast } from 'react-toastify';

const emptyForm = { leaveTypeId: '', startDate: '', endDate: '', reason: '' };

const HrMySelfService = () => {
  const [profile, setProfile] = useState(null);
  const [attendance, setAttendance] = useState([]);
  const [leave, setLeave] = useState([]);
  const [balances, setBalances] = useState([]);
  const [payslips, setPayslips] = useState([]);
  const [leaveTypes, setLeaveTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [payslip, setPayslip] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const profileRes = await hrApi.getMyProfile();
      setProfile(profileRes.data.data);

      const [attRes, leaveRes, balanceRes, slipRes, typeRes] = await Promise.all([
        hrApi.getMyAttendance().catch(() => ({ data: { data: [] } })),
        hrApi.getMyLeave().catch(() => ({ data: { data: [] } })),
        hrApi.getMyLeaveBalances().catch(() => ({ data: { data: [] } })),
        hrApi.getMyPayslips().catch(() => ({ data: { data: [] } })),
        hrApi.getLeaveTypes({ activeOnly: true }).catch(() => ({ data: { data: [] } })),
      ]);
      setAttendance(attRes.data?.data || []);
      setLeave(leaveRes.data?.data || []);
      setBalances(balanceRes.data?.data || []);
      setPayslips(slipRes.data?.data || []);
      setLeaveTypes(typeRes.data?.data || []);
    } catch (err) {
      setError(err.response?.status === 404
        ? 'No employee record is linked to your login yet. Ask HR to link your profile.'
        : 'Failed to load your HR self-service data.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const submitLeave = async (e) => {
    e.preventDefault();
    try {
      await hrApi.submitMyLeave({
        leaveTypeId: Number(form.leaveTypeId),
        startDate: form.startDate,
        endDate: form.endDate,
        reason: form.reason || undefined,
      });
      toast.success('Leave request submitted');
      setOpen(false);
      setForm(emptyForm);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Submit failed');
    }
  };

  const cancelLeave = async (id) => {
    if (!window.confirm('Cancel this leave request?')) return;
    try {
      await hrApi.cancelLeaveRequest(id);
      toast.success('Leave cancelled');
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Cancel failed');
    }
  };

  const attendanceCols = [
    { accessorKey: 'attendanceDate', header: 'Date', cell: ({ row }) => formatDate(row.original.attendanceDate) },
    { accessorKey: 'checkIn', header: 'Check In', cell: ({ row }) => row.original.checkIn || '—' },
    { accessorKey: 'checkOut', header: 'Check Out', cell: ({ row }) => row.original.checkOut || '—' },
    { accessorKey: 'hoursWorked', header: 'Hours', cell: ({ row }) => <span className="font-mono">{row.original.hoursWorked ?? '—'}</span> },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
  ];

  const leaveCols = [
    { accessorKey: 'requestNumber', header: 'Request #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.requestNumber}</span> },
    { accessorKey: 'leaveTypeName', header: 'Leave Type' },
    { accessorKey: 'startDate', header: 'From', cell: ({ row }) => formatDate(row.original.startDate) },
    { accessorKey: 'endDate', header: 'To', cell: ({ row }) => formatDate(row.original.endDate) },
    { accessorKey: 'totalDays', header: 'Days', cell: ({ row }) => <span className="font-mono">{row.original.totalDays}</span> },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => ['PENDING', 'APPROVED'].includes(row.original.status) && (
        <div className="flex justify-end">
          <Button size="sm" variant="outline" onClick={() => cancelLeave(row.original.id)}>Cancel</Button>
        </div>
      ),
    },
  ];

  const payslipCols = [
    { accessorKey: 'payslipNumber', header: 'Payslip #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.payslipNumber}</span> },
    { accessorKey: 'periodCode', header: 'Period', cell: ({ row }) => <span className="font-mono">{row.original.periodCode}</span> },
    {
      id: 'range',
      header: 'Range',
      cell: ({ row }) => `${formatDate(row.original.periodStartDate)} – ${formatDate(row.original.periodEndDate)}`,
    },
    { accessorKey: 'grossSalary', header: 'Gross', cell: ({ row }) => <span className="font-mono">{formatCurrency(row.original.grossSalary)}</span> },
    { accessorKey: 'totalDeductions', header: 'Deductions', cell: ({ row }) => <span className="font-mono">{formatCurrency(row.original.totalDeductions)}</span> },
    { accessorKey: 'netSalary', header: 'Net', cell: ({ row }) => <span className="font-mono font-bold">{formatCurrency(row.original.netSalary)}</span> },
    { accessorKey: 'paymentStatus', header: 'Payment', cell: ({ row }) => <Badge variant="outline">{row.original.paymentStatus}</Badge> },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <div className="flex justify-end">
          <Button size="sm" variant="outline" onClick={() => setPayslip(row.original)}>View</Button>
        </div>
      ),
    },
  ];

  return (
    <EnterprisePage
      title="My Self-Service"
      subtitle="Your employment record, attendance, leave, and payslips"
      breadcrumbs={['HR', 'My Self-Service']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={profile && (
        <Button size="sm" onClick={() => { setForm(emptyForm); setOpen(true); }}>
          <Plus className="h-4 w-4 mr-1" /> Request Leave
        </Button>
      )}
    >
      {profile && (
        <Tabs defaultValue="profile" className="space-y-4">
          <TabsList>
            <TabsTrigger value="profile">Profile</TabsTrigger>
            <TabsTrigger value="attendance">Attendance</TabsTrigger>
            <TabsTrigger value="leave">Leave</TabsTrigger>
            <TabsTrigger value="payslips">Payslips</TabsTrigger>
          </TabsList>

          <TabsContent value="profile">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <Card className="border-none shadow-sm">
                <CardHeader>
                  <CardTitle className="text-base">Employment</CardTitle>
                  <CardDescription>{profile.employeeNumber}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-2 text-sm">
                  <div className="flex justify-between"><span>Name</span><span className="font-semibold">{profile.fullName}</span></div>
                  <div className="flex justify-between"><span>Status</span><Badge variant="outline">{profile.employmentStatus}</Badge></div>
                  <div className="flex justify-between"><span>Type</span><span>{(profile.employmentType || '—').replace(/_/g, ' ')}</span></div>
                  <div className="flex justify-between"><span>Department</span><span>{profile.departmentName || '—'}</span></div>
                  <div className="flex justify-between"><span>Position</span><span>{profile.positionName || '—'}</span></div>
                  <div className="flex justify-between"><span>Manager</span><span>{profile.managerName || '—'}</span></div>
                  <div className="flex justify-between"><span>Hired</span><span>{formatDate(profile.hireDate)}</span></div>
                  {profile.basicSalary != null && (
                    <div className="flex justify-between"><span>Basic salary</span><span className="font-mono font-bold">{formatCurrency(profile.basicSalary)}</span></div>
                  )}
                </CardContent>
              </Card>

              <Card className="border-none shadow-sm">
                <CardHeader>
                  <CardTitle className="text-base">My Leave Balances</CardTitle>
                  <CardDescription>Entitlement for the current year</CardDescription>
                </CardHeader>
                <CardContent className="space-y-2 text-sm">
                  {balances.length === 0 ? (
                    <p className="text-slate-500 text-center py-4">No balance-controlled leave types.</p>
                  ) : balances.map((b) => (
                    <div key={b.id} className="flex justify-between border-b py-2">
                      <span>{b.leaveTypeName}</span>
                      <span className="font-mono text-xs">{b.usedDays} used of {b.entitlement} · {b.remainingDays} left</span>
                    </div>
                  ))}
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          <TabsContent value="attendance">
            <DataTable columns={attendanceCols} data={attendance} emptyMessage="No attendance recorded" />
          </TabsContent>

          <TabsContent value="leave">
            <DataTable columns={leaveCols} data={leave} emptyMessage="No leave requests yet" />
          </TabsContent>

          <TabsContent value="payslips">
            <DataTable columns={payslipCols} data={payslips} emptyMessage="No payslips issued yet" />
          </TabsContent>
        </Tabs>
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader><DialogTitle>Request Leave</DialogTitle></DialogHeader>
          <form onSubmit={submitLeave} className="space-y-3">
            <div className="space-y-1">
              <Label>Leave Type</Label>
              <Select value={form.leaveTypeId} onValueChange={(v) => setForm({ ...form, leaveTypeId: v })}>
                <SelectTrigger><SelectValue placeholder="Select leave type" /></SelectTrigger>
                <SelectContent>
                  {leaveTypes.map((t) => <SelectItem key={t.id} value={String(t.id)}>{t.name}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Start Date</Label><Input type="date" required value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></div>
              <div className="space-y-1"><Label>End Date</Label><Input type="date" required value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} /></div>
            </div>
            <div className="space-y-1"><Label>Reason</Label><Textarea value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} /></div>
            <DialogFooter>
              <Button type="submit" disabled={!form.leaveTypeId || !form.startDate || !form.endDate}>Submit</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={!!payslip} onOpenChange={(v) => { if (!v) setPayslip(null); }}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>Payslip {payslip?.payslipNumber}</DialogTitle></DialogHeader>
          {payslip && (
            <div className="space-y-2 text-sm">
              <div className="flex justify-between border-b py-2"><span>Period</span><span className="font-mono">{payslip.periodCode}</span></div>
              <div className="flex justify-between border-b py-2"><span>Range</span><span>{formatDate(payslip.periodStartDate)} – {formatDate(payslip.periodEndDate)}</span></div>
              <p className="text-xs font-semibold text-slate-400 uppercase pt-2">Earnings</p>
              <div className="flex justify-between border-b py-2"><span>Basic salary</span><span className="font-mono">{formatCurrency(payslip.basicSalary)}</span></div>
              <div className="flex justify-between border-b py-2"><span>Allowances</span><span className="font-mono">{formatCurrency(payslip.allowances)}</span></div>
              <div className="flex justify-between border-b py-2"><span>Overtime</span><span className="font-mono">{formatCurrency(payslip.overtime)}</span></div>
              <div className="flex justify-between border-b py-2"><span>Bonuses</span><span className="font-mono">{formatCurrency(payslip.bonuses)}</span></div>
              <div className="flex justify-between border-b py-2"><span>Gross</span><span className="font-mono font-bold">{formatCurrency(payslip.grossSalary)}</span></div>
              <p className="text-xs font-semibold text-slate-400 uppercase pt-2">Deductions</p>
              <div className="flex justify-between border-b py-2"><span>Income tax</span><span className="font-mono">{formatCurrency(payslip.tax)}</span></div>
              <div className="flex justify-between border-b py-2"><span>Pension</span><span className="font-mono">{formatCurrency(payslip.pension)}</span></div>
              <div className="flex justify-between border-b py-2"><span>Other</span><span className="font-mono">{formatCurrency(payslip.otherDeductions)}</span></div>
              <div className="flex justify-between border-b py-2"><span>Total deductions</span><span className="font-mono font-bold">{formatCurrency(payslip.totalDeductions)}</span></div>
              <div className="flex justify-between py-3">
                <span className="font-semibold">Net pay</span>
                <span className="font-mono font-bold text-emerald-700">{formatCurrency(payslip.netSalary)}</span>
              </div>
              <Badge variant="outline">{payslip.paymentStatus}</Badge>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default HrMySelfService;
