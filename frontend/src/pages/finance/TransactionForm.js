import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { financeApi } from '../../api';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import EnterpriseFormPage from '../../components/layout/EnterpriseFormPage';
import { toast } from 'react-toastify';

const TransactionForm = () => {
  const navigate = useNavigate();
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    transactionType: 'INCOME',
    debitAccountCode: '',
    creditAccountCode: '',
    amount: '',
    description: '',
    transactionDate: new Date().toISOString().split('T')[0],
    referenceType: 'MANUAL',
  });

  useEffect(() => {
    financeApi.getAccounts?.()
      .then(res => setAccounts(Array.isArray(res.data.data) ? res.data.data : []))
      .catch(() => setAccounts([]));
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.debitAccountCode || !form.creditAccountCode) {
      setError('Both debit and credit accounts are required.');
      return;
    }
    if (form.debitAccountCode === form.creditAccountCode) {
      setError('Debit and credit accounts must be different.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await financeApi.postTransaction({ ...form, amount: parseFloat(form.amount) });
      toast.success('Transaction posted to the ledger successfully.');
      navigate('/finance');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to post transaction.');
    } finally {
      setLoading(false);
    }
  };

  const set = (key) => (val) => setForm(f => ({ ...f, [key]: val }));

  const assetAccounts = accounts.filter(a => a.accountType === 'ASSET');
  const liabilityAccounts = accounts.filter(a => a.accountType === 'LIABILITY');
  const revenueAccounts = accounts.filter(a => a.accountType === 'REVENUE');
  const expenseAccounts = accounts.filter(a => a.accountType === 'EXPENSE');
  const equityAccounts = accounts.filter(a => a.accountType === 'EQUITY');

  const AccountGroup = ({ label, list }) => list.length === 0 ? null : (
    <>
      <SelectItem value="__group__" disabled className="text-[10px] font-bold uppercase text-slate-400 mt-2">{label}</SelectItem>
      {list.map(a => (
        <SelectItem key={a.id} value={a.accountCode}>
          {a.accountCode} – {a.accountName}
        </SelectItem>
      ))}
    </>
  );

  return (
    <EnterpriseFormPage
      title="Post Manual Journal Entry"
      subtitle="Record a double-entry transaction directly in the ledger"
      breadcrumbs={['Finance', 'Journal Entry']}
      backTo="/finance"
      error={error}
    >
            <form onSubmit={handleSubmit} className="space-y-6">

              {/* Transaction Type */}
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Transaction Type</Label>
                  <Select value={form.transactionType} onValueChange={set('transactionType')}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="INCOME">Income / Revenue</SelectItem>
                      <SelectItem value="EXPENSE">Expense</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>Transaction Date</Label>
                  <Input type="date" value={form.transactionDate}
                    onChange={e => set('transactionDate')(e.target.value)} required />
                </div>
              </div>

              {/* Double-Entry Accounts */}
              <div className="grid grid-cols-2 gap-4 p-4 bg-slate-50 rounded-lg border border-dashed">
                <div className="space-y-2">
                  <Label className="text-emerald-700 font-bold">⬆ Debit Account</Label>
                  <Select value={form.debitAccountCode} onValueChange={set('debitAccountCode')}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select account to debit…" />
                    </SelectTrigger>
                    <SelectContent>
                      <AccountGroup label="Assets" list={assetAccounts} />
                      <AccountGroup label="Expenses" list={expenseAccounts} />
                      <AccountGroup label="Revenue" list={revenueAccounts} />
                      <AccountGroup label="Liabilities" list={liabilityAccounts} />
                      <AccountGroup label="Equity" list={equityAccounts} />
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label className="text-rose-700 font-bold">⬇ Credit Account</Label>
                  <Select value={form.creditAccountCode} onValueChange={set('creditAccountCode')}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select account to credit…" />
                    </SelectTrigger>
                    <SelectContent>
                      <AccountGroup label="Assets" list={assetAccounts} />
                      <AccountGroup label="Expenses" list={expenseAccounts} />
                      <AccountGroup label="Revenue" list={revenueAccounts} />
                      <AccountGroup label="Liabilities" list={liabilityAccounts} />
                      <AccountGroup label="Equity" list={equityAccounts} />
                    </SelectContent>
                  </Select>
                </div>
              </div>

              {/* Amount */}
              <div className="space-y-2">
                <Label>Amount (ETB)</Label>
                <Input type="number" min="0.01" step="0.01" placeholder="0.00"
                  value={form.amount} onChange={e => set('amount')(e.target.value)}
                  required className="text-lg font-bold" />
              </div>

              {/* Description */}
              <div className="space-y-2">
                <Label>Description / Narration</Label>
                <Textarea
                  placeholder="Brief narration of this journal entry…"
                  value={form.description}
                  onChange={e => set('description')(e.target.value)}
                  rows={3}
                  className="resize-none"
                />
              </div>

              {/* Reference */}
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Reference Type</Label>
                  <Select value={form.referenceType} onValueChange={set('referenceType')}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="MANUAL">Manual Entry</SelectItem>
                      <SelectItem value="ADJUSTMENT">Adjustment</SelectItem>
                      <SelectItem value="OPENING">Opening Balance</SelectItem>
                      <SelectItem value="TRANSFER">Account Transfer</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>

              {error && (
                <div className="text-xs text-rose-700 bg-rose-50 border border-rose-100 rounded-lg p-3">
                  {error}
                </div>
              )}

              {/* Actions */}
              <div className="flex gap-3 pt-2">
                <Button type="submit" disabled={loading} className="bg-primary hover:bg-primary/90 flex-1">
                  {loading ? 'Posting…' : 'Post to Ledger'}
                </Button>
                <Button type="button" variant="outline" onClick={() => navigate('/finance')}>
                  Cancel
                </Button>
              </div>
            </form>
    </EnterpriseFormPage>
  );
};

export default TransactionForm;
