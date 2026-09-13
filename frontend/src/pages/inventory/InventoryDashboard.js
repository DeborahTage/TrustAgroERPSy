import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { inventoryApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Badge } from '../../components/ui/badge';
import { Button } from '../../components/ui/button';
import { useLanguage } from '../../context/LanguageContext';
import { formatCurrency } from '../../utils';
import {
  Package, AlertTriangle, Clock, Activity, BarChart3, TrendingUp, DollarSign,
  ArrowDownToLine, ArrowUpFromLine, Layers, History,
  ShoppingCart, ShieldAlert, PlusCircle, MinusCircle, FileText, RefreshCw,
  Box, ShieldCheck, Settings, Pill, ArrowRightLeft, ExternalLink, User
} from 'lucide-react';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  BarChart, Bar, Cell, PieChart, Pie
} from 'recharts';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/ui/tabs';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../../components/ui/table';
import { ScrollArea } from '../../components/ui/scroll-area';
import { useAuth } from '../../context/AuthContext';
import { cn } from '../../lib/utils';

export default function InventoryDashboard() {
  const { t } = useLanguage();
  const { user, canPerformAction } = useAuth();
  const navigate = useNavigate();

  const isIM = user?.role === 'STORE_KEEPER' || user?.role === 'ADMIN';

  const { data = {}, isLoading, isError, refetch } = useQuery({
    queryKey: ['inventory-dashboard'],
    queryFn: async () => {
      const res = await inventoryApi.getDashboardStats();
      return res.data || {};
    },
    refetchInterval: 30000,
  });

  const kpiCards = [
    {
      title: 'Gross Inventory Value',
      value: formatCurrency(data.total_value || 0),
      description: 'Working capital in stock',
      icon: DollarSign,
      iconColor: 'text-primary',
      bgColor: 'bg-primary/10',
    },
    {
      title: 'Inventory Turnover',
      value: `${(data.inventory_turnover || 0).toFixed(1)}%`,
      description: 'Stock velocity (30d)',
      icon: RefreshCw,
      iconColor: 'text-emerald-600',
      bgColor: 'bg-emerald-50'
    },
    {
      title: 'Pending Requisitions',
      value: data.pending_requisitions || 0,
      description: 'Unfulfilled internal requests',
      icon: FileText,
      iconColor: 'text-amber-600',
      bgColor: 'bg-amber-50',
      onClick: () => navigate('/inventory/requisitions')
    },
    {
      title: 'Expiry Risk',
      value: data.expiring_count || 0,
      description: 'Items expiring in 30 days',
      icon: AlertTriangle,
      iconColor: 'text-rose-600',
      bgColor: 'bg-rose-50'
    },
    {
      title: 'Low Stock SKU',
      value: data.low_stock_count || 0,
      description: 'Below replenishment point',
      icon: ShoppingCart,
      iconColor: 'text-blue-600',
      bgColor: 'bg-blue-50'
    }
  ];

  const trendData = data.movement_trend?.in?.map((item, idx) => ({
    date: item[0],
    in: item[1] || 0,
    out: data.movement_trend.out?.[idx]?.[1] || 0
  })) || [];

  if (isLoading) {
    return (
      <EnterprisePage
        title="Inventory Control Center"
        subtitle="Operational intelligence and supply chain management for Trust Agro."
        breadcrumbs={['Inventory', 'Dashboard']}
        loading
      />
    );
  }

  return (
    <EnterprisePage
      title="Inventory Control Center"
      subtitle="Operational intelligence and supply chain management for Trust Agro."
      breadcrumbs={['Inventory', 'Dashboard']}
      stats={kpiCards}
      onRefresh={refetch}
      error={isError ? 'Failed to load inventory dashboard.' : null}
      onRetry={refetch}
      actions={
        isIM ? (
          <Button className="h-11 rounded-xl bg-primary hover:bg-primary/90 shadow-lg shadow-primary/10 font-black tracking-tight" onClick={() => navigate('/inventory/stock-in')}>
            <PlusCircle className="mr-2 h-5 w-5 stroke-[3px]" />
            Record Reception (GRN)
          </Button>
        ) : null
      }
    >
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-8">
        {/* Left Column: Movement Analytics */}
        <div className="xl:col-span-2 space-y-8">
          <Card className="border-none shadow-sm bg-white rounded-[2rem] overflow-hidden">
            <CardHeader className="bg-white py-6 border-b border-slate-50 px-8">
              <div className="flex justify-between items-center">
                <div>
                  <CardTitle className="text-xl font-black tracking-tight text-slate-900">Supply Chain Flow</CardTitle>
                  <CardDescription className="text-xs font-bold text-slate-400 uppercase tracking-widest mt-1">Inbound vs Outbound Movement Trend</CardDescription>
                </div>
                <div className="flex gap-4">
                  <div className="flex items-center gap-2">
                    <div className="h-3 w-3 rounded-full bg-primary" />
                    <span className="text-[10px] font-black uppercase tracking-widest text-slate-500">Inbound</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="h-3 w-3 rounded-full bg-rose-500" />
                    <span className="text-[10px] font-black uppercase tracking-widest text-slate-500">Outbound</span>
                  </div>
                </div>
              </div>
            </CardHeader>
            <CardContent className="p-8">
              <div className="h-[300px] w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={trendData}>
                    <defs>
                      <linearGradient id="colorIn" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#4f46e5" stopOpacity={0.15} />
                        <stop offset="95%" stopColor="#4f46e5" stopOpacity={0} />
                      </linearGradient>
                      <linearGradient id="colorOut" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#f43f5e" stopOpacity={0.15} />
                        <stop offset="95%" stopColor="#f43f5e" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                    <XAxis dataKey="date" axisLine={false} tickLine={false} tick={{ fontSize: 10, fontWeight: 700, fill: '#94a3b8' }} dy={10} />
                    <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 10, fontWeight: 700, fill: '#94a3b8' }} />
                    <Tooltip
                      contentStyle={{ borderRadius: '1rem', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }}
                      itemStyle={{ fontWeight: 800, fontSize: '12px' }}
                    />
                    <Area type="monotone" dataKey="in" stroke="#4f46e5" strokeWidth={4} fill="url(#colorIn)" fillOpacity={1} />
                    <Area type="monotone" dataKey="out" stroke="#f43f5e" strokeWidth={4} fill="url(#colorOut)" fillOpacity={1} />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </CardContent>
          </Card>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            <Card className="border-none shadow-sm bg-white rounded-[2rem] overflow-hidden">
              <CardHeader className="p-6 border-b border-slate-50">
                <CardTitle className="text-sm font-black uppercase tracking-widest text-slate-400">Stock Distribution</CardTitle>
              </CardHeader>
              <CardContent className="p-8 flex flex-col items-center">
                <div className="h-[220px] w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={data.category_stats?.map((c) => ({ name: c.category, value: c.value })) || []}
                        innerRadius={70}
                        outerRadius={90}
                        paddingAngle={8}
                        dataKey="value"
                        stroke="none"
                      >
                        {(data.category_stats || []).map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={['#6366f1', '#10b981', '#f59e0b', '#3b82f6'][index % 4]} cornerRadius={8} />
                        ))}
                      </Pie>
                      <Tooltip />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              </CardContent>
            </Card>

            <Card className="border-none shadow-sm bg-white rounded-[2rem] overflow-hidden">
              <CardHeader className="p-6 border-b border-slate-50 flex flex-row justify-between items-center">
                <CardTitle className="text-sm font-black uppercase tracking-widest text-slate-400">Low Stock Registry</CardTitle>
                <Badge className="bg-rose-50 text-rose-600 border-none font-black text-[9px] uppercase tracking-tighter">Critical Replenishment</Badge>
              </CardHeader>
              <CardContent className="p-0">
                <ScrollArea className="h-[280px]">
                  <div className="p-4 space-y-4">
                    {(data.low_stock || []).map((item, idx) => (
                      <div key={idx} className="flex justify-between items-center p-3 rounded-2xl bg-slate-50">
                        <div className="flex items-center gap-3">
                          <div className="h-10 w-10 rounded-xl bg-white flex items-center justify-center shadow-sm">
                            <Package className="h-5 w-5 text-slate-400" />
                          </div>
                          <div className="flex flex-col">
                            <span className="font-bold text-slate-900 text-sm">{item.itemName}</span>
                            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">{item.sku}</span>
                          </div>
                        </div>
                        <div className="text-right">
                          <div className="text-sm font-black text-rose-600">{item.currentStock} / {item.reorderPoint}</div>
                          <div className="text-[9px] font-bold text-slate-400 uppercase tracking-widest">{item.unit || 'Units'}</div>
                        </div>
                      </div>
                    ))}
                  </div>
                </ScrollArea>
              </CardContent>
            </Card>
          </div>
        </div>

        {/* Right Column: Activity & Transfers */}
        <div className="space-y-8">
          <Card className="border-none shadow-sm bg-slate-900 text-white rounded-[2rem] overflow-hidden">
            <CardHeader className="p-8 pb-4">
              <CardTitle className="text-xs font-black uppercase tracking-[0.2em] text-slate-400">Commander Palette</CardTitle>
            </CardHeader>
            <CardContent className="p-8 pt-0 grid grid-cols-1 gap-4">
              <Button
                variant="secondary"
                className="h-20 rounded-2xl bg-slate-800 border-none hover:bg-slate-700 justify-start px-6 group transition-all"
                onClick={() => navigate('/inventory/requisitions')}
              >
                <div className="h-10 w-10 rounded-xl bg-primary/100 flex items-center justify-center mr-4 group-hover:scale-110 transition-transform">
                  <ArrowRightLeft className="h-5 w-5 text-white" />
                </div>
                <div className="flex flex-col items-start">
                  <span className="text-sm font-black text-white">REQUISITIONS</span>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Approve Internal Orders</span>
                </div>
                <ExternalLink className="ml-auto h-4 w-4 text-slate-500 opacity-0 group-hover:opacity-100 transition-opacity" />
              </Button>

              <Button
                variant="secondary"
                className="h-20 rounded-2xl bg-slate-800 border-none hover:bg-slate-700 justify-start px-6 group transition-all"
                onClick={() => navigate('/inventory/reports')}
              >
                <div className="h-10 w-10 rounded-xl bg-emerald-500 flex items-center justify-center mr-4 group-hover:scale-110 transition-transform">
                  <BarChart3 className="h-5 w-5 text-white" />
                </div>
                <div className="flex flex-col items-start">
                  <span className="text-sm font-black text-white">REPORTS</span>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Financial Valuation</span>
                </div>
                <ExternalLink className="ml-auto h-4 w-4 text-slate-500 opacity-0 group-hover:opacity-100 transition-opacity" />
              </Button>
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm bg-white rounded-[2rem] overflow-hidden">
            <CardHeader className="p-8 pb-4">
              <CardTitle className="text-sm font-black text-slate-900 flex items-center gap-3">
                <History className="h-5 w-5 text-primary" /> Operational Log
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <ScrollArea className="h-[520px]">
                <div className="p-8 space-y-8 relative">
                  <div className="absolute left-[3.15rem] top-12 bottom-8 w-[2px] bg-slate-50" />
                  {data.activity_timeline?.map((log, i) => (
                    <div key={i} className="flex gap-6 relative">
                      <div className={cn(
                        "h-6 w-6 rounded-full border-[6px] border-white z-10 mt-1 shadow-sm shrink-0",
                        log.action === 'RECEIPT' ? 'bg-emerald-500' :
                          log.action === 'SALE' ? 'bg-primary/100' :
                            log.action === 'REQUISITION' ? 'bg-amber-500' : 'bg-slate-300'
                      )} />
                      <div className="flex flex-col gap-1">
                        <div className="flex items-center gap-3">
                          <span className="text-[11px] font-black text-slate-900 leading-none">{log.action}</span>
                          <span className="text-[10px] font-bold text-slate-400">{new Date(log.time).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                        </div>
                        <p className="text-sm font-medium text-slate-600 leading-tight">
                          <span className="font-black text-slate-900">{log.qty} Units</span> of {log.item} processed.
                        </p>
                        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest flex items-center gap-1">
                          <User className="h-3 w-3" /> {log.user}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </ScrollArea>
            </CardContent>
          </Card>
        </div>
      </div>
    </EnterprisePage>
  );
}
