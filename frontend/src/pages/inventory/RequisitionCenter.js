import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { requisitionApi, inventoryApi, farmApi } from '../../api';
import PageHeader from '../../components/shared/PageHeader';
import PageContainer from '../../components/layout/PageContainer';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from '../../components/ui/dialog';
import { Label } from '../../components/ui/label';
import { Input } from '../../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import {
    ClipboardList, Plus, Search, Filter,
    FileText, CheckCircle2, Truck, AlertCircle,
    User, Calendar, Hash, ArrowUpRight,
    Activity, Clock, Box, Building2
} from 'lucide-react';
import { format } from 'date-fns';
import { cn } from '../../lib/utils';

export default function RequisitionCenter() {
    const { user, canPerformAction } = useAuth();
    const queryClient = useQueryClient();
    const [open, setOpen] = useState(false);

    // Feature Checks
    const isIM = user?.role === 'STORE_KEEPER' || user?.role === 'ADMIN';
    const canRequest = ['FARM_MANAGER', 'VETERINARY_OFFICER', 'PHARMACY_SALES'].includes(user?.role);

    // Data Fetching
    const { data: requisitions = [], isLoading: loadingReqs } = useQuery({
        queryKey: ['requisitions'],
        queryFn: async () => {
            const res = await requisitionApi.getAll();
            return res.data || [];
        }
    });

    const { data: items = [] } = useQuery({
        queryKey: ['inventory-items'],
        queryFn: async () => {
            const res = await inventoryApi.getItems();
            return res.data?.data || [];
        }
    });

    const { data: farms = [] } = useQuery({
        queryKey: ['farms'],
        queryFn: async () => {
            const res = await farmApi.getAll();
            return res.data || [];
        }
    });

    // Mutations
    const createMutation = useMutation({
        mutationFn: (data) => requisitionApi.create(data),
        onSuccess: () => {
            queryClient.invalidateQueries(['requisitions']);
            toast.success("Internal requisition submitted for approval.");
            setOpen(false);
        },
        onError: (err) => toast.error(err.response?.data?.error || "Submission failed")
    });

    const approveMutation = useMutation({
        mutationFn: (id) => requisitionApi.approve(id),
        onSuccess: () => {
            queryClient.invalidateQueries(['requisitions']);
            toast.success("Requisition approved.");
        }
    });

    const issueMutation = useMutation({
        mutationFn: (id) => requisitionApi.issue(id),
        onSuccess: () => {
            queryClient.invalidateQueries(['requisitions'], ['inventory-dashboard']);
            toast.success("Stock issued and ledgered. Requisition fulfilled.");
        }
    });

    const columns = [
        {
            accessorKey: 'requisitionNumber',
            header: 'REQUEST ID',
            cell: ({ row }) => (
                <span className="font-mono text-[11px] font-black text-slate-500">{row.original.requisitionNumber}</span>
            )
        },
        {
            accessorKey: 'department',
            header: 'SOURCE',
            cell: ({ row }) => (
                <div className="flex flex-col">
                    <span className="font-bold text-slate-900">{row.original.requestedBy?.role?.replace('_', ' ') || 'Internal'}</span>
                    <span className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">{row.original.farm?.name || 'Central'}</span>
                </div>
            )
        },
        {
            id: 'items_summary',
            header: 'ITEMS',
            cell: ({ row }) => {
                const count = row.original.items?.length || 0;
                const first = row.original.items?.[0]?.item?.itemName || 'Multiple';
                return (
                    <div className="flex items-center gap-2">
                        <span className="font-medium text-slate-700">{first}</span>
                        {count > 1 && <Badge className="bg-slate-100 text-slate-500 font-black text-[9px] border-none">+{count - 1} more</Badge>}
                    </div>
                )
            }
        },
        {
            accessorKey: 'requestedAt',
            header: 'TIMESTAMP',
            cell: ({ row }) => (
                <div className="flex items-center gap-2 text-slate-400 font-bold text-[11px]">
                    <Clock className="h-3 w-3" />
                    {format(new Date(row.original.requestedAt), 'dd MMM HH:mm')}
                </div>
            )
        },
        {
            accessorKey: 'status',
            header: 'STATUS',
            cell: ({ row }) => <StatusBadge status={row.original.status.toLowerCase()} />
        },
        {
            id: 'actions',
            header: () => <div className="text-right">OPERATION</div>,
            cell: ({ row }) => {
                const rx = row.original;
                return (
                    <div className="flex justify-end gap-2">
                        {isIM && rx.status === 'REQUESTED' && (
                            <Button size="sm" className="h-8 bg-emerald-600 hover:bg-emerald-700 font-black text-[10px] rounded-lg" onClick={() => approveMutation.mutate(rx.id)}>
                                APPROVE
                            </Button>
                        )}
                        {isIM && rx.status === 'APPROVED' && (
                            <Button size="sm" className="h-8 bg-indigo-600 hover:bg-indigo-700 font-black text-[10px] rounded-lg" onClick={() => issueMutation.mutate(rx.id)}>
                                ISSUE STOCK
                            </Button>
                        )}
                        {rx.status === 'ISSUED' && (
                            <Badge className="bg-emerald-50 text-emerald-600 border-none font-black text-[9px]">FULFILLED</Badge>
                        )}
                    </div>
                )
            }
        }
    ];

    return (
        <PageContainer>
            <PageHeader
                title="Internal Requisition Center"
                subtitle="Centralized material request and fulfillment hub for farm operations."
                breadcrumbs={['Inventory', 'Requisitions']}
            >
                {canRequest && (
                    <Dialog open={open} onOpenChange={setOpen}>
                        <DialogTrigger asChild>
                            <Button className="h-11 rounded-xl bg-indigo-600 hover:bg-indigo-700 shadow-lg shadow-indigo-100 font-black tracking-tight">
                                <Plus className="mr-2 h-5 w-5 stroke-[3px]" />
                                New Request
                            </Button>
                        </DialogTrigger>
                        <DialogContent className="max-w-2xl rounded-[2.5rem]">
                            <DialogHeader>
                                <DialogTitle className="text-2xl font-black tracking-tighter">Material Requisition</DialogTitle>
                                <DialogDescription className="font-bold text-slate-400 uppercase tracking-widest text-[10px]">Submit an operational request for warehouse stock.</DialogDescription>
                            </DialogHeader>
                            <RequisitionForm farms={farms} items={items} onSubmit={(data) => createMutation.mutate(data)} loading={createMutation.isLoading} />
                        </DialogContent>
                    </Dialog>
                )}
            </PageHeader>

            <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
                <Card className="lg:col-span-1 border-none shadow-sm bg-white rounded-[2rem] h-fit">
                    <CardHeader className="pb-4">
                        <CardTitle className="text-xs font-black uppercase tracking-widest text-slate-400">Queue Analytics</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-6 pb-8">
                        {[
                            { label: 'Pending Approval', count: requisitions.filter(r => r.status === 'REQUESTED').length, color: 'text-amber-600', bg: 'bg-amber-50', icon: Clock },
                            { label: 'Approved & Ready', count: requisitions.filter(r => r.status === 'APPROVED').length, color: 'text-indigo-600', bg: 'bg-indigo-50', icon: CheckCircle2 },
                            { label: 'Fulfilled Today', count: requisitions.filter(r => r.status === 'ISSUED' && format(new Date(r.issuedAt), 'yyyy-MM-dd') === format(new Date(), 'yyyy-MM-dd')).length, color: 'text-emerald-600', bg: 'bg-emerald-50', icon: Truck }
                        ].map((stat, i) => (
                            <div key={i} className="flex flex-col gap-3">
                                <div className="flex justify-between items-center">
                                    <div className="flex items-center gap-3">
                                        <div className={cn("h-10 w-10 rounded-xl flex items-center justify-center", stat.bg)}>
                                            <stat.icon className={cn("h-5 w-5", stat.color)} />
                                        </div>
                                        <span className="text-[10px] font-black uppercase tracking-widest text-slate-500">{stat.label}</span>
                                    </div>
                                    <span className="text-xl font-black text-slate-900">{stat.count}</span>
                                </div>
                                <div className="w-full h-1 bg-slate-50 rounded-full overflow-hidden">
                                    <div className={cn("h-full", stat.color.replace('text', 'bg'))} style={{ width: `${(stat.count / (requisitions.length || 1)) * 100}%` }} />
                                </div>
                            </div>
                        ))}
                    </CardContent>
                </Card>

                <div className="lg:col-span-3">
                    <Card className="border-none shadow-sm bg-white rounded-[2rem] overflow-hidden">
                        <CardHeader className="bg-white py-6 border-b border-slate-50 px-8 flex flex-row items-center justify-between">
                            <div>
                                <CardTitle className="text-lg font-black tracking-tight text-slate-900 flex items-center gap-2">
                                    <ClipboardList className="h-5 w-5 text-indigo-600" /> Operational Queue
                                </CardTitle>
                                <CardDescription className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Tracking all cross-module material movements</CardDescription>
                            </div>
                            <Badge className="bg-slate-50 text-indigo-600 border-none font-bold uppercase tracking-widest text-[9px]">Live Monitoring</Badge>
                        </CardHeader>
                        <CardContent className="p-0">
                            <DataTable columns={columns} data={requisitions} loading={loadingReqs} pageSize={10} selectable />
                        </CardContent>
                    </Card>
                </div>
            </div>
        </PageContainer>
    );
}

