import React, { useState, useEffect } from 'react';
import { inventoryApi } from '../../api';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import PageHeader from '../../components/shared/PageHeader';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Badge } from '../../components/ui/badge';
import { formatDate } from '../../utils';
import { useLanguage } from '../../context/LanguageContext';
import { Clock, AlertTriangle, CheckCircle2, Search, Filter, Download } from 'lucide-react';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';

const ExpiryTracking = () => {
    const { t } = useLanguage();
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');

    useEffect(() => {
        const fetchData = async () => {
            try {
                const res = await inventoryApi.getExpiryAlerts();
                setData(res.data.data || []);
            } catch (e) {
                console.error('Failed to fetch expiry data', e);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    const getStatus = (expiryDate) => {
        const days = Math.floor((new Date(expiryDate) - new Date()) / (1000 * 60 * 60 * 24));
        if (days < 0) return { label: 'Expired', variant: 'high' };
        if (days < 30) return { label: 'Critical', variant: 'high' };
        if (days < 90) return { label: 'Warning', variant: 'medium' };
        return { label: 'Safe', variant: 'low' };
    };

    const columns = [
        { accessorKey: 'itemName', header: 'Item Name', cell: ({ row }) => row.original.item?.itemName },
        { accessorKey: 'batchNumber', header: 'Batch/Lot #' },
        { accessorKey: 'quantityRemaining', header: 'Qty on Hand' },
        {
            accessorKey: 'expiryDate',
            header: 'Expiry Date',
            cell: ({ row }) => (
                <div className="flex flex-col">
                    <span className="font-semibold">{formatDate(row.original.expiryDate)}</span>
                    <span className="text-[10px] text-gray-500">
                        {Math.floor((new Date(row.original.expiryDate) - new Date()) / (1000 * 60 * 60 * 24))} days left
                    </span>
                </div>
            )
        },
        {
            accessorKey: 'status',
            header: 'Status',
            cell: ({ row }) => {
                const status = getStatus(row.original.expiryDate);
                return <StatusBadge status={status.variant} />;
            }
        },
        { accessorKey: 'supplier', header: 'Supplier' },
    ];

    const criticalCount = data.filter(i => {
        const days = Math.floor((new Date(i.expiryDate) - new Date()) / (1000 * 60 * 60 * 24));
        return days < 30;
    }).length;

    return (
        <div className="space-y-6 pb-8">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <PageHeader
                    title="Stock Expiration Tracking"
                    subtitle="Proactively manage pharmaceutical and perishable inventory lifecycles."
                    className="mb-0"
                />
                <div className="flex gap-2">
                    <Button variant="outline" size="sm">
                        <Download className="mr-2 h-4 w-4" />
                        Export Audit Log
                    </Button>
                    <Button size="sm" variant="destructive">
                        Handle Expired Items
                    </Button>
                </div>
            </div>

            {/* Summary Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <Card className="bg-red-50 border-red-100">
                    <CardContent className="pt-6">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm font-medium text-red-600 uppercase tracking-wider">Critical / Expired</p>
                                <p className="text-3xl font-bold text-red-900 mt-1">{criticalCount}</p>
                            </div>
                            <div className="bg-red-200/50 p-3 rounded-xl">
                                <AlertTriangle className="h-6 w-6 text-red-600" />
                            </div>
                        </div>
                        <p className="text-xs text-red-600/80 mt-4 font-medium">Immediate action required for removal or usage</p>
                    </CardContent>
                </Card>

                <Card className="bg-orange-50 border-orange-100">
                    <CardContent className="pt-6">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm font-medium text-orange-600 uppercase tracking-wider">Warning (30-90 Days)</p>
                                <p className="text-3xl font-bold text-orange-900 mt-1">
                                    {data.filter(i => {
                                        const days = Math.floor((new Date(i.expiryDate) - new Date()) / (1000 * 60 * 60 * 24));
                                        return days >= 30 && days < 90;
                                    }).length}
                                </p>
                            </div>
                            <div className="bg-orange-200/50 p-3 rounded-xl">
                                <Clock className="h-6 w-6 text-orange-600" />
                            </div>
                        </div>
                        <p className="text-xs text-orange-600/80 mt-4 font-medium">Prioritize these batches for upcoming operations</p>
                    </CardContent>
                </Card>

                <Card className="bg-emerald-50 border-emerald-100">
                    <CardContent className="pt-6">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm font-medium text-emerald-600 uppercase tracking-wider">Safe Stock</p>
                                <p className="text-3xl font-bold text-emerald-900 mt-1">
                                    {data.filter(i => {
                                        const days = Math.floor((new Date(i.expiryDate) - new Date()) / (1000 * 60 * 60 * 24));
                                        return days >= 90;
                                    }).length}
                                </p>
                            </div>
                            <div className="bg-emerald-200/50 p-3 rounded-xl">
                                <CheckCircle2 className="h-6 w-6 text-emerald-600" />
                            </div>
                        </div>
                        <p className="text-xs text-emerald-600/80 mt-4 font-medium">Healthy inventory lifecycle</p>
                    </CardContent>
                </Card>
            </div>

            {/* Main List */}
            <Card>
                <CardHeader className="flex flex-row items-center justify-between">
                    <CardTitle className="text-lg font-bold">Comprehensive Expiry Log</CardTitle>
                    <div className="flex items-center gap-2">
                        <div className="relative">
                            <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-gray-400" />
                            <Input
                                placeholder="Find batch..."
                                className="pl-9 w-[200px]"
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                            />
                        </div>
                        <Button variant="outline" size="icon">
                            <Filter className="h-4 w-4" />
                        </Button>
                    </div>
                </CardHeader>
                <CardContent>
                    <DataTable
                        columns={columns}
                        data={data.filter(i => i.batchNumber?.toLowerCase().includes(searchTerm.toLowerCase()) || i.item?.itemName?.toLowerCase().includes(searchTerm.toLowerCase()))}
                        loading={loading}
                        pagination
                    />
                </CardContent>
            </Card>
        </div>
    );
};

export default ExpiryTracking;
