import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { vetApi, farmApi, flockApi } from '../../api';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { Textarea } from '../../components/ui/textarea';
import { toast } from 'react-toastify';
import { useLanguage } from '../../context/LanguageContext';
import { ArrowLeft, Activity, Calendar, AlertTriangle, Users, BookOpen, ShieldAlert, Microscope, Info, CheckCircle2, Link } from 'lucide-react';
import { cn } from '../../lib/utils';

const DiseaseCaseForm = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useLanguage();
  const healthReport = location.state?.healthReport;

  const [form, setForm] = useState({
    farmId: healthReport?.farmId || '',
    flockId: healthReport?.flockId || '',
    dateDetected: healthReport?.reportDate || '',
    symptoms: healthReport?.symptoms || '',
    suspectedDisease: healthReport?.suspectedDiagnosis || '',
    numberAffected: healthReport?.numberAffected ?? '',
    numberDead: healthReport?.mortalityObserved ?? '',
    severity: healthReport?.severity || 'LOW',
    attachmentUrl: ''
  });

  const [farms, setFarms] = useState([]);
  const [flocks, setFlocks] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    farmApi.getAll().then((r) => setFarms(r.data.data?.filter((farm) => farm.status === 'ACTIVE') || []));
    if (form.farmId) {
      flockApi.getAll().then((r) => setFlocks(r.data.data?.filter((flock) => String(flock.farmId) === String(form.farmId) && flock.status === 'ACTIVE') || []));
    }
  }, [form.farmId]);

  const handleFarmChange = (farmId) => {
    setForm((prev) => ({ ...prev, farmId, flockId: '' }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    if (!form.farmId || !form.flockId || !form.suspectedDisease) {
      setError('Missing critical epidemiology data. Please complete required fields.');
      setLoading(false);
      return;
    }

    try {
      const res = await vetApi.createDiseaseCase({
        ...form,
        farmId: Number(form.farmId),
        flockId: Number(form.flockId),
        numberAffected: form.numberAffected ? Number(form.numberAffected) : null,
        numberDead: form.numberDead ? Number(form.numberDead) : null,
        attachmentUrl: form.attachmentUrl
      });

      if (healthReport?.id) {
        await vetApi.reviewHealthReport(healthReport.id, {
          suspectedDiagnosis: form.suspectedDisease,
          severity: form.severity,
          treatmentPlan: healthReport.treatmentPlan,
          status: 'REVIEWED',
          diseaseCaseId: res.data.data?.id,
        });
      }

      toast.success('Disease case recorded successfully in audit trail.');
      navigate('/veterinary');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to register clinical case.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-10 max-w-5xl mx-auto pb-16 animate-in fade-in duration-700">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="flex items-center gap-6">
          <Button
            variant="ghost"
            size="icon"
            className="h-12 w-12 rounded-2xl bg-white shadow-sm border border-gray-100 hover:bg-gray-50 group transition-all"
            onClick={() => navigate('/veterinary')}
          >
            <ArrowLeft className="h-5 w-5 text-gray-400 group-hover:text-gray-900 transition-colors" />
          </Button>
          <div>
            <h1 className="text-3xl font-black tracking-tight text-gray-900 flex items-center gap-3">
              Outbreak Record
              <div className="px-3 py-1 bg-rose-50 text-rose-700 text-[10px] font-black uppercase tracking-widest rounded-full border border-rose-100 flex items-center gap-1.5">
                <Activity className="h-3 w-3" />
                Clinical Entry
              </div>
            </h1>
            <p className="text-sm font-bold text-gray-400 mt-1 uppercase tracking-widest">Epidemiological surveillance & targeted intervention</p>
          </div>
        </div>
        <div className="h-px flex-1 bg-gray-100 hidden md:block mx-8" />
        <div className="flex items-center gap-3 bg-gray-50 px-4 py-2 rounded-2xl border border-gray-100">
          <div className="h-2 w-2 bg-emerald-500 rounded-full animate-pulse" />
          <span className="text-[10px] font-black uppercase text-gray-500">Secure Audit Active</span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        <div className="lg:col-span-2 space-y-8">
          <Card className="border-none shadow-2xl shadow-gray-200/40 rounded-[2.5rem] overflow-hidden bg-white">
            <CardHeader className="bg-gray-50/50 border-b border-gray-100 p-8">
              <CardTitle className="text-lg font-black text-gray-900 tracking-tight flex items-center gap-2 uppercase">
                <Microscope className="h-5 w-5 text-rose-600" />
                Pathological Assessment
              </CardTitle>
              <CardDescription className="text-xs font-bold text-gray-400">Formal registration of observed clinical manifestations</CardDescription>
            </CardHeader>
            <CardContent className="p-8">
              <form onSubmit={handleSubmit} className="space-y-8">
                {error && (
                  <div className="p-4 text-xs font-black text-rose-700 bg-rose-50 border border-rose-100 rounded-2xl flex items-center gap-3 animate-in slide-in-from-top-2">
                    <AlertTriangle className="h-5 w-5" />
                    {error}
                  </div>
                )}

                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Farm Venue *</Label>
                    <Select value={String(form.farmId)} onValueChange={handleFarmChange}>
                      <SelectTrigger className="h-12 bg-white border-2 border-gray-100 rounded-2xl font-bold focus:ring-rose-600 transition-all">
                        <SelectValue placeholder="Select target farm" />
                      </SelectTrigger>
                      <SelectContent className="rounded-2xl border-none shadow-2xl">
                        {farms.map((f) => (
                          <SelectItem key={f.id} value={String(f.id)} className="rounded-xl my-1 font-bold">{f.farmName}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Active Batch (Flock) *</Label>
                    <Select value={String(form.flockId)} onValueChange={(v) => setForm({ ...form, flockId: v })} disabled={!form.farmId}>
                      <SelectTrigger className={cn("h-12 border-2 rounded-2xl font-bold transition-all", !form.farmId ? "bg-gray-50 border-gray-100" : "bg-white border-gray-100 focus:ring-rose-600")}>
                        <SelectValue placeholder="Select target batch" />
                      </SelectTrigger>
                      <SelectContent className="rounded-2xl border-none shadow-2xl">
                        {flocks.map((f) => (
                          <SelectItem key={f.id} value={String(f.id)} className="rounded-xl my-1 font-bold">{f.batchCode}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Initial Detection</Label>
                    <div className="relative">
                      <Calendar className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
                      <Input
                        type="date"
                        className="h-12 pl-12 bg-white border-2 border-gray-100 rounded-2xl font-bold focus:ring-rose-600 transition-all"
                        value={form.dateDetected}
                        onChange={(e) => setForm({ ...form, dateDetected: e.target.value })}
                      />
                    </div>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Suspected Etiology *</Label>
                    <div className="relative">
                      <BookOpen className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
                      <Input
                        className="h-12 pl-12 bg-white border-2 border-gray-100 rounded-2xl font-bold focus:ring-rose-600 transition-all"
                        value={form.suspectedDisease}
                        onChange={(e) => setForm({ ...form, suspectedDisease: e.target.value })}
                        placeholder="e.g. Newcastle, IBD/Gumboro"
                      />
                    </div>
                  </div>

                  <div className="space-y-3 md:col-span-2">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Clinical Manifestations</Label>
                    <Textarea
                      rows={4}
                      className="bg-white border-2 border-gray-100 rounded-2xl font-medium text-sm focus:ring-rose-600 transition-all p-4"
                      value={form.symptoms}
                      onChange={(e) => setForm({ ...form, symptoms: e.target.value })}
                      placeholder="Provide detailed description of physiological changes, behavior, and physical signs..."
                    />
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Morbidity (Affected)</Label>
                    <div className="relative">
                      <Users className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                      <Input
                        type="number"
                        min="0"
                        className="h-12 pl-12 bg-white border-2 border-gray-100 rounded-2xl font-black focus:ring-rose-600 transition-all"
                        value={form.numberAffected}
                        onChange={(e) => setForm({ ...form, numberAffected: e.target.value })}
                      />
                    </div>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Mortality (Confirmed)</Label>
                    <div className="relative">
                      <Activity className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-rose-500" />
                      <Input
                        type="number"
                        min="0"
                        className="h-12 pl-12 bg-rose-50/30 border-rose-100 border-2 rounded-2xl font-black text-rose-700 focus:ring-rose-600 transition-all"
                        value={form.numberDead}
                        onChange={(e) => setForm({ ...form, numberDead: e.target.value })}
                        placeholder="Auto-populated from Daily Records if blank"
                      />
                    </div>
                  </div>

                  <div className="space-y-3 md:col-span-2">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Attachment (Lab Report URL)</Label>
                    <div className="relative">
                      <Link className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                      <Input
                        type="url"
                        className="h-12 pl-12 bg-white border-2 border-gray-100 rounded-2xl font-bold focus:ring-rose-600 transition-all"
                        value={form.attachmentUrl}
                        onChange={(e) => setForm({ ...form, attachmentUrl: e.target.value })}
                        placeholder="https://storage.trustagro.com/lab-report-123.pdf"
                      />
                    </div>
                  </div>

                  <div className="space-y-3 md:col-span-2">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Triage / Bio-hazard Level</Label>
                    <Select value={form.severity} onValueChange={(v) => setForm({ ...form, severity: v })}>
                      <SelectTrigger className="h-12 bg-white border-2 border-gray-100 rounded-2xl font-black focus:ring-rose-600 transition-all">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent className="rounded-2xl border-none shadow-2xl">
                        {['LOW', 'MODERATE', 'HIGH', 'CRITICAL'].map((s) => (
                          <SelectItem key={s} value={s} className="rounded-xl my-1 font-bold">{s}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>

                <div className="flex gap-4 pt-8">
                  <Button type="submit" className="flex-[2] h-14 rounded-2xl bg-slate-900 hover:bg-slate-800 text-white font-black text-sm uppercase tracking-widest shadow-xl transition-all active:scale-95" disabled={loading}>
                    {loading ? 'Commiting Audit...' : 'Register Clinical Outbreak'}
                  </Button>
                  <Button type="button" variant="outline" className="flex-1 h-14 rounded-2xl border-2 font-black text-sm uppercase tracking-widest hover:bg-gray-50" onClick={() => navigate('/veterinary')}>
                    Discard
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-8">
          <Card className="border-none shadow-2xl shadow-gray-200/40 rounded-[2.5rem] bg-emerald-900 text-white overflow-hidden p-8">
            <div className="flex items-center gap-3 mb-6">
              <div className="p-2 bg-emerald-800 rounded-xl">
                <Info className="h-5 w-5 text-emerald-300" />
              </div>
              <span className="text-xs font-black uppercase tracking-widest text-emerald-200">Clinical Notice</span>
            </div>
            <div className="space-y-6">
              <div className="space-y-3">
                <h4 className="text-sm font-black flex items-center gap-2">
                  <CheckCircle2 className="h-4 w-4 text-emerald-400" />
                  Biosecurity Mandatory
                </h4>
                <p className="text-xs text-emerald-200/70 font-medium leading-relaxed italic">
                  Registering an outbreak immediately flags this batch for isolation. Ensure bio-security protocols are active prior to record commitment.
                </p>
              </div>
              <div className="h-px bg-emerald-800" />
              <div className="space-y-4">
                <p className="text-[10px] font-black uppercase tracking-[0.2em] text-emerald-300">Target Resolution</p>
                <div className="flex justify-between items-center bg-emerald-950/30 p-4 rounded-2xl border border-emerald-800/50">
                  <span className="text-xs font-bold">Lab Results</span>
                  <span className="text-[10px] font-black text-primary/70 uppercase tracking-widest">Awaiting...</span>
                </div>
                <div className="flex justify-between items-center bg-emerald-950/30 p-4 rounded-2xl border border-emerald-800/50">
                  <span className="text-xs font-bold">Quarantine Status</span>
                  <span className="text-[10px] font-black text-rose-400 uppercase tracking-widest">Active</span>
                </div>
              </div>
            </div>
          </Card>

          <Card className="border-none shadow-2xl shadow-gray-200/40 rounded-[2.5rem] bg-white p-8">
            <div className="flex items-center gap-3 mb-6">
              <div className="p-2 bg-rose-50 rounded-xl">
                <ShieldAlert className="h-5 w-5 text-rose-600" />
              </div>
              <span className="text-xs font-black uppercase tracking-widest text-gray-400">Emergency Protocol</span>
            </div>
            <p className="text-[11px] text-gray-500 font-medium leading-relaxed italic">
              If this is a suspected zoonotic disease, contact the National Veterinary Surveillance board immediately after registering this case.
            </p>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default DiseaseCaseForm;
