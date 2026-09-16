import React, { useEffect, useState } from 'react';
import { financeApi } from '../../api';
import { useNavigate } from 'react-router-dom';


import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { formatCurrency } from '../../utils';
import {
  DollarSign, TrendingUp, TrendingDown, Wallet, Receipt,
  BarChart3, PieChart, ArrowUpRight, ArrowDownRight, Clock, ShieldCheck,
  RefreshCw, FileText, Download, Calculator
} from 'lucide-react';
import {
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  BarChart, Bar
} from 'recharts';
import { Badge } from '../../components/ui/badge';
import ReconciliationDialog from './components/ReconciliationDialog';
import POSInvoiceDialog from './components/POSInvoiceDialog';

const FinanceDashboard = () => {
  const navigate = useNavigate();

  const [data, setData] = useState({
    totalRevenue: 0,
    totalExpenses: 0,
    netProfit: 0,
    unitRevenue: {},
    transactions: [],
    accountsReceivable: 0,
    cashInHand: 0,
    bankBalance: 0,
    accountsPayable: 0,
    cashInflow: 0,
    cashOutflow: 0,
    accountingNote: '',
  });

  const [loading, setLoading] = useState(true);
  const [reconcileOpen, setReconcileOpen] = useState(false);
  const [posOpen, setPosOpen] = useState(false);

  const [openReceivables, setOpenReceivables] = useState([]);

  const [error, setError] = useState(null);

  const fetchDashboard = async () => {
    setLoading(true);
    setError(null);
    try {
      const [dashRes, txRes, arRes] = await Promise.all([
        financeApi.getDashboard(),
        financeApi.getTransactions(),
        financeApi.getReceivables().catch(() => ({ data: { data: [] } })),
      ]);
      setOpenReceivables(arRes.data?.data || []);
      setData({
        ...dashRes.data.data,
        transactions: txRes.data.data || []
      });
    } catch {
      setError('Failed to load finance dashboard.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
  }, []);

  const kpiCards = [
    {
      title: 'Total Revenue',
      value: formatCurrency(data.totalRevenue),
      icon: TrendingUp,
      iconColor: 'text-emerald-600',
      bgColor: 'bg-emerald-50',
      description: 'Posted INCOME (not cash receipts)'
    },
    {
      title: 'Total Expenses',
      value: formatCurrency(data.totalExpenses),
      icon: TrendingDown,
      iconColor: 'text-rose-600',
      bgColor: 'bg-rose-50',
      description: 'Posted EXPENSE + COGS'
    },
    {
      title: 'Net Result',
      value: formatCurrency(data.netProfit),
      icon: DollarSign,
      iconColor: data.netProfit >= 0 ? 'text-primary' : 'text-rose-600',
      bgColor: 'bg-primary/10',
      description: 'Revenue − expenses'
    },
    {
      title: 'Accounts Receivable',
      value: formatCurrency(data.accountsReceivable || 0),
      icon: Clock,
      iconColor: 'text-amber-600',
      bgColor: 'bg-amber-50',
      description: 'Open invoice outstanding'
    },
    {
      title: 'Cash + Bank',
      value: formatCurrency((Number(data.cashInHand) || 0) + (Number(data.bankBalance) || 0)),
      icon: Wallet,
      iconColor: 'text-blue-600',
      bgColor: 'bg-blue-50',
      description: `Cash ${formatCurrency(data.cashInHand || 0)} · Bank ${formatCurrency(data.bankBalance || 0)}`
    },
    {
      title: 'Accounts Payable',
      value: formatCurrency(data.accountsPayable || 0),
      icon: ShieldCheck,
      iconColor: 'text-purple-600',
      bgColor: 'bg-purple-50',
      description: 'Inventory receipts on AP'
    }
  ];



  // Process unit revenue for chart
  const unitChartData = Object.entries(data.unitRevenue || {})
    .filter(([, val]) => val > 0)
    .map(([unit, val]) => ({
      name: unit.replace(/_/g, ' '),
      revenue: Number(val) || 0
    }));

  const expenseChartData = Object.entries(data.expenseByModule || {})
    .filter(([, val]) => val > 0)
    .map(([module, val]) => ({
      name: module,
      expense: Number(val) || 0
    }));

  const revenueTransactions = (data.transactions || []).filter((tx) => tx.transactionType === 'INCOME');
  const expenseTransactions = (data.transactions || []).filter((tx) => tx.transactionType === 'EXPENSE');

  if (loading) {
    return <EnterprisePage title="Financial Oversight Center" subtitle="Loading..." breadcrumbs={['Finance']} loading />;
  }

  return (
    <EnterprisePage
      title="Financial Oversight Center"
      subtitle="Ledger KPIs from posted finance transactions and open invoices — not frontend estimates"
      breadcrumbs={['Finance', 'Dashboard']}
      error={error}
      onRetry={fetchDashboard}
      onRefresh={fetchDashboard}
      stats={kpiCards}
      actions={
        <>
          <Button variant="outline" size="sm" onClick={fetchDashboard}>
            <RefreshCw className="mr-2 h-4 w-4" /> Refresh
          </Button>
          <Button size="sm" className="bg-primary hover:bg-primary/90" onClick={() => setReconcileOpen(true)}>
            <Calculator className="mr-2 h-4 w-4" /> Reconciliation
          </Button>
        </>
      }
    >
      <div className="grid grid-cols-1 xl:grid-cols-12 gap-6">
        <div className="xl:col-span-8 space-y-6">
          <Card className="border-none shadow-sm">
            <CardHeader className="flex flex-row items-center justify-between">
              <div>
                <CardTitle className="text-lg font-bold">Revenue by Business Unit</CardTitle>
                <CardDescription>Income distribution across farm and pharmacy units with posted activity</CardDescription>
              </div>
              <BarChart3 className="text-slate-400 h-5 w-5" />
            </CardHeader>
            <CardContent>
              <div className="h-[300px]">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={unitChartData}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                    <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 11 }} />
                    <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 11 }} />
                    <Tooltip />
                    <Bar dataKey="revenue" fill="#4f46e5" radius={[4, 4, 0, 0]} barSize={40} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader className="flex flex-row items-center justify-between">
              <div>
                <CardTitle className="text-lg font-bold">Expense by Source Module</CardTitle>
                <CardDescription>Operating costs from inventory, pharmacy COGS, and farm/vet consumption</CardDescription>
              </div>
              <PieChart className="text-slate-400 h-5 w-5" />
            </CardHeader>
            <CardContent>
              <div className="h-[260px]">
                {expenseChartData.length === 0 ? (
                  <p className="text-sm text-slate-500 text-center py-16">No expense transactions recorded yet.</p>
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={expenseChartData}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                      <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 11 }} />
                      <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 11 }} />
                      <Tooltip />
                      <Bar dataKey="expense" fill="#e11d48" radius={[4, 4, 0, 0]} barSize={36} />
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </div>
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader>
              <div className="flex justify-between items-center">
                <div>
                  <CardTitle className="text-lg font-bold">Revenue Log</CardTitle>
                  <CardDescription>Income posted from pharmacy sales and chicken sales (INCOME type only)</CardDescription>
                </div>
                <Badge variant="outline">{revenueTransactions.length} records</Badge>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {revenueTransactions.length === 0 ? (
                  <p className="text-sm text-slate-500 py-6 text-center">No revenue transactions yet. Sales and services post here automatically.</p>
                ) : revenueTransactions.slice(0, 6).map((tx, i) => (
                  <div key={i} className="flex items-center justify-between p-3 border rounded-lg hover:bg-slate-50 transition-colors">
                    <div className="flex items-center gap-4">
                      <div className="p-2 rounded-full bg-emerald-50 text-emerald-600">
                        <ArrowUpRight className="h-4 w-4" />
                      </div>
                      <div>
                        <p className="text-sm font-bold text-slate-800">{tx.description}</p>
                        <div className="flex items-center gap-2">
                          <Badge variant="outline" className="text-[10px] font-mono py-0">{tx.sourceModule || tx.businessUnit}</Badge>
                          <span className="text-[10px] text-slate-400">{tx.referenceType}{tx.referenceId != null ? `#${tx.referenceId}` : ''}</span>
                          <span className="text-[10px] text-slate-400">{tx.transactionDate}</span>
                        </div>
                      </div>
                    </div>
                    <p className="text-sm font-bold text-emerald-600">+{formatCurrency(tx.amount)}</p>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader>
              <div className="flex justify-between items-center">
                <div>
                  <CardTitle className="text-lg font-bold">Expense Log</CardTitle>
                  <CardDescription>Costs from stock consumption, drugs, feed, maintenance, and HR salary expense (5210)</CardDescription>
                </div>
                <Badge variant="outline">{expenseTransactions.length} records</Badge>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {expenseTransactions.length === 0 ? (
                  <p className="text-sm text-slate-500 py-6 text-center">No expense transactions yet. Inventory and vet dispense post here automatically.</p>
                ) : expenseTransactions.slice(0, 6).map((tx, i) => (
                  <div key={i} className="flex items-center justify-between p-3 border rounded-lg hover:bg-slate-50 transition-colors">
                    <div className="flex items-center gap-4">
                      <div className="p-2 rounded-full bg-rose-50 text-rose-600">
                        <ArrowDownRight className="h-4 w-4" />
                      </div>
                      <div>
                        <p className="text-sm font-bold text-slate-800">{tx.description}</p>
                        <div className="flex items-center gap-2">
                          <Badge variant="outline" className="text-[10px] font-mono py-0">{tx.sourceModule || 'MANUAL'}</Badge>
                          <span className="text-[10px] text-slate-400">{tx.referenceType}{tx.referenceId != null ? `#${tx.referenceId}` : ''}</span>
                          <span className="text-[10px] text-slate-400">{tx.transactionDate}</span>
                        </div>
                      </div>
                    </div>
                    <p className="text-sm font-bold text-rose-600">-{formatCurrency(tx.amount)}</p>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader>
              <div className="flex justify-between items-center">
                <div>
                  <CardTitle className="text-lg font-bold">Consolidated Ledger</CardTitle>
                  <CardDescription>Real-time transaction stream from all business modules</CardDescription>
                </div>
                <Button variant="ghost" size="sm" onClick={fetchDashboard}>
                  <RefreshCw className="h-4 w-4 mr-1" /> Refresh
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {(data.transactions || []).length === 0 ? (
                  <p className="text-sm text-slate-500 py-6 text-center">Ledger is empty. Transactions appear when modules post sales, purchases, or expenses.</p>
                ) : data.transactions?.slice(0, 8).map((tx, i) => (
                  <div key={i} className="flex items-center justify-between p-3 border rounded-lg hover:bg-slate-50 transition-colors">
                    <div className="flex items-center gap-4">
                      <div className={`p-2 rounded-full ${tx.transactionType === 'INCOME' ? 'bg-emerald-50 text-emerald-600' : 'bg-rose-50 text-rose-600'}`}>
                        {tx.transactionType === 'INCOME' ? <ArrowUpRight className="h-4 w-4" /> : <ArrowDownRight className="h-4 w-4" />}
                      </div>
                      <div>
                        <p className="text-sm font-bold text-slate-800">{tx.description}</p>
                        <div className="flex items-center gap-2">
                          <Badge variant="outline" className="text-[10px] font-mono py-0">{tx.businessUnit}</Badge>
                          <span className="text-[10px] text-slate-400">{tx.transactionDate}</span>
                        </div>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className={`text-sm font-bold ${tx.transactionType === 'INCOME' ? 'text-emerald-600' : 'text-rose-600'}`}>
                        {tx.transactionType === 'INCOME' ? '+' : '-'}{formatCurrency(tx.amount)}
                      </p>
                      <p className="text-[10px] text-slate-400">
                        {tx.sourceModule || 'MANUAL'} · {tx.referenceType || '—'}
                        {tx.referenceId != null ? ` #${tx.referenceId}` : ''}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Sidebar Space */}
        <div className="xl:col-span-4 space-y-6">
          <Card className="border-none shadow-sm bg-emerald-900 text-white overflow-hidden">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-bold flex items-center gap-2 uppercase tracking-widest text-emerald-200">
                Quick Command Center
              </CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-2 gap-3 pb-6">
              <Button variant="outline" className="h-20 flex flex-col gap-2 bg-white/5 border-white/10 hover:bg-white/10 text-white border-0 shadow-none">
                <Receipt className="h-5 w-5 text-emerald-300" />
                <span className="text-[11px] font-bold">GENERATE PO</span>
              </Button>
              <Button variant="outline" className="h-20 flex flex-col gap-2 bg-white/5 border-white/10 hover:bg-white/10 text-white border-0 shadow-none" onClick={() => setPosOpen(true)}>
                <FileText className="h-5 w-5 text-emerald-300" />
                <span className="text-[11px] font-bold">POS INVOICE</span>
              </Button>

              <Button variant="outline" className="h-20 flex flex-col gap-2 bg-white/5 border-white/10 hover:bg-white/10 text-white border-0 shadow-none">
                <Wallet className="h-5 w-5 text-emerald-300" />
                <span className="text-[11px] font-bold">DEPOSIT</span>
              </Button>
              <Button variant="outline" className="h-20 flex flex-col gap-2 bg-white/5 border-white/10 hover:bg-white/10 text-white border-0 shadow-none" onClick={() => navigate('/finance/reports')}>
                <Download className="h-5 w-5 text-emerald-300" />
                <span className="text-[11px] font-bold">REPORTS</span>
              </Button>

            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader className="pb-2">
              <CardTitle className="text-base font-bold flex items-center gap-2">
                <Clock className="h-4 w-4 text-amber-500" /> Open Receivables
              </CardTitle>
              <CardDescription className="text-xs">From open invoices (authoritative AR)</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {openReceivables.length === 0 ? (
                  <p className="text-sm text-gray-500">No open invoices with outstanding balances.</p>
                ) : openReceivables.slice(0, 8).map((inv) => {
                  const outstanding = Math.max(0, (Number(inv.totalAmount) || 0) - (Number(inv.paidAmount) || 0));
                  return (
                    <div key={inv.id} className="space-y-1">
                      <div className="flex justify-between text-xs font-semibold">
                        <span className="truncate pr-2">{inv.clientName} · {inv.invoiceNumber}</span>
                        <span>{formatCurrency(outstanding)}</span>
                      </div>
                      <div className="flex justify-between text-[10px] text-slate-400">
                        <span>{inv.sourceModule || inv.invoiceType} · {inv.status}</span>
                        <span>Due {inv.dueDate || '—'}</span>
                      </div>
                    </div>
                  );
                })}
                <div className="pt-2 border-t text-xs font-bold text-slate-600 flex justify-between">
                  <span>Total AR</span>
                  <span>{formatCurrency(data.accountsReceivable || 0)}</span>
                </div>
                {(Number(data.cashInflow) > 0 || Number(data.cashOutflow) > 0) && (
                  <div className="pt-2 border-t text-[11px] text-slate-500 space-y-1">
                    <div className="flex justify-between"><span>Cash/Bank inflows</span><span>{formatCurrency(data.cashInflow || 0)}</span></div>
                    <div className="flex justify-between"><span>Cash/Bank outflows</span><span>{formatCurrency(data.cashOutflow || 0)}</span></div>
                  </div>
                )}
                {data.accountingNote && (
                  <p className="text-[10px] text-slate-400 pt-2 border-t leading-relaxed">{data.accountingNote}</p>
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
      <ReconciliationDialog
        open={reconcileOpen}
        onOpenChange={setReconcileOpen}
        systemAmount={data.cashInHand || 0}
        onSuccess={fetchDashboard}
      />

      <POSInvoiceDialog
        open={posOpen}
        onOpenChange={setPosOpen}
        onSuccess={fetchDashboard}
      />
    </EnterprisePage>
  );
};

export default FinanceDashboard;

