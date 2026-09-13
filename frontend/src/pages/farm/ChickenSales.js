import React, { useEffect, useState, useCallback } from 'react';
import { dailyRecordApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { formatDate } from '../../utils';

const ChickenSales = () => {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await dailyRecordApi.getAll();
      const rows = (res.data.data || []).filter((r) => (r.chickensSold || 0) > 0 || (r.culledBirds || 0) > 0);
      setRecords(rows);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const totalSold = records.reduce((s, r) => s + (Number(r.chickensSold) || Number(r.culledBirds) || 0), 0);

  const cols = [
    { accessorKey: 'date', header: 'Date', cell: ({ row }) => formatDate(row.original.date) },
    { accessorKey: 'farmName', header: 'Farm' },
    { accessorKey: 'flockName', header: 'Flock' },
    { accessorKey: 'chickensSold', header: 'Chickens Sold', cell: ({ row }) => row.original.chickensSold ?? row.original.culledBirds ?? 0 },
    { accessorKey: 'averageWeight', header: 'Avg Weight (kg)' },
    { accessorKey: 'symptomsOrRemarks', header: 'Notes' },
  ];

  return (
    <EnterprisePage
      title="Chicken Sales"
      subtitle="Harvest and sales records from farm operations"
      breadcrumbs={['Farm', 'Chicken Sales']}
      loading={loading}
      onRefresh={load}
      stats={[{ title: 'Total Birds Sold', value: totalSold.toLocaleString(), description: 'From daily farm records' }]}
    >
      <DataTable columns={cols} data={records} searchable emptyMessage="No chicken sales recorded yet" />
    </EnterprisePage>
  );
};

export default ChickenSales;
