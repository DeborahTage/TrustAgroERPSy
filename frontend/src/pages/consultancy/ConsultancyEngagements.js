import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { consultancyApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Input } from '../../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { formatCurrency, formatDate } from '../../utils';

const STATUSES = ['DRAFT', 'PROPOSED', 'APPROVED', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'CANCELLED'];

const ConsultancyEngagements = () => {
  const navigate = useNavigate();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = statusFilter !== 'all' ? { status: statusFilter } : {};
      const res = await consultancyApi.getEngagements(params);
      setRows(res.data.data || []);
    } catch {
      setError('Failed to load engagements.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => { load(); }, [load]);

  const filtered = useMemo(() => rows.filter((r) => {
    if (!search.trim()) return true;
    const q = search.trim().toLowerCase();
    return `${r.engagementNumber || ''} ${r.title || ''} ${r.customerName || ''} ${r.serviceName || ''}`.toLowerCase().includes(q);
  }), [rows, search]);

  const cols = [
    { accessorKey: 'engagementNumber', header: 'Engagement #', cell: ({ row }) => <span className="font-mono font-bold">{row.original.engagementNumber}</span> },
    { accessorKey: 'title', header: 'Title' },
    { accessorKey: 'customerName', header: 'Customer' },
    { accessorKey: 'serviceName', header: 'Service', cell: ({ row }) => row.original.serviceName || '—' },
    { accessorKey: 'startDate', header: 'Start', cell: ({ row }) => formatDate(row.original.startDate) },
    { accessorKey: 'agreedAmount', header: 'Agreed', cell: ({ row }) => formatCurrency(row.original.agreedAmount) },
    { accessorKey: 'outstandingAmount', header: 'Outstanding', cell: ({ row }) => formatCurrency(row.original.outstandingAmount) },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ row }) => <Badge variant="outline">{row.original.status}</Badge>,
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <Button size="sm" variant="outline" onClick={() => navigate(`/consultancy/engagements/${row.original.id}`)}>Open</Button>
      ),
    },
  ];

  return (
    <EnterprisePage
      title="Consultancy Engagements"
      subtitle="Active and historical advisory engagements"
      breadcrumbs={['Consultancy', 'Engagements']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
    >
      <div className="flex flex-wrap gap-3 mb-4">
        <Input className="max-w-xs" placeholder="Search…" value={search} onChange={(e) => setSearch(e.target.value)} />
        <Select value={statusFilter} onValueChange={setStatusFilter}>
          <SelectTrigger className="w-48"><SelectValue placeholder="Status" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All statuses</SelectItem>
            {STATUSES.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
          </SelectContent>
        </Select>
      </div>
      {filtered.length === 0 && !loading ? (
        <p className="text-sm text-slate-500 py-10 text-center">No engagements found.</p>
      ) : (
        <DataTable columns={cols} data={filtered} />
      )}
    </EnterprisePage>
  );
};

export default ConsultancyEngagements;
