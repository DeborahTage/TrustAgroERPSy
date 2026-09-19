import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { materialRequestApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { formatDate } from '../../utils';

const StoreFulfillmentQueue = () => {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [storeRes, purchaseRes] = await Promise.all([
        materialRequestApi.list({ queue: 'STORE' }),
        materialRequestApi.list({ queue: 'PURCHASE' }),
      ]);
      const store = (storeRes.data?.data || []).map((r) => ({ ...r, queueKind: 'ISSUE' }));
      const purchase = (purchaseRes.data?.data || []).map((r) => ({ ...r, queueKind: 'RECEIPT' }));
      setRows([...store, ...purchase]);
    } catch (e) {
      setError(e?.response?.data?.message || 'Failed to load store queue');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const columns = [
    {
      header: 'Number',
      cell: ({ row }) => <Link className="text-sky-700 underline" to={`/materials/requests/${row.original.id}`}>{row.original.requestNumber}</Link>,
    },
    { header: 'Queue', cell: ({ row }) => <Badge>{row.original.queueKind}</Badge> },
    { header: 'Type', accessorKey: 'type' },
    { header: 'Status', accessorKey: 'status' },
    { header: 'Farm', accessorKey: 'farmName' },
    { header: 'Date', cell: ({ row }) => formatDate(row.original.requestDate) },
  ];

  return (
    <EnterprisePage title="Store Fulfillment" subtitle="Internal issues and purchase receipts">
      {error && <p className="mb-3 text-sm text-rose-600">{error}</p>}
      <Button className="mb-3" variant="outline" onClick={load}>Refresh</Button>
      <DataTable columns={columns} data={rows} loading={loading} />
    </EnterprisePage>
  );
};

export default StoreFulfillmentQueue;
