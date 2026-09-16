import React, { useCallback, useEffect, useState } from 'react';
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

const PERIOD_STATUSES = ['OPEN', 'CALCULATED', 'APPROVED', 'PROCESSED', 'CLOSED', 'CANCELLED'];

const emptyForm = { periodCode: '', startDate: '', endDate: '', notes: '' };

const HrPayroll = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [deductionTypes, setDeductionTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState('all');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);

  const canRun = hasPermission(PERMISSIONS.HR_PAYROLL);
  const canApprove = hasPermission(PERMISSIONS.HR_PAYROLL_APPROVE);
  const canProcess = hasPermission(PERMISSIONS.HR_PAYROLL_PROCESS);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [periodRes, dedRes] = await Promise.all([
        hrApi.getPayrollPeriods({ status: statusFilter !== 'all' ? statusFilter : undefined }),
        hrApi.getDeductionTypes({ activeOnly: true }).catch(() => ({ data: { data: [] } })),
      ]);
      setRows(periodRes.data.data || []);
      setDeductionTypes(dedRes.data?.data || []);
    } catch {
      setError('Failed to load payroll periods.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

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

  const create = async (e) => {
    e.preventDefault();
    await act(() => hrApi.createPayrollPeriod({
      periodCode: form.periodCode || undefined,
      startDate: form.startDate,
      endDate: form.endDate,
      notes: form.notes || undefined,
    }), 'Payroll period created');
    setOpen(false);
    setForm(emptyForm);
  };

  const cols = [
    { accessorKey: 'periodCode', header: 'Period', cell: ({ row }) => <span className="font-mono font-bold">{row.original.periodCode}</span> },
    {
      id: 'range',
      header: 'Range',
      cell: ({ row }) => `${formatDate(row.original.startDate)} – ${formatDate(row.original.endDate)}`,
    },
    { accessorKey: 'employeeCount', header: 'Employees', cell: ({ row }) => <span className="font-mono">{row.original.employeeCount}</span> },
    { accessorKey: 'grossAmount', header: 'Gross', cell: ({ row }) => <span className="font-mono">{formatCurrency(row.original.grossAmount)}</span> },
    { accessorKey: 'deductionAmount', header: 'Deductions', cell: ({ row }) => <span className="font-mono">{formatCurrency(row.original.deductionAmount)}</span> },
    { accessorKey: 'netAmount', header: 'Net', cell: ({ row }) => <span className="font-mono font-bold">{formatCurrency(row.original.netAmount)}</span> },
    { accessorKey: 'paidAmount', header: 'Paid', cell: ({ row }) => <span className="font-mono text-emerald-700">{formatCurrency(row.original.paidAmount)}</span> },
    { accessorKey: 'outstandingAmount', header: 'Outstanding', cell: ({ row }) => <span className="font-mono text-amber-700">{formatCurrency(row.original.outstandingAmount)}</span> },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge>,
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => {
        const p = row.original;
        return (
          <div className="flex flex-wrap gap-1 justify-end">
            {p.currentRunId && canRun && (
              <Button size="sm" variant="outline" onClick={() => navigate(`/hr/payroll/runs/${p.currentRunId}`)}>Run</Button>
            )}
            {canRun && ['OPEN', 'CALCULATED'].includes(p.status) && (
              <Button size="sm" onClick={() => act(() => hrApi.calculatePayroll(p.id), 'Payroll calculated')}>
                {p.status === 'CALCULATED' ? 'Recalculate' : 'Calculate'}
              </Button>
            )}
            {canApprove && p.status === 'CALCULATED' && (
              <Button size="sm" onClick={() => {
                if (!window.confirm(`Approve payroll for ${p.periodCode}? Amounts freeze after approval.`)) return;
                act(() => hrApi.approvePayroll(p.id), 'Payroll approved');
              }}>Approve</Button>
            )}
            {canProcess && p.status === 'APPROVED' && (
              <Button size="sm" onClick={() => {
                if (!window.confirm(`Process ${p.periodCode}? This posts salary expense and payable to Finance.`)) return;
                act(() => hrApi.processPayroll(p.id), 'Payroll processed');
              }}>Process</Button>
            )}
            {canRun && p.status === 'PROCESSED' && (
              <Button size="sm" variant="outline" onClick={() => {
                if (!window.confirm(`Close ${p.periodCode}? The period becomes read-only.`)) return;
                act(() => hrApi.closePayroll(p.id), 'Payroll closed');
              }}>Close</Button>
            )}
            {canRun && ['OPEN', 'CALCULATED'].includes(p.status) && (
              <Button size="sm" variant="destructive" onClick={() => {
                if (!window.confirm(`Cancel period ${p.periodCode}? All runs and items are cancelled.`)) return;
                act(() => hrApi.cancelPayrollPeriod(p.id), 'Period cancelled');
              }}>Cancel</Button>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <EnterprisePage
      title="Payroll"
      subtitle="Periods move OPEN → CALCULATED → APPROVED → PROCESSED → CLOSED"
      breadcrumbs={['HR', 'Payroll']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={canRun && (
        <Button size="sm" onClick={() => { setForm(emptyForm); setOpen(true); }}>
          <Plus className="h-4 w-4 mr-1" /> New Period
        </Button>
      )}
    >
      <div className="flex flex-wrap gap-3 mb-4">
        <Select value={statusFilter} onValueChange={setStatusFilter}>
          <SelectTrigger className="w-48"><SelectValue placeholder="Status" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All statuses</SelectItem>
            {PERIOD_STATUSES.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
          </SelectContent>
        </Select>
        {deductionTypes.length > 0 && (
          <p className="text-xs text-slate-500 self-center">
            {deductionTypes.length} active deduction type(s) applied at calculation: {deductionTypes.map((d) => d.name).join(', ')}
          </p>
        )}
      </div>

      <DataTable columns={cols} data={rows} emptyMessage="No payroll periods yet" />

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader><DialogTitle>New Payroll Period</DialogTitle></DialogHeader>
          <form onSubmit={create} className="space-y-3">
            <div className="space-y-1">
              <Label>Period Code</Label>
              <Input value={form.periodCode} onChange={(e) => setForm({ ...form, periodCode: e.target.value })} placeholder="Auto-generated when blank" />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Start Date</Label><Input type="date" required value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></div>
              <div className="space-y-1"><Label>End Date</Label><Input type="date" required value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} /></div>
            </div>
            <div className="space-y-1"><Label>Notes</Label><Textarea value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></div>
            <DialogFooter><Button type="submit" disabled={!form.startDate || !form.endDate}>Create</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default HrPayroll;
