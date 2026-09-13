import React, { useState } from 'react';
import { financeApi } from '../../api';
import EnterpriseFormPage from '../../components/layout/EnterpriseFormPage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { formatCurrency } from '../../utils';
import { Calculator, CheckCircle2, AlertCircle } from 'lucide-react';
import { toast } from 'react-toastify';

const CashReconciliation = () => {
  const [systemAmount, setSystemAmount] = useState('');
  const [actualAmount, setActualAmount] = useState('');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      const res = await financeApi.reconcileCash({
        systemAmount: parseFloat(systemAmount),
        actualAmount: parseFloat(actualAmount),
        notes,
      });
      setResult(res.data.data);
      toast.success('Cash reconciliation recorded');
    } catch (err) {
      setError(err.response?.data?.message || 'Reconciliation failed.');
    } finally {
      setSubmitting(false);
    }
  };

  const variance = (parseFloat(actualAmount) || 0) - (parseFloat(systemAmount) || 0);

  return (
    <EnterpriseFormPage
      title="Daily Cash Reconciliation"
      subtitle="Compare system expected cash vs physical count at shift end"
      breadcrumbs={['Finance', 'Reconciliation']}
      backTo="/finance"
      error={error}
    >
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>System Amount (Ledger)</Label>
              <Input type="number" step="0.01" value={systemAmount} onChange={(e) => setSystemAmount(e.target.value)} required />
            </div>
            <div className="space-y-2">
              <Label>Actual Cash Counted</Label>
              <Input type="number" step="0.01" value={actualAmount} onChange={(e) => setActualAmount(e.target.value)} required />
            </div>
          </div>
          <div className="space-y-2">
            <Label>Notes</Label>
            <Textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={3} />
          </div>
          <div className={`p-4 rounded-xl flex items-center gap-3 ${variance === 0 ? 'bg-emerald-50 text-emerald-800' : 'bg-amber-50 text-amber-800'}`}>
            <Calculator className="h-5 w-5" />
            <span className="font-semibold">Variance: {formatCurrency(variance)}</span>
          </div>
          <Button type="submit" disabled={submitting} className="w-full bg-primary hover:bg-primary/90">
            {submitting ? 'Submitting...' : 'Submit Reconciliation'}
          </Button>
        </form>

        {result && (
          <Card className="border-emerald-200 bg-emerald-50">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-emerald-800">
                <CheckCircle2 className="h-5 w-5" /> Reconciliation Saved
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              <div className="flex justify-between"><span>Expected</span><span className="font-semibold">{formatCurrency(result.expectedCash)}</span></div>
              <div className="flex justify-between"><span>Actual</span><span className="font-semibold">{formatCurrency(result.actualCash)}</span></div>
              <div className="flex justify-between border-t pt-2">
                <span>Variance</span>
                <Badge className={result.variance === 0 ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'}>
                  {formatCurrency(result.variance || result.difference)}
                </Badge>
              </div>
            </CardContent>
          </Card>
        )}

        {!result && (
          <Card className="border-none shadow-sm">
            <CardHeader>
              <CardTitle>Audit Guidelines</CardTitle>
              <CardDescription>End-of-day cash control checklist</CardDescription>
            </CardHeader>
            <CardContent className="text-sm text-slate-600 space-y-2">
              <p className="flex items-start gap-2"><AlertCircle className="h-4 w-4 mt-0.5 text-amber-500" /> Count all cash drawers before entering amounts.</p>
              <p className="flex items-start gap-2"><AlertCircle className="h-4 w-4 mt-0.5 text-amber-500" /> System amount should match POS and wallet ledger totals.</p>
              <p className="flex items-start gap-2"><AlertCircle className="h-4 w-4 mt-0.5 text-amber-500" /> Document any variance in the notes field.</p>
            </CardContent>
          </Card>
        )}
      </div>
    </EnterpriseFormPage>
  );
};

export default CashReconciliation;
