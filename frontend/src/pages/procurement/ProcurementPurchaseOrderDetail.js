import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { procurementApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatCurrency, formatDate } from '../../utils';
import { toast } from 'react-toastify';

const ProcurementPurchaseOrderDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [po, setPo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await procurementApi.getPurchaseOrder(id);
      setPo(res.data.data);
    } catch {
      setError('Failed to load purchase order.');
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

  const canReceive = ['SENT', 'PARTIALLY_RECEIVED', 'APPROVED'].includes(po?.status);

  return (
    <EnterprisePage
      title={po?.poNumber || 'Purchase Order'}
      subtitle={po ? `${po.supplierName || ''} ${po.supplierCode ? `(${po.supplierCode})` : ''}` : ''}
      breadcrumbs={['Procurement', 'Purchase Orders', po?.poNumber]}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={
        <div className="flex flex-wrap gap-2">
          {(hasPermission(PERMISSIONS.CREATE_PROCUREMENT) || hasPermission(PERMISSIONS.EDIT_PROCUREMENT)) && po?.status === 'DRAFT' && (
            <Button size="sm" onClick={() => act(() => procurementApi.submitPurchaseOrder(id), 'Submitted')}>Submit</Button>
          )}
          {hasPermission(PERMISSIONS.APPROVE_PROCUREMENT) && po?.status === 'SUBMITTED' && (
            <Button size="sm" onClick={() => act(() => procurementApi.approvePurchaseOrder(id), 'Approved')}>Approve</Button>
          )}
          {(hasPermission(PERMISSIONS.CREATE_PROCUREMENT) || hasPermission(PERMISSIONS.EDIT_PROCUREMENT)) && po?.status === 'APPROVED' && (
            <Button size="sm" onClick={() => act(() => procurementApi.sendPurchaseOrder(id), 'Sent to supplier')}>Send</Button>
          )}
          {hasPermission(PERMISSIONS.RECEIVE_PROCUREMENT) && canReceive && (
            <Button size="sm" className="bg-emerald-600 hover:bg-emerald-700" onClick={() => navigate(`/procurement/goods-receipts?poId=${id}`)}>
              Receive Goods
            </Button>
          )}
          {(hasPermission(PERMISSIONS.EDIT_PROCUREMENT) || hasPermission(PERMISSIONS.APPROVE_PROCUREMENT)) && ['FULLY_RECEIVED', 'PARTIALLY_RECEIVED', 'SENT'].includes(po?.status) && (
            <Button size="sm" variant="outline" onClick={() => act(() => procurementApi.closePurchaseOrder(id), 'Closed')}>Close</Button>
          )}
          {hasPermission(PERMISSIONS.CANCEL_PROCUREMENT) && !['CANCELLED', 'CLOSED', 'FULLY_RECEIVED'].includes(po?.status) && (
            <Button size="sm" variant="destructive" onClick={() => {
              if (!window.confirm('Cancel this purchase order?')) return;
              act(() => procurementApi.cancelPurchaseOrder(id), 'Cancelled');
            }}>Cancel</Button>
          )}
        </div>
      }
    >
      {po && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle className="text-lg">Summary</CardTitle></CardHeader>
            <CardContent className="space-y-2 text-sm">
              <div className="flex justify-between border-b py-2"><span>Status</span><Badge variant="outline">{po.status}</Badge></div>
              <div className="flex justify-between border-b py-2"><span>Order date</span><span>{formatDate(po.orderDate)}</span></div>
              <div className="flex justify-between border-b py-2"><span>Expected</span><span>{formatDate(po.expectedDelivery)}</span></div>
              <div className="flex justify-between border-b py-2"><span>Payment terms</span><span>{po.paymentTerms || '—'}</span></div>
              <div className="flex justify-between border-b py-2"><span>Subtotal</span><span className="font-mono">{formatCurrency(po.subtotal)}</span></div>
              <div className="flex justify-between border-b py-2"><span>Tax</span><span className="font-mono">{formatCurrency(po.taxAmount)}</span></div>
              <div className="flex justify-between border-b py-2"><span>Total</span><span className="font-mono font-bold">{formatCurrency(po.totalAmount)}</span></div>
              {po.requisitionNumber && (
                <Button size="sm" variant="link" className="px-0" onClick={() => navigate(`/procurement/requisitions/${po.requisitionId}`)}>
                  From {po.requisitionNumber}
                </Button>
              )}
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm lg:col-span-2">
            <CardHeader>
              <CardTitle className="text-lg">Lines</CardTitle>
              <CardDescription>Ordered vs received quantities</CardDescription>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              {(po.items || []).map((line) => {
                const remaining = (line.quantityOrdered || 0) - (line.quantityReceived || 0);
                return (
                  <div key={line.id} className="flex justify-between border-b py-2 gap-4">
                    <div>
                      <div className="font-semibold">{line.itemName}</div>
                      <div className="text-xs text-slate-500 font-mono">{line.itemCode}</div>
                    </div>
                    <div className="text-right">
                      <div className="font-mono">{line.quantityOrdered} ordered</div>
                      <div className="text-xs text-slate-500">{line.quantityReceived || 0} received · {remaining} remaining</div>
                      <div className="font-mono text-xs">{formatCurrency(line.lineTotal)}</div>
                    </div>
                  </div>
                );
              })}
            </CardContent>
          </Card>
        </div>
      )}
    </EnterprisePage>
  );
};

export default ProcurementPurchaseOrderDetail;
