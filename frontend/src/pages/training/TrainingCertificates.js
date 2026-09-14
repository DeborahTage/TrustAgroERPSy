import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { trainingApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Input } from '../../components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { formatDate } from '../../utils';

const TrainingCertificates = () => {
  const navigate = useNavigate();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [code, setCode] = useState('');
  const [verify, setVerify] = useState(null);

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

  const cols = [
    { accessorKey: 'certificateNumber', header: 'Certificate #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.certificateNumber}</span> },
    { accessorKey: 'participantName', header: 'Participant' },
    { accessorKey: 'programTitle', header: 'Program' },
    { accessorKey: 'sessionCode', header: 'Session' },
    { accessorKey: 'issueDate', header: 'Issued', cell: ({ row }) => formatDate(row.original.issueDate) },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <Button size="sm" variant="outline" onClick={() => navigate(`/training/registrations/${row.original.registrationId}`)}>
          Registration
        </Button>
      ),
    },
  ];

  return (
    <EnterprisePage
      title="Certificates"
      subtitle="Issued completion certificates with public verification codes"
      breadcrumbs={['Training', 'Certificates']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
    >
      <Card className="border-none shadow-sm mb-6">
        <CardHeader><CardTitle className="text-base">Verify Certificate</CardTitle></CardHeader>
        <CardContent className="flex flex-wrap gap-3 items-end">
          <Input className="max-w-sm" placeholder="Verification or certificate number" value={code} onChange={(e) => setCode(e.target.value)} />
          <Button onClick={runVerify}>Verify</Button>
          {verify && (
            <div className={`text-sm ${verify.valid ? 'text-emerald-700' : 'text-rose-700'}`}>
              {verify.message}: {verify.certificateNumber || ''} · {verify.programTitle || ''} · {verify.participantName || ''}
            </div>
          )}
        </CardContent>
      </Card>
      <DataTable columns={cols} data={rows} />
    </EnterprisePage>
  );
};

export default TrainingCertificates;
