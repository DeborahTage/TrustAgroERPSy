import React, { useRef, useState } from 'react';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Badge } from '../../components/ui/badge';
import { ScanLine, Package } from 'lucide-react';
import { cn } from '../../lib/utils';
import api from '../../api/axios';

const ACTIONS = ['RECEIVE', 'ISSUE', 'TRANSFER', 'COUNT', 'RETURN'];

export default function ScanStation() {
  const [input, setInput] = useState('');
  const [scanned, setScanned] = useState(null);
  const [action, setAction] = useState('RECEIVE');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');
  const inputRef = useRef();

  const doScan = async () => {
    if (!input.trim()) return;
    setError('');
    setSuccess('');
    setLoading(true);
    try {
      const res = await api.get(`/api/inventory/scan/${input.trim()}`);
      setScanned(res.data);
    } catch {
      setError('Item not found. Check barcode or SKU.');
      setScanned(null);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e) => { if (e.key === 'Enter') doScan(); };

  const submitAction = async (e) => {
    e.preventDefault();
    const form = e.target;
    const payload = {
      barcode: input,
      action,
      quantity: form.quantity.value,
      destination: form.destination?.value || '',
      batchNumber: form.batchNumber?.value || '',
      expiryDate: form.expiryDate?.value || '',
      unitCost: form.unitCost?.value || '',
      storageZone: form.storageZone?.value || '',
    };
    setLoading(true);
    setError('');
    try {
      await api.post('/api/inventory/scan-action', payload);
      setSuccess(`${action} completed successfully`);
      setScanned(null);
      setInput('');
      inputRef.current?.focus();
    } catch (err) {
      setError(err.response?.data?.message || `${action} failed — check stock levels.`);
    } finally {
      setLoading(false);
    }
  };

  const isExpiringSoon = (date) => date && new Date(date) < new Date(Date.now() + 30 * 86400000);

  return (
    <EnterprisePage
      title="Barcode Scan Station"
      subtitle="Scan barcode, RFID tag, or enter SKU for warehouse operations"
      breadcrumbs={['Inventory', 'Scan']}
      stats={scanned ? [
        { title: 'Item', value: scanned.item?.itemName || '—', icon: Package, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
        { title: 'Available Stock', value: `${scanned.item?.availableStock ?? scanned.item?.currentStock ?? 0} ${scanned.item?.unit || ''}`, icon: ScanLine, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
      ] : []}
    >
      <div className="max-w-2xl mx-auto space-y-4">
        <div className="flex gap-2">
          <Input
            ref={inputRef}
            type="text"
            value={input}
            autoFocus
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Scan barcode / RFID / SKU..."
            className="text-lg h-12 text-center"
          />
          <Button onClick={doScan} disabled={loading} className="h-12 px-6 bg-primary hover:bg-primary/90">
            {loading ? '...' : 'Scan'}
          </Button>
        </div>

        {error && <div className="bg-rose-50 border border-rose-200 text-rose-700 p-3 rounded-xl text-sm">{error}</div>}
        {success && <div className="bg-emerald-50 border border-emerald-200 text-emerald-700 p-3 rounded-xl text-sm">{success}</div>}

        {scanned && (
          <Card className="rounded-2xl border-none shadow-sm">
            <CardHeader>
              <CardTitle className="flex items-center justify-between">
                <span>{scanned.item?.itemName}</span>
                <Badge variant="outline">SKU: {scanned.item?.sku}</Badge>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-sm text-slate-600">
                Current Stock: <strong>{scanned.item?.currentStock} {scanned.item?.unit}</strong>
                {' · '}Location: {scanned.item?.storageZone || '—'}
              </p>

              <div>
                <h3 className="font-semibold text-sm mb-2">Available Batches (FEFO)</h3>
                {(scanned.batches || []).map((b) => (
                  <div key={b.id} className={cn('flex justify-between px-3 py-2 rounded-lg mb-1 text-sm', isExpiringSoon(b.expiryDate) ? 'bg-amber-50 border border-amber-200' : 'bg-slate-50')}>
                    <span className="font-mono font-medium">{b.batchNumber}</span>
                    <span>Qty: <strong>{b.quantityRemaining}</strong></span>
                    {b.expiryDate && <span className={cn('text-xs', isExpiringSoon(b.expiryDate) ? 'text-amber-700 font-semibold' : 'text-slate-400')}>Exp: {b.expiryDate}</span>}
                  </div>
                ))}
                {(!scanned.batches || scanned.batches.length === 0) && <p className="text-slate-400 text-sm">No available batches</p>}
              </div>

              <div className="grid grid-cols-5 gap-1">
                {ACTIONS.map((a) => (
                  <Button key={a} type="button" size="sm" variant={action === a ? 'default' : 'outline'} className={action === a ? 'bg-primary' : ''} onClick={() => setAction(a)}>
                    {a}
                  </Button>
                ))}
              </div>

              <form onSubmit={submitAction} className="space-y-3">
                <Input name="quantity" type="number" step="0.01" required placeholder="Quantity *" />
                {action === 'RECEIVE' && (
                  <>
                    <div className="grid grid-cols-2 gap-2">
                      <Input name="batchNumber" placeholder="Batch Number" />
                      <Input name="unitCost" type="number" step="0.01" placeholder="Unit Cost (ETB)" />
                    </div>
                    <div className="grid grid-cols-2 gap-2">
                      <Input name="expiryDate" type="date" />
                      <Input name="storageZone" placeholder="Storage Zone" />
                    </div>
                  </>
                )}
                {action === 'TRANSFER' && <Input name="destination" placeholder="Destination Location" />}
                <Button type="submit" disabled={loading} className="w-full bg-primary hover:bg-primary/90">
                  {loading ? 'Processing...' : `Confirm ${action}`}
                </Button>
              </form>
            </CardContent>
          </Card>
        )}
      </div>
    </EnterprisePage>
  );
}
