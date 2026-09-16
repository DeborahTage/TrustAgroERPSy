import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { hrApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
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
import { toast } from 'react-toastify';

const EMPLOYMENT_TYPES = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'TEMPORARY', 'INTERN'];
const EXIT_STATUSES = ['TERMINATED', 'RESIGNED'];

const HrEmployeeDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [employee, setEmployee] = useState(null);
  const [departments, setDepartments] = useState([]);
  const [positions, setPositions] = useState([]);
  const [attendance, setAttendance] = useState([]);
  const [leaveRequests, setLeaveRequests] = useState([]);
  const [leaveBalances, setLeaveBalances] = useState([]);
  const [salaryHistory, setSalaryHistory] = useState([]);
  const [payrollItems, setPayrollItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [editOpen, setEditOpen] = useState(false);
  const [salaryOpen, setSalaryOpen] = useState(false);
  const [terminateOpen, setTerminateOpen] = useState(false);
  const [editForm, setEditForm] = useState(null);
  const [salaryForm, setSalaryForm] = useState({ basicSalary: '', currency: 'ETB', effectiveDate: '', reason: '' });
  const [exitForm, setExitForm] = useState({ status: 'TERMINATED', effectiveDate: '', reason: '' });

  const canPayroll = hasPermission(PERMISSIONS.HR_PAYROLL);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const empRes = await hrApi.getEmployee(id);
      const emp = empRes.data.data;
      setEmployee(emp);

      const [deptRes, posRes, attRes, leaveRes, balanceRes] = await Promise.all([
        hrApi.getDepartments({ activeOnly: true }).catch(() => ({ data: { data: [] } })),
        hrApi.getPositions({ activeOnly: true }).catch(() => ({ data: { data: [] } })),
        hrApi.getAttendance({ employeeId: id }).catch(() => ({ data: { data: [] } })),
        hrApi.getLeaveRequests({ employeeId: id }).catch(() => ({ data: { data: [] } })),
        hrApi.getEmployeeLeaveBalances(id).catch(() => ({ data: { data: [] } })),
      ]);
      setDepartments(deptRes.data?.data || []);
      setPositions(posRes.data?.data || []);
      setAttendance(attRes.data?.data || []);
      setLeaveRequests(leaveRes.data?.data || []);
      setLeaveBalances(balanceRes.data?.data || []);

      if (canPayroll) {
        const histRes = await hrApi.getSalaryHistory(id).catch(() => ({ data: { data: [] } }));
        setSalaryHistory(histRes.data?.data || []);

        const runsRes = await hrApi.getPayrollRuns().catch(() => ({ data: { data: [] } }));
        const runs = (runsRes.data?.data || []).slice(0, 3);
        const itemsByRun = await Promise.all(
          runs.map((run) => hrApi.getPayrollRunItems(run.id).catch(() => ({ data: { data: [] } }))),
        );
        setPayrollItems(
          itemsByRun
            .flatMap((res) => res.data?.data || [])
            .filter((item) => String(item.employeeId) === String(id)),
        );
      } else {
        setSalaryHistory([]);
        setPayrollItems([]);
      }
    } catch {
      setError('Failed to load employee.');
    } finally {
      setLoading(false);
    }
  }, [id, canPayroll]);

  useEffect(() => { load(); }, [load]);

  const act = async (fn, msg) => {
    try {
      await fn();
      toast.success(msg);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    }
  };

  const openEdit = () => {
    setEditForm({
      firstName: employee.firstName || '',
      middleName: employee.middleName || '',
      lastName: employee.lastName || '',
      phone: employee.phone || '',
      email: employee.email || '',
      address: employee.address || '',
      hireDate: employee.hireDate || '',
      employmentType: employee.employmentType || 'FULL_TIME',
      departmentId: employee.departmentId ? String(employee.departmentId) : '',
      positionId: employee.positionId ? String(employee.positionId) : '',
      bankName: employee.bankName || '',
      bankAccountRef: employee.bankAccountRef || '',
      emergencyContact: employee.emergencyContact || '',
      notes: employee.notes || '',
    });
    setEditOpen(true);
  };

  const submitEdit = async (e) => {
    e.preventDefault();
    await act(() => hrApi.updateEmployee(id, {
      firstName: editForm.firstName,
      middleName: editForm.middleName || undefined,
      lastName: editForm.lastName,
      phone: editForm.phone || undefined,
      email: editForm.email || undefined,
      address: editForm.address || undefined,
      hireDate: editForm.hireDate || undefined,
      employmentType: editForm.employmentType,
      departmentId: editForm.departmentId ? Number(editForm.departmentId) : undefined,
      positionId: editForm.positionId ? Number(editForm.positionId) : undefined,
      bankName: editForm.bankName || undefined,
      bankAccountRef: editForm.bankAccountRef || undefined,
      emergencyContact: editForm.emergencyContact || undefined,
      notes: editForm.notes || undefined,
    }), 'Employee updated');
    setEditOpen(false);
  };

  const submitSalary = async (e) => {
    e.preventDefault();
    await act(() => hrApi.changeSalary(id, {
      basicSalary: parseFloat(salaryForm.basicSalary),
      currency: salaryForm.currency || undefined,
      effectiveDate: salaryForm.effectiveDate || undefined,
      reason: salaryForm.reason || undefined,
    }), 'Salary updated');
    setSalaryOpen(false);
  };

  const submitExit = async (e) => {
    e.preventDefault();
    await act(() => hrApi.terminateEmployee(id, {
      status: exitForm.status,
      effectiveDate: exitForm.effectiveDate || undefined,
      reason: exitForm.reason || undefined,
    }), 'Employment ended');
    setTerminateOpen(false);
  };

  const recentAttendance = attendance.slice(0, 10);
  const isExited = employee && ['TERMINATED', 'RESIGNED'].includes(employee.employmentStatus);

  return (
    <EnterprisePage
      title={employee?.fullName || 'Employee'}
      subtitle={employee ? `${employee.employeeNumber} · ${employee.positionName || 'No position'}` : ''}
      breadcrumbs={['HR', 'Employees', employee?.employeeNumber]}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={
        <div className="flex flex-wrap gap-2">
          <Button size="sm" variant="outline" onClick={() => navigate('/hr/employees')}>Back to Directory</Button>
          {hasPermission(PERMISSIONS.EDIT_HR) && employee && (
            <Button size="sm" variant="outline" onClick={openEdit}>Edit</Button>
          )}
          {canPayroll && employee && (
            <Button size="sm" variant="outline" onClick={() => {
              setSalaryForm({
                basicSalary: String(employee.basicSalary ?? ''),
                currency: employee.currency || 'ETB',
                effectiveDate: '',
                reason: '',
              });
              setSalaryOpen(true);
            }}>Change Salary</Button>
          )}
          {hasPermission(PERMISSIONS.TERMINATE_HR) && employee && !isExited && (
            <Button size="sm" variant="destructive" onClick={() => {
              setExitForm({ status: 'TERMINATED', effectiveDate: '', reason: '' });
              setTerminateOpen(true);
            }}>Terminate</Button>
          )}
        </div>
      }
    >
      {employee && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <Card className="border-none shadow-sm">
              <CardHeader><CardTitle className="text-base">Employment</CardTitle></CardHeader>
              <CardContent className="space-y-2 text-sm">
                <div className="flex justify-between"><span>Status</span><Badge variant="outline">{employee.employmentStatus}</Badge></div>
                <div className="flex justify-between"><span>Employee #</span><span className="font-mono font-bold">{employee.employeeNumber}</span></div>
                <div className="flex justify-between"><span>Type</span><span>{(employee.employmentType || '—').replace(/_/g, ' ')}</span></div>
                <div className="flex justify-between"><span>Hired</span><span>{formatDate(employee.hireDate)}</span></div>
                {isExited && (
                  <>
                    <div className="flex justify-between"><span>Exit date</span><span>{formatDate(employee.terminationDate)}</span></div>
                    <div className="flex justify-between"><span>Reason</span><span>{employee.terminationReason || '—'}</span></div>
                  </>
                )}
                <div className="flex justify-between"><span>ERP login</span><span>{employee.erpAccessEnabled ? 'Linked' : 'Not linked'}</span></div>
              </CardContent>
            </Card>

            <Card className="border-none shadow-sm">
              <CardHeader><CardTitle className="text-base">Organisation & Contact</CardTitle></CardHeader>
              <CardContent className="space-y-2 text-sm">
                <div className="flex justify-between"><span>Department</span><span>{employee.departmentName || '—'}</span></div>
                <div className="flex justify-between"><span>Position</span><span>{employee.positionName || '—'}</span></div>
                <div className="flex justify-between"><span>Manager</span><span>{employee.managerName || '—'}</span></div>
                <div className="flex justify-between"><span>Phone</span><span>{employee.phone || '—'}</span></div>
                <div className="flex justify-between"><span>Email</span><span>{employee.email || '—'}</span></div>
                <div className="flex justify-between"><span>Address</span><span>{employee.address || '—'}</span></div>
                <div className="flex justify-between"><span>Emergency</span><span>{employee.emergencyContact || '—'}</span></div>
              </CardContent>
            </Card>

            <Card className="border-none shadow-sm">
              <CardHeader>
                <CardTitle className="text-base">Compensation</CardTitle>
                {!canPayroll && <CardDescription>Requires payroll clearance</CardDescription>}
              </CardHeader>
              <CardContent className="space-y-2 text-sm">
                {!canPayroll ? (
                  <p className="text-slate-500 py-4 text-center">Compensation is hidden for your role.</p>
                ) : (
                  <>
                    <div className="flex justify-between"><span>Basic salary</span><span className="font-mono font-bold">{employee.basicSalary != null ? formatCurrency(employee.basicSalary) : '—'}</span></div>
                    <div className="flex justify-between"><span>Currency</span><span>{employee.currency || 'ETB'}</span></div>
                    <div className="flex justify-between"><span>Bank</span><span>{employee.bankName || '—'}</span></div>
                    <div className="flex justify-between"><span>Account ref</span><span className="font-mono">{employee.bankAccountRef || '—'}</span></div>
                  </>
                )}
              </CardContent>
            </Card>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card className="border-none shadow-sm">
              <CardHeader>
                <CardTitle className="text-base">Recent Attendance</CardTitle>
                <CardDescription>Last 10 recorded days</CardDescription>
              </CardHeader>
              <CardContent className="space-y-2 text-sm">
                {recentAttendance.length === 0 ? (
                  <p className="text-slate-500 text-center py-4">No attendance recorded.</p>
                ) : recentAttendance.map((a) => (
                  <div key={a.id} className="flex justify-between border-b py-2">
                    <span>{formatDate(a.attendanceDate)}</span>
                    <span className="text-xs text-slate-500">{a.checkIn || '—'} – {a.checkOut || '—'}</span>
                    <span className="font-mono text-xs">{a.hoursWorked != null ? `${a.hoursWorked} h` : '—'}</span>
                    <Badge variant="outline">{a.status}</Badge>
                  </div>
                ))}
              </CardContent>
            </Card>

            <Card className="border-none shadow-sm">
              <CardHeader>
                <CardTitle className="text-base">Leave</CardTitle>
                <CardDescription>Balances and request history</CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                {leaveBalances.length > 0 && (
                  <div className="space-y-1">
                    <p className="text-xs font-semibold text-slate-400 uppercase">Balances</p>
                    {leaveBalances.map((b) => (
                      <div key={b.id} className="flex justify-between border-b py-1.5">
                        <span>{b.leaveTypeName}</span>
                        <span className="font-mono text-xs">{b.usedDays} / {b.entitlement} used · {b.remainingDays} left</span>
                      </div>
                    ))}
                  </div>
                )}
                <div className="space-y-1">
                  <p className="text-xs font-semibold text-slate-400 uppercase">Requests</p>
                  {leaveRequests.length === 0 ? (
                    <p className="text-slate-500 text-center py-4">No leave requests.</p>
                  ) : leaveRequests.slice(0, 10).map((r) => (
                    <div key={r.id} className="flex justify-between border-b py-1.5">
                      <span className="font-mono text-xs">{r.requestNumber}</span>
                      <span className="text-xs">{formatDate(r.startDate)} – {formatDate(r.endDate)}</span>
                      <span className="text-xs">{r.totalDays} d</span>
                      <Badge variant="outline">{r.status}</Badge>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>

          {canPayroll && (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <Card className="border-none shadow-sm">
                <CardHeader>
                  <CardTitle className="text-base">Payroll & Payslips</CardTitle>
                  <CardDescription>Items from the most recent payroll runs</CardDescription>
                </CardHeader>
                <CardContent className="space-y-2 text-sm">
                  {payrollItems.length === 0 ? (
                    <p className="text-slate-500 text-center py-4">No payroll items for this employee yet.</p>
                  ) : payrollItems.map((item) => (
                    <div key={item.id} className="border rounded-lg p-3 space-y-1">
                      <div className="flex justify-between">
                        <span className="font-mono font-semibold">{item.periodCode}</span>
                        <Badge variant="outline">{item.paymentStatus}</Badge>
                      </div>
                      <div className="flex justify-between text-xs text-slate-500">
                        <span>Gross {formatCurrency(item.grossSalary)}</span>
                        <span>Deductions {formatCurrency(item.totalDeductions)}</span>
                        <span className="font-semibold text-slate-700">Net {formatCurrency(item.netSalary)}</span>
                      </div>
                      {item.payslipId && (
                        <Button size="sm" variant="outline" onClick={() => navigate(`/hr/payslips?payslipId=${item.payslipId}`)}>
                          Payslip {item.payslipNumber}
                        </Button>
                      )}
                    </div>
                  ))}
                </CardContent>
              </Card>

              <Card className="border-none shadow-sm">
                <CardHeader>
                  <CardTitle className="text-base">Salary History</CardTitle>
                  <CardDescription>Every approved compensation change</CardDescription>
                </CardHeader>
                <CardContent className="space-y-2 text-sm">
                  {salaryHistory.length === 0 ? (
                    <p className="text-slate-500 text-center py-4">No salary changes recorded.</p>
                  ) : salaryHistory.map((h) => (
                    <div key={h.id} className="flex justify-between border-b py-2">
                      <span>{formatDate(h.effectiveDate)}</span>
                      <span className="font-mono font-semibold">{formatCurrency(h.basicSalary)}</span>
                      <span className="text-xs text-slate-500">{h.reason || '—'}</span>
                    </div>
                  ))}
                </CardContent>
              </Card>
            </div>
          )}
        </div>
      )}

      <Dialog open={editOpen} onOpenChange={setEditOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>Edit Employee</DialogTitle></DialogHeader>
          {editForm && (
            <form onSubmit={submitEdit} className="space-y-3">
              <div className="grid grid-cols-3 gap-3">
                <div className="space-y-1"><Label>First Name</Label><Input required value={editForm.firstName} onChange={(e) => setEditForm({ ...editForm, firstName: e.target.value })} /></div>
                <div className="space-y-1"><Label>Middle Name</Label><Input value={editForm.middleName} onChange={(e) => setEditForm({ ...editForm, middleName: e.target.value })} /></div>
                <div className="space-y-1"><Label>Last Name</Label><Input required value={editForm.lastName} onChange={(e) => setEditForm({ ...editForm, lastName: e.target.value })} /></div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1"><Label>Phone</Label><Input value={editForm.phone} onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })} /></div>
                <div className="space-y-1"><Label>Email</Label><Input type="email" value={editForm.email} onChange={(e) => setEditForm({ ...editForm, email: e.target.value })} /></div>
              </div>
              <div className="space-y-1"><Label>Address</Label><Input value={editForm.address} onChange={(e) => setEditForm({ ...editForm, address: e.target.value })} /></div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1"><Label>Hire Date</Label><Input type="date" value={editForm.hireDate} onChange={(e) => setEditForm({ ...editForm, hireDate: e.target.value })} /></div>
                <div className="space-y-1">
                  <Label>Employment Type</Label>
                  <Select value={editForm.employmentType} onValueChange={(v) => setEditForm({ ...editForm, employmentType: v })}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>{EMPLOYMENT_TYPES.map((t) => <SelectItem key={t} value={t}>{t.replace(/_/g, ' ')}</SelectItem>)}</SelectContent>
                  </Select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1">
                  <Label>Department</Label>
                  <Select value={editForm.departmentId || 'none'} onValueChange={(v) => setEditForm({ ...editForm, departmentId: v === 'none' ? '' : v })}>
                    <SelectTrigger><SelectValue placeholder="Optional" /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="none">None</SelectItem>
                      {departments.map((d) => <SelectItem key={d.id} value={String(d.id)}>{d.name}</SelectItem>)}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-1">
                  <Label>Position</Label>
                  <Select value={editForm.positionId || 'none'} onValueChange={(v) => setEditForm({ ...editForm, positionId: v === 'none' ? '' : v })}>
                    <SelectTrigger><SelectValue placeholder="Optional" /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="none">None</SelectItem>
                      {positions.map((p) => <SelectItem key={p.id} value={String(p.id)}>{p.name}</SelectItem>)}
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1"><Label>Bank Name</Label><Input value={editForm.bankName} onChange={(e) => setEditForm({ ...editForm, bankName: e.target.value })} /></div>
                <div className="space-y-1"><Label>Bank Account Ref</Label><Input value={editForm.bankAccountRef} onChange={(e) => setEditForm({ ...editForm, bankAccountRef: e.target.value })} /></div>
              </div>
              <div className="space-y-1"><Label>Emergency Contact</Label><Input value={editForm.emergencyContact} onChange={(e) => setEditForm({ ...editForm, emergencyContact: e.target.value })} /></div>
              <div className="space-y-1"><Label>Notes</Label><Textarea value={editForm.notes} onChange={(e) => setEditForm({ ...editForm, notes: e.target.value })} /></div>
              <DialogFooter><Button type="submit">Save</Button></DialogFooter>
            </form>
          )}
        </DialogContent>
      </Dialog>

      <Dialog open={salaryOpen} onOpenChange={setSalaryOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Change Salary</DialogTitle></DialogHeader>
          <form onSubmit={submitSalary} className="space-y-3">
            <div className="space-y-1"><Label>Basic Salary</Label><Input type="number" step="0.01" required value={salaryForm.basicSalary} onChange={(e) => setSalaryForm({ ...salaryForm, basicSalary: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Currency</Label><Input value={salaryForm.currency} onChange={(e) => setSalaryForm({ ...salaryForm, currency: e.target.value })} /></div>
              <div className="space-y-1"><Label>Effective Date</Label><Input type="date" value={salaryForm.effectiveDate} onChange={(e) => setSalaryForm({ ...salaryForm, effectiveDate: e.target.value })} /></div>
            </div>
            <div className="space-y-1"><Label>Reason</Label><Textarea value={salaryForm.reason} onChange={(e) => setSalaryForm({ ...salaryForm, reason: e.target.value })} /></div>
            <DialogFooter><Button type="submit" disabled={!salaryForm.basicSalary}>Save Change</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={terminateOpen} onOpenChange={setTerminateOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>End Employment</DialogTitle></DialogHeader>
          <form onSubmit={submitExit} className="space-y-3">
            <p className="text-sm text-slate-500">
              This closes the employment record for {employee?.fullName}. The employee stops being picked up by payroll runs.
            </p>
            <div className="space-y-1">
              <Label>Status</Label>
              <Select value={exitForm.status} onValueChange={(v) => setExitForm({ ...exitForm, status: v })}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>{EXIT_STATUSES.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}</SelectContent>
              </Select>
            </div>
            <div className="space-y-1"><Label>Effective Date</Label><Input type="date" value={exitForm.effectiveDate} onChange={(e) => setExitForm({ ...exitForm, effectiveDate: e.target.value })} /></div>
            <div className="space-y-1"><Label>Reason</Label><Textarea required value={exitForm.reason} onChange={(e) => setExitForm({ ...exitForm, reason: e.target.value })} /></div>
            <DialogFooter><Button type="submit" variant="destructive" disabled={!exitForm.reason}>Confirm</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default HrEmployeeDetail;
