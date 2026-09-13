import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { crmApi } from '../../api';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import { formatDate } from '../../utils';
import { exportToCsv } from '../../lib/exportTable';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/ui/tabs';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Users, Calendar, Activity, Plus, Download, Building2, AlertTriangle } from 'lucide-react';
import { PERMISSIONS } from '../../lib/permissions';

const CrmPage = () => {
  const { t } = useLanguage();
  const { hasPermission, canPerformAction } = useAuth();
  const [clients, setClients] = useState([]);
  const [visits, setVisits] = useState([]);
  const [followUps, setFollowUps] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [clientsRes, visitsRes, followUpsRes] = await Promise.all([
        crmApi.getClients(),
        crmApi.getVisits({}),
        crmApi.getFollowUps(),
      ]);
      setClients(clientsRes.data.data || []);
      setVisits(visitsRes.data.data || []);
      setFollowUps(followUpsRes.data.data || []);
    } catch {
      setError('Failed to load CRM data.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const totalClients = clients.length;
  const activeClients = clients.filter((c) => c.status === 'ACTIVE').length;
  const totalBirds = clients.reduce((sum, c) => sum + (Number(c.numberOfBirds) || 0), 0);
  const upcomingFollowUps = followUps.filter((f) => new Date(f.nextFollowUpDate) >= new Date()).length;

  const kpiCards = [
    { title: 'Total Clients', value: totalClients, icon: Users, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: 'Active Clients', value: activeClients, icon: Activity, iconColor: 'text-green-600', bgColor: 'bg-green-50' },
    { title: 'Total Birds', value: totalBirds.toLocaleString(), icon: Building2, iconColor: 'text-purple-600', bgColor: 'bg-purple-50' },
    { title: 'Upcoming Follow-ups', value: upcomingFollowUps, icon: Calendar, iconColor: 'text-orange-600', bgColor: 'bg-orange-50' },
  ];

  const clientCols = [
    { accessorKey: 'clientName', header: t('crm.name') },
    { accessorKey: 'phone', header: t('crm.phone') },
    { accessorKey: 'location', header: t('crm.location') },
    { accessorKey: 'farmType', header: t('crm.farmType') },
    { accessorKey: 'numberOfBirds', header: t('crm.birds'), cell: ({ row }) => Number(row.original.numberOfBirds).toLocaleString() },
    { accessorKey: 'status', header: t('crm.status'), cell: ({ row }) => <StatusBadge status={row.original.status} /> },
    { accessorKey: 'assignedExtensionWorkerName', header: t('crm.extensionWorker') },
    {
      id: 'actions',
      header: 'Actions',
      cell: ({ row }) => (
        <div className="flex gap-2">
          <Button size="sm" variant="outline" onClick={() => navigate(`/crm/clients/${row.original.id}/edit`)}>
            Edit
          </Button>
          <Button size="sm" variant="outline" onClick={() => navigate('/crm/visits/new', { state: { clientId: row.original.id } })}>
            Visit
          </Button>
        </div>
      ),
    },
  ];

  const visitCols = [
    { accessorKey: 'clientName', header: t('crm.client') },
    { accessorKey: 'visitDate', header: t('crm.visitDate'), cell: ({ row }) => formatDate(row.original.visitDate) },
    { accessorKey: 'visitedBy', header: t('crm.visitedBy') },
    { accessorKey: 'purpose', header: t('crm.purpose') },
    { accessorKey: 'nextFollowUpDate', header: t('crm.nextFollowUp'), cell: ({ row }) => formatDate(row.original.nextFollowUpDate) },
  ];

  const exportCols = clientCols.filter((c) => c.id !== 'actions');

  return (
    <EnterprisePage
      title="CRM Management"
      subtitle="Manage client relationships, farm visits, and follow-up schedules"
      breadcrumbs={['CRM', 'Hub']}
      loading={loading}
      error={error}
      onRetry={fetchData}
      onRefresh={fetchData}
      stats={kpiCards}
      exportData={clients}
      exportColumns={exportCols}
      exportFilename="crm-clients"
      actions={
        <>
          {hasPermission(PERMISSIONS.MANAGE_CLIENTS) && (
            <Button size="sm" onClick={() => navigate('/crm/clients/new')}>
              <Plus className="mr-2 h-4 w-4" /> Add Client
            </Button>
          )}
          {canPerformAction('create', 'farmVisits') && (
            <Button size="sm" variant="outline" onClick={() => navigate('/crm/visits/new')}>
              <Activity className="mr-2 h-4 w-4" /> Record Visit
            </Button>
          )}
          <Button size="sm" variant="outline" onClick={() => exportToCsv(clients, exportCols, 'crm-clients')}>
            <Download className="mr-2 h-4 w-4" /> Export
          </Button>
        </>
      }
    >
      {upcomingFollowUps > 0 && (
        <Card className="border-orange-200 bg-orange-50">
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <AlertTriangle className="h-5 w-5 text-orange-600" />
              <div>
                <p className="font-semibold text-orange-900">Follow-ups Due</p>
                <p className="text-sm text-orange-700">{upcomingFollowUps} client follow-ups scheduled</p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>CRM Overview</CardTitle>
        </CardHeader>
        <CardContent>
          <Tabs defaultValue="clients" className="space-y-4">
            <TabsList>
              <TabsTrigger value="clients">
                <Users className="mr-2 h-4 w-4" />
                {t('crm.clients')}
                <Badge variant="secondary" className="ml-2">{clients.length}</Badge>
              </TabsTrigger>
              <TabsTrigger value="visits">
                <Activity className="mr-2 h-4 w-4" />
                {t('crm.farmVisits')}
                <Badge variant="secondary" className="ml-2">{visits.length}</Badge>
              </TabsTrigger>
              <TabsTrigger value="followups">
                <Calendar className="mr-2 h-4 w-4" />
                {t('crm.followups')}
                <Badge variant="warning" className="ml-2">{followUps.length}</Badge>
              </TabsTrigger>
            </TabsList>

            <TabsContent value="clients">
              <DataTable columns={clientCols} data={clients} searchable pagination />
            </TabsContent>

            <TabsContent value="visits">
              <DataTable columns={visitCols} data={visits} searchable pagination />
            </TabsContent>

            <TabsContent value="followups">
              <DataTable columns={visitCols} data={followUps} searchable pagination />
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>
    </EnterprisePage>
  );
};

export default CrmPage;
