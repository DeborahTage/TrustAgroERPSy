import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { vetApi, farmApi, flockApi } from '../../api';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { Textarea } from '../../components/ui/textarea';
import { toast } from 'react-toastify';
import { useMasterData } from '../../context/MasterDataContext';
import { ArrowLeft, Syringe, Calendar, ShieldCheck, Tag, Info, CheckCircle2, FlaskConical, AlertCircle, Clock } from 'lucide-react';
import { cn } from '../../lib/utils';

const VaccinationForm = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    farmId: '',
    flockId: '',
    vaccineName: '',
    diseaseProtectedAgainst: '',
    scheduledDate: '',
    remarks: ''
  });

  const [farms, setFarms] = useState([]);
  const [flocks, setFlocks] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { options: diseaseOptions } = useMasterData('DISEASE_TYPE');

  useEffect(() => {
    farmApi.getAll().then((r) => setFarms(r.data.data?.filter((f) => f.status === 'ACTIVE') || []));
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

    if (!form.farmId || !form.flockId || !form.vaccineName || !form.scheduledDate) {
      setError('Missing required protocol parameters. Please verify all mandatory fields.');
      setLoading(false);
      return;
    }

    try {
      await vetApi.createVaccination({
        ...form,
        farmId: Number(form.farmId),
        flockId: Number(form.flockId)
      });
      toast.success('Immunization cycle successfully scheduled in medical ledger.');
      navigate('/veterinary');
    } catch (err) {
      setError(err.response?.data?.message || 'Failure to initialize vaccination protocol.');
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
              Schedule Protocol
              <div className="px-3 py-1 bg-primary/10 text-primary text-[10px] font-black uppercase tracking-widest rounded-full border border-primary/20 flex items-center gap-1.5">
                <Syringe className="h-3 w-3" />
                Medical Order
              </div>
            </h1>
            <p className="text-sm font-bold text-gray-400 mt-1 uppercase tracking-widest">Herd immunity orchestration & prophylactic management</p>
          </div>
        </div>
        <div className="h-px flex-1 bg-gray-100 hidden md:block mx-8" />
        <div className="flex items-center gap-3 bg-gray-50 px-4 py-2 rounded-2xl border border-gray-100">
          <Clock className="h-4 w-4 text-primary" />
          <span className="text-[10px] font-black uppercase text-gray-500">Global Medical Clock Active</span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        <div className="lg:col-span-2 space-y-8">
          <Card className="border-none shadow-2xl shadow-gray-200/40 rounded-[2.5rem] overflow-hidden bg-white">
            <CardHeader className="bg-gray-50/50 border-b border-gray-100 p-8">
              <CardTitle className="text-lg font-black text-gray-900 tracking-tight flex items-center gap-2 uppercase">
                <FlaskConical className="h-5 w-5 text-primary" />
                Biological Configuration
              </CardTitle>
              <CardDescription className="text-xs font-bold text-gray-400">Specify biological agent and target population parameters</CardDescription>
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
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Farm Zone *</Label>
                    <Select value={String(form.farmId)} onValueChange={handleFarmChange}>
                      <SelectTrigger className="h-12 bg-white border-2 border-gray-100 rounded-2xl font-bold focus:ring-primary transition-all">
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
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Flock Population *</Label>
                    <Select value={String(form.flockId)} onValueChange={(v) => setForm({ ...form, flockId: v })} disabled={!form.farmId}>
                      <SelectTrigger className={cn("h-12 border-2 rounded-2xl font-bold transition-all", !form.farmId ? "bg-gray-50 border-gray-100" : "bg-white border-gray-100 focus:ring-primary")}>
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
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Biological Identity *</Label>
                    <div className="relative">
                      <Tag className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
                      <Input
                        className="h-12 pl-12 bg-white border-2 border-gray-100 rounded-2xl font-bold focus:ring-primary transition-all"
                        value={form.vaccineName}
                        onChange={(e) => setForm({ ...form, vaccineName: e.target.value })}
                        placeholder="e.g. LaSota B1, Mareks HVT"
                      />
                    </div>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Pathogen Defense</Label>
                    <div className="relative">
                      <ShieldCheck className="absolute z-10 left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-emerald-500 pointer-events-none" />
                      <Select value={form.diseaseProtectedAgainst} onValueChange={(v) => setForm({ ...form, diseaseProtectedAgainst: v })}>
                        <SelectTrigger className="h-12 pl-12 bg-white border-2 border-gray-100 rounded-2xl font-bold focus:ring-primary transition-all">
                          <SelectValue placeholder="e.g. Newcastle, Gumboro" />
                        </SelectTrigger>
                        <SelectContent className="rounded-2xl border-none shadow-2xl">
                          {diseaseOptions.map((d) => (
                            <SelectItem key={d.value} value={d.value} className="rounded-xl my-1 font-bold">{d.label}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Administration Window *</Label>
                    <div className="relative">
                      <Calendar className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
                      <Input
                        type="date"
                        className="h-12 pl-12 bg-white border-2 border-gray-100 rounded-2xl font-bold focus:ring-primary transition-all"
                        value={form.scheduledDate}
                        onChange={(e) => setForm({ ...form, scheduledDate: e.target.value })}
                      />
                    </div>
                  </div>

                  <div className="space-y-3 md:col-span-2">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-1">Specific Protocol Remarks</Label>
                    <Textarea
                      rows={4}
                      className="bg-white border-2 border-gray-100 rounded-2xl font-medium text-sm focus:ring-primary transition-all p-4 leading-relaxed"
                      value={form.remarks}
                      onChange={(e) => setForm({ ...form, remarks: e.target.value })}
                      placeholder="Include cold chain requirements, administration route (In-ovo, spray, ocular), or dosage specifics..."
                    />
                  </div>
                </div>

                <div className="flex gap-4 pt-8">
                  <Button type="submit" className="flex-[2] h-14 rounded-2xl bg-emerald-900 hover:bg-emerald-800 text-white font-black text-sm uppercase tracking-widest shadow-xl transition-all active:scale-95" disabled={loading}>
                    {loading ? 'Orchestrating Cycle...' : 'Authorize Schedule'}
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
          <Card className="border-none shadow-2xl shadow-gray-200/40 rounded-[2.5rem] bg-primary text-white overflow-hidden p-8">
            <div className="flex items-center gap-3 mb-6">
              <div className="p-2 bg-white/10 rounded-xl">
                <Info className="h-5 w-5 text-emerald-100" />
              </div>
              <span className="text-xs font-black uppercase tracking-widest text-emerald-100">Medical Guard</span>
            </div>
            <div className="space-y-6">
              <div className="space-y-3">
                <h4 className="text-sm font-black flex items-center gap-2">
                  <CheckCircle2 className="h-4 w-4 text-emerald-300" />
                  Cold Chain Protocol
                </h4>
                <p className="text-xs text-emerald-100/70 font-medium leading-relaxed italic">
                  Ensure vaccines are maintained between 2°C and 8°C throughout the transit and administration phase.
                </p>
              </div>
              <div className="h-px bg-white/10" />
              <div className="space-y-4">
                <p className="text-[10px] font-black uppercase tracking-[0.2em] text-emerald-200">Compliance Checklist</p>
                <div className="flex items-center gap-3 bg-white/5 p-3 rounded-xl border border-white/10">
                  <div className="h-1.5 w-1.5 bg-emerald-400 rounded-full" />
                  <span className="text-[10px] font-bold">Withdrawal period verification</span>
                </div>
                <div className="flex items-center gap-3 bg-white/5 p-3 rounded-xl border border-white/10">
                  <div className="h-1.5 w-1.5 bg-emerald-400 rounded-full" />
                  <span className="text-[10px] font-bold">Equipment sterilization check</span>
                </div>
              </div>
            </div>
          </Card>

          <Card className="border-none shadow-2xl shadow-gray-200/40 rounded-[2.5rem] bg-emerald-900 p-8 text-white">
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <span className="text-[10px] font-black uppercase tracking-widest text-primary/70">Upcoming Vol</span>
                <TrendingUp className="h-4 w-4 text-primary/70" />
              </div>
              <p className="text-2xl font-black">4,200</p>
              <p className="text-[10px] font-bold text-emerald-300/60 uppercase tracking-widest">Birds Targeted this Week</p>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
};

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

export default VaccinationForm;
