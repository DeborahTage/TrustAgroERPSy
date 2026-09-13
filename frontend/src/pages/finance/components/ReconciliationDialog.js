import React, { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from '../../../components/ui/dialog';
import { Button } from '../../../components/ui/button';
import { Input } from '../../../components/ui/input';
import { Label } from '../../../components/ui/label';
import { Textarea } from '../../../components/ui/textarea';
import { financeApi } from '../../../api';
import { formatCurrency } from '../../../utils';
import { AlertCircle, CheckCircle2 } from 'lucide-react';

const ReconciliationDialog = ({ open, onOpenChange, systemAmount, onSuccess }) => {
    const [actualAmount, setActualAmount] = useState('');
    const [notes, setNotes] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const difference = actualAmount ? parseFloat(actualAmount) - systemAmount : 0;

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        try {
            await financeApi.reconcileCash({
                systemAmount,
                actualAmount: parseFloat(actualAmount),
                notes
            });
            onSuccess?.();
            onOpenChange(false);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to perform reconciliation');
        } finally {
            setLoading(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[425px]">
                <DialogHeader>
                    <DialogTitle>Cash Reconciliation</DialogTitle>
                    <DialogDescription>
                        Verify the physical cash in drawer against the system balance.
                    </DialogDescription>
                </DialogHeader>
                <form onSubmit={handleSubmit} className="space-y-4 py-4">
                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <Label>System Balance</Label>
                            <div className="p-2 bg-slate-50 border rounded-md font-mono text-sm">
                                {formatCurrency(systemAmount)}
                            </div>
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="actual">Actual Counted</Label>
                            <Input
                                id="actual"
                                type="number"
                                step="0.01"
                                placeholder="0.00"
                                value={actualAmount}
                                onChange={(e) => setActualAmount(e.target.value)}
                                required
                            />
                        </div>
                    </div>

                    <div className={`p-3 rounded-lg flex items-center gap-3 ${difference === 0 ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' :
                            difference > 0 ? 'bg-blue-50 text-blue-700 border border-blue-200' :
                                'bg-rose-50 text-rose-700 border border-rose-200'
                        }`}>
                        {difference === 0 ? <CheckCircle2 className="h-5 w-5" /> : <AlertCircle className="h-5 w-5" />}
                        <div>
                            <p className="text-xs font-bold uppercase tracking-tight">Difference</p>
                            <p className="text-lg font-bold">{formatCurrency(difference)}</p>
                        </div>
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="notes">Discrepancy Notes / Explanation</Label>
                        <Textarea
                            id="notes"
                            placeholder="Explain any differences found during count..."
                            value={notes}
                            onChange={(e) => setNotes(e.target.value)}
                            className="resize-none"
                        />
                    </div>

                    {error && <div className="text-xs text-rose-600 bg-rose-50 p-2 rounded border border-rose-100">{error}</div>}

                    <DialogFooter>
                        <Button type="submit" disabled={loading} className="w-full bg-primary hover:bg-primary/90">
                            {loading ? 'Processing...' : 'Finalize Reconciliation'}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
};

export default ReconciliationDialog;
