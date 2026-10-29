import React, { useState, useEffect } from 'react';
import axios from '../../api/axios';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS, canPerformAction } from '../../lib/permissions';

function StatCard({ title, value, color }) {
    const bg = {
        green: 'bg-emerald-50 border-emerald-200 dark:bg-emerald-950/30 dark:border-emerald-800/40',
        yellow: 'bg-amber-50 border-amber-200 dark:bg-amber-950/30 dark:border-amber-800/40',
        red: 'bg-rose-50 border-rose-200 dark:bg-rose-950/30 dark:border-rose-800/40',
        orange: 'bg-orange-50 border-orange-200 dark:bg-orange-950/30 dark:border-orange-800/40'
    };
    const text = {
        green: 'text-emerald-800 dark:text-emerald-400',
        yellow: 'text-amber-800 dark:text-amber-400',
        red: 'text-rose-800 dark:text-rose-400',
        orange: 'text-orange-800 dark:text-orange-400'
    };
    return (
        <div className={`p-4 rounded-xl shadow-sm border ${bg[color] || 'erp-card'}`}>
            <h3 className="text-muted-foreground text-xs font-semibold uppercase tracking-wider">{title}</h3>
            <p className={`text-2xl font-bold mt-1 ${text[color] || 'text-foreground'}`}>{value}</p>
        </div>
    );
}

