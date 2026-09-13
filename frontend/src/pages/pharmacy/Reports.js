import React, { useCallback, useEffect, useState } from 'react';
import { pharmacyApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { formatCurrency } from '../../utils';
import DataTable from '../../components/shared/DataTable';
import { DollarSign, Package, Pill, TrendingUp } from 'lucide-react';
import { toast } from 'react-toastify';

export default function PharmacyReports() {
  const [dashboard, setDashboard] = useState(null);
  const [sales, setSales] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [dashRes, salesRes] = await Promise.all([
        pharmacyApi.getDashboard(),
        pharmacyApi.getSales(),
      ]);
      setDashboard(dashRes.data?.data || dashRes.data || {});
      setSales(salesRes.data?.data || salesRes.data || []);
    } catch {
      setError('Failed to load pharmacy reports.');
      toast.error('Failed to load pharmacy reports');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const stats = dashboard ? [
    { title: 'Revenue Today', value: formatCurrency(dashboard.revenue_today || dashboard.revenueToday || 0), icon: DollarSign, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: 'Sales Today', value: dashboard.sales_count_today || dashboard.salesCountToday || 0, icon: TrendingUp, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Pending Rx', value: dashboard.pending_prescriptions || dashboard.pendingPrescriptions || 0, icon: Pill, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: 'Catalog Items', value: dashboard.catalog_count || dashboard.catalogCount || '—', icon: Package, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
  ] : [];

  const columns = [
    { accessorKey: 'saleNumber', header: 'Sale #' },
    { accessorKey: 'customerName', header: 'Customer', cell: ({ row }) => row.original.customerName || row.original.customer?.name || 'Walk-in' },
    { accessorKey: 'totalAmount', header: 'Amount', cell: ({ row }) => formatCurrency(row.original.totalAmount || row.original.total) },
    { accessorKey: 'paymentMethod', header: 'Payment' },
    { accessorKey: 'saleDate', header: 'Date' },
  ];

  return (
    <EnterprisePage
      title="Pharmacy Reports"
      subtitle="Sales performance and dispensing analytics from live pharmacy data"
      breadcrumbs={['Pharmacy', 'Reports']}
      loading={loading}
      error={error}
      onRetry={fetchData}
      onRefresh={fetchData}
      stats={stats}
      exportData={sales}
      exportColumns={columns}
      exportFilename="pharmacy_sales"
    >
      <Card className="rounded-2xl border-none shadow-sm">
        <CardHeader>
          <CardTitle>Recent Sales</CardTitle>
          <CardDescription>Transactions recorded in the pharmacy module</CardDescription>
        </CardHeader>
        <CardContent>
          <DataTable columns={columns} data={sales} loading={loading} pageSize={10} />
        </CardContent>
      </Card>
    </EnterprisePage>
  );
}
