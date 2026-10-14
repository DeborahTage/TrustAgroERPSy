import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { farmApi } from '../../api';
import EnterpriseFormPage from '../../components/layout/EnterpriseFormPage';
import { Card, CardContent } from '../../components/ui/card';
import { Input } from '../../components/ui/input';
import { Button } from '../../components/ui/button';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { ShieldCheck, Truck, UserCheck, AlertTriangle } from 'lucide-react';

const BiosecurityForm = () => {
    const navigate = useNavigate();
    const [farms, setFarms] = useState([]);
    const [loading, setLoading] = useState(false);
    const [form, setForm] = useState({
        farmId: '',
        footbathRefreshed: false,
        vehicularSpray: false,
        ppeWorn: false,
        visitorLogged: false,
        vehicleReg: '',
        visitorName: '',
        notes: '',
        staffSignature: ''
    });

    useEffect(() => {
        farmApi.getAll().then(res => setFarms(res.data.data || []));
    }, []);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!form.farmId || !form.staffSignature) {
            toast.error('Farm and Signature are mandatory.');
            return;
        }
        setLoading(true);
        try {
            await farmApi.logBiosecurity(form.farmId, form);
            toast.success('Biosecurity compliance log recorded.');
            navigate('/veterinary');
        } catch (err) {
            toast.error('Failed to log biosecurity event');
        } finally {
            setLoading(false);
        }
    };

    return (
        <EnterpriseFormPage
            title="Protocol Compliance Log"
            subtitle="Record sanitization standards, visitor access, and PPE enforcement"
            breadcrumbs={['Veterinary', 'Biosecurity']}
            backTo="/veterinary"
        >
            <form onSubmit={handleSubmit}>
                <Card className="erp-card rounded-3xl overflow-hidden border border-border">
                    <CardContent className="p-8 space-y-8">
                        <div className="space-y-2">
                            <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground">Target Farm Location *</Label>
                            <Select value={form.farmId} onValueChange={(val) => setForm({ ...form, farmId: val })}>
                                <SelectTrigger className="h-14 rounded-xl bg-background border border-border font-bold text-foreground focus:ring-emerald-600">
                                    <SelectValue placeholder="Select farm location" />
                                </SelectTrigger>
                                <SelectContent className="rounded-xl border border-border shadow-2xl">
                                    {farms.map(f => <SelectItem key={f.id} value={String(f.id)} className="font-semibold my-1">{f.farmName}</SelectItem>)}
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 p-6 bg-muted/40 rounded-2xl border border-border/60 text-sm">
                            <div className="flex items-center justify-between">
                                <span className="font-bold text-foreground flex items-center gap-2"><ShieldCheck className="h-5 w-5 text-sky-500" /> Footbath Refreshed?</span>
                                <Input type="checkbox" className="h-6 w-6 rounded-lg text-emerald-600" checked={form.footbathRefreshed} onChange={e => setForm({ ...form, footbathRefreshed: e.target.checked })} />
                            </div>
                            <div className="flex items-center justify-between">
                                <span className="font-bold text-foreground flex items-center gap-2"><Truck className="h-5 w-5 text-primary" /> Vehicular Spray Used?</span>
                                <Input type="checkbox" className="h-6 w-6 rounded-lg text-emerald-600" checked={form.vehicularSpray} onChange={e => setForm({ ...form, vehicularSpray: e.target.checked })} />
                            </div>
                            <div className="flex items-center justify-between">
                                <span className="font-bold text-foreground flex items-center gap-2"><UserCheck className="h-5 w-5 text-emerald-500" /> PPE Enforced?</span>
                                <Input type="checkbox" className="h-6 w-6 rounded-lg text-emerald-600" checked={form.ppeWorn} onChange={e => setForm({ ...form, ppeWorn: e.target.checked })} />
                            </div>
                            <div className="flex items-center justify-between">
                                <span className="font-bold text-foreground flex items-center gap-2"><AlertTriangle className="h-5 w-5 text-rose-500" /> Visitor Register Signed?</span>
                                <Input type="checkbox" className="h-6 w-6 rounded-lg text-emerald-600" checked={form.visitorLogged} onChange={e => setForm({ ...form, visitorLogged: e.target.checked })} />
                            </div>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div className="space-y-2">
                                <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground">Visitor Link (if applicable)</Label>
                                <Input
                                    className="h-14 rounded-xl bg-background border border-border font-bold text-foreground focus:border-emerald-600 focus:ring-emerald-600"
                                    placeholder="Visitor Name"
                                    value={form.visitorName}
                                    onChange={(e) => setForm({ ...form, visitorName: e.target.value })}
                                />
                            </div>
                            <div className="space-y-2">
                                <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground">Vehicle Logs (if applicable)</Label>
                                <Input
                                    className="h-14 rounded-xl bg-background border border-border font-bold text-foreground focus:border-emerald-600 focus:ring-emerald-600 uppercase"
                                    placeholder="E.g., CA-12345"
                                    value={form.vehicleReg}
                                    onChange={(e) => setForm({ ...form, vehicleReg: e.target.value })}
                                />
                            </div>
                        </div>

                        <div className="space-y-2">
                            <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground">Inspector Notes</Label>
                            <Textarea
                                className="rounded-xl border border-border font-bold text-foreground bg-background focus:border-emerald-600 focus:ring-emerald-600 min-h-[100px]"
                                placeholder="Mention any breaches or anomalies observed..."
                                value={form.notes}
                                onChange={(e) => setForm({ ...form, notes: e.target.value })}
                            />
                        </div>

                        <div className="space-y-2 pt-4 border-t border-border/60">
                            <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground">Oversight Signature *</Label>
                            <Input
                                className="h-14 rounded-xl bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-800/40 font-mono text-emerald-900 dark:text-emerald-400 tracking-wider font-extrabold focus:ring-emerald-600"
                                placeholder="TYPE DIGITAL SIGNATURE HERE"
                                value={form.staffSignature}
                                onChange={(e) => setForm({ ...form, staffSignature: e.target.value })}
                                required
                            />
                        </div>

                    </CardContent>
                </Card>

                <div className="mt-8 flex justify-end gap-4">
                    <Button type="button" variant="ghost" className="h-14 px-8 rounded-xl font-bold tracking-wider text-muted-foreground hover:text-foreground uppercase" onClick={() => navigate(-1)}>Cancel</Button>
                    <Button type="submit" className="h-14 px-10 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white shadow-md font-bold tracking-wider uppercase" disabled={loading}>
                        {loading ? 'Submitting...' : 'Sign & Complete Audit'}
                    </Button>
                </div>
            </form>
        </EnterpriseFormPage>
    );
};

export default BiosecurityForm;
