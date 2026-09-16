import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { consultancyApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { formatCurrency } from '../../utils';
import { Briefcase, ClipboardList, FileText, CheckCircle, DollarSign, AlertCircle, Package } from 'lucide-react';

const ConsultancyDashboard = () => {
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [extra, setExtra] = useState({ pendingProposals: 0, deliverablesAwaiting: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [dashRes, reportsRes, proposalsRes] = await Promise.all([
        consultancyApi.getDashboard(),
        consultancyApi.getReports().catch(() => ({ data: { data: {} } })),
        consultancyApi.getProposals().catch(() => ({ data: { data: [] } })),
      ]);
      const dash = dashRes.data.data;
      const reports = reportsRes.data?.data || {};
      const proposals = proposalsRes.data?.data || [];
      setData(dash);
      setExtra({
        pendingProposals: proposals.filter((p) => ['DRAFT', 'SENT'].includes(p.status)).length,
        deliverablesAwaiting: reports.deliverablesPendingApproval || 0,
      });
    } catch {
      setError('Failed to load consultancy dashboard.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const stats = data ? [
    { title: 'Active Engagements', value: data.activeEngagements || 0, icon: Briefcase, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Open Requests', value: data.openRequests || 0, icon: ClipboardList, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: 'Pending Proposals', value: extra.pendingProposals, icon: FileText, iconColor: 'text-indigo-600', bgColor: 'bg-indigo-50' },
    { title: 'Deliverables Awaiting', value: extra.deliverablesAwaiting, icon: Package, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: 'Revenue', value: formatCurrency(data.revenue || 0), icon: DollarSign, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: 'Outstanding AR', value: formatCurrency(data.outstandingAmount || 0), icon: AlertCircle, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    { title: 'Completed', value: data.completedEngagements || 0, icon: CheckCircle, iconColor: 'text-slate-600', bgColor: 'bg-slate-50' },
  ] : [];

  return (
    <EnterprisePage
      title="Consultancy"
      subtitle="Service catalog, requests, proposals, engagements, and collections — live ledger data"
      breadcrumbs={['Consultancy']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      stats={stats}
      actions={
        <>
          <Button variant="outline" size="sm" onClick={() => navigate('/consultancy/requests')}>Requests</Button>
          <Button size="sm" onClick={() => navigate('/consultancy/engagements')}>Engagements</Button>
        </>
      }
    >
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Quick Links</CardTitle>
            <CardDescription>Navigate consultancy workflows</CardDescription>
          </CardHeader>
          <CardContent className="grid grid-cols-2 gap-3">
            {[
              ['Services', '/consultancy/services'],
              ['Requests', '/consultancy/requests'],
              ['Proposals', '/consultancy/proposals'],
              ['Engagements', '/consultancy/engagements'],
              ['Activities', '/consultancy/activities'],
              ['Contracts', '/consultancy/contracts'],
              ['Reports', '/consultancy/reports'],
            ].map(([label, path]) => (
              <Button key={path} variant="outline" className="justify-start" onClick={() => navigate(path)}>{label}</Button>
            ))}
          </CardContent>
        </Card>

        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Collections Snapshot</CardTitle>
            <CardDescription>Revenue from Finance INCOME (source CONSULTANCY)</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            {!data ? (
              <p className="text-sm text-slate-500 py-6 text-center">No data yet.</p>
            ) : (
              <>
                <div className="flex justify-between border-b py-2"><span>Recognized revenue</span><span className="font-mono font-bold">{formatCurrency(data.revenue || 0)}</span></div>
                <div className="flex justify-between border-b py-2"><span>Outstanding AR</span><span className="font-mono text-amber-700">{formatCurrency(data.outstandingAmount || 0)}</span></div>
                <div className="flex justify-between border-b py-2"><span>On hold</span><span className="font-mono">{data.onHoldEngagements || 0}</span></div>
                <div className="flex justify-between border-b py-2"><span>Total engagements</span><span className="font-mono">{data.totalEngagements || 0}</span></div>
                <p className="text-[11px] text-slate-400 pt-2">Cash receipts settle AR and are not counted again as revenue.</p>
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </EnterprisePage>
  );
};

export default ConsultancyDashboard;
