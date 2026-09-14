import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { inventoryApi } from '../../api';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import PageHeader from '../../components/shared/PageHeader';
import StatsCard from '../../components/shared/StatsCard';
import PageContainer from '../../components/layout/PageContainer';
import { useAuth } from '../../context/AuthContext';
import { formatDate, formatCurrency } from '../../utils';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/ui/tabs';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import {
  Package,
  AlertTriangle,
  Clock,
  ArrowUp,
  ArrowDown,
  Plus,
  DollarSign,
  MoreVertical,
  Edit,
  History
} from 'lucide-react';
import { cn } from '../../lib/utils';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "../../components/ui/dropdown-menu";

const InventoryItemList = () => {
  const { canPerformAction } = useAuth();
  const [items, setItems] = useState([]);
  const [lowStock, setLowStock] = useState([]);
  const [expiry, setExpiry] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    setLoading(true);
    Promise.all([
      inventoryApi.getItems(),
      inventoryApi.getLowStock(),
      inventoryApi.getExpiryAlerts(),
    ]).then(([itemsRes, lowStockRes, expiryRes]) => {
      setItems(itemsRes.data.data || []);
      setLowStock(lowStockRes.data.data || []);
      setExpiry(expiryRes.data.data || []);
    }).finally(() => setLoading(false));
  }, []);

  const totalValue = items.reduce((sum, i) => sum + (Number(i.currentStock || 0) * Number(i.unitCost || 0)), 0);

  const kpiCards = [
    { title: 'Active SKUs', value: items.length, icon: Package },
    { title: 'Low Stock', value: lowStock.length, icon: AlertTriangle, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    { title: 'Expiry Alerts', value: expiry.length, icon: Clock, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: 'Inventory Value', value: formatCurrency(totalValue), icon: DollarSign, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' }
  ];

  const columns = useMemo(() => [
    {
      accessorKey: 'itemName',
      header: 'Product Details',
      cell: ({ row }) => (
        <div className="flex flex-col">
          <span className="font-black text-gray-900 truncate max-w-[200px]">{row.original.itemName}</span>
          <span className="text-[10px] font-black text-primary uppercase tracking-widest">{row.original.category}</span>
        </div>
      )
    },
    {
      accessorKey: 'currentStock',
      header: 'Availability',
      cell: ({ row }) => (
        <div className="flex items-center gap-3">
          <div className="flex flex-col">
            <span className={cn(
              "text-sm font-black",
              row.original.currentStock <= row.original.minimumStockLevel ? "text-rose-600" : "text-gray-900"
            )}>
              {row.original.currentStock} {row.original.unit}
            </span>
            <span className="text-[10px] text-gray-400 font-bold uppercase">Min: {row.original.minimumStockLevel}</span>
          </div>
        </div>
      )
    },
    {
      accessorKey: 'unitCost',
      header: 'Wholesale Price',
      cell: ({ row }) => <span className="font-bold text-gray-600">{formatCurrency(row.original.unitCost)}</span>
    },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ row }) => {
        let status = 'in_stock';
        if (row.original.currentStock <= 0) status = 'out_of_stock';
        else if (row.original.currentStock <= row.original.minimumStockLevel) status = 'low_stock';
        return <StatusBadge status={status} />
      }
    },
    {
      id: 'actions',
      cell: ({ row }) => (
        <div className="flex justify-end">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="sm" className="h-9 w-9 p-0 rounded-xl hover:bg-gray-100 transition-all">
                <MoreVertical className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56 p-2 rounded-2xl border-none shadow-2xl bg-white/95 backdrop-blur-md">
              {canPerformAction('edit', 'inventoryItems') && (
                <DropdownMenuItem onClick={() => navigate(`/inventory/items/${row.original.id}/edit`)} className="rounded-xl p-3 cursor-pointer hover:bg-primary/10 hover:text-primary transition-all">
                  <Edit className="mr-3 h-4 w-4" />
                  <span className="font-bold text-sm">Edit Details</span>
                </DropdownMenuItem>
              )}
              <DropdownMenuItem onClick={() => navigate('/inventory/stock-in', { state: { itemId: row.original.id } })} className="rounded-xl p-3 cursor-pointer hover:bg-emerald-50 hover:text-emerald-600 transition-all">
                <ArrowUp className="mr-3 h-4 w-4" />
                <span className="font-bold text-sm">Receive Stock</span>
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => navigate('/inventory/stock-out', { state: { itemId: row.original.id } })} className="rounded-xl p-3 cursor-pointer hover:bg-rose-50 hover:text-rose-600 transition-all">
                <ArrowDown className="mr-3 h-4 w-4" />
                <span className="font-bold text-sm">Issue Usage</span>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      )
    }
  ], [navigate, canPerformAction]);

  return (
    <PageContainer>
      <PageHeader
        title="Inventory Registry"
        subtitle="Manage and monitor all pharmaceutical and farm supplies."
        breadcrumbs={['Inventory', 'Master Registry']}
      >
        <div className="flex gap-2">
          <Button variant="outline" className="rounded-xl h-11 px-5 font-bold border-gray-200 hover:bg-gray-50" onClick={() => navigate('/inventory/reports')}>
            <History className="mr-2 h-4 w-4 text-primary" />
            Audit Log
          </Button>
          <Button className="bg-slate-900 hover:bg-slate-800 text-white rounded-xl h-11 px-5 font-bold shadow-lg shadow-slate-200 transition-all active:scale-95" onClick={() => navigate('/inventory/items/new')}>
            <Plus className="mr-2 h-4 w-4" />
            Add New Item
          </Button>
        </div>
      </PageHeader>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {kpiCards.map((card, index) => (
          <StatsCard key={index} {...card} />
        ))}
      </div>

      <div className="mt-8 space-y-6">
        <Tabs defaultValue="all" className="w-full">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
            <TabsList className="bg-gray-100/50 p-1.5 rounded-2xl h-auto border border-gray-100">
              <TabsTrigger value="all" className="data-[state=active]:bg-white data-[state=active]:text-primary data-[state=active]:shadow-sm rounded-xl px-6 py-2 font-black text-[11px] uppercase tracking-wider transition-all">
                All Products
              </TabsTrigger>
              <TabsTrigger value="low" className="data-[state=active]:bg-white data-[state=active]:text-rose-600 data-[state=active]:shadow-sm rounded-xl px-6 py-2 font-black text-[11px] uppercase tracking-wider transition-all">
                Low Stock
                {lowStock.length > 0 && <Badge className="ml-2 bg-rose-100 text-rose-600 border-none font-black">{lowStock.length}</Badge>}
              </TabsTrigger>
              <TabsTrigger value="expiry" className="data-[state=active]:bg-white data-[state=active]:text-amber-600 data-[state=active]:shadow-sm rounded-xl px-6 py-2 font-black text-[11px] uppercase tracking-wider transition-all">
                Expiring
                {expiry.length > 0 && <Badge className="ml-2 bg-amber-100 text-amber-600 border-none font-black">{expiry.length}</Badge>}
              </TabsTrigger>
            </TabsList>

            <div className="flex gap-2">
              {/* Search/Filter actions could go here */}
            </div>
          </div>

          <TabsContent value="all" className="focus-visible:outline-none focus-visible:ring-0">
            <DataTable columns={columns} data={items} loading={loading} />
          </TabsContent>

          <TabsContent value="low" className="focus-visible:outline-none focus-visible:ring-0">
            <DataTable columns={columns} data={lowStock} loading={loading} />
          </TabsContent>

          <TabsContent value="expiry" className="focus-visible:outline-none focus-visible:ring-0">
            <DataTable
              columns={[
                { accessorKey: 'item.itemName', header: 'Product', cell: ({ row }) => <span className="font-black">{row.original.item?.itemName}</span> },
                { accessorKey: 'batchNumber', header: 'Batch' },
                { accessorKey: 'quantityRemaining', header: 'Rem. Qty', cell: ({ row }) => <span className="font-bold">{row.original.quantityRemaining}</span> },
                { accessorKey: 'expiryDate', header: 'Expiry', cell: ({ row }) => <span className="font-black text-rose-600">{formatDate(row.original.expiryDate)}</span> },
                { id: 'status', header: 'Status', cell: () => <StatusBadge status="expiring_soon" /> }
              ]}
              data={expiry}
              loading={loading}
            />
          </TabsContent>
        </Tabs>
      </div>
    </PageContainer>
  );
};

export default InventoryItemList;
