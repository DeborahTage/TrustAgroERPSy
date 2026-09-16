import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { crmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatCurrency, formatDate } from '../../utils';
import { toast } from 'react-toastify';

const CrmQuotationDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [quote, setQuote] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await crmApi.getQuotation(id);
      setQuote(res.data.data);
    } catch {
      setError('Failed to load quotation.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const act = async (fn, msg) => {
    try {
      const res = await fn();
      toast.success(msg);
      const updated = res?.data?.data;
      if (updated?.convertedOrderId) {
        navigate(`/crm/orders/${updated.convertedOrderId}`);
      } else {
        load();
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    }
  };

  const itemCols = [
    { accessorKey: 'description', header: 'Description' },
    { accessorKey: 'quantity', header: 'Qty' },
    { accessorKey: 'unit', header: 'Unit' },
    { accessorKey: 'unitPrice', header: 'Unit Price', cell: ({ row }) => formatCurrency(row.original.unitPrice) },
    { accessorKey: 'lineTotal', header: 'Line Total', cell: ({ row }) => formatCurrency(row.original.lineTotal) },
  ];

  return (
    <EnterprisePage
      title={quote?.quotationNumber || 'Quotation'}
      subtitle={quote ? `${quote.clientName || ''} · ${quote.status}` : 'Quotation detail'}
      breadcrumbs={['CRM', 'Quotations', quote?.quotationNumber || id]}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={
        <>
          <Button variant="outline" size="sm" onClick={() => navigate('/crm/quotations')}>Back</Button>
          {hasPermission(PERMISSIONS.CRM_QUOTATION) && quote?.status === 'DRAFT' && (
            <Button size="sm" onClick={() => act(() => crmApi.submitQuotation(id), 'Submitted')}>Submit</Button>
          )}
          {hasPermission(PERMISSIONS.CRM_QUOTATION_APPROVE) && quote?.status === 'SUBMITTED' && (
            <Button size="sm" onClick={() => act(() => crmApi.approveQuotation(id), 'Approved')}>Approve</Button>
          )}
          {hasPermission(PERMISSIONS.CRM_QUOTATION) && quote?.status === 'APPROVED' && (
            <Button size="sm" onClick={() => act(() => crmApi.sendQuotation(id), 'Sent')}>Send</Button>
          )}
          {hasPermission(PERMISSIONS.CRM_QUOTATION) && quote?.status === 'SENT' && (
            <>
              <Button size="sm" onClick={() => act(() => crmApi.acceptQuotation(id), 'Accepted')}>Accept</Button>
              <Button size="sm" variant="destructive" onClick={() => act(() => crmApi.rejectQuotation(id), 'Rejected')}>Reject</Button>
            </>
          )}
          {hasPermission(PERMISSIONS.CRM_QUOTATION) && ['DRAFT', 'SUBMITTED', 'APPROVED'].includes(quote?.status) && (
            <Button size="sm" variant="outline" onClick={() => act(() => crmApi.cancelQuotation(id), 'Cancelled')}>Cancel</Button>
          )}
        </>
      }
    >
      {quote && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card className="border-none shadow-sm">
              <CardHeader><CardTitle className="text-base">Summary</CardTitle></CardHeader>
              <CardContent className="space-y-2 text-sm">
                {[
                  ['Status', <Badge key="s" variant="outline">{quote.status}</Badge>],
                  ['Customer', quote.clientName],
                  ['Type', quote.quotationType],
                  ['Issue', formatDate(quote.issueDate)],
                  ['Expiry', formatDate(quote.expiryDate)],
                  ['Subtotal', formatCurrency(quote.subtotal)],
                  ['Discount', formatCurrency(quote.discountAmount)],
                  ['Tax', formatCurrency(quote.taxAmount)],
                  ['Total', formatCurrency(quote.totalAmount)],
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
                {quote.clientId && (
                  <Button variant="outline" className="w-full justify-start" onClick={() => navigate(`/crm/clients/${quote.clientId}`)}>Customer 360</Button>
                )}
                {quote.opportunityId && (
                  <Button variant="outline" className="w-full justify-start" onClick={() => navigate(`/crm/opportunities/${quote.opportunityId}`)}>Opportunity</Button>
                )}
                {quote.convertedOrderId && (
                  <Button variant="outline" className="w-full justify-start" onClick={() => navigate(`/crm/orders/${quote.convertedOrderId}`)}>Converted Order</Button>
                )}
              </CardContent>
            </Card>
          </div>
          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle className="text-base">Line Items</CardTitle></CardHeader>
            <CardContent>
              <DataTable columns={itemCols} data={quote.items || []} />
            </CardContent>
          </Card>
        </div>
      )}
    </EnterprisePage>
  );
};

export default CrmQuotationDetail;
