import React, { useEffect, useState, useCallback } from 'react';
import { crmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Users, Target, Activity, TrendingUp } from 'lucide-react';
import { formatCurrency } from '../../utils';

const CLIENT_STAGES = ['LEAD', 'ACTIVE', 'INACTIVE', 'LOST'];
const OPP_STAGES = ['PROSPECTING', 'QUALIFICATION', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST'];

const CrmPipeline = () => {
  const [pipeline, setPipeline] = useState({});
  const [oppByStage, setOppByStage] = useState(null);
  const [openValue, setOpenValue] = useState(0);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [pipeRes, dashRes] = await Promise.all([
        crmApi.getPipeline(),
        crmApi.getDashboard().catch(() => ({ data: { data: {} } })),
      ]);
      setPipeline(pipeRes.data.data || {});
      const dash = dashRes.data?.data || {};
      setOppByStage(dash.opportunitiesByStage || null);
      setOpenValue(Number(dash.openOpportunityValue) || 0);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const useOppStages = oppByStage && Object.keys(oppByStage).length > 0;
  const stages = useOppStages ? OPP_STAGES : CLIENT_STAGES;
  const counts = useOppStages ? oppByStage : pipeline;
  const total = Object.values(counts || {}).reduce((s, v) => s + Number(v || 0), 0);

  return (
    <EnterprisePage
      title="Sales Pipeline"
      subtitle={useOppStages
        ? 'Opportunity stages from commercial dashboard'
        : 'Client status pipeline (opportunity stages unavailable)'}
      breadcrumbs={['CRM', 'Pipeline']}
      loading={loading}
      onRefresh={load}
      stats={[
        { title: 'Total Records', value: total, icon: Users, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
        { title: useOppStages ? 'Prospecting' : 'Leads', value: (counts.PROSPECTING ?? counts.LEAD) ?? 0, icon: Target, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
        { title: useOppStages ? 'Won' : 'Active', value: (counts.WON ?? counts.ACTIVE) ?? 0, icon: Activity, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
        { title: 'Open Opp. Value', value: formatCurrency(openValue), icon: TrendingUp, iconColor: 'text-indigo-600', bgColor: 'bg-indigo-50' },
      ]}
    >
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        {stages.map((stage) => (
          <Card key={stage}>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold">{stage.replace(/_/g, ' ')}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-3xl font-bold text-foreground">{counts[stage] ?? 0}</p>
              <p className="text-xs text-muted-foreground mt-1">
                {total > 0 ? `${Math.round(((counts[stage] ?? 0) / total) * 100)}% of pipeline` : 'No data'}
              </p>
            </CardContent>
          </Card>
        ))}
      </div>
      {!useOppStages && Object.keys(pipeline).length > 0 && (
        <p className="text-xs text-slate-500 mt-4">
          Showing client status counts. Opportunity stage breakdown will appear when commercial data is available.
        </p>
      )}
    </EnterprisePage>
  );
};

export default CrmPipeline;
