import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { chickenSaleApi, crmApi, farmApi, flockApi } from '../../api';
import EnterpriseFormPage from '../../components/layout/EnterpriseFormPage';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { toast } from 'react-toastify';
import { formatCurrency } from '../../utils';

const emptyLine = () => ({
  pricingMethod: 'PER_BIRD',
  quantity: '',
  unitPrice: '',
  weightKg: '',
  pricePerKg: '',
  notes: '',
});

const ChickenSaleForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);
  const [form, setForm] = useState({
    farmId: '',
    barnId: '',
    flockId: '',
    customerId: '',
    saleDate: new Date().toISOString().split('T')[0],
    saleType: 'FARM_GATE',
    discountAmount: '0',
    taxAmount: '0',
    notes: '',
    items: [emptyLine()],
  });
  const [farms, setFarms] = useState([]);
  const [flocks, setFlocks] = useState([]);
  const [barns, setBarns] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(isEdit);

  useEffect(() => {
    farmApi.getAll().then((r) => setFarms(r.data.data?.filter((f) => f.status === 'ACTIVE') || []));
    farmApi.getBarns().then((r) => setBarns(r.data.data || [])).catch(() => setBarns([]));
    crmApi.getClients().then((r) => setCustomers((r.data.data || []).filter((c) => c.status !== 'INACTIVE' && c.status !== 'LOST')));
    flockApi.getAll().then((r) => setFlocks((r.data.data || []).filter((f) => f.status === 'ACTIVE')));
    if (isEdit) {
      setFetching(true);
      chickenSaleApi.getById(id)
        .then((r) => {
          const s = r.data.data;
          setForm({
            farmId: String(s.farmId),
            barnId: s.barnId ? String(s.barnId) : '',
            flockId: String(s.flockId),
            customerId: String(s.customerId),
            saleDate: s.saleDate,
            saleType: s.saleType,
            discountAmount: String(s.discountAmount ?? 0),
            taxAmount: String(s.taxAmount ?? 0),
            notes: s.notes || '',
            items: (s.items || []).map((item) => ({
              pricingMethod: item.pricingMethod,
              quantity: String(item.quantity ?? ''),
              unitPrice: String(item.unitPrice ?? ''),
              weightKg: String(item.weightKg ?? ''),
              pricePerKg: String(item.pricePerKg ?? ''),
              notes: item.notes || '',
            })),
          });
        })
        .catch(() => setError('Failed to load sale.'))
        .finally(() => setFetching(false));
    }
  }, [id, isEdit]);

  const farmFlocks = flocks.filter((f) => !form.farmId || String(f.farmId) === form.farmId);
  const selectedFlock = farmFlocks.find((f) => String(f.id) === form.flockId);
  const farmBarns = barns.filter((b) => !form.farmId || String(b.farmId || b.farm?.id) === form.farmId);
  const available = Number(selectedFlock?.currentBirdCount || 0);

  const totals = useMemo(() => {
    const qty = form.items.reduce((s, i) => s + (Number(i.quantity) || 0), 0);
    const subtotal = form.items.reduce((s, i) => {
      if (i.pricingMethod === 'BY_WEIGHT') {
        return s + (Number(i.weightKg) || 0) * (Number(i.pricePerKg) || 0);
      }
      return s + (Number(i.quantity) || 0) * (Number(i.unitPrice) || 0);
    }, 0);
    const discount = Number(form.discountAmount) || 0;
    const tax = Number(form.taxAmount) || 0;
    return {
      qty,
      subtotal,
      total: Math.max(0, subtotal - discount + tax),
      remaining: available - qty,
    };
  }, [form, available]);

  const updateLine = (idx, patch) => {
    setForm((prev) => ({
      ...prev,
      items: prev.items.map((line, i) => (i === idx ? { ...line, ...patch } : line)),
    }));
  };

  const handleFlock = (flockId) => {
    const flock = flocks.find((f) => String(f.id) === flockId);
    setForm((prev) => ({
      ...prev,
      flockId,
      farmId: flock ? String(flock.farmId) : prev.farmId,
      barnId: flock?.barnId ? String(flock.barnId) : prev.barnId,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const payload = {
        farmId: Number(form.farmId),
        barnId: form.barnId ? Number(form.barnId) : null,
        flockId: Number(form.flockId),
        customerId: Number(form.customerId),
        saleDate: form.saleDate,
        saleType: form.saleType,
        notes: form.notes,
        discountAmount: Number(form.discountAmount) || 0,
        taxAmount: Number(form.taxAmount) || 0,
        items: form.items.map((line) => ({
          flockId: Number(form.flockId),
          pricingMethod: line.pricingMethod,
          quantity: Number(line.quantity),
          unitPrice: line.pricingMethod === 'PER_BIRD' ? Number(line.unitPrice) : null,
          weightKg: line.pricingMethod === 'BY_WEIGHT' ? Number(line.weightKg) : null,
          pricePerKg: line.pricingMethod === 'BY_WEIGHT' ? Number(line.pricePerKg) : null,
          notes: line.notes,
        })),
      };
      if (isEdit) await chickenSaleApi.update(id, payload);
      else await chickenSaleApi.create(payload);
      toast.success(isEdit ? 'Sale updated' : 'Sale created');
      navigate('/farm/chicken-sales');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save sale.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <EnterpriseFormPage
      title={isEdit ? 'Edit Chicken Sale' : 'New Chicken Sale'}
      subtitle="Birds are taken from the selected flock population. Totals are calculated from the lines below."
      breadcrumbs={['Farm', 'Chicken Sales', isEdit ? 'Edit' : 'New']}
      backTo="/farm/chicken-sales"
      loading={fetching}
      error={error}
    >
      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="space-y-2">
            <Label>Farm</Label>
            <Select value={form.farmId || 'none'} onValueChange={(v) => setForm((f) => ({ ...f, farmId: v === 'none' ? '' : v, flockId: '', barnId: '' }))}>
              <SelectTrigger><SelectValue placeholder="Select farm" /></SelectTrigger>
              <SelectContent>
                <SelectItem value="none">Select farm</SelectItem>
                {farms.map((f) => <SelectItem key={f.id} value={String(f.id)}>{f.farmName}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-2">
            <Label>Flock</Label>
            <Select value={form.flockId || 'none'} onValueChange={(v) => handleFlock(v === 'none' ? '' : v)}>
              <SelectTrigger><SelectValue placeholder="Select flock" /></SelectTrigger>
              <SelectContent>
                <SelectItem value="none">Select flock</SelectItem>
                {farmFlocks.map((f) => (
                  <SelectItem key={f.id} value={String(f.id)}>
                    {f.batchCode} — {Number(f.currentBirdCount || 0).toLocaleString()} birds
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-2">
            <Label>Barn</Label>
            <Select value={form.barnId || 'none'} onValueChange={(v) => setForm((f) => ({ ...f, barnId: v === 'none' ? '' : v }))}>
              <SelectTrigger><SelectValue placeholder="Select barn" /></SelectTrigger>
              <SelectContent>
                <SelectItem value="none">None / flock default</SelectItem>
                {farmBarns.map((b) => <SelectItem key={b.id} value={String(b.id)}>{b.name}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-2">
            <Label>Customer</Label>
            <Select value={form.customerId || 'none'} onValueChange={(v) => setForm((f) => ({ ...f, customerId: v === 'none' ? '' : v }))}>
              <SelectTrigger><SelectValue placeholder="Select customer" /></SelectTrigger>
              <SelectContent>
                <SelectItem value="none">Select customer</SelectItem>
                {customers.map((c) => <SelectItem key={c.id} value={String(c.id)}>{c.clientName}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-2">
            <Label>Sale type</Label>
            <Select value={form.saleType} onValueChange={(v) => setForm((f) => ({ ...f, saleType: v }))}>
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                {['RETAIL', 'WHOLESALE', 'B2B', 'FARM_GATE'].map((t) => <SelectItem key={t} value={t}>{t}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-2">
            <Label>Sale date</Label>
            <Input type="date" value={form.saleDate} onChange={(e) => setForm((f) => ({ ...f, saleDate: e.target.value }))} required />
          </div>
        </div>

        <div className="rounded-xl border p-4 bg-slate-50 text-sm grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div><span className="text-slate-500 block">Available flock</span><strong>{available.toLocaleString()}</strong></div>
          <div><span className="text-slate-500 block">Sale quantity</span><strong>{totals.qty.toLocaleString()}</strong></div>
          <div><span className="text-slate-500 block">Remaining after sale</span><strong>{totals.remaining.toLocaleString()}</strong></div>
          <div><span className="text-slate-500 block">Total</span><strong>{formatCurrency(totals.total)}</strong></div>
        </div>

        {form.items.map((line, idx) => (
          <div key={idx} className="grid grid-cols-1 sm:grid-cols-5 gap-3 border rounded-xl p-4">
            <div className="space-y-2">
              <Label>Pricing</Label>
              <Select value={line.pricingMethod} onValueChange={(v) => updateLine(idx, { pricingMethod: v })}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="PER_BIRD">Per bird</SelectItem>
                  <SelectItem value="BY_WEIGHT">By weight</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Quantity (birds)</Label>
              <Input type="number" min="1" value={line.quantity} onChange={(e) => updateLine(idx, { quantity: e.target.value })} required />
            </div>
            {line.pricingMethod === 'PER_BIRD' ? (
              <div className="space-y-2">
                <Label>Unit price (ETB)</Label>
                <Input type="number" min="0" step="0.01" value={line.unitPrice} onChange={(e) => updateLine(idx, { unitPrice: e.target.value })} required />
              </div>
            ) : (
              <>
                <div className="space-y-2">
                  <Label>Weight (kg)</Label>
                  <Input type="number" min="0" step="0.001" value={line.weightKg} onChange={(e) => updateLine(idx, { weightKg: e.target.value })} required />
                </div>
                <div className="space-y-2">
                  <Label>Price / kg</Label>
                  <Input type="number" min="0" step="0.01" value={line.pricePerKg} onChange={(e) => updateLine(idx, { pricePerKg: e.target.value })} required />
                </div>
              </>
            )}
            <div className="space-y-2 sm:col-span-5">
              <Label>Line notes</Label>
              <Input value={line.notes} onChange={(e) => updateLine(idx, { notes: e.target.value })} />
            </div>
          </div>
        ))}

        <Button type="button" variant="outline" onClick={() => setForm((f) => ({ ...f, items: [...f.items, emptyLine()] }))}>
          Add line
        </Button>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="space-y-2"><Label>Discount</Label><Input type="number" min="0" step="0.01" value={form.discountAmount} onChange={(e) => setForm((f) => ({ ...f, discountAmount: e.target.value }))} /></div>
          <div className="space-y-2"><Label>Tax</Label><Input type="number" min="0" step="0.01" value={form.taxAmount} onChange={(e) => setForm((f) => ({ ...f, taxAmount: e.target.value }))} /></div>
          <div className="space-y-2"><Label>Subtotal</Label><Input readOnly value={formatCurrency(totals.subtotal)} /></div>
        </div>
        <div className="space-y-2">
          <Label>Notes</Label>
          <Textarea rows={2} value={form.notes} onChange={(e) => setForm((f) => ({ ...f, notes: e.target.value }))} />
        </div>
        <div className="flex gap-3 pt-4 border-t">
          <Button type="submit" disabled={loading} className="bg-primary hover:bg-primary/90">
            {loading ? 'Saving…' : 'Save draft'}
          </Button>
          <Button type="button" variant="outline" onClick={() => navigate('/farm/chicken-sales')}>Cancel</Button>
        </div>
      </form>
    </EnterpriseFormPage>
  );
};

export default ChickenSaleForm;
