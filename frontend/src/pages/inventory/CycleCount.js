import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { inventoryApi } from '../../api';
import EnterpriseFormPage from '../../components/layout/EnterpriseFormPage';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { Card, CardContent } from '../../components/ui/card';
import { cn } from '../../lib/utils';
import api from '../../api/axios';

export default function CycleCount() {
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [countedItem, setCountedItem] = useState('');
  const [countedQty, setCountedQty] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);

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

  const startCount = async (e) => {
    e.preventDefault();
    if (!countedItem || !countedQty) return;
    setLoading(true);
    setError('');
    try {
      const res = await api.post('/api/inventory/count', { itemId: parseInt(countedItem, 10), countedQty: parseFloat(countedQty) });
      setResult(res.data);
    } catch {
      setError('Error recording count. Verify item and quantity.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <EnterpriseFormPage
      title="Cycle Count"
      subtitle="Physically count an item and record variances against system stock"
      breadcrumbs={['Inventory', 'Cycle Count']}
      backTo="/inventory"
      loading={fetching}
      error={error}
    >
      <form onSubmit={startCount} className="space-y-6">
        <div className="space-y-2">
          <Label>Item to Count *</Label>
          <Select value={countedItem || 'none'} onValueChange={(v) => setCountedItem(v === 'none' ? '' : v)}>
            <SelectTrigger><SelectValue placeholder="Select item..." /></SelectTrigger>
            <SelectContent>
              <SelectItem value="none">Select item...</SelectItem>
              {items.map((i) => (
                <SelectItem key={i.id} value={String(i.id)}>
                  {i.itemName} (Expected: {i.currentStock} {i.unit})
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-2">
          <Label>Physical Count (Actual Qty) *</Label>
          <Input type="number" step="0.01" value={countedQty} onChange={(e) => setCountedQty(e.target.value)} required placeholder="0.00" />
        </div>
        <Button type="submit" disabled={loading} className="w-full bg-primary hover:bg-primary/90">
          {loading ? 'Submitting...' : 'Record Count'}
        </Button>
      </form>

      {result && (
        <Card className={cn('mt-6 border', result.variance === 0 ? 'bg-emerald-50 border-emerald-200' : result.variance < 0 ? 'bg-rose-50 border-rose-200' : 'bg-amber-50 border-amber-200')}>
          <CardContent className="p-6 space-y-3">
            <h2 className="text-lg font-bold">Count Result</h2>
            <div className="flex justify-between"><span>Expected Qty</span><span className="font-semibold">{result.expectedQty}</span></div>
            <div className="flex justify-between"><span>Counted Qty</span><span className="font-semibold">{result.countedQty}</span></div>
            <div className="flex justify-between border-t pt-2">
              <span className="font-bold">Variance</span>
              <span className={cn('font-bold text-lg', result.variance === 0 ? 'text-emerald-600' : result.variance < 0 ? 'text-rose-600' : 'text-amber-600')}>
                {result.variance >= 0 ? '+' : ''}{result.variance}
              </span>
            </div>
            {result.variance !== 0 && (
              <Button type="button" variant="outline" className="w-full mt-2" onClick={() => navigate('/inventory/stock-in')}>
                Review Stock Adjustment
              </Button>
            )}
          </CardContent>
        </Card>
      )}
    </EnterpriseFormPage>
  );
}
