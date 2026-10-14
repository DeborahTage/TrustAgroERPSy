import React, { useEffect, useState } from 'react';
import { vetApi, farmApi, flockApi } from '../../api';
import { Card, CardContent } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { exportToPdf } from '../../lib/exportTable';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart as RechartsPie, Pie, Cell } from 'recharts';
import { Activity, ActivitySquare, Download } from 'lucide-react';
import { toast } from 'react-toastify';
import { formatCurrency } from '../../utils';

const COLORS = ['#4f46e5', '#ec4899', '#f59e0b', '#10b981', '#6366f1', '#14b8a6', '#8b5cf6'];

const DrugUsageReport = () => {
    const [reportData, setReportData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [farms, setFarms] = useState([]);
    const [flocks, setFlocks] = useState([]);

    const [filters, setFilters] = useState({
        startDate: '',
        endDate: '',
        farmId: '',
        flockId: ''
    });

    useEffect(() => {
        farmApi.getAll().then(res => setFarms(res.data.data || []));
        fetchReport();
        // eslint-disable-next-line
    }, []);

    const fetchReport = async () => {
        setLoading(true);
        try {
            const params = {};
            if (filters.startDate) params.startDate = filters.startDate;
            if (filters.endDate) params.endDate = filters.endDate;
            if (filters.flockId) params.flockId = filters.flockId;

            const res = await vetApi.getDrugUsageReport(params);
            setReportData(res.data.data);
        } catch (error) {
            toast.error("Failed to load veterinary report data");
        } finally {
            setLoading(false);
        }
    };

    const handleFarmChange = async (farmId) => {
        setFilters(prev => ({ ...prev, farmId, flockId: '' }));
        if (farmId && farmId !== 'all') {
            const res = await flockApi.getAll();
            setFlocks(res.data.data.filter(f => String(f.farmId) === String(farmId)) || []);
        } else {
            setFlocks([]);
        }
    };

    const handleExportPDF = () => {
        exportToPdf('Drug Usage Report', 'drug-usage-report');
    };

    const chartData = reportData ? Object.keys(reportData.costByDrug || {}).map(name => ({
        name,
        cost: reportData.costByDrug[name],
        quantity: reportData.quantityByDrug[name]
    })).sort((a, b) => b.cost - a.cost) : [];

    const stats = reportData ? [
        { title: 'Total Drug Spend', value: formatCurrency(reportData.totalCost), icon: Activity, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
        { title: 'Prescriptions Issued', value: reportData.totalPrescriptions || 0, icon: ActivitySquare, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    ] : [];

    return (
        <EnterprisePage
            title="Drug Usage Analytics"
            subtitle="Therapeutic consumption and financial analysis"
            breadcrumbs={['Veterinary', 'Reports']}
            loading={loading && !reportData}
            stats={stats}
            onRefresh={fetchReport}
            actions={
                <Button variant="outline" onClick={handleExportPDF}>
                    <Download className="mr-2 h-4 w-4" /> Export
                </Button>
            }
        >
            <Card className="erp-card rounded-2xl border border-border print:hidden">
                <CardContent className="p-6">
                    <div className="flex items-end gap-6 flex-wrap">
                        <div className="space-y-2 flex-col">
                            <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground">Date Range Start</Label>
                            <Input type="date" className="h-11 rounded-xl bg-background font-bold border border-border text-foreground" value={filters.startDate} onChange={e => setFilters(old => ({ ...old, startDate: e.target.value }))} />
                        </div>
                        <div className="space-y-2 flex-col">
                            <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground">Date Range End</Label>
                            <Input type="date" className="h-11 rounded-xl bg-background font-bold border border-border text-foreground" value={filters.endDate} onChange={e => setFilters(old => ({ ...old, endDate: e.target.value }))} />
                        </div>
                        <div className="space-y-2 flex-col">
                            <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground">Target Farm</Label>
                            <Select value={filters.farmId} onValueChange={handleFarmChange}>
                                <SelectTrigger className="h-11 rounded-xl bg-background font-bold border border-border text-foreground min-w-[150px]">
                                    <SelectValue placeholder="All Farms" />
                                </SelectTrigger>
                                <SelectContent className="rounded-2xl border border-border shadow-2xl">
                                    <SelectItem value="all">All Farms</SelectItem>
                                    {farms.map(f => <SelectItem key={f.id} value={String(f.id)}>{f.farmName}</SelectItem>)}
                                </SelectContent>
                            </Select>
                        </div>
                        <div className="space-y-2 flex-col">
                            <Label className="text-[10px] font-black uppercase tracking-widest text-muted-foreground">Batch / Flock</Label>
                            <Select value={filters.flockId} onValueChange={v => setFilters(old => ({ ...old, flockId: v }))} disabled={!filters.farmId || filters.farmId === 'all'}>
                                <SelectTrigger className="h-11 rounded-xl bg-background font-bold border border-border text-foreground min-w-[150px]">
                                    <SelectValue placeholder={!filters.farmId || filters.farmId === 'all' ? "Select a farm first" : "All Batches"} />
                                </SelectTrigger>
                                <SelectContent className="rounded-2xl border border-border shadow-2xl">
                                    <SelectItem value="all">All Batches</SelectItem>
                                    {flocks.map(f => <SelectItem key={f.id} value={String(f.id)}>{f.batchCode}</SelectItem>)}
                                </SelectContent>
                            </Select>
                        </div>
                        <Button className="h-11 px-8 rounded-xl bg-primary text-primary-foreground font-bold tracking-wider uppercase hover:opacity-90 transition-opacity" onClick={fetchReport} disabled={loading}>
                            {loading ? 'Crunching...' : 'Apply Filters'}
                        </Button>
                    </div>
                </CardContent>
            </Card>

            {reportData && (
            <div id="drug-usage-report" className="space-y-6">
            {/* Charts Area */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 px-1">
                <Card className="erp-card rounded-3xl border border-border overflow-hidden p-8">
                    <h3 className="text-sm font-black text-foreground uppercase tracking-widest mb-6">Cost Breakdown By Agent</h3>
                    <div className="h-80 w-full">
                        <ResponsiveContainer width="100%" height="100%">
                            <RechartsPie>
                                <Pie data={chartData} cx="50%" cy="50%" innerRadius={60} outerRadius={100} paddingAngle={5} dataKey="cost">
                                    {chartData.map((entry, index) => <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />)}
                                </Pie>
                                <Tooltip
                                    formatter={(value) => formatCurrency(value)}
                                    contentStyle={{
                                        backgroundColor: 'var(--card, #0f172a)',
                                        borderColor: 'var(--border, #334155)',
                                        borderRadius: '0.75rem',
                                        color: 'var(--foreground, #fff)'
                                    }}
                                />
                                <Legend />
                            </RechartsPie>
                        </ResponsiveContainer>
                    </div>
                </Card>
                <Card className="erp-card rounded-3xl border border-border overflow-hidden p-8">
                    <h3 className="text-sm font-black text-foreground uppercase tracking-widest mb-6">Volume Consumption (Qty)</h3>
                    <div className="h-80 w-full">
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 20 }}>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="currentColor" className="text-border" />
                                <XAxis dataKey="name" tick={{ fontSize: 10, fontWeight: 700, fill: 'currentColor' }} className="text-muted-foreground" axisLine={false} tickLine={false} />
                                <YAxis tick={{ fontSize: 10, fontWeight: 700, fill: 'currentColor' }} className="text-muted-foreground" axisLine={false} tickLine={false} />
                                <Tooltip
                                    contentStyle={{
                                        backgroundColor: 'var(--card, #0f172a)',
                                        borderColor: 'var(--border, #334155)',
                                        borderRadius: '0.75rem',
                                        color: 'var(--foreground, #fff)'
                                    }}
                                />
                                <Bar dataKey="quantity" fill="#ec4899" radius={[4, 4, 0, 0]} />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                </Card>
            </div>
            </div>
            )}
        </EnterprisePage>
    );
};

export default DrugUsageReport;
