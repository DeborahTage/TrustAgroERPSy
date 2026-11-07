import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { crmApi, userApi, chickenSaleApi } from '../../api';
import EnterpriseFormPage from '../../components/layout/EnterpriseFormPage';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { Badge } from '../../components/ui/badge';
import { useLanguage } from '../../context/LanguageContext';
import { toast } from 'react-toastify';
import { User, MapPin, Building2, Activity, ShoppingBag } from 'lucide-react';

const CrmClientForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { t } = useLanguage();
  const isEdit = Boolean(id);

  const [form, setForm] = useState({
    clientName: '',
    phone: '',
    email: '',
    contactPerson: '',
    taxIdentifier: '',
    customerType: 'FARM',
    region: '',
    city: '',
    location: '',
    address: '',
    farmType: 'Layer',
    farmSize: '',
    numberOfChickens: '',
    status: 'ACTIVE',
    assignedExtensionWorkerId: '',
    notes: '',
  });

  const [workers, setWorkers] = useState([]);
  const [sales, setSales] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(isEdit);

  useEffect(() => {
    userApi.getAll()
      .then((r) => {
        const uList = Array.isArray(r.data?.data) ? r.data.data : (Array.isArray(r.data) ? r.data : []);
        setWorkers(uList.filter((u) => u.role === 'EXTENSION_WORKER' || u.role === 'CRM_MANAGER' || u.role === 'CONSULTANT'));
      })
      .catch(() => setWorkers([]));

    if (isEdit) {
      setFetching(true);
      crmApi.getClient(id)
        .then((r) => {
          const c = r.data?.data || r.data;
          if (c) {
            setForm({
              clientName: c.clientName || '',
              phone: c.phone || '',
              email: c.email || '',
              contactPerson: c.contactPerson || '',
              taxIdentifier: c.taxIdentifier || '',
              customerType: c.customerType || 'FARM',
              region: c.region || '',
              city: c.city || '',
              location: c.location || '',
              address: c.address || '',
              farmType: c.farmType || 'Layer',
              farmSize: c.farmSize || '',
              numberOfChickens: c.numberOfChickens != null ? String(c.numberOfChickens) : '',
              status: c.status || 'ACTIVE',
              assignedExtensionWorkerId: c.assignedExtensionWorkerId ? String(c.assignedExtensionWorkerId) : '',
              notes: c.notes || '',
            });
          }
        })
        .catch(() => setError('Failed to load client details.'))
        .finally(() => setFetching(false));

      chickenSaleApi.getAll({ customerId: id })
        .then((r) => setSales(r.data?.data || []))
        .catch(() => setSales([]));
    }
  }, [id, isEdit]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.clientName.trim()) {
      setError('Client name is required.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const payload = {
        ...form,
        numberOfChickens: form.numberOfChickens ? Number(form.numberOfChickens) : null,
        assignedExtensionWorkerId: form.assignedExtensionWorkerId ? Number(form.assignedExtensionWorkerId) : null,
      };
      if (isEdit) {
        await crmApi.updateClient(id, payload);
        toast.success(t('crmClientForm.successUpdate') || 'Client updated successfully');
      } else {
        await crmApi.createClient(payload);
        toast.success(t('crmClientForm.successCreate') || 'Client registered successfully');
      }
      navigate('/crm/clients');
    } catch (err) {
      const errData = err.response?.data;
      if (errData?.data && typeof errData.data === 'object') {
        const details = Object.entries(errData.data).map(([k, v]) => `${k}: ${v}`).join(', ');
        setError(`Validation failed: ${details}`);
      } else {
        setError(errData?.message || t('crmClientForm.error') || 'Failed to save client');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <EnterpriseFormPage
      title={isEdit ? 'Edit Client' : 'Add New Client'}
      subtitle="Client profile, farm operations, contact details, and assigned extension support"
      breadcrumbs={['CRM', 'Clients', isEdit ? 'Edit' : 'New']}
      backTo="/crm/clients"
      loading={fetching}
      error={error}
    >
      <form onSubmit={handleSubmit} className="space-y-8">
        {/* Section 1: Client Identity */}
        <section className="space-y-4">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-500 uppercase tracking-wider">
            <User className="h-4 w-4 text-primary" /> Client Identity & Contact
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="clientName">Client Name *</Label>
              <Input
                id="clientName"
                value={form.clientName}
                onChange={(e) => setForm({ ...form, clientName: e.target.value })}
                placeholder="e.g. Bishoftu Commercial Layer Farm"
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="contactPerson">Contact Person</Label>
              <Input
                id="contactPerson"
                value={form.contactPerson}
                onChange={(e) => setForm({ ...form, contactPerson: e.target.value })}
                placeholder="e.g. Ato Dawit Bekele"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="phone">Phone Number</Label>
              <Input
                id="phone"
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
                placeholder="e.g. +251 91 123 4567"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Email Address</Label>
              <Input
                id="email"
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                placeholder="client@example.com"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="taxIdentifier">TIN / Tax Identifier</Label>
              <Input
                id="taxIdentifier"
                value={form.taxIdentifier}
                onChange={(e) => setForm({ ...form, taxIdentifier: e.target.value })}
                placeholder="e.g. 0012345678"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="customerType">Customer Category</Label>
              <Select
                value={form.customerType}
                onValueChange={(v) => setForm({ ...form, customerType: v })}
              >
                <SelectTrigger id="customerType"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="FARM">Poultry Farm / Producer</SelectItem>
                  <SelectItem value="COMPANY">Commercial Enterprise / Company</SelectItem>
                  <SelectItem value="INDIVIDUAL">Individual Farmer / Buyer</SelectItem>
                  <SelectItem value="ORGANIZATION">Cooperative / Organization</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </section>

        {/* Section 2: Location & Address */}
        <section className="space-y-4 pt-6 border-t border-border/60">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-500 uppercase tracking-wider">
            <MapPin className="h-4 w-4 text-emerald-600" /> Geographic Location
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="region">Region</Label>
              <Input
                id="region"
                value={form.region}
                onChange={(e) => setForm({ ...form, region: e.target.value })}
                placeholder="e.g. Oromia, Addis Ababa, Sidama"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="city">City / Town</Label>
              <Input
                id="city"
                value={form.city}
                onChange={(e) => setForm({ ...form, city: e.target.value })}
                placeholder="e.g. Bishoftu, Dukem, Hawassa"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="location">Specific Site / Village</Label>
              <Input
                id="location"
                value={form.location}
                onChange={(e) => setForm({ ...form, location: e.target.value })}
                placeholder="e.g. Kebele 04, near expressway exit"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="address">Physical Address</Label>
              <Input
                id="address"
                value={form.address}
                onChange={(e) => setForm({ ...form, address: e.target.value })}
                placeholder="House No / Street Address"
              />
            </div>
          </div>
        </section>

        {/* Section 3: Farm Profile */}
        <section className="space-y-4 pt-6 border-t border-border/60">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-500 uppercase tracking-wider">
            <Building2 className="h-4 w-4 text-amber-600" /> Farm & Production Profile
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label htmlFor="farmType">Farm Production Type</Label>
              <Select
                value={form.farmType}
                onValueChange={(v) => setForm({ ...form, farmType: v })}
              >
                <SelectTrigger id="farmType"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="Layer">Commercial Layer</SelectItem>
                  <SelectItem value="Broiler">Commercial Broiler</SelectItem>
                  <SelectItem value="Breeder">Parent Stock / Breeder</SelectItem>
                  <SelectItem value="Hatchery">Hatchery Operations</SelectItem>
                  <SelectItem value="Mixed Commercial">Mixed Poultry Operations</SelectItem>
                  <SelectItem value="Feed Mill">Feed Miller / Retailer</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="farmSize">Farm Size / Land Area</Label>
              <Input
                id="farmSize"
                value={form.farmSize}
                onChange={(e) => setForm({ ...form, farmSize: e.target.value })}
                placeholder="e.g. 1.5 Hectares / 4 Barns"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="numberOfChickens">Flock Capacity / Bird Count</Label>
              <Input
                id="numberOfChickens"
                type="number"
                min="0"
                value={form.numberOfChickens}
                onChange={(e) => setForm({ ...form, numberOfChickens: e.target.value })}
                placeholder="e.g. 5000"
              />
            </div>
          </div>
        </section>

        {/* Section 4: Lifecycle & Assignment */}
        <section className="space-y-4 pt-6 border-t border-border/60">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-500 uppercase tracking-wider">
            <Activity className="h-4 w-4 text-indigo-600" /> Account Status & Support
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="status">Account Status</Label>
              <Select
                value={form.status}
                onValueChange={(v) => setForm({ ...form, status: v })}
              >
                <SelectTrigger id="status"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="LEAD">Prospective Lead</SelectItem>
                  <SelectItem value="ACTIVE">Active Account</SelectItem>
                  <SelectItem value="INACTIVE">Inactive Account</SelectItem>
                  <SelectItem value="LOST">Lost / Dormant</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="assignedExtensionWorkerId">Assigned Extension Worker / Officer</Label>
              <Select
                value={form.assignedExtensionWorkerId || 'none'}
                onValueChange={(v) => setForm({ ...form, assignedExtensionWorkerId: v === 'none' ? '' : v })}
              >
                <SelectTrigger id="assignedExtensionWorkerId"><SelectValue placeholder="Select worker (optional)" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">-- Unassigned --</SelectItem>
                  {workers.map((w) => (
                    <SelectItem key={w.id} value={String(w.id)}>{w.fullName || w.email} ({w.role?.replace('_', ' ')})</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2 md:col-span-2">
              <Label htmlFor="notes">Notes & Engagement History</Label>
              <Textarea
                id="notes"
                value={form.notes}
                onChange={(e) => setForm({ ...form, notes: e.target.value })}
                placeholder="Record client preferences, feed schedules, or special requirements..."
                rows={3}
              />
            </div>
          </div>
        </section>

        {/* Submit Actions */}
        <div className="flex items-center gap-3 pt-6 border-t border-border/60">
          <Button type="submit" disabled={loading} className="bg-primary hover:bg-primary/90 text-primary-foreground font-semibold px-6">
            {loading ? 'Saving Client...' : (isEdit ? 'Update Client' : 'Save Client')}
          </Button>
          <Button type="button" variant="outline" onClick={() => navigate('/crm/clients')}>
            Cancel
          </Button>
        </div>
      </form>

      {/* Historical Purchases (Edit mode) */}
      {isEdit && (
        <div className="mt-10 rounded-2xl border border-border p-6 bg-card space-y-4">
          <div className="flex items-center gap-2 text-sm font-bold text-foreground">
            <ShoppingBag className="h-4 w-4 text-primary" /> Chicken Purchase Orders
          </div>
          {sales.length === 0 ? (
            <p className="text-sm text-muted-foreground">No purchase records registered for this client yet.</p>
          ) : (
            <div className="divide-y divide-border/60">
              {sales.map((s) => (
                <div key={s.id} className="py-3 flex items-center justify-between text-sm">
                  <div>
                    <span className="font-mono font-bold text-foreground mr-2">{s.saleNumber}</span>
                    <Badge variant="outline" className="text-xs">{s.status}</Badge>
                    <span className="text-muted-foreground ml-2 text-xs">Flock: {s.flockBatchCode || 'N/A'}</span>
                  </div>
                  <div className="font-semibold text-foreground">
                    {Number(s.totalAmount).toLocaleString()} ETB
                    {s.outstandingAmount > 0 && (
                      <span className="text-rose-500 ml-2 text-xs font-normal">
                        ({Number(s.outstandingAmount).toLocaleString()} ETB pending)
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </EnterpriseFormPage>
  );
};

export default CrmClientForm;
