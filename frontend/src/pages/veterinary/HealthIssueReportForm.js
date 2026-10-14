import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { farmApi, flockApi, vetApi } from '../../api';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { Textarea } from '../../components/ui/textarea';
import { toast } from 'react-toastify';
import { ArrowLeft, AlertCircle, Calendar, Users, Activity, ShieldAlert, Zap, Info, CheckCircle2, Siren } from 'lucide-react';
import { cn } from '../../lib/utils';

const HealthIssueReportForm = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dailyRecord = location.state?.dailyRecord;

  const [form, setForm] = useState({
    farmId: dailyRecord?.farmId || '',
    flockId: dailyRecord?.flockId || '',
    dailyFarmRecordId: dailyRecord?.id || null,
    reportDate: dailyRecord?.date || new Date().toISOString().split('T')[0],
    symptoms: dailyRecord?.symptomsOrRemarks || '',
    mortalityObserved: dailyRecord?.mortality ?? '',
    numberAffected: '',
    remarks: '',
  });

  const [farms, setFarms] = useState([]);
  const [flocks, setFlocks] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    farmApi.getAll().then((r) => setFarms(r.data.data?.filter((farm) => farm.status === 'ACTIVE') || []));
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

    if (!form.farmId || !form.flockId || !form.symptoms) {
      setError('Incomplete alert data. Please specify the flock and observed symptoms.');
      setLoading(false);
      return;
    }

    try {
      await vetApi.createHealthReport({
        ...form,
        farmId: Number(form.farmId),
        flockId: Number(form.flockId),
        dailyFarmRecordId: form.dailyFarmRecordId ? Number(form.dailyFarmRecordId) : null,
        mortalityObserved: form.mortalityObserved ? Number(form.mortalityObserved) : null,
        numberAffected: form.numberAffected ? Number(form.numberAffected) : null,
      });
      toast.success('Critical health alert dispatched to the veterinary queue.');
      navigate('/veterinary');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to transmit health alert.');
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
            className="h-12 w-12 rounded-2xl bg-card shadow-sm border border-border hover:bg-muted group transition-all"
            onClick={() => navigate(-1)}
          >
            <ArrowLeft className="h-5 w-5 text-muted-foreground group-hover:text-foreground transition-colors" />
          </Button>
          <div>
            <h1 className="text-3xl font-black tracking-tight text-foreground flex items-center gap-3">
              Fast-Track Alert
              <div className="px-3 py-1 bg-amber-50 dark:bg-amber-950/40 text-amber-700 dark:text-amber-400 text-[10px] font-black uppercase tracking-widest rounded-full border border-amber-200 dark:border-amber-800/40 flex items-center gap-1.5">
                <Zap className="h-3 w-3 animate-pulse" />
                Priority Incident
              </div>
            </h1>
            <p className="text-sm font-bold text-muted-foreground mt-1 uppercase tracking-widest">Immediate field-to-clinic diagnostic relay</p>
          </div>
        </div>
        <div className="h-px flex-1 bg-border hidden md:block mx-8" />
        <div className="flex items-center gap-3 bg-muted/40 px-4 py-2 rounded-2xl border border-border">
          <Siren className="h-4 w-4 text-rose-500 animate-pulse" />
          <span className="text-[10px] font-black uppercase text-rose-600 dark:text-rose-400">Priority Link Active</span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        <div className="lg:col-span-2 space-y-8">
          <Card className="erp-card rounded-3xl overflow-hidden border border-border">
            <CardHeader className="bg-muted/40 border-b border-border/60 p-8">
              <CardTitle className="text-lg font-black text-foreground tracking-tight flex items-center gap-2 uppercase">
                <AlertCircle className="h-5 w-5 text-amber-500" />
                Incident Matrix
              </CardTitle>
              <CardDescription className="text-xs font-bold text-muted-foreground">Specify biological agent and target population parameters</CardDescription>
            </CardHeader>
            <CardContent className="p-8">
              <form onSubmit={handleSubmit} className="space-y-8">
                {error && (
                  <div className="p-4 text-xs font-black text-rose-700 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800/40 rounded-2xl flex items-center gap-3 animate-in slide-in-from-top-2">
                    <AlertCircle className="h-5 w-5" />
                    {error}
                  </div>
                )}

                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground ml-1">Incident Source (Farm) *</Label>
                    <Select value={String(form.farmId)} onValueChange={(v) => setForm((prev) => ({ ...prev, farmId: v, flockId: '' }))}>
                      <SelectTrigger className="h-12 bg-background border border-border rounded-xl font-bold text-foreground focus:ring-amber-600 transition-all">
                        <SelectValue placeholder="Select origin farm" />
                      </SelectTrigger>
                      <SelectContent className="rounded-xl border border-border shadow-2xl">
                        {farms.map((f) => (
                          <SelectItem key={f.id} value={String(f.id)} className="rounded-xl my-1 font-bold">{f.farmName}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground ml-1">Impacted Batch (Flock) *</Label>
                    <Select value={String(form.flockId)} onValueChange={(v) => setForm((prev) => ({ ...prev, flockId: v }))} disabled={!form.farmId}>
                      <SelectTrigger className={cn("h-12 border rounded-xl font-bold transition-all text-foreground", !form.farmId ? "bg-muted/40 border-border" : "bg-background border-border focus:ring-amber-600")}>
                        <SelectValue placeholder="Select target batch" />
                      </SelectTrigger>
                      <SelectContent className="rounded-xl border border-border shadow-2xl">
                        {flocks.map((f) => (
                          <SelectItem key={f.id} value={String(f.id)} className="rounded-xl my-1 font-bold">{f.batchCode}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground ml-1">Observation Date</Label>
                    <div className="relative">
                      <Calendar className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
                      <Input
                        type="date"
                        className="h-12 pl-12 bg-background border border-border rounded-xl font-bold text-foreground focus:ring-amber-600 transition-all"
                        value={form.reportDate}
                        onChange={(e) => setForm((prev) => ({ ...prev, reportDate: e.target.value }))}
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-3">
                      <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground ml-1">Mortality</Label>
                      <div className="relative">
                        <Activity className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-rose-500" />
                        <Input
                          type="number"
                          min="0"
                          className="h-12 pl-12 bg-rose-50/40 dark:bg-rose-950/30 border-rose-200 dark:border-rose-800/40 border rounded-xl font-black text-rose-700 dark:text-rose-400 focus:ring-amber-600 transition-all"
                          value={form.mortalityObserved}
                          onChange={(e) => setForm((prev) => ({ ...prev, mortalityObserved: e.target.value }))}
                        />
                      </div>
                    </div>
                    <div className="space-y-3">
                      <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground ml-1">Symptomatic</Label>
                      <div className="relative">
                        <Users className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-amber-500" />
                        <Input
                          type="number"
                          min="0"
                          className="h-12 pl-12 bg-amber-50/40 dark:bg-amber-950/30 border-amber-200 dark:border-amber-800/40 border rounded-xl font-black text-amber-700 dark:text-amber-400 focus:ring-amber-600 transition-all"
                          value={form.numberAffected}
                          onChange={(e) => setForm((prev) => ({ ...prev, numberAffected: e.target.value }))}
                        />
                      </div>
                    </div>
                  </div>

                  <div className="space-y-3 md:col-span-2">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground ml-1">Observed Clinical Signs *</Label>
                    <Textarea
                      rows={4}
                      className="bg-background border border-border rounded-xl font-medium text-foreground text-sm focus:ring-amber-600 transition-all p-4 leading-relaxed"
                      value={form.symptoms}
                      onChange={(e) => setForm((prev) => ({ ...prev, symptoms: e.target.value }))}
                      placeholder="Describe specific symptoms (e.g., respiratory distress, unusual gait, lethargy)..."
                    />
                  </div>

                  <div className="space-y-3 md:col-span-2">
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground ml-1">Supplemental Field Notes</Label>
                    <Textarea
                      rows={2}
                      className="bg-background border border-border rounded-xl font-medium text-foreground text-sm focus:ring-amber-600 transition-all p-4"
                      value={form.remarks}
                      onChange={(e) => setForm((prev) => ({ ...prev, remarks: e.target.value }))}
                      placeholder="Contextual details (feed changes, weather events, vaccine status)..."
                    />
                  </div>
                </div>

                <div className="flex gap-4 pt-8">
                  <Button type="submit" className="flex-[2] h-14 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold text-sm uppercase tracking-wider shadow-md transition-all active:scale-95" disabled={loading}>
                    {loading ? 'Dispatching...' : 'Broadcast Emergency Alert'}
                  </Button>
                  <Button type="button" variant="outline" className="flex-1 h-14 rounded-xl border border-border font-bold text-sm uppercase tracking-wider text-muted-foreground hover:text-foreground hover:bg-muted" onClick={() => navigate(-1)}>
                    Discard
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-8">
          <Card className="erp-card border border-emerald-800/60 rounded-3xl bg-emerald-950/80 text-white overflow-hidden p-8 shadow-xl">
            <div className="flex items-center gap-3 mb-6">
              <div className="p-2 bg-emerald-800/80 rounded-xl">
                <Info className="h-5 w-5 text-emerald-300" />
              </div>
              <span className="text-xs font-black uppercase tracking-widest text-emerald-300">Submission Protocol</span>
            </div>
            <div className="space-y-6">
              <div className="space-y-3">
                <h4 className="text-sm font-black flex items-center gap-2">
                  <CheckCircle2 className="h-4 w-4 text-emerald-400" />
                  Biosecurity Initializer
                </h4>
                <p className="text-xs text-emerald-200/80 font-medium leading-relaxed italic">
                  Dispatched alerts are immediately visible to all veterinary officers. Use this only for genuine clinical concerns requiring audit.
                </p>
              </div>
              <div className="h-px bg-emerald-800/60" />
              <div className="space-y-4">
                <p className="text-[10px] font-black uppercase tracking-[0.2em] text-emerald-300">Relay Status</p>
                <div className="flex items-center gap-3 bg-emerald-950/50 p-3 rounded-xl border border-emerald-800/50">
                  <div className="h-1.5 w-1.5 bg-emerald-400 rounded-full animate-pulse" />
                  <span className="text-[10px] font-bold text-emerald-100">Encrypted Audit Link Active</span>
                </div>
                <div className="flex items-center gap-3 bg-emerald-950/50 p-3 rounded-xl border border-emerald-800/50">
                  <div className="h-1.5 w-1.5 bg-emerald-400 rounded-full animate-pulse" />
                  <span className="text-[10px] font-bold text-emerald-100">Vet Notification Queue Ready</span>
                </div>
              </div>
            </div>
          </Card>

          <Card className="erp-card border border-border rounded-3xl p-8">
            <div className="flex items-center gap-3 mb-4">
              <ShieldAlert className="h-5 w-5 text-amber-500" />
              <span className="text-xs font-black uppercase tracking-widest text-muted-foreground">Surveillance Warning</span>
            </div>
            <p className="text-[11px] text-muted-foreground font-medium leading-relaxed italic">
              Multiple reports within a 5-mile radius will trigger an automatic biosecurity perimeter alert. Ensure accuracy of GPS/Farm coordinates.
            </p>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default HealthIssueReportForm;
