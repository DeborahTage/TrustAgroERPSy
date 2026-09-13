import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { inventoryApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { formatDate } from '../../utils';
import { Clock, AlertTriangle, CheckCircle2, Search } from 'lucide-react';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { exportToCsv } from '../../lib/exportTable';

const ExpiryTracking = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await inventoryApi.getExpiryAlerts();
      setData(res.data.data || []);
    } catch {
      setError('Failed to load expiry data.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const getStatus = (expiryDate) => {
    const days = Math.floor((new Date(expiryDate) - new Date()) / (1000 * 60 * 60 * 24));
    if (days < 0) return { label: 'Expired', variant: 'high' };
    if (days < 30) return { label: 'Critical', variant: 'high' };
    if (days < 90) return { label: 'Warning', variant: 'medium' };
    return { label: 'Safe', variant: 'low' };
  };

  const filtered = useMemo(() =>
    data.filter(i =>
      i.batchNumber?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      i.item?.itemName?.toLowerCase().includes(searchTerm.toLowerCase())
    ), [data, searchTerm]);

  const stats = useMemo(() => {
    const daysLeft = (d) => Math.floor((new Date(d) - new Date()) / (1000 * 60 * 60 * 24));
    return [
      { title: 'Critical / Expired', value: data.filter(i => daysLeft(i.expiryDate) < 30).length, icon: AlertTriangle, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
      { title: 'Warning (30-90d)', value: data.filter(i => { const d = daysLeft(i.expiryDate); return d >= 30 && d < 90; }).length, icon: Clock, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
      { title: 'Safe Stock', value: data.filter(i => daysLeft(i.expiryDate) >= 90).length, icon: CheckCircle2, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    ];
  }, [data]);

  const columns = [
    { accessorKey: 'itemName', header: 'Item Name', cell: ({ row }) => row.original.item?.itemName },
    { accessorKey: 'batchNumber', header: 'Batch/Lot #' },
    { accessorKey: 'quantityRemaining', header: 'Qty on Hand' },
    {
      accessorKey: 'expiryDate',
      header: 'Expiry Date',
      cell: ({ row }) => (
        <div className="flex flex-col">
          <span className="font-semibold">{formatDate(row.original.expiryDate)}</span>
          <span className="text-xs text-slate-500">
            {Math.floor((new Date(row.original.expiryDate) - new Date()) / (1000 * 60 * 60 * 24))} days left
          </span>
        </div>
      ),
    },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ row }) => <StatusBadge status={getStatus(row.original.expiryDate).variant} />,
    },
    { accessorKey: 'supplier', header: 'Supplier' },
  ];

  return (
    <EnterprisePage
      title="Stock Expiration Tracking"
      subtitle="Proactively manage pharmaceutical and perishable inventory lifecycles"
      breadcrumbs={['Inventory', 'Expiry']}
      loading={loading}
      error={error}
      onRetry={fetchData}
      onRefresh={fetchData}
      stats={stats}
      exportData={filtered}
      exportColumns={columns}
      exportFilename="expiry_tracking"
      actions={
        <Button variant="outline" size="sm" onClick={() => exportToCsv(filtered, columns, 'expiry_tracking')}>
          Export CSV
        </Button>
      }
      empty={!filtered.length && !loading ? { title: 'No expiry alerts', description: 'All batches are within safe lifecycle windows.' } : null}
    >
      <Card className="rounded-2xl border-none shadow-sm">
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>Comprehensive Expiry Log</CardTitle>
          <div className="relative">
            <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-slate-400" />
            <Input placeholder="Find batch..." className="pl-9 w-[200px]" value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
          </div>
        </CardHeader>
        <CardContent>
          <DataTable columns={columns} data={filtered} loading={loading} pagination />
        </CardContent>
      </Card>
    </EnterprisePage>
  );
};

export default ExpiryTracking;
