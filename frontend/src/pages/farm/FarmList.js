import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { farmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import { Button } from '../../components/ui/button';
import { Building2, MapPin, Users, Activity, Plus } from 'lucide-react';

const FarmList = () => {
  const { t } = useLanguage();
  const { canPerformAction } = useAuth();
  const [farms, setFarms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const fetchFarms = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const r = await farmApi.getAll();
      setFarms(r.data.data || []);
    } catch (e) {
      setError('Failed to load farms.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchFarms(); }, [fetchFarms]);

  const stats = useMemo(() => [
    { title: 'Total Farms', value: farms.length, icon: Building2, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: 'Active', value: farms.filter((f) => f.status === 'ACTIVE').length, icon: Activity, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: 'Capacity', value: `${farms.reduce((s, f) => s + (Number(f.capacity) || 0), 0).toLocaleString()} birds`, icon: Users, iconColor: 'text-purple-600', bgColor: 'bg-purple-50' },
    { title: 'Active Flocks', value: farms.reduce((s, f) => s + (f.activeFlocks || 0), 0), icon: MapPin, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
  ], [farms]);

  const columns = useMemo(() => [
    { accessorKey: 'farmName', header: t('farms.farmName') },
    { accessorKey: 'location', header: t('farms.location') },
    { accessorKey: 'farmType', header: t('farms.type') },
    { accessorKey: 'capacity', header: t('farms.capacity'), cell: ({ row }) => `${Number(row.original.capacity || 0).toLocaleString()} birds` },
    { accessorKey: 'assignedFarmManagerName', header: t('farms.manager') },
    { accessorKey: 'status', header: t('farms.status'), cell: ({ row }) => <StatusBadge status={row.original.status} /> },
    {
      id: 'actions',
      cell: ({ row }) => (
        <div className="flex gap-2">
          {canPerformAction('edit', 'farms') && (
            <Button size="sm" variant="outline" onClick={() => navigate(`/farms/${row.original.id}/edit`)}>Edit</Button>
          )}
        </div>
      ),
    },
  ], [t, navigate, canPerformAction]);

  const exportColumns = [
    { header: 'Farm', accessorKey: 'farmName' },
    { header: 'Location', accessorKey: 'location' },
    { header: 'Type', accessorKey: 'farmType' },
    { header: 'Capacity', accessorKey: 'capacity' },
    { header: 'Manager', accessorKey: 'assignedFarmManagerName' },
    { header: 'Status', accessorKey: 'status' },
  ];

  return (
    <EnterprisePage
      title="Farm Management"
      subtitle="Farm profiles, capacity, managers, and operational status"
      breadcrumbs={['Farm', 'Farms']}
      loading={loading}
      error={error}
      onRetry={fetchFarms}
      stats={stats}
      onRefresh={fetchFarms}
      exportData={farms}
      exportColumns={exportColumns}
      exportFilename="farms"
      exportElementId="farms-table"
      exportTitle="Farm Registry"
      empty={!farms.length ? { title: 'No farms registered', description: 'Add your first farm to begin production tracking.' } : null}
      actions={canPerformAction('create', 'farms') && (
        <Button onClick={() => navigate('/farms/new')} className="h-11 bg-primary hover:bg-primary/90 rounded-xl">
          <Plus className="mr-2 h-4 w-4" /> Add Farm
        </Button>
      )}
    >
      <div id="farms-table">
        <DataTable columns={columns} data={farms} searchable pagination pageSize={15} />
      </div>
    </EnterprisePage>
  );
};

export default FarmList;
