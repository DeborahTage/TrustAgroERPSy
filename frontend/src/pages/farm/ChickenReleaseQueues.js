import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { chickenReleaseApi, farmDocumentApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Input } from '../../components/ui/input';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatCurrency } from '../../utils';

const STATUS_STYLES = {
  SUBMITTED: 'bg-slate-100 text-slate-700',
  UNDER_REVIEW: 'bg-slate-100 text-slate-700',
  VERIFIED: 'bg-blue-100 text-blue-700',
  REJECTED: 'bg-rose-100 text-rose-700',
  FORWARDED_TO_FARM: 'bg-indigo-100 text-indigo-700',
  FARM_PENDING: 'bg-amber-100 text-amber-700',
  FARM_APPROVED: 'bg-emerald-100 text-emerald-700',
  FARM_REJECTED: 'bg-rose-100 text-rose-700',
  DISPATCHED: 'bg-teal-100 text-teal-700',
  MARKETING_RECEIVED: 'bg-cyan-100 text-cyan-700',
  OUT_FOR_DELIVERY: 'bg-violet-100 text-violet-700',
  DELIVERED: 'bg-emerald-200 text-emerald-800',
  DELIVERY_FAILED: 'bg-rose-100 text-rose-700',
  CANCELLED: 'bg-slate-200 text-slate-600',
};

/**
 * Shared queue over ChickenDeliveryRelease — one record, role-specific actions.
 * queue: FINANCE | FARM | MARKETING
 */
