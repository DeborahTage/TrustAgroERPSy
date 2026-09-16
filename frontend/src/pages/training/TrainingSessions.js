import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { trainingApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { formatDate } from '../../utils';

const TrainingSessions = () => {
  const navigate = useNavigate();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await trainingApi.getSessions();
      setRows(res.data.data || []);
    } catch {
      setError('Failed to load sessions.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const cols = [
    { accessorKey: 'sessionCode', header: 'Session', cell: ({ row }) => <span className="font-mono font-bold">{row.original.sessionCode}</span> },
    { accessorKey: 'programTitle', header: 'Program' },
    { accessorKey: 'startDate', header: 'Start', cell: ({ row }) => formatDate(row.original.startDate) },
    { accessorKey: 'venue', header: 'Venue' },
    {
      id: 'capacity',
      header: 'Capacity',
      cell: ({ row }) => `${row.original.confirmedCount}/${row.original.capacity}`,
    },
    { accessorKey: 'remainingCapacity', header: 'Seats left' },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge> },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <Button size="sm" variant="outline" onClick={() => navigate(`/training/sessions/${row.original.id}`)}>
          Open
        </Button>
      ),
    },
  ];

  return (
    <EnterprisePage
      title="Training Sessions"
      subtitle="Cohorts with live capacity utilization"
      breadcrumbs={['Training', 'Sessions']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
    >
      <DataTable columns={cols} data={rows} />
    </EnterprisePage>
  );
};

export default TrainingSessions;
