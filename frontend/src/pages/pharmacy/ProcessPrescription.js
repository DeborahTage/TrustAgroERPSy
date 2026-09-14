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
        <Card className="border-none shadow-2xl shadow-gray-200/50 rounded-[2.5rem] bg-white h-[400px] flex flex-col items-center justify-center gap-6">
          <AlertCircle className="h-10 w-10 text-gray-300" />
          <p className="text-xs font-bold text-gray-400 uppercase tracking-widest">Select a prescription from the queue.</p>
          <Button onClick={() => navigate('/pharmacy/overview')}>Return to Command Center</Button>
        </Card>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-8">
            <Card className="border-none shadow-2xl shadow-gray-200/40 rounded-[2.5rem] overflow-hidden bg-white">
              <CardHeader className="bg-[#0f172a] text-white p-10">
                <div className="flex gap-2 mb-3">
                  <Badge className="border-none font-black text-[9px] uppercase tracking-widest px-3 py-1 rounded-full bg-primary">
                    {String(rx.status || '').toUpperCase()}
                  </Badge>
                  {rx.priority === 'URGENT' || rx.priority === 'HIGH' ? (
                    <Badge className="bg-rose-500 border-none font-black text-[9px] uppercase">{rx.priority}</Badge>
                  ) : null}
                </div>
                <CardTitle className="text-4xl font-black tracking-tighter">{code}</CardTitle>
                <CardDescription className="text-emerald-300 font-bold text-xs uppercase tracking-widest flex items-center gap-2 mt-2">
                  <Calendar className="h-3 w-3" />
                  Authorized {format(new Date(rx.createdAt || Date.now()), 'PPP')}
                </CardDescription>
              </CardHeader>
              <CardContent className="p-10 space-y-8">
                <div className="grid grid-cols-2 gap-8">
                  <div>
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400">Medication</Label>
                    <p className="text-2xl font-black text-gray-900">{rx.drugName}</p>
                  </div>
                  <div>
                    <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400">Quantity</Label>
                    <p className="text-2xl font-black text-primary">{rx.quantity} <span className="text-sm text-gray-400">{rx.unit || 'units'}</span></p>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-8">
                  <div className="flex items-center gap-4">
                    <Navigation className="h-5 w-5 text-blue-600" />
                    <div>
                      <Label className="text-[10px] font-black uppercase text-gray-300">Route</Label>
                      <p className="font-black text-gray-700 text-sm">{rx.administrationRoute || '—'}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    <Stethoscope className="h-5 w-5 text-amber-600" />
                    <div>
                      <Label className="text-[10px] font-black uppercase text-gray-300">Frequency</Label>
                      <p className="font-black text-gray-700 text-sm">{rx.frequency || '—'}</p>
                    </div>
                  </div>
                </div>
                <div className="p-6 bg-gray-50 rounded-[2rem]">
                  <Label className="text-[10px] font-black uppercase text-gray-400 block mb-2">Dosage / instructions</Label>
                  <p className="text-gray-700 font-medium italic">{rx.dosageInstruction || '—'}</p>
                </div>
                {rx.availableStock != null && (
                  <p className="text-sm font-bold text-gray-600">Available stock: <span className="text-primary">{rx.availableStock}</span></p>
                )}
                {step === 2 && (
                  <div className="space-y-2">
                    <Label className="text-[10px] font-black uppercase text-gray-400">Batch / lot (FEFO)</Label>
                    <Select value={batchId} onValueChange={setBatchId}>
                      <SelectTrigger><SelectValue placeholder="Auto FEFO if empty" /></SelectTrigger>
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
                  <Card className="border-none bg-emerald-600 text-white p-8 rounded-[2rem]">
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
            <Card className="border-none shadow-xl rounded-[2.5rem] bg-white p-8 space-y-4">
              <div className="flex items-start gap-4">
                <Hash className="h-6 w-6 text-primary" />
                <div>
                  <p className="font-black text-gray-900">{rx.farmName || 'Farm'}</p>
                  <p className="text-[10px] text-primary font-black uppercase">{rx.batchCode || rx.flockName || 'Flock'}</p>
                </div>
              </div>
              <div>
                <Label className="text-[10px] font-black uppercase text-gray-400">Veterinarian</Label>
                <p className="text-sm font-black text-gray-700">{rx.createdByVet || rx.prescribedBy || '—'}</p>
              </div>
            </Card>

            {step === 1 && PENDING.has(rx.status) && (
              <>
                <Button className="w-full h-16 rounded-[2rem] bg-[#0f172a] text-white font-black" onClick={handleApprove} disabled={processing}>
                  {processing ? <Activity className="animate-spin h-6 w-6" /> : <ShieldCheck className="mr-2 h-6 w-6" />}
                  Approve
                </Button>
                {!showReject ? (
                  <Button variant="outline" className="w-full h-12 rounded-2xl border-rose-200 text-rose-600" onClick={() => setShowReject(true)}>
                    <XCircle className="mr-2 h-4 w-4" /> Reject
                  </Button>
                ) : (
                  <div className="space-y-3">
                    <Textarea placeholder="Rejection reason (required)" value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} />
                    <Button className="w-full bg-rose-600 hover:bg-rose-700 text-white" onClick={handleReject} disabled={processing}>
                      Confirm Reject
                    </Button>
                  </div>
                )}
              </>
            )}

            {step === 2 && rx.status === 'APPROVED' && (
              <Button className="w-full h-16 rounded-[2rem] bg-primary text-white font-black" onClick={handleDispense} disabled={processing}>
                {processing ? <Activity className="animate-spin h-6 w-6" /> : <Truck className="mr-2 h-6 w-6" />}
                Dispense
              </Button>
            )}

            {step === 3 && (
              <Button className="w-full h-16 rounded-[2rem] bg-emerald-50 text-emerald-600 border-2 border-emerald-100" disabled>
                <CheckCircle2 className="mr-2 h-6 w-6" /> Completed
              </Button>
            )}
          </div>
        </div>
      )}
    </EnterprisePage>
  );
}
