import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { trainingApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../../components/ui/dialog';
import { formatDate } from '../../utils';
import { Printer, Award, Plus, CheckCircle2 } from 'lucide-react';
import { renderCertificateHtml, printHtml } from '../../lib/trustDocumentTemplates';
import { toast } from 'react-toastify';

const TrainingCertificates = () => {
  const navigate = useNavigate();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [code, setCode] = useState('');
  const [verify, setVerify] = useState(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [certForm, setCertForm] = useState({
    recipientName: '',
    trainingTitle: 'Poultry Farm Establishment, Poultry Business Production and Management',
    startDate: new Date().toISOString().slice(0, 10),
    endDate: new Date().toISOString().slice(0, 10),
    venue: 'Addis Ababa Center, Ethiopia',
    batchNo: '73',
  });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await trainingApi.getCertificates();
      setRows(res.data.data || []);
    } catch {
      setError('Failed to load certificates.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const runVerify = async () => {
    if (!code.trim()) return;
    try {
      const res = await trainingApi.verifyCertificate(code.trim());
      setVerify(res.data.data);
    } catch {
      setVerify({ valid: false, message: 'Verification failed' });
    }
  };

  const handlePrintRow = (cert) => {
    printHtml(renderCertificateHtml({
      recipientName: cert.participantName,
      trainingTitle: cert.programTitle || 'Poultry Farm Establishment, Poultry Business Production and Management',
      startDate: cert.sessionStartDate ? formatDate(cert.sessionStartDate) : '01/10/2026',
      endDate: cert.sessionEndDate ? formatDate(cert.sessionEndDate) : '05/10/2026',
      venue: cert.venue || 'Addis Ababa, Ethiopia',
      batchNo: cert.sessionCode || '73',
    }), `Certificate - ${cert.participantName}`);
  };

  const handlePrintCustom = (e) => {
    e.preventDefault();
    if (!certForm.recipientName.trim()) {
      toast.error('Recipient name is required');
      return;
    }
    printHtml(renderCertificateHtml(certForm), `Certificate - ${certForm.recipientName}`);
    toast.success('Certificate dispatched to printer');
    setDialogOpen(false);
  };

  const cols = [
    { accessorKey: 'certificateNumber', header: 'Certificate #', cell: ({ row }) => <span className="font-mono font-bold text-foreground">{row.original.certificateNumber}</span> },
    { accessorKey: 'participantName', header: 'Participant', cell: ({ row }) => <span className="font-semibold">{row.original.participantName}</span> },
    { accessorKey: 'programTitle', header: 'Program' },
    { accessorKey: 'sessionCode', header: 'Session / Batch' },
    { accessorKey: 'issueDate', header: 'Issued', cell: ({ row }) => formatDate(row.original.issueDate) },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <Badge variant="outline" className="border-emerald-600 text-emerald-700">{row.original.status}</Badge> },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <div className="flex items-center justify-end gap-2">
          <Button size="sm" variant="outline" onClick={() => handlePrintRow(row.original)}>
            <Printer className="h-3.5 w-3.5 mr-1" /> Print Certificate
          </Button>
          <Button size="sm" variant="ghost" onClick={() => navigate(`/training/registrations/${row.original.registrationId}`)}>
            Registration
          </Button>
        </div>
      ),
    },
  ];

  return (
    <EnterprisePage
      title="Training Certificates"
      subtitle="Issued completion certificates with public verification codes and official printable diplomas"
      breadcrumbs={['Training', 'Certificates']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={
        <Button size="sm" onClick={() => setDialogOpen(true)}>
          <Plus className="h-4 w-4 mr-1.5" /> Generate Certificate
        </Button>
      }
    >
      <Card className="border-none shadow-sm mb-6 bg-card border border-border/60">
        <CardHeader><CardTitle className="text-base flex items-center gap-2"><Award className="h-4 w-4 text-primary" /> Verify Certificate</CardTitle></CardHeader>
        <CardContent className="flex flex-wrap gap-3 items-end">
          <Input className="max-w-sm" placeholder="Verification code or certificate number" value={code} onChange={(e) => setCode(e.target.value)} />
          <Button onClick={runVerify}>Verify</Button>
          {verify && (
            <div className={`text-sm flex items-center gap-1.5 ${verify.valid ? 'text-emerald-700 dark:text-emerald-400 font-semibold' : 'text-rose-700 font-semibold'}`}>
              {verify.valid && <CheckCircle2 className="h-4 w-4" />}
              {verify.message}: {verify.certificateNumber || ''} · {verify.programTitle || ''} · {verify.participantName || ''}
            </div>
          )}
        </CardContent>
      </Card>

      <div className="bg-card rounded-xl border border-border/70 p-1">
        <DataTable columns={cols} data={rows} emptyMessage="No training certificates issued yet" />
      </div>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Generate & Print Certificate</DialogTitle>
          </DialogHeader>
          <form onSubmit={handlePrintCustom} className="space-y-3">
            <div className="space-y-1">
              <Label>Recipient Full Name *</Label>
              <Input
                required
                placeholder="e.g. Abebe Balcha"
                value={certForm.recipientName}
                onChange={(e) => setCertForm({ ...certForm, recipientName: e.target.value })}
              />
            </div>
            <div className="space-y-1">
              <Label>Training Title</Label>
              <Input
                value={certForm.trainingTitle}
                onChange={(e) => setCertForm({ ...certForm, trainingTitle: e.target.value })}
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label>Start Date</Label>
                <Input
                  type="date"
                  value={certForm.startDate}
                  onChange={(e) => setCertForm({ ...certForm, startDate: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label>End Date</Label>
                <Input
                  type="date"
                  value={certForm.endDate}
                  onChange={(e) => setCertForm({ ...certForm, endDate: e.target.value })}
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label>Venue</Label>
                <Input
                  value={certForm.venue}
                  onChange={(e) => setCertForm({ ...certForm, venue: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label>Batch Number</Label>
                <Input
                  value={certForm.batchNo}
                  onChange={(e) => setCertForm({ ...certForm, batchNo: e.target.value })}
                />
              </div>
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                Cancel
              </Button>
              <Button type="submit">
                <Printer className="h-4 w-4 mr-1.5" /> Print Certificate
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default TrainingCertificates;
