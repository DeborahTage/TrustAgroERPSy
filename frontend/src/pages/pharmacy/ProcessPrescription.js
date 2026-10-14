import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { pharmacyApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { toast } from 'react-toastify';
import {
  ArrowLeft, CheckCircle2, AlertCircle, Stethoscope,
  Navigation, Calendar, Hash, Activity, ShieldCheck, Truck, XCircle
} from 'lucide-react';
import { format } from 'date-fns';

const PENDING = new Set(['PENDING_PHARMACY', 'PENDING', 'SUBMITTED']);

export default function ProcessPrescription() {
  const navigate = useNavigate();
  const location = useLocation();
  const initialRxId = new URLSearchParams(location.search).get('rxId');

  const [rx, setRx] = useState(null);
  const [loading, setLoading] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [step, setStep] = useState(1);
  const [rejectReason, setRejectReason] = useState('');
  const [showReject, setShowReject] = useState(false);
  const [batchId, setBatchId] = useState('');

  useEffect(() => {
    if (initialRxId) fetchRx(initialRxId);
  }, [initialRxId]);

  const fetchRx = async (id) => {
    setLoading(true);
    try {
      const res = await pharmacyApi.getPrescription(id);
      const found = res.data?.data || res.data;
      if (!found) {
        toast.error('Prescription not found');
        return;
      }
      setRx(found);
      if (found.status === 'APPROVED') setStep(2);
      else if (found.status === 'DISPENSED' || found.status === 'COMPLETED') setStep(3);
      else setStep(1);
      if (found.availableBatches?.length === 1) {
        setBatchId(String(found.availableBatches[0].id));
      }
    } catch {
      toast.error('Failed to load prescription details');
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async () => {
    if (!rx) return;
    setProcessing(true);
    try {
      const res = await pharmacyApi.approvePrescription(rx.id);
      const updated = res.data?.data || { ...rx, status: 'APPROVED' };
      toast.success('Prescription approved.');
      setRx(updated);
      setStep(2);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Approval failure');
    } finally {
      setProcessing(false);
    }
  };

  const handleReject = async () => {
    if (!rx) return;
    if (!rejectReason.trim()) {
      toast.error('Rejection reason is required');
      return;
    }
    setProcessing(true);
    try {
      await pharmacyApi.rejectPrescription(rx.id, rejectReason.trim());
      toast.success('Prescription rejected.');
      navigate('/pharmacy/overview');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Rejection failure');
    } finally {
      setProcessing(false);
    }
  };

  const handleDispense = async () => {
    if (!rx) return;
    setProcessing(true);
    try {
      const res = await pharmacyApi.dispensePrescription(rx.id, {
        batchId: batchId ? Number(batchId) : null,
      });
      const updated = res.data?.data || { ...rx, status: 'COMPLETED' };
      toast.success('Stock deducted and veterinary cost recorded.');
      setRx(updated);
      setStep(3);
      setTimeout(() => navigate('/pharmacy/overview'), 2000);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Dispensing failure');
    } finally {
      setProcessing(false);
    }
  };

  if (loading) {
    return (
      <EnterprisePage title="Dispensing Command" subtitle="Loading prescription..." breadcrumbs={['Pharmacy', 'Fulfillment']} loading />
    );
  }

  const code = rx?.prescriptionNumber || rx?.prescriptionCode;

  return (
    <EnterprisePage
      title="Dispensing Command"
      subtitle="Verify clinical directives and execute pharmaceutical issuance"
      breadcrumbs={['Pharmacy', 'Fulfillment', code || 'Order']}
      actions={
        <Button variant="outline" onClick={() => navigate(-1)}>
          <ArrowLeft className="mr-2 h-4 w-4" /> Back
        </Button>
      }
    >
      {!rx ? (
        <Card className="erp-card rounded-3xl h-[400px] flex flex-col items-center justify-center gap-6">
          <AlertCircle className="h-10 w-10 text-muted-foreground" />
          <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">Select a prescription from the queue.</p>
          <Button onClick={() => navigate('/pharmacy/overview')}>Return to Command Center</Button>
        </Card>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-8">
            <Card className="erp-card rounded-3xl overflow-hidden border border-border">
              <CardHeader className="bg-slate-900 text-white p-8 sm:p-10">
                <div className="flex gap-2 mb-3">
                  <Badge className="border-none font-black text-[9px] uppercase tracking-widest px-3 py-1 rounded-full bg-primary text-primary-foreground">
                    {String(rx.status || '').toUpperCase()}
                  </Badge>
                  {rx.priority === 'URGENT' || rx.priority === 'HIGH' ? (
                    <Badge className="bg-rose-500 text-white border-none font-black text-[9px] uppercase">{rx.priority}</Badge>
                  ) : null}
                </div>
                <CardTitle className="text-3xl sm:text-4xl font-black tracking-tight">{code}</CardTitle>
                <CardDescription className="text-emerald-400 font-bold text-xs uppercase tracking-widest flex items-center gap-2 mt-2">
                  <Calendar className="h-3 w-3" />
                  Authorized {format(new Date(rx.createdAt || Date.now()), 'PPP')}
                </CardDescription>
              </CardHeader>
              <CardContent className="p-8 sm:p-10 space-y-8">
                <div className="grid grid-cols-2 gap-8">
                  <div>
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground">Medication</Label>
                    <p className="text-2xl font-black text-foreground">{rx.drugName}</p>
                  </div>
                  <div>
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground">Quantity</Label>
                    <p className="text-2xl font-black text-primary">{rx.quantity} <span className="text-sm text-muted-foreground">{rx.unit || 'units'}</span></p>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-8">
                  <div className="flex items-center gap-4">
                    <Navigation className="h-5 w-5 text-blue-500" />
                    <div>
                      <Label className="text-[10px] font-black uppercase text-muted-foreground">Route</Label>
                      <p className="font-bold text-foreground text-sm">{rx.administrationRoute || '—'}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    <Stethoscope className="h-5 w-5 text-amber-500" />
                    <div>
                      <Label className="text-[10px] font-black uppercase text-muted-foreground">Frequency</Label>
                      <p className="font-bold text-foreground text-sm">{rx.frequency || '—'}</p>
                    </div>
                  </div>
                </div>
                <div className="p-6 bg-muted/40 rounded-2xl border border-border/50">
                  <Label className="text-[10px] font-black uppercase text-muted-foreground block mb-2">Dosage / instructions</Label>
                  <p className="text-foreground font-medium italic">{rx.dosageInstruction || '—'}</p>
                </div>
                {rx.availableStock != null && (
                  <p className="text-sm font-bold text-muted-foreground">Available stock: <span className="text-primary">{rx.availableStock}</span></p>
                )}
                {step === 2 && (
                  <div className="space-y-2">
                    <Label className="text-[10px] font-black uppercase text-muted-foreground">Batch / lot (FEFO)</Label>
                    <Select value={batchId} onValueChange={setBatchId}>
                      <SelectTrigger className="bg-background border-border text-foreground"><SelectValue placeholder="Auto FEFO if empty" /></SelectTrigger>
                      <SelectContent>
                        {(rx.availableBatches || []).map((b) => (
                          <SelectItem key={b.id} value={String(b.id)}>
                            {b.batchNumber} · rem {b.quantityRemaining} · exp {b.expiryDate || 'n/a'}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                )}
                {step === 3 && (
                  <Card className="border-none bg-emerald-600 text-white p-6 sm:p-8 rounded-2xl">
                    <div className="flex items-center gap-3 mb-2">
                      <CheckCircle2 className="h-6 w-6" />
                      <h3 className="text-xl font-black uppercase">Fulfillment Complete</h3>
                    </div>
                    <p className="text-sm text-emerald-100">
                      Lot {rx.dispensedLotNumber || '—'} · Cost ETB {rx.totalCostAtDispense ?? '—'}
                    </p>
                  </Card>
                )}
              </CardContent>
            </Card>
          </div>

          <div className="space-y-6">
            <Card className="erp-card rounded-3xl border border-border p-8 space-y-4">
              <div className="flex items-start gap-4">
                <Hash className="h-6 w-6 text-primary" />
                <div>
                  <p className="font-black text-foreground">{rx.farmName || 'Farm'}</p>
                  <p className="text-[10px] text-primary font-black uppercase">{rx.batchCode || rx.flockName || 'Flock'}</p>
                </div>
              </div>
              <div>
                <Label className="text-[10px] font-black uppercase text-muted-foreground">Veterinarian</Label>
                <p className="text-sm font-black text-foreground">{rx.createdByVet || rx.prescribedBy || '—'}</p>
              </div>
            </Card>

            {step === 1 && PENDING.has(rx.status) && (
              <>
                <Button className="w-full h-14 rounded-2xl bg-primary text-primary-foreground font-black shadow-md" onClick={handleApprove} disabled={processing}>
                  {processing ? <Activity className="animate-spin h-5 w-5" /> : <ShieldCheck className="mr-2 h-5 w-5" />}
                  Approve
                </Button>
                {!showReject ? (
                  <Button variant="outline" className="w-full h-12 rounded-2xl border-rose-300 dark:border-rose-900/60 text-rose-600 dark:text-rose-400" onClick={() => setShowReject(true)}>
                    <XCircle className="mr-2 h-4 w-4" /> Reject
                  </Button>
                ) : (
                  <div className="space-y-3">
                    <Textarea placeholder="Rejection reason (required)" value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} className="bg-background border-border text-foreground" />
                    <Button className="w-full bg-rose-600 hover:bg-rose-700 text-white rounded-xl font-bold" onClick={handleReject} disabled={processing}>
                      Confirm Reject
                    </Button>
                  </div>
                )}
              </>
            )}

            {step === 2 && rx.status === 'APPROVED' && (
              <Button className="w-full h-14 rounded-2xl bg-primary text-primary-foreground font-black shadow-md" onClick={handleDispense} disabled={processing}>
                {processing ? <Activity className="animate-spin h-5 w-5" /> : <Truck className="mr-2 h-5 w-5" />}
                Dispense
              </Button>
            )}

            {step === 3 && (
              <Button className="w-full h-14 rounded-2xl bg-emerald-950/40 text-emerald-400 border border-emerald-800/40 font-bold" disabled>
                <CheckCircle2 className="mr-2 h-5 w-5" /> Completed
              </Button>
            )}
          </div>
        </div>
      )}
    </EnterprisePage>
  );
}
