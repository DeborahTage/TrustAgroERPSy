import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { crmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/ui/tabs';
import { formatCurrency, formatDate } from '../../utils';
import { Users, Target, TrendingUp, FileText, ShoppingCart, ClipboardList, Activity } from 'lucide-react';

const CrmCustomer360 = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await crmApi.getCustomer360(id);
      setData(res.data.data || {});
    } catch {
      setError('Failed to load customer 360.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const client = data?.client || {};
  const stats = data ? [
    { title: 'Opportunities', value: (data.opportunities || []).length, icon: TrendingUp, iconColor: 'text-indigo-600', bgColor: 'bg-indigo-50' },
    { title: 'Quotations', value: (data.quotations || []).length, icon: FileText, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: 'Orders', value: (data.orders || []).length, icon: ShoppingCart, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Open Tasks', value: (data.tasks || []).filter((t) => t.status !== 'COMPLETED' && t.status !== 'CANCELLED').length, icon: ClipboardList, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
  ] : [];

  const oppCols = [
    { accessorKey: 'opportunityNumber', header: 'Opp #', cell: ({ row }) => <button type="button" className="font-mono font-bold text-primary" onClick={() => navigate(`/crm/opportunities/${row.original.id}`)}>{row.original.opportunityNumber}</button> },
    { accessorKey: 'title', header: 'Title' },
    { accessorKey: 'estimatedValue', header: 'Value', cell: ({ row }) => formatCurrency(row.original.estimatedValue) },
    { accessorKey: 'stage', header: 'Stage', cell: ({ row }) => <StatusBadge status={row.original.stage} /> },
  ];
  const quoteCols = [
    { accessorKey: 'quotationNumber', header: 'Quote #', cell: ({ row }) => <button type="button" className="font-mono font-bold text-primary" onClick={() => navigate(`/crm/quotations/${row.original.id}`)}>{row.original.quotationNumber}</button> },
    { accessorKey: 'totalAmount', header: 'Total', cell: ({ row }) => formatCurrency(row.original.totalAmount) },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <StatusBadge status={row.original.status} /> },
  ];
  const orderCols = [
    { accessorKey: 'orderNumber', header: 'Order #', cell: ({ row }) => <button type="button" className="font-mono font-bold text-primary" onClick={() => navigate(`/crm/orders/${row.original.id}`)}>{row.original.orderNumber}</button> },
    { accessorKey: 'totalAmount', header: 'Total', cell: ({ row }) => formatCurrency(row.original.totalAmount) },
    { accessorKey: 'outstandingAmount', header: 'Outstanding', cell: ({ row }) => formatCurrency(row.original.outstandingAmount) },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <StatusBadge status={row.original.status} /> },
  ];
  const taskCols = [
    { accessorKey: 'title', header: 'Task' },
    { accessorKey: 'dueDate', header: 'Due', cell: ({ row }) => formatDate(row.original.dueDate) },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <StatusBadge status={row.original.status} /> },
  ];
  const activityCols = [
    { accessorKey: 'type', header: 'Type' },
    { accessorKey: 'subject', header: 'Subject' },
    { accessorKey: 'activityDate', header: 'Date', cell: ({ row }) => formatDate(row.original.activityDate) },
    { accessorKey: 'performedByName', header: 'By' },
  ];
  const visitCols = [
    { accessorKey: 'visitDate', header: 'Date', cell: ({ row }) => formatDate(row.original.visitDate) },
    { accessorKey: 'purpose', header: 'Purpose' },
    { accessorKey: 'visitedBy', header: 'Visited By' },
    { accessorKey: 'nextFollowUpDate', header: 'Next Follow-up', cell: ({ row }) => formatDate(row.original.nextFollowUpDate) },
  ];

  return (
    <EnterprisePage
      title={client.clientName || 'Customer 360'}
      subtitle={client.customerCode ? `${client.customerCode} · full commercial view` : 'Customer commercial 360'}
      breadcrumbs={['CRM', 'Customers', client.clientName || id]}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      stats={stats}
      actions={
        <>
          <Button variant="outline" size="sm" onClick={() => navigate('/crm/clients')}>All Customers</Button>
          <Button size="sm" variant="outline" onClick={() => navigate(`/crm/clients/${id}/edit`)}>Edit</Button>
          <Button size="sm" onClick={() => navigate('/crm/visits/new', { state: { clientId: Number(id) } })}>Record Visit</Button>
        </>
      }
    >
      {data && (
        <div className="space-y-6">
          <Card className="border-none shadow-sm">
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2"><Users className="h-4 w-4" /> Profile</CardTitle>
              <CardDescription>{client.status} · {client.phone || 'No phone'} · {client.location || 'No location'}</CardDescription>
            </CardHeader>
            <CardContent className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div><p className="text-slate-500">Farm Type</p><p className="font-medium">{client.farmType || '—'}</p></div>
              <div><p className="text-slate-500">Birds</p><p className="font-medium">{client.numberOfBirds != null ? Number(client.numberOfBirds).toLocaleString() : '—'}</p></div>
              <div><p className="text-slate-500">Contact</p><p className="font-medium">{client.contactPerson || '—'}</p></div>
              <div><p className="text-slate-500">Owner</p><p className="font-medium">{client.assignedExtensionWorkerName || client.assignedOwnerName || '—'}</p></div>
            </CardContent>
          </Card>

          <Tabs defaultValue="opportunities">
            <TabsList className="flex flex-wrap h-auto">
              <TabsTrigger value="opportunities"><TrendingUp className="h-3.5 w-3.5 mr-1" /> Opportunities</TabsTrigger>
              <TabsTrigger value="quotations"><FileText className="h-3.5 w-3.5 mr-1" /> Quotations</TabsTrigger>
              <TabsTrigger value="orders"><ShoppingCart className="h-3.5 w-3.5 mr-1" /> Orders</TabsTrigger>
              <TabsTrigger value="tasks"><ClipboardList className="h-3.5 w-3.5 mr-1" /> Tasks</TabsTrigger>
              <TabsTrigger value="activities"><Activity className="h-3.5 w-3.5 mr-1" /> Activities</TabsTrigger>
              <TabsTrigger value="visits"><Target className="h-3.5 w-3.5 mr-1" /> Visits</TabsTrigger>
              {data.invoices && <TabsTrigger value="finance">Finance</TabsTrigger>}
            </TabsList>
            <TabsContent value="opportunities"><DataTable columns={oppCols} data={data.opportunities || []} /></TabsContent>
            <TabsContent value="quotations"><DataTable columns={quoteCols} data={data.quotations || []} /></TabsContent>
            <TabsContent value="orders"><DataTable columns={orderCols} data={data.orders || []} /></TabsContent>
            <TabsContent value="tasks"><DataTable columns={taskCols} data={data.tasks || []} /></TabsContent>
            <TabsContent value="activities"><DataTable columns={activityCols} data={data.activities || []} /></TabsContent>
            <TabsContent value="visits"><DataTable columns={visitCols} data={data.visits || []} /></TabsContent>
            {data.invoices && (
              <TabsContent value="finance">
                <div className="space-y-4">
                  <p className="text-sm text-slate-500">Invoices: {(data.invoices || []).length} · Payments: {(data.payments || []).length}</p>
                  <DataTable
                    columns={[
                      { accessorKey: 'paymentNumber', header: 'Payment #', cell: ({ row }) => <span className="font-mono">{row.original.paymentNumber}</span> },
                      { accessorKey: 'amount', header: 'Amount', cell: ({ row }) => formatCurrency(row.original.amount) },
                      { accessorKey: 'method', header: 'Method' },
                      { accessorKey: 'paymentDate', header: 'Date', cell: ({ row }) => formatDate(row.original.paymentDate) },
                    ]}
                    data={data.payments || []}
                  />
                </div>
              </TabsContent>
            )}
          </Tabs>
        </div>
      )}
    </EnterprisePage>
  );
};

export default CrmCustomer360;
