import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { inventoryApi } from '../../api';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { Switch } from '../../components/ui/switch';
import { useLanguage } from '../../context/LanguageContext';
import { toast } from 'react-toastify';
import EnterpriseFormPage from '../../components/layout/EnterpriseFormPage';
import { AlertTriangle, DollarSign } from 'lucide-react';

const InventoryItemForm = () => {
  const { t } = useLanguage();
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);
  const [form, setForm] = useState({ itemName: '', sku: '', category: 'FEED', unit: 'KG', minimumStockLevel: '', reorderPoint: '', reorderQty: '', storageLocation: '', expiryRequired: false, retailPrice: '', wholesalePrice: '', partnerPrice: '', isControlled: false });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const [fetching, setFetching] = useState(isEdit);

  useEffect(() => {
    if (isEdit) {
      setFetching(true);
      inventoryApi.getItem(id).then(r => {
        const i = r.data.data;
        setForm({
          itemName: i.itemName,
          sku: i.sku || '',
          category: i.category,
          unit: i.unit,
          minimumStockLevel: i.minimumStockLevel || '',
          reorderPoint: i.reorderPoint || '',
          reorderQty: i.reorderQty || '',
          storageLocation: i.storageLocation || '',
          expiryRequired: i.expiryRequired,
          retailPrice: i.retailPrice || '',
          wholesalePrice: i.wholesalePrice || '',
          partnerPrice: i.partnerPrice || '',
          isControlled: i.isControlled || false
        });
      }).finally(() => setFetching(false));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, isEdit]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const payload = {
        ...form,
        minimumStockLevel: form.minimumStockLevel ? Number(form.minimumStockLevel) : null,
        reorderPoint: form.reorderPoint ? Number(form.reorderPoint) : null,
        reorderQty: form.reorderQty ? Number(form.reorderQty) : null,
        retailPrice: form.retailPrice ? Number(form.retailPrice) : null,
        wholesalePrice: form.wholesalePrice ? Number(form.wholesalePrice) : null,
        partnerPrice: form.partnerPrice ? Number(form.partnerPrice) : null,
      };
      if (isEdit) await inventoryApi.updateItem(id, payload);
      else await inventoryApi.createItem(payload);
      toast.success(t('inventoryItemForm.success'));
      navigate('/inventory');
    } catch (err) {
      setError(err.response?.data?.message || t('inventoryItemForm.error'));
    } finally { setLoading(false); }
  };

  return (
    <EnterpriseFormPage
      title={isEdit ? t('inventoryItemForm.editTitle') : t('inventoryItemForm.addTitle')}
      subtitle={isEdit ? 'Update inventory item details and settings' : 'Add a new inventory item to the system'}
      breadcrumbs={['Inventory', 'Items', isEdit ? 'Edit' : 'New']}
      backTo="/inventory"
      loading={fetching}
      error={error}
    >
          <form onSubmit={handleSubmit} className="space-y-8">
            {/* Basic Information Section */}
            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-2 md:col-span-2">
                  <Label htmlFor="itemName" className="text-sm font-bold text-gray-700">{t('inventoryItemForm.itemName')} *</Label>
                  <Input
                    id="itemName"
                    value={form.itemName}
                    onChange={e => setForm({ ...form, itemName: e.target.value })}
                    required
                    placeholder="e.g. Broiler Feed - Stage 1"
                    className="h-11 bg-gray-50/50 border-gray-200 focus:bg-white transition-all shadow-sm"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="category" className="text-sm font-bold text-gray-700">{t('inventoryItemForm.category')}</Label>
                  <Select value={form.category} onValueChange={value => setForm({ ...form, category: value })}>
                    <SelectTrigger className="h-11 bg-gray-50/50 border-gray-200">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {['FEED', 'DRUG', 'VACCINE', 'VITAMIN', 'EQUIPMENT', 'FARM_MATERIAL', 'CLEANING_MATERIAL', 'OFFICE_SUPPLY', 'CONSUMABLE', 'PACKAGING'].map(c => (
                        <SelectItem key={c} value={c}>{c.replace('_', ' ')}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="unit" className="text-sm font-bold text-gray-700">{t('inventoryItemForm.unit')}</Label>
                  <Select value={form.unit} onValueChange={value => setForm({ ...form, unit: value })}>
                    <SelectTrigger className="h-11 bg-gray-50/50 border-gray-200">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {['KG', 'SACK', 'BOTTLE', 'VIAL', 'PIECE', 'LITER', 'DOSE', 'BOX'].map(u => (
                        <SelectItem key={u} value={u}>{t(`options.${u}`)}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </div>

            {/* Threshold & Compliance Section */}
            <div className="space-y-6 pt-6 border-t border-dashed">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                <div className="space-y-3">
                  <div className="flex items-center gap-2">
                    <AlertTriangle className="h-4 w-4 text-amber-500" />
                    <Label htmlFor="minimumStockLevel" className="text-sm font-bold text-gray-700">Safety Stock Threshold</Label>
                  </div>
                  <Input
                    id="minimumStockLevel"
                    type="number"
                    min="0"
                    value={form.minimumStockLevel}
                    onChange={e => setForm({ ...form, minimumStockLevel: e.target.value })}
                    placeholder="Alert level"
                    className="h-11 bg-gray-50/50 border-gray-200 shadow-sm"
                  />
                </div>

                <div className="space-y-3">
                  <div className="flex items-center gap-2">
                    <Label htmlFor="sku" className="text-sm font-bold text-gray-700">SKU / Barcode</Label>
                  </div>
                  <Input
                    id="sku"
                    value={form.sku}
                    onChange={e => setForm({ ...form, sku: e.target.value })}
                    placeholder="Auto-generate or scan..."
                    className="h-11 bg-gray-50/50 border-gray-200 shadow-sm"
                  />
                </div>

                <div className="space-y-3">
                  <Label htmlFor="reorderPoint" className="text-sm font-bold text-gray-700">Reorder Point</Label>
                  <Input
                    id="reorderPoint"
                    type="number"
                    min="0"
                    value={form.reorderPoint}
                    onChange={e => setForm({ ...form, reorderPoint: e.target.value })}
                    placeholder="Reorder at..."
                    className="h-11 bg-gray-50/50 border-gray-200 shadow-sm"
                  />
                </div>

                <div className="space-y-3">
                  <Label htmlFor="reorderQty" className="text-sm font-bold text-gray-700">Reorder Quantity</Label>
                  <Input
                    id="reorderQty"
                    type="number"
                    min="0"
                    value={form.reorderQty}
                    onChange={e => setForm({ ...form, reorderQty: e.target.value })}
                    placeholder="Order amount"
                    className="h-11 bg-gray-50/50 border-gray-200 shadow-sm"
                  />
                </div>

                <div className="space-y-3">
                  <Label htmlFor="storageLocation" className="text-sm font-bold text-gray-700">Storage Location</Label>
                  <Input
                    id="storageLocation"
                    value={form.storageLocation}
                    onChange={e => setForm({ ...form, storageLocation: e.target.value })}
                    placeholder="e.g. Cold Room A"
                    className="h-11 bg-gray-50/50 border-gray-200 shadow-sm"
                  />
                </div>

                <div className="space-y-3 p-4 bg-gray-50 rounded-lg border border-gray-100">
                  <div className="flex items-center justify-between">
                    <div className="flex flex-col gap-0.5">
                      <Label htmlFor="expiryRequired" className="text-sm font-bold text-gray-700">Expiry Tracking</Label>
                      <span className="text-[10px] text-gray-500">Require expiry dates for all stock receipts.</span>
                    </div>
                    <Switch
                      id="expiryRequired"
                      checked={form.expiryRequired}
                      onCheckedChange={checked => setForm({ ...form, expiryRequired: checked })}
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* Pricing Section */}
            <div className="space-y-6 pt-6 border-t border-dashed">
              <div className="flex items-center gap-2 mb-2">
                <DollarSign className="h-4 w-4 text-emerald-500" />
                <span className="text-sm font-bold text-gray-700">Commercial Pricing (ETB)</span>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="space-y-2">
                  <Label htmlFor="retailPrice" className="text-sm font-bold text-gray-700">Retail Price *</Label>
                  <Input
                    id="retailPrice"
                    type="number"
                    min="0"
                    step="0.01"
                    value={form.retailPrice}
                    onChange={e => setForm({ ...form, retailPrice: e.target.value })}
                    placeholder="0.00"
                    className="h-11 bg-gray-50/50 border-gray-200 shadow-sm"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="wholesalePrice" className="text-sm font-bold text-gray-700">Wholesale Price</Label>
                  <Input
                    id="wholesalePrice"
                    type="number"
                    min="0"
                    step="0.01"
                    value={form.wholesalePrice}
                    onChange={e => setForm({ ...form, wholesalePrice: e.target.value })}
                    placeholder="0.00"
                    className="h-11 bg-gray-50/50 border-gray-200 shadow-sm"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="partnerPrice" className="text-sm font-bold text-gray-700">Partner Farm Price</Label>
                  <Input
                    id="partnerPrice"
                    type="number"
                    min="0"
                    step="0.01"
                    value={form.partnerPrice}
                    onChange={e => setForm({ ...form, partnerPrice: e.target.value })}
                    placeholder="0.00"
                    className="h-11 bg-gray-50/50 border-gray-200 shadow-sm"
                  />
                </div>
              </div>
              <div className="p-4 bg-gray-50 rounded-lg border border-gray-100">
                <div className="flex items-center justify-between">
                  <div className="flex flex-col gap-0.5">
                    <Label htmlFor="isControlled" className="text-sm font-bold text-gray-700">Controlled Substance</Label>
                    <span className="text-[10px] text-gray-500">Requires a valid prescription to dispense from pharmacy.</span>
                  </div>
                  <Switch
                    id="isControlled"
                    checked={form.isControlled}
                    onCheckedChange={checked => setForm({ ...form, isControlled: checked })}
                  />
                </div>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex items-center justify-end gap-3 pt-6 border-t">
              <Button type="button" variant="ghost" onClick={() => navigate('/inventory')} className="text-gray-500">
                {t('forms.cancel')}
              </Button>
              <Button type="submit" disabled={loading} className="px-8 h-11 bg-blue-600 hover:bg-blue-700 shadow-lg shadow-blue-200">
                {loading ? t('forms.saving') : isEdit ? 'Update Specification' : 'Register New Item'}
              </Button>
            </div>
          </form>
    </EnterpriseFormPage>
  );
};

export default InventoryItemForm;
