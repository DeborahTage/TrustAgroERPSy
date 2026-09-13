import React, { useCallback, useEffect, useState } from 'react';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { DollarSign, Package, AlertTriangle, Timer, Download, TrendingUp } from 'lucide-react';
import { toast } from 'react-toastify';
import api from '../../api/axios';

export default function InventoryReports() {
  const [summary, setSummary] = useState(null);
  const [turnover, setTurnover] = useState([]);
  const [lowStock, setLowStock] = useState([]);
  const [expiring, setExpiring] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchReports = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [summaryRes, turnoverRes, lowStockRes, expiringRes] = await Promise.all([
        api.get('/api/inventory/reports/summary'),
        api.get('/api/inventory/reports/turnover'),
        api.get('/api/inventory/low-stock'),
        api.get('/api/inventory/expiring'),
      ]);
      setSummary(summaryRes.data || {});
      setTurnover(turnoverRes.data || []);
      setLowStock(lowStockRes.data?.content || lowStockRes.data || []);
      setExpiring(expiringRes.data || []);
    } catch {
      setError('Failed to load report data.');
      toast.error('Failed to load report data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchReports(); }, [fetchReports]);

  const handleDownloadValuation = async () => {
    try {
      const res = await api.get('/api/inventory/reports/valuation/csv', { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'stock_valuation.csv');
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch {
      toast.error('Failed to download report');
    }
  };

  const stats = [
    { title: 'Total Valuation', value: `${summary?.totalValue?.toLocaleString() || '0'} ETB`, icon: DollarSign, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50', description: 'Current asset value' },
    { title: 'Active SKUs', value: summary?.totalSkus || '0', icon: Package, iconColor: 'text-blue-600', bgColor: 'bg-blue-50', description: 'Items tracked' },
    { title: 'Low Stock Alerts', value: summary?.lowStockCount || '0', icon: AlertTriangle, iconColor: 'text-amber-600', bgColor: 'bg-amber-50', description: 'Below reorder point' },
    { title: 'Expiring Soon', value: summary?.expiringCount || '0', icon: Timer, iconColor: 'text-rose-600', bgColor: 'bg-rose-50', description: 'Next 30 days' },
  ];

  return (
    <EnterprisePage
      title="Inventory Intelligence"
      subtitle="Comprehensive stock analytics, valuation, and forecasting"
      breadcrumbs={['Inventory', 'Reports']}
      loading={loading}
      error={error}
      onRetry={fetchReports}
      onRefresh={fetchReports}
      stats={stats}
      actions={
        <Button variant="outline" onClick={handleDownloadValuation}>
          <Download className="h-4 w-4 mr-2" /> Export Valuation
        </Button>
      }
    >
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-2 rounded-2xl border-none shadow-sm">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingUp className="h-5 w-5 text-primary" />
              Stock Turnover Trend (Last 6 Months)
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={320}>
              <BarChart data={turnover}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="month" />
                <YAxis />
                <Tooltip />
                <Bar dataKey="turnover_rate" fill="#4f46e5" radius={[6, 6, 0, 0]} name="Movement Qty" />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card className="rounded-2xl border-none shadow-sm">
          <CardHeader><CardTitle>Critical Alerts</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            {lowStock.slice(0, 5).map((item) => (
              <div key={item.id} className="flex justify-between items-center p-3 bg-amber-50 rounded-xl border border-amber-100">
                <div>
                  <p className="font-semibold text-sm">{item.itemName}</p>
                  <p className="text-xs text-slate-500">Stock: {item.currentStock}</p>
                </div>
                <Badge className="bg-amber-100 text-amber-700">LOW</Badge>
              </div>
            ))}
            {lowStock.length === 0 && (
              <p className="text-sm text-emerald-600 text-center py-4">All stock levels healthy</p>
            )}
            {expiring.slice(0, 5).map((batch) => (
              <div key={batch.id} className="flex justify-between items-center p-3 bg-rose-50 rounded-xl border border-rose-100">
                <div>
                  <p className="font-semibold text-sm">{batch.batchNumber}</p>
                  <p className="text-xs text-slate-500">Exp: {batch.expiryDate}</p>
                </div>
                <Badge className="bg-rose-100 text-rose-700">EXPIRY</Badge>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    </EnterprisePage>
  );
}
