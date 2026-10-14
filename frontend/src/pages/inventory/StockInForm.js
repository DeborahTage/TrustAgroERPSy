import React, { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { inventoryApi } from '../../api';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import { Package, Truck, Calendar, Tag, DollarSign, Activity, Save, Info, FileStack } from 'lucide-react';
import { cn } from '../../lib/utils';
import api from '../../api/axios';
import EnterpriseFormPage from '../../components/layout/EnterpriseFormPage';

const StockInForm = () => {
  const { canPerformAction } = useAuth();
  const canStockIn = canPerformAction('stockIn', 'inventoryItems');
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({
    itemId: location.state?.itemId || '',
    batchNumber: '',
    quantity: '',
    unitCost: '',
    supplierId: '',
    poReference: '',
    expiryDate: '',
    dateReceived: new Date().toISOString().split('T')[0]
  });
  const [items, setItems] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    inventoryApi.getItems().then(r => setItems(Array.isArray(r.data) ? r.data : r.data.data || [])).catch(() => {});
    api.get('/api/inventory/suppliers').then(r => setSuppliers(Array.isArray(r.data) ? r.data : r.data.data || [])).catch(() => {});
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!canStockIn) {
      setError('Your account does not have permission to record stock-in. Please log in as a Store Keeper.');
      return;
    }
    if (!form.itemId || !form.quantity || Number(form.quantity) <= 0) {
      setError('Select an item and enter a quantity greater than zero.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await inventoryApi.stockIn({
        ...form,
        itemId: Number(form.itemId),
        quantity: Number(form.quantity),
        unitCost: form.unitCost ? Number(form.unitCost) : null,
        supplierId: form.supplierId ? Number(form.supplierId) : null
      });
      toast.success('Stock-in transaction successfully recorded');
      navigate('/inventory');
    } catch (err) {
      const status = err.response?.status;
      const apiMessage = err.response?.data?.message || err.response?.data?.error;
      if (status === 403) {
        setError(apiMessage || 'Access denied. Stock-in requires the Store Keeper role.');
      } else if (status === 401) {
        setError('Your session has expired. Please log in again.');
      } else {
        setError(apiMessage || 'Failed to process transaction');
      }
    } finally { setLoading(false); }
  };

  return (
    <EnterpriseFormPage
      title="Inventory Stock-In"
      subtitle="Record incoming goods and medications to update warehouse levels"
      breadcrumbs={['Inventory', 'Stock In']}
      backTo="/inventory"
      error={error}
    >
      <form onSubmit={handleSubmit} className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-8">
          <Card className="erp-card overflow-hidden">
            <CardHeader className="border-b border-border/60">
              <CardTitle className="text-lg font-black text-foreground tracking-tight flex items-center gap-2 uppercase">
                <Package className="h-5 w-5 text-emerald-600 dark:text-emerald-400" />
                Receipt Details
              </CardTitle>
              <CardDescription className="text-xs font-bold text-muted-foreground">Core information about the incoming stock</CardDescription>
            </CardHeader>
            <CardContent className="pt-8 space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-3">
                  <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground">Inventory Category</Label>
                  <Select value={form.category || ''} onValueChange={v => setForm({ ...form, category: v, itemId: '' })}>
                    <SelectTrigger className="h-14 bg-card border border-border rounded-xl focus:ring-emerald-500 transition-all font-bold text-foreground">
                      <SelectValue placeholder="Select Category" />
                    </SelectTrigger>
                    <SelectContent className="rounded-2xl border-border shadow-2xl">
                      {['FEED', 'DRUG', 'VACCINE', 'VITAMIN', 'EQUIPMENT', 'FARM_MATERIAL', 'CLEANING_MATERIAL', 'OFFICE_SUPPLY', 'CONSUMABLE'].map(c => (
                        <SelectItem key={c} value={c} className="rounded-xl my-1 font-medium">{c.replace('_', ' ')}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-3">
                  <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground">Inventory Item *</Label>
                  <Select value={String(form.itemId)} onValueChange={v => setForm({ ...form, itemId: v })} disabled={!form.category}>
                    <SelectTrigger className={cn(
                      "h-14 bg-card border border-border rounded-xl focus:ring-emerald-500 transition-all font-bold text-foreground",
                      !form.category && "opacity-50 cursor-not-allowed"
                    )}>
                      <SelectValue placeholder={form.category ? "Select item" : "Select category first"} />
                    </SelectTrigger>
                    <SelectContent className="rounded-2xl border-border shadow-2xl">
                      {items.filter(i => i.category === form.category).map(i => (
                        <SelectItem key={i.id} value={String(i.id)} className="rounded-xl my-1 font-medium">
                          {i.itemName} <span className="text-muted-foreground ml-2 font-normal">({i.unit})</span>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-3">
                  <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground flex items-center gap-1.5">
                    <Tag className="h-3 w-3" /> Batch / Lot Number
                  </Label>
                  <Input
                    className="h-14 bg-card border border-border rounded-xl focus-visible:ring-emerald-500 transition-all font-bold text-foreground"
                    value={form.batchNumber}
                    onChange={e => setForm({ ...form, batchNumber: e.target.value })}
                    placeholder="e.g. BTC-2024-001"
                  />
                </div>

                <div className="space-y-3">
                  <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground flex items-center gap-1.5">
                    <Activity className="h-3 w-3" /> Quantity Received *
                  </Label>
                  <Input
                    type="number"
                    step="0.01"
                    className="h-14 bg-card border border-border rounded-xl focus-visible:ring-emerald-500 transition-all font-bold text-foreground"
                    placeholder="0.00"
                    value={form.quantity}
                    onChange={e => setForm({ ...form, quantity: e.target.value })}
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-3">
                  <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground flex items-center gap-1.5">
                    <Truck className="h-3 w-3" /> Supplier Entity
                  </Label>
                  <Select value={String(form.supplierId)} onValueChange={v => setForm({ ...form, supplierId: v })}>
                    <SelectTrigger className="h-14 bg-card border border-border rounded-xl focus:ring-emerald-500 transition-all font-bold text-foreground">
                      <SelectValue placeholder="Select Vendor" />
                    </SelectTrigger>
                    <SelectContent className="rounded-2xl border-border shadow-2xl">
                      {suppliers.map(s => <SelectItem key={s.id} value={String(s.id)} className="rounded-xl my-1 font-medium">{s.name}</SelectItem>)}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-3">
                  <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground flex items-center gap-1.5">
                    <FileStack className="h-3 w-3" /> PO Reference
                  </Label>
                  <Input
                    className="h-14 bg-card border border-border rounded-xl focus-visible:ring-emerald-500 transition-all font-bold text-foreground"
                    value={form.poReference}
                    onChange={e => setForm({ ...form, poReference: e.target.value })}
                    placeholder="e.g. PO-8821"
                  />
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-8">
          <Card className="erp-card overflow-hidden">
            <CardHeader className="border-b border-border/60">
              <CardTitle className="text-sm font-black text-foreground tracking-tight uppercase">Dates & Valuation</CardTitle>
            </CardHeader>
            <CardContent className="pt-8 space-y-6">
              <div className="space-y-3">
                <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground flex items-center gap-1.5">
                  <DollarSign className="h-3 w-3" /> Unit Cost
                </Label>
                <div className="relative">
                  <span className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground font-bold text-sm">ETB</span>
                  <Input
                    type="number"
                    step="0.01"
                    className="h-14 pl-12 bg-card border border-border rounded-xl focus-visible:ring-emerald-500 transition-all font-bold text-foreground text-lg"
                    placeholder="0.00"
                    value={form.unitCost}
                    onChange={e => setForm({ ...form, unitCost: e.target.value })}
                  />
                </div>
              </div>

              <div className="space-y-3">
                <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground flex items-center gap-1.5">
                  <Calendar className="h-3 w-3" /> Expiry Date
                </Label>
                <Input
                  type="date"
                  className="h-14 bg-card border border-border rounded-xl focus-visible:ring-emerald-500 transition-all font-bold text-foreground"
                  value={form.expiryDate}
                  onChange={e => setForm({ ...form, expiryDate: e.target.value })}
                />
              </div>

              <div className="space-y-3">
                <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground flex items-center gap-1.5">
                  <Calendar className="h-3 w-3" /> Receipt Date
                </Label>
                <Input
                  type="date"
                  className="h-14 bg-card border border-border rounded-xl focus-visible:ring-emerald-500 transition-all font-bold text-foreground"
                  value={form.dateReceived}
                  onChange={e => setForm({ ...form, dateReceived: e.target.value })}
                />
              </div>
            </CardContent>
          </Card>

          <Card className="bg-slate-900 border-none shadow-2xl rounded-3xl overflow-hidden p-1">
            <CardContent className="p-6 space-y-4">
              <div className="bg-slate-800/50 p-4 rounded-2xl border border-slate-700/50">
                <div className="flex gap-3">
                  <div className="p-2 bg-emerald-500/10 rounded-lg">
                    <Info className="h-4 w-4 text-emerald-400" />
                  </div>
                  <div className="space-y-1">
                    <p className="text-xs font-bold text-white uppercase tracking-tight">Data Integrity</p>
                    <p className="text-[10px] text-slate-400 leading-relaxed font-medium">Recording stock-in will immediately update inventory valuations and provide real-time visibility to veterinary staff.</p>
                  </div>
                </div>
              </div>
              <Button type="submit" className="w-full h-14 bg-emerald-600 hover:bg-emerald-700 shadow-emerald-900/20 shadow-xl rounded-2xl text-md font-black tracking-tight" disabled={loading}>
                {loading ? 'Processing...' : (
                  <>
                    <Save className="mr-2 h-5 w-5" />
                    Record Receipt
                  </>
                )}
              </Button>
              <Button type="button" variant="ghost" className="w-full text-slate-400 hover:text-white hover:bg-slate-800 font-bold" onClick={() => navigate('/inventory')}>
                Cancel Transaction
              </Button>
            </CardContent>
          </Card>
        </div>
      </form>
    </EnterpriseFormPage>
  );
};

export default StockInForm;
