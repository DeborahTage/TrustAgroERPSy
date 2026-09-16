import React, { useCallback, useEffect, useState } from 'react';
import { procurementApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { formatCurrency } from '../../utils';
import { ClipboardList, Truck, Package, DollarSign, FileText, AlertCircle } from 'lucide-react';

const ProcurementReports = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await procurementApi.getReports();
      setData(res.data.data || {});
    } catch {
      setError('Failed to load procurement reports.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const dash = data?.dashboard || {};
  const aging = data?.apAging || {};
  const invoiceCounts = data?.invoiceStatusCounts || {};

  const stats = data ? [
    { title: 'Open Requisitions', value: dash.openRequisitions || 0, icon: ClipboardList, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: 'Open POs', value: dash.openPurchaseOrders || 0, icon: Truck, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Draft GRNs', value: dash.draftGrns || 0, icon: Package, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: 'Awaiting Match', value: dash.invoicesAwaitingMatch || 0, icon: FileText, iconColor: 'text-indigo-600', bgColor: 'bg-indigo-50' },
    { title: 'AP Outstanding', value: formatCurrency(dash.apOutstanding || 0), icon: DollarSign, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: 'AP Total (Aging)', value: formatCurrency(aging.total || 0), icon: AlertCircle, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
  ] : [];

  return (
    <EnterprisePage
      title="Procurement Reports"
      subtitle="Dashboard aggregates, invoice status counts, and AP aging"
      breadcrumbs={['Procurement', 'Reports']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      stats={stats}
    >
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">Invoice Status Counts</CardTitle>
            <CardDescription>Live counts from supplier invoices</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            {Object.keys(invoiceCounts).length === 0 ? (
              <p className="text-slate-500 text-center py-6">No invoice data.</p>
            ) : (
              Object.entries(invoiceCounts).map(([status, count]) => (
                <div key={status} className="flex justify-between border-b py-2">
                  <span>{status}</span>
                  <span className="font-mono font-bold">{count}</span>
                </div>
              ))
            )}
          </CardContent>
        </Card>

        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg">AP Aging Summary</CardTitle>
            <CardDescription>Outstanding by days past due</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            {!aging ? (
              <p className="text-slate-500 text-center py-6">No aging data.</p>
            ) : (
              <>
                <div className="flex justify-between border-b py-2"><span>Current</span><span className="font-mono">{formatCurrency(aging.current)}</span></div>
                <div className="flex justify-between border-b py-2"><span>1–30 days</span><span className="font-mono">{formatCurrency(aging.days1to30)}</span></div>
                <div className="flex justify-between border-b py-2"><span>31–60 days</span><span className="font-mono">{formatCurrency(aging.days31to60)}</span></div>
                <div className="flex justify-between border-b py-2"><span>61–90 days</span><span className="font-mono">{formatCurrency(aging.days61to90)}</span></div>
                <div className="flex justify-between border-b py-2"><span>90+ days</span><span className="font-mono">{formatCurrency(aging.days90Plus)}</span></div>
                <div className="flex justify-between border-b py-2 font-semibold"><span>Total</span><span className="font-mono">{formatCurrency(aging.total)}</span></div>
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </EnterprisePage>
  );
};

export default ProcurementReports;
