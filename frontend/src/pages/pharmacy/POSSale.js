import React, { useState, useEffect } from 'react';
import axios from '../../api/axios';
import { MASTER_CATALOG_ITEMS } from '../../utils/masterCatalog';

export default function POSSale() {
    const [cart, setCart] = useState([]);
    const [customer, setCustomer] = useState({ name: '', phone: '' });
    const [paymentMethod, setPaymentMethod] = useState('Cash');
    const [sellableItems, setSellableItems] = useState([]);

    useEffect(() => {
        // Fetch inventory items that are drugs/vaccines using the specialized endpoint
        axios.get('/api/inventory/pharmacy-items')
            .then(res => {
                const data = res.data;
                const items = Array.isArray(data) ? data : (data?.data || []);
                setSellableItems(items.length > 0 ? items : MASTER_CATALOG_ITEMS);
            })
            .catch(e => {
                console.error('Failed to load pharmacy items:', e);
                setSellableItems(MASTER_CATALOG_ITEMS);
            });
    }, []);

    const addToCart = (item) => {
        const defaultPrice = Number(item.retailPrice || item.sellingPrice || item.avgUnitCost || 0);
        const existing = cart.find(c => c.id === item.id);
        if (existing) {
            setCart(cart.map(c => c.id === item.id ? { ...c, cartQty: c.cartQty + 1 } : c));
        } else {
            setCart([...cart, {
                ...item,
                cartQty: 1,
                unitPrice: defaultPrice,
                unit: item.unit || item.unitString || 'PIECE'
            }]);
        }
    };

    const updateCartPrice = (id, newPrice) => {
        const val = parseFloat(newPrice) || 0;
        setCart(cart.map(c => c.id === id ? { ...c, unitPrice: val } : c));
    };

    const updateCartQty = (id, newQty) => {
        const val = Math.max(1, parseInt(newQty, 10) || 1);
        setCart(cart.map(c => c.id === id ? { ...c, cartQty: val } : c));
    };

    const handleCheckout = async () => {
        try {
            const payload = {
                items: cart.map(c => ({
                    itemId: c.id,
                    qty: c.cartQty,
                    unitPrice: c.unitPrice
                })),
                customerName: customer.name,
                customerPhone: customer.phone,
                paymentMethod
            };

            const res = await axios.post('/api/pharmacy/sales', payload);
            alert(`Sale complete! Receipt: ${res.data.saleCode}`);
            setCart([]);
            setCustomer({ name: '', phone: '' });
        } catch (e) {
            alert("Checkout failed. Error: " + (e.response?.data?.message || e.message));
        }
    };

    const total = cart.reduce((sum, item) => sum + item.unitPrice * item.cartQty, 0);

    return (
        <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-6 items-start">
            <div className="erp-card rounded-2xl border border-border p-5">
                <h2 className="text-xl font-bold mb-4 text-foreground">Inventory Items</h2>
                <input
                    placeholder="Search OTC drugs/vaccines..."
                    className="w-full bg-background border border-border p-2.5 rounded-xl mb-4 text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                />
                <div className="space-y-3 max-h-96 overflow-y-auto pr-2">
                    {sellableItems.map(item => (
                        <div key={item.id} className="border border-border p-3 rounded-xl flex justify-between items-center bg-muted/40 hover:bg-muted/70 transition-colors">
                            <div>
                                <div className="font-semibold text-foreground">{item.itemName}</div>
                                <div className="text-xs text-muted-foreground">Stock: {item.currentStock || 0} {item.unit} | SKU: {item.sku}</div>
                            </div>
                            <button
                                onClick={() => addToCart(item)}
                                className="bg-primary text-primary-foreground font-semibold px-3 py-1.5 rounded-lg text-sm hover:opacity-90 disabled:opacity-50 transition"
                                disabled={(item.currentStock || 0) <= 0}
                            >
                                Add+
                            </button>
                        </div>
                    ))}
                    {sellableItems.length === 0 && <p className="text-muted-foreground text-center py-4">No inventory found.</p>}
                </div>
            </div>

            <div className="erp-card rounded-2xl border border-border p-6">
                <h2 className="text-xl font-bold mb-4 text-foreground">Cart Checkout</h2>

                <div className="min-h-32 divide-y divide-border">
                    {cart.map((item, i) => (
                        <div key={i} className="flex justify-between items-center py-2.5 gap-2">
                            <div className="flex-1 min-w-0">
                                <div className="font-medium text-foreground truncate">{item.itemName}</div>
                                <div className="text-xs text-muted-foreground">{item.unit || 'PCS'}</div>
                            </div>
                            <div className="flex items-center gap-1.5">
                                <input
                                    type="number"
                                    min="1"
                                    value={item.cartQty}
                                    onChange={e => updateCartQty(item.id, e.target.value)}
                                    className="w-14 bg-background border border-border p-1 rounded text-center text-sm font-bold text-foreground"
                                    title="Quantity"
                                />
                                <span className="text-xs text-muted-foreground">×</span>
                                <input
                                    type="number"
                                    step="any"
                                    value={item.unitPrice}
                                    onChange={e => updateCartPrice(item.id, e.target.value)}
                                    className="w-20 bg-background border border-border p-1 rounded text-right text-sm font-bold text-emerald-600 dark:text-emerald-400"
                                    title="Unit price in ETB (editable)"
                                />
                                <span className="text-[11px] text-muted-foreground font-semibold">ETB</span>
                            </div>
                            <span className="font-semibold text-foreground text-sm min-w-20 text-right">
                                {(item.unitPrice * item.cartQty).toFixed(2)}
                            </span>
                        </div>
                    ))}
                    {cart.length === 0 && <p className="text-muted-foreground py-6 text-center">Cart is empty</p>}
                </div>

                <div className="text-xl font-bold mt-6 mb-4 flex justify-between text-foreground border-t border-border pt-4">
                    <span>Total:</span>
                    <span className="text-emerald-600 dark:text-emerald-400">{total.toFixed(2)} Birr</span>
                </div>

                <div className="mt-4 space-y-3">
                    <input
                        placeholder="Customer Name"
                        value={customer.name}
                        onChange={e => setCustomer({ ...customer, name: e.target.value })}
                        className="w-full bg-background border border-border p-2.5 rounded-xl text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                    />
                    <input
                        placeholder="Phone Number"
                        value={customer.phone}
                        onChange={e => setCustomer({ ...customer, phone: e.target.value })}
                        className="w-full bg-background border border-border p-2.5 rounded-xl text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                    />
                    <select
                        value={paymentMethod}
                        onChange={e => setPaymentMethod(e.target.value)}
                        className="w-full bg-background border border-border p-2.5 rounded-xl text-foreground focus:outline-none focus:ring-2 focus:ring-primary font-medium"
                    >
                        <option value="Cash">Cash</option>
                        <option value="Bank Transfer">Bank Transfer</option>
                    </select>
                </div>

                <button
                    onClick={handleCheckout}
                    disabled={cart.length === 0}
                    className="bg-emerald-600 text-white font-bold text-base w-full mt-6 py-3 rounded-xl hover:bg-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed transition shadow-md">
                    Complete Sale & Print Receipt
                </button>
            </div>
        </div>
    );
}
