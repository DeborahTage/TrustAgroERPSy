import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { financeApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Badge } from '../../components/ui/badge';
import { formatCurrency } from '../../utils';

const ChartOfAccounts = () => {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchAccounts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await financeApi.getAccounts();
      setAccounts(res.data.data || []);
    } catch (e) {
      setError('Failed to load chart of accounts.');
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchAccounts(); }, [fetchAccounts]);

  const stats = useMemo(() => {
    const byType = (type) => accounts.filter((a) => a.accountType === type);
    const sumBalance = (list) => list.reduce((s, a) => s + Number(a.balance || 0), 0);
    return [
      { title: 'Total Accounts', value: accounts.length, description: 'Ledger accounts' },
      { title: 'Assets', value: formatCurrency(sumBalance(byType('ASSET'))), description: `${byType('ASSET').length} accounts` },
      { title: 'Revenue', value: formatCurrency(sumBalance(byType('REVENUE'))), description: `${byType('REVENUE').length} accounts` },
      { title: 'Expenses', value: formatCurrency(sumBalance(byType('EXPENSE'))), description: `${byType('EXPENSE').length} accounts` },
    ];
  }, [accounts]);

  const columns = useMemo(() => [
    { accessorKey: 'accountCode', header: 'Code', cell: ({ row }) => <Badge variant="outline" className="font-mono text-[10px]">{row.original.accountCode}</Badge> },
    { accessorKey: 'accountName', header: 'Account Name', cell: ({ row }) => <span className="font-bold text-slate-800">{row.original.accountName}</span> },
    { accessorKey: 'accountType', header: 'Type', cell: ({ row }) => <Badge className="bg-slate-100 text-slate-700">{row.original.accountType}</Badge> },
    { accessorKey: 'businessUnit', header: 'Unit', cell: ({ row }) => row.original.businessUnit || 'GENERAL' },
    {
      accessorKey: 'balance',
      header: 'Balance',
      cell: ({ row }) => (
        <span className={`font-bold ${Number(row.original.balance) >= 0 ? 'text-emerald-600' : 'text-rose-600'}`}>
          {formatCurrency(row.original.balance)}
        </span>
      ),
    },
  ], []);

  const exportColumns = [
    { header: 'Code', accessorKey: 'accountCode' },
    { header: 'Name', accessorKey: 'accountName' },
    { header: 'Type', accessorKey: 'accountType' },
    { header: 'Unit', accessorKey: 'businessUnit' },
    { header: 'Balance', accessorKey: 'balance' },
  ];

  return (
    <EnterprisePage
      title="Chart of Accounts"
      subtitle="General ledger structure — all balances from posted transactions"
      breadcrumbs={['Finance', 'Chart of Accounts']}
      loading={loading}
      error={error}
      onRetry={fetchAccounts}
      stats={stats}
      onRefresh={fetchAccounts}
      exportData={accounts}
      exportColumns={exportColumns}
      exportFilename="chart-of-accounts"
      exportElementId="coa-table"
      exportTitle="Chart of Accounts"
      empty={!accounts.length ? { title: 'No accounts configured', description: 'Accounts are seeded at startup. Post transactions to update balances.' } : null}
    >
      <div id="coa-table">
        <DataTable columns={columns} data={accounts} searchable pagination pageSize={20} />
      </div>
    </EnterprisePage>
  );
};

export default ChartOfAccounts;
