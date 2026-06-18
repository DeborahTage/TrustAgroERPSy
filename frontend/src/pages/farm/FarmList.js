import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { farmApi } from '../../api';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import PageHeader from '../../components/shared/PageHeader';
import StatsCard from '../../components/shared/StatsCard';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Building2, MapPin, Users, Activity, Plus, Download, FileText } from 'lucide-react';

const FarmList = () => {
  const { t } = useLanguage();
  const { canPerformAction } = useAuth();
  const [farms, setFarms] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    farmApi.getAll().then(r => setFarms(r.data.data)).finally(() => setLoading(false));
  }, []);

  // Calculate analytics
  const totalFarms = farms.length;
  const activeFarms = farms.filter(f => f.status === 'ACTIVE').length;
  const totalCapacity = farms.reduce((sum, f) => sum + (Number(f.capacity) || 0), 0);
  const totalFlocks = farms.reduce((sum, f) => sum + (f.activeFlocks || 0), 0);

  const kpiCards = [
    {
      title: 'Total Farms',
      value: totalFarms,
      icon: Building2,
      iconColor: 'text-blue-600',
      bgColor: 'bg-blue-50'
    },
    {
      title: 'Active Farms',
      value: activeFarms,
      icon: Activity,
      iconColor: 'text-green-600',
      bgColor: 'bg-green-50'
    },
    {
      title: 'Total Capacity',
      value: `${totalCapacity.toLocaleString()} birds`,
      icon: Users,
      iconColor: 'text-purple-600',
      bgColor: 'bg-purple-50'
    },
    {
      title: 'Active Flocks',
      value: totalFlocks,
      icon: Activity,
      iconColor: 'text-orange-600',
      bgColor: 'bg-orange-50'
    }
  ];

  const columns = [
    { accessorKey: 'farmName', header: t('farms.farmName') },
    { accessorKey: 'location', header: t('farms.location') },
    { accessorKey: 'farmType', header: t('farms.type') },
    { accessorKey: 'capacity', header: t('farms.capacity'), cell: ({ row }) => `${Number(row.original.capacity).toLocaleString()} birds` },
    { accessorKey: 'assignedFarmManagerName', header: t('farms.manager') },
    { accessorKey: 'status', header: t('farms.status'), cell: ({ row }) => <StatusBadge status={row.original.status} /> },
    {
      id: 'actions',
      cell: ({ row }) => (
        <div className="flex gap-2">
          <Button
            size="sm"
            variant="outline"
            onClick={() => navigate(`/farms/${row.original.id}`)}
          >
            View
          </Button>
          {canPerformAction('edit', 'farms') && (
            <Button
              size="sm"
              variant="outline"
              onClick={() => navigate(`/farms/${row.original.id}/edit`)}
            >
              Edit
            </Button>
          )}
        </div>
      )
    },
  ];

  const actions = [
    {
      label: 'Add Farm',
      icon: Plus,
      onClick: () => navigate('/farms/new'),
      show: canPerformAction('create', 'farms')
    },
    {
      label: 'Export',
      icon: Download,
      onClick: () => console.log('Export farms'),
      show: true
    },
    {
      label: 'Generate Report',
      icon: FileText,
      onClick: () => console.log('Generate report'),
      show: true
    }
  ].filter(a => a.show);

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <PageHeader
        title="Farm Management"
        description="Manage farms, monitor capacity, and track farm performance."
        actions={actions}
      />

      {/* KPI Section */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {kpiCards.map((card, index) => (
          <StatsCard key={index} {...card} />
        ))}
      </div>

      {/* Main Content */}
      <Card>
        <CardHeader>
          <CardTitle>All Farms</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable columns={columns} data={farms} loading={loading} />
        </CardContent>
      </Card>
    </div>
  );
};

export default FarmList;
