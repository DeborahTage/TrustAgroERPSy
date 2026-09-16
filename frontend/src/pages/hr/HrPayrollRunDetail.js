import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
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
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatCurrency, formatDate } from '../../utils';
import { toast } from 'react-toastify';

const PAYMENT_METHODS = ['CASH', 'BANK_TRANSFER', 'MOBILE_MONEY', 'CREDIT'];

const HrPayrollRunDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [run, setRun] = useState(null);
  const [items, setItems] = useState([]);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [payOpen, setPayOpen] = useState(false);
  const [payForm, setPayForm] = useState({
    payrollItemId: '', employeeName: '', amount: '', paymentMethod: 'BANK_TRANSFER', paymentDate: '', notes: '',
  });

  const canPay = hasPermission(PERMISSIONS.HR_PAYROLL_PROCESS);
  const canSeeItems = hasPermission(PERMISSIONS.HR_PAYROLL);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const runRes = await hrApi.getPayrollRun(id);
      setRun(runRes.data.data);

      const [itemsRes, payRes] = await Promise.all([
        canSeeItems ? hrApi.getPayrollRunItems(id).catch(() => ({ data: { data: [] } })) : Promise.resolve({ data: { data: [] } }),
        hrApi.getPayrollRunPayments(id).catch(() => ({ data: { data: [] } })),
      ]);
      setItems(itemsRes.data?.data || []);
      setPayments(payRes.data?.data || []);
    } catch {
      setError('Failed to load payroll run.');
    } finally {
      setLoading(false);
    }
  }, [id, canSeeItems]);

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

  const submitPay = async (e) => {
    e.preventDefault();
    await act(() => hrApi.payPayroll(id, {
      payrollItemId: payForm.payrollItemId ? Number(payForm.payrollItemId) : undefined,
      amount: payForm.amount ? parseFloat(payForm.amount) : undefined,
      paymentMethod: payForm.paymentMethod,
      paymentDate: payForm.paymentDate || undefined,
      notes: payForm.notes || undefined,
    }), 'Payment recorded');
    setPayOpen(false);
  };

  const openPay = (item) => {
    setPayForm({
      payrollItemId: item ? String(item.id) : '',
      employeeName: item ? item.employeeName : '',
      amount: item ? String(item.outstandingAmount ?? '') : String(run?.outstandingAmount ?? ''),
      paymentMethod: 'BANK_TRANSFER',
      paymentDate: '',
      notes: '',
    });
    setPayOpen(true);
  };

  const isPayable = run?.status === 'PROCESSED';

  const stats = run ? [
    { title: 'Employees', value: run.employeeCount || 0 },
    { title: 'Gross', value: formatCurrency(run.grossAmount) },
    { title: 'Deductions', value: formatCurrency(run.deductionAmount) },
    { title: 'Net', value: formatCurrency(run.netAmount) },
    { title: 'Paid', value: formatCurrency(run.paidAmount) },
    { title: 'Outstanding', value: formatCurrency(run.outstandingAmount) },
  ] : [];

  const cols = [
    { accessorKey: 'employeeNumber', header: 'Employee #', cell: ({ row }) => <span className="font-mono">{row.original.employeeNumber}</span> },
    { accessorKey: 'employeeName', header: 'Employee' },
    { accessorKey: 'departmentName', header: 'Department', cell: ({ row }) => row.original.departmentName || '—' },
    { accessorKey: 'basicSalary', header: 'Basic', cell: ({ row }) => <span className="font-mono">{formatCurrency(row.original.basicSalary)}</span> },
    { accessorKey: 'grossSalary', header: 'Gross', cell: ({ row }) => <span className="font-mono">{formatCurrency(row.original.grossSalary)}</span> },
    { accessorKey: 'tax', header: 'Tax', cell: ({ row }) => <span className="font-mono">{formatCurrency(row.original.tax)}</span> },
    { accessorKey: 'pension', header: 'Pension', cell: ({ row }) => <span className="font-mono">{formatCurrency(row.original.pension)}</span> },
    { accessorKey: 'totalDeductions', header: 'Deductions', cell: ({ row }) => <span className="font-mono">{formatCurrency(row.original.totalDeductions)}</span> },
    { accessorKey: 'netSalary', header: 'Net', cell: ({ row }) => <span className="font-mono font-bold">{formatCurrency(row.original.netSalary)}</span> },
    { accessorKey: 'outstandingAmount', header: 'Outstanding', cell: ({ row }) => <span className="font-mono text-amber-700">{formatCurrency(row.original.outstandingAmount)}</span> },
    {
      accessorKey: 'paymentStatus',
      header: 'Payment',
      cell: ({ row }) => <Badge variant="outline">{row.original.paymentStatus}</Badge>,
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => {
        const item = row.original;
        return (
          <div className="flex flex-wrap gap-1 justify-end">
            {canPay && isPayable && item.paymentStatus !== 'PAID' && (
              <Button size="sm" onClick={() => openPay(item)}>Pay</Button>
            )}
            {item.payslipId && (
              <Button size="sm" variant="outline" onClick={() => navigate(`/hr/payslips?payslipId=${item.payslipId}`)}>Payslip</Button>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <EnterprisePage
      title={run ? `Payroll Run ${run.periodCode} #${run.runNumber}` : 'Payroll Run'}
      subtitle={run ? `${formatDate(run.periodStartDate)} – ${formatDate(run.periodEndDate)} · ${run.status}` : ''}
      breadcrumbs={['HR', 'Payroll', run?.periodCode]}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={
        <div className="flex flex-wrap gap-2">
          <Button size="sm" variant="outline" onClick={() => navigate('/hr/payroll')}>Back to Periods</Button>
          {canPay && isPayable && Number(run?.outstandingAmount || 0) > 0 && (
            <Button size="sm" onClick={() => openPay(null)}>Disburse Run</Button>
          )}
        </div>
      }
    >
      {run && (
        <div className="space-y-6">
          <Card className="border-none shadow-sm">
            <CardHeader>
              <CardTitle className="text-base">Run Totals</CardTitle>
              <CardDescription>Created by {run.createdByName || '—'} · Approved by {run.approvedByName || '—'} · Processed by {run.processedByName || '—'}</CardDescription>
            </CardHeader>
            <CardContent className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-4 text-sm">
              {stats.map((s) => (
                <div key={s.title} className="rounded-lg border p-3">
                  <p className="text-xs text-slate-500">{s.title}</p>
                  <p className="font-mono font-bold">{s.value}</p>
                </div>
              ))}
            </CardContent>
          </Card>

          {!canSeeItems ? (
            <Card className="border-none shadow-sm">
              <CardContent className="py-10 text-center text-sm text-slate-500">
                Payroll line items require payroll clearance.
              </CardContent>
            </Card>
          ) : (
            <DataTable columns={cols} data={items} emptyMessage="No payroll items in this run" />
          )}

          <Card className="border-none shadow-sm">
            <CardHeader>
              <CardTitle className="text-base">Disbursements</CardTitle>
              <CardDescription>Net pay settled against salary payable</CardDescription>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              {payments.length === 0 ? (
                <p className="text-slate-500 text-center py-4">No payments recorded yet.</p>
              ) : payments.map((p) => (
                <div key={p.id} className="flex flex-wrap justify-between gap-2 border-b py-2">
                  <span className="font-mono">{p.paymentNumber}</span>
                  <span>{p.employeeName || 'Whole run'}</span>
                  <span className="font-mono font-semibold">{formatCurrency(p.amount)}</span>
                  <span className="text-xs text-slate-500">{p.paymentMethod} · {formatDate(p.paymentDate)}</span>
                  <span className="text-xs text-slate-400">{p.recordedByName || '—'}</span>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>
      )}

      <Dialog open={payOpen} onOpenChange={setPayOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{payForm.payrollItemId ? `Pay ${payForm.employeeName}` : 'Disburse Whole Run'}</DialogTitle>
          </DialogHeader>
          <form onSubmit={submitPay} className="space-y-3">
            <div className="space-y-1">
              <Label>Amount</Label>
              <Input type="number" step="0.01" value={payForm.amount} onChange={(e) => setPayForm({ ...payForm, amount: e.target.value })} placeholder="Full outstanding when blank" />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label>Method</Label>
                <Select value={payForm.paymentMethod} onValueChange={(v) => setPayForm({ ...payForm, paymentMethod: v })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>{PAYMENT_METHODS.map((m) => <SelectItem key={m} value={m}>{m.replace(/_/g, ' ')}</SelectItem>)}</SelectContent>
                </Select>
              </div>
              <div className="space-y-1"><Label>Payment Date</Label><Input type="date" value={payForm.paymentDate} onChange={(e) => setPayForm({ ...payForm, paymentDate: e.target.value })} /></div>
            </div>
            <div className="space-y-1"><Label>Notes</Label><Textarea value={payForm.notes} onChange={(e) => setPayForm({ ...payForm, notes: e.target.value })} /></div>
            <DialogFooter><Button type="submit">Record Payment</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default HrPayrollRunDetail;
