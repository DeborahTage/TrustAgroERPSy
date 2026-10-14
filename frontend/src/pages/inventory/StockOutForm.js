import React, { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { farmApi, inventoryApi } from '../../api';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import { ArrowDownRight, Building2, User, FileText, Calendar, Box, Tag, Save, Info, Layers, Stethoscope, Syringe } from 'lucide-react';
import { cn } from '../../lib/utils';
import api from '../../api/axios';
import EnterpriseFormPage from '../../components/layout/EnterpriseFormPage';

const StockOutForm = () => {
  const { canPerformAction } = useAuth();
  const canStockOut = canPerformAction('stockOut', 'inventoryItems');
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({
    itemId: location.state?.itemId || '',
    quantity: '',
    reason: '',
    issuedToType: 'INTERNAL',
    farmId: '',
    department: '',
    referenceType: '',
    referenceId: '',
    movementDate: new Date().toISOString().split('T')[0],
    prescriptionId: '',
    vaccinationId: '',
  });
  const [items, setItems] = useState([]);
  const [farms, setFarms] = useState([]);
  const [prescriptions, setPrescriptions] = useState([]);
  const [vaccinations, setVaccinations] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    inventoryApi.getItems().then(r => setItems(Array.isArray(r.data) ? r.data : r.data.data || [])).catch(() => {});
    farmApi.getAll().then(r => setFarms(r.data.data || []));
    // Fetch prescriptions and vaccinations for medical linking
    api.get('/api/vet/prescriptions').then(r => setPrescriptions(r.data.data || []));
    api.get('/api/vet/vaccinations').then(r => setVaccinations(r.data.data || []));
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!canStockOut) {
      setError('Your account does not have permission to record stock-out. Please log in as a Store Keeper.');
      return;
    }
    if (!form.itemId || !form.quantity || Number(form.quantity) <= 0) {
      setError('Select an item and enter a quantity greater than zero.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await inventoryApi.stockOut({
        ...form,
        itemId: Number(form.itemId),
        quantity: Number(form.quantity),
        reason: form.reason?.trim() || 'ISSUE',
        farmId: form.farmId ? Number(form.farmId) : null,
        referenceId: form.referenceId ? Number(form.referenceId) : null,
        department: form.department || null,
        referenceType: form.referenceType || null,
        prescriptionId: form.prescriptionId ? Number(form.prescriptionId) : null,
        vaccinationId: form.vaccinationId ? Number(form.vaccinationId) : null,
      });
      toast.success('Stock-out transaction successfully recorded');
      navigate('/inventory');
    } catch (err) {
      const status = err.response?.status;
      const apiMessage = err.response?.data?.message || err.response?.data?.error;
      if (status === 403) {
        setError(apiMessage || 'Access denied. Stock-out requires the Store Keeper role.');
      } else if (status === 401) {
        setError('Your session has expired. Please log in again.');
      } else if (status === 400) {
        setError(apiMessage || 'Invalid stock-out request. Check quantity and available stock.');
      } else if (status === 404) {
        setError(apiMessage || 'Inventory item not found. Please refresh and select the item again.');
      } else {
        setError(apiMessage || 'Failed to process transaction');
      }
    } finally { setLoading(false); }
  };

  return (
    <EnterpriseFormPage
      title="Inventory Stock-Out"
      subtitle="Record stock issuance for farms, departments, or internal use"
      breadcrumbs={['Inventory', 'Stock Out']}
      backTo="/inventory"
      error={error}
    >
      <form onSubmit={handleSubmit} className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-8">
          <Card className="erp-card overflow-hidden rounded-2xl">
            <CardHeader className="bg-muted/40 border-b border-border/50">
              <CardTitle className="text-lg font-black text-foreground tracking-tight flex items-center gap-2 uppercase">
                <ArrowDownRight className="h-5 w-5 text-rose-600 dark:text-rose-400" />
                Issuance Details
              </CardTitle>
              <CardDescription className="text-xs font-bold text-muted-foreground">Specify what and how much is being issued</CardDescription>
            </CardHeader>
            <CardContent className="pt-8 space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-3">
                  <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground">Inventory Category</Label>
                  <Select value={form.category || ''} onValueChange={v => setForm({ ...form, category: v, itemId: '' })}>
                    <SelectTrigger className="h-14 bg-card border border-border rounded-xl focus:ring-rose-500 transition-all font-bold text-foreground">
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
                      "h-14 bg-card border border-border rounded-xl focus:ring-rose-500 transition-all font-bold text-foreground",
                      !form.category && "opacity-50 cursor-not-allowed"
                    )}>
                      <SelectValue placeholder={form.category ? "Select item to issue" : "Select category first"} />
                    </SelectTrigger>
                    <SelectContent className="rounded-2xl border-border shadow-2xl">
                      {items.filter(i => i.category === form.category).map(i => (
                        <SelectItem key={i.id} value={String(i.id)} className="rounded-xl my-1 font-medium">
                          {i.itemName} <span className="text-muted-foreground ml-2 font-normal">(Left: {i.currentStock} {i.unit})</span>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-3">
                  <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground flex items-center gap-1.5">
                    <Box className="h-3 w-3" /> Quantity to Issue *
                  </Label>
                  <Input
                    type="number"
                    step="0.01"
                    className="h-14 bg-card border border-border rounded-xl focus-visible:ring-rose-500 focus-visible:border-rose-500 transition-all font-bold text-foreground"
                    placeholder="0.00"
                    value={form.quantity}
                    onChange={e => setForm({ ...form, quantity: e.target.value })}
                    required
                  />
                </div>

                <div className="space-y-3">
                  <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground flex items-center gap-1.5">
                    <Layers className="h-3 w-3" /> Transaction Entity
                  </Label>
                  <Select value={form.issuedToType} onValueChange={v => setForm({ ...form, issuedToType: v, farmId: '', department: '' })}>
                    <SelectTrigger className="h-14 bg-card border border-border rounded-xl focus:ring-rose-500 focus:border-rose-500 transition-all font-bold text-foreground">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent className="rounded-2xl border-border shadow-2xl">
                      {['FARM', 'DEPARTMENT', 'CUSTOMER', 'INTERNAL', 'MEDICAL'].map(o => (
                        <SelectItem key={o} value={o} className="rounded-xl my-1 font-medium">Issue to {o}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="space-y-6">
                {form.issuedToType === 'FARM' && (
                  <div className="space-y-3 animate-in fade-in slide-in-from-top-2">
                    <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground flex items-center gap-1.5">
                      <Building2 className="h-3 w-3" /> Target Farm *
                    </Label>
                    <Select value={String(form.farmId)} onValueChange={v => setForm({ ...form, farmId: v })}>
                      <SelectTrigger className="h-14 bg-card border border-border rounded-xl focus:ring-rose-500 focus:border-rose-500 transition-all font-bold text-foreground">
                        <SelectValue placeholder="Select farm location" />
                      </SelectTrigger>
                      <SelectContent className="rounded-2xl border-border shadow-2xl">
                        {farms.map(f => <SelectItem key={f.id} value={String(f.id)} className="rounded-xl my-1 font-medium">{f.farmName}</SelectItem>)}
                      </SelectContent>
                    </Select>
                  </div>
                )}

                {form.issuedToType === 'DEPARTMENT' && (
                  <div className="space-y-3 animate-in fade-in slide-in-from-top-2">
                    <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground flex items-center gap-1.5">
                      <User className="h-3 w-3" /> Department Name *
                    </Label>
                    <Input
                      className="h-14 bg-card border border-border rounded-xl focus-visible:ring-rose-500 focus-visible:border-rose-500 transition-all font-bold text-foreground"
                      value={form.department}
                      onChange={e => setForm({ ...form, department: e.target.value })}
                      placeholder="e.g. Broiler Section, Veterinary"
                    />
                  </div>
                )}

                {form.issuedToType === 'MEDICAL' && (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6 animate-in fade-in slide-in-from-top-2">
                    <div className="space-y-3">
                      <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground flex items-center gap-1.5">
                        <Stethoscope className="h-3 w-3" /> Linked Prescription
                      </Label>
                      <Select value={String(form.prescriptionId)} onValueChange={v => setForm({ ...form, prescriptionId: v, vaccinationId: '' })}>
                        <SelectTrigger className="h-14 bg-card border border-border rounded-xl font-bold text-foreground">
                          <SelectValue placeholder="Select Prescription" />
                        </SelectTrigger>
                        <SelectContent className="rounded-2xl border-border shadow-2xl">
                          {prescriptions.map(p => <SelectItem key={p.id} value={String(p.id)}>{p.drugName} (Prescribed: {p.prescriptionDate})</SelectItem>)}
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="space-y-3">
                      <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground flex items-center gap-1.5">
                        <Syringe className="h-3 w-3" /> Linked Vaccination
                      </Label>
                      <Select value={String(form.vaccinationId)} onValueChange={v => setForm({ ...form, vaccinationId: v, prescriptionId: '' })}>
                        <SelectTrigger className="h-14 bg-card border border-border rounded-xl font-bold text-foreground">
                          <SelectValue placeholder="Select Vaccination Task" />
                        </SelectTrigger>
                        <SelectContent className="rounded-2xl border-border shadow-2xl">
                          {vaccinations.map(v => <SelectItem key={v.id} value={String(v.id)}>{v.vaccineName} (Due: {v.scheduledDate})</SelectItem>)}
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>

          <Card className="erp-card overflow-hidden rounded-2xl">
            <CardHeader className="bg-muted/40 border-b border-border/50">
              <CardTitle className="text-sm font-black text-foreground tracking-tight uppercase">Audit & Reference</CardTitle>
            </CardHeader>
            <CardContent className="pt-8 space-y-6">
              <div className="space-y-3">
                <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground flex items-center gap-1.5">
                  <FileText className="h-3 w-3" /> Reason / Purpose
                </Label>
                <Input
                  className="h-14 bg-card border border-border rounded-xl focus-visible:ring-rose-500 focus-visible:border-rose-500 transition-all font-bold text-foreground"
                  value={form.reason}
                  onChange={e => setForm({ ...form, reason: e.target.value })}
                  placeholder="e.g. Scheduled vaccination, Emergency repair"
                />
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-8">
          <Card className="erp-card overflow-hidden rounded-2xl">
            <CardHeader className="bg-muted/40 border-b border-border/50">
              <CardTitle className="text-sm font-black text-foreground tracking-tight uppercase">Timeline & Log</CardTitle>
            </CardHeader>
            <CardContent className="pt-8 space-y-6">
              <div className="space-y-3">
                <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground flex items-center gap-1.5">
                  <Calendar className="h-3 w-3" /> Transaction Date
                </Label>
                <Input
                  type="date"
                  className="h-14 bg-card border border-border rounded-xl focus-visible:ring-rose-500 focus-visible:border-rose-500 transition-all font-bold text-foreground"
                  value={form.movementDate}
                  onChange={e => setForm({ ...form, movementDate: e.target.value })}
                />
              </div>

              <div className="space-y-3">
                <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground flex items-center gap-1.5">
                  <Tag className="h-3 w-3" /> External Reference
                </Label>
                <Input
                  className="h-14 bg-card border border-border rounded-xl focus-visible:ring-rose-500 focus-visible:border-rose-500 transition-all font-bold text-foreground"
                  value={form.referenceType}
                  onChange={e => setForm({ ...form, referenceType: e.target.value })}
                  placeholder="Requisition ID, Voucher #"
                />
              </div>
            </CardContent>
          </Card>

          <Card className="erp-card bg-slate-900 border-none shadow-2xl rounded-2xl overflow-hidden p-1 text-white">
            <CardContent className="p-6 space-y-4">
              <div className="bg-slate-800/50 p-4 rounded-xl border border-slate-700/50">
                <div className="flex gap-3">
                  <div className="p-2 bg-rose-500/10 rounded-lg">
                    <Info className="h-4 w-4 text-rose-400" />
                  </div>
                  <div className="space-y-1">
                    <p className="text-xs font-bold text-white uppercase tracking-tight">System Notice</p>
                    <p className="text-[10px] text-slate-400 leading-relaxed font-medium">Issuing stock will reduce warehouse inventory balance. This operation is logged for financial auditing.</p>
                  </div>
                </div>
              </div>
              <Button type="submit" className="w-full h-14 bg-rose-600 hover:bg-rose-700 shadow-rose-900/20 shadow-xl rounded-xl text-md font-black tracking-tight" disabled={loading}>
                {loading ? 'Processing...' : (
                  <>
                    <Save className="mr-2 h-5 w-5" />
                    Verify & Issue
                  </>
                )}
              </Button>
              <Button type="button" variant="ghost" className="w-full text-slate-400 hover:text-white hover:bg-slate-800 font-bold rounded-xl" onClick={() => navigate('/inventory')}>
                Cancel Operation
              </Button>
            </CardContent>
          </Card>
        </div>
      </form>
    </EnterpriseFormPage>
  );
};

export default StockOutForm;
