import React, { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { consultancyApi, userApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../../components/ui/dialog';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatCurrency, formatDate } from '../../utils';
import { toast } from 'react-toastify';

const ACTIVITY_TYPES = [
  'SITE_VISIT', 'FARM_INSPECTION', 'CLIENT_MEETING', 'ASSESSMENT', 'ADVISORY_SESSION',
  'FOLLOW_UP', 'REPORT_PREPARATION', 'REMOTE_CONSULTATION', 'OTHER',
];

const ConsultancyEngagementDetail = () => {
  const { id } = useParams();
  const { hasPermission } = useAuth();
  const [eng, setEng] = useState(null);
  const [activities, setActivities] = useState([]);
  const [deliverables, setDeliverables] = useState([]);
  const [contract, setContract] = useState(null);
  const [history, setHistory] = useState([]);
  const [consultants, setConsultants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [payOpen, setPayOpen] = useState(false);
  const [assignOpen, setAssignOpen] = useState(false);
  const [activityOpen, setActivityOpen] = useState(false);
  const [deliverableOpen, setDeliverableOpen] = useState(false);
  const [contractOpen, setContractOpen] = useState(false);

  const [payForm, setPayForm] = useState({ amount: '', paymentMethod: 'CASH', paymentDate: '', notes: '' });
  const [consultantId, setConsultantId] = useState('');
  const [activityForm, setActivityForm] = useState({
    activityType: 'SITE_VISIT', activityDate: '', description: '', location: '', durationMinutes: '', findings: '', recommendations: '',
  });
  const [deliverableForm, setDeliverableForm] = useState({
    title: '', description: '', dueDate: '', billableAmount: '', billable: false, notes: '',
  });
  const [contractForm, setContractForm] = useState({
    title: '', scope: '', terms: '', startDate: '', endDate: '', contractValue: '', currency: 'ETB', notes: '',
  });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [engRes, actRes, delRes, userRes] = await Promise.all([
        consultancyApi.getEngagement(id),
        consultancyApi.getActivities(id).catch(() => ({ data: { data: [] } })),
        consultancyApi.getDeliverables(id).catch(() => ({ data: { data: [] } })),
        userApi.getAll().catch(() => ({ data: { data: [] } })),
      ]);
      const engagement = engRes.data.data;
      setEng(engagement);
      setActivities(actRes.data?.data || []);
      setDeliverables(delRes.data?.data || []);

      const users = userRes.data?.data || userRes.data || [];
      setConsultants((Array.isArray(users) ? users : []).filter((u) => {
        const role = u.role || u.roles?.[0];
        return ['CONSULTANT', 'CRM_MANAGER', 'ADMIN', 'GENERAL_MANAGER'].includes(role);
      }));

      try {
        const cRes = await consultancyApi.getContractByEngagement(id);
        setContract(cRes.data.data);
      } catch {
        setContract(null);
      }

      if (engagement?.customerId) {
        try {
          const hRes = await consultancyApi.getClientHistory(engagement.customerId);
          setHistory(hRes.data?.data?.events || []);
        } catch {
          setHistory([]);
        }
      } else {
        setHistory([]);
      }
    } catch {
      setError('Failed to load engagement.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const act = async (fn, msg) => {
    try {
      await fn();
      toast.success(msg);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    }
  };

  const submitPay = async (e) => {
    e.preventDefault();
    await act(() => consultancyApi.recordPayment(id, {
      amount: parseFloat(payForm.amount),
      paymentMethod: payForm.paymentMethod,
      paymentDate: payForm.paymentDate || undefined,
      notes: payForm.notes || undefined,
    }), 'Payment recorded');
    setPayOpen(false);
  };

  const submitAssign = async (e) => {
    e.preventDefault();
    await act(() => consultancyApi.assignConsultant(id, {
      consultantId: Number(consultantId),
      asLead: true,
    }), 'Consultant assigned');
    setAssignOpen(false);
  };

  const submitActivity = async (e) => {
    e.preventDefault();
    await act(() => consultancyApi.createActivity({
      engagementId: Number(id),
      activityType: activityForm.activityType,
      activityDate: activityForm.activityDate,
      description: activityForm.description,
      location: activityForm.location || undefined,
      durationMinutes: activityForm.durationMinutes ? parseInt(activityForm.durationMinutes, 10) : undefined,
      findings: activityForm.findings || undefined,
      recommendations: activityForm.recommendations || undefined,
    }), 'Activity created');
    setActivityOpen(false);
  };

  const submitDeliverable = async (e) => {
    e.preventDefault();
    await act(() => consultancyApi.createDeliverable({
      engagementId: Number(id),
      title: deliverableForm.title,
      description: deliverableForm.description || undefined,
      dueDate: deliverableForm.dueDate || undefined,
      billable: !!deliverableForm.billable,
      billableAmount: deliverableForm.billableAmount ? parseFloat(deliverableForm.billableAmount) : undefined,
      notes: deliverableForm.notes || undefined,
    }), 'Deliverable created');
    setDeliverableOpen(false);
  };

  const submitContract = async (e) => {
    e.preventDefault();
    await act(() => consultancyApi.createContract({
      engagementId: Number(id),
      title: contractForm.title,
      scope: contractForm.scope || undefined,
      terms: contractForm.terms || undefined,
      startDate: contractForm.startDate || undefined,
      endDate: contractForm.endDate || undefined,
      contractValue: parseFloat(contractForm.contractValue) || 0,
      currency: contractForm.currency || 'ETB',
      notes: contractForm.notes || undefined,
    }), 'Contract created');
    setContractOpen(false);
  };

  return (
    <EnterprisePage
      title={eng?.engagementNumber || 'Engagement'}
      subtitle={eng ? `${eng.title} · ${eng.customerName || ''}` : ''}
      breadcrumbs={['Consultancy', 'Engagements', eng?.engagementNumber]}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={
        <div className="flex flex-wrap gap-2">
          {hasPermission(PERMISSIONS.EDIT_CONSULTANCY) && eng?.status === 'DRAFT' && (
            <Button size="sm" onClick={() => act(() => consultancyApi.proposeEngagement(id), 'Proposed')}>Propose</Button>
          )}
          {hasPermission(PERMISSIONS.APPROVE_CONSULTANCY) && eng?.status === 'PROPOSED' && (
            <Button size="sm" onClick={() => act(() => consultancyApi.approveEngagement(id), 'Approved')}>Approve</Button>
          )}
          {(hasPermission(PERMISSIONS.APPROVE_CONSULTANCY) || hasPermission(PERMISSIONS.EDIT_CONSULTANCY)) && eng?.status === 'APPROVED' && (
            <Button size="sm" onClick={() => act(() => consultancyApi.activateEngagement(id), 'Activated')}>Activate</Button>
          )}
          {hasPermission(PERMISSIONS.EDIT_CONSULTANCY) && eng?.status === 'ACTIVE' && (
            <Button size="sm" variant="outline" onClick={() => act(() => consultancyApi.holdEngagement(id), 'On hold')}>Hold</Button>
          )}
          {hasPermission(PERMISSIONS.EDIT_CONSULTANCY) && eng?.status === 'ON_HOLD' && (
            <Button size="sm" onClick={() => act(() => consultancyApi.resumeEngagement(id), 'Resumed')}>Resume</Button>
          )}
          {hasPermission(PERMISSIONS.COMPLETE_CONSULTANCY) && ['ACTIVE', 'ON_HOLD'].includes(eng?.status) && (
            <Button size="sm" onClick={() => {
              if (!window.confirm('Mark engagement complete?')) return;
              act(() => consultancyApi.completeEngagement(id), 'Completed');
            }}>Complete</Button>
          )}
          {hasPermission(PERMISSIONS.CANCEL_CONSULTANCY) && !['COMPLETED', 'CANCELLED'].includes(eng?.status) && (
            <Button size="sm" variant="destructive" onClick={() => {
              if (!window.confirm('Cancel this engagement?')) return;
              act(() => consultancyApi.cancelEngagement(id), 'Cancelled');
            }}>Cancel</Button>
          )}
          {hasPermission(PERMISSIONS.ASSIGN_CONSULTANCY) && (
            <Button size="sm" variant="outline" onClick={() => setAssignOpen(true)}>Assign</Button>
          )}
          {(hasPermission(PERMISSIONS.CONSULTANCY_INVOICE) || hasPermission(PERMISSIONS.CREATE_INCOME)) && ['ACTIVE', 'APPROVED', 'COMPLETED'].includes(eng?.status) && (
            <Button size="sm" variant="outline" onClick={() => act(() => consultancyApi.createEngagementInvoice(id), 'Invoice created')}>Create Invoice</Button>
          )}
          {hasPermission(PERMISSIONS.CONSULTANCY_PAYMENT) && eng?.paymentStatus !== 'PAID' && (
            <Button size="sm" variant="outline" onClick={() => {
              setPayForm({ amount: String(eng.outstandingAmount || ''), paymentMethod: 'CASH', paymentDate: '', notes: '' });
              setPayOpen(true);
            }}>Record Payment</Button>
          )}
        </div>
      }
    >
      {eng && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <Card className="border-none shadow-sm">
              <CardHeader><CardTitle className="text-base">Overview</CardTitle></CardHeader>
              <CardContent className="space-y-2 text-sm">
                <div className="flex justify-between"><span>Status</span><Badge>{eng.status}</Badge></div>
                <div className="flex justify-between"><span>Customer</span><span className="font-semibold">{eng.customerName}</span></div>
                <div className="flex justify-between"><span>Service</span><span>{eng.serviceName || '—'}</span></div>
                <div className="flex justify-between"><span>Lead</span><span>{eng.leadConsultantName || '—'}</span></div>
                <div className="flex justify-between"><span>Start</span><span>{formatDate(eng.startDate)}</span></div>
                <div className="flex justify-between"><span>Expected End</span><span>{formatDate(eng.expectedEndDate)}</span></div>
                <div className="flex justify-between"><span>Billing</span><span>{eng.billingType || '—'}</span></div>
                <div className="flex justify-between"><span>Location</span><span>{eng.location || '—'}</span></div>
              </CardContent>
            </Card>

            <Card className="border-none shadow-sm">
              <CardHeader><CardTitle className="text-base">Financial</CardTitle></CardHeader>
              <CardContent className="space-y-2 text-sm">
                <div className="flex justify-between"><span>Agreed</span><span className="font-mono font-bold">{formatCurrency(eng.agreedAmount)}</span></div>
                <div className="flex justify-between"><span>Invoiced</span><span className="font-mono">{formatCurrency(eng.invoicedAmount)}</span></div>
                <div className="flex justify-between"><span>Paid</span><span className="font-mono text-emerald-700">{formatCurrency(eng.paidAmount)}</span></div>
                <div className="flex justify-between"><span>Outstanding</span><span className="font-mono text-amber-700">{formatCurrency(eng.outstandingAmount)}</span></div>
                <div className="flex justify-between"><span>Revenue recognized</span><span className="font-mono">{formatCurrency(eng.revenueRecognized)}</span></div>
                <div className="flex justify-between"><span>Invoice</span><span className="font-mono">{eng.invoiceNumber || '—'}</span></div>
                <Badge variant="outline">{eng.paymentStatus}</Badge>
                {eng.profitNote && <p className="text-xs text-slate-500 pt-2 border-t">{eng.profitNote}</p>}
              </CardContent>
            </Card>

            <Card className="border-none shadow-sm">
              <CardHeader><CardTitle className="text-base">Scope</CardTitle></CardHeader>
              <CardContent className="space-y-2 text-sm">
                <p className="text-slate-600 whitespace-pre-wrap">{eng.description || 'No description.'}</p>
                {eng.objectives && (
                  <div>
                    <p className="text-xs font-semibold text-slate-400 uppercase">Objectives</p>
                    <p className="whitespace-pre-wrap">{eng.objectives}</p>
                  </div>
                )}
                {eng.scope && (
                  <div>
                    <p className="text-xs font-semibold text-slate-400 uppercase">Scope</p>
                    <p className="whitespace-pre-wrap">{eng.scope}</p>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          <Card className="border-none shadow-sm">
            <CardHeader className="flex flex-row items-center justify-between">
              <div>
                <CardTitle className="text-base">Activities</CardTitle>
                <CardDescription>Site visits, meetings, and advisory sessions</CardDescription>
              </div>
              {hasPermission(PERMISSIONS.CONSULTANCY_ACTIVITY) && (
                <Button size="sm" onClick={() => {
                  setActivityForm({
                    activityType: 'SITE_VISIT', activityDate: '', description: '', location: '', durationMinutes: '', findings: '', recommendations: '',
                  });
                  setActivityOpen(true);
                }}>Add Activity</Button>
              )}
            </CardHeader>
            <CardContent className="space-y-3">
              {activities.length === 0 ? (
                <p className="text-sm text-slate-500 text-center py-4">No activities yet.</p>
              ) : activities.map((a) => (
                <div key={a.id} className="flex flex-wrap justify-between gap-2 border rounded-lg p-3 text-sm">
                  <div>
                    <p className="font-semibold">{a.activityNumber} · {a.activityType}</p>
                    <p className="text-xs text-slate-500">{formatDate(a.activityDate)} · {a.description}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant="outline">{a.status}</Badge>
                    {hasPermission(PERMISSIONS.CONSULTANCY_ACTIVITY) && a.status === 'PLANNED' && (
                      <Button size="sm" variant="outline" onClick={() => act(() => consultancyApi.startActivity(a.id), 'Started')}>Start</Button>
                    )}
                    {hasPermission(PERMISSIONS.CONSULTANCY_ACTIVITY) && ['PLANNED', 'IN_PROGRESS'].includes(a.status) && (
                      <Button size="sm" onClick={() => act(() => consultancyApi.completeActivity(a.id), 'Completed')}>Complete</Button>
                    )}
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader className="flex flex-row items-center justify-between">
              <div>
                <CardTitle className="text-base">Deliverables</CardTitle>
                <CardDescription>Milestones and billable outputs</CardDescription>
              </div>
              {hasPermission(PERMISSIONS.CONSULTANCY_DELIVERABLE) && (
                <Button size="sm" onClick={() => {
                  setDeliverableForm({ title: '', description: '', dueDate: '', billableAmount: '', billable: false, notes: '' });
                  setDeliverableOpen(true);
                }}>Add Deliverable</Button>
              )}
            </CardHeader>
            <CardContent className="space-y-3">
              {deliverables.length === 0 ? (
                <p className="text-sm text-slate-500 text-center py-4">No deliverables yet.</p>
              ) : deliverables.map((d) => (
                <div key={d.id} className="flex flex-wrap justify-between gap-2 border rounded-lg p-3 text-sm">
                  <div>
                    <p className="font-semibold">{d.deliverableNumber} · {d.title}</p>
                    <p className="text-xs text-slate-500">Due {formatDate(d.dueDate)} · {d.billable ? formatCurrency(d.billableAmount) : 'Non-billable'}</p>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <Badge variant="outline">{d.status}</Badge>
                    {hasPermission(PERMISSIONS.CONSULTANCY_DELIVERABLE) && d.status === 'PENDING' && (
                      <Button size="sm" variant="outline" onClick={() => act(() => consultancyApi.startDeliverable(d.id), 'Started')}>Start</Button>
                    )}
                    {hasPermission(PERMISSIONS.CONSULTANCY_DELIVERABLE) && ['PENDING', 'IN_PROGRESS'].includes(d.status) && (
                      <Button size="sm" variant="outline" onClick={() => act(() => consultancyApi.submitDeliverable(d.id), 'Submitted')}>Submit</Button>
                    )}
                    {hasPermission(PERMISSIONS.APPROVE_CONSULTANCY) && d.status === 'SUBMITTED' && (
                      <>
                        <Button size="sm" onClick={() => act(() => consultancyApi.approveDeliverable(d.id), 'Approved')}>Approve</Button>
                        <Button size="sm" variant="destructive" onClick={() => {
                          if (!window.confirm('Reject deliverable?')) return;
                          act(() => consultancyApi.rejectDeliverable(d.id, { completionNotes: 'Rejected' }), 'Rejected');
                        }}>Reject</Button>
                      </>
                    )}
                    {(hasPermission(PERMISSIONS.CONSULTANCY_INVOICE) || hasPermission(PERMISSIONS.CREATE_INCOME)) && d.billable && d.status === 'APPROVED' && !d.invoiceId && (
                      <Button size="sm" variant="outline" onClick={() => act(() => consultancyApi.billMilestone(d.id), 'Milestone invoiced')}>Invoice</Button>
                    )}
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader className="flex flex-row items-center justify-between">
              <div>
                <CardTitle className="text-base">Contract</CardTitle>
                <CardDescription>Engagement agreement</CardDescription>
              </div>
              {hasPermission(PERMISSIONS.CONSULTANCY_CONTRACT) && !contract && (
                <Button size="sm" onClick={() => {
                  setContractForm({
                    title: eng.title || '',
                    scope: eng.scope || '',
                    terms: '',
                    startDate: eng.startDate || '',
                    endDate: eng.expectedEndDate || '',
                    contractValue: String(eng.agreedAmount || ''),
                    currency: eng.currency || 'ETB',
                    notes: '',
                  });
                  setContractOpen(true);
                }}>Create Contract</Button>
              )}
              {hasPermission(PERMISSIONS.CONSULTANCY_CONTRACT) && contract?.status === 'DRAFT' && (
                <Button size="sm" onClick={() => act(() => consultancyApi.activateContract(contract.id), 'Contract activated')}>Activate</Button>
              )}
            </CardHeader>
            <CardContent className="text-sm space-y-2">
              {!contract ? (
                <p className="text-slate-500 text-center py-4">No contract on file.</p>
              ) : (
                <>
                  <div className="flex justify-between"><span>Number</span><span className="font-mono font-bold">{contract.contractNumber}</span></div>
                  <div className="flex justify-between"><span>Title</span><span>{contract.title}</span></div>
                  <div className="flex justify-between"><span>Value</span><span className="font-mono">{formatCurrency(contract.contractValue)}</span></div>
                  <div className="flex justify-between"><span>Period</span><span>{formatDate(contract.startDate)} – {formatDate(contract.endDate)}</span></div>
                  <div className="flex justify-between"><span>Status</span><Badge>{contract.status}</Badge></div>
                </>
              )}
            </CardContent>
          </Card>

          {eng.customerId && (
            <Card className="border-none shadow-sm">
              <CardHeader>
                <CardTitle className="text-base">Client Timeline</CardTitle>
                <CardDescription>Consultancy history for this customer</CardDescription>
              </CardHeader>
              <CardContent className="space-y-2">
                {history.length === 0 ? (
                  <p className="text-sm text-slate-500 text-center py-4">No history events.</p>
                ) : history.map((ev, idx) => (
                  <div key={`${ev.type}-${ev.entityId}-${idx}`} className="flex justify-between border-b py-2 text-sm">
                    <div>
                      <p className="font-semibold text-xs text-slate-400">{ev.type}</p>
                      <p>{ev.summary}</p>
                    </div>
                    <span className="text-xs text-slate-400 whitespace-nowrap">{ev.at ? String(ev.at).slice(0, 19).replace('T', ' ') : '—'}</span>
                  </div>
                ))}
              </CardContent>
            </Card>
          )}
        </div>
      )}

      <Dialog open={payOpen} onOpenChange={setPayOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Record Payment</DialogTitle></DialogHeader>
          <form onSubmit={submitPay} className="space-y-3">
            <div className="space-y-1"><Label>Amount</Label><Input type="number" step="0.01" required value={payForm.amount} onChange={(e) => setPayForm({ ...payForm, amount: e.target.value })} /></div>
            <div className="space-y-1">
              <Label>Method</Label>
              <Select value={payForm.paymentMethod} onValueChange={(v) => setPayForm({ ...payForm, paymentMethod: v })}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  {['CASH', 'BANK_TRANSFER', 'MOBILE_MONEY', 'CREDIT'].map((m) => <SelectItem key={m} value={m}>{m}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1"><Label>Date</Label><Input type="date" value={payForm.paymentDate} onChange={(e) => setPayForm({ ...payForm, paymentDate: e.target.value })} /></div>
            <DialogFooter><Button type="submit">Save Payment</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={assignOpen} onOpenChange={setAssignOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Assign Consultant</DialogTitle></DialogHeader>
          <form onSubmit={submitAssign} className="space-y-3">
            <div className="space-y-1">
              <Label>Consultant</Label>
              <Select value={consultantId} onValueChange={setConsultantId}>
                <SelectTrigger><SelectValue placeholder="Select user" /></SelectTrigger>
                <SelectContent>
                  {consultants.map((u) => (
                    <SelectItem key={u.id} value={String(u.id)}>{u.fullName || u.name || u.username || `#${u.id}`}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <DialogFooter><Button type="submit" disabled={!consultantId}>Assign</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={activityOpen} onOpenChange={setActivityOpen}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>Add Activity</DialogTitle></DialogHeader>
          <form onSubmit={submitActivity} className="space-y-3">
            <div className="space-y-1">
              <Label>Type</Label>
              <Select value={activityForm.activityType} onValueChange={(v) => setActivityForm({ ...activityForm, activityType: v })}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>{ACTIVITY_TYPES.map((t) => <SelectItem key={t} value={t}>{t}</SelectItem>)}</SelectContent>
              </Select>
            </div>
            <div className="space-y-1"><Label>Date</Label><Input type="date" required value={activityForm.activityDate} onChange={(e) => setActivityForm({ ...activityForm, activityDate: e.target.value })} /></div>
            <div className="space-y-1"><Label>Description</Label><Textarea required value={activityForm.description} onChange={(e) => setActivityForm({ ...activityForm, description: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Location</Label><Input value={activityForm.location} onChange={(e) => setActivityForm({ ...activityForm, location: e.target.value })} /></div>
              <div className="space-y-1"><Label>Duration (min)</Label><Input type="number" value={activityForm.durationMinutes} onChange={(e) => setActivityForm({ ...activityForm, durationMinutes: e.target.value })} /></div>
            </div>
            <DialogFooter><Button type="submit">Create</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={deliverableOpen} onOpenChange={setDeliverableOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader><DialogTitle>Add Deliverable</DialogTitle></DialogHeader>
          <form onSubmit={submitDeliverable} className="space-y-3">
            <div className="space-y-1"><Label>Title</Label><Input required value={deliverableForm.title} onChange={(e) => setDeliverableForm({ ...deliverableForm, title: e.target.value })} /></div>
            <div className="space-y-1"><Label>Description</Label><Textarea value={deliverableForm.description} onChange={(e) => setDeliverableForm({ ...deliverableForm, description: e.target.value })} /></div>
            <div className="space-y-1"><Label>Due Date</Label><Input type="date" value={deliverableForm.dueDate} onChange={(e) => setDeliverableForm({ ...deliverableForm, dueDate: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1 flex items-end gap-2 pb-2">
                <input type="checkbox" id="billable" checked={deliverableForm.billable} onChange={(e) => setDeliverableForm({ ...deliverableForm, billable: e.target.checked })} />
                <Label htmlFor="billable">Billable</Label>
              </div>
              <div className="space-y-1"><Label>Amount</Label><Input type="number" step="0.01" value={deliverableForm.billableAmount} onChange={(e) => setDeliverableForm({ ...deliverableForm, billableAmount: e.target.value })} disabled={!deliverableForm.billable} /></div>
            </div>
            <DialogFooter><Button type="submit">Create</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={contractOpen} onOpenChange={setContractOpen}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>Create Contract</DialogTitle></DialogHeader>
          <form onSubmit={submitContract} className="space-y-3">
            <div className="space-y-1"><Label>Title</Label><Input required value={contractForm.title} onChange={(e) => setContractForm({ ...contractForm, title: e.target.value })} /></div>
            <div className="space-y-1"><Label>Scope</Label><Textarea value={contractForm.scope} onChange={(e) => setContractForm({ ...contractForm, scope: e.target.value })} /></div>
            <div className="space-y-1"><Label>Terms</Label><Textarea value={contractForm.terms} onChange={(e) => setContractForm({ ...contractForm, terms: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Start</Label><Input type="date" value={contractForm.startDate} onChange={(e) => setContractForm({ ...contractForm, startDate: e.target.value })} /></div>
              <div className="space-y-1"><Label>End</Label><Input type="date" value={contractForm.endDate} onChange={(e) => setContractForm({ ...contractForm, endDate: e.target.value })} /></div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Value</Label><Input type="number" step="0.01" required value={contractForm.contractValue} onChange={(e) => setContractForm({ ...contractForm, contractValue: e.target.value })} /></div>
              <div className="space-y-1"><Label>Currency</Label><Input value={contractForm.currency} onChange={(e) => setContractForm({ ...contractForm, currency: e.target.value })} /></div>
            </div>
            <DialogFooter><Button type="submit">Create</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default ConsultancyEngagementDetail;
