import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { crmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent } from '../../components/ui/card';
import { Badge } from '../../components/ui/badge';
import { ShoppingBag, Truck, CheckCircle, Clock } from 'lucide-react';
import { formatCurrency } from '../../utils';

const STATUS_COLORS = {
  DELIVERED: 'bg-emerald-100 text-emerald-700',
  SHIPPED: 'bg-blue-100 text-blue-700',
  PENDING: 'bg-amber-100 text-amber-700',
};

const OrderDashboard = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await crmApi.getOrders();
      setOrders(res.data.data || []);
    } catch {
      setError('Failed to load customer orders.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  const stats = useMemo(() => {
    const pending = orders.filter((o) => o.status === 'PENDING').length;
    const shipped = orders.filter((o) => o.status === 'SHIPPED').length;
    const delivered = orders.filter((o) => o.status === 'DELIVERED').length;
    const revenue = orders.reduce((sum, o) => sum + (Number(o.totalAmount) || 0), 0);
    return { pending, shipped, delivered, revenue };
  }, [orders]);

  const kpiCards = [
    { title: 'Total Orders', value: orders.length, icon: ShoppingBag, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Pending', value: stats.pending, icon: Clock, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: 'In Transit', value: stats.shipped, icon: Truck, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: 'Delivered', value: stats.delivered, icon: CheckCircle, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
  ];

  return (
    <EnterprisePage
      title="Sales & Orders"
      subtitle="Customer egg and meat orders with live delivery status tracking"
      breadcrumbs={['CRM', 'Orders']}
      loading={loading}
      error={error}
      onRetry={fetchOrders}
      onRefresh={fetchOrders}
      stats={kpiCards}
      empty={!loading && orders.length === 0 ? { title: 'No orders yet', description: 'Customer orders will appear here once created in the CRM module.' } : null}
    >
      {orders.length > 0 && (
        <div className="space-y-4">
          <div className="flex justify-end text-sm text-slate-500">
            Pipeline value: <span className="ml-2 font-bold text-primary">{formatCurrency(stats.revenue)}</span>
          </div>
          {orders.map((order) => (
            <Card key={order.id} className="border shadow-sm group hover:shadow-md transition-shadow overflow-hidden">
              <CardContent className="flex flex-col md:flex-row items-center p-6 gap-6">
                <div className="h-16 w-16 bg-gray-50 rounded-2xl flex items-center justify-center text-primary">
                  <ShoppingBag className="h-8 w-8" />
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-3 flex-wrap">
                    <h3 className="text-xl font-bold text-gray-900">Order #{order.id}</h3>
                    <Badge className={STATUS_COLORS[order.status] || 'bg-gray-100 text-gray-700'}>{order.status}</Badge>
                  </div>
                  <p className="text-sm text-gray-500 mt-1">
                    {order.client?.clientName || 'Unknown client'} • {order.orderDate}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-xs font-semibold uppercase tracking-wide text-gray-400">Amount</p>
                  <p className="text-xl font-bold text-primary">{formatCurrency(order.totalAmount)}</p>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </EnterprisePage>
  );
};

export default OrderDashboard;
