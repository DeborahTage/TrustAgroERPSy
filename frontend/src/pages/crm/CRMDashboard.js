import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { crmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { formatCurrency } from '../../utils';
import {
  Users, Target, TrendingUp, FileText, ShoppingCart, ClipboardList, DollarSign, AlertCircle, Percent, Link2,
} from 'lucide-react';

const countMap = (obj) => Object.values(obj || {}).reduce((s, n) => s + Number(n || 0), 0);

const CRMDashboard = () => {
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await crmApi.getDashboard();
      setData(res.data.data || {});
    } catch {
      setError('Failed to load CRM dashboard.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const pipelineLeads = data?.pipelineLeads || {};
  const oppByStage = data?.opportunitiesByStage || {};
  const quotesByStatus = data?.quotationsByStatus || {};
  const ordersByStatus = data?.ordersByStatus || {};

  const stats = data ? [
    { title: 'Total Clients', value: data.totalClients || 0, icon: Users, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Active Clients', value: data.activeClients || 0, icon: Users, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: 'Pipeline Leads', value: countMap(pipelineLeads), icon: Target, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: 'Lead Conversion', value: `${Number(data.conversionRate || data.leadConversionRate || 0).toFixed(1)}%`, icon: Percent, iconColor: 'text-teal-600', bgColor: 'bg-teal-50' },
    { title: 'Open Opp. Value', value: formatCurrency(data.openOpportunityValue || 0), icon: TrendingUp, iconColor: 'text-indigo-600', bgColor: 'bg-indigo-50' },
    { title: 'Quotations', value: countMap(quotesByStatus), icon: FileText, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: 'Orders', value: countMap(ordersByStatus), icon: ShoppingCart, iconColor: 'text-slate-600', bgColor: 'bg-slate-50' },
    { title: 'Invoiced Revenue', value: formatCurrency(data.totalOrderRevenueInvoiced || 0), icon: DollarSign, iconColor: 'text-emerald-700', bgColor: 'bg-emerald-50' },
    { title: 'Outstanding AR', value: formatCurrency(data.outstandingAr || 0), icon: DollarSign, iconColor: 'text-amber-700', bgColor: 'bg-amber-50' },
    { title: 'CRM Contracts', value: data.crmLinkedContracts || 0, icon: Link2, iconColor: 'text-violet-600', bgColor: 'bg-violet-50' },
    { title: 'Open Tasks', value: data.openTasks || 0, icon: ClipboardList, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
  ] : [];

  const renderBreakdown = (title, map) => (
    <Card className="border-none shadow-sm">
      <CardHeader>
        <CardTitle className="text-base">{title}</CardTitle>
        <CardDescription>Live counts from CRM commercial APIs</CardDescription>
      </CardHeader>
      <CardContent className="space-y-2 text-sm">
        {Object.keys(map).length === 0 ? (
          <p className="text-slate-500 py-4 text-center">No data yet.</p>
        ) : (
          Object.entries(map).map(([k, v]) => (
            <div key={k} className="flex justify-between border-b py-2">
              <span>{k.replace(/_/g, ' ')}</span>
              <span className="font-mono font-bold">{Number(v)}</span>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  );

  return (
    <EnterprisePage
      title="CRM Dashboard"
      subtitle="Commercial pipeline, customers, orders, and collections"
      breadcrumbs={['CRM']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      stats={stats}
      actions={
        <>
          <Button variant="outline" size="sm" onClick={() => navigate('/crm/leads')}>Leads</Button>
          <Button size="sm" onClick={() => navigate('/crm/orders')}>Orders</Button>
        </>
      }
    >
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Quick Links</CardTitle>
            <CardDescription>Navigate CRM workflows</CardDescription>
          </CardHeader>
          <CardContent className="grid grid-cols-2 gap-3">
            {[
              ['Leads', '/crm/leads'],
              ['Opportunities', '/crm/opportunities'],
              ['Quotations', '/crm/quotations'],
              ['Orders', '/crm/orders'],
              ['Customers', '/crm/clients'],
              ['Tasks', '/crm/tasks'],
              ['Reports', '/crm/reports'],
              ['Visits', '/crm/follow-ups'],
            ].map(([label, path]) => (
              <Button key={path} variant="outline" className="justify-start" onClick={() => navigate(path)}>{label}</Button>
            ))}
          </CardContent>
        </Card>

        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Collections Snapshot</CardTitle>
            <CardDescription>Order invoicing and payments</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            {!data ? (
              <p className="text-sm text-slate-500 py-6 text-center">No data yet.</p>
            ) : (
              <>
                <div className="flex justify-between border-b py-2">
                  <span>Invoiced revenue</span>
                  <span className="font-mono font-bold">{formatCurrency(data.totalOrderRevenueInvoiced || 0)}</span>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span>Total paid</span>
                  <span className="font-mono text-emerald-700">{formatCurrency(data.totalOrderPaid || 0)}</span>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span>Outstanding AR</span>
                  <span className="font-mono text-amber-700">{formatCurrency(data.outstandingAr || 0)}</span>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span>CRM invoices</span>
                  <span className="font-mono">{data.crmInvoices || 0}</span>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span>CRM-linked contracts</span>
                  <span className="font-mono">{data.crmLinkedContracts || 0}</span>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span>Lead conversion</span>
                  <span className="font-mono">{Number(data.conversionRate || data.leadConversionRate || 0).toFixed(1)}%</span>
                </div>
                <div className="flex justify-between border-b py-2">
                  <span>Due follow-ups</span>
                  <span className="font-mono flex items-center gap-1">
                    {(data.dueFollowUps || 0) > 0 && <AlertCircle className="h-3.5 w-3.5 text-amber-600" />}
                    {data.dueFollowUps || 0}
                  </span>
                </div>
              </>
            )}
          </CardContent>
        </Card>

        {renderBreakdown('Leads by Status', pipelineLeads)}
        {renderBreakdown('Opportunities by Stage', oppByStage)}
        {renderBreakdown('Quotations by Status', quotesByStatus)}
        {renderBreakdown('Orders by Status', ordersByStatus)}
      </div>
    </EnterprisePage>
  );
};

export default CRMDashboard;
