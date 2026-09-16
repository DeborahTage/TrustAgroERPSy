import React, { useCallback, useEffect, useState } from 'react';
import { consultancyApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { formatCurrency } from '../../utils';
import { DollarSign, Briefcase, ClipboardList, Package } from 'lucide-react';

const ConsultancyReports = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await consultancyApi.getReports();
      setData(res.data.data);
    } catch {
      setError('Failed to load consultancy reports.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const byStatus = data?.engagementsByStatus || {};
  const stats = data ? [
    {
      title: 'Consultancy Revenue',
      value: formatCurrency(data.consultancyRevenueFromFinance || 0),
      icon: DollarSign,
      iconColor: 'text-emerald-600',
      bgColor: 'bg-emerald-50',
    },
    {
      title: 'Open Requests',
      value: data.requestsOpen || 0,
      icon: ClipboardList,
      iconColor: 'text-blue-600',
      bgColor: 'bg-blue-50',
    },
    {
      title: 'Active Engagements',
      value: byStatus.ACTIVE || 0,
      icon: Briefcase,
      iconColor: 'text-primary',
      bgColor: 'bg-primary/10',
    },
    {
      title: 'Deliverables Pending',
      value: data.deliverablesPendingApproval || 0,
      icon: Package,
      iconColor: 'text-amber-600',
      bgColor: 'bg-amber-50',
    },
  ] : [];

  return (
    <EnterprisePage
      title="Consultancy Reports"
      subtitle="Aggregates from engagements and Finance CONSULTANCY income"
      breadcrumbs={['Consultancy', 'Reports']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      stats={stats}
    >
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Engagements by Status</CardTitle>
            <CardDescription>Live counts from the engagement ledger</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            {!data ? (
              <p className="text-slate-500 text-center py-6">No report data.</p>
            ) : (
              Object.entries(byStatus).map(([status, count]) => (
                <div key={status} className="flex justify-between border-b py-2">
                  <span>{status}</span>
                  <span className="font-mono font-bold">{count}</span>
                </div>
              ))
            )}
          </CardContent>
        </Card>

        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Notes</CardTitle>
            <CardDescription>How figures are derived</CardDescription>
          </CardHeader>
          <CardContent className="text-sm text-slate-600 space-y-2">
            <p>{data?.note || 'No notes available.'}</p>
            <div className="pt-2 border-t space-y-2">
              <div className="flex justify-between">
                <span>Revenue (Finance)</span>
                <span className="font-mono font-bold">{formatCurrency(data?.consultancyRevenueFromFinance || 0)}</span>
              </div>
              <div className="flex justify-between">
                <span>Requests open</span>
                <span className="font-mono">{data?.requestsOpen || 0}</span>
              </div>
              <div className="flex justify-between">
                <span>Deliverables awaiting approval</span>
                <span className="font-mono">{data?.deliverablesPendingApproval || 0}</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </EnterprisePage>
  );
};

export default ConsultancyReports;
