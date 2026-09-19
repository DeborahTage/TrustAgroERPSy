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
                        <div className="bg-rose-50 border border-rose-200 p-3 rounded-lg flex items-center gap-3">
                            <ShieldAlert className="h-6 w-6 text-rose-600" />
                            <div>
                                <div className="text-xs font-bold text-rose-800 uppercase tracking-tighter">Biological Lock Active</div>
                                <div className="text-sm font-black text-rose-600">{formatDate(emr.withdrawalEndDate)}</div>
                            </div>
                        </div>
                    ) : (
                        <Badge className="bg-emerald-100 text-emerald-700 h-fit py-2 px-4 border-emerald-200">
                            <CheckCircle2 className="w-4 h-4 mr-2" /> Market Ready
                        </Badge>
                    )}
                    <Button className="bg-primary shadow-primary/10 shadow-lg">
                        <Plus className="w-4 h-4 mr-2" /> New Entry
                    </Button>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">

                {/* SIDEBAR KPI */}
                <div className="lg:col-span-1 space-y-6">
                    <Card className="shadow-sm border-gray-100">
                        <CardHeader className="pb-2">
                            <CardTitle className="text-xs font-bold uppercase tracking-widest text-gray-400">Health Index</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            <div className="space-y-1">
                                <div className="flex justify-between text-sm">
                                    <span className="text-gray-500">Mortality Rate</span>
                                    <span className={emr.mortalityRate > 5 ? "text-rose-600 font-bold" : "text-gray-900 font-bold"}>
                                        {emr.mortalityRate?.toFixed(2)}%
                                    </span>
                                </div>
                                <Progress value={emr.mortalityRate} className="h-1.5 bg-gray-50" />
                            </div>
                            <div className="pt-2">
                                <div className="text-xs text-gray-500 mb-2">Total Mortality</div>
                                <div className="text-2xl font-black text-gray-900">{emr.totalMortality} Chickens</div>
                            </div>
                        </CardContent>
                    </Card>

                    <Card className="shadow-sm border-gray-100 bg-gray-50/20">
                        <CardHeader className="pb-2">
                            <CardTitle className="text-xs font-bold uppercase tracking-widest text-gray-400">Biological Parameters</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-3">
                            <div className="flex justify-between items-center text-sm">
                                <span className="flex items-center gap-2 text-gray-600"><AlertTriangle className="h-4 w-4 text-amber-500" /> Resp. Score</span>
                                <Badge variant="outline">Normal</Badge>
                            </div>
                            <div className="flex justify-between items-center text-sm">
                                <span className="flex items-center gap-2 text-gray-600"><Droplet className="h-4 w-4 text-blue-500" /> Diarrhea</span>
                                <Badge variant="outline">None</Badge>
                            </div>
                        </CardContent>
                    </Card>
                </div>

                {/* MAIN EMR CONTENT */}
                <div className="lg:col-span-3">
                    <Tabs defaultValue="timeline" className="w-full">
                        <TabsList className="bg-white p-1 border rounded-lg shadow-sm mb-6 w-full justify-start overflow-x-auto h-12">
                            <TabsTrigger value="timeline" className="px-6 data-[state=active]:bg-primary data-[state=active]:text-white rounded-md flex items-center gap-2">
                                <History className="h-4 w-4" /> Medical Timeline
                            </TabsTrigger>
                            <TabsTrigger value="treatments" className="px-6 data-[state=active]:bg-primary data-[state=active]:text-white rounded-md flex items-center gap-2">
                                <Beaker className="h-4 w-4" /> Treatments
                            </TabsTrigger>
                            <TabsTrigger value="vaccinations" className="px-6 data-[state=active]:bg-primary data-[state=active]:text-white rounded-md flex items-center gap-2">
                                <Zap className="h-4 w-4" /> Vaccinations
                            </TabsTrigger>
                            <TabsTrigger value="necropsies" className="px-6 data-[state=active]:bg-primary data-[state=active]:text-white rounded-md flex items-center gap-2">
                                <Microscope className="h-4 w-4" /> Necropsies
                            </TabsTrigger>
                        </TabsList>

                        <TabsContent value="timeline" className="space-y-0 relative mt-0">
                            <div className="absolute left-[19px] top-0 bottom-0 w-0.5 bg-gray-100"></div>
                            <div className="space-y-8">
                                {/* Aggregated Timeline Logic (Simplified for Demo) */}
                                {[...(emr.diseaseHistory || []), ...(emr.treatments || []), ...(emr.vaccinations || [])].sort((a, b) => new Date(b.createdAt || b.startDate || 0) - new Date(a.createdAt || a.startDate || 0)).map((item, idx) => (
                                    <div key={idx} className="relative pl-12 group">
                                        <div className="absolute left-0 bg-white border-2 border-primary rounded-full h-10 w-10 flex items-center justify-center z-10 group-hover:scale-110 transition-transform">
                                            {item.vaccineName ? <Zap className="h-5 w-5 text-primary" /> :
                                                item.drugName ? <Beaker className="h-5 w-5 text-primary" /> :
                                                    <AlertTriangle className="h-5 w-5 text-rose-600" />}
                                        </div>
                                        <Card className="shadow-none border-gray-100 hover:border-primary/20 transition-colors">
                                            <CardHeader className="p-4 py-3">
                                                <div className="flex justify-between items-start">
                                                    <div>
                                                        <CardTitle className="text-base text-gray-800">
                                                            {item.vaccineName || item.drugName || item.suspectedDisease}
                                                        </CardTitle>
                                                        <CardDescription className="text-[10px] uppercase font-bold text-gray-400 mt-0.5">
                                                            {item.vaccineName ? 'Vaccination' : item.drugName ? 'Treatment' : 'Disease Case Detected'}
                                                        </CardDescription>
                                                    </div>
                                                    <div className="text-[10px] text-gray-400 font-mono">{formatDate(item.createdAt)}</div>
                                                </div>
                                            </CardHeader>
                                            <CardContent className="p-4 pt-0 text-sm text-gray-600">
                                                {item.dosage && <p>Dosage: <b>{item.dosage}</b> via {item.route}</p>}
                                                {item.symptoms && <p className="italic bg-gray-50 p-2 rounded mt-2">"{item.symptoms}"</p>}
                                                <div className="mt-3 flex items-center gap-2 text-xs text-gray-400">
                                                    <User className="h-3 w-3" /> Given by: {item.vetOfficer || item.reportedBy || 'System'}
                                                </div>
                                            </CardContent>
                                        </Card>
                                    </div>
                                ))}
                            </div>
                        </TabsContent>

                        <TabsContent value="treatments">
                            <Card>
                                <CardContent className="p-0">
                                    <table className="w-full text-sm">
                                        <thead className="bg-gray-50/50 border-b">
                                            <tr className="text-left text-gray-500">
                                                <th className="p-4 font-medium">Drug</th>
                                                <th className="p-4 font-medium">Dosage / Route</th>
                                                <th className="p-4 font-medium">Start Date</th>
                                                <th className="p-4 font-medium">Days</th>
                                                <th className="p-4 font-medium">Outcome</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y">
                                            {emr.treatments?.map(t => (
                                                <tr key={t.id} className="hover:bg-gray-50/50 transition-colors">
                                                    <td className="p-4 font-bold text-gray-800">{t.drugName}</td>
                                                    <td className="p-4 text-gray-600">{t.dosage} ({t.route})</td>
                                                    <td className="p-4">{formatDate(t.startDate)}</td>
                                                    <td className="p-4 font-mono">{t.duration}d</td>
                                                    <td className="p-4"><Badge variant="outline">{t.outcome || 'Active'}</Badge></td>
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
                                    <Card key={n.id} className="hover:border-rose-200 transition-all cursor-pointer">
                                        <CardHeader className="p-4 pb-2">
                                            <CardTitle className="text-gray-800 text-lg">{n.finalDiagnosis || 'Inconclusive'}</CardTitle>
                                            <CardDescription>Date: {formatDate(n.neopsyDate)}</CardDescription>
                                        </CardHeader>
                                        <CardContent className="p-4 text-sm text-gray-600">
                                            <div className="bg-rose-50/50 p-3 rounded-lg border border-rose-100 text-rose-800 font-medium mb-3">
                                                Cause: {n.suspectedCauseOfDeath}
                                            </div>
                                            <div className="flex items-center gap-2 text-xs text-gray-400">
                                                <Stethoscope className="h-3 w-3" /> Performed by: {n.performedBy}
                                            </div>
                                        </CardContent>
                                    </Card>
                                ))}
                                {(emr.necropsies || []).length === 0 && <div className="col-span-full py-10 text-center text-gray-400 italic">No necropsy records filed.</div>}
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
