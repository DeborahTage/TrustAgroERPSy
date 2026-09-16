import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { crmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatDate } from '../../utils';
import { toast } from 'react-toastify';

const STATUSES = ['NEW', 'CONTACTED', 'QUALIFIED', 'UNQUALIFIED', 'CONVERTED', 'LOST'];

const CrmLeadDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [lead, setLead] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [status, setStatus] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await crmApi.getLead(id);
      setLead(res.data.data);
      setStatus(res.data.data?.status || '');
    } catch {
      setError('Failed to load lead.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const changeStatus = async () => {
    try {
      await crmApi.changeLeadStatus(id, { status });
      toast.success('Status updated');
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Status change failed');
    }
  };

  return (
    <EnterprisePage
      title={lead?.leadNumber || 'Lead'}
      subtitle={lead ? `${lead.name}${lead.company ? ` · ${lead.company}` : ''}` : 'Lead detail'}
      breadcrumbs={['CRM', 'Leads', lead?.leadNumber || id]}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={
        <>
          <Button variant="outline" size="sm" onClick={() => navigate('/crm/leads')}>Back</Button>
          {hasPermission(PERMISSIONS.CRM_LEAD_CONVERT) && lead && !['CONVERTED', 'LOST', 'UNQUALIFIED'].includes(lead.status) && (
            <Button size="sm" onClick={() => navigate('/crm/leads', { state: { convertId: lead.id } })}>Convert</Button>
          )}
        </>
      }
    >
      {lead && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle className="text-base">Details</CardTitle></CardHeader>
            <CardContent className="space-y-2 text-sm">
              {[
                ['Status', <Badge key="s" variant="outline">{lead.status}</Badge>],
                ['Source', lead.source],
                ['Priority', lead.priority],
                ['Phone', lead.phone],
                ['Email', lead.email],
                ['Contact', lead.contactPerson],
                ['Industry', lead.industry],
                ['Owner', lead.assignedOwnerName],
                ['Created', formatDate(lead.createdAt)],
              ].map(([label, value]) => (
                <div key={label} className="flex justify-between border-b py-2 gap-4">
                  <span className="text-slate-500">{label}</span>
                  <span className="text-right font-medium">{value || '—'}</span>
                </div>
              ))}
              {lead.notes && <p className="pt-2 text-slate-600 whitespace-pre-wrap">{lead.notes}</p>}
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle className="text-base">Lifecycle</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              {hasPermission(PERMISSIONS.CRM_LEAD) && !['CONVERTED'].includes(lead.status) && (
                <div className="flex flex-wrap items-end gap-3">
                  <div className="space-y-1 flex-1 min-w-[160px]">
                    <Select value={status} onValueChange={setStatus}>
                      <SelectTrigger><SelectValue /></SelectTrigger>
                      <SelectContent>
                        {STATUSES.filter((s) => s !== 'CONVERTED').map((s) => (
                          <SelectItem key={s} value={s}>{s}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <Button size="sm" onClick={changeStatus} disabled={status === lead.status}>Update Status</Button>
                </div>
              )}
              {lead.convertedClientId && (
                <Button variant="outline" className="w-full justify-start" onClick={() => navigate(`/crm/clients/${lead.convertedClientId}`)}>
                  Open converted customer #{lead.convertedClientId}
                </Button>
              )}
              {lead.convertedOpportunityId && (
                <Button variant="outline" className="w-full justify-start" onClick={() => navigate(`/crm/opportunities/${lead.convertedOpportunityId}`)}>
                  Open opportunity #{lead.convertedOpportunityId}
                </Button>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </EnterprisePage>
  );
};

export default CrmLeadDetail;
