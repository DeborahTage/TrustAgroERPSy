import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { crmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../../components/ui/dialog';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatCurrency, formatDate } from '../../utils';
import { toast } from 'react-toastify';

const STAGES = ['PROSPECTING', 'QUALIFICATION', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST'];

const CrmOpportunityDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [opp, setOpp] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [stageOpen, setStageOpen] = useState(false);
  const [stage, setStage] = useState('');
  const [lossReason, setLossReason] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await crmApi.getOpportunity(id);
      setOpp(res.data.data);
      setStage(res.data.data?.stage || '');
    } catch {
      setError('Failed to load opportunity.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const changeStage = async (e) => {
    e.preventDefault();
    try {
      await crmApi.changeOpportunityStage(id, {
        stage,
        lossReason: stage === 'LOST' ? (lossReason || undefined) : undefined,
      });
      toast.success('Stage updated');
      setStageOpen(false);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Stage change failed');
    }
  };

  return (
    <EnterprisePage
      title={opp?.opportunityNumber || 'Opportunity'}
      subtitle={opp?.title || 'Opportunity detail'}
      breadcrumbs={['CRM', 'Opportunities', opp?.opportunityNumber || id]}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={
        <>
          <Button variant="outline" size="sm" onClick={() => navigate('/crm/opportunities')}>Back</Button>
          {hasPermission(PERMISSIONS.CRM_OPPORTUNITY) && opp && !['WON', 'LOST'].includes(opp.stage) && (
            <Button size="sm" onClick={() => { setStage(opp.stage); setStageOpen(true); }}>Change Stage</Button>
          )}
          {hasPermission(PERMISSIONS.CRM_QUOTATION) && opp?.clientId && (
            <Button size="sm" variant="outline" onClick={() => navigate('/crm/quotations')}>Quotations</Button>
          )}
        </>
      }
    >
      {opp && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle className="text-base">Details</CardTitle></CardHeader>
            <CardContent className="space-y-2 text-sm">
              {[
                ['Stage', <Badge key="s" variant="outline">{opp.stage}</Badge>],
                ['Customer', opp.clientName],
                ['Value', formatCurrency(opp.estimatedValue)],
                ['Probability', opp.probability != null ? `${opp.probability}%` : '—'],
                ['Expected Close', formatDate(opp.expectedCloseDate)],
                ['Owner', opp.salesOwnerName],
                ['Priority', opp.priority],
                ['Source', opp.source],
              ].map(([label, value]) => (
                <div key={label} className="flex justify-between border-b py-2 gap-4">
                  <span className="text-slate-500">{label}</span>
                  <span className="text-right font-medium">{value || '—'}</span>
                </div>
              ))}
              {opp.description && <p className="pt-2 text-slate-600 whitespace-pre-wrap">{opp.description}</p>}
              {opp.lossReason && <p className="pt-2 text-rose-600">Loss reason: {opp.lossReason}</p>}
            </CardContent>
          </Card>
          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle className="text-base">Links</CardTitle></CardHeader>
            <CardContent className="space-y-2">
              {opp.clientId && (
                <Button variant="outline" className="w-full justify-start" onClick={() => navigate(`/crm/clients/${opp.clientId}`)}>
                  Customer 360
                </Button>
              )}
              {opp.leadId && (
                <Button variant="outline" className="w-full justify-start" onClick={() => navigate(`/crm/leads/${opp.leadId}`)}>
                  Source Lead
                </Button>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      <Dialog open={stageOpen} onOpenChange={setStageOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Change Stage</DialogTitle></DialogHeader>
          <form onSubmit={changeStage} className="space-y-3">
            <div className="space-y-1">
              <Label>Stage</Label>
              <Select value={stage} onValueChange={setStage}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>{STAGES.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}</SelectContent>
              </Select>
            </div>
            {stage === 'LOST' && (
              <div className="space-y-1">
                <Label>Loss Reason</Label>
                <Input value={lossReason} onChange={(e) => setLossReason(e.target.value)} />
              </div>
            )}
            <DialogFooter><Button type="submit">Save</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default CrmOpportunityDetail;
