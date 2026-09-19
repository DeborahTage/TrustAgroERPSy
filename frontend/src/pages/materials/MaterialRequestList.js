import React, { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { materialRequestApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatDate } from '../../utils';

const STATUS_STYLES = {
  DRAFT: 'bg-slate-100 text-slate-700',
  FINANCE_REVIEW: 'bg-blue-100 text-blue-700',
  APPROVED: 'bg-indigo-100 text-indigo-700',
  READY_FOR_STORE: 'bg-cyan-100 text-cyan-800',
  REJECTED: 'bg-rose-100 text-rose-700',
  PAYMENT_SENT: 'bg-amber-100 text-amber-800',
  PARTIALLY_FULFILLED: 'bg-orange-100 text-orange-800',
  ISSUED: 'bg-emerald-100 text-emerald-700',
  FULFILLED: 'bg-emerald-100 text-emerald-800',
  CANCELLED: 'bg-slate-200 text-slate-600',
};

const MaterialRequestList = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [rows, setRows] = useState([]);
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [status, setStatus] = useState('ALL');
  const [type, setType] = useState('ALL');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = {};
      if (status !== 'ALL') params.status = status;
      if (type !== 'ALL') params.type = type;
      const [listRes, dashRes] = await Promise.all([
        materialRequestApi.list(params),
        materialRequestApi.dashboard().catch(() => ({ data: { data: null } })),
      ]);
      setRows(listRes.data?.data || []);
      setDashboard(dashRes.data?.data || null);
    } catch (e) {
      setError(e?.response?.data?.message || 'Failed to load material requests');
    } finally {
      setLoading(false);
    }
  }, [status, type]);

  useEffect(() => { load(); }, [load]);

  const columns = [
    {
      header: 'Number',
      accessorKey: 'requestNumber',
      cell: ({ row }) => (
        <Link className="text-sky-700 underline" to={`/materials/requests/${row.original.id}`}>
          {row.original.requestNumber}
        </Link>
      ),
    },
    { header: 'Type', accessorKey: 'type' },
    {
      header: 'Status',
      accessorKey: 'status',
      cell: ({ row }) => (
        <Badge className={STATUS_STYLES[row.original.status] || 'bg-slate-100'}>{row.original.status}</Badge>
      ),
    },
    { header: 'Requester', accessorKey: 'requesterName' },
    { header: 'Farm', accessorKey: 'farmName' },
    {
      header: 'Date',
      accessorKey: 'requestDate',
      cell: ({ row }) => formatDate(row.original.requestDate),
    },
    {
      header: 'Est. Total',
      accessorKey: 'estimatedTotal',
      cell: ({ row }) => row.original.estimatedTotal ?? '—',
    },
  ];

  return (
    <EnterprisePage
      title="Material Requests"
      subtitle="Purchase and internal issue workflow"
      actions={
        hasPermission(PERMISSIONS.CREATE_MATERIAL_REQUESTS) && (
          <Button onClick={() => navigate('/materials/requests/new')}>New request</Button>
        )
      }
    >
      {dashboard && (
        <div className="mb-4 grid gap-3 sm:grid-cols-4 text-sm">
          <div className="rounded border p-3">Finance pending: <strong>{dashboard.financePending}</strong></div>
          <div className="rounded border p-3">Store issues: <strong>{dashboard.storePendingIssues}</strong></div>
          <div className="rounded border p-3">Pending receipts: <strong>{dashboard.pendingReceipts}</strong></div>
          <div className="rounded border p-3">Fulfillment: <strong>{Number(dashboard.fulfillmentRate || 0).toFixed(1)}%</strong></div>
        </div>
      )}
      <div className="mb-4 flex flex-wrap gap-3">
        <Select value={status} onValueChange={setStatus}>
          <SelectTrigger className="w-48"><SelectValue placeholder="Status" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All statuses</SelectItem>
            {Object.keys(STATUS_STYLES).map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
          </SelectContent>
        </Select>
        <Select value={type} onValueChange={setType}>
          <SelectTrigger className="w-56"><SelectValue placeholder="Type" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All types</SelectItem>
            <SelectItem value="PURCHASE_REQUEST">Purchase</SelectItem>
            <SelectItem value="INTERNAL_ISSUE_REQUEST">Internal issue</SelectItem>
          </SelectContent>
        </Select>
        <Button variant="outline" onClick={load}>Refresh</Button>
      </div>
      {error && <p className="mb-3 text-sm text-rose-600">{error}</p>}
      <DataTable columns={columns} data={rows} loading={loading} />
    </EnterprisePage>
  );
};

export default MaterialRequestList;
