import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { chickenSaleApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatCurrency, formatDate } from '../../utils';
import { Plus } from 'lucide-react';

const STATUS_STYLES = {
  DRAFT: 'bg-slate-100 text-slate-700',
  CONFIRMED: 'bg-blue-100 text-blue-700',
  INVOICED: 'bg-indigo-100 text-indigo-700',
  PARTIALLY_PAID: 'bg-amber-100 text-amber-700',
  PAID: 'bg-emerald-100 text-emerald-700',
  COMPLETED: 'bg-emerald-200 text-emerald-800',
  CANCELLED: 'bg-rose-100 text-rose-700',
};

const ChickenSales = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [sales, setSales] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [status, setStatus] = useState('all');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = status !== 'all' ? { status } : {};
      const res = await chickenSaleApi.getAll(params);
      setSales(res.data.data || []);
    } catch {
      setError('Failed to load chicken sales.');
    } finally {
      setLoading(false);
    }
  }, [status]);

  useEffect(() => { load(); }, [load]);

  const stats = useMemo(() => [
    { title: 'Open Sales', value: sales.filter((s) => !['CANCELLED', 'COMPLETED'].includes(s.status)).length },
    { title: 'Outstanding', value: formatCurrency(sales.reduce((sum, s) => sum + (Number(s.outstandingAmount) || 0), 0)) },
    { title: 'Chickens Sold (confirmed+)', value: sales.filter((s) => !['DRAFT', 'CANCELLED'].includes(s.status)).reduce((sum, s) => sum + (Number(s.totalChickens) || 0), 0).toLocaleString() },
  ], [sales]);

  const cols = [
    { accessorKey: 'saleNumber', header: 'Sale #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.saleNumber}</span> },
    { accessorKey: 'saleDate', header: 'Date', cell: ({ row }) => formatDate(row.original.saleDate) },
    { accessorKey: 'customerName', header: 'Customer' },
    { accessorKey: 'farmName', header: 'Farm' },
    { accessorKey: 'flockBatchCode', header: 'Flock' },
    { accessorKey: 'totalChickens', header: 'Chickens' },
    { accessorKey: 'totalAmount', header: 'Total', cell: ({ row }) => formatCurrency(row.original.totalAmount) },
    { accessorKey: 'outstandingAmount', header: 'Outstanding', cell: ({ row }) => formatCurrency(row.original.outstandingAmount) },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ row }) => (
        <Badge className={`${STATUS_STYLES[row.original.status] || 'bg-slate-100'} shadow-none`}>{row.original.status}</Badge>
      ),
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <Button size="sm" variant="outline" onClick={() => navigate(`/farm/chicken-sales/${row.original.id}`)}>
          Open
        </Button>
      ),
    },
  ];

  return (
    <EnterprisePage
      title="Chicken Sales"
      subtitle="Sell Chickens from managed flocks — invoices and payments post to finance"
      breadcrumbs={['Farm', 'Chicken Sales']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      stats={stats}
      actions={
        <div className="flex items-center gap-3">
          <Select value={status} onValueChange={setStatus}>
            <SelectTrigger className="w-44"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All statuses</SelectItem>
              {['DRAFT', 'CONFIRMED', 'INVOICED', 'PARTIALLY_PAID', 'PAID', 'COMPLETED', 'CANCELLED'].map((s) => (
                <SelectItem key={s} value={s}>{s}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          {hasPermission(PERMISSIONS.CREATE_CHICKEN_SALES) && (
            <Button className="bg-primary hover:bg-primary/90" onClick={() => navigate('/farm/chicken-sales/new')}>
              <Plus className="mr-2 h-4 w-4" /> New Sale
            </Button>
          )}
        </div>
      }
    >
      <DataTable columns={cols} data={sales} searchable emptyMessage="No chicken sales recorded yet" />
    </EnterprisePage>
  );
};

export default ChickenSales;
