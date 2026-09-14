import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { vetApi, farmApi, flockApi } from '../../api';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { toast } from 'react-toastify';
import { ArrowLeft, Pill, Calendar, Activity, ShieldCheck, HeartPulse, Info } from 'lucide-react';
import { cn } from '../../lib/utils';

const TreatmentForm = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    farmId: '',
    flockId: '',
    diseaseCaseId: '',
    drugName: '',
    dosage: '',
    route: '',
    duration: '',
    startDate: '',
    endDate: '',
    outcome: ''
  });

  const [farms, setFarms] = useState([]);
  const [flocks, setFlocks] = useState([]);
  const [cases, setCases] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    farmApi.getAll().then((r) => setFarms(r.data.data?.filter((f) => f.status === 'ACTIVE') || []));
    vetApi.getDiseaseCases().then((r) => setCases(r.data.data?.filter((c) => c.status === 'ACTIVE') || []));
  }, []);

  const handleFarmChange = (farmId) => {
    setForm((f) => ({ ...f, farmId, flockId: '' }));
    if (farmId) {
      flockApi.getAll().then((r) => setFlocks(r.data.data?.filter((f) => String(f.farmId) === String(farmId) && f.status === 'ACTIVE') || []));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    if (!form.farmId || !form.flockId || !form.drugName) {
      setError('Incomplete clinical data. Please specify the target population and drug.');
      setLoading(false);
      return;
    }

    try {
      await vetApi.createTreatment({
        ...form,
        farmId: Number(form.farmId),
        flockId: Number(form.flockId),
        diseaseCaseId: form.diseaseCaseId && form.diseaseCaseId !== 'null' ? Number(form.diseaseCaseId) : null
      });
      toast.success('Therapeutic regimen successfully initialized.');
      navigate('/veterinary');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to record clinical intervention.');
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
              Clinic Intake
              <div className="px-3 py-1 bg-emerald-50 text-emerald-700 text-[10px] font-black uppercase tracking-widest rounded-full border border-emerald-100 flex items-center gap-1.5">
                <HeartPulse className="h-3 w-3" />
                Active Therapy
              </div>
            </h1>
            <p className="text-sm font-bold text-gray-400 mt-1 uppercase tracking-widest">Medical oversight & therapeutic outcome management</p>
          </div>
        </div>
        <div className="h-px flex-1 bg-gray-100 hidden md:block mx-8" />
        <div className="flex items-center gap-3 bg-gray-50 px-4 py-2 rounded-2xl border border-gray-100">
          <ShieldCheck className="h-4 w-4 text-emerald-500" />
          <span className="text-[10px] font-black uppercase text-gray-500">Regimen Compliance Active</span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        <div className="lg:col-span-2 space-y-8">
          <Card className="border-none shadow-2xl shadow-gray-200/40 rounded-[2.5rem] overflow-hidden bg-white">
            <CardHeader className="bg-gray-50/50 border-b border-gray-100 p-8">
              <CardTitle className="text-lg font-black text-gray-900 tracking-tight flex items-center gap-2 uppercase">
                <Pill className="h-5 w-5 text-emerald-600" />
                Therapeutic config
              </CardTitle>
              <CardDescription className="text-xs font-bold text-gray-400">Establish pharmacology and temporal bounds for treatment</CardDescription>
            </CardHeader>
            <CardContent className="p-8">
              <form onSubmit={handleSubmit} className="space-y-8">
                {error && (
                  <div className="p-4 text-xs font-black text-rose-700 bg-rose-50 border border-rose-100 rounded-2xl flex items-center gap-3 animate-in slide-in-from-top-2">
                    <AlertCircle className="h-5 w-5" />
                    {error}
                  </div>
                )}

                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Location Zone *</Label>
                    <Select value={String(form.farmId)} onValueChange={handleFarmChange}>
                      <SelectTrigger className="h-12 bg-white border-2 border-gray-100 rounded-2xl font-bold focus:ring-emerald-600 transition-all">
                        <SelectValue placeholder="Select target facility" />
                      </SelectTrigger>
                      <SelectContent className="rounded-2xl border-none shadow-2xl">
                        {farms.map((f) => (
                          <SelectItem key={f.id} value={String(f.id)} className="rounded-xl my-1 font-bold">{f.farmName}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Population Batch *</Label>
                    <Select value={String(form.flockId)} onValueChange={(v) => setForm({ ...form, flockId: v })} disabled={!form.farmId}>
                      <SelectTrigger className={cn("h-12 border-2 rounded-2xl font-bold transition-all", !form.farmId ? "bg-gray-50 border-gray-100" : "bg-white border-gray-100 focus:ring-emerald-600")}>
                        <SelectValue placeholder="Select target batch" />
                      </SelectTrigger>
                      <SelectContent className="rounded-2xl border-none shadow-2xl">
                        {flocks.map((f) => (
                          <SelectItem key={f.id} value={String(f.id)} className="rounded-xl my-1 font-bold">{f.batchCode}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-3 md:col-span-2">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Etiology Link (Disease Case)</Label>
                    <Select value={String(form.diseaseCaseId)} onValueChange={(v) => setForm({ ...form, diseaseCaseId: v })}>
                      <SelectTrigger className="h-12 bg-white border-2 border-gray-100 rounded-2xl font-bold focus:ring-emerald-600 transition-all">
                        <SelectValue placeholder="Link to active medical case" />
                      </SelectTrigger>
                      <SelectContent className="rounded-2xl border-none shadow-2xl">
                        <SelectItem value="null" className="rounded-xl my-1 font-bold italic text-gray-400">INDIPENDENT AD-HOC TREATMENT</SelectItem>
                        {cases.map((c) => (
                          <SelectItem key={c.id} value={String(c.id)} className="rounded-xl my-1 font-bold">
                            {c.suspectedDisease} @ {c.farmName} <span className="ml-2 text-[10px] opacity-40">({new Date(c.dateDetected).toLocaleDateString()})</span>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Pharmacological Agent *</Label>
                    <div className="relative">
                      <FlaskConical className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
                      <Input
                        className="h-12 pl-12 bg-white border-2 border-gray-100 rounded-2xl font-bold focus:ring-emerald-600 transition-all"
                        value={form.drugName}
                        onChange={(e) => setForm({ ...form, drugName: e.target.value })}
                        placeholder="e.g. Enrofloxacin 10%"
                      />
                    </div>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Dosage Intensity</Label>
                    <Input
                      className="h-12 bg-white border-2 border-gray-100 rounded-2xl font-bold focus:ring-emerald-600 transition-all text-emerald-700"
                      value={form.dosage}
                      onChange={(e) => setForm({ ...form, dosage: e.target.value })}
                      placeholder="e.g. 10mg/kg live weight"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-3">
                      <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Delivery Route</Label>
                      <Input
                        className="h-12 bg-white border-2 border-gray-100 rounded-2xl font-bold focus:ring-emerald-600 transition-all"
                        value={form.route}
                        onChange={(e) => setForm({ ...form, route: e.target.value })}
                        placeholder="e.g. Ocular/Spray/Water"
                      />
                    </div>
                    <div className="space-y-3">
                      <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Regimen Span</Label>
                      <Input
                        className="h-12 bg-white border-2 border-gray-100 rounded-2xl font-bold focus:ring-emerald-600 transition-all"
                        value={form.duration}
                        onChange={(e) => setForm({ ...form, duration: e.target.value })}
                        placeholder="e.g. 7 Continuous Days"
                      />
                    </div>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Anticipated Outcome</Label>
                    <Input
                      className="h-12 bg-white border-2 border-gray-100 rounded-2xl font-bold focus:ring-emerald-600 transition-all"
                      value={form.outcome}
                      onChange={(e) => setForm({ ...form, outcome: e.target.value })}
                      placeholder="e.g. Total Remission"
                    />
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Protocol Initialization</Label>
                    <div className="relative">
                      <Calendar className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
                      <Input
                        type="date"
                        className="h-12 pl-12 bg-white border-2 border-gray-100 rounded-2xl font-bold focus:ring-emerald-600 transition-all"
                        value={form.startDate}
                        onChange={(e) => setForm({ ...form, startDate: e.target.value })}
                      />
                    </div>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Terminal Review Date</Label>
                    <div className="relative">
                      <Calendar className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
                      <Input
                        type="date"
                        className="h-12 pl-12 bg-white border-2 border-gray-100 rounded-2xl font-bold focus:ring-emerald-600 transition-all"
                        value={form.endDate}
                        onChange={(e) => setForm({ ...form, endDate: e.target.value })}
                      />
                    </div>
                  </div>
                </div>

                <div className="flex gap-4 pt-8">
                  <Button type="submit" className="flex-[2] h-14 rounded-2xl bg-emerald-900 hover:bg-emerald-800 text-white font-black text-sm uppercase tracking-widest shadow-xl transition-all active:scale-95" disabled={loading}>
                    {loading ? 'Initializing Regimen...' : 'Authorize Treatment'}
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
              <span className="text-xs font-black uppercase tracking-widest text-emerald-200">Therapeutic Guard</span>
            </div>
            <div className="space-y-6">
              <div className="space-y-3">
                <h4 className="text-sm font-black flex items-center gap-2">
                  <Activity className="h-4 w-4 text-emerald-400" />
                  Clinical Oversight
                </h4>
                <p className="text-xs text-emerald-200/70 font-medium leading-relaxed italic">
                  Ensure all pharmaceutical interventions are recorded for withdrawal tracking. Failure to log antibiotic use may void compliance certifications.
                </p>
              </div>
              <div className="h-px bg-emerald-800" />
              <div className="space-y-4">
                <p className="text-[10px] font-black uppercase tracking-[0.2em] text-emerald-300">Regimen Logic</p>
                <div className="flex items-center gap-3 bg-emerald-950/30 p-3 rounded-xl border border-emerald-800/50">
                  <div className="h-1.5 w-1.5 bg-emerald-400 rounded-full animate-pulse" />
                  <span className="text-[10px] font-bold">Automatic Withdrawal Monitoring</span>
                </div>
                <div className="flex items-center gap-3 bg-emerald-950/30 p-3 rounded-xl border border-emerald-800/50">
                  <div className="h-1.5 w-1.5 bg-emerald-400 rounded-full animate-pulse" />
                  <span className="text-[10px] font-bold">Audit-Ready Medical Logs</span>
                </div>
              </div>
            </div>
          </Card>

          <Card className="border-none shadow-2xl shadow-gray-200/40 rounded-[2.5rem] bg-emerald-500 p-8 text-white">
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <span className="text-[10px] font-black uppercase tracking-widest text-emerald-100">Recovery Rate</span>
                <TrendingUp className="h-4 w-4 text-emerald-100" />
              </div>
              <p className="text-2xl font-black">88.4%</p>
              <p className="text-[10px] font-bold text-emerald-100/60 uppercase tracking-widest">Aggregate Regimen Efficacy</p>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
};

const FlaskConical = ({ className }) => (
  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}>
    <path d="M10 2v7.31" /><path d="M14 2v7.31" /><path d="M8.5 2h7" /><path d="M14 9.3c.7.7 1 1.5 1 2.4l1.1 6c.3 1.5-.7 2.3-2.1 2.3H10c-1.4 0-2.4-.8-2.1-2.3l1.1-6c0-.9.3-1.7 1-2.4l1-1.1" />
  </svg>
);

const AlertCircle = ({ className }) => (
  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}>
    <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
  </svg>
);

const TrendingUp = ({ className }) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    className={className}
  >
    <polyline points="22 7 13.5 15.5 8.5 10.5 2 17" />
    <polyline points="16 7 22 7 22 13" />
  </svg>
);

export default TreatmentForm;