export default function InventoryOverview() {
    const { user } = useAuth();
    const [stats, setStats] = useState({});
    const [lowStock, setLowStock] = useState([]);
    const [expiring, setExpiring] = useState([]);
    const [movements, setMovements] = useState([]);

    useEffect(() => {
        Promise.all([
            axios.get('/api/inventory/stats'),
            axios.get('/api/inventory/low-stock'),
            axios.get('/api/inventory/expiring'),
            axios.get('/api/inventory/movements/recent')
        ]).then(([s, l, e, m]) => {
            setStats(s.data || {});
            setLowStock(l.data || []);
            setExpiring(e.data || []);
            setMovements((m.data || []).slice(0, 7).map(mv => ({
                name: mv.item?.itemName || 'Unknown',
                qty: mv.quantity || 0
            })));
        }).catch(err => console.error(err));
    }, []);

    const createQuickPO = (itemId) => alert(`Creating draft PO for item ${itemId}...`);

    return (
        <div className="p-6 space-y-6">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-black text-foreground tracking-tight">Inventory Command Center</h1>
                    <p className="text-xs font-medium text-muted-foreground mt-0.5">Real-time stock alerts and warehouse replenishment overview</p>
                </div>
                <div className="flex gap-2">
                    {canPerformAction(user?.role, 'stockIn', 'inventoryItems') && (
                        <>
                            <a href="/inventory/scan" className="bg-primary text-primary-foreground px-4 py-2 rounded-xl text-sm font-semibold hover:opacity-90 transition-opacity">Scan Station</a>
                            <a href="/inventory/stock-in" className="bg-emerald-600 text-white px-4 py-2 rounded-xl text-sm font-semibold hover:bg-emerald-700 transition-colors">Stock In</a>
                        </>
                    )}
                </div>
            </div>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <StatCard title="Total SKUs" value={stats.totalSkus || 0} color="green" />
                <StatCard title="Total Value" value={`${(stats.totalValue || 0).toFixed(2)} Birr`} color="green" />
                <StatCard title="Low Stock Alerts" value={stats.lowStockCount || 0} color="red" />
                <StatCard title="Expiring (30d)" value={stats.expiringCount || 0} color="orange" />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="erp-card rounded-2xl border border-border p-5">
                    <h2 className="text-base font-bold text-foreground mb-4">Recent Stock Movements</h2>
                    <ResponsiveContainer width="100%" height={220}>
                        <BarChart data={movements}>
                            <XAxis dataKey="name" tick={{ fontSize: 10, fill: 'currentColor' }} stroke="currentColor" className="text-muted-foreground" />
                            <YAxis tick={{ fontSize: 10, fill: 'currentColor' }} stroke="currentColor" className="text-muted-foreground" />
                            <Tooltip
                                contentStyle={{
                                    backgroundColor: 'var(--card, #0f172a)',
                                    borderColor: 'var(--border, #334155)',
                                    borderRadius: '0.75rem',
                                    color: 'var(--foreground, #fff)'
                                }}
                            />
                            <Bar dataKey="qty" fill="#3b82f6" radius={[4, 4, 0, 0]} />
                        </BarChart>
                    </ResponsiveContainer>
                </div>

                <div className="erp-card rounded-2xl border border-border p-5">
                    <h2 className="text-base font-bold mb-4 text-rose-600 dark:text-rose-400 flex items-center gap-2">
                        <span>⚠ Critical Stock Alerts</span>
                    </h2>
                    <div className="overflow-y-auto max-h-56">
                        <table className="w-full text-left text-sm">
                            <thead className="erp-table-head"><tr className="border-b border-border"><th className="py-2 px-2 text-xs font-semibold text-muted-foreground uppercase">SKU</th><th className="py-2 px-2 text-xs font-semibold text-muted-foreground uppercase">Item</th><th className="py-2 px-2 text-xs font-semibold text-muted-foreground uppercase">Stock</th><th className="py-2 px-2 text-xs font-semibold text-muted-foreground uppercase">Reorder At</th><th className="py-2 px-2"></th></tr></thead>
                            <tbody className="divide-y divide-border">
                                {lowStock.map(item => (
                                    <tr key={item.id} className="hover:bg-muted/40 transition-colors">
                                        <td className="py-2.5 px-2 font-mono text-xs text-muted-foreground">{item.sku}</td>
                                        <td className="py-2.5 px-2 font-medium text-foreground">{item.itemName}</td>
                                        <td className="py-2.5 px-2 font-bold text-rose-600 dark:text-rose-400">{item.currentStock}</td>
                                        <td className="py-2.5 px-2 text-muted-foreground">{item.reorderPoint}</td>
                                        <td className="py-2.5 px-2 text-right">
                                            <button onClick={() => createQuickPO(item.id)} className="text-primary hover:underline text-xs font-semibold">Create PO</button>
                                        </td>
                                    </tr>
                                ))}
                                {lowStock.length === 0 && <tr><td colSpan="5" className="py-6 text-center text-muted-foreground">All stock levels healthy ✓</td></tr>}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <div className="erp-card rounded-2xl border border-border p-5">
                <h2 className="text-base font-bold mb-4 text-amber-600 dark:text-amber-400 flex items-center gap-2">
                    <span>⏰ Expiring Batches (Next 30 Days)</span>
                </h2>
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead className="erp-table-head"><tr className="border-b border-border"><th className="py-2 px-3 text-xs font-semibold text-muted-foreground uppercase">Item</th><th className="py-2 px-3 text-xs font-semibold text-muted-foreground uppercase">Batch #</th><th className="py-2 px-3 text-xs font-semibold text-muted-foreground uppercase">Qty Remaining</th><th className="py-2 px-3 text-xs font-semibold text-muted-foreground uppercase">Expiry Date</th><th className="py-2 px-3 text-xs font-semibold text-muted-foreground uppercase">Location</th></tr></thead>
                        <tbody className="divide-y divide-border">
                            {expiring.map(b => (
                                <tr key={b.id} className="hover:bg-muted/40 transition-colors">
                                    <td className="py-2.5 px-3 font-medium text-foreground">{b.item?.itemName}</td>
                                    <td className="py-2.5 px-3 font-mono text-xs text-muted-foreground">{b.batchNumber}</td>
                                    <td className="py-2.5 px-3 text-foreground font-semibold">{b.quantityRemaining}</td>
                                    <td className="py-2.5 px-3 text-amber-600 dark:text-amber-400 font-semibold">{b.expiryDate}</td>
                                    <td className="py-2.5 px-3 text-muted-foreground">{b.storageZone}</td>
                                </tr>
                            ))}
                            {expiring.length === 0 && <tr><td colSpan="5" className="py-6 text-center text-muted-foreground">No items expiring within 30 days ✓</td></tr>}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}
