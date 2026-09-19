import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { materialRequestApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { formatDate } from '../../utils';

const FinanceMaterialApprovalQueue = () => {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const [reasons, setReasons] = useState({});

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await materialRequestApi.list({ queue: 'FINANCE' });
      setRows(res.data?.data || []);
    } catch (e) {
      setError(e?.response?.data?.message || 'Failed to load finance queue');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const run = async (id, fn) => {
    setBusyId(id);
    try {
      await fn();
      await load();
    } catch (e) {
      setError(e?.response?.data?.message || 'Action failed');
    } finally {
      setBusyId(null);
    }
  };

  const columns = [
    {
      header: 'Number',
      cell: ({ row }) => <Link className="text-sky-700 underline" to={`/materials/requests/${row.original.id}`}>{row.original.requestNumber}</Link>,
    },
    { header: 'Type', accessorKey: 'type' },
    { header: 'Requester', accessorKey: 'requesterName' },
    { header: 'Est. total', accessorKey: 'estimatedTotal' },
    { header: 'Date', cell: ({ row }) => formatDate(row.original.requestDate) },
    {
      header: 'Actions',
      cell: ({ row }) => (
        <div className="flex flex-wrap items-center gap-2">
          <Button size="sm" disabled={busyId === row.original.id}
            onClick={() => run(row.original.id, () => materialRequestApi.financeApprove(row.original.id, {}))}>
            Approve
          </Button>
          <Input className="w-40" placeholder="Reason"
            value={reasons[row.original.id] || ''}
            onChange={(e) => setReasons({ ...reasons, [row.original.id]: e.target.value })} />
          <Button size="sm" variant="destructive" disabled={busyId === row.original.id}
            onClick={() => run(row.original.id, () => materialRequestApi.financeReject(row.original.id, { reason: reasons[row.original.id] }))}>
            Reject
          </Button>
        </div>
      ),
    },
  ];

  return (
    <EnterprisePage title="Finance Material Approvals" subtitle="Pending material request reviews">
      {error && <p className="mb-3 text-sm text-rose-600">{error}</p>}
      <Button className="mb-3" variant="outline" onClick={load}>Refresh</Button>
      <DataTable columns={columns} data={rows} loading={loading} />
    </EnterprisePage>
  );
};

export default FinanceMaterialApprovalQueue;
