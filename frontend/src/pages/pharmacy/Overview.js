import React, { useEffect, useState, useMemo } from 'react';
import { pharmacyApi, inventoryApi } from '../../api';
import PageHeader from '../../components/shared/PageHeader';
import PageContainer from '../../components/layout/PageContainer';
import StatsCard from '../../components/shared/StatsCard';
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
    BadgeAlert,
    ClipboardList,
    Activity,
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
                inventoryApi.getItems()
            ]);
            setDashData({
                ...dashRes.data,
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

    const kpis = [
        {
            title: "Today's Revenue",
            value: formatCurrency(dashData.revenue_today || 0),
            description: `${dashData.sales_count_today || 0} successful sales`,
            icon: Wallet,
            iconColor: 'text-emerald-600',
            bgColor: 'bg-emerald-50'
        },
        {
            title: "Monthly Volume",
            value: formatCurrency(dashData.revenue_month || 0),
            description: `Targeting 15% growth`,
            icon: TrendingUp,
            iconColor: 'text-indigo-600',
            bgColor: 'bg-indigo-50'
        },
        {
            title: 'Current Valuation',
            value: formatCurrency(inventoryValue),
            description: `${items.length} unique products`,
            icon: Package,
            iconColor: 'text-blue-600',
            bgColor: 'bg-blue-50'
        },
        {
            title: 'Active Alerts',
            value: lowStockCount,
            description: 'Items below threshold',
            icon: AlertTriangle,
            iconColor: 'text-amber-600',
            bgColor: 'bg-amber-50',
            trend: lowStockCount > 0 ? 'up' : 'none',
            trendValue: lowStockCount
        }
    ];

    const columns = useMemo(() => [
        {
            accessorKey: 'itemName',
            header: 'Product',
            cell: ({ row }) => (
                <div className="flex flex-col">
                    <span className="font-black text-gray-900">{row.original.itemName}</span>
                    <div className="flex items-center gap-2 mt-0.5">
                        <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">{row.original.sku || 'N/A'}</span>
                        {row.original.isControlled && (
                            <Badge className="h-4 px-1.5 bg-rose-100 text-rose-600 text-[9px] font-black uppercase border-none">Controlled</Badge>
                        )}
                    </div>
                </div>
            )
        },
        {
            accessorKey: 'currentStock',
            header: 'On-Hand',
            cell: ({ row }) => (
                <span className={cn(
                    "font-black",
                    row.original.currentStock <= (row.original.minimumStockLevel || 0) ? "text-rose-600" : "text-gray-900"
                )}>
                    {row.original.currentStock} {row.original.unit}
                </span>
            )
        },
        {
            accessorKey: 'retailPrice',
            header: 'Retail Price',
            cell: ({ row }) => <span className="font-bold text-indigo-600">{formatCurrency(row.original.retailPrice || row.original.sellingPrice)}</span>
        },
        {
            id: 'invValue',
            header: 'Inv. Value',
            cell: ({ row }) => {
                const price = Number(row.original.retailPrice || row.original.sellingPrice || 0);
                return <span className="font-bold text-gray-900">{formatCurrency(price * (row.original.currentStock || 0))}</span>
            }
        },
        {
            accessorKey: 'status',
            header: 'Stock Status',
            cell: ({ row }) => {
                let status = 'in_stock';
                if (row.original.currentStock <= 0) status = 'out_of_stock';
                else if (row.original.currentStock <= row.original.minimumStockLevel) status = 'low_stock';
                return <StatusBadge status={status} />
            }
        }
    ], []);

    if (loading) {
        return (
            <PageContainer>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                    {[1, 2, 3, 4].map(i => <div key={i} className="h-32 bg-gray-100 rounded-2xl animate-pulse" />)}
                </div>
            </PageContainer>
        );
    }

    return (
        <PageContainer>
            <PageHeader
                title="Pharmacy Terminal"
                subtitle="End-to-end medical sales and fulfillment oversight."
                breadcrumbs={['Pharmacy', 'Command Center']}
            >
                <div className="flex gap-2">
                    <Button variant="outline" className="h-11 rounded-xl font-bold border-gray-200" onClick={fetchAll} disabled={refreshing}>
                        <RefreshCw className={cn("mr-2 h-4 w-4", refreshing && "animate-spin")} />
                        Sync Data
                    </Button>
                    <Button className="h-11 rounded-xl font-bold bg-indigo-600 hover:bg-indigo-700 shadow-lg shadow-indigo-100" onClick={() => navigate('/pharmacy/pos')}>
                        <Plus className="mr-2 h-4 w-4 stroke-[3px]" />
                        New POS Sale
                    </Button>
                </div>
            </PageHeader>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                {kpis.map((kpi, i) => (
                    <StatsCard key={i} {...kpi} />
                ))}
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mt-8">
                <div className="lg:col-span-2 space-y-6">
                    <Card className="border-none shadow-sm bg-white rounded-3xl overflow-hidden">
                        <CardHeader className="border-b border-gray-50 flex flex-row items-center justify-between px-8 py-6">
                            <CardTitle className="text-base font-black uppercase tracking-tight text-gray-900 flex items-center gap-2">
                                <Package className="h-4.5 w-4.5 text-indigo-600" />
                                Drugs & Supply Monitoring
                            </CardTitle>
                            <Badge className="bg-indigo-50 text-indigo-600 border-none font-bold">Live Catalog</Badge>
                        </CardHeader>
                        <CardContent className="p-0">
                            <DataTable columns={columns} data={items} pageSize={5} searchable />
                        </CardContent>
                    </Card>
                </div>

                <div className="space-y-6">
                    <Card className="border-none shadow-sm bg-white rounded-3xl overflow-hidden">
                        <CardHeader className="border-b border-gray-50 px-8 py-6">
                            <CardTitle className="text-base font-black uppercase tracking-tight text-gray-900 flex items-center gap-2">
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
        </PageContainer>
    );
};

export default PharmacyOverview;
