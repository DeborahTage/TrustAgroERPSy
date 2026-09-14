import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { financeApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/ui/tabs';
import { formatCurrency } from '../../utils';
import { FileText, Plus } from 'lucide-react';
import EmptyState from '../../components/shared/EmptyState';
import POSInvoiceDialog from './components/POSInvoiceDialog';

const StatusBadge = ({ status }) => {
  const variants = {
    PAID: 'bg-emerald-100 text-emerald-700',
    OVERDUE: 'bg-rose-100 text-rose-700',
    SENT: 'bg-blue-100 text-blue-700',
    DRAFT: 'bg-slate-100 text-slate-600',
    PARTIALLY_PAID: 'bg-amber-100 text-amber-700',
  };
  return <Badge className={`${variants[status] || 'bg-slate-100'} shadow-none`}>{status}</Badge>;
};

const InvoiceCenter = () => {
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [tab, setTab] = useState('all');
  const [posOpen, setPosOpen] = useState(false);

  const fetchInvoices = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await financeApi.getInvoices();
      setInvoices(res.data.data || []);
    } catch (e) {
      setError('Failed to load invoices.');
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchInvoices(); }, [fetchInvoices]);

  const filtered = useMemo(() => {
    if (tab === 'pos') return invoices.filter((i) => i.invoiceType === 'POS');
    if (tab === 'service') return invoices.filter((i) => i.invoiceType === 'SERVICE');
    if (tab === 'chicken') return invoices.filter((i) => i.invoiceType === 'CHICKEN_SALE' || i.sourceModule === 'CHICKEN_SALE');
    if (tab === 'training') return invoices.filter((i) => i.invoiceType === 'TRAINING' || i.sourceModule === 'TRAINING');
    if (tab === 'overdue') return invoices.filter((i) => i.status === 'OVERDUE');
    return invoices;
  }, [invoices, tab]);

  const outstandingTotal = invoices
    .filter((i) => ['SENT', 'PARTIALLY_PAID', 'OVERDUE'].includes(i.status))
    .reduce((s, i) => s + Math.max(0, (Number(i.totalAmount) || 0) - (Number(i.paidAmount) || 0)), 0);

  const columns = [
    { accessorKey: 'invoiceNumber', header: 'Invoice #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.invoiceNumber}</span> },
    { accessorKey: 'invoiceType', header: 'Type' },
    { accessorKey: 'sourceModule', header: 'Source', cell: ({ row }) => row.original.sourceModule || row.original.invoiceType || '—' },
    { accessorKey: 'sourceId', header: 'Source ID', cell: ({ row }) => row.original.sourceId || '—' },
    { accessorKey: 'clientName', header: 'Customer' },
    { accessorKey: 'businessUnit', header: 'Unit', cell: ({ row }) => <Badge variant="outline" className="text-[10px]">{row.original.businessUnit}</Badge> },
    {
      accessorKey: 'createdAt',
      header: 'Date',
      cell: ({ row }) => row.original.createdAt ? new Date(row.original.createdAt).toLocaleDateString() : '—',
    },
    {
      accessorKey: 'totalAmount',
      header: 'Total',
      cell: ({ row }) => <span className="font-bold">{formatCurrency(row.original.totalAmount)}</span>,
    },
    {
      id: 'outstanding',
      header: 'Outstanding',
      cell: ({ row }) => formatCurrency(Math.max(0, (Number(row.original.totalAmount) || 0) - (Number(row.original.paidAmount) || 0))),
    },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <StatusBadge status={row.original.status} /> },
  ];

  const stats = [
    { title: 'Total Invoices', value: invoices.length, icon: FileText, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Overdue', value: invoices.filter((i) => i.status === 'OVERDUE').length, icon: FileText, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    { title: 'Outstanding AR', value: formatCurrency(outstandingTotal), icon: FileText, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
  ];

  return (
    <EnterprisePage
      title="Invoice Center"
      subtitle="Multi-stream billing from pharmacy POS, chicken sales, and services"
      breadcrumbs={['Finance', 'Invoices']}
      loading={loading}
      error={error}
      onRetry={fetchInvoices}
      onRefresh={fetchInvoices}
      stats={stats}
      exportData={filtered}
      exportColumns={columns}
      exportFilename="invoices"
      exportElementId="invoice-table"
      exportTitle="Invoice Report"
      actions={
        <Button className="bg-primary hover:bg-primary/90" onClick={() => setPosOpen(true)}>
          <Plus className="mr-2 h-4 w-4" /> POS Invoice
        </Button>
      }
    >
      <Tabs value={tab} onValueChange={setTab}>
        <TabsList className="bg-slate-100 mb-6">
          <TabsTrigger value="all">All ({invoices.length})</TabsTrigger>
          <TabsTrigger value="pos">POS</TabsTrigger>
          <TabsTrigger value="service">Service</TabsTrigger>
          <TabsTrigger value="chicken">Chicken Sales</TabsTrigger>
          <TabsTrigger value="training">Training</TabsTrigger>
          <TabsTrigger value="overdue">Overdue</TabsTrigger>
        </TabsList>

        <TabsContent value={tab} className="m-0">
          <div id="invoice-table">
            {filtered.length === 0 ? (
              <EmptyState
                icon={FileText}
                title="No invoices found"
                description="Generate a POS invoice or create a service invoice to get started."
                actionLabel="Create POS Invoice"
                action={() => setPosOpen(true)}
              />
            ) : (
              <DataTable columns={columns} data={filtered} searchable pagination pageSize={15} />
            )}
          </div>
        </TabsContent>
      </Tabs>

      <POSInvoiceDialog open={posOpen} onOpenChange={setPosOpen} onSuccess={fetchInvoices} />
    </EnterprisePage>
  );
};

export default InvoiceCenter;
