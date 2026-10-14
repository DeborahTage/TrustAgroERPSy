import React, { useCallback, useEffect, useState } from 'react';
import {
    Droplet,
    ShieldAlert, Plus,
    History, User, Zap, AlertTriangle, CheckCircle2,
    Stethoscope, Microscope, Beaker
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/ui/tabs';
import { Progress } from '../../components/ui/progress';
import { vetApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { useParams } from 'react-router-dom';
import { formatDate } from '../../utils';
import { toast } from 'react-toastify';

const FlockEMR = () => {
    const { id } = useParams();
    const [emr, setEmr] = useState(null);
    const [loading, setLoading] = useState(true);

    const [error, setError] = useState(null);

    const fetchEMR = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await vetApi.getFlockEMR(id);
            setEmr(res.data);
        } catch {
            setError('Failed to load flock medical records.');
            toast.error('Failed to load Flock EMR');
        } finally {
            setLoading(false);
        }
    }, [id]);

    useEffect(() => {
        fetchEMR();
    }, [fetchEMR]);

    return (
        <EnterprisePage
            title={emr ? `Flock EMR — ${emr.batchCode}` : 'Flock EMR'}
            subtitle={emr ? `${emr.breed || 'Flock'} · ${emr.ageDays ?? '—'} days · ${emr.currentBirdCount ?? '—'} Chickens` : 'Electronic medical record'}
            breadcrumbs={['Veterinary', 'EMR', emr?.batchCode || id]}
            loading={loading}
            error={error || (!loading && !emr ? 'No medical records found for this flock.' : null)}
            onRetry={fetchEMR}
        >
        {emr && (
        <div className="space-y-6">
            <div className="flex flex-wrap gap-3 items-center justify-between">
                    {emr.isUnderWithdrawal ? (
                        <div className="bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800/40 p-3 rounded-xl flex items-center gap-3">
                            <ShieldAlert className="h-6 w-6 text-rose-600 dark:text-rose-400" />
                            <div>
                                <div className="text-xs font-bold text-rose-800 dark:text-rose-300 uppercase tracking-wider">Biological Lock Active</div>
                                <div className="text-sm font-black text-rose-600 dark:text-rose-400">{formatDate(emr.withdrawalEndDate)}</div>
                            </div>
                        </div>
                    ) : (
                        <Badge className="bg-emerald-100 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-400 h-fit py-2 px-4 border border-emerald-200 dark:border-emerald-800/40 rounded-xl">
                            <CheckCircle2 className="w-4 h-4 mr-2" /> Market Ready
                        </Badge>
                    )}
                    <Button className="bg-primary text-primary-foreground font-bold shadow-md rounded-xl">
                        <Plus className="w-4 h-4 mr-2" /> New Entry
                    </Button>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">

                {/* SIDEBAR KPI */}
                <div className="lg:col-span-1 space-y-6">
                    <Card className="erp-card rounded-2xl border border-border">
                        <CardHeader className="pb-2">
                            <CardTitle className="text-xs font-bold uppercase tracking-widest text-muted-foreground">Health Index</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            <div className="space-y-1">
                                <div className="flex justify-between text-sm">
                                    <span className="text-muted-foreground">Mortality Rate</span>
                                    <span className={emr.mortalityRate > 5 ? "text-rose-600 dark:text-rose-400 font-bold" : "text-foreground font-bold"}>
                                        {emr.mortalityRate?.toFixed(2)}%
                                    </span>
                                </div>
                                <Progress value={emr.mortalityRate} className="h-1.5 bg-muted" />
                            </div>
                            <div className="pt-2">
                                <div className="text-xs text-muted-foreground mb-2">Total Mortality</div>
                                <div className="text-2xl font-black text-foreground">{emr.totalMortality} Chickens</div>
                            </div>
                        </CardContent>
                    </Card>

                    <Card className="erp-card rounded-2xl border border-border">
                        <CardHeader className="pb-2">
                            <CardTitle className="text-xs font-bold uppercase tracking-widest text-muted-foreground">Biological Parameters</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-3">
                            <div className="flex justify-between items-center text-sm">
                                <span className="flex items-center gap-2 text-muted-foreground"><AlertTriangle className="h-4 w-4 text-amber-500" /> Resp. Score</span>
                                <Badge variant="outline" className="border-border text-foreground">Normal</Badge>
                            </div>
                            <div className="flex justify-between items-center text-sm">
                                <span className="flex items-center gap-2 text-muted-foreground"><Droplet className="h-4 w-4 text-blue-500" /> Diarrhea</span>
                                <Badge variant="outline" className="border-border text-foreground">None</Badge>
                            </div>
                        </CardContent>
                    </Card>
                </div>

                {/* MAIN EMR CONTENT */}
                <div className="lg:col-span-3">
                    <Tabs defaultValue="timeline" className="w-full">
                        <TabsList className="bg-card p-1 border border-border rounded-xl shadow-sm mb-6 w-full justify-start overflow-x-auto h-12">
                            <TabsTrigger value="timeline" className="px-6 data-[state=active]:bg-primary data-[state=active]:text-primary-foreground rounded-lg flex items-center gap-2 font-semibold">
                                <History className="h-4 w-4" /> Medical Timeline
                            </TabsTrigger>
                            <TabsTrigger value="treatments" className="px-6 data-[state=active]:bg-primary data-[state=active]:text-primary-foreground rounded-lg flex items-center gap-2 font-semibold">
                                <Beaker className="h-4 w-4" /> Treatments
                            </TabsTrigger>
                            <TabsTrigger value="vaccinations" className="px-6 data-[state=active]:bg-primary data-[state=active]:text-primary-foreground rounded-lg flex items-center gap-2 font-semibold">
                                <Zap className="h-4 w-4" /> Vaccinations
                            </TabsTrigger>
                            <TabsTrigger value="necropsies" className="px-6 data-[state=active]:bg-primary data-[state=active]:text-primary-foreground rounded-lg flex items-center gap-2 font-semibold">
                                <Microscope className="h-4 w-4" /> Necropsies
                            </TabsTrigger>
                        </TabsList>

                        <TabsContent value="timeline" className="space-y-0 relative mt-0">
                            <div className="absolute left-[19px] top-0 bottom-0 w-0.5 bg-border"></div>
                            <div className="space-y-8">
                                {[...(emr.diseaseHistory || []), ...(emr.treatments || []), ...(emr.vaccinations || [])].sort((a, b) => new Date(b.createdAt || b.startDate || 0) - new Date(a.createdAt || a.startDate || 0)).map((item, idx) => (
                                    <div key={idx} className="relative pl-12 group">
                                        <div className="absolute left-0 bg-card border-2 border-primary rounded-full h-10 w-10 flex items-center justify-center z-10 group-hover:scale-110 transition-transform">
                                            {item.vaccineName ? <Zap className="h-5 w-5 text-primary" /> :
                                                item.drugName ? <Beaker className="h-5 w-5 text-primary" /> :
                                                    <AlertTriangle className="h-5 w-5 text-rose-600 dark:text-rose-400" />}
                                        </div>
                                        <Card className="erp-card rounded-2xl border border-border hover:border-primary/40 transition-colors">
                                            <CardHeader className="p-4 py-3">
                                                <div className="flex justify-between items-start">
                                                    <div>
                                                        <CardTitle className="text-base font-bold text-foreground">
                                                            {item.vaccineName || item.drugName || item.suspectedDisease}
                                                        </CardTitle>
                                                        <CardDescription className="text-[10px] uppercase font-bold text-muted-foreground mt-0.5">
                                                            {item.vaccineName ? 'Vaccination' : item.drugName ? 'Treatment' : 'Disease Case Detected'}
                                                        </CardDescription>
                                                    </div>
                                                    <div className="text-[10px] text-muted-foreground font-mono">{formatDate(item.createdAt)}</div>
                                                </div>
                                            </CardHeader>
                                            <CardContent className="p-4 pt-0 text-sm text-muted-foreground">
                                                {item.dosage && <p>Dosage: <b className="text-foreground">{item.dosage}</b> via {item.route}</p>}
                                                {item.symptoms && <p className="italic bg-muted/40 text-foreground p-3 rounded-xl mt-2 border border-border">"{item.symptoms}"</p>}
                                                <div className="mt-3 flex items-center gap-2 text-xs text-muted-foreground">
                                                    <User className="h-3 w-3" /> Given by: {item.vetOfficer || item.reportedBy || 'System'}
                                                </div>
                                            </CardContent>
                                        </Card>
                                    </div>
                                ))}
                            </div>
                        </TabsContent>

                        <TabsContent value="treatments">
                            <Card className="erp-card rounded-2xl border border-border overflow-hidden">
                                <CardContent className="p-0">
                                    <table className="w-full text-sm">
                                        <thead className="erp-table-head border-b border-border">
                                            <tr className="text-left text-muted-foreground">
                                                <th className="p-4 font-semibold uppercase text-xs">Drug</th>
                                                <th className="p-4 font-semibold uppercase text-xs">Dosage / Route</th>
                                                <th className="p-4 font-semibold uppercase text-xs">Start Date</th>
                                                <th className="p-4 font-semibold uppercase text-xs">Days</th>
                                                <th className="p-4 font-semibold uppercase text-xs">Outcome</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-border">
                                            {emr.treatments?.map(t => (
                                                <tr key={t.id} className="hover:bg-muted/40 transition-colors">
                                                    <td className="p-4 font-bold text-foreground">{t.drugName}</td>
                                                    <td className="p-4 text-muted-foreground">{t.dosage} ({t.route})</td>
                                                    <td className="p-4 text-muted-foreground">{formatDate(t.startDate)}</td>
                                                    <td className="p-4 font-mono text-muted-foreground">{t.duration}d</td>
                                                    <td className="p-4"><Badge variant="outline" className="border-border text-foreground">{t.outcome || 'Active'}</Badge></td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </CardContent>
                            </Card>
                        </TabsContent>

                        <TabsContent value="necropsies">
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                {(emr.necropsies || []).map(n => (
                                    <Card key={n.id} className="erp-card rounded-2xl border border-border hover:border-rose-400/40 transition-all cursor-pointer">
                                        <CardHeader className="p-4 pb-2">
                                            <CardTitle className="text-foreground text-lg font-bold">{n.finalDiagnosis || 'Inconclusive'}</CardTitle>
                                            <CardDescription className="text-muted-foreground">Date: {formatDate(n.neopsyDate)}</CardDescription>
                                        </CardHeader>
                                        <CardContent className="p-4 text-sm text-muted-foreground">
                                            <div className="bg-rose-50/40 dark:bg-rose-950/30 p-3 rounded-xl border border-rose-200 dark:border-rose-800/40 text-rose-800 dark:text-rose-400 font-semibold mb-3">
                                                Cause: {n.suspectedCauseOfDeath}
                                            </div>
                                            <div className="flex items-center gap-2 text-xs text-muted-foreground">
                                                <Stethoscope className="h-3 w-3" /> Performed by: {n.performedBy}
                                            </div>
                                        </CardContent>
                                    </Card>
                                ))}
                                {(emr.necropsies || []).length === 0 && <div className="col-span-full py-10 text-center text-muted-foreground italic">No necropsy records filed.</div>}
                            </div>
                        </TabsContent>
                    </Tabs>
                </div>
            </div>
        </div>
        )}
        </EnterprisePage>
    );
};

export default FlockEMR;
