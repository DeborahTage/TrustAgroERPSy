import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { crmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import { Button } from '../../components/ui/button';
import { Plus } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

const CrmLeads = () => {
  const navigate = useNavigate();
  const { canPerformAction } = useAuth();
  const [leads, setLeads] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await crmApi.getLeads();
      setLeads(res.data.data || []);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const cols = [
    { accessorKey: 'clientName', header: 'Lead Name' },
    { accessorKey: 'phone', header: 'Phone' },
    { accessorKey: 'location', header: 'Location' },
    { accessorKey: 'farmType', header: 'Farm Type' },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <StatusBadge status={row.original.status} /> },
    {
      id: 'actions',
      header: 'Actions',
      cell: ({ row }) => (
        <Button size="sm" variant="outline" onClick={() => navigate(`/crm/clients/${row.original.id}/edit`)}>Manage</Button>
      ),
    },
  ];

  return (
    <EnterprisePage
      title="Leads"
      subtitle="Prospective customers from marketing and outreach"
      breadcrumbs={['CRM', 'Leads']}
      loading={loading}
      onRefresh={load}
      actions={canPerformAction('create', 'customers') && (
        <Button onClick={() => navigate('/crm/clients/new')}><Plus className="h-4 w-4 mr-1" />New Lead</Button>
      )}
    >
      <DataTable columns={cols} data={leads} searchable />
    </EnterprisePage>
  );
};

export default CrmLeads;
