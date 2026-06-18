import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { dailyRecordApi } from '../../api';
import DataTable from '../../components/shared/DataTable';
import PageHeader from '../../components/shared/PageHeader';
import StatsCard from '../../components/shared/StatsCard';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { FileText, TrendingDown, Activity, Plus, Download, FileText as FileIcon, AlertTriangle } from 'lucide-react';
import { formatDate } from '../../utils';

const DailyFarmRecordList = () => {
  const { t } = useLanguage();
  const { canPerformAction } = useAuth();
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    dailyRecordApi.getAll().then(r => setRecords(r.data.data)).finally(() => setLoading(false));
  }, []);

  // Calculate analytics
  const totalRecords = records.length;
  const todayRecords = records.filter(r => r.date === new Date().toISOString().split('T')[0]).length;
  const totalMortality = records.reduce((sum, r) => sum + (Number(r.mortality) || 0), 0);
  const totalEggs = records.reduce((sum, r) => sum + (Number(r.eggProduction) || 0), 0);
  const highMortalityRecords = records.filter(r => r.mortality > 10).length;

  const kpiCards = [
    {
      title: 'Total Records',
      value: totalRecords,
      icon: FileText,
      iconColor: 'text-blue-600',
      bgColor: 'bg-blue-50'
    },
    {
      title: "Today's Records",
      value: todayRecords,
      icon: Activity,
      iconColor: 'text-green-600',
      bgColor: 'bg-green-50'
    },
    {
      title: 'Total Mortality',
      value: totalMortality,
      icon: TrendingDown,
      iconColor: 'text-red-600',
      bgColor: 'bg-red-50'
    },
    {
      title: 'Total Eggs',
      value: totalEggs.toLocaleString(),
      icon: FileIcon,
      iconColor: 'text-purple-600',
      bgColor: 'bg-purple-50'
    }
  ];

  const columns = [
    { accessorKey: 'date', header: t('dailyRecordList.date'), cell: ({ row }) => formatDate(row.original.date) },
    { accessorKey: 'farmName', header: t('dailyRecordList.farm') },
    { accessorKey: 'batchCode', header: t('dailyRecordList.batch') },
    { accessorKey: 'openingBirdCount', header: t('dailyRecordList.opening'), cell: ({ row }) => Number(row.original.openingBirdCount).toLocaleString() },
    { accessorKey: 'mortality', header: t('dailyRecordList.mortality'), cell: ({ row }) => row.original.mortality || 0 },
    { accessorKey: 'feedConsumed', header: t('dailyRecordList.feed'), cell: ({ row }) => `${row.original.feedConsumed || 0} kg` },
    { accessorKey: 'eggProduction', header: t('dailyRecordList.eggs'), cell: ({ row }) => row.original.eggProduction || 0 },
    { accessorKey: 'mortalityRate', header: t('dailyRecordList.mortalityRate'), cell: ({ row }) => row.original.mortalityRate ? `${row.original.mortalityRate.toFixed(2)}%` : '-' },
    { accessorKey: 'recordedBy', header: t('dailyRecordList.recordedBy') },
    {
      id: 'actions',
      cell: ({ row }) => (
        <div className="flex gap-2">
          {canPerformAction('edit', 'dailyRecords') && (
          <Button
            size="sm"
            variant="outline"
            onClick={() => navigate(`/daily-records/${row.original.id}/edit`)}
          >
            Edit
          </Button>
          )}
          {(row.original.mortality > 0 || row.original.symptomsOrRemarks) && (
            <Button
              size="sm"
              variant="outline"
              onClick={() => navigate('/veterinary/health-reports/new', { state: { dailyRecord: row.original } })}
            >
              Health Report
            </Button>
          )}
        </div>
      )
    },
  ];

  const actions = [
    {
      label: 'Add Record',
      icon: Plus,
      onClick: () => navigate('/daily-records/new'),
      show: canPerformAction('create', 'dailyRecords')
    },
    {
      label: 'Export',
      icon: Download,
      onClick: () => console.log('Export records'),
      show: true
    },
    {
      label: 'Generate Report',
      icon: FileIcon,
      onClick: () => console.log('Generate report'),
      show: true
    }
  ].filter(a => a.show);

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <PageHeader
        title="Daily Operations"
        description="Monitor daily farm operations, mortality analytics, and production trends."
        actions={actions}
      />

      {/* KPI Section */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {kpiCards.map((card, index) => (
          <StatsCard key={index} {...card} />
        ))}
      </div>

      {/* Alert Banner */}
      {highMortalityRecords > 0 && (
        <Card className="border-red-200 bg-red-50">
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <AlertTriangle className="h-5 w-5 text-red-600" />
              <div>
                <p className="font-semibold text-red-900">High Mortality Alert</p>
                <p className="text-sm text-red-700">{highMortalityRecords} records show mortality above 10 birds</p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Main Content */}
      <Card>
        <CardHeader>
          <CardTitle>All Daily Records</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable columns={columns} data={records} loading={loading} />
        </CardContent>
      </Card>
    </div>
  );
};

export default DailyFarmRecordList;
