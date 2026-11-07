import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { financeApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/ui/tabs';
import { formatCurrency } from '../../utils';
import { FileText, Plus, Printer } from 'lucide-react';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../../components/ui/dialog';
import EmptyState from '../../components/shared/EmptyState';
import POSInvoiceDialog from './components/POSInvoiceDialog';
import { renderProformaInvoiceHtml, printHtml } from '../../lib/trustDocumentTemplates';
import { toast } from 'react-toastify';

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
  const [proformaOpen, setProformaOpen] = useState(false);
  const [proformaForm, setProformaForm] = useState({
    customer_name: '',
    customer_phone: '',
    description: '9-Week Bovans Brown Pullets (ቄብ ዶሮ)',
    quantity: '1000',
    unit_price: '650',
    unit: 'PCS',
  });

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
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <div className="flex justify-end">
          <Button
            size="sm"
            variant="ghost"
            title="Print Proforma Invoice"
            onClick={() => {
              const inv = row.original;
              printHtml(
                renderProformaInvoiceHtml({
                  proforma_no: inv.invoiceNumber,
                  customer_name: inv.clientName || 'Customer',
                  customer_phone: inv.phone || '-',
                  items: [
                    {
                      description: `${inv.invoiceType || 'Standard'} invoice (#${inv.invoiceNumber})`,
                      unit: 'PCS',
                      quantity: 1,
                      unit_price: Number(inv.totalAmount) || 0,
                      total_price: Number(inv.totalAmount) || 0,
                    },
                  ],
                  sub_total: (Number(inv.totalAmount) || 0) / 1.15,
                  vat: (Number(inv.totalAmount) || 0) - ((Number(inv.totalAmount) || 0) / 1.15),
                  grand_total: Number(inv.totalAmount) || 0,
                  date: inv.createdAt ? new Date(inv.createdAt).toLocaleDateString('en-GB') : undefined,
                }),
                `Proforma - ${inv.invoiceNumber}`
              );
            }}
          >
            <Printer className="h-4 w-4 mr-1" /> Proforma
          </Button>
        </div>
      ),
    },
  ];

  const stats = [
    { title: 'Total Invoices', value: invoices.length, icon: FileText, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Overdue', value: invoices.filter((i) => i.status === 'OVERDUE').length, icon: FileText, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    { title: 'Outstanding AR', value: formatCurrency(outstandingTotal), icon: FileText, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
  ];

  const handlePrintGeneratedProforma = (e) => {
    e.preventDefault();
    if (!proformaForm.customer_name.trim()) {
      toast.error('Customer name is required');
      return;
    }
    const qty = Number(proformaForm.quantity) || 1;
    const price = Number(proformaForm.unit_price) || 0;
    const sub = qty * price;
    const vat = sub * 0.15;
    const grand = sub + vat;

    printHtml(
      renderProformaInvoiceHtml({
        customer_name: proformaForm.customer_name,
        customer_phone: proformaForm.customer_phone,
        items: [
          {
            description: proformaForm.description,
            unit: proformaForm.unit,
            quantity: qty,
            unit_price: price,
            total_price: sub,
          },
        ],
        sub_total: sub,
        vat_rate: 0.15,
        vat: vat,
        grand_total: grand,
      }),
      `Proforma - ${proformaForm.customer_name}`
    );
    toast.success('Proforma invoice dispatched to printer');
    setProformaOpen(false);
  };

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
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => setProformaOpen(true)}>
            <Printer className="mr-2 h-4 w-4" /> Generate Proforma
          </Button>
          <Button className="bg-primary hover:bg-primary/90" onClick={() => setPosOpen(true)}>
            <Plus className="mr-2 h-4 w-4" /> POS Invoice
          </Button>
        </div>
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

      <Dialog open={proformaOpen} onOpenChange={setProformaOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Generate Official Proforma Invoice</DialogTitle>
          </DialogHeader>
          <form onSubmit={handlePrintGeneratedProforma} className="space-y-3">
            <div className="space-y-1">
              <Label>Customer Name *</Label>
              <Input
                required
                placeholder="e.g. Tadesse Poultry Farm"
                value={proformaForm.customer_name}
                onChange={(e) => setProformaForm({ ...proformaForm, customer_name: e.target.value })}
              />
            </div>
            <div className="space-y-1">
              <Label>Customer Phone</Label>
              <Input
                placeholder="0911223344"
                value={proformaForm.customer_phone}
                onChange={(e) => setProformaForm({ ...proformaForm, customer_phone: e.target.value })}
              />
            </div>
            <div className="space-y-1">
              <Label>Product Description</Label>
              <Input
                value={proformaForm.description}
                onChange={(e) => setProformaForm({ ...proformaForm, description: e.target.value })}
              />
            </div>
            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-1">
                <Label>Quantity</Label>
                <Input
                  type="number"
                  min="1"
                  value={proformaForm.quantity}
                  onChange={(e) => setProformaForm({ ...proformaForm, quantity: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label>Unit</Label>
                <Input
                  value={proformaForm.unit}
                  onChange={(e) => setProformaForm({ ...proformaForm, unit: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label>Unit Price (ETB)</Label>
                <Input
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={proformaForm.unit_price}
                  onChange={(e) => setProformaForm({ ...proformaForm, unit_price: e.target.value })}
                />
              </div>
            </div>

            <div className="p-3 bg-muted/40 rounded-lg text-xs space-y-1">
              <div className="flex justify-between">
                <span>Subtotal:</span>
                <span className="font-bold">
                  {((Number(proformaForm.quantity) || 0) * (Number(proformaForm.unit_price) || 0)).toLocaleString()} ETB
                </span>
              </div>
              <div className="flex justify-between">
                <span>VAT (15%):</span>
                <span className="font-bold">
                  {(((Number(proformaForm.quantity) || 0) * (Number(proformaForm.unit_price) || 0)) * 0.15).toLocaleString()} ETB
                </span>
              </div>
              <div className="flex justify-between text-primary font-bold border-t pt-1">
                <span>Grand Total:</span>
                <span>
                  {(((Number(proformaForm.quantity) || 0) * (Number(proformaForm.unit_price) || 0)) * 1.15).toLocaleString()} ETB
                </span>
              </div>
            </div>

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setProformaOpen(false)}>
                Cancel
              </Button>
              <Button type="submit">
                <Printer className="h-4 w-4 mr-1.5" /> Print Proforma
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default InvoiceCenter;
