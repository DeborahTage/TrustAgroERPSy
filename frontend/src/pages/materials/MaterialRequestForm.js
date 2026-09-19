import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { farmApi, inventoryApi, materialRequestApi, supplierApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';

const emptyLine = () => ({ inventoryItemId: '', quantityRequested: '', unit: '', estimatedUnitPrice: '', purpose: '' });

const MaterialRequestForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);
  const [items, setItems] = useState([]);
  const [farms, setFarms] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    type: 'INTERNAL_ISSUE_REQUEST',
    department: '',
    farmId: '',
    neededBy: '',
    priority: 'NORMAL',
    reason: '',
    notes: '',
    preferredSupplierId: '',
    lines: [emptyLine()],
  });

  useEffect(() => {
    Promise.all([
      inventoryApi.getItems(),
      farmApi.getAll?.() || farmApi.list?.() || Promise.resolve({ data: { data: [] } }),
      supplierApi.getAll?.() || supplierApi.list?.() || Promise.resolve({ data: { data: [] } }),
    ]).then(([itemRes, farmRes, supRes]) => {
      setItems(itemRes.data?.data || []);
      setFarms(farmRes.data?.data || farmRes.data || []);
      setSuppliers(supRes.data?.data || supRes.data || []);
    }).catch(() => setError('Failed to load form options'));
  }, []);

  useEffect(() => {
    if (!isEdit) return;
    materialRequestApi.getById(id).then((res) => {
      const d = res.data?.data;
      if (!d) return;
      setForm({
        type: d.type,
        department: d.department || '',
        farmId: d.farmId ? String(d.farmId) : '',
        neededBy: d.neededBy || '',
        priority: d.priority || 'NORMAL',
        reason: d.reason || '',
        notes: d.notes || '',
        preferredSupplierId: d.preferredSupplierId ? String(d.preferredSupplierId) : '',
        lines: (d.lines || []).map((l) => ({
          inventoryItemId: String(l.inventoryItemId),
          quantityRequested: String(l.quantityRequested),
          unit: l.unit || '',
          estimatedUnitPrice: l.estimatedUnitPrice != null ? String(l.estimatedUnitPrice) : '',
          purpose: l.purpose || '',
        })),
      });
    }).catch((e) => setError(e?.response?.data?.message || 'Failed to load request'));
  }, [id, isEdit]);

  const setLine = (idx, key, value) => {
    setForm((prev) => {
      const lines = [...prev.lines];
      lines[idx] = { ...lines[idx], [key]: value };
      return { ...prev, lines };
    });
  };

  const payload = () => ({
    type: form.type,
    department: form.department || null,
    farmId: form.farmId ? Number(form.farmId) : null,
    neededBy: form.neededBy || null,
    priority: form.priority,
    reason: form.reason,
    notes: form.notes,
    preferredSupplierId: form.preferredSupplierId ? Number(form.preferredSupplierId) : null,
    lines: form.lines.map((l) => ({
      inventoryItemId: Number(l.inventoryItemId),
      quantityRequested: Number(l.quantityRequested),
      unit: l.unit || null,
      estimatedUnitPrice: l.estimatedUnitPrice ? Number(l.estimatedUnitPrice) : null,
      purpose: l.purpose || null,
    })),
  });

  const save = async (submitAfter) => {
    setSaving(true);
    setError(null);
    try {
      const body = payload();
      const res = isEdit
        ? await materialRequestApi.update(id, body)
        : await materialRequestApi.create(body);
      const savedId = res.data?.data?.id || id;
      if (submitAfter) {
        await materialRequestApi.submit(savedId);
      }
      navigate(`/materials/requests/${savedId}`);
    } catch (e) {
      setError(e?.response?.data?.message || 'Save failed');
    } finally {
      setSaving(false);
    }
  };

  return (
    <EnterprisePage title={isEdit ? 'Edit Material Request' : 'New Material Request'} subtitle="Draft material request">
      {error && <p className="mb-3 text-sm text-rose-600">{error}</p>}
      <div className="grid gap-4 max-w-3xl">
        <div className="grid gap-2 sm:grid-cols-2">
          <div>
            <label className="text-sm font-medium">Type</label>
            <Select value={form.type} onValueChange={(v) => setForm({ ...form, type: v })}>
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="INTERNAL_ISSUE_REQUEST">Internal issue</SelectItem>
                <SelectItem value="PURCHASE_REQUEST">Purchase</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div>
            <label className="text-sm font-medium">Priority</label>
            <Select value={form.priority} onValueChange={(v) => setForm({ ...form, priority: v })}>
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                {['LOW', 'NORMAL', 'HIGH', 'URGENT'].map((p) => <SelectItem key={p} value={p}>{p}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
        </div>
        <div className="grid gap-2 sm:grid-cols-2">
          <div>
            <label className="text-sm font-medium">Department</label>
            <Input value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })} />
          </div>
          <div>
            <label className="text-sm font-medium">Farm {form.type === 'INTERNAL_ISSUE_REQUEST' ? '(required)' : ''}</label>
            <Select value={form.farmId || 'NONE'} onValueChange={(v) => setForm({ ...form, farmId: v === 'NONE' ? '' : v })}>
              <SelectTrigger><SelectValue placeholder="Select farm" /></SelectTrigger>
              <SelectContent>
                <SelectItem value="NONE">None</SelectItem>
                {farms.map((f) => (
                  <SelectItem key={f.id} value={String(f.id)}>{f.farmName || f.name}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
        {form.type === 'PURCHASE_REQUEST' && (
          <div>
            <label className="text-sm font-medium">Preferred supplier</label>
            <Select value={form.preferredSupplierId || 'NONE'} onValueChange={(v) => setForm({ ...form, preferredSupplierId: v === 'NONE' ? '' : v })}>
              <SelectTrigger><SelectValue placeholder="Supplier" /></SelectTrigger>
              <SelectContent>
                <SelectItem value="NONE">None</SelectItem>
                {suppliers.map((s) => (
                  <SelectItem key={s.id} value={String(s.id)}>{s.name}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        )}
        <div>
          <label className="text-sm font-medium">Needed by</label>
          <Input type="date" value={form.neededBy} onChange={(e) => setForm({ ...form, neededBy: e.target.value })} />
        </div>
        <div>
          <label className="text-sm font-medium">Reason / purpose</label>
          <Input value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} />
        </div>
        <div>
          <label className="text-sm font-medium">Notes</label>
          <Input value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
        </div>

        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="font-medium">Lines</h3>
            <Button type="button" variant="outline" onClick={() => setForm({ ...form, lines: [...form.lines, emptyLine()] })}>
              Add line
            </Button>
          </div>
          {form.lines.map((line, idx) => (
            <div key={idx} className="grid gap-2 rounded border p-3 sm:grid-cols-5">
              <Select value={line.inventoryItemId} onValueChange={(v) => setLine(idx, 'inventoryItemId', v)}>
                <SelectTrigger className="sm:col-span-2"><SelectValue placeholder="Item" /></SelectTrigger>
                <SelectContent>
                  {items.map((it) => (
                    <SelectItem key={it.id} value={String(it.id)}>{it.itemName}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Input placeholder="Qty" value={line.quantityRequested} onChange={(e) => setLine(idx, 'quantityRequested', e.target.value)} />
              <Input placeholder="Unit" value={line.unit} onChange={(e) => setLine(idx, 'unit', e.target.value)} />
              <Input placeholder="Est. price" value={line.estimatedUnitPrice} onChange={(e) => setLine(idx, 'estimatedUnitPrice', e.target.value)} />
            </div>
          ))}
        </div>

        <div className="flex gap-2">
          <Button disabled={saving} onClick={() => save(false)}>Save draft</Button>
          <Button disabled={saving} variant="secondary" onClick={() => save(true)}>Save & submit</Button>
          <Button variant="outline" onClick={() => navigate('/materials/requests')}>Cancel</Button>
        </div>
      </div>
    </EnterprisePage>
  );
};

export default MaterialRequestForm;
