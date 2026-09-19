import React, { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { materialRequestApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Input } from '../../components/ui/input';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatDate } from '../../utils';

const MaterialRequestDetail = () => {
  const { id } = useParams();
  const { hasPermission } = useAuth();
  const [row, setRow] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [reason, setReason] = useState('');
  const [disburseAmount, setDisburseAmount] = useState('');
  const [issueQtys, setIssueQtys] = useState({});

  const load = useCallback(async () => {
    setError(null);
    try {
      const res = await materialRequestApi.getById(id);
      setRow(res.data?.data || null);
    } catch (e) {
      setError(e?.response?.data?.message || 'Failed to load');
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const run = async (fn) => {
    setBusy(true);
    setError(null);
    try {
      await fn();
      await load();
    } catch (e) {
      setError(e?.response?.data?.message || 'Action failed');
    } finally {
      setBusy(false);
    }
  };

  const openDoc = async (type) => {
    const res = await materialRequestApi.documentHtml(type, id);
    const w = window.open('', '_blank');
    if (w) {
      w.document.write(typeof res.data === 'string' ? res.data : (res.data?.data || ''));
      w.document.close();
    }
  };

  if (!row) {
    return (
      <EnterprisePage title="Material Request">
        {error ? <p className="text-rose-600">{error}</p> : <p>Loading…</p>}
      </EnterprisePage>
    );
  }

  const timeline = [
    { label: 'Created', value: formatDate(row.createdAt) },
    { label: 'Submitted / Finance', value: row.status },
    { label: 'Finance approved', value: row.financeApprovedAt ? `${row.financeApprovedByName} @ ${formatDate(row.financeApprovedAt)}` : '—' },
    { label: 'PO / Requisition', value: row.purchaseOrderId ? `PO #${row.purchaseOrderId}` : (row.procRequisitionId ? `REQ #${row.procRequisitionId}` : '—') },
    { label: 'Paid', value: row.paidAmount != null ? String(row.paidAmount) : '—' },
    { label: 'Acknowledged', value: row.acknowledgedAt ? `${row.acknowledgedByName} @ ${formatDate(row.acknowledgedAt)}` : '—' },
  ];

  return (
    <EnterprisePage
      title={row.requestNumber}
      subtitle={`${row.type} · ${row.status}`}
      actions={
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => openDoc('MATERIAL_REQUEST')}>Print</Button>
          {row.status === 'DRAFT' || row.status === 'RETURNED' ? (
            <Button asChild variant="secondary"><Link to={`/materials/requests/${id}/edit`}>Edit</Link></Button>
          ) : null}
        </div>
      }
    >
      {error && <p className="mb-3 text-sm text-rose-600">{error}</p>}
      <div className="mb-4 flex flex-wrap gap-2">
        <Badge>{row.status}</Badge>
        <Badge variant="outline">{row.priority}</Badge>
        {row.farmName && <Badge variant="outline">{row.farmName}</Badge>}
      </div>

      <div className="mb-6 grid gap-2 rounded border p-4 text-sm sm:grid-cols-2">
        <div>Requester: <strong>{row.requesterName}</strong></div>
        <div>Department: <strong>{row.department || '—'}</strong></div>
        <div>Request date: <strong>{formatDate(row.requestDate)}</strong></div>
        <div>Needed by: <strong>{formatDate(row.neededBy)}</strong></div>
        <div>Reason: <strong>{row.reason || '—'}</strong></div>
        <div>Estimated / Approved / Paid: <strong>{row.estimatedTotal} / {row.approvedAmount} / {row.paidAmount}</strong></div>
      </div>

      <h3 className="mb-2 font-medium">Timeline</h3>
      <ol className="mb-6 space-y-2 border-l pl-4 text-sm">
        {timeline.map((t) => (
          <li key={t.label}><span className="text-slate-500">{t.label}:</span> {t.value}</li>
        ))}
      </ol>

      <h3 className="mb-2 font-medium">Lines</h3>
      <div className="mb-6 overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b text-left">
              <th className="py-2">Item</th>
              <th>Requested</th>
              <th>Approved</th>
              <th>Received</th>
              <th>Issued</th>
              <th>Issue qty</th>
            </tr>
          </thead>
          <tbody>
            {(row.lines || []).map((l) => (
              <tr key={l.id} className="border-b">
                <td className="py-2">{l.itemName}</td>
                <td>{l.quantityRequested}</td>
                <td>{l.quantityApproved}</td>
                <td>{l.quantityReceived}</td>
                <td>{l.quantityIssued}</td>
                <td>
                  <Input
                    className="w-24"
                    value={issueQtys[l.id] ?? ''}
                    onChange={(e) => setIssueQtys({ ...issueQtys, [l.id]: e.target.value })}
                    placeholder="qty"
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex flex-wrap gap-2">
        {(row.status === 'DRAFT' || row.status === 'RETURNED') && hasPermission(PERMISSIONS.SUBMIT_MATERIAL_REQUESTS) && (
          <Button disabled={busy} onClick={() => run(() => materialRequestApi.submit(id))}>Submit</Button>
        )}
        {(row.status === 'FINANCE_REVIEW' || row.status === 'SUBMITTED') && hasPermission(PERMISSIONS.APPROVE_MATERIAL_REQUESTS) && (
          <>
            <Button disabled={busy} onClick={() => run(() => materialRequestApi.financeApprove(id, {}))}>Finance approve</Button>
            <Input className="w-56" placeholder="Reject/return reason" value={reason} onChange={(e) => setReason(e.target.value)} />
            <Button disabled={busy} variant="destructive" onClick={() => run(() => materialRequestApi.financeReject(id, { reason }))}>Reject</Button>
            <Button disabled={busy} variant="outline" onClick={() => run(() => materialRequestApi.returnForCorrection(id, { reason }))}>Return</Button>
          </>
        )}
        {row.type === 'PURCHASE_REQUEST' && row.status === 'APPROVED' && hasPermission(PERMISSIONS.EDIT_MATERIAL_REQUESTS) && (
          <Button disabled={busy} onClick={() => run(() => materialRequestApi.authorizePurchase(id, { supplierId: row.preferredSupplierId }))}>
            Authorize purchase
          </Button>
        )}
        {row.type === 'PURCHASE_REQUEST' && ['APPROVED', 'PURCHASE_AUTHORIZED', 'PAYMENT_SENT'].includes(row.status)
          && hasPermission(PERMISSIONS.APPROVE_MATERIAL_REQUESTS) && (
          <>
            <Input className="w-32" placeholder="Amount" value={disburseAmount} onChange={(e) => setDisburseAmount(e.target.value)} />
            <Button disabled={busy} onClick={() => run(() => materialRequestApi.disburse(id, {
              amount: Number(disburseAmount),
              supplierId: row.preferredSupplierId,
              paymentMethod: 'CASH',
            }))}>Record disbursement</Button>
          </>
        )}
        {row.type === 'PURCHASE_REQUEST' && hasPermission(PERMISSIONS.RECEIVE_MATERIAL_REQUESTS) && (
          <Button disabled={busy} onClick={() => run(() => materialRequestApi.goodsReceipt(id, {
            lines: (row.lines || []).map((l) => ({
              lineId: l.id,
              quantityReceived: Number(issueQtys[l.id] || l.quantityApproved || l.quantityRequested),
              unitCost: l.estimatedUnitPrice,
            })),
          }))}>Register goods receipt</Button>
        )}
        {row.type === 'INTERNAL_ISSUE_REQUEST' && hasPermission(PERMISSIONS.FULFILL_MATERIAL_REQUESTS)
          && ['READY_FOR_STORE', 'APPROVED', 'PARTIALLY_FULFILLED'].includes(row.status) && (
          <Button disabled={busy} onClick={() => run(() => materialRequestApi.issue(id, {
            lines: (row.lines || [])
              .filter((l) => issueQtys[l.id])
              .map((l) => ({ lineId: l.id, quantityIssued: Number(issueQtys[l.id]) })),
          }))}>Issue from store</Button>
        )}
        {hasPermission(PERMISSIONS.VIEW_MATERIAL_REQUESTS) && ['ISSUED', 'PARTIALLY_FULFILLED', 'FULFILLED'].includes(row.status) && (
          <Button disabled={busy} variant="secondary" onClick={() => run(() => materialRequestApi.acknowledge(id, { notes: 'Acknowledged' }))}>
            Acknowledge
          </Button>
        )}
        {hasPermission(PERMISSIONS.EDIT_MATERIAL_REQUESTS) && (
          <Button disabled={busy} variant="outline" onClick={() => run(() => materialRequestApi.cancel(id))}>Cancel</Button>
        )}
      </div>
    </EnterprisePage>
  );
};

export default MaterialRequestDetail;
