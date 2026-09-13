import React, { useCallback, useEffect, useState } from 'react';
import { pharmacyApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Badge } from '../../components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../../components/ui/table';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { formatCurrency } from '../../utils';
import { Users, UserPlus, Search, Phone, MapPin, History, ExternalLink } from 'lucide-react';

const PharmacyCustomers = () => {
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  const fetchCustomers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await pharmacyApi.getCustomers();
      setCustomers(res.data?.data || res.data || []);
    } catch {
      setError('Failed to load customers.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchCustomers(); }, [fetchCustomers]);

  const filtered = customers.filter((c) =>
    c.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.phone?.includes(searchQuery)
  );

  const getCustomerTypeBadge = (type) => {
    switch (type) {
      case 'WHOLESALE': return <Badge className="bg-blue-100 text-blue-700">Wholesale</Badge>;
      case 'PARTNER_FARM': return <Badge className="bg-primary/10 text-primary">Partner Farm</Badge>;
      default: return <Badge variant="secondary">Retail</Badge>;
    }
  };

  const stats = [
    { title: 'Total Customers', value: customers.length, icon: Users, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'With Balance', value: customers.filter((c) => (c.outstandingBalance || 0) > 0).length, icon: History, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
  ];

  return (
    <EnterprisePage
      title="Pharmacy Customers"
      subtitle="Commercial clients, credit accounts, and purchase history"
      breadcrumbs={['Pharmacy', 'Customers']}
      loading={loading}
      error={error}
      onRetry={fetchCustomers}
      onRefresh={fetchCustomers}
      stats={stats}
      actions={
        <Button className="bg-primary hover:bg-primary/90">
          <UserPlus className="h-4 w-4 mr-2" /> Add Customer
        </Button>
      }
      empty={!filtered.length && !loading ? { title: 'No customers', description: 'Register pharmacy customers to track sales and credit.' } : null}
    >
      <div className="relative max-w-sm mb-4">
        <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
        <Input placeholder="Search by name or phone..." className="pl-10" value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} />
      </div>

      <Card className="rounded-2xl border-none shadow-sm">
        <CardHeader>
          <CardTitle>Commercial Clients</CardTitle>
          <CardDescription>Registered farmers and corporate partners</CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Code</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Contact</TableHead>
                <TableHead>Balance</TableHead>
                <TableHead>Credit Limit</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((customer) => (
                <TableRow key={customer.id}>
                  <TableCell className="font-mono text-xs">{customer.customerCode}</TableCell>
                  <TableCell>
                    <div className="font-semibold">{customer.name}</div>
                    <div className="text-xs text-slate-500">{customer.farmName || 'Private Client'}</div>
                  </TableCell>
                  <TableCell>{getCustomerTypeBadge(customer.customerType)}</TableCell>
                  <TableCell className="text-xs">
                    {customer.phone && <span className="flex items-center gap-1"><Phone className="h-3 w-3" />{customer.phone}</span>}
                    {customer.address && <span className="flex items-center gap-1 text-slate-500"><MapPin className="h-3 w-3" />{customer.address.substring(0, 24)}</span>}
                  </TableCell>
                  <TableCell className={customer.outstandingBalance > 0 ? 'text-rose-600 font-semibold' : 'text-slate-400'}>
                    {formatCurrency(customer.outstandingBalance || 0)}
                  </TableCell>
                  <TableCell>{formatCurrency(customer.creditLimit || 0)}</TableCell>
                  <TableCell className="text-right">
                    <Button variant="ghost" size="sm"><ExternalLink className="h-4 w-4" /></Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </EnterprisePage>
  );
};

export default PharmacyCustomers;
