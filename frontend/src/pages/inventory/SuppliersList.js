import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { supplierApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';

/**
 * Legacy inventory suppliers list — now loads real Supplier master data.
 * Prefer /procurement/suppliers for the full procurement workflow.
 */
const SuppliersList = () => {
  const navigate = useNavigate();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await supplierApi.getAll();
      setRows(res.data?.data || res.data || []);
    } catch {
      setError('Failed to load suppliers.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const cols = [
    { accessorKey: 'supplierCode', header: 'Code', cell: ({ row }) => <span className="font-mono font-bold">{row.original.supplierCode || '—'}</span> },
    { accessorKey: 'name', header: 'Name' },
    { accessorKey: 'contactPerson', header: 'Contact' },
    { accessorKey: 'phone', header: 'Phone' },
    { accessorKey: 'paymentTerms', header: 'Terms' },
    {
      accessorKey: 'active',
      header: 'Status',
      cell: ({ row }) => (
        <Badge variant={row.original.active === false ? 'secondary' : 'default'}>
          {row.original.active === false ? 'Inactive' : 'Active'}
        </Badge>
      ),
    },
  ];

  return (
    <EnterprisePage
      title="Suppliers"
      subtitle="Canonical supplier master (inventory)"
      breadcrumbs={['Inventory', 'Suppliers']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={(
        <Button size="sm" variant="outline" onClick={() => navigate('/procurement/suppliers')}>
          Open Procurement Suppliers
        </Button>
      )}
    >
      <DataTable columns={cols} data={rows} />
    </EnterprisePage>
  );
};

export default SuppliersList;
