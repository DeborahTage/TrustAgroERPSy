import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { inventoryApi, materialRequestApi, supplierApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { MASTER_CATALOG_ITEMS } from '../../utils/masterCatalog';

const emptyLine = () => ({ inventoryItemId: '', quantityRequested: '', unit: '', estimatedUnitPrice: '', purpose: '' });

const MaterialRequestForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);
  const [items, setItems] = useState(MASTER_CATALOG_ITEMS);
  const [suppliers, setSuppliers] = useState([]);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    type: 'INTERNAL_ISSUE_REQUEST',
    department: 'Pharmacy / Stock',
    neededBy: '',
    priority: 'NORMAL',
    reason: '',
    notes: '',
    preferredSupplierId: '',
    lines: [emptyLine()],
  });

  useEffect(() => {
    let isMounted = true;
    const fetchOptions = async () => {
      try {
        const [itemRes, supRes] = await Promise.allSettled([
          inventoryApi.getItems(),
          supplierApi.getAll?.() || Promise.resolve({ data: [] }),
        ]);

        if (!isMounted) return;

        // Extract items
        let loadedItems = [];
        if (itemRes.status === 'fulfilled' && itemRes.value?.data) {
          const raw = itemRes.value.data;
          loadedItems = Array.isArray(raw) ? raw : (raw?.data || []);
        }
        if (!loadedItems || loadedItems.length === 0) {
          loadedItems = MASTER_CATALOG_ITEMS;
        }
        setItems(loadedItems);

        // Extract suppliers
        if (supRes.status === 'fulfilled' && supRes.value?.data) {
          const raw = supRes.value.data;
          const loadedSuppliers = Array.isArray(raw) ? raw : (raw?.data || []);
          setSuppliers(loadedSuppliers);
        }
      } catch (err) {
        if (isMounted) {
          setItems(MASTER_CATALOG_ITEMS);
        }
      }
    };

    fetchOptions();
    return () => { isMounted = false; };
  }, []);

  useEffect(() => {
    if (!isEdit) return;
    materialRequestApi.getById(id).then((res) => {
      const d = res.data?.data;
      if (!d) return;
      setForm({
        type: d.type,
        department: d.department || 'Pharmacy / Stock',
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

  const handleItemSelect = (idx, itemId) => {
    const selected = items.find((i) => String(i.id) === String(itemId)) ||
      MASTER_CATALOG_ITEMS.find((i) => String(i.id) === String(itemId));
    const price = selected?.retailPrice != null
      ? String(selected.retailPrice)
      : (selected?.sellingPrice != null
          ? String(selected.sellingPrice)
          : (selected?.avgUnitCost != null ? String(selected.avgUnitCost) : ''));
    const unit = selected?.unit || selected?.unitString || 'PCS';
    setForm((prev) => {
      const lines = [...prev.lines];
      lines[idx] = {
        ...lines[idx],
        inventoryItemId: itemId,
        unit: unit,
        estimatedUnitPrice: price,
      };
      return { ...prev, lines };
    });
  };

  const removeLine = (idx) => {
    if (form.lines.length <= 1) return;
    setForm((prev) => ({
      ...prev,
      lines: prev.lines.filter((_, i) => i !== idx),
    }));
  };

  const payload = () => ({
    type: form.type,
    department: form.department || 'Pharmacy / Stock',
    farmId: null,
    neededBy: form.neededBy || null,
    priority: form.priority,
    reason: form.reason,
    notes: form.notes,
    preferredSupplierId: form.preferredSupplierId && form.preferredSupplierId !== 'NONE' ? Number(form.preferredSupplierId) : null,
    lines: form.lines.map((l) => ({
      inventoryItemId: Number(l.inventoryItemId),
      quantityRequested: Number(l.quantityRequested),
      unit: l.unit || null,
      estimatedUnitPrice: l.estimatedUnitPrice ? Number(l.estimatedUnitPrice) : null,
      purpose: l.purpose || null,
    })),
  });

  const save = async (submitAfter) => {
    setError(null);
    if (!form.lines || form.lines.length === 0 || form.lines.some((l) => !l.inventoryItemId || Number(l.inventoryItemId) <= 0)) {
      setError('Please select an inventory item for all request lines.');
      return;
    }
    if (form.lines.some((l) => !l.quantityRequested || Number(l.quantityRequested) <= 0)) {
      setError('Please enter a valid quantity greater than 0 for all lines.');
      return;
    }
    setSaving(true);
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
    <EnterprisePage title={isEdit ? 'Edit Material Request' : 'New Material Request'} subtitle="Stock & pharmacy issue request">
      {error && <p className="mb-3 text-sm text-rose-600 font-medium">{error}</p>}
      <div className="grid gap-4 max-w-3xl">
        <div className="grid gap-2 sm:grid-cols-2">
          <div>
            <label className="text-sm font-medium">Request Type</label>
            <Select value={form.type} onValueChange={(v) => setForm({ ...form, type: v })}>
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="INTERNAL_ISSUE_REQUEST">Stock Internal Issue (Pharmacy / Store)</SelectItem>
                <SelectItem value="PURCHASE_REQUEST">Purchase Requisition</SelectItem>
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
            <label className="text-sm font-medium">Department / Section</label>
            <Input
              placeholder="e.g. Pharmacy, Central Stock, Finance"
              value={form.department}
              onChange={(e) => setForm({ ...form, department: e.target.value })}
            />
          </div>
          <div>
            <label className="text-sm font-medium">Needed By Date</label>
            <Input type="date" value={form.neededBy} onChange={(e) => setForm({ ...form, neededBy: e.target.value })} />
          </div>
        </div>

        {form.type === 'PURCHASE_REQUEST' && (
          <div>
            <label className="text-sm font-medium">Preferred Supplier</label>
            <Select value={form.preferredSupplierId || 'NONE'} onValueChange={(v) => setForm({ ...form, preferredSupplierId: v === 'NONE' ? '' : v })}>
              <SelectTrigger><SelectValue placeholder="Select Supplier" /></SelectTrigger>
              <SelectContent>
                <SelectItem value="NONE">No Preferred Supplier</SelectItem>
                {suppliers.map((s) => (
                  <SelectItem key={s.id} value={String(s.id)}>{s.name}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        )}

        <div>
          <label className="text-sm font-medium">Reason / Purpose</label>
          <Input
            placeholder="e.g. Pharmacy stock replenishment, vaccine distribution"
            value={form.reason}
            onChange={(e) => setForm({ ...form, reason: e.target.value })}
          />
        </div>
        <div>
          <label className="text-sm font-medium">Notes & Instructions</label>
          <Input
            placeholder="Additional details or special handling requirements"
            value={form.notes}
            onChange={(e) => setForm({ ...form, notes: e.target.value })}
          />
        </div>

        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="font-medium">Requested Items</h3>
            <Button type="button" variant="outline" onClick={() => setForm({ ...form, lines: [...form.lines, emptyLine()] })}>
              Add line
            </Button>
          </div>
          <div className="hidden sm:grid sm:grid-cols-6 gap-2 text-xs font-semibold text-muted-foreground px-3">
            <span className="sm:col-span-2">Item *</span>
            <span>Quantity *</span>
            <span>Unit (Editable)</span>
            <span>Est. Price / Unit (ETB)</span>
            <span>Action</span>
          </div>
          {form.lines.map((line, idx) => (
            <div key={idx} className="grid gap-2 rounded-xl border p-3 sm:grid-cols-6 items-center bg-card">
              <div className="sm:col-span-2 space-y-1">
                <label className="text-xs sm:hidden font-medium text-muted-foreground">Item *</label>
                <Select value={line.inventoryItemId} onValueChange={(v) => handleItemSelect(idx, v)}>
                  <SelectTrigger><SelectValue placeholder="Select Item" /></SelectTrigger>
                  <SelectContent>
                    {items.map((it) => (
                      <SelectItem key={it.id} value={String(it.id)}>
                        {it.itemName} ({it.unit || 'PCS'}) — {it.retailPrice || it.sellingPrice ? `${Number(it.retailPrice || it.sellingPrice).toLocaleString()} ETB` : ''}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1">
                <label className="text-xs sm:hidden font-medium text-muted-foreground">Quantity *</label>
                <Input placeholder="Qty" type="number" min="1" step="any" value={line.quantityRequested} onChange={(e) => setLine(idx, 'quantityRequested', e.target.value)} />
              </div>
              <div className="space-y-1">
                <label className="text-xs sm:hidden font-medium text-muted-foreground">Unit</label>
                <Input placeholder="Unit" value={line.unit} onChange={(e) => setLine(idx, 'unit', e.target.value)} />
              </div>
              <div className="space-y-1">
                <label className="text-xs sm:hidden font-medium text-muted-foreground">Est. Unit Price (ETB)</label>
                <Input placeholder="Est. price" type="number" step="any" value={line.estimatedUnitPrice} onChange={(e) => setLine(idx, 'estimatedUnitPrice', e.target.value)} />
              </div>
              <div className="flex sm:justify-center">
                {form.lines.length > 1 ? (
                  <Button type="button" variant="ghost" size="sm" className="text-rose-600 hover:text-rose-700" onClick={() => removeLine(idx)}>
                    Remove
                  </Button>
                ) : <div />}
              </div>
            </div>
          ))}
        </div>

        <div className="flex gap-2 pt-2">
          <Button disabled={saving} onClick={() => save(false)}>Save draft</Button>
          <Button disabled={saving} variant="secondary" onClick={() => save(true)}>Save & submit</Button>
          <Button variant="outline" onClick={() => navigate('/materials/requests')}>Cancel</Button>
        </div>
      </div>
    </EnterprisePage>
  );
};

export default MaterialRequestForm;
