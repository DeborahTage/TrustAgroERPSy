import React, { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from '../../../components/ui/dialog';
import { Button } from '../../../components/ui/button';
import { Input } from '../../../components/ui/input';
import { Label } from '../../../components/ui/label';
import { financeApi } from '../../../api';
import { ShoppingBag } from 'lucide-react';

const POSInvoiceDialog = ({ open, onOpenChange, onSuccess }) => {
    const [amount, setAmount] = useState('');
    const [customerName, setCustomerName] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        try {
            await financeApi.createPOSInvoice({
                amount: parseFloat(amount),
                customerName
            });
            onSuccess?.();
            onOpenChange(false);
            setAmount('');
            setCustomerName('');
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to create POS invoice');
        } finally {
            setLoading(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[425px]">
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2">
                        <ShoppingBag className="h-5 w-5 text-primary" />
                        Quick POS Invoice
                    </DialogTitle>
                    <DialogDescription>
                        Generate a quick cash sale invoice and record it in the ledger.
                    </DialogDescription>
                </DialogHeader>
                <form onSubmit={handleSubmit} className="space-y-4 py-4">
                    <div className="space-y-2">
                        <Label htmlFor="customer">Customer Name</Label>
                        <Input
                            id="customer"
                            placeholder="Walk-in Customer"
                            value={customerName}
                            onChange={(e) => setCustomerName(e.target.value)}
                            required
                        />
                    </div>
                    <div className="space-y-2">
                        <Label htmlFor="amount">Total Amount (ETB)</Label>
                        <Input
                            id="amount"
                            type="number"
                            step="0.01"
                            placeholder="0.00"
                            value={amount}
                            onChange={(e) => setAmount(e.target.value)}
                            required
                            className="text-lg font-bold"
                        />
                    </div>

                    {error && <div className="text-xs text-rose-600 bg-rose-50 p-2 rounded border border-rose-100">{error}</div>}

                    <DialogFooter>
                        <Button type="submit" disabled={loading} className="w-full bg-primary hover:bg-primary/90">
                            {loading ? 'Processing...' : 'Charge & Print Receipt'}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
};

export default POSInvoiceDialog;
