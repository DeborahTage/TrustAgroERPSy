import React, { useState, useEffect } from 'react';
import axios from '../../api/axios';

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
                setSellableItems(Array.isArray(data) ? data : []);
            })
            .catch(e => {
                console.error('Failed to load pharmacy items:', e);
                setSellableItems([]);
            });
    }, []);

    const addToCart = (item) => {
        // Basic POS cart add
        setCart([...cart, { ...item, cartQty: 1, lineTotal: (item.avgUnitCost || 100) * 1.5 }]);
    };

    const handleCheckout = async () => {
        try {
            const payload = {
                items: cart.map(c => ({
                    itemId: c.id,
                    qty: c.cartQty,
                    unitPrice: c.lineTotal
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

    const total = cart.reduce((sum, item) => sum + item.lineTotal * item.cartQty, 0);

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
                        <div key={i} className="flex justify-between py-2.5">
                            <span className="font-medium text-foreground">{item.itemName} <span className="text-xs text-muted-foreground">x{item.cartQty}</span></span>
                            <span className="font-semibold text-foreground">{item.lineTotal.toFixed(2)} Birr</span>
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
