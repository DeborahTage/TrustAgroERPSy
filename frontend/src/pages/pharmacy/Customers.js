import React, { useCallback, useEffect, useState } from 'react';
import { pharmacyApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Badge } from '../../components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../../components/ui/table';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '../../components/ui/dialog';
import { formatCurrency } from '../../utils';
import { Users, UserPlus, Search, Phone, MapPin, History, ExternalLink, Building2, AlertCircle } from 'lucide-react';

const emptyCustomer = {
  name: '',
  phone: '',
  email: '',
  farmName: '',
  farmType: 'Poultry',
  customerType: 'RETAIL',
  creditLimit: '0',
  address: '',
  notes: '',
};

const PharmacyCustomers = () => {
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  // Dialog state
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [formData, setFormData] = useState(emptyCustomer);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);

  const fetchCustomers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await pharmacyApi.getCustomers();
      const list = Array.isArray(res.data) ? res.data : (res.data?.data || []);
      setCustomers(list);
    } catch {
      setError('Failed to load customers.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchCustomers(); }, [fetchCustomers]);

  const handleOpenDialog = () => {
    setFormData(emptyCustomer);
    setFormError(null);
    setIsDialogOpen(true);
  };

  const handleCreateCustomer = async (e) => {
    if (e && e.preventDefault) e.preventDefault();
    if (!formData.name.trim()) {
      setFormError('Customer name is required.');
      return;
    }

    setSubmitting(true);
    setFormError(null);
    try {
      const payload = {
        name: formData.name.trim(),
        phone: formData.phone.trim() || null,
        email: formData.email.trim() || null,
        farmName: formData.farmName.trim() || null,
        farmType: formData.farmType || null,
        customerType: formData.customerType || 'RETAIL',
        creditLimit: formData.creditLimit ? parseFloat(formData.creditLimit) : 0,
        address: formData.address.trim() || null,
        notes: formData.notes.trim() || null,
        isActive: true,
      };

      await pharmacyApi.createCustomer(payload);
      setIsDialogOpen(false);
      setFormData(emptyCustomer);
      fetchCustomers();
    } catch (err) {
      setFormError(err?.response?.data?.message || err?.message || 'Failed to create customer. Please check permissions.');
    } finally {
      setSubmitting(false);
    }
  };

  const filtered = customers.filter((c) =>
    c.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.phone?.includes(searchQuery) ||
    c.farmName?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const getCustomerTypeBadge = (type) => {
    switch (type) {
      case 'WHOLESALE': return <Badge className="bg-blue-100 text-blue-700 hover:bg-blue-200">Wholesale</Badge>;
      case 'PARTNER_FARM': return <Badge className="bg-emerald-100 text-emerald-800 hover:bg-emerald-200">Partner Farm</Badge>;
      default: return <Badge variant="secondary">Retail</Badge>;
    }
  };

  const stats = [
    { title: 'Total Customers', value: customers.length, icon: Users, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'With Balance', value: customers.filter((c) => (c.outstandingBalance || 0) > 0).length, icon: History, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
  ];

  return (
    <>
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
          <Button className="bg-primary hover:bg-primary/90 text-white font-medium" onClick={handleOpenDialog}>
            <UserPlus className="h-4 w-4 mr-2" /> Add Customer
          </Button>
        }
        empty={!filtered.length && !loading ? {
          title: 'No customers found',
          description: 'Register pharmacy customers to track dispensary sales, prescription issues, and credit lines.',
          action: (
            <Button className="bg-primary hover:bg-primary/90 text-white" onClick={handleOpenDialog}>
              <UserPlus className="h-4 w-4 mr-2" /> Add Customer
            </Button>
          )
        } : null}
      >
        <div className="relative max-w-sm mb-4">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
          <Input placeholder="Search by name, phone, or farm..." className="pl-10" value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} />
        </div>

        <Card className="rounded-2xl border-none shadow-sm">
          <CardHeader>
            <CardTitle>Commercial Clients</CardTitle>
            <CardDescription>Registered farmers, commercial buyers, and partner organizations</CardDescription>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Code</TableHead>
                  <TableHead>Name</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Contact & Address</TableHead>
                  <TableHead>Outstanding Balance</TableHead>
                  <TableHead>Credit Limit</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filtered.map((customer) => (
                  <TableRow key={customer.id}>
                    <TableCell className="font-mono text-xs text-muted-foreground">{customer.customerCode || `#${customer.id}`}</TableCell>
                    <TableCell>
                      <div className="font-semibold">{customer.name}</div>
                      {customer.farmName && (
                        <div className="text-xs text-slate-500 flex items-center gap-1">
                          <Building2 className="h-3 w-3" /> {customer.farmName}
                        </div>
                      )}
                    </TableCell>
                    <TableCell>{getCustomerTypeBadge(customer.customerType)}</TableCell>
                    <TableCell className="text-xs space-y-0.5">
                      {customer.phone && <div className="flex items-center gap-1"><Phone className="h-3 w-3 text-slate-400" />{customer.phone}</div>}
                      {customer.address && <div className="flex items-center gap-1 text-slate-500"><MapPin className="h-3 w-3 text-slate-400" />{customer.address.substring(0, 32)}</div>}
                    </TableCell>
                    <TableCell className={Number(customer.outstandingBalance || 0) > 0 ? 'text-rose-600 font-semibold' : 'text-slate-400'}>
                      {formatCurrency(customer.outstandingBalance || 0)}
                    </TableCell>
                    <TableCell>{formatCurrency(customer.creditLimit || 0)}</TableCell>
                    <TableCell className="text-right">
                      <Button variant="ghost" size="sm" onClick={() => {
                        setFormData({
                          name: customer.name || '',
                          phone: customer.phone || '',
                          email: customer.email || '',
                          farmName: customer.farmName || '',
                          farmType: customer.farmType || 'Poultry',
                          customerType: customer.customerType || 'RETAIL',
                          creditLimit: String(customer.creditLimit || '0'),
                          address: customer.address || '',
                          notes: customer.notes || '',
                        });
                        setIsDialogOpen(true);
                      }}>
                        <ExternalLink className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </EnterprisePage>

      {/* Add / Edit Customer Dialog - Mounted outside EnterprisePage */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="max-w-xl">
          <DialogHeader>
            <DialogTitle>Add Pharmacy Customer</DialogTitle>
            <DialogDescription>
              Register a client for veterinary drug dispensing, sales receipts, and credit management.
            </DialogDescription>
          </DialogHeader>

          {formError && (
            <div className="flex items-center gap-2 p-3 text-sm text-rose-700 bg-rose-50 border border-rose-200 rounded-lg">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          <div className="grid gap-4 py-2">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label htmlFor="cust-name">Client / Farmer Name *</Label>
                <Input
                  id="cust-name"
                  placeholder="e.g. Abebe Kebede"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                />
              </div>

              <div className="space-y-1">
                <Label htmlFor="cust-farm">Farm / Company Name</Label>
                <Input
                  id="cust-farm"
                  placeholder="e.g. Bishoftu Layer Farm"
                  value={formData.farmName}
                  onChange={(e) => setFormData({ ...formData, farmName: e.target.value })}
                />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label htmlFor="cust-phone">Phone Number</Label>
                <Input
                  id="cust-phone"
                  placeholder="e.g. +251 911 234567"
                  value={formData.phone}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                />
              </div>

              <div className="space-y-1">
                <Label htmlFor="cust-email">Email Address</Label>
                <Input
                  id="cust-email"
                  type="email"
                  placeholder="e.g. farm@example.com"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="space-y-1">
                <Label>Customer Type</Label>
                <Select
                  value={formData.customerType}
                  onValueChange={(v) => setFormData({ ...formData, customerType: v })}
                >
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="RETAIL">Retail Farmer</SelectItem>
                    <SelectItem value="WHOLESALE">Wholesale Buyer</SelectItem>
                    <SelectItem value="PARTNER_FARM">Partner Farm</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1">
                <Label>Operation Type</Label>
                <Select
                  value={formData.farmType}
                  onValueChange={(v) => setFormData({ ...formData, farmType: v })}
                >
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="Poultry">Poultry</SelectItem>
                    <SelectItem value="Cattle">Cattle / Dairy</SelectItem>
                    <SelectItem value="Sheep & Goat">Sheep & Goat</SelectItem>
                    <SelectItem value="Mixed">Mixed Farming</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1">
                <Label htmlFor="cust-credit">Credit Limit (ETB)</Label>
                <Input
                  id="cust-credit"
                  type="number"
                  min="0"
                  step="any"
                  placeholder="0.00"
                  value={formData.creditLimit}
                  onChange={(e) => setFormData({ ...formData, creditLimit: e.target.value })}
                />
              </div>
            </div>

            <div className="space-y-1">
              <Label htmlFor="cust-address">Delivery / Physical Address</Label>
              <Input
                id="cust-address"
                placeholder="e.g. Bishoftu Kebele 02, near poultry market"
                value={formData.address}
                onChange={(e) => setFormData({ ...formData, address: e.target.value })}
              />
            </div>

            <div className="space-y-1">
              <Label htmlFor="cust-notes">Notes / Special Instructions</Label>
              <Textarea
                id="cust-notes"
                rows={2}
                placeholder="Optional veterinary notes, delivery instructions, or flock history"
                value={formData.notes}
                onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
              />
            </div>
          </div>

          <DialogFooter className="gap-2">
            <Button variant="outline" onClick={() => setIsDialogOpen(false)} disabled={submitting}>
              Cancel
            </Button>
            <Button className="bg-primary hover:bg-primary/90 text-white" onClick={handleCreateCustomer} disabled={submitting}>
              {submitting ? 'Saving...' : 'Save Customer'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
};

export default PharmacyCustomers;
