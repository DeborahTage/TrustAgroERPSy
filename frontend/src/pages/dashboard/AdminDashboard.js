import React, { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { dashboardApi, financeApi, notificationApi, auditApi } from '../../api';
import { formatCurrency } from '../../utils';
import EnterprisePage from '../../components/layout/EnterprisePage';
import AlertCard from '../../components/shared/AlertCard';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Badge } from '../../components/ui/badge';
import {
  Building2, Users, Activity, AlertTriangle, DollarSign, UserCheck,
  TrendingUp, Package, HeartPulse, ShoppingCart, Wallet, Bell
} from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts';

const AdminDashboard = () => {
  const { user } = useAuth();
  const [data, setData] = useState({});
  const [finance, setFinance] = useState({});
  const [notifications, setNotifications] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchDashboard = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [adminResult, financeResult, notifResult, auditResult] = await Promise.allSettled([
        dashboardApi.admin(),
        financeApi.getDashboard(),
        notificationApi.getAll(),
        auditApi.getAll(),
      ]);

      if (adminResult.status === 'fulfilled') {
        setData(adminResult.value.data?.data || adminResult.value.data || {});
      } else {
        const err = adminResult.reason;
        const status = err?.response?.status;
        const message = err?.response?.data?.message;
        if (!err?.response) {
          setError('Cannot reach the API server. Start the backend on port 8081 (./start-dev.sh or mvn spring-boot:run -Dspring-boot.run.profiles=local).');
        } else if (status === 403) {
          setError(message || 'You do not have permission to view the executive dashboard.');
        } else {
          setError(message || 'Failed to load executive dashboard data.');
        }
        console.error('Admin dashboard API error:', err);
      }

      if (financeResult.status === 'fulfilled') {
        setFinance(financeResult.value.data?.data || financeResult.value.data || {});
      }

      if (notifResult.status === 'fulfilled') {
        const rows = notifResult.value.data?.data || notifResult.value.data || [];
        setNotifications(Array.isArray(rows) ? rows.slice(0, 6) : []);
      }

      if (auditResult.status === 'fulfilled') {
        const rows = auditResult.value.data?.data || auditResult.value.data || [];
        setAuditLogs(Array.isArray(rows) ? rows.slice(0, 8) : []);
      }
    } catch (e) {
      setError('Failed to load executive dashboard data.');
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchDashboard(); }, [fetchDashboard]);

  const kpiCards = [
    { title: 'YTD Revenue', value: formatCurrency(data.totalRevenue || 0), icon: DollarSign, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50', description: 'From finance ledger' },
    { title: 'Net Profit (YTD)', value: formatCurrency(data.netProfitLoss || 0), icon: TrendingUp, iconColor: (data.netProfitLoss || 0) >= 0 ? 'text-primary' : 'text-rose-600', bgColor: 'bg-primary/10', description: 'Revenue minus expenses' },
    { title: 'Cash Position', value: formatCurrency(data.cashInHand || finance.cashInHand || 0), icon: Wallet, iconColor: 'text-blue-600', bgColor: 'bg-blue-50', description: 'Liquid assets on hand' },
    { title: 'Inventory Value', value: formatCurrency(data.inventoryValue || 0), icon: Package, iconColor: 'text-amber-600', bgColor: 'bg-amber-50', description: 'Stock at average cost' },
    { title: 'Active Farms', value: data.totalFarms || 0, icon: Building2, iconColor: 'text-primary', bgColor: 'bg-primary/10', description: `${data.totalActiveFlocks || 0} active flocks` },
    { title: 'Pharmacy Sales Today', value: formatCurrency(data.pharmacySalesToday || 0), icon: ShoppingCart, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50', description: `${data.totalSalesCount || 0} total sales` },
    { title: 'Accounts Receivable', value: formatCurrency(data.accountsReceivable || finance.accountsReceivable || 0), icon: UserCheck, iconColor: 'text-purple-600', bgColor: 'bg-purple-50', description: 'Outstanding customer balances' },
    { title: 'Pending Approvals', value: (data.pendingPrescriptions || 0) + (data.openHealthReports || 0), icon: HeartPulse, iconColor: 'text-rose-600', bgColor: 'bg-rose-50', description: 'Rx + health reports' },
    { title: 'System Users', value: data.totalUsers || 0, icon: Users, iconColor: 'text-blue-600', bgColor: 'bg-blue-50', description: `${data.activeClients || 0} CRM clients` },
  ];

  const unitRevenue = Object.entries(finance.unitRevenue || {})
    .filter(([, v]) => Number(v) > 0)
    .map(([name, revenue]) => ({ name: name.replace(/_/g, ' '), revenue: Number(revenue) }));

  if (loading) {
    return (
      <EnterprisePage
        title="Executive Command Center"
        subtitle="Real-time company performance across all business units"
        breadcrumbs={['Home', 'Executive', 'Dashboard']}
        loading
      />
    );
  }

  return (
    <EnterprisePage
      title="Executive Command Center"
      subtitle={`Welcome back, ${user?.fullName || 'Administrator'}. Real-time company performance across all business units.`}
      breadcrumbs={['Home', 'Executive', 'Dashboard']}
      error={error}
      onRetry={fetchDashboard}
      onRefresh={fetchDashboard}
      stats={kpiCards}
    >
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
        <Card className="lg:col-span-2 border-none shadow-sm bg-white rounded-2xl">
          <CardHeader className="border-b border-gray-50 px-6 py-4">
            <CardTitle className="text-base font-black uppercase tracking-tight">Revenue by Business Unit</CardTitle>
          </CardHeader>
          <CardContent className="p-6">
            <div className="h-[280px]">
              {unitRevenue.length === 0 ? (
                <p className="text-sm text-gray-500 text-center py-16">No revenue recorded yet. Sales and services post here automatically.</p>
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={unitRevenue}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                    <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                    <YAxis tick={{ fontSize: 11 }} />
                    <Tooltip formatter={(v) => formatCurrency(v)} />
                    <Bar dataKey="revenue" fill="#10B981" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </div>
          </CardContent>
        </Card>

        <Card className="border-none shadow-sm bg-white rounded-2xl">
          <CardHeader className="border-b border-gray-100 px-6 py-4">
            <CardTitle className="flex items-center gap-2 text-base font-black uppercase tracking-tight">
              <Bell className="h-4 w-4 text-primary" /> Live Alerts
              <Badge variant="outline" className="ml-auto">{data.pendingAlerts || 0}</Badge>
            </CardTitle>
          </CardHeader>
          <CardContent className="p-6 space-y-3 max-h-[320px] overflow-y-auto">
            {notifications.length === 0 ? (
              <p className="text-sm text-gray-500 text-center py-8">No active notifications.</p>
            ) : notifications.map((n) => (
              <AlertCard
                key={n.id}
                title={n.title}
                description={n.message}
                type={n.type === 'URGENT' ? 'critical' : n.type === 'WARNING' ? 'warning' : 'info'}
                timestamp={n.createdAt ? new Date(n.createdAt).toLocaleString() : ''}
              />
            ))}
            {(data.lowStockCount > 0 || data.expiringItems > 0) && (
              <>
                {data.lowStockCount > 0 && (
                  <AlertCard title="Low Stock" description={`${data.lowStockCount} SKUs below reorder point`} type="warning" icon={Package} />
                )}
                {data.expiringItems > 0 && (
                  <AlertCard title="Expiry Risk" description={`${data.expiringItems} batches expiring within 30 days`} type="critical" icon={AlertTriangle} />
                )}
              </>
            )}
          </CardContent>
        </Card>
      </div>

      <Card className="border-none shadow-sm bg-white rounded-2xl">
        <CardHeader className="border-b border-gray-50 px-6 py-4">
          <CardTitle className="flex items-center gap-2 text-base font-black uppercase tracking-tight">
            <Activity className="h-4 w-4 text-primary" /> Audit Activity Stream
          </CardTitle>
        </CardHeader>
        <CardContent className="p-6">
          {auditLogs.length === 0 ? (
            <p className="text-sm text-gray-500 text-center py-8">No audit records yet. Operations will appear here as users work in the system.</p>
          ) : (
            <div className="space-y-3">
              {auditLogs.map((log) => (
                <div key={log.id} className="flex items-start justify-between p-3 rounded-xl bg-slate-50 border border-slate-100">
                  <div>
                    <p className="text-sm font-bold text-slate-900">{log.action} — {log.entityType}</p>
                    <p className="text-xs text-slate-500">{log.module} · {log.performedBy?.fullName || log.performedByEmail || 'System'}</p>
                  </div>
                  <span className="text-[10px] font-bold text-slate-400 uppercase">
                    {log.createdAt ? new Date(log.createdAt).toLocaleString() : ''}
                  </span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </EnterprisePage>
  );
};

export default AdminDashboard;
