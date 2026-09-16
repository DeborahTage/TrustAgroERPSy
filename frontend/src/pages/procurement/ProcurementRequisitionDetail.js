import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { procurementApi, supplierApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../../components/ui/dialog';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatCurrency, formatDate } from '../../utils';
import { toast } from 'react-toastify';

const ProcurementRequisitionDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [req, setReq] = useState(null);
  const [suppliers, setSuppliers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [convertOpen, setConvertOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [supplierId, setSupplierId] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [reqRes, supRes] = await Promise.all([
        procurementApi.getRequisition(id),
        supplierApi.getAll().catch(() => ({ data: { data: [] } })),
      ]);
      setReq(reqRes.data.data);
      const list = Array.isArray(supRes.data) ? supRes.data : supRes.data?.data || [];
      setSuppliers(list);
    } catch {
      setError('Failed to load requisition.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const act = async (fn, msg) => {
    try {
      const res = await fn();
      toast.success(msg);
      if (msg.includes('Converted') && res?.data?.data?.id) {
        navigate(`/procurement/purchase-orders/${res.data.data.id}`);
        return;
      }
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    }
  };

  return (
    <EnterprisePage
      title={req?.requisitionNumber || 'Requisition'}
      subtitle={req ? `${req.title} · ${req.requestedByName || ''}` : ''}
      breadcrumbs={['Procurement', 'Requisitions', req?.requisitionNumber]}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={
        <div className="flex flex-wrap gap-2">
          {(hasPermission(PERMISSIONS.CREATE_PROCUREMENT) || hasPermission(PERMISSIONS.EDIT_PROCUREMENT)) && req?.status === 'DRAFT' && (
            <Button size="sm" onClick={() => act(() => procurementApi.submitRequisition(id), 'Submitted')}>Submit</Button>
          )}
          {hasPermission(PERMISSIONS.APPROVE_PROCUREMENT) && req?.status === 'SUBMITTED' && (
            <Button size="sm" variant="outline" onClick={() => act(() => procurementApi.reviewRequisition(id), 'Under review')}>Review</Button>
          )}
          {hasPermission(PERMISSIONS.APPROVE_PROCUREMENT) && ['SUBMITTED', 'UNDER_REVIEW'].includes(req?.status) && (
            <>
              <Button size="sm" onClick={() => act(() => procurementApi.approveRequisition(id), 'Approved')}>Approve</Button>
              <Button size="sm" variant="destructive" onClick={() => setRejectOpen(true)}>Reject</Button>
            </>
          )}
          {hasPermission(PERMISSIONS.CANCEL_PROCUREMENT) && !['CANCELLED', 'CONVERTED', 'REJECTED'].includes(req?.status) && (
            <Button size="sm" variant="outline" onClick={() => {
              if (!window.confirm('Cancel this requisition?')) return;
              act(() => procurementApi.cancelRequisition(id), 'Cancelled');
            }}>Cancel</Button>
          )}
          {(hasPermission(PERMISSIONS.CREATE_PROCUREMENT) || hasPermission(PERMISSIONS.EDIT_PROCUREMENT)) && req?.status === 'APPROVED' && (
            <Button size="sm" onClick={() => setConvertOpen(true)}>Convert to PO</Button>
          )}
          {req?.convertedPurchaseOrderId && (
            <Button size="sm" variant="outline" onClick={() => navigate(`/procurement/purchase-orders/${req.convertedPurchaseOrderId}`)}>
              Open PO {req.convertedPoNumber || ''}
            </Button>
          )}
        </div>
      }
    >
      {req && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <Card className="border-none shadow-sm lg:col-span-1">
            <CardHeader>
              <CardTitle className="text-lg">Summary</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              <div className="flex justify-between border-b py-2"><span>Status</span><Badge variant="outline">{req.status}</Badge></div>
              <div className="flex justify-between border-b py-2"><span>Priority</span><span>{req.priority}</span></div>
              <div className="flex justify-between border-b py-2"><span>Needed by</span><span>{formatDate(req.neededBy)}</span></div>
              <div className="flex justify-between border-b py-2"><span>Approved by</span><span>{req.approvedByName || '—'}</span></div>
              {req.rejectionReason && (
                <div className="pt-2 text-rose-700"><span className="font-semibold">Rejection:</span> {req.rejectionReason}</div>
              )}
              {req.justification && (
                <div className="pt-2"><span className="font-semibold">Justification</span><p className="text-slate-600 mt-1">{req.justification}</p></div>
              )}
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm lg:col-span-2">
            <CardHeader>
              <CardTitle className="text-lg">Lines</CardTitle>
              <CardDescription>Requested inventory items</CardDescription>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              {(req.items || []).map((line) => (
                <div key={line.id} className="flex justify-between border-b py-2 gap-4">
                  <div>
                    <div className="font-semibold">{line.itemName}</div>
                    <div className="text-xs text-slate-500 font-mono">{line.itemCode}</div>
                  </div>
                  <div className="text-right">
                    <div className="font-mono">{line.quantityRequested}</div>
                    <div className="text-xs text-slate-500">{line.estimatedUnitPrice != null ? formatCurrency(line.estimatedUnitPrice) : '—'}</div>
                  </div>
                </div>
              ))}
              {!req.items?.length && <p className="text-slate-500 py-4 text-center">No lines.</p>}
            </CardContent>
          </Card>
        </div>
      )}

      <Dialog open={rejectOpen} onOpenChange={setRejectOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Reject Requisition</DialogTitle></DialogHeader>
          <form onSubmit={(e) => {
            e.preventDefault();
            act(() => procurementApi.rejectRequisition(id, { reason: rejectReason || undefined }), 'Rejected');
            setRejectOpen(false);
          }} className="space-y-3">
            <div className="space-y-1"><Label>Reason</Label><Textarea value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} /></div>
            <DialogFooter><Button type="submit" variant="destructive">Reject</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={convertOpen} onOpenChange={setConvertOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Convert to Purchase Order</DialogTitle></DialogHeader>
          <form onSubmit={(e) => {
            e.preventDefault();
            act(() => procurementApi.convertRequisitionToPo(id, Number(supplierId)), 'Converted to PO');
            setConvertOpen(false);
          }} className="space-y-3">
            <div className="space-y-1">
              <Label>Supplier</Label>
              <Select value={supplierId} onValueChange={setSupplierId}>
                <SelectTrigger><SelectValue placeholder="Select supplier" /></SelectTrigger>
                <SelectContent>
                  {suppliers.map((s) => (
                    <SelectItem key={s.id} value={String(s.id)}>
                      {[s.supplierCode, s.name].filter(Boolean).join(' — ')}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <DialogFooter><Button type="submit" disabled={!supplierId}>Convert</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default ProcurementRequisitionDetail;
