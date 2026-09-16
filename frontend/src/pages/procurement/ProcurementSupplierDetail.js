import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { procurementApi, supplierApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Badge } from '../../components/ui/badge';
import { Button } from '../../components/ui/button';
import { formatCurrency, formatDate } from '../../utils';

const ProcurementSupplierDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [supplier, setSupplier] = useState(null);
  const [statement, setStatement] = useState(null);
  const [pos, setPos] = useState([]);
  const [receipts, setReceipts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [supRes, stmtRes, poRes, grnRes] = await Promise.all([
        supplierApi.getById(id),
        procurementApi.getSupplierStatement(id).catch(() => ({ data: { data: null } })),
        procurementApi.getPurchaseOrders().catch(() => ({ data: { data: [] } })),
        procurementApi.getGoodsReceipts().catch(() => ({ data: { data: [] } })),
      ]);
      const s = supRes.data?.data || supRes.data;
      setSupplier(s);
      setStatement(stmtRes.data?.data || null);
      const allPos = poRes.data?.data || [];
      const allGrns = grnRes.data?.data || [];
      setPos(allPos.filter((p) => String(p.supplierId) === String(id)));
      setReceipts(allGrns.filter((g) => String(g.supplierId) === String(id)));
    } catch {
      setError('Failed to load supplier detail.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const poCols = [
    { accessorKey: 'poNumber', header: 'PO #', cell: ({ row }) => (
      <button type="button" className="font-mono font-bold text-primary" onClick={() => navigate(`/procurement/purchase-orders/${row.original.id}`)}>
        {row.original.poNumber}
      </button>
    ) },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { accessorKey: 'totalAmount', header: 'Total', cell: ({ row }) => formatCurrency(row.original.totalAmount) },
    { accessorKey: 'expectedDelivery', header: 'Expected', cell: ({ row }) => formatDate(row.original.expectedDelivery) },
  ];

  const grnCols = [
    { accessorKey: 'grnNumber', header: 'GRN #' },
    { accessorKey: 'poNumber', header: 'PO #' },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { accessorKey: 'receiptDate', header: 'Date', cell: ({ row }) => formatDate(row.original.receiptDate) },
  ];

  const invCols = [
    { accessorKey: 'invoiceNumber', header: 'Invoice #' },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    { accessorKey: 'totalAmount', header: 'Total', cell: ({ row }) => formatCurrency(row.original.totalAmount) },
    { accessorKey: 'outstanding', header: 'Outstanding', cell: ({ row }) => formatCurrency(row.original.outstanding) },
  ];

  const payCols = [
    { accessorKey: 'paymentNumber', header: 'Payment #' },
    { accessorKey: 'invoiceNumber', header: 'Invoice #' },
    { accessorKey: 'amount', header: 'Amount', cell: ({ row }) => formatCurrency(row.original.amount) },
    { accessorKey: 'paymentDate', header: 'Date', cell: ({ row }) => formatDate(row.original.paymentDate) },
  ];

  return (
    <EnterprisePage
      title={supplier?.name || 'Supplier'}
      subtitle={[supplier?.supplierCode, supplier?.category].filter(Boolean).join(' · ')}
      breadcrumbs={['Procurement', 'Suppliers', supplier?.name]}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={<Button size="sm" variant="outline" onClick={() => navigate('/procurement/suppliers')}>Back</Button>}
      stats={statement ? [
        { title: 'Billed', value: formatCurrency(statement.billed || 0) },
        { title: 'Paid', value: formatCurrency(statement.paid || 0) },
        { title: 'Outstanding', value: formatCurrency(statement.outstanding || 0) },
      ] : []}
    >
      <div className="space-y-6">
        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Purchase Orders</CardTitle>
            <CardDescription>POs linked to this supplier</CardDescription>
          </CardHeader>
          <CardContent>
            {pos.length === 0 ? <p className="text-sm text-slate-500">No purchase orders.</p> : <DataTable columns={poCols} data={pos} />}
          </CardContent>
        </Card>

        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Goods Receipts</CardTitle>
          </CardHeader>
          <CardContent>
            {receipts.length === 0 ? <p className="text-sm text-slate-500">No goods receipts.</p> : <DataTable columns={grnCols} data={receipts} />}
          </CardContent>
        </Card>

        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Invoices</CardTitle>
            <CardDescription>From supplier statement</CardDescription>
          </CardHeader>
          <CardContent>
            {!statement?.invoices?.length
              ? <p className="text-sm text-slate-500">No invoices.</p>
              : <DataTable columns={invCols} data={statement.invoices} />}
          </CardContent>
        </Card>

        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Payments</CardTitle>
          </CardHeader>
          <CardContent>
            {!statement?.payments?.length
              ? <p className="text-sm text-slate-500">No payments.</p>
              : <DataTable columns={payCols} data={statement.payments} />}
          </CardContent>
        </Card>
      </div>
    </EnterprisePage>
  );
};

export default ProcurementSupplierDetail;
