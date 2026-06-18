import React, { useEffect, useState } from 'react';
import { financeApi } from '../../api';
import { useNavigate } from 'react-router-dom';


import StatsCard from '../../components/shared/StatsCard';
import PageHeader from '../../components/shared/PageHeader';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { useLanguage } from '../../context/LanguageContext';
import { formatCurrency } from '../../utils';
import {
  DollarSign, TrendingUp, TrendingDown, Activity, Wallet, Receipt,
  BarChart3, PieChart, ArrowUpRight, ArrowDownRight, Clock, ShieldCheck,
  RefreshCw, FileText, Download, Calculator
} from 'lucide-react';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  BarChart, Bar, Cell, Legend
} from 'recharts';
import { Badge } from '../../components/ui/badge';
import ReconciliationDialog from './components/ReconciliationDialog';
import POSInvoiceDialog from './components/POSInvoiceDialog';

const FinanceDashboard = () => {
  const { t } = useLanguage();
  const navigate = useNavigate();

  const [data, setData] = useState({
    totalRevenue: 0,
    totalExpenses: 0,
    netProfit: 0,
    unitRevenue: {},
    transactions: [],
    accountsReceivable: 0,
    cashInHand: 0,
    accountsPayable: 0
  });

  const [loading, setLoading] = useState(true);
  const [reconcileOpen, setReconcileOpen] = useState(false);
  const [posOpen, setPosOpen] = useState(false);

  const fetchDashboard = async () => {
    setLoading(true);
    try {
      const [dashRes, txRes] = await Promise.all([
        financeApi.getDashboard(),
        financeApi.getTransactions()
      ]);
      setData({
        ...dashRes.data.data,
        transactions: txRes.data.data || []
      });
    } catch (e) {
      console.error('Failed to fetch finance dashboard', e);
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
      description: 'Consolidated income'
    },
    {
      title: 'Total Expenses',
      value: formatCurrency(data.totalExpenses),
      icon: TrendingDown,
      iconColor: 'text-rose-600',
      bgColor: 'bg-rose-50',
      description: 'Operating costs'
    },
    {
      title: 'Net Profit',
      value: formatCurrency(data.netProfit),
      icon: DollarSign,
      iconColor: data.netProfit >= 0 ? 'text-indigo-600' : 'text-rose-600',
      bgColor: 'bg-indigo-50',
      description: 'Bottom line earnings'
    },
    {
      title: 'Accounts Receivable',
      value: formatCurrency(data.accountsReceivable || 0),
      icon: Clock,
      iconColor: 'text-amber-600',
      bgColor: 'bg-amber-50',
      description: 'Pending payments'
    },
    {
      title: 'Cash Position',
      value: formatCurrency(data.cashInHand || 0),
      icon: Wallet,
      iconColor: 'text-blue-600',
      bgColor: 'bg-blue-50',
      description: 'Liquid assets'
    },
    {
      title: 'Accounts Payable',
      value: formatCurrency(data.accountsPayable || 0),
      icon: ShieldCheck,
      iconColor: 'text-purple-600',
      bgColor: 'bg-purple-50',
      description: 'Pending obligations'
    }
  ];



  // Process unit revenue for chart
  const unitChartData = Object.entries(data.unitRevenue || {}).map(([unit, val]) => ({
    name: unit.replace('_', ' '),
    revenue: val || 0
  }));

  if (loading) return <div className="p-8 text-center"><Activity className="animate-spin h-8 w-8 text-blue-600 mx-auto" /></div>;

  return (
    <div className="flex flex-col gap-6 p-4 md:p-8 bg-slate-50 min-h-screen">
      <div className="flex justify-between items-center">
        <PageHeader
          title="Financial Oversight Center"
          subtitle="Multi-stream revenue tracking and professional ERP accounting"
        />
        <div className="flex gap-3">
          <Button variant="outline" size="sm" className="bg-white">
            <RefreshCw className="mr-2 h-4 w-4" /> Hard Close
          </Button>
          <Button size="sm" className="bg-indigo-600 hover:bg-indigo-700" onClick={() => setReconcileOpen(true)}>
            <Calculator className="mr-2 h-4 w-4" /> Reconciliation
          </Button>

        </div>
      </div>

      {/* KPI Section */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        {kpiCards.map((card, index) => (
          <StatsCard key={index} {...card} />
        ))}
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-12 gap-6">
        {/* Main Analytics */}
        <div className="xl:col-span-8 space-y-6">
          <Card className="border-none shadow-sm">
            <CardHeader className="flex flex-row items-center justify-between">
              <div>
                <CardTitle className="text-lg font-bold">Revenue by Business Unit</CardTitle>
                <CardDescription>Income distribution across farm, pharmacy, and services</CardDescription>
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
            <CardHeader>
              <div className="flex justify-between items-center">
                <div>
                  <CardTitle className="text-lg font-bold">Consolidated Ledger</CardTitle>
                  <CardDescription>Real-time transaction stream from all business modules</CardDescription>
                </div>
                <Button variant="ghost" size="sm">Export CSV</Button>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {data.transactions?.slice(0, 8).map((tx, i) => (
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
                      <p className="text-[10px] text-slate-400">Ref: {tx.referenceType || 'Manual'}</p>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Sidebar Space */}
        <div className="xl:col-span-4 space-y-6">
          <Card className="border-none shadow-sm bg-indigo-900 text-white overflow-hidden">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-bold flex items-center gap-2 uppercase tracking-widest text-indigo-200">
                Quick Command Center
              </CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-2 gap-3 pb-6">
              <Button variant="outline" className="h-20 flex flex-col gap-2 bg-white/5 border-white/10 hover:bg-white/10 text-white border-0 shadow-none">
                <Receipt className="h-5 w-5 text-indigo-300" />
                <span className="text-[11px] font-bold">GENERATE PO</span>
              </Button>
              <Button variant="outline" className="h-20 flex flex-col gap-2 bg-white/5 border-white/10 hover:bg-white/10 text-white border-0 shadow-none" onClick={() => setPosOpen(true)}>
                <FileText className="h-5 w-5 text-indigo-300" />
                <span className="text-[11px] font-bold">POS INVOICE</span>
              </Button>

              <Button variant="outline" className="h-20 flex flex-col gap-2 bg-white/5 border-white/10 hover:bg-white/10 text-white border-0 shadow-none">
                <Wallet className="h-5 w-5 text-indigo-300" />
                <span className="text-[11px] font-bold">DEPOSIT</span>
              </Button>
              <Button variant="outline" className="h-20 flex flex-col gap-2 bg-white/5 border-white/10 hover:bg-white/10 text-white border-0 shadow-none" onClick={() => navigate('/finance/reports')}>
                <Download className="h-5 w-5 text-indigo-300" />
                <span className="text-[11px] font-bold">REPORTS</span>
              </Button>

            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader className="pb-2">
              <CardTitle className="text-base font-bold flex items-center gap-2">
                <Clock className="h-4 w-4 text-amber-500" /> AR Aging Watch
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {[
                  { label: '0-30 Days', val: 250000, color: 'bg-emerald-500' },
                  { label: '31-60 Days', val: 120000, color: 'bg-amber-500' },
                  { label: '61-90 Days', val: 55000, color: 'bg-orange-500' },
                  { label: '90+ Days', val: 25000, color: 'bg-rose-500' }
                ].map((bucket, j) => (
                  <div key={j} className="space-y-1">
                    <div className="flex justify-between text-xs font-semibold">
                      <span>{bucket.label}</span>
                      <span>{formatCurrency(bucket.val)}</span>
                    </div>
                    <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
                      <div className={`h-full ${bucket.color}`} style={{ width: `${(bucket.val / 450000) * 100}%` }} />
                    </div>
                  </div>
                ))}
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
    </div>
  );
};

export default FinanceDashboard;

