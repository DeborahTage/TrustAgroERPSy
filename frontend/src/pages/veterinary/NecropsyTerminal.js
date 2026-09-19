import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronRight } from 'lucide-react';
import EnterpriseFormPage from '../../components/layout/EnterpriseFormPage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { flockApi, vetApi } from '../../api';
import { toast } from 'react-toastify';

const organSections = [
  { id: 'heartFindings', label: 'Heart & Pericardium' },
  { id: 'liverFindings', label: 'Liver & Gallbladder' },
  { id: 'lungFindings', label: 'Lungs & Air Sacs' },
  { id: 'gizzardFindings', label: 'Gizzard & Proventriculus' },
  { id: 'spleenFindings', label: 'Spleen' },
  { id: 'intestineFindings', label: 'Intestinal Tract' },
];

const NecropsyTerminal = () => {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [flocks, setFlocks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    flockId: '',
    numChickensExamined: '',
    generalFindings: '',
    heartFindings: '',
    liverFindings: '',
    lungFindings: '',
    gizzardFindings: '',
    spleenFindings: '',
    intestineFindings: '',
    suspectedCauseOfDeath: '',
    finalDiagnosis: '',
    recommendations: '',
  });

  useEffect(() => {
    flockApi.getAll().then((r) => setFlocks(r.data?.data?.filter((f) => f.status === 'ACTIVE') || []));
  }, []);

  const setField = (field, value) => setForm((f) => ({ ...f, [field]: value }));

  const handleSubmit = async () => {
    if (!form.flockId) { setError('Select an affected flock.'); return; }
    setLoading(true);
    setError('');
    try {
      await vetApi.createNecropsy({
        ...form,
        flockId: Number(form.flockId),
        numChickensExamined: form.numChickensExamined ? Number(form.numChickensExamined) : null,
      });
      toast.success('Necropsy report submitted');
      navigate('/vet');
    } catch {
      setError('Failed to submit necropsy report.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <EnterpriseFormPage
      title="Forensic Necropsy Terminal"
      subtitle="Record post-mortem findings and diagnostic conclusions"
      breadcrumbs={['Veterinary', 'Necropsy']}
      backTo="/vet"
      error={error}
    >
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <Card className="lg:col-span-1 h-fit border-none shadow-sm">
          <CardHeader className="pb-2"><CardTitle className="text-xs text-slate-500 uppercase">Steps</CardTitle></CardHeader>
          <CardContent className="space-y-1">
            {['Flock Context', 'External Exam', 'Organ Check', 'Diagnosis'].map((label, i) => (
              <button
                key={label}
                type="button"
                onClick={() => setStep(i + 1)}
                className={`w-full flex items-center gap-3 p-3 rounded-lg text-left text-sm ${step === i + 1 ? 'bg-primary text-white font-semibold' : 'text-slate-600 hover:bg-slate-100'}`}
              >
                <span className="w-6 h-6 rounded-full border flex items-center justify-center text-xs">{i + 1}</span>
                {label}
              </button>
            ))}
          </CardContent>
        </Card>

        <div className="lg:col-span-3 space-y-4">
          {step === 1 && (
            <Card className="border-none shadow-sm">
              <CardHeader>
                <CardTitle>Flock Context</CardTitle>
                <CardDescription>Register specimens from affected flock</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>Affected Flock *</Label>
                    <Select value={form.flockId || 'none'} onValueChange={(v) => setField('flockId', v === 'none' ? '' : v)}>
                      <SelectTrigger><SelectValue placeholder="Choose flock..." /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="none">Select flock...</SelectItem>
                        {flocks.map((f) => <SelectItem key={f.id} value={String(f.id)}>{f.batchCode} ({f.breed})</SelectItem>)}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label>Specimens Examined</Label>
                    <Input type="number" min="1" value={form.numChickensExamined} onChange={(e) => setField('numChickensExamined', e.target.value)} />
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {step === 2 && (
            <Card className="border-none shadow-sm">
              <CardHeader><CardTitle>External Examination</CardTitle></CardHeader>
              <CardContent>
                <Textarea rows={5} placeholder="External lesions, dehydration, feather condition..." value={form.generalFindings} onChange={(e) => setField('generalFindings', e.target.value)} />
              </CardContent>
            </Card>
          )}

          {step === 3 && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {organSections.map((section) => (
                <Card key={section.id} className="border-none shadow-sm">
                  <CardHeader className="py-3"><CardTitle className="text-sm">{section.label}</CardTitle></CardHeader>
                  <CardContent>
                    <Textarea rows={3} placeholder={`Observations for ${section.label}...`} value={form[section.id]} onChange={(e) => setField(section.id, e.target.value)} />
                  </CardContent>
                </Card>
              ))}
            </div>
          )}

          {step === 4 && (
            <Card className="border-none shadow-sm">
              <CardHeader><CardTitle>Final Diagnosis</CardTitle></CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label>Suspected Cause of Death</Label>
                  <Input value={form.suspectedCauseOfDeath} onChange={(e) => setField('suspectedCauseOfDeath', e.target.value)} />
                </div>
                <div className="space-y-2">
                  <Label>Final Diagnosis</Label>
                  <Textarea value={form.finalDiagnosis} onChange={(e) => setField('finalDiagnosis', e.target.value)} />
                </div>
                <div className="space-y-2">
                  <Label>Recommendations</Label>
                  <Textarea value={form.recommendations} onChange={(e) => setField('recommendations', e.target.value)} />
                </div>
              </CardContent>
            </Card>
          )}

          <div className="flex justify-between pt-2">
            <Button type="button" variant="outline" disabled={step === 1} onClick={() => setStep((s) => s - 1)}>Previous</Button>
            {step < 4 ? (
              <Button type="button" onClick={() => setStep((s) => s + 1)}>Continue <ChevronRight className="ml-1 h-4 w-4" /></Button>
            ) : (
              <Button type="button" className="bg-rose-600 hover:bg-rose-700" disabled={loading} onClick={handleSubmit}>
                {loading ? 'Submitting...' : 'Submit Report'}
              </Button>
            )}
          </div>
        </div>
      </div>
    </EnterpriseFormPage>
  );
};

export default NecropsyTerminal;
