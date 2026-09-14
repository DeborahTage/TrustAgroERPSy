import React, { useEffect, useState } from 'react';
import { financeApi } from '../../api';
import EnterpriseFormPage from '../../components/layout/EnterpriseFormPage';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { formatCurrency } from '../../utils';
import { Calculator, CheckCircle2, AlertCircle } from 'lucide-react';
import { toast } from 'react-toastify';

const CashReconciliation = () => {
  const [expectedCash, setExpectedCash] = useState(null);
  const [actualAmount, setActualAmount] = useState('');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [loadingExpected, setLoadingExpected] = useState(true);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  useEffect(() => {
    const load = async () => {
      setLoadingExpected(true);
      try {
        const res = await financeApi.getExpectedCash();
        setExpectedCash(Number(res.data?.data?.expectedCash ?? 0));
      } catch {
        setError('Could not load expected cash from ledger account 1000.');
      } finally {
        setLoadingExpected(false);
      }
    };
    load();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      const res = await financeApi.reconcileCash({
        systemAmount: expectedCash ?? 0,
        actualAmount: parseFloat(actualAmount),
        notes,
      });
      setResult(res.data.data);
      toast.success('Cash reconciliation recorded');
      const snap = await financeApi.getExpectedCash();
      setExpectedCash(Number(snap.data?.data?.expectedCash ?? 0));
    } catch (err) {
      setError(err.response?.data?.message || 'Reconciliation failed.');
    } finally {
      setSubmitting(false);
    }
  };

  const variance = (parseFloat(actualAmount) || 0) - (Number(expectedCash) || 0);

  return (
    <EnterpriseFormPage
      title="Daily Cash Reconciliation"
      subtitle="Expected cash is Chart of Accounts account 1000 — not a typed dashboard figure"
      breadcrumbs={['Finance', 'Reconciliation']}
      backTo="/finance"
      error={error}
    >
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Expected Cash (Ledger 1000)</Label>
              <div className="p-2 bg-slate-50 border rounded-md font-mono text-sm min-h-[40px] flex items-center">
                {loadingExpected ? 'Loading…' : formatCurrency(expectedCash ?? 0)}
              </div>
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
          <Button type="submit" disabled={submitting || loadingExpected} className="w-full bg-primary hover:bg-primary/90">
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
              <div className="flex justify-between"><span>Expected</span><span className="font-mono">{formatCurrency(result.expectedCash)}</span></div>
              <div className="flex justify-between"><span>Counted</span><span className="font-mono">{formatCurrency(result.actualCash)}</span></div>
              <div className="flex justify-between"><span>Variance</span><span className="font-mono">{formatCurrency(result.variance)}</span></div>
              <Badge variant="outline">{result.status}</Badge>
              {Number(result.variance) !== 0 && (
                <p className="text-xs text-amber-800 flex items-center gap-1 pt-2">
                  <AlertCircle className="h-3 w-3" /> Variance posted to the finance ledger against this reconciliation.
                </p>
              )}
            </CardContent>
          </Card>
        )}
      </div>
    </EnterpriseFormPage>
  );
};

export default CashReconciliation;