const ChickenReleaseQueue = ({ queue = 'FINANCE', title = 'Chicken Delivery Queue' }) => {
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const [bonusById, setBonusById] = useState({});
  const [rejectReason, setRejectReason] = useState({});

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await chickenReleaseApi.list({ queue });
      setRows(res.data?.data || []);
    } catch {
      setError('Failed to load delivery queue.');
    } finally {
      setLoading(false);
    }
  }, [queue]);

  useEffect(() => { load(); }, [load]);

  const run = async (id, action) => {
    setBusyId(id);
    setError(null);
    try {
      await action();
      await load();
    } catch (e) {
      setError(e?.response?.data?.message || e?.message || 'Action failed');
    } finally {
      setBusyId(null);
    }
  };

  const openDoc = async (id) => {
    try {
      const res = await farmDocumentApi.html('CHICKEN_DISPATCH', id);
      const w = window.open('', '_blank');
      if (w) {
        w.document.write(typeof res.data === 'string' ? res.data : (res.data?.data || ''));
        w.document.close();
      }
    } catch {
      setError('Could not open document');
    }
  };

  const stats = useMemo(() => [
    { title: 'In queue', value: rows.length },
    { title: 'Authorized birds', value: rows.reduce((s, r) => s + (Number(r.authorizedChickenQuantity) || 0), 0).toLocaleString() },
    { title: 'Bonus birds', value: rows.reduce((s, r) => s + (Number(r.bonusQuantity) || 0), 0).toLocaleString() },
  ], [rows]);

  const cols = [
    { accessorKey: 'releaseNumber', header: 'Release #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.releaseNumber}</span> },
    { accessorKey: 'customerName', header: 'Customer' },
    { accessorKey: 'farmName', header: 'Farm' },
    { accessorKey: 'flockBatchCode', header: 'Batch' },
    { accessorKey: 'authorizedChickenQuantity', header: 'Paid qty' },
    { accessorKey: 'bonusQuantity', header: 'Bonus' },
    { accessorKey: 'paymentAmount', header: 'Payment', cell: ({ row }) => formatCurrency(row.original.paymentAmount) },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ row }) => (
        <Badge className={`${STATUS_STYLES[row.original.status] || 'bg-slate-100'} shadow-none`}>{row.original.status}</Badge>
      ),
    },
    {
      id: 'actions',
      header: 'Actions',
      cell: ({ row }) => {
        const r = row.original;
        const busy = busyId === r.id;
        return (
          <div className="flex flex-wrap gap-1 items-center">
            {queue === 'FINANCE' && hasPermission(PERMISSIONS.VERIFY_CHICKEN_SALES) && ['SUBMITTED', 'UNDER_REVIEW'].includes(r.status) && (
              <>
                <Button size="sm" disabled={busy} onClick={() => run(r.id, () => chickenReleaseApi.verify(r.id))}>Verify</Button>
                <Input
                  className="h-8 w-28"
                  placeholder="Reject reason"
                  value={rejectReason[r.id] || ''}
                  onChange={(e) => setRejectReason((m) => ({ ...m, [r.id]: e.target.value }))}
                />
                <Button size="sm" variant="outline" disabled={busy} onClick={() => run(r.id, () => chickenReleaseApi.reject(r.id, { reason: rejectReason[r.id] || 'Rejected' }))}>Reject</Button>
              </>
            )}
            {queue === 'FINANCE' && hasPermission(PERMISSIONS.VERIFY_CHICKEN_SALES) && r.status === 'VERIFIED' && (
              <Button size="sm" disabled={busy} onClick={() => run(r.id, () => chickenReleaseApi.forwardToFarm(r.id))}>Forward to Farm</Button>
            )}
            {queue === 'FARM' && hasPermission(PERMISSIONS.EDIT_FARMS) && ['FARM_PENDING', 'FORWARDED_TO_FARM'].includes(r.status) && (
              <>
                <Button size="sm" disabled={busy} onClick={() => run(r.id, () => chickenReleaseApi.farmApprove(r.id))}>Approve</Button>
                <Input
                  className="h-8 w-28"
                  placeholder="Reject reason"
                  value={rejectReason[r.id] || ''}
                  onChange={(e) => setRejectReason((m) => ({ ...m, [r.id]: e.target.value }))}
                />
                <Button size="sm" variant="outline" disabled={busy} onClick={() => run(r.id, () => chickenReleaseApi.farmReject(r.id, { reason: rejectReason[r.id] || 'Farm rejected' }))}>Reject</Button>
              </>
            )}
            {queue === 'FARM' && hasPermission(PERMISSIONS.EDIT_FARMS) && r.status === 'FARM_APPROVED' && (
              <>
                <Input
                  className="h-8 w-20"
                  type="number"
                  min={0}
                  placeholder="Bonus"
                  value={bonusById[r.id] ?? r.bonusQuantity ?? 0}
                  onChange={(e) => setBonusById((m) => ({ ...m, [r.id]: Number(e.target.value) }))}
                />
                <Button
                  size="sm"
                  disabled={busy}
                  onClick={() => run(r.id, () => chickenReleaseApi.dispatch(r.id, {
                    dispatchDate: new Date().toISOString().slice(0, 10),
                    bonusQuantity: bonusById[r.id] ?? r.bonusQuantity ?? 0,
                    chickenType: r.chickenType,
                    flockId: r.flockId,
                    customerId: r.customerId,
                  }))}
                >
                  Dispatch
                </Button>
              </>
            )}
            {queue === 'MARKETING' && hasPermission(PERMISSIONS.VIEW_CHICKEN_SALES) && r.status === 'DISPATCHED' && (
              <Button size="sm" disabled={busy} onClick={() => run(r.id, () => chickenReleaseApi.marketingReceive(r.id))}>Receive</Button>
            )}
            {queue === 'MARKETING' && hasPermission(PERMISSIONS.VIEW_CHICKEN_SALES) && ['MARKETING_RECEIVED', 'OUT_FOR_DELIVERY'].includes(r.status) && (
              <Button size="sm" disabled={busy} onClick={() => run(r.id, () => chickenReleaseApi.markDelivered(r.id))}>Mark Delivered</Button>
            )}
            <Button size="sm" variant="ghost" onClick={() => openDoc(r.id)}>Print</Button>
          </div>
        );
      },
    },
  ];

  return (
    <EnterprisePage
      title={title}
      description="Payment verification → farm approval → dispatch → marketing (single release record)"
      stats={stats}
      loading={loading}
      error={error}
      onRetry={load}
    >
      <DataTable columns={cols} data={rows} />
    </EnterprisePage>
  );
};

export const PaymentVerificationQueue = () => (
  <ChickenReleaseQueue queue="FINANCE" title="Payment Verification Queue" />
);

export const FarmReleaseApprovalQueue = () => (
  <ChickenReleaseQueue queue="FARM" title="Chicken Release / Farm Approval Queue" />
);

export const MarketingDeliveryQueue = () => (
  <ChickenReleaseQueue queue="MARKETING" title="Marketing Chicken Delivery Queue" />
);

export default ChickenReleaseQueue;