function RequisitionForm({ farms, items, onSubmit, loading }) {
    const [selectedItems, setSelectedItems] = useState([{ itemId: '', quantity: 1 }]);
    const [farmId, setFarmId] = useState('');
    const [notes, setNotes] = useState('');

    const addItem = () => setSelectedItems([...selectedItems, { itemId: '', quantity: 1 }]);
    const removeItem = (idx) => setSelectedItems(selectedItems.filter((_, i) => i !== idx));
    const updateItem = (idx, field, val) => {
        const next = [...selectedItems];
        next[idx][field] = val;
        setSelectedItems(next);
    };

    const handleFormSubmit = (e) => {
        e.preventDefault();
        onSubmit({ farmId, notes, items: selectedItems });
    };

    return (
        <form onSubmit={handleFormSubmit} className="space-y-8 pt-6">
            <div className="grid grid-cols-2 gap-6">
                <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-widest text-slate-400">Target Farm Location</Label>
                    <Select onValueChange={setFarmId} required>
                        <SelectTrigger className="h-12 rounded-xl bg-slate-50 border-none shadow-none focus:ring-0">
                            <SelectValue placeholder="Select Destination" />
                        </SelectTrigger>
                        <SelectContent className="rounded-xl">
                            {farms.map(f => (
                                <SelectItem key={f.id} value={f.id.toString()} className="font-bold">{f.name}</SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>
                <div className="space-y-3">
                    <Label className="text-[10px] font-black uppercase tracking-widest text-slate-400">Movement Context</Label>
                    <Input
                        placeholder="Internal Order #"
                        className="h-12 rounded-xl bg-slate-50 border-none shadow-none font-bold placeholder:text-slate-300"
                    />
                </div>
            </div>

            <div className="space-y-6">
                <div className="flex justify-between items-center">
                    <Label className="text-[10px] font-black uppercase tracking-widest text-slate-400">Requested Materials</Label>
                    <Button type="button" variant="ghost" size="sm" onClick={addItem} className="h-8 rounded-lg text-[10px] font-black text-indigo-600 hover:bg-indigo-50">
                        <Plus className="h-3 w-3 mr-1" /> ADD ITEM
                    </Button>
                </div>

                <div className="space-y-3">
                    {selectedItems.map((si, idx) => (
                        <div key={idx} className="flex gap-4 items-end animate-in slide-in-from-left-2 duration-300">
                            <div className="flex-1 space-y-2">
                                <Select onValueChange={(val) => updateItem(idx, 'itemId', val)} required>
                                    <SelectTrigger className="h-12 rounded-xl bg-white border border-slate-100 shadow-none">
                                        <SelectValue placeholder="Identify Product" />
                                    </SelectTrigger>
                                    <SelectContent className="rounded-xl">
                                        {items.map(item => (
                                            <SelectItem key={item.id} value={item.id.toString()}>
                                                <div className="flex items-center gap-2">
                                                    <span className="font-bold">{item.itemName}</span>
                                                    <span className="text-[10px] text-slate-400 font-mono">({item.currentStock} in stock)</span>
                                                </div>
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                            <div className="w-32 space-y-2">
                                <Input
                                    type="number"
                                    placeholder="Qty"
                                    value={si.quantity}
                                    onChange={(e) => updateItem(idx, 'quantity', e.target.value)}
                                    className="h-12 rounded-xl bg-white border border-slate-100 shadow-none font-black"
                                    required
                                />
                            </div>
                            {selectedItems.length > 1 && (
                                <Button type="button" variant="ghost" size="icon" onClick={() => removeItem(idx)} className="h-12 w-12 rounded-xl text-rose-500 hover:bg-rose-50">
                                    <Hash className="h-4 w-4 rotate-45" />
                                </Button>
                            )}
                        </div>
                    ))}
                </div>
            </div>

            <div className="space-y-3">
                <Label className="text-[10px] font-black uppercase tracking-widest text-slate-400">Protocol Remarks</Label>
                <textarea
                    className="w-full min-h-[100px] rounded-2xl bg-slate-50 border-none p-4 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-indigo-100 transition-all"
                    placeholder="Provide specific clinical or operational justification..."
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                />
            </div>

            <Button disabled={loading} className="w-full h-14 rounded-2xl bg-[#0f172a] hover:bg-[#1e293b] text-white font-black text-lg shadow-xl shadow-slate-200 transition-all active:scale-95">
                {loading ? <Activity className="animate-spin h-6 w-6" /> : "Deploy Requisition Order"}
            </Button>
        </form>
    );
}
