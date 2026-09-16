import React, { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { trainingApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../../components/ui/dialog';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatCurrency, formatDate } from '../../utils';
import { toast } from 'react-toastify';

const TrainingRegistrationDetail = () => {
  const { id } = useParams();
  const { hasPermission } = useAuth();
  const [reg, setReg] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [payOpen, setPayOpen] = useState(false);
  const [attOpen, setAttOpen] = useState(false);
  const [payForm, setPayForm] = useState({ amount: '', paymentMethod: 'CASH', paymentDate: '' });
  const [attForm, setAttForm] = useState({ sessionDate: '', status: 'PRESENT' });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await trainingApi.getRegistration(id);
      setReg(res.data.data);
    } catch {
      setError('Failed to load registration.');
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
    await act(() => trainingApi.pay(id, {
      amount: parseFloat(payForm.amount),
      paymentMethod: payForm.paymentMethod,
      paymentDate: payForm.paymentDate || undefined,
    }), 'Payment recorded');
    setPayOpen(false);
  };

  const submitAtt = async (e) => {
    e.preventDefault();
    await act(() => trainingApi.recordAttendance(id, {
      sessionDate: attForm.sessionDate,
      status: attForm.status,
    }), 'Attendance saved');
    setAttOpen(false);
  };

  return (
    <EnterprisePage
      title={reg?.registrationNumber || 'Registration'}
      subtitle={reg ? `${reg.participantName} · ${reg.sessionCode}` : ''}
      breadcrumbs={['Training', 'Registrations', reg?.registrationNumber]}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={
        <div className="flex flex-wrap gap-2">
          {hasPermission(PERMISSIONS.REGISTER_TRAINING) && reg?.status === 'PENDING' && (
            <Button size="sm" onClick={() => act(() => trainingApi.confirmRegistration(id), 'Confirmed + invoiced')}>Confirm</Button>
          )}
          {hasPermission(PERMISSIONS.TRAINING_PAYMENT) && ['CONFIRMED', 'COMPLETED'].includes(reg?.status) && reg?.paymentStatus !== 'PAID' && (
            <Button size="sm" variant="outline" onClick={() => {
              setPayForm({ amount: String(reg.outstandingAmount || ''), paymentMethod: 'CASH', paymentDate: '' });
              setPayOpen(true);
            }}>Record Payment</Button>
          )}
          {hasPermission(PERMISSIONS.TRAINING_ATTENDANCE) && ['CONFIRMED', 'COMPLETED'].includes(reg?.status) && (
            <Button size="sm" variant="outline" onClick={() => setAttOpen(true)}>Attendance</Button>
          )}
          {hasPermission(PERMISSIONS.COMPLETE_TRAINING) && reg?.status === 'CONFIRMED' && (
            <Button size="sm" onClick={() => act(() => trainingApi.completeRegistration(id), 'Marked completed')}>Mark Completed</Button>
          )}
          {hasPermission(PERMISSIONS.TRAINING_CERTIFICATE) && reg?.status === 'COMPLETED' && !reg?.certificate && (
            <Button size="sm" onClick={() => act(() => trainingApi.issueCertificate(id), 'Certificate issued')}>Issue Certificate</Button>
          )}
          {hasPermission(PERMISSIONS.CANCEL_TRAINING) && ['PENDING', 'CONFIRMED'].includes(reg?.status) && (
            <Button size="sm" variant="destructive" onClick={() => act(() => trainingApi.cancelRegistration(id), 'Cancelled')}>Cancel</Button>
          )}
        </div>
      }
    >
      {reg && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle className="text-base">Participant</CardTitle></CardHeader>
            <CardContent className="space-y-2 text-sm">
              <div className="flex justify-between"><span>Name</span><span className="font-semibold">{reg.participantName}</span></div>
              <div className="flex justify-between"><span>Phone</span><span>{reg.participantPhone || '—'}</span></div>
              <div className="flex justify-between"><span>Email</span><span>{reg.participantEmail || '—'}</span></div>
              <div className="flex justify-between"><span>Organization</span><span>{reg.organization || '—'}</span></div>
              <div className="flex justify-between"><span>CRM Client</span><span>#{reg.clientId}</span></div>
              <div className="flex justify-between"><span>Status</span><Badge>{reg.status}</Badge></div>
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle className="text-base">Invoice & Payment</CardTitle></CardHeader>
            <CardContent className="space-y-2 text-sm">
              <div className="flex justify-between"><span>Program</span><span>{reg.programTitle}</span></div>
              <div className="flex justify-between"><span>Session</span><span className="font-mono">{reg.sessionCode}</span></div>
              <div className="flex justify-between"><span>Invoice</span><span className="font-mono">{reg.invoiceNumber || '—'}</span></div>
              <div className="flex justify-between"><span>Price</span><span>{formatCurrency(reg.price)}</span></div>
              <div className="flex justify-between"><span>Discount</span><span>{formatCurrency(reg.discountAmount)}</span></div>
              <div className="flex justify-between"><span>Tax</span><span>{formatCurrency(reg.taxAmount)}</span></div>
              <div className="flex justify-between font-bold"><span>Total</span><span>{formatCurrency(reg.totalAmount)}</span></div>
              <div className="flex justify-between"><span>Paid</span><span className="text-emerald-700">{formatCurrency(reg.paidAmount)}</span></div>
              <div className="flex justify-between"><span>Outstanding</span><span className="text-amber-700">{formatCurrency(reg.outstandingAmount)}</span></div>
              <Badge variant="outline">{reg.paymentStatus}</Badge>
              {(reg.payments || []).length > 0 && (
                <div className="pt-3 border-t space-y-1">
                  {reg.payments.map((p) => (
                    <div key={p.id} className="flex justify-between text-xs">
                      <span>{formatDate(p.paymentDate)} · {p.paymentMethod}</span>
                      <span className="font-mono">{formatCurrency(p.amount)}</span>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle className="text-base">Attendance & Certificate</CardTitle></CardHeader>
            <CardContent className="space-y-3 text-sm">
              {(reg.attendance || []).length === 0 ? (
                <p className="text-slate-500">No attendance records yet.</p>
              ) : (reg.attendance || []).map((a) => (
                <div key={a.id} className="flex justify-between border-b py-1">
                  <span>{formatDate(a.sessionDate)}</span>
                  <Badge variant="outline">{a.status}</Badge>
                </div>
              ))}
              <div className="pt-2 border-t">
                <div className="flex justify-between"><span>Completed</span><span>{reg.completed ? 'Yes' : 'No'}</span></div>
                {reg.attendancePercent != null && (
                  <div className="flex justify-between"><span>Attendance %</span><span>{reg.attendancePercent}%</span></div>
                )}
                {reg.certificate ? (
                  <div className="mt-2 space-y-1 text-xs">
                    <p className="font-mono font-bold">{reg.certificate.certificateNumber}</p>
                    <p>Verify: {reg.certificate.verificationCode}</p>
                    <Badge>{reg.certificate.status}</Badge>
                  </div>
                ) : (
                  <p className="text-xs text-slate-400 mt-2">No certificate issued.</p>
                )}
              </div>
            </CardContent>
          </Card>
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
                  {['CASH', 'BANK_TRANSFER', 'MOBILE_MONEY'].map((m) => <SelectItem key={m} value={m}>{m}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1"><Label>Date</Label><Input type="date" value={payForm.paymentDate} onChange={(e) => setPayForm({ ...payForm, paymentDate: e.target.value })} /></div>
            <DialogFooter><Button type="submit">Save Payment</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={attOpen} onOpenChange={setAttOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Record Attendance</DialogTitle></DialogHeader>
          <form onSubmit={submitAtt} className="space-y-3">
            <div className="space-y-1"><Label>Session date</Label><Input type="date" required value={attForm.sessionDate} onChange={(e) => setAttForm({ ...attForm, sessionDate: e.target.value })} /></div>
            <div className="space-y-1">
              <Label>Status</Label>
              <Select value={attForm.status} onValueChange={(v) => setAttForm({ ...attForm, status: v })}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  {['PRESENT', 'ABSENT', 'EXCUSED'].map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <DialogFooter><Button type="submit">Save</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default TrainingRegistrationDetail;
