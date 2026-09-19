import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { dailyRecordApi, farmApi, flockApi } from '../../api';
import EnterpriseFormPage from '../../components/layout/EnterpriseFormPage';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useLanguage } from '../../context/LanguageContext';
import { toast } from 'react-toastify';

const DailyFarmRecordForm = () => {
  const { t } = useLanguage();
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);
  const [form, setForm] = useState({
    date: new Date().toISOString().split('T')[0],
    farmId: '', flockId: '',     openingBirdCount: '', mortality: 0, culledChickens: 0,
    feedConsumed: '', waterConsumed: '', averageWeight: '',
    symptomsOrRemarks: '', mortalityCause: '', mortalityNotes: '',
  });
  const [farms, setFarms] = useState([]);
  const [flocks, setFlocks] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(isEdit);

  useEffect(() => {
    farmApi.getAll().then((r) => setFarms(r.data.data?.filter((f) => f.status === 'ACTIVE') || []));
    if (isEdit) {
      setFetching(true);
      dailyRecordApi.getById(id)
        .then(async (r) => {
          const d = r.data.data;
          setForm({
            date: d.date, farmId: String(d.farmId), flockId: String(d.flockId),
            openingBirdCount: d.openingBirdCount || '', mortality: d.mortality || 0,
            culledChickens: d.culledChickens || 0,
            feedConsumed: d.feedConsumed || '',
            waterConsumed: d.waterConsumed || '', averageWeight: d.averageWeight || '',
            symptomsOrRemarks: d.symptomsOrRemarks || '', mortalityCause: d.mortalityCause || '',
            mortalityNotes: d.mortalityNotes || '',
          });
          const fr = await flockApi.getAll();
          setFlocks(fr.data.data?.filter((f) => String(f.farmId) === String(d.farmId)) || []);
        })
        .catch(() => setError('Failed to load record.'))
        .finally(() => setFetching(false));
    }
  }, [id, isEdit]);

  const handleFarmChange = (farmId) => {
    setForm((f) => ({ ...f, farmId, flockId: '' }));
    if (farmId) {
      flockApi.getAll().then((r) =>
        setFlocks(r.data.data?.filter((f) => String(f.farmId) === String(farmId) && f.status === 'ACTIVE') || [])
      );
    } else setFlocks([]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const payload = {
        ...form,
        farmId: Number(form.farmId),
        flockId: Number(form.flockId),
        openingBirdCount: Number(form.openingBirdCount),
        mortality: Number(form.mortality),
        culledChickens: Number(form.culledChickens) || 0,
        feedConsumed: Number(form.feedConsumed),
        waterConsumed: Number(form.waterConsumed),
        averageWeight: form.averageWeight ? Number(form.averageWeight) : null,
        mortalityCause: form.mortalityCause || null,
      };
      if (isEdit) await dailyRecordApi.update(id, payload);
      else await dailyRecordApi.create(payload);
      toast.success(t('dailyRecordForm.success'));
      navigate('/daily-records');
    } catch (err) {
      setError(err.response?.data?.message || t('dailyRecordForm.error'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <EnterpriseFormPage
      title={isEdit ? t('dailyRecordForm.editTitle') : t('dailyRecordForm.addTitle')}
      subtitle="Daily mortality, feed, water, and growth records"
      breadcrumbs={['Farm', 'Daily Records', isEdit ? 'Edit' : 'New']}
      backTo="/daily-records"
      loading={fetching}
      error={error}
    >
      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="space-y-2">
            <Label>{t('dailyRecordForm.date')}</Label>
            <Input type="date" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} required />
          </div>
          <div className="space-y-2">
            <Label>{t('dailyRecordForm.farm')}</Label>
            <Select value={form.farmId || 'none'} onValueChange={(v) => handleFarmChange(v === 'none' ? '' : v)}>
              <SelectTrigger><SelectValue placeholder={t('forms.select')} /></SelectTrigger>
              <SelectContent>
                <SelectItem value="none">{t('forms.select')}</SelectItem>
                {farms.map((f) => <SelectItem key={f.id} value={String(f.id)}>{f.farmName}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-2">
            <Label>{t('dailyRecordForm.flock')}</Label>
            <Select value={form.flockId || 'none'} onValueChange={(v) => setForm({ ...form, flockId: v === 'none' ? '' : v })}>
              <SelectTrigger><SelectValue placeholder={t('forms.select')} /></SelectTrigger>
              <SelectContent>
                <SelectItem value="none">{t('forms.select')}</SelectItem>
                {flocks.map((f) => <SelectItem key={f.id} value={String(f.id)}>{f.batchCode}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="space-y-2"><Label>{t('dailyRecordForm.openingCount')}</Label><Input type="number" value={form.openingBirdCount} onChange={(e) => setForm({ ...form, openingBirdCount: e.target.value })} /></div>
          <div className="space-y-2"><Label>{t('dailyRecordForm.mortality')}</Label><Input type="number" min="0" value={form.mortality} onChange={(e) => setForm({ ...form, mortality: e.target.value })} /></div>
          <div className="space-y-2"><Label>{t('dailyRecordForm.culledChickens')}</Label><Input type="number" min="0" value={form.culledChickens} onChange={(e) => setForm({ ...form, culledChickens: e.target.value })} /></div>
          <div className="space-y-2"><Label>{t('dailyRecordForm.feedConsumed')}</Label><Input type="number" min="0" step="0.01" value={form.feedConsumed} onChange={(e) => setForm({ ...form, feedConsumed: e.target.value })} /></div>
          <div className="space-y-2"><Label>{t('dailyRecordForm.water')}</Label><Input type="number" min="0" step="0.01" value={form.waterConsumed} onChange={(e) => setForm({ ...form, waterConsumed: e.target.value })} /></div>
          <div className="space-y-2"><Label>{t('dailyRecordForm.averageWeight')}</Label><Input type="number" min="0" step="0.01" value={form.averageWeight} onChange={(e) => setForm({ ...form, averageWeight: e.target.value })} /></div>
        </div>

        <div className="space-y-2">
          <Label>{t('dailyRecordForm.symptoms')}</Label>
          <Textarea rows={2} value={form.symptomsOrRemarks} onChange={(e) => setForm({ ...form, symptomsOrRemarks: e.target.value })} />
        </div>

        {Number(form.mortality) > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 p-4 bg-rose-50 rounded-xl border border-rose-100">
            <div className="space-y-2">
              <Label className="text-rose-700">Mortality Cause</Label>
              <Select value={form.mortalityCause || 'none'} onValueChange={(v) => setForm({ ...form, mortalityCause: v === 'none' ? '' : v })}>
                <SelectTrigger><SelectValue placeholder="Select cause" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">Select cause</SelectItem>
                  {['DISEASE', 'PREDATOR', 'HEAT', 'CULLING', 'UNKNOWN'].map((c) => (
                    <SelectItem key={c} value={c}>{c}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Mortality Notes</Label>
              <Input value={form.mortalityNotes} onChange={(e) => setForm({ ...form, mortalityNotes: e.target.value })} />
            </div>
          </div>
        )}

        <div className="flex gap-3 pt-4 border-t">
          <Button type="submit" disabled={loading} className="bg-primary hover:bg-primary/90">
            {loading ? t('forms.saving') : t('forms.save')}
          </Button>
          <Button type="button" variant="outline" onClick={() => navigate('/daily-records')}>{t('forms.cancel')}</Button>
        </div>
      </form>
    </EnterpriseFormPage>
  );
};

export default DailyFarmRecordForm;
