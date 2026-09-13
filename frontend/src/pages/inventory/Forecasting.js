import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { inventoryApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';
import { TrendingUp, TrendingDown, Package, ShoppingCart } from 'lucide-react';
import { cn } from '../../lib/utils';
import api from '../../api/axios';

export default function Forecasting() {
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [forecast, setForecast] = useState(null);
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const [error, setError] = useState(null);

  const loadItems = useCallback(async () => {
    setFetching(true);
    try {
      const r = await inventoryApi.getItems();
      setItems(r.data?.data || r.data || []);
    } catch {
      setError('Failed to load inventory items.');
    } finally {
      setFetching(false);
    }
  }, []);

  useEffect(() => { loadItems(); }, [loadItems]);

  const loadForecast = async (id) => {
    if (!id) { setSelectedId(''); setForecast(null); return; }
    setSelectedId(id);
    setLoading(true);
    setForecast(null);
    try {
      const res = await api.get(`/api/inventory/forecast/${id}`);
      setForecast(res.data);
    } catch {
      setForecast(null);
    } finally {
      setLoading(false);
    }
  };

  const chartData = forecast ? [
    { month: 'Now', qty: forecast.predictedDemand || 0 },
    { month: '+1mo', qty: (forecast.predictedDemand || 0) + (forecast.trend || 0) },
    { month: '+2mo', qty: (forecast.predictedDemand || 0) + (forecast.trend || 0) * 2 },
    { month: '+3mo', qty: (forecast.predictedDemand || 0) + (forecast.trend || 0) * 3 },
  ] : [];

  const stats = forecast ? [
    { title: 'Predicted Demand', value: forecast.predictedDemand?.toFixed(1) || '—', icon: Package, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Trend', value: `${forecast.trend >= 0 ? '+' : ''}${(forecast.trend || 0).toFixed(2)}`, icon: forecast.trend >= 0 ? TrendingUp : TrendingDown, iconColor: forecast.trend >= 0 ? 'text-emerald-600' : 'text-rose-600', bgColor: forecast.trend >= 0 ? 'bg-emerald-50' : 'bg-rose-50' },
    { title: 'Recommended Order', value: forecast.recommendedOrder?.toFixed(1) || '—', icon: ShoppingCart, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: 'Confidence', value: `${((forecast.confidenceScore || 0) * 100).toFixed(0)}%`, icon: TrendingUp, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
  ] : [];

  return (
    <EnterprisePage
      title="Demand Forecasting"
      subtitle="Predict consumption and recommended reorder quantities from movement history"
      breadcrumbs={['Inventory', 'Forecasting']}
      loading={fetching || loading}
      error={error}
      onRetry={loadItems}
      stats={stats}
    >
      <div className="mb-6 max-w-md">
        <Select value={selectedId || 'none'} onValueChange={(v) => loadForecast(v === 'none' ? '' : v)}>
          <SelectTrigger><SelectValue placeholder="Select an inventory item" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="none">Select item...</SelectItem>
            {items.map((i) => <SelectItem key={i.id} value={String(i.id)}>{i.itemName} ({i.sku})</SelectItem>)}
          </SelectContent>
        </Select>
      </div>

      {forecast && !loading && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card className="rounded-2xl border-none shadow-sm">
            <CardHeader><CardTitle>Forecast Summary</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              {[
                { label: 'Predicted Demand (next month)', value: forecast.predictedDemand?.toFixed(2) },
                { label: 'Trend', value: `${forecast.trend >= 0 ? '↑ +' : '↓ '}${Math.abs(forecast.trend || 0).toFixed(2)}`, color: forecast.trend >= 0 ? 'text-emerald-600' : 'text-rose-600' },
                { label: 'Confidence Score', value: `${((forecast.confidenceScore || 0) * 100).toFixed(0)}%` },
                { label: 'Recommended Order Qty', value: forecast.recommendedOrder?.toFixed(2), color: 'text-primary font-bold' },
              ].map(({ label, value, color }) => (
                <div key={label} className="flex justify-between py-2 border-b">
                  <span className="text-slate-600 text-sm">{label}</span>
                  <span className={cn('font-semibold', color)}>{value}</span>
                </div>
              ))}
              <Button className="w-full mt-4 bg-primary hover:bg-primary/90" onClick={() => navigate('/inventory/po')}>
                Create Purchase Order
              </Button>
            </CardContent>
          </Card>

          <Card className="rounded-2xl border-none shadow-sm">
            <CardHeader><CardTitle>Projected Demand</CardTitle></CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={220}>
                <LineChart data={chartData}>
                  <XAxis dataKey="month" />
                  <YAxis />
                  <Tooltip />
                  <Line type="monotone" dataKey="qty" stroke="#4f46e5" strokeWidth={2} dot />
                </LineChart>
              </ResponsiveContainer>
              <p className="text-xs text-slate-500 mt-2 text-center">Based on 90-day movement history</p>
            </CardContent>
          </Card>
        </div>
      )}

      {!forecast && !loading && selectedId && (
        <Card className="bg-amber-50 border-amber-200 rounded-2xl">
          <CardContent className="p-6 text-amber-800 text-center">
            No historical movement data found. Record stock movements to enable predictions.
          </CardContent>
        </Card>
      )}
    </EnterprisePage>
  );
}
