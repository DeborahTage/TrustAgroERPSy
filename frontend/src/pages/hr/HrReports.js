import React, { useCallback, useEffect, useState } from 'react';
import { hrApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Badge } from '../../components/ui/badge';
import { formatCurrency, formatDate } from '../../utils';
import { Users, Banknote, Wallet } from 'lucide-react';

const HrReports = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await hrApi.getReports();
      setData(res.data.data || {});
    } catch {
      setError('Failed to load HR reports.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const headcount = data?.headcountByDepartment || {};
  const byType = data?.employeesByEmploymentType || {};
  const leaveByStatus = data?.leaveRequestsByStatus || {};
  const payrollByPeriod = data?.payrollByPeriod || [];
  const totalHeadcount = Object.values(headcount).reduce((sum, n) => sum + Number(n || 0), 0);

  const stats = data ? [
    { title: 'Headcount', value: totalHeadcount, icon: Users, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Payroll Cost (Finance)', value: formatCurrency(data.payrollCostFromFinance || 0), icon: Banknote, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: 'Salary Payable Outstanding', value: formatCurrency(data.salaryPayableOutstanding || 0), icon: Wallet, iconColor: 'text-amber-700', bgColor: 'bg-amber-50' },
    { title: 'Payroll Periods', value: payrollByPeriod.length, icon: Banknote, iconColor: 'text-slate-600', bgColor: 'bg-slate-50' },
  ] : [];

  const periodCols = [
    { accessorKey: 'periodCode', header: 'Period', cell: ({ row }) => <span className="font-mono font-bold">{row.original.periodCode}</span> },
    {
      id: 'range',
      header: 'Range',
      cell: ({ row }) => `${formatDate(row.original.startDate)} – ${formatDate(row.original.endDate)}`,
    },
    { accessorKey: 'employeeCount', header: 'Employees', cell: ({ row }) => <span className="font-mono">{row.original.employeeCount}</span> },
    { accessorKey: 'gross', header: 'Gross', cell: ({ row }) => <span className="font-mono">{formatCurrency(row.original.gross)}</span> },
    { accessorKey: 'deductions', header: 'Deductions', cell: ({ row }) => <span className="font-mono">{formatCurrency(row.original.deductions)}</span> },
    { accessorKey: 'net', header: 'Net', cell: ({ row }) => <span className="font-mono font-bold">{formatCurrency(row.original.net)}</span> },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge>,
    },
  ];

  return (
    <EnterprisePage
      title="HR Reports"
      subtitle="Headcount, leave, and payroll aggregates from the HR and Finance ledgers"
      breadcrumbs={['HR', 'Reports']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      stats={stats}
    >
      <div className="space-y-6">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <Card className="border-none shadow-sm">
            <CardHeader>
              <CardTitle className="text-base">Headcount by Department</CardTitle>
              <CardDescription>Employees per organisational unit</CardDescription>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              {Object.keys(headcount).length === 0 ? (
                <p className="text-slate-500 text-center py-4">No data.</p>
              ) : Object.entries(headcount).map(([dept, count]) => (
                <div key={dept} className="flex justify-between border-b py-2">
                  <span>{dept}</span>
                  <span className="font-mono font-semibold">{count}</span>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader>
              <CardTitle className="text-base">Employment Type Mix</CardTitle>
              <CardDescription>Contract structure across the workforce</CardDescription>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              {Object.entries(byType).map(([type, count]) => (
                <div key={type} className="flex justify-between border-b py-2">
                  <span>{type.replace(/_/g, ' ')}</span>
                  <span className="font-mono">{count}</span>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader>
              <CardTitle className="text-base">Leave Requests by Status</CardTitle>
              <CardDescription>All-time request outcomes</CardDescription>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              {Object.entries(leaveByStatus).map(([status, count]) => (
                <div key={status} className="flex justify-between border-b py-2">
                  <span>{status}</span>
                  <span className="font-mono">{count}</span>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>

        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-base">Payroll by Period</CardTitle>
            <CardDescription>Totals from the current run of each period</CardDescription>
          </CardHeader>
          <CardContent>
            <DataTable columns={periodCols} data={payrollByPeriod} searchable={false} emptyMessage="No payroll periods yet" />
          </CardContent>
        </Card>

        {data?.note && (
          <Card className="border-none shadow-sm">
            <CardContent className="pt-6">
              <p className="text-xs text-slate-500">{data.note}</p>
            </CardContent>
          </Card>
        )}
      </div>
    </EnterprisePage>
  );
};

export default HrReports;
