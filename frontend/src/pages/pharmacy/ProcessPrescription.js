import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { pharmacyApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Label } from '../../components/ui/label';
import { Input } from '../../components/ui/input';
import { toast } from 'react-toastify';
import {
    Pill,
    ArrowLeft,
    CheckCircle2,
    AlertCircle,
    Stethoscope,
    ClipboardCheck,
    Navigation,
    Calendar,
    Hash,
    Activity,
    ShieldCheck,
    Truck,
    Receipt,
    FlaskConical
} from 'lucide-react';
import { cn } from '../../lib/utils';
import { format } from 'date-fns';

export default function ProcessPrescription() {
    const navigate = useNavigate();
    const location = useLocation();
    const queryParams = new URLSearchParams(location.search);
    const initialRxId = queryParams.get('rxId');

    const [rx, setRx] = useState(null);
    const [loading, setLoading] = useState(false);
    const [processing, setProcessing] = useState(false);
    const [step, setStep] = useState(1); // 1: Review, 2: Approve, 3: Dispense

    useEffect(() => {
        if (initialRxId) {
            fetchRx(initialRxId);
        }
    }, [initialRxId]);

    const fetchRx = async (id) => {
        setLoading(true);
        try {
            // Note: In a real system we'd have getPrescriptionById, 
            // for now we'll find it in the pending list
            const res = await pharmacyApi.getPrescriptions();
            const found = (res.data.data || []).find(r => String(r.id) === String(id));
            if (found) {
                setRx(found);
                if (found.status === 'APPROVED') setStep(2);
                if (found.status === 'DISPENSED' || found.status === 'COMPLETED') setStep(3);
            } else {
                toast.error("Prescription not found or already processed");
            }
        } catch (err) {
            toast.error("Failed to load prescription details");
        } finally {
            setLoading(false);
        }
    };

    const handleApprove = async () => {
        if (!rx) return;
        setProcessing(true);
        try {
            await pharmacyApi.approvePrescription(rx.id);
            toast.success("Prescription instructions verified and approved.");
            setStep(2);
            setRx({ ...rx, status: 'APPROVED' });
        } catch (error) {
            toast.error(error.response?.data?.message || "Approval failure");
        } finally {
            setProcessing(false);
        }
    };

    const handleDispense = async () => {
        if (!rx) return;
        setProcessing(true);
        try {
            await pharmacyApi.dispensePrescription(rx.id);
            toast.success("Medical stock successfully issued and ledgered.");
            setStep(3);
            setRx({ ...rx, status: 'DISPENSED' });
            setTimeout(() => navigate('/pharmacy/overview'), 2000);
        } catch (error) {
            toast.error(error.response?.data?.message || "Dispensing failure");
        } finally {
            setProcessing(false);
        }
    };

    if (loading) {
        return (
            <EnterprisePage title="Dispensing Command" subtitle="Loading prescription..." breadcrumbs={['Pharmacy', 'Fulfillment']} loading />
        );
    }

    return (
        <EnterprisePage
            title="Dispensing Command"
            subtitle="Verify clinical directives and execute pharmaceutical issuance"
            breadcrumbs={['Pharmacy', 'Fulfillment', rx?.prescriptionCode || rx?.prescriptionNumber || 'Order']}
            actions={
                <Button variant="outline" onClick={() => navigate(-1)}>
                    <ArrowLeft className="mr-2 h-4 w-4" /> Back
                </Button>
            }
        >
            {!rx ? (
                <Card className="border-none shadow-2xl shadow-gray-200/50 rounded-[2.5rem] bg-white h-[400px] flex flex-col items-center justify-center gap-6">
                    <div className="h-20 w-20 bg-gray-50 rounded-3xl flex items-center justify-center">
                        <AlertCircle className="h-10 w-10 text-gray-300" />
                    </div>
                    <div className="text-center space-y-2">
                        <h3 className="text-xl font-black text-gray-900">No Active Order</h3>
                        <p className="text-xs font-bold text-gray-400 uppercase tracking-widest">Select a prescription from the dashboard to begin processing.</p>
                    </div>
                    <Button onClick={() => navigate('/pharmacy/overview')} className="rounded-2xl bg-[#0f172a] h-12 px-8 font-black uppercase tracking-tighter text-[11px]">
                        Return to Command Center
                    </Button>
                </Card>
            ) : (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    <div className="lg:col-span-2 space-y-8">
                        {/* Fulfillment Progress */}
                        <div className="bg-white p-2 rounded-[2rem] shadow-sm border border-gray-100 flex items-center justify-between px-6 h-20">
                            {[
                                { s: 1, label: 'Review', icon: ClipboardCheck },
                                { s: 2, label: 'Approval', icon: ShieldCheck },
                                { s: 3, label: 'Dispense', icon: Truck }
                            ].map((item, i) => (
                                <React.Fragment key={item.s}>
                                    <div className={cn(
                                        "flex items-center gap-3 transition-all duration-500",
                                        step >= item.s ? "text-[#0f172a]" : "text-gray-300"
                                    )}>
                                        <div className={cn(
                                            "h-10 w-10 rounded-xl flex items-center justify-center transition-all",
                                            step >= item.s ? "bg-emerald-900 text-white shadow-lg shadow-primary/10" : "bg-gray-100"
                                        )}>
                                            <item.icon className="h-5 w-5" />
                                        </div>
                                        <span className="text-[10px] font-black uppercase tracking-widest">{item.label}</span>
                                    </div>
                                    {i < 2 && <div className={cn("h-px flex-1 mx-4 max-w-[60px]", step > item.s ? "bg-emerald-900" : "bg-gray-100")} />}
                                </React.Fragment>
                            ))}
                        </div>

                        <Card className="border-none shadow-2xl shadow-gray-200/40 rounded-[2.5rem] overflow-hidden bg-white">
                            <CardHeader className="bg-[#0f172a] text-white p-10 relative">
                                <div className="absolute top-0 right-0 p-10 opacity-10">
                                    <FlaskConical className="h-32 w-32" />
                                </div>
                                <div className="flex justify-between items-start gap-4 relative z-10">
                                    <div className="space-y-3">
                                        <div className="flex gap-2">
                                            <Badge className={cn(
                                                "border-none font-black text-[9px] uppercase tracking-widest px-3 py-1 rounded-full",
                                                step === 3 ? "bg-emerald-500" : "bg-primary/100"
                                            )}>
                                                {rx.status.toUpperCase()}
                                            </Badge>
                                            {rx.vetPrescription?.priority === 'EMERGENCY' && (
                                                <Badge className="bg-rose-500 border-none font-black text-[9px] uppercase tracking-widest px-3 py-1 rounded-full animate-pulse">EMERGENCY</Badge>
                                            )}
                                        </div>
                                        <CardTitle className="text-4xl font-black tracking-tighter">{rx.prescriptionCode}</CardTitle>
                                        <CardDescription className="text-emerald-300 font-bold text-xs uppercase tracking-widest flex items-center gap-2">
                                            <Calendar className="h-3 w-3" />
                                            Authorized {format(new Date(rx.createdAt || Date.now()), 'PPP')}
                                        </CardDescription>
                                    </div>
                                    <div className="h-16 w-16 bg-white/10 rounded-2xl flex items-center justify-center backdrop-blur-md">
                                        <Pill className="h-8 w-8 text-emerald-300" />
                                    </div>
                                </div>
                            </CardHeader>
                            <CardContent className="p-10 space-y-10">
                                <div className="grid grid-cols-2 gap-10">
                                    <div className="space-y-2">
                                        <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400">Biological Agent</Label>
                                        <p className="text-2xl font-black text-gray-900 tracking-tight">{rx.drugName || rx.vetPrescription?.drugName}</p>
                                    </div>
                                    <div className="space-y-2">
                                        <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400">Dose Quantity</Label>
                                        <p className="text-2xl font-black text-primary tracking-tight">
                                            {rx.quantity || rx.vetPrescription?.quantity}
                                            <span className="text-sm font-bold text-gray-400 uppercase tracking-tighter ml-2">Units</span>
                                        </p>
                                    </div>
                                </div>

                                <div className="grid grid-cols-2 gap-10">
                                    <div className="flex items-center gap-4 group">
                                        <div className="h-12 w-12 bg-blue-50 rounded-2xl flex items-center justify-center group-hover:scale-110 transition-transform">
                                            <Navigation className="h-5 w-5 text-blue-600" />
                                        </div>
                                        <div>
                                            <Label className="text-[10px] font-black uppercase tracking-widest text-gray-300">Route</Label>
                                            <p className="font-black text-gray-700 text-sm">{rx.administrationRoute || rx.vetPrescription?.administrationRoute}</p>
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-4 group">
                                        <div className="h-12 w-12 bg-amber-50 rounded-2xl flex items-center justify-center group-hover:scale-110 transition-transform">
                                            <Stethoscope className="h-5 w-5 text-amber-600" />
                                        </div>
                                        <div>
                                            <Label className="text-[10px] font-black uppercase tracking-widest text-gray-300">Frequency</Label>
                                            <p className="font-black text-gray-700 text-sm">{rx.frequency || rx.vetPrescription?.frequency}</p>
                                        </div>
                                    </div>
                                </div>

                                <div className="p-8 bg-gray-50 rounded-[2rem] border border-gray-100 relative group overflow-hidden">
                                    <div className="absolute top-0 right-0 p-6 opacity-5 group-hover:scale-110 transition-transform">
                                        <ClipboardCheck className="h-20 w-20" />
                                    </div>
                                    <Label className="text-[10px] font-black uppercase tracking-widest text-gray-400 block mb-4">Clinical Peer Directives</Label>
                                    <p className="text-gray-700 font-medium leading-relaxed italic relative z-10">
                                        "{rx.diagnosis || rx.vetPrescription?.dosageInstruction || 'Standard pharmacological protocol requested.'}"
                                    </p>
                                </div>
                            </CardContent>
                        </Card>

                        {step === 3 && (
                            <Card className="border-none shadow-2xl shadow-emerald-100 rounded-[2.5rem] bg-emerald-600 text-white p-10 overflow-hidden relative">
                                <div className="absolute top-0 right-0 p-8 opacity-20">
                                    <Receipt className="h-32 w-32 rotate-12" />
                                </div>
                                <div className="space-y-4 relative z-10">
                                    <div className="flex items-center gap-3">
                                        <div className="h-10 w-10 bg-white/20 rounded-xl flex items-center justify-center">
                                            <CheckCircle2 className="h-6 w-6 text-white" />
                                        </div>
                                        <h3 className="text-xl font-black uppercase tracking-tighter">Fulfillment Complete</h3>
                                    </div>
                                    <p className="text-sm font-bold text-emerald-100 leading-relaxed max-w-md">
                                        Clinical instructions have been ledgered. Stock levels updated and COGS expense mapped to target flock.
                                    </p>
                                </div>
                            </Card>
                        )}
                    </div>

                    <div className="space-y-8">
                        <Card className="border-none shadow-2xl shadow-gray-200/40 rounded-[2.5rem] overflow-hidden bg-white">
                            <CardHeader className="pb-4 border-b border-gray-50">
                                <CardTitle className="text-xs font-black uppercase tracking-widest text-gray-400">Target Entity</CardTitle>
                            </CardHeader>
                            <CardContent className="p-8 space-y-8">
                                <div className="flex items-start gap-4">
                                    <div className="h-14 w-14 rounded-2xl bg-primary/10 flex items-center justify-center text-primary">
                                        <Hash className="h-7 w-7" />
                                    </div>
                                    <div>
                                        <p className="font-black text-gray-900 text-lg leading-tight">{rx.vetPrescription?.flockBatchCode || 'Internal Flock'}</p>
                                        <p className="text-[10px] text-primary font-black uppercase tracking-widest mt-1">Farm Hub ID: {rx.vetPrescription?.farmId}</p>
                                    </div>
                                </div>
                                <div className="space-y-6 pt-4">
                                    <div className="space-y-1">
                                        <Label className="text-[10px] font-black uppercase tracking-widest text-gray-400">Clinical Provider</Label>
                                        <p className="text-sm font-black text-gray-700">Medical Authority {rx.vetPrescription?.createdByVetId || 'System'}</p>
                                    </div>
                                    <div className="space-y-1">
                                        <Label className="text-[10px] font-black uppercase tracking-widest text-gray-400">Protocol Period</Label>
                                        <div className="flex items-center gap-2 text-sm font-black text-gray-700">
                                            <Calendar className="h-4 w-4 text-primary" />
                                            {format(new Date(rx.createdAt || Date.now()), 'dd MMM yyyy')}
                                        </div>
                                    </div>
                                </div>
                            </CardContent>
                        </Card>

                        {step === 1 ? (
                            <Button
                                className="w-full h-20 rounded-[2rem] bg-[#0f172a] hover:bg-[#1e293b] text-white font-black text-lg shadow-2xl shadow-gray-200 transition-all active:scale-95 group"
                                onClick={handleApprove}
                                disabled={processing}
                            >
                                {processing ? <Activity className="animate-spin h-6 w-6" /> : <ShieldCheck className="mr-3 h-7 w-7 group-hover:scale-110 transition-transform" />}
                                {processing ? 'Verifying...' : 'Verify Order'}
                            </Button>
                        ) : step === 2 ? (
                            <Button
                                className="w-full h-20 rounded-[2rem] bg-primary hover:bg-primary/90 text-white font-black text-lg shadow-2xl shadow-primary/10 transition-all active:scale-95 group"
                                onClick={handleDispense}
                                disabled={processing}
                            >
                                {processing ? <Activity className="animate-spin h-6 w-6" /> : <CheckCircle2 className="mr-3 h-7 w-7 group-hover:scale-110 transition-transform" />}
                                {processing ? 'Executing...' : 'Dispatch Product'}
                            </Button>
                        ) : (
                            <Button
                                className="w-full h-20 rounded-[2rem] bg-emerald-50 text-emerald-600 font-black text-lg border-2 border-emerald-100 cursor-default"
                                disabled
                            >
                                <CheckCircle2 className="mr-3 h-6 w-6" />
                                Order Ledgered
                            </Button>
                        )}

                        <div className="bg-primary/10/50 p-6 rounded-3xl border border-primary/20/50">
                            <p className="text-[9px] text-center text-primary/60 font-black uppercase tracking-widest leading-relaxed">
                                PHARMACEUTICAL CONTROL SYSTEM ACTIVE. DISPENSING TRIGGERS AUTOMATIC STOCK DEDUCTION AND FINANCIAL RECOGNITION.
                            </p>
                        </div>
                    </div>
                </div>
            )}
        </EnterprisePage>
    );
}
