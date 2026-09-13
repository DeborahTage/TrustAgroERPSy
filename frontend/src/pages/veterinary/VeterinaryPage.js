import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { vetApi, farmApi } from '../../api';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import { formatDate } from '../../utils';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/ui/tabs';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from '../../components/ui/dialog';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { Textarea } from '../../components/ui/textarea';
import { Syringe, FileText, Activity, Pill, Check, MoreHorizontal, Plus, ShieldAlert, ClipboardCheck, Stethoscope, Search, Filter, ArrowUpRight, TrendingUp, AlertTriangle, ChevronRight, Lock, Calendar as CalendarIcon, List, ShieldCheck } from 'lucide-react';
import { cn } from '../../lib/utils';
import { PERMISSIONS } from '../../lib/permissions';

const VeterinaryPage = () => {
  const { t } = useLanguage();
  const { hasAnyPermission } = useAuth();
  const [vaccinations, setVaccinations] = useState([]);
  const [healthReports, setHealthReports] = useState([]);
  const [diseaseCases, setDiseaseCases] = useState([]);
  const [treatments, setTreatments] = useState([]);
  const [prescriptions, setPrescriptions] = useState([]);
  const [biosecurityLogs, setBiosecurityLogs] = useState([]);
  const [selectedReport, setSelectedReport] = useState(null);
  const [reviewForm, setReviewForm] = useState({ suspectedDiagnosis: '', severity: 'LOW', treatmentPlan: '', status: 'REVIEWED' });
  const [reviewLoading, setReviewLoading] = useState(false);
  const [reviewError, setReviewError] = useState('');
  const [loading, setLoading] = useState(true);
  const [vaccinationView, setVaccinationView] = useState('list');
  const navigate = useNavigate();

  const getDaysInMonth = (year, month) => new Date(year, month + 1, 0).getDate();
  const getFirstDayOfMonth = (year, month) => new Date(year, month, 1).getDay();
  const currentDate = new Date();
  const [currentMonth, setCurrentMonth] = useState(currentDate.getMonth());
  const [currentYear, setCurrentYear] = useState(currentDate.getFullYear());

  const daysInMonth = getDaysInMonth(currentYear, currentMonth);
  const firstDay = getFirstDayOfMonth(currentYear, currentMonth);

  const renderCalendar = () => {
    let days = [];
    for (let i = 0; i < firstDay; i++) {
      days.push(<div key={`empty-${i}`} className="h-24 bg-gray-50/30 rounded-2xl" />);
    }
    for (let i = 1; i <= daysInMonth; i++) {
      const dateStr = `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`;
      const dayVacs = vaccinations.filter(v => v.scheduledDate === dateStr);
      const isToday = dateStr === new Date().toISOString().split('T')[0];

      days.push(
        <div key={i} className={cn(
          "h-24 p-3 border border-gray-100 rounded-2xl flex flex-col overflow-hidden hover:shadow-lg hover:border-primary/20 transition-all cursor-pointer group",
          isToday ? "bg-emerald-900 border-emerald-900 text-white shadow-xl shadow-primary/10" : "bg-white"
        )}>
          <span className={cn("text-xs font-black uppercase tracking-tighter", isToday ? "text-emerald-200" : "text-gray-400 group-hover:text-primary")}>{i}</span>
          <div className="flex-1 overflow-y-auto mt-2 space-y-1 scrollbar-hide">
            {dayVacs.map(v => (
              <div key={v.id} title={v.vaccineName} className={cn(
                "text-[9px] px-2 py-0.5 rounded-lg truncate font-black uppercase tracking-tight",
                isToday ? "bg-white/10 text-white" : v.status === 'COMPLETED' ? "bg-emerald-50 text-emerald-700" : "bg-primary/10 text-primary"
              )}>
                {v.vaccineName}
              </div>
            ))}
          </div>
        </div>
      );
    }
    return days;
  };

  const canWrite = hasAnyPermission([
    PERMISSIONS.CREATE_DISEASE_CASES,
    PERMISSIONS.EDIT_DISEASE_CASES,
    PERMISSIONS.CREATE_TREATMENTS,
    PERMISSIONS.CREATE_PRESCRIPTIONS,
    PERMISSIONS.MANAGE_VACCINATIONS,
  ]);

  const [error, setError] = useState(null);

  const loadData = () => {
    setLoading(true);
    setError(null);
    Promise.all([
      vetApi.getVaccinations(),
      vetApi.getHealthReports(),
      vetApi.getDiseaseCases(),
      vetApi.getTreatments(),
      vetApi.getPrescriptions()
    ]).then(([vaccinationsRes, healthReportsRes, diseaseCasesRes, treatmentsRes, prescriptionsRes]) => {
      setVaccinations(vaccinationsRes.data.data || []);
      setHealthReports(healthReportsRes.data.data || []);
      setDiseaseCases(diseaseCasesRes.data.data || []);
      setTreatments(treatmentsRes.data.data || []);
      setPrescriptions(prescriptionsRes.data.data || []);
      fetchBiosecurityLogs();
    }).catch((err) => {
      console.error('Failed to load veterinary data', err);
      setError('Could not load veterinary data.');
      toast.error('Could not load veterinary data. Please refresh or try again.');
    }).finally(() => setLoading(false));
  };

  useEffect(() => { loadData(); }, []);

  const fetchBiosecurityLogs = async () => {
    try {
      const farmRes = await farmApi.getAll();
      if (farmRes.data.data?.length > 0) {
        const res = await farmApi.getBiosecurityLogs(farmRes.data.data[0].id);
        setBiosecurityLogs(res.data.data || []);
      }
    } catch (err) { console.error("Could not load biosecurity", err); }
  };

  const completeVaccination = async (id) => {
    if (!canWrite) return;
    try {
      await vetApi.completeVaccination(id);
      setVaccinations((prev) => prev.map((item) => item.id === id ? { ...item, status: 'COMPLETED' } : item));
      toast.success('Vaccination marked as complete');
    } catch (e) {
      toast.error('Failed to update vaccination');
    }
  };

  const openReviewModal = (report) => {
    if (!canWrite) return;
    setSelectedReport(report);
    setReviewForm({
      suspectedDiagnosis: report.suspectedDiagnosis || '',
      severity: report.severity || 'LOW',
      treatmentPlan: report.treatmentPlan || '',
      status: report.status === 'CLOSED' ? 'CLOSED' : 'REVIEWED',
    });
    setReviewError('');
  };

  const submitReview = async () => {
    if (!selectedReport || !canWrite) return;
    setReviewLoading(true);
    setReviewError('');
    try {
      const res = await vetApi.reviewHealthReport(selectedReport.id, reviewForm);
      setHealthReports((prev) => prev.map((report) => report.id === selectedReport.id ? res.data.data : report));
      toast.success('Report reviewed successfully');
      setSelectedReport(null);
    } catch (err) {
      setReviewError(err.response?.data?.message || 'Unable to review report');
    } finally {
      setReviewLoading(false);
    }
  };

  const vaccinationCols = useMemo(() => [
    {
      accessorKey: 'farmName',
      header: 'Farm / Batch',
      cell: ({ row }) => (
        <div className="flex flex-col">
          <span className="font-black text-gray-900 leading-tight">{row.original.farmName}</span>
          <span className="text-[10px] font-black text-primary uppercase tracking-widest">{row.original.batchCode}</span>
        </div>
      )
    },
    {
      accessorKey: 'vaccineName',
      header: 'Biological Item',
      cell: ({ row }) => <span className="font-black text-slate-700">{row.original.vaccineName}</span>
    },
    {
      accessorKey: 'scheduledDate',
      header: 'Protocol Schedule',
      cell: ({ row }) => (
        <div className="flex flex-col">
          <span className="font-bold text-gray-600">{formatDate(row.original.scheduledDate)}</span>
          {row.original.actualDate && <span className="text-[10px] text-emerald-600 font-black uppercase italic">Completed: {formatDate(row.original.actualDate)}</span>}
        </div>
      )
    },
    {
      accessorKey: 'status',
      header: 'Compliance',
      cell: ({ row }) => <StatusBadge status={row.original.status?.toLowerCase()} />
    },
    {
      id: 'actions',
      cell: ({ row }) => row.original.status === 'SCHEDULED' && canWrite && (
        <div className="flex justify-end">
          <Button size="sm" variant="outline" className="rounded-xl border-gray-200 h-9 font-black text-[10px] uppercase tracking-wider hover:bg-emerald-50 hover:text-emerald-600 hover:border-emerald-100 transition-all" onClick={() => completeVaccination(row.original.id)}>
            <Check className="mr-2 h-3.5 w-3.5" />
            Sign Completion
          </Button>
        </div>
      )
    },
  ], [canWrite]);

  const healthReportCols = useMemo(() => [
    {
      accessorKey: 'farmName',
      header: 'Clinical Source',
      cell: ({ row }) => (
        <div className="flex flex-col">
          <span className="font-black text-gray-900">{row.original.farmName}</span>
          <span className="text-[10px] font-black text-rose-500 uppercase tracking-widest">{row.original.batchCode}</span>
        </div>
      )
    },
    {
      accessorKey: 'reportDate',
      header: 'Submission',
      cell: ({ row }) => <span className="font-bold text-gray-600">{formatDate(row.original.reportDate)}</span>
    },
    {
      accessorKey: 'mortalityObserved',
      header: 'Mortality Impact',
      cell: ({ row }) => (
        <div className="flex items-center gap-3">
          <div className="flex flex-col items-center justify-center h-10 w-12 bg-rose-50 rounded-xl">
            <span className="text-sm font-black text-rose-600 leading-none">{row.original.mortalityObserved || 0}</span>
          </div>
          <div className="flex flex-col items-center justify-center h-10 w-12 bg-amber-50 rounded-xl">
            <span className="text-sm font-black text-amber-600 leading-none">{row.original.numberAffected || 0}</span>
          </div>
        </div>
      )
    },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <StatusBadge status={row.original.status?.toLowerCase()} /> },
    {
      id: 'actions',
      cell: ({ row }) => canWrite && (
        <div className="flex justify-end gap-2">
          <Button size="sm" variant="ghost" className="h-9 w-9 rounded-xl hover:bg-primary/10 hover:text-primary" onClick={() => openReviewModal(row.original)}>
            <ClipboardCheck className="h-4.5 w-4.5" />
          </Button>
          <Button size="sm" variant="ghost" className="h-9 w-9 rounded-xl hover:bg-rose-50 hover:text-rose-600" onClick={() => navigate('/veterinary/disease-cases/new', { state: { healthReport: row.original } })}>
            <ShieldAlert className="h-4.5 w-4.5" />
          </Button>
          <Button size="sm" variant="ghost" className="h-9 w-9 rounded-xl hover:bg-blue-50 hover:text-blue-600" onClick={() => navigate('/veterinary/prescriptions/new', { state: { healthReport: row.original } })}>
            <Pill className="h-4.5 w-4.5" />
          </Button>
        </div>
      ),
    },
  ], [canWrite, navigate]);

  const vetStats = [
    { title: 'Active Cases', value: diseaseCases.filter(c => c.status === 'ACTIVE').length, icon: Activity, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    { title: 'Open Reports', value: healthReports.filter(r => r.status === 'OPEN').length, icon: AlertTriangle, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: 'Pending Rx', value: prescriptions.filter(p => p.status === 'PENDING').length, icon: Pill, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Vaccinations', value: vaccinations.filter(v => v.status !== 'COMPLETED').length, icon: Syringe, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
  ];

  return (
    <EnterprisePage
      title="Veterinary Command Center"
      subtitle="Medical surveillance, biosecurity enforcement, and pharmaceutical oversight"
      breadcrumbs={['Veterinary', 'Hub']}
      loading={loading}
      error={error}
      onRetry={loadData}
      onRefresh={loadData}
      stats={vetStats}
      actions={
        <>
          <Button variant="outline" onClick={() => navigate('/veterinary/reports')}>
            <FileText className="mr-2 h-4 w-4" /> Reports
          </Button>
          <Button className="bg-rose-600 hover:bg-rose-700" onClick={() => navigate('/veterinary/health-reports/new')}>
            <AlertTriangle className="mr-2 h-4 w-4" /> Emergency Signal
          </Button>
        </>
      }
    >
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        <div className="lg:col-span-1 space-y-6">
          <Card className="border-none shadow-2xl shadow-gray-200/50 rounded-[2.5rem] bg-[#0f172a] text-white overflow-hidden relative">
            <div className="absolute top-0 right-0 p-8 opacity-10">
              <Activity className="h-32 w-32 rotate-12" />
            </div>
            <CardHeader className="p-8 pb-4">
              <CardTitle className="text-xs font-black uppercase tracking-[0.2em] text-emerald-300">Surveillance Pulse</CardTitle>
            </CardHeader>
            <CardContent className="p-8 pt-0 space-y-8">
              <div className="flex flex-col">
                <span className="text-5xl font-black">{diseaseCases.filter(c => c.status === 'ACTIVE').length}</span>
                <span className="text-[10px] font-black uppercase tracking-widest text-emerald-300 mt-2">Active Pathologies</span>
              </div>
              <div className="grid grid-cols-2 gap-4 border-t border-white/10 pt-8">
                <div className="flex flex-col">
                  <span className="text-xl font-black text-emerald-400">{vaccinations.filter(v => v.status === 'SCHEDULED' && new Date(v.scheduledDate) <= new Date()).length}</span>
                  <span className="text-[9px] font-black uppercase text-white/50 tracking-tight">Vacs Pending</span>
                </div>
                <div className="flex flex-col">
                  <span className="text-xl font-black text-amber-400">{prescriptions.filter(p => p.status === 'PENDING').length}</span>
                  <span className="text-[9px] font-black uppercase text-white/50 tracking-tight">Rx Queued</span>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm bg-white rounded-[2.5rem] p-4 flex flex-col gap-2">
            <Button variant="ghost" className="h-14 rounded-2xl justify-start px-6 group hover:bg-blue-50 transition-all" onClick={() => navigate('/veterinary/vaccinations/new')}>
              <div className="h-10 w-10 bg-blue-100 rounded-xl flex items-center justify-center mr-4 group-hover:scale-110 transition-transform">
                <Syringe className="h-5 w-5 text-blue-600" />
              </div>
              <span className="text-sm font-black text-gray-700 uppercase tracking-tight">Protocol Entry</span>
            </Button>
            <Button variant="ghost" className="h-14 rounded-2xl justify-start px-6 group hover:bg-rose-50 transition-all" onClick={() => navigate('/veterinary/disease-cases/new')}>
              <div className="h-10 w-10 bg-rose-100 rounded-xl flex items-center justify-center mr-4 group-hover:scale-110 transition-transform">
                <ShieldAlert className="h-5 w-5 text-rose-600" />
              </div>
              <span className="text-sm font-black text-gray-700 uppercase tracking-tight">Pathology Log</span>
            </Button>
            <Button variant="ghost" className="h-14 rounded-2xl justify-start px-6 group hover:bg-emerald-50 transition-all" onClick={() => navigate('/veterinary/treatments/new')}>
              <div className="h-10 w-10 bg-emerald-100 rounded-xl flex items-center justify-center mr-4 group-hover:scale-110 transition-transform">
                <Stethoscope className="h-5 w-5 text-emerald-600" />
              </div>
              <span className="text-sm font-black text-gray-700 uppercase tracking-tight">Medical Plan</span>
            </Button>
          </Card>
        </div>

        <div className="lg:col-span-3">
          <Tabs defaultValue="vaccinations" className="w-full">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
              <TabsList className="bg-gray-100/50 p-1.5 rounded-3xl h-auto border border-gray-100 flex-wrap sm:flex-nowrap">
                {['vaccinations', 'healthReports', 'surveillance', 'ledger', 'biosecurity'].map(tab => (
                  <TabsTrigger key={tab} value={tab} className="data-[state=active]:bg-white data-[state=active]:text-primary data-[state=active]:shadow-lg rounded-2xl px-6 py-2.5 font-black text-[10px] uppercase tracking-wider transition-all">
                    {tab === 'healthReports' ? 'Surveillance' : tab === 'surveillance' ? 'Diseases' : tab === 'ledger' ? 'Rx Ledger' : tab}
                  </TabsTrigger>
                ))}
              </TabsList>
              <div className="h-11 px-6 bg-emerald-50 rounded-2xl flex items-center gap-3 border border-emerald-100 shadow-sm shadow-emerald-50">
                <ShieldCheck className="h-4.5 w-4.5 text-emerald-600" />
                <span className="text-[10px] font-black uppercase text-emerald-700 tracking-widest whitespace-nowrap">Status: Biosecure</span>
              </div>
            </div>

            <div className="bg-white rounded-[2.5rem] shadow-sm border border-gray-100 overflow-hidden min-h-[600px]">
              <TabsContent value="vaccinations" className="m-0 focus:ring-0 outline-none">
                <div className="flex justify-between items-center p-6 border-b border-gray-50">
                  <h3 className="text-xs font-black uppercase tracking-[0.2em] text-gray-400">Vaccination Compliance</h3>
                  <div className="bg-gray-100 p-1 rounded-xl flex gap-1">
                    <Button size="sm" variant={vaccinationView === 'list' ? 'default' : 'ghost'} className={cn("h-8 rounded-lg text-[9px] uppercase font-black", vaccinationView === 'list' && "bg-white text-primary shadow-sm hover:bg-white")} onClick={() => setVaccinationView('list')}>
                      <List className="h-3 w-3 mr-1" /> Grid
                    </Button>
                    <Button size="sm" variant={vaccinationView === 'calendar' ? 'default' : 'ghost'} className={cn("h-8 rounded-lg text-[9px] uppercase font-black", vaccinationView === 'calendar' && "bg-white text-primary shadow-sm hover:bg-white")} onClick={() => setVaccinationView('calendar')}>
                      <CalendarIcon className="h-3 w-3 mr-1" /> Calendar
                    </Button>
                  </div>
                </div>
                {vaccinationView === 'list' ? (
                  <DataTable columns={vaccinationCols} data={vaccinations} loading={loading} />
                ) : (
                  <div className="p-8">
                    <div className="flex items-center justify-between mb-8">
                      <h3 className="text-xl font-black text-emerald-900 uppercase tracking-tighter">
                        {new Date(currentYear, currentMonth).toLocaleString('default', { month: 'long', year: 'numeric' })}
                      </h3>
                      <div className="flex gap-2">
                        <Button variant="ghost" size="icon" className="rounded-xl bg-gray-50" onClick={() => { setCurrentMonth(prev => prev === 0 ? 11 : prev - 1); setCurrentYear(prev => currentMonth === 0 ? prev - 1 : prev); }}><ChevronRight className="rotate-180 h-4 w-4" /></Button>
                        <Button variant="ghost" size="icon" className="rounded-xl bg-gray-50" onClick={() => { setCurrentMonth(prev => prev === 11 ? 0 : prev + 1); setCurrentYear(prev => currentMonth === 11 ? prev + 1 : prev); }}><ChevronRight className="h-4 w-4" /></Button>
                      </div>
                    </div>
                    <div className="grid grid-cols-7 gap-4">
                      {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map(d => <div key={d} className="text-center text-[10px] font-black uppercase text-gray-300 tracking-widest pb-4">{d}</div>)}
                      {renderCalendar()}
                    </div>
                  </div>
                )}
              </TabsContent>

              <TabsContent value="healthReports" className="m-0 focus:ring-0 outline-none">
                <DataTable columns={healthReportCols} data={healthReports} loading={loading} searchable />
              </TabsContent>

              <TabsContent value="surveillance" className="m-0 focus:ring-0 outline-none">
                <DataTable columns={[
                  { accessorKey: 'farmName', header: 'Farm' },
                  { accessorKey: 'suspectedDisease', header: 'Pathology', cell: ({ row }) => <span className="font-black text-rose-600">{row.original.suspectedDisease}</span> },
                  { accessorKey: 'dateDetected', header: 'Detection', cell: ({ row }) => <span className="font-bold text-gray-500">{formatDate(row.original.dateDetected)}</span> },
                  { accessorKey: 'severity', header: 'Triage', cell: ({ row }) => <StatusBadge status={row.original.severity?.toLowerCase()} /> },
                  { accessorKey: 'status', header: 'Resolution', cell: ({ row }) => <StatusBadge status={row.original.status?.toLowerCase()} /> }
                ]} data={diseaseCases} loading={loading} />
              </TabsContent>

              <TabsContent value="ledger" className="m-0 focus:ring-0 outline-none">
                <DataTable columns={[
                  { accessorKey: 'prescriptionNumber', header: 'Rx Reference', cell: ({ row }) => <span className="font-black text-primary">#{row.original.prescriptionNumber}</span> },
                  { accessorKey: 'drugName', header: 'Medication', cell: ({ row }) => <span className="font-bold text-slate-700">{row.original.drugName}</span> },
                  { accessorKey: 'quantity', header: 'Dose Qty', cell: ({ row }) => <span className="font-black">{row.original.quantity} {row.original.unit}</span> },
                  { accessorKey: 'status', header: 'Fulfillment', cell: ({ row }) => <StatusBadge status={row.original.status?.toLowerCase()} /> }
                ]} data={prescriptions} loading={loading} />
              </TabsContent>

              <TabsContent value="biosecurity" className="m-0 focus:ring-0 outline-none p-6">
                <div className="flex justify-between items-center mb-8 px-2">
                  <div>
                    <h3 className="text-xl font-black text-[#0f172a] uppercase tracking-tight">Security Audit</h3>
                    <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mt-1">Farm Access and Sanitation Compliance</p>
                  </div>
                  <Button className="h-11 rounded-xl bg-[#0f172a] hover:bg-[#1e293b] font-black uppercase text-[10px] px-6" onClick={() => navigate('/veterinary/biosecurity/new')}>
                    <Plus className="mr-2 h-4 w-4" /> Protocol Log
                  </Button>
                </div>
                <DataTable columns={[
                  { accessorKey: 'timestamp', header: 'Timestamp', cell: ({ row }) => <span className="font-medium text-gray-500">{new Date(row.original.timestamp).toLocaleString()}</span> },
                  { accessorKey: 'farmName', header: 'Entity', cell: ({ row }) => <span className="font-black">{row.original.farmName}</span> },
                  { accessorKey: 'visitorName', header: 'Operator', cell: ({ row }) => <span className="font-bold text-primary">{row.original.visitorName || 'External Staff'}</span> },
                  {
                    id: 'compliance', header: 'Protocol Checks', cell: ({ row }) => (
                      <div className="flex gap-1.5 flex-wrap">
                        {row.original.ppeWorn && <span className="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold bg-emerald-50 text-emerald-700 border-none font-black text-[8px] uppercase tracking-tight">PPE</span>}
                        {row.original.footbathRefreshed && <span className="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold bg-blue-50 text-blue-700 border-none font-black text-[8px] uppercase tracking-tight">SANITIZED</span>}
                      </div>
                    )
                  }
                ]} data={biosecurityLogs} loading={loading} />
              </TabsContent>
            </div>
          </Tabs>
        </div>
      </div>

      <Dialog open={!!selectedReport} onOpenChange={() => setSelectedReport(null)}>
        <DialogContent className="sm:max-w-[650px] rounded-[3rem] p-0 overflow-hidden border-none shadow-2xl">
          <div className="bg-[#0f172a] p-10 text-white relative">
            <div className="absolute top-0 right-0 p-10 opacity-5">
              <ClipboardCheck className="h-32 w-32" />
            </div>
            <DialogHeader>
              <DialogTitle className="text-3xl font-black tracking-tighter">Clinical Peer Review</DialogTitle>
              <DialogDescription className="text-emerald-300 font-black uppercase tracking-widest text-[10px] mt-2 opacity-70">
                Assessing Environmental Health Incident: {selectedReport?.id}
              </DialogDescription>
            </DialogHeader>
          </div>

          <div className="p-10 space-y-8 bg-white">
            <div className="grid grid-cols-2 gap-8">
              <div className="space-y-3">
                <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400">Diagnosis Confirmation</Label>
                <Input
                  placeholder="Suspected Pathology..."
                  className="h-12 rounded-2xl border-gray-100 bg-gray-50/50 font-bold focus:ring-primary"
                  value={reviewForm.suspectedDiagnosis}
                  onChange={e => setReviewForm(p => ({ ...p, suspectedDiagnosis: e.target.value }))}
                />
              </div>
              <div className="space-y-3">
                <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400">Risk Severity</Label>
                <Select value={reviewForm.severity} onValueChange={v => setReviewForm(p => ({ ...p, severity: v }))}>
                  <SelectTrigger className="h-12 rounded-2xl border-gray-100 bg-gray-50/50 font-black focus:ring-0">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent className="rounded-2xl border-none shadow-2xl">
                    {['LOW', 'MODERATE', 'HIGH', 'CRITICAL'].map(s => <SelectItem key={s} value={s} className="rounded-xl font-bold">{s}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="space-y-3">
              <Label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400">Clinical Directives</Label>
              <Textarea
                placeholder="Document required medical interventions..."
                className="rounded-3xl border-gray-100 bg-gray-50/50 min-h-[120px] p-5 font-medium leading-relaxed"
                value={reviewForm.treatmentPlan}
                onChange={e => setReviewForm(p => ({ ...p, treatmentPlan: e.target.value }))}
              />
            </div>

            <div className="flex justify-between items-center pt-6 border-t border-gray-50">
              <Button variant="ghost" className="rounded-2xl font-black text-gray-400 uppercase tracking-widest text-[10px] px-8" onClick={() => setSelectedReport(null)}>Discard</Button>
              <Button className="h-12 rounded-2xl bg-primary hover:bg-primary/90 text-white font-black uppercase tracking-tighter px-10 shadow-lg shadow-primary/10" onClick={submitReview}>
                Commit Audit
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default VeterinaryPage;
