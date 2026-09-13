import React, { useCallback, useEffect, useState } from 'react';
import { financeApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Badge } from '../../components/ui/badge';
import { formatCurrency } from '../../utils';
import { Wallet, CreditCard, Users } from 'lucide-react';
import EmptyState from '../../components/shared/EmptyState';

const WalletManagement = () => {
  const [wallets, setWallets] = useState([]);
  const [summary, setSummary] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchWallets = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [walletRes, summaryRes] = await Promise.all([
        financeApi.getWallets(),
        financeApi.getWalletSummary(),
      ]);
      setWallets(walletRes.data.data || []);
      setSummary(summaryRes.data.data || {});
    } catch (e) {
      setError('Failed to load wallet data.');
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchWallets(); }, [fetchWallets]);

  const columns = [
    { accessorKey: 'clientName', header: 'Customer / Client' },
    {
      accessorKey: 'blocked',
      header: 'Status',
      cell: ({ row }) => (
        <Badge className={row.original.blocked ? 'bg-rose-100 text-rose-700' : 'bg-emerald-100 text-emerald-700'}>
          {row.original.blocked ? 'BLOCKED' : 'ACTIVE'}
        </Badge>
      ),
    },
    {
      accessorKey: 'balance',
      header: 'Balance',
      cell: ({ row }) => <span className="font-bold text-emerald-600">{formatCurrency(row.original.balance)}</span>,
    },
    {
      accessorKey: 'creditLimit',
      header: 'Credit Limit',
      cell: ({ row }) => formatCurrency(row.original.creditLimit),
    },
  ];

  const stats = [
    { title: 'Total Deposits', value: formatCurrency(summary.totalDeposits || 0), icon: Wallet, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50', description: 'Customer-held funds' },
    { title: 'Active Wallets', value: summary.activeWallets || 0, icon: Users, iconColor: 'text-primary', bgColor: 'bg-primary/10', description: 'Registered accounts' },
    { title: 'Total Exposure', value: formatCurrency(summary.totalExposure || 0), icon: CreditCard, iconColor: 'text-amber-600', bgColor: 'bg-amber-50', description: 'Aggregated credit limits' },
  ];

  return (
    <EnterprisePage
      title="Digital Wallet System"
      subtitle="Customer deposits, training credits, and credit limits — synced with finance ledger"
      breadcrumbs={['Finance', 'Wallets']}
      loading={loading}
      error={error}
      onRetry={fetchWallets}
      onRefresh={fetchWallets}
      stats={stats}
      exportData={wallets}
      exportColumns={columns}
      exportFilename="customer-wallets"
      empty={!loading && wallets.length === 0 ? {
        title: 'No digital wallets registered',
        description: 'Wallets are created automatically when customers make deposits through finance.',
      } : null}
    >
      {wallets.length > 0 && (
        <DataTable columns={columns} data={wallets} searchable pagination />
      )}
    </EnterprisePage>
  );
};

export default WalletManagement;
