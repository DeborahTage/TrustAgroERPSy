import React, { useCallback, useEffect, useState } from 'react';
import { crmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { formatCurrency } from '../../utils';
import { DollarSign, FileText, ShoppingCart, Target, ClipboardList, TrendingUp } from 'lucide-react';

const countMap = (obj) => Object.values(obj || {}).reduce((s, n) => s + Number(n || 0), 0);

const CrmReports = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await crmApi.getCommercialReports();
      setData(res.data.data || {});
    } catch {
      setError('Failed to load commercial reports. Requires crm:report permission.');
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
    { title: 'Open Opp. Value', value: formatCurrency(data.openOpportunityValue || 0), icon: TrendingUp, iconColor: 'text-indigo-600', bgColor: 'bg-indigo-50' },
    { title: 'Invoiced Revenue', value: formatCurrency(data.totalOrderRevenueInvoiced || 0), icon: DollarSign, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: 'Total Paid', value: formatCurrency(data.totalOrderPaid || 0), icon: ShoppingCart, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Open Tasks', value: data.openTasks || 0, icon: ClipboardList, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: 'Pipeline Leads', value: countMap(pipelineLeads), icon: Target, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: 'CRM Invoices', value: data.crmInvoices || 0, icon: FileText, iconColor: 'text-slate-600', bgColor: 'bg-slate-50' },
  ] : [];

  const renderBreakdown = (title, description, map) => (
    <Card className="border-none shadow-sm">
      <CardHeader>
        <CardTitle className="text-base">{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-2 text-sm">
        {Object.keys(map).length === 0 ? (
          <p className="text-slate-500 py-4 text-center">No data.</p>
        ) : (
          Object.entries(map).map(([k, v]) => (
            <div key={k} className="flex justify-between border-b py-2">
              <span>{String(k).replace(/_/g, ' ')}</span>
              <span className="font-mono font-bold">{Number(v)}</span>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  );

  return (
    <EnterprisePage
      title="Commercial Reports"
      subtitle="Pipeline, quotation, order, and collections aggregates"
      breadcrumbs={['CRM', 'Reports']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      stats={stats}
    >
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {renderBreakdown('Leads by Status', 'CrmLead pipeline distribution', pipelineLeads)}
        {renderBreakdown('Opportunities by Stage', 'Open and closed opportunity mix', oppByStage)}
        {renderBreakdown('Quotations by Status', 'Quote lifecycle counts', quotesByStatus)}
        {renderBreakdown('Orders by Status', 'Order fulfillment pipeline', ordersByStatus)}
      </div>
    </EnterprisePage>
  );
};

export default CrmReports;
