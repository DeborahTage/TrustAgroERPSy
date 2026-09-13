import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { dailyRecordApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import { Button } from '../../components/ui/button';
import { FileText, TrendingDown, Activity, Plus, Egg } from 'lucide-react';
import { formatDate } from '../../utils';

const DailyFarmRecordList = () => {
  const { t } = useLanguage();
  const { canPerformAction } = useAuth();
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const fetchRecords = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const r = await dailyRecordApi.getAll();
      setRecords(r.data.data || []);
    } catch (e) {
      setError('Failed to load daily records.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchRecords(); }, [fetchRecords]);

  const today = new Date().toISOString().split('T')[0];

  const stats = useMemo(() => [
    { title: 'Total Records', value: records.length, icon: FileText, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: "Today's Records", value: records.filter((r) => r.date === today).length, icon: Activity, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: 'Total Mortality', value: records.reduce((s, r) => s + (Number(r.mortality) || 0), 0), icon: TrendingDown, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    { title: 'Total Eggs', value: records.reduce((s, r) => s + (Number(r.eggProduction) || 0), 0).toLocaleString(), icon: Egg, iconColor: 'text-purple-600', bgColor: 'bg-purple-50' },
  ], [records, today]);

  const columns = useMemo(() => [
    { accessorKey: 'date', header: t('dailyRecordList.date'), cell: ({ row }) => formatDate(row.original.date) },
    { accessorKey: 'farmName', header: t('dailyRecordList.farm') },
    { accessorKey: 'batchCode', header: t('dailyRecordList.batch') },
    { accessorKey: 'openingBirdCount', header: t('dailyRecordList.opening'), cell: ({ row }) => Number(row.original.openingBirdCount || 0).toLocaleString() },
    { accessorKey: 'mortality', header: t('dailyRecordList.mortality') },
    { accessorKey: 'feedConsumed', header: t('dailyRecordList.feed'), cell: ({ row }) => `${row.original.feedConsumed || 0} kg` },
    { accessorKey: 'eggProduction', header: t('dailyRecordList.eggs') },
    { accessorKey: 'mortalityRate', header: t('dailyRecordList.mortalityRate'), cell: ({ row }) => row.original.mortalityRate ? `${row.original.mortalityRate.toFixed(2)}%` : '—' },
    { accessorKey: 'recordedBy', header: t('dailyRecordList.recordedBy') },
    {
      id: 'actions',
      cell: ({ row }) => (
        <div className="flex gap-2 flex-wrap">
          {canPerformAction('edit', 'dailyRecords') && (
            <Button size="sm" variant="outline" onClick={() => navigate(`/daily-records/${row.original.id}/edit`)}>Edit</Button>
          )}
          {(row.original.mortality > 0 || row.original.symptomsOrRemarks) && (
            <Button size="sm" variant="outline" onClick={() => navigate('/veterinary/health-reports/new', { state: { dailyRecord: row.original } })}>
              Health Report
            </Button>
          )}
        </div>
      ),
    },
  ], [t, navigate, canPerformAction]);

  const exportColumns = [
    { header: 'Date', accessorKey: 'date' },
    { header: 'Farm', accessorKey: 'farmName' },
    { header: 'Batch', accessorKey: 'batchCode' },
    { header: 'Mortality', accessorKey: 'mortality' },
    { header: 'Eggs', accessorKey: 'eggProduction' },
  ];

  return (
    <EnterprisePage
      title="Daily Farm Records"
      subtitle="Mortality, feed consumption, egg production, and flock performance logs"
      breadcrumbs={['Farm', 'Daily Records']}
      loading={loading}
      error={error}
      onRetry={fetchRecords}
      stats={stats}
      onRefresh={fetchRecords}
      exportData={records}
      exportColumns={exportColumns}
      exportFilename="daily-records"
      exportElementId="daily-records-table"
      exportTitle="Daily Farm Records"
      empty={!records.length ? { title: 'No daily records', description: 'Record today\'s farm operations to track production and mortality.' } : null}
      actions={canPerformAction('create', 'dailyRecords') && (
        <Button onClick={() => navigate('/daily-records/new')} className="h-11 bg-primary hover:bg-primary/90 rounded-xl">
          <Plus className="mr-2 h-4 w-4" /> New Record
        </Button>
      )}
    >
      <div id="daily-records-table">
        <DataTable columns={columns} data={records} searchable pagination pageSize={15} />
      </div>
    </EnterprisePage>
  );
};

export default DailyFarmRecordList;
