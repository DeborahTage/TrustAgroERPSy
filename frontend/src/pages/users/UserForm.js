import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { userApi } from '../../api';
import EnterpriseFormPage from '../../components/layout/EnterpriseFormPage';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useLanguage } from '../../context/LanguageContext';
import { toast } from 'react-toastify';
import { CANONICAL_ROLES, ROLE_LABELS, normalizeRole } from '../../lib/permissions';
import { User, Lock } from 'lucide-react';

const ROLE_OPTIONS = CANONICAL_ROLES;

const UserForm = () => {
  const { t } = useLanguage();
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', password: '', role: 'FARM_MANAGER' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(isEdit);

  useEffect(() => {
    if (isEdit) {
      setFetching(true);
      userApi.getById(id)
        .then((r) => {
          const u = r.data.data;
          setForm({ fullName: u.fullName, email: u.email, phone: u.phone || '', password: '', role: normalizeRole(u.role) || u.role });
        })
        .catch(() => setError('Failed to load user.'))
        .finally(() => setFetching(false));
    }
  }, [id, isEdit]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const payload = { ...form };
      if (isEdit && !payload.password) delete payload.password;
      if (isEdit) await userApi.update(id, payload);
      else await userApi.create(payload);
      toast.success(isEdit ? t('userForm.successUpdate') : t('userForm.successCreate'));
      navigate('/users');
    } catch (err) {
      setError(err.response?.data?.message || t('userForm.error'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <EnterpriseFormPage
      title={isEdit ? t('userForm.editTitle') : t('userForm.addTitle')}
      subtitle={isEdit ? 'Update credentials and role assignment' : 'Provision a new system user with role-based access'}
      breadcrumbs={['Administration', 'Users', isEdit ? 'Edit' : 'New']}
      backTo="/users"
      loading={fetching}
      error={error}
    >
      <form onSubmit={handleSubmit} className="space-y-8">
        <section className="space-y-4">
          <div className="flex items-center gap-2 text-sm font-bold text-slate-500 uppercase tracking-wider">
            <User className="h-4 w-4" /> Personal Information
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2 md:col-span-2">
              <Label htmlFor="fullName">{t('userForm.fullName')} *</Label>
              <Input id="fullName" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
            </div>
            <div className="space-y-2 md:col-span-2">
              <Label htmlFor="email">{t('userForm.email')} *</Label>
              <Input id="email" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required disabled={isEdit} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="phone">{t('userForm.phone')}</Label>
              <Input id="phone" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="role">{t('userForm.role')}</Label>
              <Select value={form.role} onValueChange={(value) => setForm({ ...form, role: value })}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  {ROLE_OPTIONS.map((r) => (
                    <SelectItem key={r} value={r}>{ROLE_LABELS[r] || t(`roles.${r}`)}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        </section>

        <section className="space-y-4 pt-6 border-t">
          <div className="flex items-center gap-2 text-sm font-bold text-slate-500 uppercase tracking-wider">
            <Lock className="h-4 w-4" /> Security
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">{isEdit ? t('userForm.newPassword') : t('userForm.password')} {!isEdit && '*'}</Label>
            <Input id="password" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required={!isEdit} />
            {isEdit && <p className="text-xs text-slate-500">Leave blank to keep current password</p>}
          </div>
        </section>

        <div className="flex gap-3 pt-4 border-t">
          <Button type="submit" disabled={loading} className="bg-primary hover:bg-primary/90">
            {loading ? t('forms.saving') : t('forms.save')}
          </Button>
          <Button type="button" variant="outline" onClick={() => navigate('/users')}>{t('forms.cancel')}</Button>
        </div>
      </form>
    </EnterpriseFormPage>
  );
};

export default UserForm;
