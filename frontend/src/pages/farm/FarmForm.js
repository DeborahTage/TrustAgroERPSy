import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { farmApi, userApi } from '../../api';
import EnterpriseFormPage from '../../components/layout/EnterpriseFormPage';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useLanguage } from '../../context/LanguageContext';
import { toast } from 'react-toastify';
import { Building2, Settings } from 'lucide-react';

const FarmForm = () => {
  const { t } = useLanguage();
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);
  const [form, setForm] = useState({ farmName: '', location: '', farmType: 'BROILER', capacity: '', assignedFarmManagerId: '' });
  const [managers, setManagers] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(isEdit);

  useEffect(() => {
    userApi.getAll().then((r) => setManagers(r.data.data?.filter((u) => u.role === 'FARM_MANAGER') || []));
    if (isEdit) {
      setFetching(true);
      farmApi.getById(id)
        .then((r) => {
          const f = r.data.data;
          setForm({
            farmName: f.farmName,
            location: f.location || '',
            farmType: f.farmType,
            capacity: f.capacity || '',
            assignedFarmManagerId: f.assignedFarmManagerId ? String(f.assignedFarmManagerId) : '',
          });
        })
        .catch(() => setError('Failed to load farm.'))
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
        capacity: Number(form.capacity),
        assignedFarmManagerId: form.assignedFarmManagerId ? Number(form.assignedFarmManagerId) : null,
      };
      if (isEdit) await farmApi.update(id, payload);
      else await farmApi.create(payload);
      toast.success(isEdit ? t('farmEditForm.successUpdate') : t('farmEditForm.successCreate'));
      navigate('/farms');
    } catch (err) {
      setError(err.response?.data?.message || t('farmEditForm.error'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <EnterpriseFormPage
      title={isEdit ? t('farmEditForm.editTitle') : t('farmEditForm.addTitle')}
      subtitle="Farm profile, capacity, location, and assigned manager"
      breadcrumbs={['Farm', 'Farms', isEdit ? 'Edit' : 'New']}
      backTo="/farms"
      loading={fetching}
      error={error}
    >
      <form onSubmit={handleSubmit} className="space-y-8">
        <section className="space-y-4">
          <div className="flex items-center gap-2 text-sm font-bold text-slate-500 uppercase tracking-wider">
            <Building2 className="h-4 w-4" /> Basic Information
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2 md:col-span-2">
              <Label htmlFor="farmName">{t('farmEditForm.farmName')} *</Label>
              <Input id="farmName" value={form.farmName} onChange={(e) => setForm({ ...form, farmName: e.target.value })} required />
            </div>
            <div className="space-y-2">
              <Label htmlFor="location">{t('farmEditForm.location')}</Label>
              <Input id="location" value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="farmType">{t('farmEditForm.farmType')}</Label>
              <Select value={form.farmType} onValueChange={(value) => setForm({ ...form, farmType: value })}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="BROILER">{t('options.BROILER')}</SelectItem>
                  <SelectItem value="MIXED">{t('options.MIXED')}</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </section>

        <section className="space-y-4 pt-6 border-t">
          <div className="flex items-center gap-2 text-sm font-bold text-slate-500 uppercase tracking-wider">
            <Settings className="h-4 w-4" /> Capacity & Management
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="capacity">{t('farmEditForm.capacity')}</Label>
              <Input id="capacity" type="number" min="0" value={form.capacity} onChange={(e) => setForm({ ...form, capacity: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="manager">{t('farmEditForm.manager')}</Label>
              <Select value={form.assignedFarmManagerId || 'none'} onValueChange={(value) => setForm({ ...form, assignedFarmManagerId: value === 'none' ? '' : value })}>
                <SelectTrigger><SelectValue placeholder={t('forms.select')} /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">{t('forms.select')}</SelectItem>
                  {managers.map((m) => (
                    <SelectItem key={m.id} value={String(m.id)}>{m.fullName}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        </section>

        <div className="flex gap-3 pt-4 border-t">
          <Button type="submit" disabled={loading} className="bg-primary hover:bg-primary/90">
            {loading ? t('forms.saving') : t('forms.save')}
          </Button>
          <Button type="button" variant="outline" onClick={() => navigate('/farms')}>{t('forms.cancel')}</Button>
        </div>
      </form>
    </EnterpriseFormPage>
  );
};

export default FarmForm;
