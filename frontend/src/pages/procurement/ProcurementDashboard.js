import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { procurementApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatCurrency } from '../../utils';
import { ClipboardList, FileText, Package, AlertCircle, DollarSign, Truck } from 'lucide-react';

const ProcurementDashboard = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await procurementApi.getDashboard();
      setData(res.data.data || {});
    } catch {
      setError('Failed to load procurement dashboard.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const stats = data ? [
    { title: 'Open Requisitions', value: data.openRequisitions || 0, icon: ClipboardList, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: 'Draft Requisitions', value: data.draftRequisitions || 0, icon: FileText, iconColor: 'text-slate-600', bgColor: 'bg-slate-50' },
    { title: 'Open POs', value: data.openPurchaseOrders || 0, icon: Truck, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Draft GRNs', value: data.draftGrns || 0, icon: Package, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: 'Invoices Awaiting Match', value: data.invoicesAwaitingMatch || 0, icon: AlertCircle, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    { title: 'Approved Unpaid', value: data.approvedUnpaidInvoices || 0, icon: FileText, iconColor: 'text-indigo-600', bgColor: 'bg-indigo-50' },
    { title: 'AP Outstanding', value: formatCurrency(data.apOutstanding || 0), icon: DollarSign, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
  ] : [];

  const quickLinks = [
    ['Requisitions', '/procurement/requisitions'],
    ['Purchase Orders', '/procurement/purchase-orders'],
    ['Goods Receipts', '/procurement/goods-receipts'],
    ['Suppliers', '/procurement/suppliers'],
    ['Supplier Invoices', '/procurement/supplier-invoices'],
    ['Accounts Payable', '/procurement/ap'],
    ['Reports', '/procurement/reports'],
  ];

  return (
    <EnterprisePage
      title="Procurement"
      subtitle="Requisitions, purchase orders, goods receipts, and supplier AP — live ledger"
      breadcrumbs={['Procurement']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      stats={stats}
      actions={
        <>
          {hasPermission(PERMISSIONS.CREATE_PROCUREMENT) && (
            <Button variant="outline" size="sm" onClick={() => navigate('/procurement/requisitions')}>Requisitions</Button>
          )}
          <Button size="sm" onClick={() => navigate('/procurement/purchase-orders')}>Purchase Orders</Button>
        </>
      }
    >
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Quick Links</CardTitle>
            <CardDescription>Navigate procurement workflows</CardDescription>
          </CardHeader>
          <CardContent className="grid grid-cols-2 gap-3">
            {quickLinks.map(([label, path]) => (
              <Button key={path} variant="outline" className="justify-start" onClick={() => navigate(path)}>{label}</Button>
            ))}
          </CardContent>
        </Card>

        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">AP Snapshot</CardTitle>
            <CardDescription>Outstanding supplier payables</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            {!data ? (
              <p className="text-sm text-slate-500 py-6 text-center">No data yet.</p>
            ) : (
              <>
                <div className="flex justify-between border-b py-2"><span>AP outstanding</span><span className="font-mono font-bold">{formatCurrency(data.apOutstanding || 0)}</span></div>
                <div className="flex justify-between border-b py-2"><span>Approved unpaid invoices</span><span className="font-mono">{data.approvedUnpaidInvoices || 0}</span></div>
                <div className="flex justify-between border-b py-2"><span>Awaiting match</span><span className="font-mono">{data.invoicesAwaitingMatch || 0}</span></div>
                <div className="flex justify-between border-b py-2"><span>Open purchase orders</span><span className="font-mono">{data.openPurchaseOrders || 0}</span></div>
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </EnterprisePage>
  );
};

export default ProcurementDashboard;
