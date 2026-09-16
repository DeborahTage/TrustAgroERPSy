import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { procurementApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Badge } from '../../components/ui/badge';
import { Input } from '../../components/ui/input';
import { formatCurrency, formatDate } from '../../utils';
import { DollarSign, Clock, AlertTriangle } from 'lucide-react';

const ProcurementAccountsPayable = () => {
  const [invoices, setInvoices] = useState([]);
  const [aging, setAging] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [invRes, agingRes] = await Promise.all([
        procurementApi.getSupplierInvoices(),
        procurementApi.getApAging().catch(() => ({ data: { data: null } })),
      ]);
      const all = invRes.data?.data || [];
      setInvoices(all.filter((i) => ['APPROVED', 'PARTIALLY_PAID'].includes(i.status) && Number(i.outstanding) > 0));
      setAging(agingRes.data?.data || null);
    } catch {
      setError('Failed to load accounts payable.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const filtered = useMemo(() => invoices.filter((r) => {
    if (!search.trim()) return true;
    const q = search.trim().toLowerCase();
    return `${r.invoiceNumber || ''} ${r.supplierName || ''}`.toLowerCase().includes(q);
  }), [invoices, search]);

  const stats = aging ? [
    { title: 'Current', value: formatCurrency(aging.current || 0), icon: DollarSign, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: '1–30 Days', value: formatCurrency(aging.days1to30 || 0), icon: Clock, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: '31–60 Days', value: formatCurrency(aging.days31to60 || 0), icon: Clock, iconColor: 'text-orange-600', bgColor: 'bg-orange-50' },
    { title: '61–90 Days', value: formatCurrency(aging.days61to90 || 0), icon: AlertTriangle, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    { title: '90+ Days', value: formatCurrency(aging.days90Plus || 0), icon: AlertTriangle, iconColor: 'text-rose-700', bgColor: 'bg-rose-50' },
    { title: 'Total AP', value: formatCurrency(aging.total || 0), icon: DollarSign, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
  ] : [];

  const cols = [
    { accessorKey: 'invoiceNumber', header: 'Invoice #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.invoiceNumber}</span> },
    { accessorKey: 'supplierName', header: 'Supplier' },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { accessorKey: 'dueDate', header: 'Due', cell: ({ row }) => (
      <span className={row.original.overdue ? 'text-rose-600 font-semibold' : ''}>{formatDate(row.original.dueDate)}</span>
    ) },
    { accessorKey: 'totalAmount', header: 'Total', cell: ({ row }) => formatCurrency(row.original.totalAmount) },
    { accessorKey: 'outstanding', header: 'Outstanding', cell: ({ row }) => formatCurrency(row.original.outstanding) },
  ];

  const agingCols = [
    { accessorKey: 'invoiceNumber', header: 'Invoice #' },
    { accessorKey: 'supplierName', header: 'Supplier' },
    { accessorKey: 'bucket', header: 'Bucket' },
    { accessorKey: 'daysPastDue', header: 'Days Past Due' },
    { accessorKey: 'outstanding', header: 'Outstanding', cell: ({ row }) => formatCurrency(row.original.outstanding) },
  ];

  return (
    <EnterprisePage
      title="Accounts Payable"
      subtitle="Open supplier balances and aging buckets"
      breadcrumbs={['Procurement', 'Accounts Payable']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      stats={stats}
    >
      <div className="space-y-6">
        <div>
          <Input className="max-w-xs mb-4" placeholder="Search open AP…" value={search} onChange={(e) => setSearch(e.target.value)} />
          {filtered.length === 0 && !loading ? (
            <p className="text-sm text-slate-500 py-6 text-center">No open payables.</p>
          ) : (
            <DataTable columns={cols} data={filtered} />
          )}
        </div>

        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Aging Detail</CardTitle>
            <CardDescription>Invoices bucketed by days past due</CardDescription>
          </CardHeader>
          <CardContent>
            {!aging?.invoices?.length ? (
              <p className="text-sm text-slate-500 py-4 text-center">No aging rows.</p>
            ) : (
              <DataTable columns={agingCols} data={aging.invoices} />
            )}
          </CardContent>
        </Card>
      </div>
    </EnterprisePage>
  );
};

export default ProcurementAccountsPayable;
