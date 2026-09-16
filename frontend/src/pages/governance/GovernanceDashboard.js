import React, { useEffect, useState } from 'react';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { governanceApi } from '../../api';
import { Loader2, RefreshCw, ShieldAlert } from 'lucide-react';
import { toast } from 'react-toastify';

const statusColor = (status) => {
  switch (status) {
    case 'OK': return 'bg-emerald-100 text-emerald-800';
    case 'WARNING': return 'bg-amber-100 text-amber-800';
    case 'ERROR': return 'bg-red-100 text-red-800';
    default: return 'bg-slate-100 text-slate-700';
  }
};

const GovernanceDashboard = () => {
  const [dashboard, setDashboard] = useState(null);
  const [controls, setControls] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const [dashRes, ctrlRes] = await Promise.all([
        governanceApi.dashboard(),
        governanceApi.controls(),
      ]);
      setDashboard(dashRes.data?.data || null);
      setControls(ctrlRes.data?.data || []);
    } catch {
      toast.error('Failed to load governance metrics');
      setDashboard(null);
      setControls([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const metrics = [
    { label: 'Audit events today', value: dashboard?.auditToday ?? '—' },
    { label: 'Failed logins (24h)', value: dashboard?.failedLogins24h ?? '—' },
    { label: 'Pending approvals', value: dashboard?.pendingApprovals ?? '—' },
    { label: 'Control errors', value: dashboard?.controlErrorCount ?? '—' },
  ];

  return (
    <EnterprisePage
      title="Enterprise Governance"
      subtitle="Control checks, pending approvals, and high-risk audit activity"
      breadcrumbs={['Governance']}
      actions={
        <Button variant="outline" onClick={load} className="gap-2" disabled={loading}>
          {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <RefreshCw className="w-4 h-4" />}
          Refresh
        </Button>
      }
    >
      {loading && !dashboard ? (
        <div className="flex justify-center py-16 text-gray-500">
          <Loader2 className="w-6 h-6 animate-spin" />
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
            {metrics.map((m) => (
              <Card key={m.label}>
                <CardContent className="pt-6">
                  <div className="text-xs uppercase tracking-wide text-gray-500">{m.label}</div>
                  <div className="text-2xl font-semibold mt-1">{m.value}</div>
                </CardContent>
              </Card>
            ))}
          </div>

          {dashboard?.pendingApprovalsDetail && (
            <Card className="mb-6">
              <CardHeader>
                <CardTitle className="text-base">Pending approvals breakdown</CardTitle>
              </CardHeader>
              <CardContent className="flex flex-wrap gap-4 text-sm">
                <span>Procurement: <strong>{dashboard.pendingApprovalsDetail.procurement}</strong></span>
                <span>Leave: <strong>{dashboard.pendingApprovalsDetail.leave}</strong></span>
                <span>Payroll: <strong>{dashboard.pendingApprovalsDetail.payroll}</strong></span>
              </CardContent>
            </Card>
          )}

          <Card className="mb-6">
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <ShieldAlert className="w-4 h-4" /> Control checks
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="text-left text-gray-500 border-b">
                    <tr>
                      <th className="py-2 pr-4">Check</th>
                      <th className="py-2 pr-4">Status</th>
                      <th className="py-2 pr-4">Count</th>
                      <th className="py-2">Message</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {controls.map((c) => (
                      <tr key={c.code}>
                        <td className="py-3 pr-4 font-medium">{c.title}</td>
                        <td className="py-3 pr-4">
                          <span className={`px-2 py-0.5 rounded text-xs font-semibold ${statusColor(c.status)}`}>
                            {c.status}
                          </span>
                        </td>
                        <td className="py-3 pr-4">{c.count}</td>
                        <td className="py-3 text-gray-600">{c.message}</td>
                      </tr>
                    ))}
                    {controls.length === 0 && (
                      <tr>
                        <td colSpan="4" className="py-6 text-center text-gray-500">No control results</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Recent high-risk audit actions</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                {(dashboard?.recentHighRisk || []).map((a) => (
                  <div key={a.id} className="flex flex-wrap items-center justify-between gap-2 border-b border-gray-100 py-2 text-sm">
                    <div>
                      <Badge variant="outline">{a.action}</Badge>
                      <span className="ml-2 text-gray-700">{a.module}</span>
                      <span className="ml-2 text-gray-400">{a.userEmail}</span>
                    </div>
                    <span className="text-xs text-gray-500">
                      {a.createdAt ? new Date(a.createdAt).toLocaleString() : ''}
                    </span>
                  </div>
                ))}
                {(!dashboard?.recentHighRisk || dashboard.recentHighRisk.length === 0) && (
                  <div className="text-sm text-gray-500 py-4">No high-risk events</div>
                )}
              </div>
            </CardContent>
          </Card>
        </>
      )}
    </EnterprisePage>
  );
};

export default GovernanceDashboard;
