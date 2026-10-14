import React, { useEffect, useState, useMemo } from 'react';
import { pharmacyApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Badge } from '../../components/ui/badge';
import { Button } from '../../components/ui/button';
import { formatCurrency } from '../../utils';
import {
    Wallet,
    TrendingUp,
    Package,
    AlertTriangle,
    RefreshCw,
    ClipboardList,
    Plus
} from 'lucide-react';
import { cn } from '../../lib/utils';
import PrescriptionQueue from './PrescriptionQueue';
import { useNavigate } from 'react-router-dom';

const PharmacyOverview = () => {
    const navigate = useNavigate();
    const [dashData, setDashData] = useState({
        revenue_today: 0,
        sales_count_today: 0,
        pending_prescriptions: 0,
        pending_rx_list: []
    });
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);

    useEffect(() => { fetchAll(); }, []);

    const fetchAll = async () => {
        setRefreshing(true);
        try {
            const [dashRes, rxRes, itemsRes] = await Promise.all([
                pharmacyApi.getDashboard(),
                pharmacyApi.getPrescriptions(),
                pharmacyApi.getPharmacyCatalog()
            ]);
            setDashData({
                ...(dashRes.data?.data || dashRes.data || {}),
                pending_rx_list: rxRes.data?.data || []
            });
            setItems(itemsRes.data?.data || []);
        } catch (e) {
            console.error('Failed to fetch pharmacy overview', e);
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    };

    const inventoryValue = items.reduce((sum, i) => {
        const price = Number(i.retailPrice || i.sellingPrice || 0);
        return sum + price * (i.currentStock || 0);
    }, 0);

    const lowStockCount = items.filter(i => (i.currentStock || 0) <= (i.minimumStockLevel || 0)).length;

    const expiringCount = items.filter(i => i.expiryDate && new Date(i.expiryDate) <= new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)).length;

    const getStockStatus = (item) => {
        if ((item.currentStock || 0) <= 0) return 'out_of_stock';
        if (item.expiryDate && new Date(item.expiryDate) <= new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)) return 'expiring_soon';
        if ((item.currentStock || 0) <= (item.minimumStockLevel || item.reorderPoint || 0)) return 'low_stock';
        return 'in_stock';
    };

    const kpis = [
        {
            title: "Today's Revenue",
            value: formatCurrency(dashData.revenue_today || 0),
            description: `${dashData.sales_count_today || 0} successful sales`,
            icon: Wallet,
            iconColor: 'text-emerald-600 dark:text-emerald-400',
            bgColor: 'bg-emerald-500/10 dark:bg-emerald-500/20'
        },
        {
            title: 'Monthly Revenue',
            value: formatCurrency(dashData.revenue_month || 0),
            description: `${dashData.sales_count_month || 0} sales this month`,
            icon: TrendingUp,
            iconColor: 'text-primary',
            bgColor: 'bg-primary/10 dark:bg-primary/20'
        },
        {
            title: 'Inventory Value',
            value: formatCurrency(inventoryValue),
            description: `${items.length} products monitored`,
            icon: Package,
            iconColor: 'text-blue-600 dark:text-blue-400',
            bgColor: 'bg-blue-500/10 dark:bg-blue-500/20'
        },
        {
            title: 'Alerts',
            value: lowStockCount + expiringCount,
            description: `${lowStockCount} low stock · ${expiringCount} expiring`,
            icon: AlertTriangle,
            iconColor: 'text-amber-600 dark:text-amber-400',
            bgColor: 'bg-amber-500/10 dark:bg-amber-500/20',
        }
    ];

    const columns = useMemo(() => [
        {
            accessorKey: 'category',
            header: 'Category',
            cell: ({ row }) => (
                <Badge variant="secondary" className="text-[10px] font-bold uppercase">
                    {row.original.category?.replace('_', ' ') || 'N/A'}
                </Badge>
            )
        },
        {
            accessorKey: 'itemName',
            header: 'Product',
            cell: ({ row }) => (
                <div className="flex flex-col">
                    <span className="font-black text-foreground">{row.original.itemName}</span>
                    <div className="flex items-center gap-2 mt-0.5">
                        <span className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">{row.original.sku || 'N/A'}</span>
                        {row.original.isControlled && (
                            <Badge className="h-4 px-1.5 bg-rose-500/20 text-rose-600 dark:text-rose-400 text-[9px] font-black uppercase border-none">Controlled</Badge>
                        )}
                    </div>
                </div>
            )
        },
        {
            accessorKey: 'currentStock',
            header: 'Stock',
            cell: ({ row }) => (
                <span className={cn(
                    "font-black",
                    row.original.currentStock <= (row.original.minimumStockLevel || 0) ? "text-rose-600 dark:text-rose-400" : "text-foreground"
                )}>
                    {row.original.currentStock} {row.original.unit}
                </span>
            )
        },
        {
            accessorKey: 'retailPrice',
            header: 'Unit Price',
            cell: ({ row }) => <span className="font-bold text-primary">{formatCurrency(row.original.retailPrice || row.original.sellingPrice)}</span>
        },
        {
            id: 'batch',
            header: 'Batch',
            cell: ({ row }) => <span className="text-xs font-mono text-muted-foreground">{row.original.batchNumber || '—'}</span>
        },
        {
            id: 'expiry',
            header: 'Expiry',
            cell: ({ row }) => <span className="text-xs text-muted-foreground">{row.original.expiryDate || '—'}</span>
        },
        {
            id: 'invValue',
            header: 'Inv. Value',
            cell: ({ row }) => {
                const price = Number(row.original.retailPrice || row.original.sellingPrice || 0);
                return <span className="font-bold text-foreground">{formatCurrency(price * (row.original.currentStock || 0))}</span>
            }
        },
        {
            accessorKey: 'status',
            header: 'Stock Status',
            cell: ({ row }) => {
                const status = getStockStatus(row.original);
                return <StatusBadge status={status} />
            }
        }
    ], []);

    if (loading) {
        return <EnterprisePage title="Pharmacy Terminal" subtitle="Loading..." breadcrumbs={['Pharmacy']} loading />;
    }

    return (
        <EnterprisePage
            title="Pharmacy Terminal"
            subtitle="End-to-end medical sales and fulfillment oversight"
            breadcrumbs={['Pharmacy', 'Command Center']}
            onRefresh={fetchAll}
            stats={kpis}
            actions={
                <>
                    <Button variant="outline" onClick={fetchAll} disabled={refreshing}>
                        <RefreshCw className={cn('mr-2 h-4 w-4', refreshing && 'animate-spin')} />
                        Sync
                    </Button>
                    <Button className="bg-primary hover:bg-primary/90" onClick={() => navigate('/pharmacy/pos')}>
                        <Plus className="mr-2 h-4 w-4" /> New POS Sale
                    </Button>
                </>
            }
        >
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-2 space-y-6">
                    <Card className="erp-card overflow-hidden">
                        <CardHeader className="border-b border-border/60 flex flex-row items-center justify-between px-8 py-6">
                            <CardTitle className="text-base font-black uppercase tracking-tight text-foreground flex items-center gap-2">
                                <Package className="h-4.5 w-4.5 text-primary" />
                                Drugs & Supply Monitoring
                            </CardTitle>
                            <Badge className="bg-primary/10 text-primary border-none font-bold">Live Catalog</Badge>
                        </CardHeader>
                        <CardContent className="p-0">
                            <DataTable columns={columns} data={items} pageSize={5} searchable />
                        </CardContent>
                    </Card>
                </div>

                <div className="space-y-6">
                    <Card className="erp-card overflow-hidden">
                        <CardHeader className="border-b border-border/60 px-8 py-6">
                            <CardTitle className="text-base font-black uppercase tracking-tight text-foreground flex items-center gap-2">
                                <ClipboardList className="h-4.5 w-4.5 text-rose-500" />
                                Fulfillment Queue
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="p-6">
                            <PrescriptionQueue prescriptions={dashData.pending_rx_list} loading={refreshing} />
                        </CardContent>
                    </Card>
                </div>
            </div>
        </EnterprisePage>
    );
};

export default PharmacyOverview;
