import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { hrApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatCurrency } from '../../utils';
import { Users, UserCheck, CalendarOff, Clock, Banknote, Wallet, Building2 } from 'lucide-react';

const HrDashboard = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await hrApi.getDashboard();
      setData(res.data.data || {});
    } catch {
      setError('Failed to load HR dashboard.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const attendance = data?.attendanceToday || {};
  const byDepartment = data?.headcountByDepartment || {};
  const byStatus = data?.employeesByStatus || {};

  const stats = data ? [
    { title: 'Total Employees', value: data.totalEmployees || 0, icon: Users, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Active Headcount', value: data.activeHeadcount || 0, icon: UserCheck, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: 'On Leave', value: data.onLeave || 0, icon: CalendarOff, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: 'Attendance Recorded Today', value: attendance.recorded || 0, icon: Clock, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: 'Pending Leave Requests', value: data.pendingLeaveRequests || 0, icon: CalendarOff, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    { title: 'Current Payroll Net', value: formatCurrency(data.currentPayrollNet || 0), icon: Banknote, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: 'Salary Payable Outstanding', value: formatCurrency(data.salaryPayableOutstanding || 0), icon: Wallet, iconColor: 'text-amber-700', bgColor: 'bg-amber-50' },
    { title: 'Departments', value: Object.keys(byDepartment).length, icon: Building2, iconColor: 'text-slate-600', bgColor: 'bg-slate-50' },
  ] : [];

  const quickLinks = [
    ['Employees', '/hr/employees', PERMISSIONS.VIEW_HR],
    ['Attendance', '/hr/attendance', PERMISSIONS.VIEW_HR],
    ['Leave Requests', '/hr/leave', PERMISSIONS.VIEW_HR],
    ['Leave Types', '/hr/leave-types', PERMISSIONS.VIEW_HR],
    ['Payroll', '/hr/payroll', PERMISSIONS.HR_PAYROLL],
    ['Payslips', '/hr/payslips', PERMISSIONS.HR_PAYROLL],
    ['Departments', '/hr/departments', PERMISSIONS.VIEW_HR],
    ['Positions', '/hr/positions', PERMISSIONS.VIEW_HR],
    ['Reports', '/hr/reports', PERMISSIONS.HR_REPORT],
    ['My Self-Service', '/hr/me', PERMISSIONS.HR_SELF],
  ].filter(([, , permission]) => hasPermission(permission));

  return (
    <EnterprisePage
      title="Human Resources"
      subtitle="Headcount, attendance, leave, and payroll — live from the HR ledger"
      breadcrumbs={['HR']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      stats={stats}
      actions={
        <>
          <Button variant="outline" size="sm" onClick={() => navigate('/hr/employees')}>Employees</Button>
          {hasPermission(PERMISSIONS.HR_PAYROLL) && (
            <Button size="sm" onClick={() => navigate('/hr/payroll')}>Payroll</Button>
          )}
        </>
      }
    >
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Quick Links</CardTitle>
            <CardDescription>Navigate HR workflows</CardDescription>
          </CardHeader>
          <CardContent className="grid grid-cols-2 gap-3">
            {quickLinks.map(([label, path]) => (
              <Button key={path} variant="outline" className="justify-start" onClick={() => navigate(path)}>{label}</Button>
            ))}
          </CardContent>
        </Card>

        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Current Payroll Period</CardTitle>
            <CardDescription>Latest non-cancelled period and its run totals</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            {!data?.currentPayrollPeriod ? (
              <p className="text-sm text-slate-500 py-6 text-center">No payroll period opened yet.</p>
            ) : (
              <>
                <div className="flex justify-between border-b py-2">
                  <span>Period</span>
                  <span className="font-mono font-bold">{data.currentPayrollPeriod}</span>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span>Status</span>
                  <Badge variant="outline">{data.currentPayrollStatus}</Badge>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span>Employees in run</span>
                  <span className="font-mono">{data.currentPayrollEmployees ?? 0}</span>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span>Gross</span>
                  <span className="font-mono">{formatCurrency(data.currentPayrollGross || 0)}</span>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span>Net</span>
                  <span className="font-mono font-bold">{formatCurrency(data.currentPayrollNet || 0)}</span>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span>Salary payable outstanding</span>
                  <span className="font-mono text-amber-700">{formatCurrency(data.salaryPayableOutstanding || 0)}</span>
                </div>
              </>
            )}
          </CardContent>
        </Card>

        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Headcount by Department</CardTitle>
            <CardDescription>Active employee distribution</CardDescription>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            {Object.keys(byDepartment).length === 0 ? (
              <p className="text-sm text-slate-500 py-6 text-center">No employees assigned to departments.</p>
            ) : Object.entries(byDepartment).map(([dept, count]) => (
              <div key={dept} className="flex justify-between border-b py-2">
                <span>{dept}</span>
                <span className="font-mono font-semibold">{count}</span>
              </div>
            ))}
          </CardContent>
        </Card>

        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Attendance Today</CardTitle>
            <CardDescription>Records captured for the current date</CardDescription>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            {Object.entries(attendance).filter(([key]) => key !== 'recorded').map(([status, count]) => (
              <div key={status} className="flex justify-between border-b py-2">
                <span>{status.replace(/_/g, ' ')}</span>
                <span className="font-mono">{count}</span>
              </div>
            ))}
            <div className="flex justify-between pt-2">
              <span className="font-semibold">Employment statuses</span>
              <span className="text-xs text-slate-400">count</span>
            </div>
            {Object.entries(byStatus).map(([status, count]) => (
              <div key={status} className="flex justify-between border-b py-2">
                <span>{status.replace(/_/g, ' ')}</span>
                <span className="font-mono">{count}</span>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    </EnterprisePage>
  );
};

export default HrDashboard;
