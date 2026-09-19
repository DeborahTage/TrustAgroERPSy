import React, { useCallback, useEffect, useState } from 'react';
import { materialRequestApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { formatDate } from '../../utils';

const MaterialRequestReports = () => {
  const [rows, setRows] = useState([]);
  const [fulfillment, setFulfillment] = useState(null);
  const [byStatus, setByStatus] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState({ from: '', to: '', type: 'ALL', status: 'ALL' });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = {};
      if (filters.from) params.from = filters.from;
      if (filters.to) params.to = filters.to;
      if (filters.type !== 'ALL') params.type = filters.type;
      if (filters.status !== 'ALL') params.status = filters.status;
      const [sum, ful, st] = await Promise.all([
        materialRequestApi.reportSummary(params),
        materialRequestApi.reportFulfillment(params),
        materialRequestApi.reportByStatus(params),
      ]);
      setRows(sum.data?.data || []);
      setFulfillment(ful.data?.data || null);
      setByStatus(st.data?.data || null);
    } catch (e) {
      setError(e?.response?.data?.message || 'Failed to load reports');
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => { load(); }, [load]);

  const columns = [
    { header: 'Number', accessorKey: 'requestNumber' },
    { header: 'Type', accessorKey: 'type' },
    { header: 'Status', accessorKey: 'status' },
    { header: 'Department', accessorKey: 'department' },
    { header: 'Farm', accessorKey: 'farmName' },
    { header: 'Date', cell: ({ row }) => formatDate(row.original.requestDate) },
    { header: 'Approved', accessorKey: 'approvedAmount' },
  ];

  return (
    <EnterprisePage title="Material Request Reports" subtitle="Summary and fulfillment metrics">
      <div className="mb-4 flex flex-wrap gap-3">
        <Input type="date" className="w-40" value={filters.from} onChange={(e) => setFilters({ ...filters, from: e.target.value })} />
        <Input type="date" className="w-40" value={filters.to} onChange={(e) => setFilters({ ...filters, to: e.target.value })} />
        <Select value={filters.type} onValueChange={(v) => setFilters({ ...filters, type: v })}>
          <SelectTrigger className="w-48"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All types</SelectItem>
            <SelectItem value="PURCHASE_REQUEST">Purchase</SelectItem>
            <SelectItem value="INTERNAL_ISSUE_REQUEST">Internal</SelectItem>
          </SelectContent>
        </Select>
        <Button onClick={load}>Run</Button>
      </div>
      {error && <p className="mb-3 text-sm text-rose-600">{error}</p>}
      {fulfillment && (
        <div className="mb-4 grid gap-3 sm:grid-cols-3 text-sm">
          <div className="rounded border p-3">Total: <strong>{fulfillment.total}</strong></div>
          <div className="rounded border p-3">Fulfilled: <strong>{fulfillment.fulfilled}</strong></div>
          <div className="rounded border p-3">Rate: <strong>{Number(fulfillment.fulfillmentRate || 0).toFixed(1)}%</strong></div>
        </div>
      )}
      {byStatus?.counts && (
        <p className="mb-3 text-xs text-slate-600">
          By status: {Object.entries(byStatus.counts).filter(([, v]) => v > 0).map(([k, v]) => `${k}=${v}`).join(', ') || 'none'}
        </p>
      )}
      <DataTable columns={columns} data={rows} loading={loading} />
    </EnterprisePage>
  );
};

export default MaterialRequestReports;
