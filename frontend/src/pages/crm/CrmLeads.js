import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { crmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import { Button } from '../../components/ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from '../../components/ui/dialog';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { Plus } from 'lucide-react';
import { toast } from 'react-toastify';

const LEAD_SOURCES = ['WEBSITE', 'REFERRAL', 'SOCIAL_MEDIA', 'EVENT', 'TRAINING', 'CONSULTANCY', 'FARM_SERVICE', 'PHARMACY', 'MARKETING', 'OTHER'];
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

const emptyForm = {
  name: '', company: '', contactPerson: '', phone: '', email: '', source: 'OTHER', industry: '', notes: '', priority: 'MEDIUM',
};

const parseDuplicateIds = (message) => {
  if (!message) return [];
  const match = message.match(/Possible duplicate clients found \[(.+?)\]/);
  if (!match) return [];
  return match[1].split(',').map((part) => {
    const [id, ...rest] = part.trim().split(':');
    return { id: id?.trim(), name: rest.join(':').trim() || id };
  }).filter((d) => d.id);
};

const CrmLeads = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [leads, setLeads] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);

  const [convertOpen, setConvertOpen] = useState(false);
  const [convertLead, setConvertLead] = useState(null);
  const [convertForm, setConvertForm] = useState({
    opportunityTitle: '', existingClientId: '', forceCreate: false, confirmDuplicate: false,
  });
  const [duplicates, setDuplicates] = useState([]);
  const [convertError, setConvertError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = statusFilter !== 'ALL' ? { status: statusFilter } : undefined;
      const res = await crmApi.getLeads(params);
      setLeads(res.data.data || []);
    } catch {
      setError('Failed to load leads.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => { load(); }, [load]);

  const create = async (e) => {
    e.preventDefault();
    try {
      await crmApi.createLead({
        ...form,
        company: form.company || undefined,
        contactPerson: form.contactPerson || undefined,
        phone: form.phone || undefined,
        email: form.email || undefined,
        industry: form.industry || undefined,
        notes: form.notes || undefined,
      });
      toast.success('Lead created');
      setOpen(false);
      setForm(emptyForm);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Create failed');
    }
  };

  const openConvert = (lead) => {
    setConvertLead(lead);
    setDuplicates([]);
    setConvertError(null);
    setConvertForm({
      opportunityTitle: `${lead.name || lead.company || 'Opportunity'}`,
      existingClientId: '',
      forceCreate: false,
      confirmDuplicate: false,
    });
    setConvertOpen(true);
  };

  const submitConvert = async (e) => {
    e.preventDefault();
    if (!convertLead) return;
    setConvertError(null);
    try {
      const payload = {
        opportunityTitle: convertForm.opportunityTitle || undefined,
        forceCreate: !!convertForm.forceCreate,
        confirmDuplicate: !!convertForm.confirmDuplicate,
      };
      if (convertForm.existingClientId) {
        payload.existingClientId = Number(convertForm.existingClientId);
      }
      const res = await crmApi.convertLead(convertLead.id, payload);
      toast.success('Lead converted');
      setConvertOpen(false);
      const converted = res.data?.data;
      if (converted?.convertedClientId) {
        navigate(`/crm/clients/${converted.convertedClientId}`);
      } else if (converted?.convertedOpportunityId) {
        navigate(`/crm/opportunities/${converted.convertedOpportunityId}`);
      } else {
        load();
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Convert failed';
      setConvertError(msg);
      const dups = parseDuplicateIds(msg);
      if (dups.length) setDuplicates(dups);
      toast.error(msg);
    }
  };

  const filtered = useMemo(() => leads.filter((r) => {
    if (!search.trim()) return true;
    const q = search.trim().toLowerCase();
    return `${r.leadNumber || ''} ${r.name || ''} ${r.company || ''} ${r.phone || ''} ${r.email || ''}`.toLowerCase().includes(q);
  }), [leads, search]);

  const cols = [
    { accessorKey: 'leadNumber', header: 'Lead #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.leadNumber}</span> },
    { accessorKey: 'name', header: 'Name' },
    { accessorKey: 'company', header: 'Company' },
    { accessorKey: 'phone', header: 'Phone' },
    { accessorKey: 'source', header: 'Source' },
    { accessorKey: 'priority', header: 'Priority' },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <StatusBadge status={row.original.status} /> },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => {
        const lead = row.original;
        return (
          <div className="flex flex-wrap gap-1 justify-end">
            <Button size="sm" variant="outline" onClick={() => navigate(`/crm/leads/${lead.id}`)}>Open</Button>
            {hasPermission(PERMISSIONS.CRM_LEAD_CONVERT) && !['CONVERTED', 'LOST', 'UNQUALIFIED'].includes(lead.status) && (
              <Button size="sm" onClick={() => openConvert(lead)}>Convert</Button>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <EnterprisePage
      title="Leads"
      subtitle="Commercial pipeline leads — convert to customer and opportunity"
      breadcrumbs={['CRM', 'Leads']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={hasPermission(PERMISSIONS.CRM_LEAD) && (
        <Button size="sm" onClick={() => setOpen(true)}><Plus className="h-4 w-4 mr-1" /> New Lead</Button>
      )}
    >
      <div className="mb-4 flex flex-wrap gap-3">
        <Input className="max-w-xs" placeholder="Search…" value={search} onChange={(e) => setSearch(e.target.value)} />
        <Select value={statusFilter} onValueChange={setStatusFilter}>
          <SelectTrigger className="w-44"><SelectValue /></SelectTrigger>
          <SelectContent>
            {['ALL', 'NEW', 'CONTACTED', 'QUALIFIED', 'UNQUALIFIED', 'CONVERTED', 'LOST'].map((s) => (
              <SelectItem key={s} value={s}>{s === 'ALL' ? 'All statuses' : s}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      {filtered.length === 0 && !loading ? (
        <p className="text-sm text-slate-500 py-10 text-center">No leads yet.</p>
      ) : (
        <DataTable columns={cols} data={filtered} />
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>Create Lead</DialogTitle></DialogHeader>
          <form onSubmit={create} className="space-y-3">
            <div className="space-y-1"><Label>Name</Label><Input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
            <div className="space-y-1"><Label>Company</Label><Input value={form.company} onChange={(e) => setForm({ ...form, company: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Phone</Label><Input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></div>
              <div className="space-y-1"><Label>Email</Label><Input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label>Source</Label>
                <Select value={form.source} onValueChange={(v) => setForm({ ...form, source: v })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>{LEAD_SOURCES.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}</SelectContent>
                </Select>
              </div>
              <div className="space-y-1">
                <Label>Priority</Label>
                <Select value={form.priority} onValueChange={(v) => setForm({ ...form, priority: v })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>{PRIORITIES.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}</SelectContent>
                </Select>
              </div>
            </div>
            <div className="space-y-1"><Label>Notes</Label><Textarea value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></div>
            <DialogFooter><Button type="submit">Create</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={convertOpen} onOpenChange={setConvertOpen}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Convert Lead</DialogTitle>
            <DialogDescription>
              Creates a customer and opportunity from {convertLead?.name || 'this lead'}.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={submitConvert} className="space-y-3">
            <div className="space-y-1">
              <Label>Opportunity Title</Label>
              <Input value={convertForm.opportunityTitle} onChange={(e) => setConvertForm({ ...convertForm, opportunityTitle: e.target.value })} />
            </div>
            <div className="space-y-1">
              <Label>Link Existing Client ID (optional)</Label>
              <Input
                placeholder="Client ID"
                value={convertForm.existingClientId}
                onChange={(e) => setConvertForm({ ...convertForm, existingClientId: e.target.value })}
              />
            </div>
            {duplicates.length > 0 && (
              <div className="rounded-md border border-amber-200 bg-amber-50 p-3 text-sm space-y-2">
                <p className="font-semibold text-amber-900">Possible duplicates</p>
                {duplicates.map((d) => (
                  <button
                    key={d.id}
                    type="button"
                    className="block w-full text-left text-amber-800 hover:underline"
                    onClick={() => setConvertForm({ ...convertForm, existingClientId: d.id, forceCreate: false })}
                  >
                    #{d.id} — {d.name}
                  </button>
                ))}
                <p className="text-xs text-amber-700">Select a match above, or confirm force-create below.</p>
              </div>
            )}
            {convertError && !duplicates.length && (
              <p className="text-sm text-rose-600">{convertError}</p>
            )}
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={convertForm.confirmDuplicate}
                onChange={(e) => setConvertForm({ ...convertForm, confirmDuplicate: e.target.checked })}
              />
              Confirm I reviewed duplicate candidates
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={convertForm.forceCreate}
                onChange={(e) => setConvertForm({ ...convertForm, forceCreate: e.target.checked })}
              />
              Force create new client despite duplicates
            </label>
            <DialogFooter>
              <Button type="submit">Convert</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default CrmLeads;
