import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { executiveApi } from '../../api';
import { formatCurrency } from '../../utils';
import EnterprisePage from '../../components/layout/EnterprisePage';
import AlertCard from '../../components/shared/AlertCard';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Badge } from '../../components/ui/badge';
import {
  Building2, Users, Activity, DollarSign, UserCheck,
  TrendingUp, Package, ShoppingCart, Wallet, Bell, Briefcase, Truck,
} from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts';

const PRESETS = [
  { value: 'TODAY', label: 'Today' },
  { value: 'THIS_WEEK', label: 'This week' },
  { value: 'THIS_MONTH', label: 'This month' },
  { value: 'THIS_QUARTER', label: 'This quarter' },
  { value: 'THIS_YEAR', label: 'This year' },
  { value: 'PREVIOUS_MONTH', label: 'Previous month' },
  { value: 'PREVIOUS_QUARTER', label: 'Previous quarter' },
  { value: 'CUSTOM', label: 'Custom' },
];

const BUSINESS_LINES = [
  'ALL', 'CHICKEN_SALE', 'PHARMACY', 'CONSULTANCY', 'TRAINING', 'CRM', 'HR', 'PROCUREMENT', 'OTHER',
];

const statusBadge = (status) => {
  if (status === 'UNAVAILABLE') return <Badge variant="outline" className="text-amber-700 border-amber-200 bg-amber-50">N/A</Badge>;
  if (status === 'INCOMPLETE') return <Badge variant="outline" className="text-orange-700 border-orange-200 bg-orange-50">Incomplete</Badge>;
  return null;
};

const displayMoney = (value, status) => {
  if (status === 'UNAVAILABLE' || status === 'INCOMPLETE') return '—';
  if (value === null || value === undefined) return '—';
  return formatCurrency(value);
};

const AdminDashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [payload, setPayload] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [preset, setPreset] = useState('THIS_MONTH');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [businessLine, setBusinessLine] = useState('ALL');

  const fetchDashboard = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = { preset, businessLine: businessLine === 'ALL' ? undefined : businessLine };
      if (preset === 'CUSTOM') {
        if (!from || !to) {
          setError('Custom range requires both from and to dates.');
          setLoading(false);
          return;
        }
        params.from = from;
        params.to = to;
      }
      const res = await executiveApi.getDashboard(params);
      setPayload(res.data?.data || res.data || {});
    } catch (err) {
      const status = err?.response?.status;
      const message = err?.response?.data?.message;
      if (!err?.response) {
        setError('Cannot reach the API server. Start the backend on port 8081.');
      } else if (status === 403) {
        setError(message || 'You do not have permission to view the executive dashboard.');
      } else {
        setError(message || 'Failed to load executive dashboard data.');
      }
    } finally {
      setLoading(false);
    }
  }, [preset, from, to, businessLine]);

  useEffect(() => { fetchDashboard(); }, [fetchDashboard]);

  const finance = payload.finance || null;
  const sales = payload.sales || null;
  const farm = payload.farm || null;
  const inventory = payload.inventory || null;
  const procurement = payload.procurement || null;
  const services = payload.services || null;
  const workforce = payload.workforce || null;
  const alertsPayload = payload.alerts || null;
  const alerts = alertsPayload?.alerts || [];

  const kpiCards = useMemo(() => {
    const cards = [];
    if (finance) {
      cards.push(
        { title: 'Period Revenue', value: displayMoney(finance.totalRevenue, finance.status), icon: DollarSign, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50', description: 'INCOME ledger only' },
        { title: 'Net Profit', value: displayMoney(finance.netProfit, finance.profitabilityStatus || finance.status), icon: TrendingUp, iconColor: 'text-primary', bgColor: 'bg-primary/10', description: 'Revenue − expenses' },
        { title: 'Cash + Bank', value: displayMoney(
          Number(finance.cashInHand || 0) + Number(finance.bankBalance || 0),
          finance.status,
        ), icon: Wallet, iconColor: 'text-blue-600', bgColor: 'bg-blue-50', description: 'Point-in-time balances' },
        { title: 'Accounts Receivable', value: displayMoney(finance.accountsReceivable, finance.status), icon: UserCheck, iconColor: 'text-violet-600', bgColor: 'bg-violet-50', description: 'Open invoice outstanding' },
      );
    }
    if (farm) {
      cards.push({
        title: 'Active Farms',
        value: farm.activeFarms ?? '—',
        icon: Building2,
        iconColor: 'text-primary',
        bgColor: 'bg-primary/10',
        description: `${farm.activeFlocks ?? 0} active flocks`,
      });
    }
    if (inventory) {
      cards.push({
        title: 'Inventory Value',
        value: displayMoney(inventory.totalValue, inventory.status),
        icon: Package,
        iconColor: 'text-amber-600',
        bgColor: 'bg-amber-50',
        description: `${inventory.lowStockCount ?? 0} low stock`,
      });
    }
    if (sales) {
      cards.push({
        title: 'Lead conversion',
        value: sales.conversionRate != null ? `${sales.conversionRate}%` : '—',
        icon: ShoppingCart,
        iconColor: 'text-emerald-600',
        bgColor: 'bg-emerald-50',
        description: 'CRM commercial dashboard',
      });
    }
    if (workforce) {
      cards.push({
        title: 'Active headcount',
        value: workforce.activeHeadcount ?? '—',
        icon: Users,
        iconColor: 'text-blue-600',
        bgColor: 'bg-blue-50',
        description: 'Aggregated HR only',
      });
    }
    return cards;
  }, [finance, farm, inventory, sales, workforce]);

  const revenueByModule = Object.entries(finance?.revenueByModule || {})
    .filter(([, v]) => Number(v) > 0)
    .map(([name, revenue]) => ({ name: name.replace(/_/g, ' '), revenue: Number(revenue) }));

  const filters = (
    <div className="flex flex-wrap gap-3 items-end mb-6">
      <label className="text-xs font-semibold text-slate-600">
        Period
        <select
          className="mt-1 block rounded-lg border border-slate-200 px-3 py-2 text-sm bg-white"
          value={preset}
          onChange={(e) => setPreset(e.target.value)}
        >
          {PRESETS.map((p) => <option key={p.value} value={p.value}>{p.label}</option>)}
        </select>
      </label>
      {preset === 'CUSTOM' && (
        <>
          <label className="text-xs font-semibold text-slate-600">
            From
            <input type="date" className="mt-1 block rounded-lg border border-slate-200 px-3 py-2 text-sm" value={from} onChange={(e) => setFrom(e.target.value)} />
          </label>
          <label className="text-xs font-semibold text-slate-600">
            To
            <input type="date" className="mt-1 block rounded-lg border border-slate-200 px-3 py-2 text-sm" value={to} onChange={(e) => setTo(e.target.value)} />
          </label>
        </>
      )}
      <label className="text-xs font-semibold text-slate-600">
        Business line
        <select
          className="mt-1 block rounded-lg border border-slate-200 px-3 py-2 text-sm bg-white"
          value={businessLine}
          onChange={(e) => setBusinessLine(e.target.value)}
        >
          {BUSINESS_LINES.map((b) => <option key={b} value={b}>{b.replace(/_/g, ' ')}</option>)}
        </select>
      </label>
      {payload.period && (
        <span className="text-xs text-slate-500 pb-2">
          {payload.period.from} → {payload.period.to}
        </span>
      )}
    </div>
  );

  if (loading) {
    return (
      <EnterprisePage
        title="Executive Command Center"
        subtitle="Read-only company performance across modules"
        breadcrumbs={['Home', 'Executive', 'Dashboard']}
        loading
      />
    );
  }

  return (
    <EnterprisePage
      title="Executive Command Center"
      subtitle={`Welcome back, ${user?.fullName || 'Leader'}. Finance is the source of truth for revenue (INCOME ≠ payments).`}
      breadcrumbs={['Home', 'Executive', 'Dashboard']}
      error={error}
      onRetry={fetchDashboard}
      onRefresh={fetchDashboard}
      stats={kpiCards}
    >
      {filters}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
        <Card className="lg:col-span-2 border-none shadow-sm bg-white rounded-2xl">
          <CardHeader className="border-b border-gray-50 px-6 py-4 flex flex-row items-center justify-between">
            <CardTitle className="text-base font-black uppercase tracking-tight">Revenue by module</CardTitle>
            {finance && statusBadge(finance.status)}
          </CardHeader>
          <CardContent className="p-6">
            {!finance ? (
              <p className="text-sm text-gray-500 text-center py-16">Finance section unavailable for your role.</p>
            ) : (
              <div className="h-[280px]">
                {revenueByModule.length === 0 ? (
                  <p className="text-sm text-gray-500 text-center py-16">No INCOME recorded in this period.</p>
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={revenueByModule}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                      <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                      <YAxis tick={{ fontSize: 11 }} />
                      <Tooltip formatter={(v) => formatCurrency(v)} />
                      <Bar dataKey="revenue" fill="#10B981" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </div>
            )}
            {finance?.accountingNote && (
              <p className="text-xs text-slate-500 mt-3">{finance.accountingNote}</p>
            )}
          </CardContent>
        </Card>

        <Card className="border-none shadow-sm bg-white rounded-2xl">
          <CardHeader className="border-b border-gray-100 px-6 py-4">
            <CardTitle className="flex items-center gap-2 text-base font-black uppercase tracking-tight">
              <Bell className="h-4 w-4 text-primary" /> Alerts
              <Badge variant="outline" className="ml-auto">{alerts.length}</Badge>
            </CardTitle>
          </CardHeader>
          <CardContent className="p-6 space-y-3 max-h-[320px] overflow-y-auto">
            {alerts.length === 0 ? (
              <p className="text-sm text-gray-500 text-center py-8">No active executive alerts.</p>
            ) : alerts.map((a, idx) => (
              <button
                key={`${a.title}-${idx}`}
                type="button"
                className="w-full text-left"
                onClick={() => a.link && navigate(a.link)}
              >
                <AlertCard
                  title={a.title}
                  description={a.message}
                  type={a.severity === 'HIGH' ? 'critical' : a.severity === 'MEDIUM' ? 'warning' : 'info'}
                />
              </button>
            ))}
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6 mb-8">
        {farm && (
          <Card className="border-none shadow-sm bg-white rounded-2xl">
            <CardHeader className="border-b border-gray-50 px-6 py-4 flex flex-row items-center justify-between">
              <CardTitle className="text-sm font-black uppercase tracking-tight">Farm</CardTitle>
              {statusBadge(farm.fcr?.status)}
            </CardHeader>
            <CardContent className="p-6 space-y-2 text-sm">
              <p>Active farms: <strong>{farm.activeFarms ?? '—'}</strong></p>
              <p>Active flocks: <strong>{farm.activeFlocks ?? '—'}</strong></p>
              <p>Mortality in range: <strong>{farm.mortalityInRange ?? 0}</strong></p>
              <p className="text-xs text-slate-500">FCR: N/A — not calculated from live feed/weight data.</p>
            </CardContent>
          </Card>
        )}

        {inventory && (
          <Card className="border-none shadow-sm bg-white rounded-2xl">
            <CardHeader className="border-b border-gray-50 px-6 py-4">
              <CardTitle className="text-sm font-black uppercase tracking-tight flex items-center gap-2">
                <Package className="h-4 w-4" /> Inventory
              </CardTitle>
            </CardHeader>
            <CardContent className="p-6 space-y-2 text-sm">
              <p>Value: <strong>{displayMoney(inventory.totalValue, inventory.status)}</strong></p>
              <p>Low stock: <strong>{inventory.lowStockCount ?? 0}</strong></p>
              <p>Expiring: <strong>{inventory.expiringCount ?? 0}</strong></p>
            </CardContent>
          </Card>
        )}

        {sales && (
          <Card className="border-none shadow-sm bg-white rounded-2xl">
            <CardHeader className="border-b border-gray-50 px-6 py-4">
              <CardTitle className="text-sm font-black uppercase tracking-tight">Sales / CRM</CardTitle>
            </CardHeader>
            <CardContent className="p-6 space-y-2 text-sm">
              <p>Invoiced: <strong>{displayMoney(sales.crmInvoiced)}</strong></p>
              <p>Paid: <strong>{displayMoney(sales.crmPaid)}</strong></p>
              <p>Outstanding AR: <strong>{displayMoney(sales.crmOutstandingAr)}</strong></p>
              <p>Conversion: <strong>{sales.conversionRate != null ? `${sales.conversionRate}%` : '—'}</strong></p>
            </CardContent>
          </Card>
        )}

        {procurement && (
          <Card className="border-none shadow-sm bg-white rounded-2xl">
            <CardHeader className="border-b border-gray-50 px-6 py-4">
              <CardTitle className="text-sm font-black uppercase tracking-tight flex items-center gap-2">
                <Truck className="h-4 w-4" /> Procurement
              </CardTitle>
            </CardHeader>
            <CardContent className="p-6 space-y-2 text-sm">
              <p>Open POs: <strong>{procurement.openPurchaseOrders ?? 0}</strong></p>
              <p>Unpaid invoices: <strong>{procurement.approvedUnpaidInvoices ?? 0}</strong></p>
              <p>AP outstanding: <strong>{displayMoney(procurement.apOutstanding)}</strong></p>
            </CardContent>
          </Card>
        )}

        {services && (
          <Card className="border-none shadow-sm bg-white rounded-2xl">
            <CardHeader className="border-b border-gray-50 px-6 py-4">
              <CardTitle className="text-sm font-black uppercase tracking-tight flex items-center gap-2">
                <Briefcase className="h-4 w-4" /> Services
              </CardTitle>
            </CardHeader>
            <CardContent className="p-6 space-y-2 text-sm">
              <p>Consultancy revenue: <strong>{displayMoney(services.consultancy?.revenue)}</strong></p>
              <p>Training revenue: <strong>{displayMoney(services.training?.revenue)}</strong></p>
              <p className="text-xs text-slate-500">{services.note}</p>
            </CardContent>
          </Card>
        )}

        {workforce && (
          <Card className="border-none shadow-sm bg-white rounded-2xl">
            <CardHeader className="border-b border-gray-50 px-6 py-4">
              <CardTitle className="text-sm font-black uppercase tracking-tight flex items-center gap-2">
                <Users className="h-4 w-4" /> Workforce
              </CardTitle>
            </CardHeader>
            <CardContent className="p-6 space-y-2 text-sm">
              <p>Active headcount: <strong>{workforce.activeHeadcount ?? '—'}</strong></p>
              <p>On leave: <strong>{workforce.onLeave ?? 0}</strong></p>
              <p>Pending leave: <strong>{workforce.pendingLeaveRequests ?? 0}</strong></p>
              <p className="text-xs text-slate-500">{workforce.privacyNote}</p>
            </CardContent>
          </Card>
        )}
      </div>

      {finance && (
        <Card className="border-none shadow-sm bg-white rounded-2xl">
          <CardHeader className="border-b border-gray-50 px-6 py-4">
            <CardTitle className="flex items-center gap-2 text-base font-black uppercase tracking-tight">
              <Activity className="h-4 w-4 text-primary" /> Cash flow (period)
            </CardTitle>
          </CardHeader>
          <CardContent className="p-6 grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
            <div>
              <p className="text-slate-500">Inflows</p>
              <p className="text-lg font-bold">{displayMoney(finance.cashInflows)}</p>
            </div>
            <div>
              <p className="text-slate-500">Outflows</p>
              <p className="text-lg font-bold">{displayMoney(finance.cashOutflows)}</p>
            </div>
            <div>
              <p className="text-slate-500">Net change</p>
              <p className="text-lg font-bold">{displayMoney(finance.netCashChange)}</p>
            </div>
          </CardContent>
        </Card>
      )}
    </EnterprisePage>
  );
};

export default AdminDashboard;
