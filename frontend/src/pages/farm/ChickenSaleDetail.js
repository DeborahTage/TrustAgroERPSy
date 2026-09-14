import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { chickenSaleApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '../../components/ui/dialog';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatCurrency, formatDate } from '../../utils';
import { toast } from 'react-toastify';

const ChickenSaleDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [sale, setSale] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [payOpen, setPayOpen] = useState(false);
  const [payForm, setPayForm] = useState({ amount: '', paymentMethod: 'CASH', notes: '' });
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await chickenSaleApi.getById(id);
      setSale(res.data.data);
    } catch {
      setError('Failed to load chicken sale.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const run = async (fn, ok) => {
    setBusy(true);
    try {
      await fn();
      toast.success(ok);
      await load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    } finally {
      setBusy(false);
    }
  };

  const pay = async () => {
    await run(
      () => chickenSaleApi.pay(id, {
        amount: Number(payForm.amount),
        paymentMethod: payForm.paymentMethod,
        notes: payForm.notes,
      }),
      'Payment recorded'
    );
    setPayOpen(false);
  };

  const actions = [];
  if (sale?.status === 'DRAFT') {
    if (hasPermission(PERMISSIONS.EDIT_CHICKEN_SALES)) {
      actions.push(<Button key="edit" variant="outline" onClick={() => navigate(`/farm/chicken-sales/${id}/edit`)}>Edit</Button>);
    }
    if (hasPermission(PERMISSIONS.CONFIRM_CHICKEN_SALES)) {
      actions.push(<Button key="confirm" disabled={busy} onClick={() => run(() => chickenSaleApi.confirm(id), 'Sale confirmed')}>Confirm</Button>);
    }
    if (hasPermission(PERMISSIONS.CANCEL_CHICKEN_SALES)) {
      actions.push(<Button key="cancel" variant="destructive" disabled={busy} onClick={() => run(() => chickenSaleApi.cancel(id), 'Sale cancelled')}>Cancel</Button>);
    }
  }
  if (sale?.status === 'CONFIRMED') {
    if (hasPermission(PERMISSIONS.INVOICE_CHICKEN_SALES)) {
      actions.push(<Button key="invoice" disabled={busy} onClick={() => run(() => chickenSaleApi.invoice(id), 'Invoice created')}>Create invoice</Button>);
    }
    if (hasPermission(PERMISSIONS.CANCEL_CHICKEN_SALES)) {
      actions.push(<Button key="cancel" variant="destructive" disabled={busy} onClick={() => run(() => chickenSaleApi.cancel(id), 'Sale cancelled')}>Cancel</Button>);
    }
  }
  if (sale?.status === 'INVOICED' || sale?.status === 'PARTIALLY_PAID') {
    if (hasPermission(PERMISSIONS.PAY_CHICKEN_SALES)) {
      actions.push(<Button key="pay" disabled={busy} onClick={() => { setPayForm({ amount: String(sale.outstandingAmount || ''), paymentMethod: 'CASH', notes: '' }); setPayOpen(true); }}>Record payment</Button>);
    }
  }
  if (sale?.status === 'PAID' && hasPermission(PERMISSIONS.CONFIRM_CHICKEN_SALES)) {
    actions.push(<Button key="complete" disabled={busy} onClick={() => run(() => chickenSaleApi.complete(id), 'Sale completed')}>Complete</Button>);
  }

  return (
    <EnterprisePage
      title={sale ? sale.saleNumber : 'Chicken Sale'}
      subtitle={sale ? `${sale.customerName} · ${sale.flockBatchCode}` : 'Sale detail'}
      breadcrumbs={['Farm', 'Chicken Sales', sale?.saleNumber || 'Detail']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={<div className="flex flex-wrap gap-2">{actions}</div>}
    >
      {sale && (
        <div className="space-y-6">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-sm">
            <div><span className="text-slate-500 block">Status</span><Badge>{sale.status}</Badge></div>
            <div><span className="text-slate-500 block">Type</span>{sale.saleType}</div>
            <div><span className="text-slate-500 block">Date</span>{formatDate(sale.saleDate)}</div>
            <div><span className="text-slate-500 block">Farm / Barn</span>{sale.farmName}{sale.barnName ? ` / ${sale.barnName}` : ''}</div>
            <div><span className="text-slate-500 block">Flock</span>{sale.flockBatchCode} ({sale.flockPopulation?.toLocaleString()} remaining)</div>
            <div><span className="text-slate-500 block">Customer</span>{sale.customerName}</div>
            <div><span className="text-slate-500 block">Created by</span>{sale.createdByName || '—'}</div>
            <div><span className="text-slate-500 block">Confirmed by</span>{sale.confirmedByName || '—'}</div>
          </div>

          <div className="overflow-x-auto rounded-xl border">
            <table className="w-full text-sm">
              <thead className="bg-slate-50"><tr>
                <th className="p-3 text-left">Pricing</th>
                <th className="p-3 text-left">Qty</th>
                <th className="p-3 text-left">Unit / kg</th>
                <th className="p-3 text-left">Weight</th>
                <th className="p-3 text-right">Subtotal</th>
              </tr></thead>
              <tbody>
                {(sale.items || []).map((item) => (
                  <tr key={item.id} className="border-t">
                    <td className="p-3">{item.pricingMethod}</td>
                    <td className="p-3">{item.quantity}</td>
                    <td className="p-3">{formatCurrency(item.unitPrice || item.pricePerKg)}</td>
                    <td className="p-3">{item.weightKg ?? '—'}</td>
                    <td className="p-3 text-right">{formatCurrency(item.lineSubtotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 text-sm">
            <div>Subtotal<br /><strong>{formatCurrency(sale.subtotal)}</strong></div>
            <div>Discount<br /><strong>{formatCurrency(sale.discountAmount)}</strong></div>
            <div>Tax<br /><strong>{formatCurrency(sale.taxAmount)}</strong></div>
            <div>Total<br /><strong>{formatCurrency(sale.totalAmount)}</strong></div>
            <div>Paid / Outstanding<br /><strong>{formatCurrency(sale.paidAmount)} / {formatCurrency(sale.outstandingAmount)}</strong></div>
          </div>

          <div className="rounded-xl border p-4 text-sm">
            <h3 className="font-bold mb-2">Invoice</h3>
            {sale.invoiceNumber ? (
              <p>{sale.invoiceNumber} (#{sale.invoiceId})</p>
            ) : (
              <p className="text-slate-500">Not invoiced yet.</p>
            )}
            <p className="mt-2 text-slate-500">
              Revenue {formatCurrency(sale.revenue)}.
              {sale.profitabilityAvailable
                ? ` Estimated profit ${formatCurrency(sale.profit)} from flock cost allocation.`
                : ` ${sale.profitabilityNote || 'Profitability is not yet fully implemented.'}`}
            </p>
          </div>

          <div className="rounded-xl border p-4 text-sm">
            <h3 className="font-bold mb-2">Payments</h3>
            {(sale.payments || []).length === 0 && <p className="text-slate-500">No payments.</p>}
            {(sale.payments || []).map((p) => (
              <div key={p.id} className="flex justify-between border-t py-2">
                <span>{formatDate(p.paymentDate)} · {p.paymentMethod} · tx {p.financeTransactionId || '—'}</span>
                <strong>{formatCurrency(p.amount)}</strong>
              </div>
            ))}
          </div>

          <div className="rounded-xl border p-4 text-sm">
            <h3 className="font-bold mb-2">Flock population history</h3>
            {(sale.populationEvents || []).length === 0 && <p className="text-slate-500">No flock movement yet (confirm to decrement).</p>}
            {(sale.populationEvents || []).map((e) => (
              <div key={e.id} className="border-t py-2">
                {formatDate(e.eventDate)} · {e.eventType}: {e.previousPopulation.toLocaleString()}
                {' − '}{e.quantityChange.toLocaleString()}{' = '}{e.newPopulation.toLocaleString()}
              </div>
            ))}
          </div>

          {sale.notes && <p className="text-sm text-slate-600">{sale.notes}</p>}
        </div>
      )}

      <Dialog open={payOpen} onOpenChange={setPayOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Record payment</DialogTitle></DialogHeader>
          <div className="space-y-3">
            <div className="space-y-2">
              <Label>Amount</Label>
              <Input type="number" min="0.01" step="0.01" value={payForm.amount} onChange={(e) => setPayForm((f) => ({ ...f, amount: e.target.value }))} />
            </div>
            <div className="space-y-2">
              <Label>Method</Label>
              <Select value={payForm.paymentMethod} onValueChange={(v) => setPayForm((f) => ({ ...f, paymentMethod: v }))}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="CASH">Cash</SelectItem>
                  <SelectItem value="BANK_TRANSFER">Bank transfer</SelectItem>
                  <SelectItem value="MOBILE_MONEY">Mobile money</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setPayOpen(false)}>Close</Button>
            <Button disabled={busy} onClick={pay}>Save payment</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default ChickenSaleDetail;
