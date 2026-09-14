import React, { useEffect, useState, useCallback } from 'react';
import { crmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Users, Target, Activity, Calendar } from 'lucide-react';

const STAGES = ['LEAD', 'ACTIVE', 'INACTIVE', 'LOST'];

const CrmPipeline = () => {
  const [pipeline, setPipeline] = useState({});
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await crmApi.getPipeline();
      setPipeline(res.data.data || {});
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const total = Object.values(pipeline).reduce((s, v) => s + Number(v), 0);

  return (
    <EnterprisePage
      title="Sales Pipeline"
      subtitle="Kanban-style view of customer lifecycle stages"
      breadcrumbs={['CRM', 'Pipeline']}
      loading={loading}
      onRefresh={load}
      stats={[
        { title: 'Total Records', value: total, icon: Users, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
        { title: 'Leads', value: pipeline.LEAD ?? 0, icon: Target, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
        { title: 'Active', value: pipeline.ACTIVE ?? 0, icon: Activity, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
        { title: 'Follow-up Due', value: pipeline.LEAD ?? 0, icon: Calendar, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
      ]}
    >
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {STAGES.map((stage) => (
          <Card key={stage}>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold">{stage.replace('_', ' ')}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-3xl font-bold text-foreground">{pipeline[stage] ?? 0}</p>
              <p className="text-xs text-muted-foreground mt-1">
                {total > 0 ? `${Math.round(((pipeline[stage] ?? 0) / total) * 100)}% of pipeline` : 'No data'}
              </p>
            </CardContent>
          </Card>
        ))}
      </div>
    </EnterprisePage>
  );
};

export default CrmPipeline;
