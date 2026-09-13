import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { flockApi, farmApi } from '../../api';
import EnterpriseFormPage from '../../components/layout/EnterpriseFormPage';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useLanguage } from '../../context/LanguageContext';
import { useMasterData } from '../../context/MasterDataContext';
import { toast } from 'react-toastify';
import { Hash, Calendar } from 'lucide-react';

const FlockForm = () => {
  const { t } = useLanguage();
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);
  const [form, setForm] = useState({
    batchCode: '', farmId: '', birdType: '', breed: '', initialBirdCount: '', startDate: '', expectedEndDate: '',
  });
  const [farms, setFarms] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(isEdit);
  const { options: breedOptions } = useMasterData('BREED');

  useEffect(() => {
    farmApi.getAll().then((r) => setFarms(r.data.data?.filter((f) => f.status === 'ACTIVE') || []));
    if (isEdit) {
      setFetching(true);
      flockApi.getById(id)
        .then((r) => {
          const f = r.data.data;
          setForm({
            batchCode: f.batchCode,
            farmId: String(f.farmId),
            birdType: f.birdType || '',
            breed: f.breed || '',
            initialBirdCount: f.initialBirdCount || '',
            startDate: f.startDate || '',
            expectedEndDate: f.expectedEndDate || '',
          });
        })
        .catch(() => setError('Failed to load flock.'))
        .finally(() => setFetching(false));
    }
  }, [id, isEdit]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const payload = {
        ...form,
        farmId: Number(form.farmId),
        initialBirdCount: Number(form.initialBirdCount),
      };
      if (isEdit) await flockApi.update(id, payload);
      else await flockApi.create(payload);
      toast.success(isEdit ? t('flockForm.successUpdate') : t('flockForm.successCreate'));
      navigate('/flocks');
    } catch (err) {
      setError(err.response?.data?.message || t('flockForm.error'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <EnterpriseFormPage
      title={isEdit ? t('flockForm.editTitle') : t('flockForm.addTitle')}
      subtitle="Batch code, farm assignment, breed, and production timeline"
      breadcrumbs={['Farm', 'Flocks', isEdit ? 'Edit' : 'New']}
      backTo="/flocks"
      loading={fetching}
      error={error}
    >
      <form onSubmit={handleSubmit} className="space-y-8">
        <section className="space-y-4">
          <div className="flex items-center gap-2 text-sm font-bold text-slate-500 uppercase tracking-wider">
            <Hash className="h-4 w-4" /> Flock Identity
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="batchCode">{t('flockForm.batchCode')} *</Label>
              <Input id="batchCode" value={form.batchCode} onChange={(e) => setForm({ ...form, batchCode: e.target.value })} required disabled={isEdit} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="farmId">{t('flockForm.farm')} *</Label>
              <Select value={form.farmId || 'none'} onValueChange={(v) => setForm({ ...form, farmId: v === 'none' ? '' : v })} disabled={isEdit}>
                <SelectTrigger><SelectValue placeholder={t('forms.selectFarm')} /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">{t('forms.selectFarm')}</SelectItem>
                  {farms.map((f) => (
                    <SelectItem key={f.id} value={String(f.id)}>{f.farmName}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="birdType">{t('flockForm.birdType')}</Label>
              <Select value={form.birdType || 'none'} onValueChange={(v) => setForm({ ...form, birdType: v === 'none' ? '' : v })}>
                <SelectTrigger><SelectValue placeholder="Bird type" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">Select type</SelectItem>
                  {breedOptions.map((b) => (
                    <SelectItem key={b.value} value={b.value}>{b.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="breed">Breed</Label>
              <Input id="breed" value={form.breed} onChange={(e) => setForm({ ...form, breed: e.target.value })} placeholder="e.g. Cobb 500" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="initialBirdCount">{t('flockForm.initialBirdCount')}</Label>
              <Input id="initialBirdCount" type="number" min="1" value={form.initialBirdCount} onChange={(e) => setForm({ ...form, initialBirdCount: e.target.value })} disabled={isEdit} />
            </div>
          </div>
        </section>

        <section className="space-y-4 pt-6 border-t">
          <div className="flex items-center gap-2 text-sm font-bold text-slate-500 uppercase tracking-wider">
            <Calendar className="h-4 w-4" /> Timeline
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="startDate">{t('flockForm.startDate')}</Label>
              <Input id="startDate" type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="expectedEndDate">{t('flockForm.expectedEndDate')}</Label>
              <Input id="expectedEndDate" type="date" value={form.expectedEndDate} onChange={(e) => setForm({ ...form, expectedEndDate: e.target.value })} />
            </div>
          </div>
        </section>

        <div className="flex gap-3 pt-4 border-t">
          <Button type="submit" disabled={loading} className="bg-primary hover:bg-primary/90">
            {loading ? t('forms.saving') : t('forms.save')}
          </Button>
          <Button type="button" variant="outline" onClick={() => navigate('/flocks')}>{t('forms.cancel')}</Button>
        </div>
      </form>
    </EnterpriseFormPage>
  );
};

export default FlockForm;
