import React, { useEffect, useState, useCallback } from 'react';
import { marketingApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import StatsCard from '../../components/shared/StatsCard';
import DataTable from '../../components/shared/DataTable';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Megaphone, Target, TrendingUp, DollarSign, Plus } from 'lucide-react';
import { toast } from 'react-toastify';

const MarketingPage = () => {
  const [stats, setStats] = useState({});
  const [campaigns, setCampaigns] = useState([]);
  const [leads, setLeads] = useState([]);
  const [loading, setLoading] = useState(true);
  const [newCampaign, setNewCampaign] = useState({ name: '', channel: 'WEBSITE', budget: '' });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [dash, camps, leadRows] = await Promise.all([
        marketingApi.getDashboard(),
        marketingApi.getCampaigns(),
        marketingApi.getLeads(),
      ]);
      setStats(dash.data.data || {});
      setCampaigns(camps.data.data || []);
      setLeads(leadRows.data.data || []);
    } catch {
      toast.error('Failed to load marketing data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleCreateCampaign = async () => {
    if (!newCampaign.name) return;
    try {
      await marketingApi.createCampaign({
        name: newCampaign.name,
        channel: newCampaign.channel,
        budget: Number(newCampaign.budget) || 0,
        status: 'ACTIVE',
      });
      setNewCampaign({ name: '', channel: 'WEBSITE', budget: '' });
      toast.success('Campaign created');
      load();
    } catch {
      toast.error('Failed to create campaign');
    }
  };

  const kpiCards = [
    { title: 'Active Campaigns', value: stats.activeCampaigns ?? 0, icon: Megaphone, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Total Leads', value: stats.totalLeads ?? 0, icon: Target, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: 'Open Leads', value: stats.openLeads ?? 0, icon: TrendingUp, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: 'Conversion Rate', value: `${stats.conversionRate ?? 0}%`, icon: DollarSign, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
  ];

  const campaignCols = [
    { accessorKey: 'name', header: 'Campaign' },
    { accessorKey: 'channel', header: 'Channel' },
    { accessorKey: 'status', header: 'Status' },
    { accessorKey: 'leadsGenerated', header: 'Leads' },
    { accessorKey: 'conversions', header: 'Conversions' },
    { accessorKey: 'budget', header: 'Budget' },
  ];

  const leadCols = [
    { accessorKey: 'fullName', header: 'Name' },
    { accessorKey: 'phone', header: 'Phone' },
    { accessorKey: 'source', header: 'Source' },
    { accessorKey: 'converted', header: 'Converted', cell: ({ row }) => row.original.converted ? 'Yes' : 'No' },
    {
      id: 'actions',
      header: 'Actions',
      cell: ({ row }) => !row.original.converted && (
        <Button size="sm" variant="outline" onClick={async () => {
          try {
            await marketingApi.convertLead(row.original.id);
            toast.success('Lead sent to CRM');
            load();
          } catch { toast.error('Conversion failed'); }
        }}>Send to CRM</Button>
      ),
    },
  ];

  return (
    <EnterprisePage
      title="Marketing Dashboard"
      subtitle="Campaign performance, lead generation, and ROI tracking"
      breadcrumbs={['Marketing', 'Dashboard']}
      loading={loading}
      onRefresh={load}
      stats={kpiCards}
    >
      <Card className="mb-6">
        <CardHeader><CardTitle className="text-base">Quick Campaign</CardTitle></CardHeader>
        <CardContent className="flex flex-wrap gap-3">
          <Input placeholder="Campaign name" value={newCampaign.name} onChange={(e) => setNewCampaign((c) => ({ ...c, name: e.target.value }))} className="max-w-xs" />
          <Input placeholder="Budget (ETB)" type="number" value={newCampaign.budget} onChange={(e) => setNewCampaign((c) => ({ ...c, budget: e.target.value }))} className="max-w-[140px]" />
          <Button onClick={handleCreateCampaign}><Plus className="h-4 w-4 mr-1" />Create Campaign</Button>
        </CardContent>
      </Card>
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <DataTable columns={campaignCols} data={campaigns} searchable pagination pageSize={5} emptyMessage="No campaigns yet" />
        <DataTable columns={leadCols} data={leads} searchable pagination pageSize={5} emptyMessage="No leads captured" />
      </div>
    </EnterprisePage>
  );
};

export default MarketingPage;
