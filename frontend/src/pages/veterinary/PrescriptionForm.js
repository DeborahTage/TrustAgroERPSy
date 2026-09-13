import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { crmApi, farmApi, flockApi, inventoryApi, vetApi } from '../../api';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { Textarea } from '../../components/ui/textarea';
import { toast } from 'react-toastify';
import { useLanguage } from '../../context/LanguageContext';
import { ArrowLeft, FileSignature, Hash, User, Package, Clipboard, Info, CheckCircle2, FlaskConical, AlertCircle, Clock, ShieldCheck, Activity, Pill, Syringe, ClipboardList } from 'lucide-react';
import { cn } from '../../lib/utils';
import EnterpriseFormPage from '../../components/layout/EnterpriseFormPage';

const PrescriptionForm = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useLanguage();
  const healthReport = location.state?.healthReport;
  const diseaseCase = location.state?.diseaseCase;

  const [form, setForm] = useState({
    prescriptionNumber: `RX-${Date.now()}`,
    prescriptionType: healthReport?.clientId ? 'EXTERNAL_CLIENT' : 'INTERNAL_FARM',
    drugName: '',
    quantity: '',
    dosageInstruction: healthReport?.treatmentPlan || '',
    farmId: healthReport?.farmId || diseaseCase?.farmId || '',
    flockId: healthReport?.flockId || diseaseCase?.flockId || '',
    clientId: healthReport?.clientId || '',
    diseaseCaseId: diseaseCase?.id || healthReport?.diseaseCaseId || '',
    inventoryItemId: '',
    withdrawalPeriodDays: '',
    administrationRoute: '',
    frequency: '',
    priority: 'NORMAL'
  });

  const [farms, setFarms] = useState([]);
  const [flocks, setFlocks] = useState([]);
  const [clients, setClients] = useState([]);
  const [inventoryItems, setInventoryItems] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    farmApi.getAll().then((r) => setFarms(r.data.data?.filter((farm) => farm.status === 'ACTIVE') || []));
    crmApi.getClients().then((r) => setClients(r.data.data || []));
    inventoryApi.getItems().then((r) => setInventoryItems(r.data.data?.filter((item) => item.status === 'ACTIVE') || []));
  }, []);

  useEffect(() => {
    if (form.farmId) {
      flockApi.getAll().then((r) => setFlocks(r.data.data?.filter((flock) => String(flock.farmId) === String(form.farmId) && flock.status === 'ACTIVE') || []));
    } else {
      setFlocks([]);
    }
  }, [form.farmId]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    if (!form.drugName || !form.quantity) {
      setError('Missing pharmacological data. Please specify the drug and quantity.');
      setLoading(false);
      return;
    }

    try {
      await vetApi.createPrescription({
        ...form,
        quantity: Number(form.quantity),
        farmId: form.farmId ? Number(form.farmId) : null,
        flockId: form.flockId ? Number(form.flockId) : null,
        clientId: form.clientId ? Number(form.clientId) : null,
        diseaseCaseId: form.diseaseCaseId ? Number(form.diseaseCaseId) : null,
        inventoryItemId: form.inventoryItemId ? Number(form.inventoryItemId) : null,
        withdrawalPeriodDays: form.withdrawalPeriodDays ? Number(form.withdrawalPeriodDays) : null,
      });
      toast.success('Prescription issued and logged in pharmacy ledger.');
      navigate('/veterinary');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to authorize prescription.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <EnterpriseFormPage
      title="Clinical Authorization"
      subtitle="Issue medical directives with automated pharmacy and inventory tracking"
      breadcrumbs={['Veterinary', 'Prescription', 'New']}
      backTo="/veterinary"
      error={error}
    >
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        <div className="lg:col-span-2 space-y-8">
          <Card className="border-none shadow-2xl shadow-gray-200/40 rounded-[2.5rem] overflow-hidden bg-white">
            <CardHeader className="bg-gray-50/50 border-b border-gray-100 p-10">
              <div className="flex items-center gap-4 mb-2">
                <div className="h-12 w-12 bg-emerald-900 rounded-2xl flex items-center justify-center">
                  <FlaskConical className="h-6 w-6 text-emerald-300" />
                </div>
                <div>
                  <CardTitle className="text-xl font-black text-gray-900 tracking-tight uppercase">Medical Order Instruction</CardTitle>
                  <CardDescription className="text-xs font-bold text-gray-400">Specify biological agent and target population protocols</CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent className="p-10">
              <form onSubmit={handleSubmit} className="space-y-10">
                {error && (
                  <div className="p-6 text-xs font-black text-rose-700 bg-rose-50 border border-rose-100 rounded-3xl flex items-center gap-4 animate-in slide-in-from-top-2">
                    <AlertCircle className="h-6 w-6" />
                    {error}
                  </div>
                )}

                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Prescription Tier</Label>
                    <Select value={form.prescriptionType} onValueChange={(v) => setForm({ ...form, prescriptionType: v })}>
                      <SelectTrigger className="h-14 bg-white border-2 border-gray-100 rounded-[1.25rem] font-bold focus:ring-[#0f172a] transition-all">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent className="rounded-3xl border-none shadow-2xl">
                        <SelectItem value="INTERNAL_FARM" className="rounded-xl my-1 font-bold">INTERNAL FARM HUB</SelectItem>
                        <SelectItem value="EXTERNAL_CLIENT" className="rounded-xl my-1 font-bold">EXTERNAL CLIENT PORTFOLIO</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">RX Reference ID</Label>
                    <div className="relative">
                      <Hash className="absolute left-5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
                      <Input
                        className="h-14 pl-14 bg-gray-50 border-2 border-transparent rounded-[1.25rem] font-mono font-black text-primary cursor-not-allowed"
                        value={form.prescriptionNumber}
                        readOnly
                      />
                    </div>
                  </div>

                  {form.prescriptionType === 'INTERNAL_FARM' ? (
                    <>
                      <div className="space-y-3">
                        <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Target Zone *</Label>
                        <Select value={String(form.farmId)} onValueChange={(v) => setForm({ ...form, farmId: v, flockId: '' })}>
                          <SelectTrigger className="h-14 bg-white border-2 border-gray-100 rounded-[1.25rem] font-bold focus:ring-[#0f172a] transition-all">
                            <SelectValue placeholder="Select facility" />
                          </SelectTrigger>
                          <SelectContent className="rounded-3xl border-none shadow-2xl">
                            {farms.map((f) => (
                              <SelectItem key={f.id} value={String(f.id)} className="rounded-xl my-1 font-bold">{f.farmName}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                      <div className="space-y-3">
                        <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Flock Identity *</Label>
                        <Select value={String(form.flockId)} onValueChange={(v) => setForm({ ...form, flockId: v })} disabled={!form.farmId}>
                          <SelectTrigger className={cn("h-14 border-2 rounded-[1.25rem] font-bold transition-all", !form.farmId ? "bg-gray-50 border-gray-100" : "bg-white border-gray-100 focus:ring-[#0f172a]")}>
                            <SelectValue placeholder="Select target batch" />
                          </SelectTrigger>
                          <SelectContent className="rounded-3xl border-none shadow-2xl">
                            {flocks.map((f) => (
                              <SelectItem key={f.id} value={String(f.id)} className="rounded-xl my-1 font-bold">{f.batchCode}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                    </>
                  ) : (
                    <div className="space-y-3 md:col-span-2">
                      <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Account Portfolio *</Label>
                      <div className="relative">
                        <User className="absolute left-5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
                        <Select value={String(form.clientId)} onValueChange={(v) => setForm({ ...form, clientId: v })}>
                          <SelectTrigger className="h-14 pl-14 bg-white border-2 border-gray-100 rounded-[1.25rem] font-bold focus:ring-[#0f172a] transition-all">
                            <SelectValue placeholder="Select registered client" />
                          </SelectTrigger>
                          <SelectContent className="rounded-3xl border-none shadow-2xl">
                            {clients.map((c) => (
                              <SelectItem key={c.id} value={String(c.id)} className="rounded-xl my-1 font-bold">{c.clientName}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                    </div>
                  )}

                  <div className="space-y-3 md:col-span-2">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Pharmacy Inventory Link</Label>
                    <Select value={String(form.inventoryItemId)} onValueChange={(v) => {
                      const selected = inventoryItems.find((item) => String(item.id) === String(v));
                      setForm({ ...form, inventoryItemId: v, drugName: selected?.itemName || form.drugName });
                    }}>
                      <SelectTrigger className="h-14 bg-white border-2 border-gray-100 rounded-[1.25rem] font-black focus:ring-[#0f172a] transition-all">
                        <SelectValue placeholder="Link to existing medical stock" />
                      </SelectTrigger>
                      <SelectContent className="rounded-3xl border-none shadow-2xl max-h-[300px]">
                        {inventoryItems.map((item) => (
                          <SelectItem key={item.id} value={String(item.id)} className="rounded-xl my-1 font-bold">
                            <div className="flex justify-between w-full gap-4">
                              <span>{item.itemName}</span>
                              <span className="text-[10px] text-primary bg-primary/10 px-2 py-0.5 rounded-full uppercase tracking-tighter">BAL: {item.currentStock} {item.unit}</span>
                            </div>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Priority Lifecycle *</Label>
                    <Select value={form.priority || 'NORMAL'} onValueChange={(v) => setForm({ ...form, priority: v })}>
                      <SelectTrigger className={cn(
                        "h-14 border-2 rounded-[1.25rem] font-black transition-all",
                        form.priority === 'EMERGENCY' ? "border-rose-200 bg-rose-50 text-rose-700" : "bg-white border-gray-100"
                      )}>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent className="rounded-3xl border-none shadow-2xl">
                        <SelectItem value="NORMAL" className="rounded-xl my-1 font-bold tracking-tight">NORMAL PRIORITY</SelectItem>
                        <SelectItem value="EMERGENCY" className="rounded-xl my-1 font-black text-rose-600 tracking-tight">EMERGENCY / URGENT</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Biological Identity *</Label>
                    <div className="relative">
                      <Package className="absolute left-5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
                      <Input
                        className="h-14 pl-14 bg-white border-2 border-gray-100 rounded-[1.25rem] font-bold focus:ring-[#0f172a] transition-all"
                        value={form.drugName}
                        onChange={(e) => setForm({ ...form, drugName: e.target.value })}
                        placeholder="e.g. Oxytetracycline 20%"
                      />
                    </div>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Dispense Volume *</Label>
                    <Input
                      type="number"
                      min="0.01"
                      step="0.01"
                      className="h-14 bg-white border-2 border-gray-100 rounded-[1.25rem] font-black focus:ring-[#0f172a] transition-all text-primary text-lg"
                      value={form.quantity}
                      onChange={(e) => setForm({ ...form, quantity: e.target.value })}
                      placeholder="0.00"
                    />
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Admin Route *</Label>
                    <Select value={form.administrationRoute || ''} onValueChange={(v) => setForm({ ...form, administrationRoute: v })}>
                      <SelectTrigger className="h-14 bg-white border-2 border-gray-100 rounded-[1.25rem] font-bold focus:ring-[#0f172a] transition-all">
                        <SelectValue placeholder="e.g. Oral, Injection" />
                      </SelectTrigger>
                      <SelectContent className="rounded-3xl border-none shadow-2xl">
                        {['ORAL', 'INJECTION', 'WATER_SOLUBLE', 'FEED_MIX', 'TOPICAL'].map(r => (
                          <SelectItem key={r} value={r} className="rounded-xl my-1 font-bold">{r.replace('_', ' ')}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Withdrawal Days (Meat/Eggs)</Label>
                    <div className="relative">
                      <ShieldCheck className="absolute left-5 top-1/2 -translate-y-1/2 h-5 w-5 text-rose-500" />
                      <Input
                        type="number"
                        min="0"
                        className="h-14 pl-14 bg-rose-50/20 border-rose-100 border-2 rounded-[1.25rem] font-black text-rose-700 focus:ring-rose-600 transition-all text-lg"
                        value={form.withdrawalPeriodDays}
                        onChange={(e) => setForm({ ...form, withdrawalPeriodDays: e.target.value })}
                        placeholder="0"
                      />
                    </div>
                  </div>

                  <div className="space-y-3 md:col-span-2">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Clinical Instruction Set</Label>
                    <div className="relative">
                      <ClipboardList className="absolute left-5 top-5 h-5 w-5 text-gray-400 pointer-events-none" />
                      <Textarea
                        rows={5}
                        className="bg-white border-2 border-gray-100 rounded-[1.5rem] font-medium text-sm focus:ring-[#0f172a] transition-all p-6 pl-14 leading-relaxed"
                        value={form.dosageInstruction}
                        onChange={(e) => setForm({ ...form, dosageInstruction: e.target.value })}
                        placeholder="Specify dosage, route of administration, and withdrawal period protocols..."
                      />
                    </div>
                  </div>
                </div>

                <div className="flex gap-4 pt-4">
                  <Button type="submit" className="flex-[2] h-16 rounded-3xl bg-[#0f172a] hover:bg-[#1e293b] text-white font-black text-base uppercase tracking-widest shadow-2xl shadow-gray-200 transition-all active:scale-95 flex gap-3" disabled={loading}>
                    {loading ? <Activity className="animate-spin h-5 w-5" /> : <FileSignature className="h-5 w-5" />}
                    {loading ? 'Authorizing Order...' : 'Dispatch Prescription'}
                  </Button>
                  <Button type="button" variant="outline" className="flex-1 h-16 rounded-3xl border-2 border-gray-200 font-black text-sm uppercase tracking-widest hover:bg-gray-50 text-gray-400 hover:text-gray-900 transition-all" onClick={() => navigate('/veterinary')}>
                    Discard
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-8">
          <Card className="border-none shadow-2xl shadow-gray-200/40 rounded-[2.5rem] bg-[#0f172a] text-white overflow-hidden p-10 relative">
            <div className="absolute top-0 right-0 p-8 opacity-5">
              <Activity className="h-40 w-40 rotate-12" />
            </div>
            <div className="flex items-center gap-3 mb-8">
              <div className="p-3 bg-white/10 rounded-2xl">
                <ShieldCheck className="h-6 w-6 text-primary/70" />
              </div>
              <span className="text-sm font-black uppercase tracking-[0.2em] text-emerald-300">Compliance Audit</span>
            </div>
            <div className="space-y-8 relative z-10">
              <div className="space-y-3">
                <h4 className="text-base font-black flex items-center gap-2">
                  <CheckCircle2 className="h-5 w-5 text-emerald-400" />
                  Medical Lifecycle
                </h4>
                <p className="text-xs text-emerald-200/70 font-medium leading-relaxed italic">
                  Prescriptions are automatically queued for Pharmacist verification. Inventory deduction occurs only upon final fulfillment validation.
                </p>
              </div>
              <div className="h-px bg-white/10" />
              <div className="space-y-4">
                <p className="text-[10px] font-black uppercase tracking-[0.2em] text-emerald-300 opacity-50">Authorized Effects</p>
                {[
                  { icon: Pill, text: "Fulfillment Queue Sync" },
                  { icon: Syringe, text: "COGS Expense Mapping" },
                  { icon: Clipboard, text: "Withdrawal Period Tracking" }
                ].map((item, i) => (
                  <div key={i} className="flex items-center gap-4 bg-white/5 p-4 rounded-2xl border border-white/5">
                    <item.icon className="h-4 w-4 text-primary/70" />
                    <span className="text-xs font-bold">{item.text}</span>
                  </div>
                ))}
              </div>
            </div>
          </Card>

          <Card className="border-none shadow-2xl shadow-gray-200/40 rounded-[2.5rem] bg-primary/10 p-10 border border-primary/20 group hover:bg-primary/10 transition-all">
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <span className="text-[10px] font-black uppercase tracking-widest text-primary/70">Inventory Status</span>
                <Activity className="h-5 w-5 text-primary/70 group-hover:animate-pulse" />
              </div>
              <p className="text-4xl font-black text-[#0f172a]">Live</p>
              <p className="text-[10px] font-bold text-primary/70 uppercase tracking-widest">Medical SSOT Active</p>
            </div>
          </Card>
        </div>
      </div>
    </EnterpriseFormPage>
  );
};

export default PrescriptionForm;
