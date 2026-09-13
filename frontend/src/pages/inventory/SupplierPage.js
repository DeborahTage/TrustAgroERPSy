import React, { useEffect, useState } from 'react';
import { inventoryApi, supplierApi, api } from '../../api';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "../../components/ui/table";
import { cn } from "../../lib/utils";
import { Badge } from '../../components/ui/badge';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '../../components/ui/dialog';
import { Plus, Users, Mail, Phone, MapPin, Star, Trash2, Edit } from 'lucide-react';
import { toast } from 'react-toastify';
import EnterprisePage from '../../components/layout/EnterprisePage';

const SupplierPage = () => {
    const [suppliers, setSuppliers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [form, setForm] = useState({ name: '', contactPerson: '', email: '', phone: '', address: '', category: '', paymentTerms: '', rating: 5 });
    const [editingId, setEditingId] = useState(null);
    const [isDialogOpen, setIsDialogOpen] = useState(false);

    useEffect(() => {
        fetchSuppliers();
    }, []);

    const [error, setError] = useState(null);

    const fetchSuppliers = async () => {
        setLoading(true);
        setError(null);
        try {
            const r = await supplierApi.getAll();
            setSuppliers(r.data.data || []);
        } catch (err) {
            setError('Failed to load suppliers.');
            toast.error('Failed to load suppliers');
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            if (editingId) await supplierApi.update(editingId, form);
            else await supplierApi.create(form);
            toast.success(editingId ? 'Supplier updated' : 'Supplier added');
            fetchSuppliers();
            setIsDialogOpen(false);
            resetForm();
        } catch (err) {
            toast.error('Operation failed');
        }
    };

    const resetForm = () => {
        setForm({ name: '', contactPerson: '', email: '', phone: '', address: '', category: '', paymentTerms: '', rating: 4 });
        setEditingId(null);
    };

    const handleEdit = (s) => {
        setForm(s);
        setEditingId(s.id);
        setIsDialogOpen(true);
    };

    const handleDelete = async (id) => {
        if (!window.confirm('Delete this supplier?')) return;
        try {
            await supplierApi.delete(id);
            toast.success('Supplier deleted');
            fetchSuppliers();
        } catch (err) {
            toast.error('Failed to delete');
        }
    };

    return (
        <EnterprisePage
            title="Supplier Management"
            subtitle="Manage vendors, contact details and performance ratings for streamlined procurement"
            breadcrumbs={['Inventory', 'Suppliers']}
            loading={loading}
            error={error}
            onRetry={fetchSuppliers}
            onRefresh={fetchSuppliers}
            stats={[
                { title: 'Total Suppliers', value: suppliers.length, icon: Users, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
                { title: 'Rated 4+ Stars', value: suppliers.filter(s => (s.rating || 0) >= 4).length, icon: Star, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
            ]}
            actions={
                <Dialog open={isDialogOpen} onOpenChange={(open) => { setIsDialogOpen(open); if (!open) resetForm(); }}>
                    <DialogTrigger asChild>
                        <Button className="bg-primary hover:bg-primary/90 rounded-xl">
                            <Plus className="mr-2 h-4 w-4" /> Add Supplier
                        </Button>
                    </DialogTrigger>
                    <DialogContent className="sm:max-w-[500px] rounded-2xl">
                        <DialogHeader>
                            <DialogTitle>{editingId ? 'Update Supplier' : 'Register New Vendor'}</DialogTitle>
                        </DialogHeader>
                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div className="space-y-2">
                                <Label>Supplier Name *</Label>
                                <Input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required />
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                    <Label>Contact Person</Label>
                                    <Input value={form.contactPerson} onChange={e => setForm({ ...form, contactPerson: e.target.value })} />
                                </div>
                                <div className="space-y-2">
                                    <Label>Phone</Label>
                                    <Input value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })} />
                                </div>
                            </div>
                            <div className="space-y-2">
                                <Label>Email</Label>
                                <Input type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} />
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                    <Label>Category</Label>
                                    <Input value={form.category} onChange={e => setForm({ ...form, category: e.target.value })} placeholder="FEED, DRUGS..." />
                                </div>
                                <div className="space-y-2">
                                    <Label>Rating (1-5)</Label>
                                    <Input type="number" min="1" max="5" value={form.rating} onChange={e => setForm({ ...form, rating: Number(e.target.value) })} />
                                </div>
                            </div>
                            <div className="flex gap-3 pt-2">
                                <Button type="button" variant="outline" className="flex-1" onClick={() => setIsDialogOpen(false)}>Cancel</Button>
                                <Button type="submit" className="flex-1 bg-primary hover:bg-primary/90">{editingId ? 'Save' : 'Register'}</Button>
                            </div>
                        </form>
                    </DialogContent>
                </Dialog>
            }
            empty={!suppliers.length ? { title: 'No suppliers registered', description: 'Add your first vendor to enable purchase orders.' } : null}
        >
            <Card className="border-none shadow-sm rounded-2xl overflow-hidden">
                    <CardContent className="p-0">
                        <Table>
                            <TableHeader className="bg-gray-50/50">
                                <TableRow className="hover:bg-transparent border-b-gray-100">
                                    <TableHead className="font-bold text-gray-900 h-14 pl-8 uppercase text-[10px] tracking-widest">Provider Information</TableHead>
                                    <TableHead className="font-bold text-gray-900 uppercase text-[10px] tracking-widest">Specialization</TableHead>
                                    <TableHead className="font-bold text-gray-900 uppercase text-[10px] tracking-widest">Performance</TableHead>
                                    <TableHead className="text-right pr-8 font-bold text-gray-900 uppercase text-[10px] tracking-widest">Actions</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {suppliers.map((s) => (
                                    <TableRow key={s.id} className="hover:bg-gray-50/30 transition-colors border-b-gray-50">
                                        <TableCell className="pl-8 py-5">
                                            <div className="flex flex-col gap-1">
                                                <span className="font-black text-gray-900 tracking-tight">{s.name}</span>
                                                <div className="flex items-center gap-3 mt-1">
                                                    {s.email && <span className="flex items-center gap-1 text-[10px] font-bold text-gray-400 lowercase"><Mail className="h-3 w-3" />{s.email}</span>}
                                                    {s.phone && <span className="flex items-center gap-1 text-[10px] font-bold text-gray-400"><Phone className="h-3 w-3" />{s.phone}</span>}
                                                </div>
                                            </div>
                                        </TableCell>
                                        <TableCell>
                                            <Badge variant="outline" className="rounded-lg bg-primary/10/50 text-primary border-primary/20 font-bold px-3 py-1 text-[10px] uppercase tracking-wider">{s.category || 'GENERAL'}</Badge>
                                        </TableCell>
                                        <TableCell>
                                            <div className="flex items-center gap-1">
                                                {[...Array(5)].map((_, i) => (
                                                    <Star key={i} className={cn("h-3 w-3", i < (s.rating || 0) ? "fill-amber-400 text-amber-400" : "text-gray-200")} />
                                                ))}
                                            </div>
                                        </TableCell>
                                        <TableCell className="text-right pr-8">
                                            <div className="flex justify-end gap-2">
                                                <Button variant="ghost" size="icon" onClick={() => handleEdit(s)} className="h-8 w-8 text-blue-600 hover:bg-blue-50 rounded-lg"><Edit className="h-4 w-4" /></Button>
                                                <Button variant="ghost" size="icon" onClick={() => handleDelete(s.id)} className="h-8 w-8 text-rose-600 hover:bg-rose-50 rounded-lg"><Trash2 className="h-4 w-4" /></Button>
                                            </div>
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

export default SupplierPage;
