import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { flockApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import { Button } from '../../components/ui/button';
import { Layers, TrendingDown, Activity, Plus } from 'lucide-react';
import { formatDate } from '../../utils';
import { toast } from 'react-toastify';

const FlockList = () => {
  const { t } = useLanguage();
  const { canPerformAction } = useAuth();
  const [flocks, setFlocks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const fetchFlocks = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const r = await flockApi.getAll();
      setFlocks(r.data.data || []);
    } catch (e) {
      setError('Failed to load flocks.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchFlocks(); }, [fetchFlocks]);

  const handleClose = async (id) => {
    try {
      await flockApi.close(id);
      setFlocks((prev) => prev.map((x) => (x.id === id ? { ...x, status: 'CLOSED' } : x)));
      toast.success('Flock closed');
    } catch {
      toast.error('Failed to close flock');
    }
  };

  const stats = useMemo(() => [
    { title: 'Total Flocks', value: flocks.length, icon: Layers, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: 'Active', value: flocks.filter((f) => f.status === 'ACTIVE').length, icon: Activity, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: 'Total Chickens', value: flocks.reduce((s, f) => s + (Number(f.currentBirdCount) || 0), 0).toLocaleString(), icon: Activity, iconColor: 'text-purple-600', bgColor: 'bg-purple-50' },
    { title: 'Closed', value: flocks.filter((f) => f.status === 'CLOSED').length, icon: TrendingDown, iconColor: 'text-gray-600', bgColor: 'bg-gray-50' },
  ], [flocks]);

  const columns = useMemo(() => [
    { accessorKey: 'batchCode', header: t('flocks.batchCode') },
    { accessorKey: 'farmName', header: t('flocks.farm') },
    { accessorKey: 'birdType', header: t('flocks.birdType') },
    { accessorKey: 'breed', header: 'Breed', cell: ({ row }) => row.original.breed || '—' },
    { accessorKey: 'initialBirdCount', header: t('flocks.initialCount'), cell: ({ row }) => Number(row.original.initialBirdCount || 0).toLocaleString() },
    { accessorKey: 'currentBirdCount', header: t('flocks.currentCount'), cell: ({ row }) => Number(row.original.currentBirdCount || 0).toLocaleString() },
    { accessorKey: 'startDate', header: t('flocks.startDate'), cell: ({ row }) => formatDate(row.original.startDate) },
    { accessorKey: 'status', header: t('flocks.status'), cell: ({ row }) => <StatusBadge status={row.original.status} /> },
    {
      id: 'actions',
      cell: ({ row }) => (
        <div className="flex gap-2 flex-wrap">
          <Button size="sm" variant="outline" onClick={() => navigate(`/vet/flock-emr/${row.original.id}`)}>EMR</Button>
          {canPerformAction('edit', 'flocks') && (
            <Button size="sm" variant="outline" onClick={() => navigate(`/flocks/${row.original.id}/edit`)}>Edit</Button>
          )}
          {row.original.status === 'ACTIVE' && canPerformAction('edit', 'flocks') && (
            <Button size="sm" variant="outline" onClick={() => handleClose(row.original.id)}>Close</Button>
          )}
        </div>
      ),
    },
  ], [t, navigate, canPerformAction]);

  const exportColumns = [
    { header: 'Batch', accessorKey: 'batchCode' },
    { header: 'Farm', accessorKey: 'farmName' },
    { header: 'Bird Type', accessorKey: 'birdType' },
    { header: 'Current Count', accessorKey: 'currentBirdCount' },
    { header: 'Status', accessorKey: 'status' },
  ];

  return (
    <EnterprisePage
      title="Flock Management"
      subtitle="Batch tracking, bird counts, breeds, and clinical EMR access"
      breadcrumbs={['Farm', 'Flocks']}
      loading={loading}
      error={error}
      onRetry={fetchFlocks}
      stats={stats}
      onRefresh={fetchFlocks}
      exportData={flocks}
      exportColumns={exportColumns}
      exportFilename="flocks"
      exportElementId="flocks-table"
      exportTitle="Flock Registry"
      empty={!flocks.length ? { title: 'No flocks', description: 'Create a flock batch to start health and production tracking.' } : null}
      actions={canPerformAction('create', 'flocks') && (
        <Button onClick={() => navigate('/flocks/new')} className="h-11 bg-primary hover:bg-primary/90 rounded-xl">
          <Plus className="mr-2 h-4 w-4" /> Add Flock
        </Button>
      )}
    >
      <div id="flocks-table">
        <DataTable columns={columns} data={flocks} searchable pagination pageSize={15} />
      </div>
    </EnterprisePage>
  );
};

export default FlockList;
