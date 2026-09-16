import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { consultancyApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { formatCurrency, formatDate } from '../../utils';

const ConsultancyContracts = () => {
  const navigate = useNavigate();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const engRes = await consultancyApi.getEngagements();
      const engagements = engRes.data.data || [];
      const settled = await Promise.allSettled(
        engagements.map(async (e) => {
          try {
            const cRes = await consultancyApi.getContractByEngagement(e.id);
            return { ...cRes.data.data, engagementTitle: e.title, engagementStatus: e.status };
          } catch {
            return {
              id: `eng-${e.id}`,
              engagementId: e.id,
              engagementNumber: e.engagementNumber,
              engagementTitle: e.title,
              customerName: e.customerName,
              title: '—',
              contractValue: e.agreedAmount,
              status: 'NONE',
              startDate: e.startDate,
              endDate: e.expectedEndDate,
              engagementStatus: e.status,
              noContract: true,
            };
          }
        })
      );
      setRows(settled.filter((s) => s.status === 'fulfilled').map((s) => s.value));
    } catch {
      setError('Failed to load contracts.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const cols = [
    {
      accessorKey: 'contractNumber',
      header: 'Contract #',
      cell: ({ row }) => (
        <span className="font-mono font-bold">
          {row.original.noContract ? '—' : row.original.contractNumber}
        </span>
      ),
    },
    { accessorKey: 'engagementNumber', header: 'Engagement' },
    { accessorKey: 'customerName', header: 'Customer' },
    { accessorKey: 'title', header: 'Title' },
    {
      accessorKey: 'contractValue',
      header: 'Value',
      cell: ({ row }) => formatCurrency(row.original.contractValue),
    },
    {
      accessorKey: 'startDate',
      header: 'Start',
      cell: ({ row }) => formatDate(row.original.startDate),
    },
    {
      accessorKey: 'status',
      header: 'Contract',
      cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge>,
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <Button
          size="sm"
          variant="outline"
          onClick={() => navigate(`/consultancy/engagements/${row.original.engagementId}`)}
        >
          Open Engagement
        </Button>
      ),
    },
  ];

  return (
    <EnterprisePage
      title="Consultancy Contracts"
      subtitle="Contracts are managed on each engagement — open an engagement to create or activate"
      breadcrumbs={['Consultancy', 'Contracts']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={
        <Button size="sm" variant="outline" onClick={() => navigate('/consultancy/engagements')}>Engagements</Button>
      }
    >
      {rows.length === 0 && !loading ? (
        <p className="text-sm text-slate-500 py-10 text-center">No engagements yet. Open Engagements to get started.</p>
      ) : (
        <DataTable columns={cols} data={rows} />
      )}
    </EnterprisePage>
  );
};

export default ConsultancyContracts;
