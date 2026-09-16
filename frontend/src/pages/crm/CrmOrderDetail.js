import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { crmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../../components/ui/dialog';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatCurrency, formatDate } from '../../utils';
import { toast } from 'react-toastify';

const PAYMENT_METHODS = ['CASH', 'BANK_TRANSFER', 'MOBILE_MONEY', 'CREDIT'];

const CrmOrderDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [order, setOrder] = useState(null);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [payOpen, setPayOpen] = useState(false);
  const [payForm, setPayForm] = useState({ amount: '', paymentMethod: 'CASH', paymentDate: '' });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [oRes, pRes] = await Promise.all([
        crmApi.getOrder(id),
        crmApi.getOrderPayments(id).catch(() => ({ data: { data: [] } })),
      ]);
      setOrder(oRes.data.data);
      setPayments(pRes.data?.data || []);
    } catch {
      setError('Failed to load order.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const act = async (fn, msg) => {
    try {
      await fn();
      toast.success(msg);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    }
  };

  const submitPay = async (e) => {
    e.preventDefault();
    await act(() => crmApi.recordOrderPayment(id, {
      amount: parseFloat(payForm.amount),
      paymentMethod: payForm.paymentMethod,
      paymentDate: payForm.paymentDate || undefined,
    }), 'Payment recorded');
    setPayOpen(false);
    setPayForm({ amount: '', paymentMethod: 'CASH', paymentDate: '' });
  };

  const itemCols = [
    { accessorKey: 'productName', header: 'Product' },
    { accessorKey: 'quantity', header: 'Qty' },
    { accessorKey: 'unit', header: 'Unit' },
    { accessorKey: 'unitPrice', header: 'Unit Price', cell: ({ row }) => formatCurrency(row.original.unitPrice) },
    { accessorKey: 'subtotal', header: 'Line Total', cell: ({ row }) => formatCurrency(row.original.subtotal) },
  ];

  const payCols = [
    { accessorKey: 'paymentNumber', header: 'Payment #', cell: ({ row }) => <span className="font-mono">{row.original.paymentNumber}</span> },
    { accessorKey: 'amount', header: 'Amount', cell: ({ row }) => formatCurrency(row.original.amount) },
    { accessorKey: 'method', header: 'Method' },
    { accessorKey: 'paymentDate', header: 'Date', cell: ({ row }) => formatDate(row.original.paymentDate) },
  ];

  return (
    <EnterprisePage
      title={order?.orderNumber || 'Order'}
      subtitle={order ? `${order.clientName || ''} · ${order.status}` : 'Order detail'}
      breadcrumbs={['CRM', 'Orders', order?.orderNumber || id]}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={
        <>
          <Button variant="outline" size="sm" onClick={() => navigate('/crm/orders')}>Back</Button>
          {hasPermission(PERMISSIONS.CRM_ORDER_CONFIRM) && order?.status === 'DRAFT' && (
            <Button size="sm" onClick={() => act(() => crmApi.confirmOrder(id), 'Confirmed')}>Confirm</Button>
          )}
          {hasPermission(PERMISSIONS.CRM_ORDER) && ['CONFIRMED', 'IN_PROGRESS', 'PARTIALLY_FULFILLED'].includes(order?.status) && (
            <Button size="sm" variant="outline" onClick={() => act(() => crmApi.fulfillOrder(id, false), 'Fulfilled')}>Fulfill</Button>
          )}
          {hasPermission(PERMISSIONS.CRM_ORDER_INVOICE) && order && !order.invoiceId && order.status !== 'DRAFT' && order.status !== 'CANCELLED' && (
            <Button size="sm" onClick={() => act(() => crmApi.invoiceOrder(id), 'Invoice created')}>Create Invoice</Button>
          )}
          {hasPermission(PERMISSIONS.CRM_ORDER_PAY) && order && Number(order.outstandingAmount) > 0 && (
            <Button size="sm" onClick={() => {
              setPayForm({ amount: String(order.outstandingAmount || ''), paymentMethod: 'CASH', paymentDate: '' });
              setPayOpen(true);
            }}>Record Payment</Button>
          )}
          {hasPermission(PERMISSIONS.CRM_ORDER) && order && !['FULFILLED', 'CANCELLED'].includes(order.status) && (
            <Button size="sm" variant="destructive" onClick={() => {
              if (!window.confirm('Cancel this order?')) return;
              act(() => crmApi.cancelOrder(id), 'Cancelled');
            }}>Cancel</Button>
          )}
        </>
      }
    >
      {order && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card className="border-none shadow-sm">
              <CardHeader><CardTitle className="text-base">Summary</CardTitle></CardHeader>
              <CardContent className="space-y-2 text-sm">
                {[
                  ['Status', <Badge key="s" variant="outline">{order.status}</Badge>],
                  ['Customer', order.clientName],
                  ['Order Date', formatDate(order.orderDate)],
                  ['Expected Delivery', formatDate(order.expectedDeliveryDate)],
                  ['Total', formatCurrency(order.totalAmount)],
                  ['Paid', formatCurrency(order.paidAmount)],
                  ['Outstanding', formatCurrency(order.outstandingAmount)],
                  ['Invoice', order.invoiceNumber || (order.invoiceId ? `#${order.invoiceId}` : '—')],
                ].map(([label, value]) => (
                  <div key={label} className="flex justify-between border-b py-2 gap-4">
                    <span className="text-slate-500">{label}</span>
                    <span className="text-right font-medium">{value || '—'}</span>
                  </div>
                ))}
              </CardContent>
            </Card>
            <Card className="border-none shadow-sm">
              <CardHeader><CardTitle className="text-base">Links</CardTitle></CardHeader>
              <CardContent className="space-y-2">
                {order.clientId && (
                  <Button variant="outline" className="w-full justify-start" onClick={() => navigate(`/crm/clients/${order.clientId}`)}>Customer 360</Button>
                )}
                {order.quotationId && (
                  <Button variant="outline" className="w-full justify-start" onClick={() => navigate(`/crm/quotations/${order.quotationId}`)}>Source Quotation</Button>
                )}
                {order.opportunityId && (
                  <Button variant="outline" className="w-full justify-start" onClick={() => navigate(`/crm/opportunities/${order.opportunityId}`)}>Opportunity</Button>
                )}
              </CardContent>
            </Card>
          </div>

          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle className="text-base">Line Items</CardTitle></CardHeader>
            <CardContent><DataTable columns={itemCols} data={order.items || []} /></CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle className="text-base">Payments</CardTitle></CardHeader>
            <CardContent>
              {payments.length === 0 ? (
                <p className="text-sm text-slate-500 py-4 text-center">No payments recorded.</p>
              ) : (
                <DataTable columns={payCols} data={payments} />
              )}
            </CardContent>
          </Card>
        </div>
      )}

      <Dialog open={payOpen} onOpenChange={setPayOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Record Payment</DialogTitle></DialogHeader>
          <form onSubmit={submitPay} className="space-y-3">
            <div className="space-y-1"><Label>Amount</Label><Input type="number" step="0.01" required value={payForm.amount} onChange={(e) => setPayForm({ ...payForm, amount: e.target.value })} /></div>
            <div className="space-y-1">
              <Label>Method</Label>
              <Select value={payForm.paymentMethod} onValueChange={(v) => setPayForm({ ...payForm, paymentMethod: v })}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>{PAYMENT_METHODS.map((m) => <SelectItem key={m} value={m}>{m}</SelectItem>)}</SelectContent>
              </Select>
            </div>
            <div className="space-y-1"><Label>Payment Date</Label><Input type="date" value={payForm.paymentDate} onChange={(e) => setPayForm({ ...payForm, paymentDate: e.target.value })} /></div>
            <DialogFooter><Button type="submit">Record</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default CrmOrderDetail;
