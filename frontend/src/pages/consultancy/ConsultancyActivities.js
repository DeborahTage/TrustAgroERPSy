import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { consultancyApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { formatDate } from '../../utils';

const ConsultancyActivities = () => {
  const navigate = useNavigate();
  const [engagements, setEngagements] = useState([]);
  const [engagementId, setEngagementId] = useState('');
  const [activities, setActivities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [listLoading, setListLoading] = useState(false);
  const [error, setError] = useState(null);

  const loadEngagements = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await consultancyApi.getEngagements();
      setEngagements(res.data.data || []);
    } catch {
      setError('Failed to load engagements.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadEngagements(); }, [loadEngagements]);

  useEffect(() => {
    if (!engagementId) {
      setActivities([]);
      return;
    }
    let cancelled = false;
    (async () => {
      setListLoading(true);
      try {
        const res = await consultancyApi.getActivities(engagementId);
        if (!cancelled) setActivities(res.data.data || []);
      } catch {
        if (!cancelled) {
          setActivities([]);
          setError('Failed to load activities.');
        }
      } finally {
        if (!cancelled) setListLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [engagementId]);

  return (
    <EnterprisePage
      title="Consultancy Activities"
      subtitle="Select an engagement to view its activity log"
      breadcrumbs={['Consultancy', 'Activities']}
      loading={loading}
      error={error}
      onRetry={loadEngagements}
      onRefresh={loadEngagements}
      actions={
        <Button size="sm" variant="outline" onClick={() => navigate('/consultancy/engagements')}>All Engagements</Button>
      }
    >
      <div className="mb-4 max-w-md">
        <Select value={engagementId || 'none'} onValueChange={(v) => setEngagementId(v === 'none' ? '' : v)}>
          <SelectTrigger><SelectValue placeholder="Select engagement" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="none">Select engagement…</SelectItem>
            {engagements.map((e) => (
              <SelectItem key={e.id} value={String(e.id)}>
                {e.engagementNumber} — {e.title}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <Card className="border-none shadow-sm">
        <CardHeader>
          <CardTitle className="text-base">Activities</CardTitle>
          <CardDescription>
            {engagementId ? 'Logged work for the selected engagement' : 'Choose an engagement above'}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {!engagementId && (
            <p className="text-sm text-slate-500 text-center py-8">Select an engagement to list activities.</p>
          )}
          {engagementId && listLoading && (
            <p className="text-sm text-slate-500 text-center py-8">Loading…</p>
          )}
          {engagementId && !listLoading && activities.length === 0 && (
            <p className="text-sm text-slate-500 text-center py-8">No activities for this engagement.</p>
          )}
          {activities.map((a) => (
            <div key={a.id} className="flex flex-wrap justify-between gap-2 border rounded-lg p-3 text-sm">
              <div>
                <p className="font-semibold">{a.activityNumber} · {a.activityType}</p>
                <p className="text-xs text-slate-500">{formatDate(a.activityDate)} · {a.description}</p>
                {a.consultantName && <p className="text-xs text-slate-400">{a.consultantName}</p>}
              </div>
              <div className="flex items-center gap-2">
                <Badge variant="outline">{a.status}</Badge>
                <Button size="sm" variant="ghost" onClick={() => navigate(`/consultancy/engagements/${a.engagementId}`)}>Open</Button>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </EnterprisePage>
  );
};

export default ConsultancyActivities;
