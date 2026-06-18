import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { crmApi } from '../../api';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import PageHeader from '../../components/shared/PageHeader';
import StatsCard from '../../components/shared/StatsCard';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import { formatDate } from '../../utils';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/ui/tabs';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Users, Calendar, Activity, Plus, Download, FileText, Building2, AlertTriangle } from 'lucide-react';
import { PERMISSIONS } from '../../lib/permissions';

const CrmPage = () => {
  const { t } = useLanguage();
  const { hasPermission, canPerformAction } = useAuth();
  const [clients, setClients] = useState([]);
  const [visits, setVisits] = useState([]);
  const [followUps, setFollowUps] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    Promise.all([crmApi.getClients(), crmApi.getVisits({}), crmApi.getFollowUps()])
      .then(([clientsRes, visitsRes, followUpsRes]) => {
        setClients(clientsRes.data.data || []);
        setVisits(visitsRes.data.data || []);
        setFollowUps(followUpsRes.data.data || []);
      })
      .finally(() => setLoading(false));
  }, []);

  // Calculate analytics
  const totalClients = clients.length;
  const activeClients = clients.filter(c => c.status === 'ACTIVE').length;
  const totalBirds = clients.reduce((sum, c) => sum + (Number(c.numberOfBirds) || 0), 0);
  const upcomingFollowUps = followUps.filter(f => new Date(f.nextFollowUpDate) >= new Date()).length;

  const kpiCards = [
    {
      title: 'Total Clients',
      value: totalClients,
      icon: Users,
      iconColor: 'text-blue-600',
      bgColor: 'bg-blue-50'
    },
    {
      title: 'Active Clients',
      value: activeClients,
      icon: Activity,
      iconColor: 'text-green-600',
      bgColor: 'bg-green-50'
    },
    {
      title: 'Total Birds',
      value: totalBirds.toLocaleString(),
      icon: Building2,
      iconColor: 'text-purple-600',
      bgColor: 'bg-purple-50'
    },
    {
      title: 'Upcoming Follow-ups',
      value: upcomingFollowUps,
      icon: Calendar,
      iconColor: 'text-orange-600',
      bgColor: 'bg-orange-50'
    }
  ];

  const actions = [
    {
      label: 'Add Client',
      icon: Plus,
      onClick: () => navigate('/crm/clients/new'),
      show: hasPermission(PERMISSIONS.MANAGE_CLIENTS)
    },
    {
      label: 'Record Visit',
      icon: Activity,
      onClick: () => navigate('/crm/visits/new'),
      show: canPerformAction('create', 'farmVisits')
    },
    {
      label: 'Export',
      icon: Download,
      onClick: () => console.log('Export CRM data'),
      show: true
    },
    {
      label: 'Generate Report',
      icon: FileText,
      onClick: () => console.log('Generate report'),
      show: true
    }
  ].filter(a => a.show);

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
      cell: ({ row }) => (
        <div className="flex gap-2">
          <Button size="sm" variant="outline" onClick={() => navigate(`/crm/clients/${row.original.id}/edit`)}>
            Edit
          </Button>
          <Button size="sm" variant="outline" onClick={() => navigate('/crm/visits/new', { state: { clientId: row.original.id } })}>
            Visit
          </Button>
        </div>
      )
    },
  ];

  const visitCols = [
    { accessorKey: 'clientName', header: t('crm.client') },
    { accessorKey: 'visitDate', header: t('crm.visitDate'), cell: ({ row }) => formatDate(row.original.visitDate) },
    { accessorKey: 'visitedBy', header: t('crm.visitedBy') },
    { accessorKey: 'purpose', header: t('crm.purpose') },
    { accessorKey: 'nextFollowUpDate', header: t('crm.nextFollowUp'), cell: ({ row }) => formatDate(row.original.nextFollowUpDate) },
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <PageHeader
        title="CRM Management"
        description="Manage client relationships, lead pipeline, and follow-up schedules."
        actions={actions}
      />

      {/* KPI Section */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {kpiCards.map((card, index) => (
          <StatsCard key={index} {...card} />
        ))}
      </div>

      {/* Alert Banner */}
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

      {/* Main Content */}
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
              <DataTable columns={clientCols} data={clients} loading={loading} searchable pagination />
            </TabsContent>

            <TabsContent value="visits">
              <DataTable columns={visitCols} data={visits} loading={loading} searchable pagination />
            </TabsContent>

            <TabsContent value="followups">
              <DataTable columns={visitCols} data={followUps} loading={loading} searchable pagination />
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>
    </div>
  );
};

export default CrmPage;
