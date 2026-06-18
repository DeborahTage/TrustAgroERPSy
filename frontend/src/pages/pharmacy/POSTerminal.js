import React, { useState, useEffect, useCallback } from 'react';
import {
    Search, ShoppingCart, User, CreditCard, Receipt, Trash2,
    Plus, Minus, AlertTriangle, CheckCircle2, Package, Pill,
    Zap, Filter, RefreshCw, ClipboardCheck, Info
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription, CardFooter } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Badge } from '../../components/ui/badge';
import { Label } from '../../components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../../components/ui/table';
import { ScrollArea } from '../../components/ui/scroll-area';
import {
    Dialog, DialogContent, DialogHeader, DialogTitle,
    DialogDescription, DialogFooter
} from '../../components/ui/dialog';
import {
    Select, SelectContent, SelectItem, SelectTrigger, SelectValue
} from '../../components/ui/select';
import { Tabs, TabsList, TabsTrigger } from '../../components/ui/tabs';
import { inventoryApi, pharmacyApi } from '../../api';
import { formatCurrency } from '../../utils';
import { toast } from 'react-toastify';
import { cn } from '../../lib/utils';

const CATEGORY_TABS = ['ALL', 'DRUG', 'VACCINE', 'VITAMIN', 'EQUIPMENT', 'FEED', 'FARM_MATERIAL'];

const getCatIcon = (cat) => {
    switch (cat) {
        case 'DRUG': return '💊';
        case 'VACCINE': return '💉';
        case 'VITAMIN': return '🧪';
        case 'EQUIPMENT': return '🔧';
        case 'FEED': return '🌾';
        default: return '📦';
    }
};

const POSTerminal = () => {
    const [products, setProducts] = useState([]);
    const [customers, setCustomers] = useState([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [selectedCategory, setSelectedCategory] = useState('ALL');
    const [cart, setCart] = useState([]);
    const [selectedCustomer, setSelectedCustomer] = useState(null);
    const [paymentMethod, setPaymentMethod] = useState('CASH');
    const [prescriptionNumber, setPrescriptionNumber] = useState('');
    const [showCheckout, setShowCheckout] = useState(false);
    const [showRxWarning, setShowRxWarning] = useState(false);
    const [blockedItem, setBlockedItem] = useState(null);
    const [processing, setProcessing] = useState(false);
    const [loading, setLoading] = useState(true);
    const [lastReceipt, setLastReceipt] = useState(null);

    const fetchData = useCallback(async () => {
        setLoading(true);
        try {
            const [prodRes, custRes] = await Promise.all([
                inventoryApi.getItems(),
                pharmacyApi.getCustomers()
            ]);
            setProducts(prodRes.data?.data || []);
            setCustomers(custRes.data?.data || []);
        } catch (e) {
            toast.error('Failed to sync pharmacy data');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { fetchData(); }, [fetchData]);

    const getPriceForCustomer = (product) => {
        const retail = Number(product.retailPrice || product.sellingPrice || 0);
        if (!selectedCustomer) return retail;
        switch (selectedCustomer.customerType) {
            case 'WHOLESALE': return Number(product.wholesalePrice || retail);
            case 'PARTNER_FARM': return Number(product.partnerPrice || retail);
            default: return retail;
        }
    };

    const filteredProducts = products.filter(p => {
        const matchCat = selectedCategory === 'ALL' || p.category === selectedCategory;
        const matchSearch = !searchQuery ||
            p.itemName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
            p.sku?.toLowerCase().includes(searchQuery.toLowerCase());
        return matchCat && matchSearch;
    });

    const addToCart = (product) => {
        // Controlled substance guard
        if (product.isControlled && !prescriptionNumber.trim()) {
            setBlockedItem(product);
            setShowRxWarning(true);
            return;
        }

        // Stock check
        const price = getPriceForCustomer(product);
        const existing = cart.find(item => item.id === product.id);
        const currentQtyInCart = existing ? existing.qty : 0;

        if (currentQtyInCart + 1 > (product.currentStock || 0)) {
            toast.error(`Insufficient stock for ${product.itemName}. Available: ${product.currentStock || 0}`);
            return;
        }

        if (price <= 0) {
            toast.warning(`${product.itemName} has no price set. Please update inventory pricing.`);
        }

        if (existing) {
            setCart(cart.map(item => item.id === product.id ? { ...item, qty: item.qty + 1 } : item));
        } else {
            setCart([...cart, { ...product, qty: 1, price }]);
        }
    };

    const updateQty = (id, delta) => {
        const product = products.find(p => p.id === id);
        setCart(cart.map(item => {
            if (item.id === id) {
                const newQty = Math.max(1, item.qty + delta);
                if (delta > 0 && newQty > (product?.currentStock || 0)) {
                    toast.error(`Cannot add more — only ${product?.currentStock || 0} in stock.`);
                    return item;
                }
                return { ...item, qty: newQty };
            }
            return item;
        }));
    };

    const removeFromCart = (id) => setCart(cart.filter(item => item.id !== id));
    const clearCart = () => { setCart([]); setSelectedCustomer(null); setPrescriptionNumber(''); };

    const subtotal = cart.reduce((sum, item) => sum + (item.price * item.qty), 0);
    const tax = subtotal * 0.15;
    const total = subtotal + tax;

    const handleCheckout = async () => {
        if (cart.length === 0) return;
        setProcessing(true);
        try {
            const saleData = {
                items: cart.map(i => ({ itemId: i.id, qty: i.qty, unitPrice: i.price })),
                customerId: selectedCustomer?.id || null,
                paymentMethod,
                customerName: selectedCustomer ? selectedCustomer.name : 'Walk-in Customer',
                prescriptionNumber: prescriptionNumber || null
            };
            const res = await pharmacyApi.createSale(saleData);
            const saleCode = res.data?.saleCode || 'SAL-' + Date.now();
            setLastReceipt({ saleCode, total, items: [...cart], customer: selectedCustomer, paymentMethod });
            toast.success(`✅ Sale ${saleCode} completed! Revenue recorded.`);
            clearCart();
            setShowCheckout(false);
            fetchData(); // Refresh stock
        } catch (e) {
            toast.error(e.response?.data?.message || 'Transaction failed. Please try again.');
        } finally {
            setProcessing(false);
        }
    };

    const hasUnpricedItems = cart.some(i => i.price <= 0);

    return (
        <div className="flex h-[calc(100vh-120px)] gap-6 animate-in fade-in duration-300 overflow-hidden">

            {/* LEFT: Product Catalog */}
            <div className="flex-1 flex flex-col gap-3 overflow-hidden min-w-0">

                {/* Search + Refresh */}
                <div className="flex gap-3">
                    <div className="relative flex-1">
                        <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
                        <Input
                            placeholder="Search by name or SKU..."
                            className="pl-10 h-11"
                            value={searchQuery}
                            onChange={e => setSearchQuery(e.target.value)}
                        />
                    </div>
                    <Button variant="outline" size="icon" className="h-11 w-11" onClick={fetchData} disabled={loading}>
                        <RefreshCw className={cn('h-4 w-4', loading && 'animate-spin')} />
                    </Button>
                </div>

                {/* Category Tabs */}
                <ScrollArea orientation="horizontal" className="w-full">
                    <div className="flex gap-2 pb-1">
                        {CATEGORY_TABS.map(cat => (
                            <Button
                                key={cat}
                                variant={selectedCategory === cat ? 'default' : 'outline'}
                                size="sm"
                                className={cn('h-8 whitespace-nowrap text-xs font-semibold', selectedCategory === cat && 'bg-indigo-600')}
                                onClick={() => setSelectedCategory(cat)}
                            >
                                {cat !== 'ALL' && <span className="mr-1">{getCatIcon(cat)}</span>}
                                {cat === 'ALL' ? 'All Products' : cat.replace('_', ' ')}
                            </Button>
                        ))}
                    </div>
                </ScrollArea>

                {/* Product Grid */}
                <ScrollArea className="flex-1 rounded-xl border border-gray-100 bg-gray-50/30 p-3">
                    {loading ? (
                        <div className="flex items-center justify-center h-48 text-gray-400 gap-2">
                            <RefreshCw className="animate-spin h-5 w-5" /> Loading products...
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                            {filteredProducts.length === 0 ? (
                                <div className="col-span-full flex flex-col items-center justify-center py-16 text-gray-400 gap-2">
                                    <Package className="h-12 w-12 opacity-20" />
                                    <p>No products found</p>
                                </div>
                            ) : filteredProducts.map(p => {
                                const price = getPriceForCustomer(p);
                                const outOfStock = (p.currentStock || 0) <= 0;
                                return (
                                    <Card
                                        key={p.id}
                                        className={cn(
                                            'cursor-pointer hover:border-indigo-400 hover:shadow-md transition-all h-fit group',
                                            outOfStock && 'opacity-50 cursor-not-allowed'
                                        )}
                                        onClick={() => !outOfStock && addToCart(p)}
                                    >
                                        <CardContent className="p-3">
                                            <div className="flex justify-between items-start mb-2">
                                                {p.isControlled
                                                    ? <Badge variant="destructive" className="text-[10px]"><Zap className="w-2 h-2 mr-1" />Controlled</Badge>
                                                    : <Badge variant="secondary" className="text-[10px]">{p.category?.replace('_', ' ')}</Badge>
                                                }
                                                <span className="text-[10px] font-mono text-gray-400">{p.sku}</span>
                                            </div>
                                            <div className="font-bold text-gray-800 line-clamp-1 group-hover:text-indigo-600 transition-colors text-sm">
                                                {p.itemName}
                                            </div>
                                            <div className="flex justify-between items-end mt-2">
                                                <div className={cn('text-base font-black', price > 0 ? 'text-indigo-600' : 'text-rose-400')}>
                                                    {price > 0 ? formatCurrency(price) : 'No Price'}
                                                </div>
                                                <div className={cn('text-xs font-semibold', outOfStock ? 'text-rose-600' : p.currentStock <= (p.reorderPoint || 0) ? 'text-amber-600' : 'text-emerald-600')}>
                                                    {outOfStock ? 'Out of Stock' : `${p.currentStock} ${p.unit || ''}`}
                                                </div>
                                            </div>
                                        </CardContent>
                                    </Card>
                                );
                            })}
                        </div>
                    )}
                </ScrollArea>
            </div>

            {/* RIGHT: Checkout Panel */}
            <Card className="w-[400px] flex flex-col shadow-xl border-indigo-100 bg-white shrink-0">
                <CardHeader className="bg-gradient-to-br from-indigo-600 to-indigo-700 text-white rounded-t-xl pb-4">
                    <div className="flex items-center gap-2">
                        <ShoppingCart className="h-5 w-5" />
                        <CardTitle className="text-lg">Checkout Terminal</CardTitle>
                        {cart.length > 0 && (
                            <Badge className="ml-auto bg-white/20 text-white hover:bg-white/20">{cart.length} items</Badge>
                        )}
                    </div>
                </CardHeader>

                <CardContent className="flex-1 flex flex-col p-0 overflow-hidden">
                    {/* Customer + Prescription */}
                    <div className="p-4 border-b space-y-3">
                        <div className="space-y-1">
                            <Label className="text-xs font-bold text-gray-600 flex items-center gap-1"><User className="h-3 w-3" /> Customer</Label>
                            <Select onValueChange={val => setSelectedCustomer(customers.find(c => c.id === parseInt(val)) || null)}>
                                <SelectTrigger className="h-9 bg-gray-50 text-sm">
                                    <SelectValue placeholder="Walk-in Customer" />
                                </SelectTrigger>
                                <SelectContent>
                                    {customers.map(c => (
                                        <SelectItem key={c.id} value={c.id.toString()}>{c.name} ({c.customerType})</SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                        <div className="space-y-1">
                            <Label className="text-xs font-bold text-gray-600 flex items-center gap-1"><ClipboardCheck className="h-3 w-3" /> Prescription No. (for controlled drugs)</Label>
                            <Input
                                placeholder="e.g. RX-2024-00123"
                                className="h-9 text-sm"
                                value={prescriptionNumber}
                                onChange={e => setPrescriptionNumber(e.target.value)}
                            />
                        </div>
                        {selectedCustomer && (
                            <div className="text-xs flex gap-3 text-gray-500 bg-indigo-50 p-2 rounded">
                                <span>Type: <b>{selectedCustomer.customerType}</b></span>
                                <span>Balance: <b className="text-rose-600">{formatCurrency(selectedCustomer.outstandingBalance || 0)}</b></span>
                            </div>
                        )}
                    </div>

                    {/* Cart Items */}
                    <ScrollArea className="flex-1 px-3">
                        {cart.length === 0 ? (
                            <div className="flex flex-col items-center justify-center h-52 text-gray-400 gap-2">
                                <ShoppingCart className="h-10 w-10 opacity-20" />
                                <p className="text-sm">Cart is empty</p>
                                <p className="text-xs text-gray-300">Click a product to add it</p>
                            </div>
                        ) : (
                            <div className="py-3 space-y-3">
                                {cart.map(item => (
                                    <div key={item.id} className="flex gap-2 items-start p-2 rounded-lg bg-gray-50 border border-gray-100 animate-in slide-in-from-right-2">
                                        <div className="flex-1 min-w-0">
                                            <div className="font-semibold text-sm text-gray-800 truncate">{item.itemName}</div>
                                            <div className="text-xs text-indigo-600 font-medium">
                                                {item.price > 0 ? formatCurrency(item.price) + ' / unit' : <span className="text-rose-500">⚠ No price set</span>}
                                            </div>
                                            <div className="text-xs text-gray-400">Subtotal: {formatCurrency(item.price * item.qty)}</div>
                                        </div>
                                        <div className="flex items-center gap-1.5 border rounded-lg p-1 bg-white shrink-0">
                                            <Button variant="ghost" size="sm" className="h-6 w-6 p-0" onClick={() => updateQty(item.id, -1)}><Minus className="h-3 w-3" /></Button>
                                            <span className="text-sm font-bold w-5 text-center">{item.qty}</span>
                                            <Button variant="ghost" size="sm" className="h-6 w-6 p-0" onClick={() => updateQty(item.id, 1)}><Plus className="h-3 w-3" /></Button>
                                        </div>
                                        <Button variant="ghost" size="sm" className="h-7 w-7 p-0 text-gray-300 hover:text-rose-500 shrink-0" onClick={() => removeFromCart(item.id)}>
                                            <Trash2 className="h-3.5 w-3.5" />
                                        </Button>
                                    </div>
                                ))}
                            </div>
                        )}
                    </ScrollArea>

                    {/* Totals */}
                    {cart.length > 0 && (
                        <div className="p-4 bg-gray-50 border-t space-y-2">
                            {hasUnpricedItems && (
                                <div className="flex items-center gap-2 text-xs text-amber-700 bg-amber-50 border border-amber-200 p-2 rounded">
                                    <AlertTriangle className="h-3.5 w-3.5 shrink-0" />
                                    Some items have no price. Update inventory pricing.
                                </div>
                            )}
                            <div className="flex justify-between text-sm text-gray-500">
                                <span>Subtotal</span><span>{formatCurrency(subtotal)}</span>
                            </div>
                            <div className="flex justify-between text-sm text-gray-500">
                                <span>VAT (15%)</span><span>{formatCurrency(tax)}</span>
                            </div>
                            <div className="flex justify-between text-xl font-black text-gray-900 border-t pt-2 mt-1">
                                <span>TOTAL</span><span className="text-indigo-700">{formatCurrency(total)}</span>
                            </div>
                        </div>
                    )}
                </CardContent>

                <CardFooter className="p-3 flex flex-col gap-2">
                    <Select value={paymentMethod} onValueChange={setPaymentMethod}>
                        <SelectTrigger className="w-full h-10 font-semibold text-gray-700">
                            <div className="flex items-center gap-2">
                                <CreditCard className="h-3.5 w-3.5 text-indigo-500" />
                                <SelectValue placeholder="Payment Method" />
                            </div>
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="CASH">Cash Payment</SelectItem>
                            <SelectItem value="MOBILE_MONEY">Mobile Money</SelectItem>
                            <SelectItem value="BANK_TRANSFER">Bank Transfer</SelectItem>
                            <SelectItem value="CREDIT">Farmer Credit Account</SelectItem>
                        </SelectContent>
                    </Select>
                    <div className="flex gap-2 w-full">
                        {cart.length > 0 && (
                            <Button variant="outline" size="sm" className="h-12 text-gray-500" onClick={clearCart}>
                                Clear
                            </Button>
                        )}
                        <Button
                            className="flex-1 h-12 text-base font-bold bg-indigo-600 hover:bg-indigo-700 shadow-lg shadow-indigo-200"
                            disabled={cart.length === 0 || processing}
                            onClick={() => setShowCheckout(true)}
                        >
                            {processing ? 'Processing...' : 'Complete Transaction'}
                        </Button>
                    </div>
                </CardFooter>
            </Card>

            {/* Rx Warning Dialog */}
            <Dialog open={showRxWarning} onOpenChange={setShowRxWarning}>
                <DialogContent className="sm:max-w-md">
                    <DialogHeader>
                        <DialogTitle className="flex items-center gap-2 text-rose-600">
                            <AlertTriangle className="h-5 w-5" /> Prescription Required
                        </DialogTitle>
                        <DialogDescription>
                            <b>{blockedItem?.itemName}</b> is a controlled substance and requires a validated prescription number.
                            Enter the Prescription No. in the checkout panel before adding this item.
                        </DialogDescription>
                    </DialogHeader>
                    <div className="bg-rose-50 p-3 rounded border border-rose-100 text-sm text-rose-800 italic">
                        Unauthorized sale of controlled veterinary substances is subject to regulatory audit.
                    </div>
                    <DialogFooter>
                        <Button variant="secondary" onClick={() => setShowRxWarning(false)}>Close</Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Checkout Confirmation Dialog */}
            <Dialog open={showCheckout} onOpenChange={setShowCheckout}>
                <DialogContent className="sm:max-w-md">
                    <DialogHeader>
                        <DialogTitle className="text-xl">Confirm Transaction</DialogTitle>
                        <DialogDescription>
                            Review and confirm sale for <b>{selectedCustomer ? selectedCustomer.name : 'Walk-in Customer'}</b>
                        </DialogDescription>
                    </DialogHeader>
                    <div className="space-y-3">
                        <div className="bg-gray-50 p-4 rounded-lg border divide-y">
                            <div className="pb-2 space-y-1">
                                {cart.map(i => (
                                    <div key={i.id} className="flex justify-between text-sm">
                                        <span>{i.itemName} × {i.qty}</span>
                                        <span className="font-semibold">{formatCurrency(i.price * i.qty)}</span>
                                    </div>
                                ))}
                            </div>
                            <div className="pt-2 space-y-1">
                                <div className="flex justify-between text-sm text-gray-500"><span>VAT (15%):</span><span>{formatCurrency(tax)}</span></div>
                                <div className="flex justify-between text-lg font-black"><span>Total:</span><span className="text-indigo-700">{formatCurrency(total)}</span></div>
                            </div>
                        </div>
                        <div className="flex justify-between text-sm text-gray-600 bg-gray-50 p-3 rounded border">
                            <span>Payment: <b>{paymentMethod}</b></span>
                            {prescriptionNumber && <span>Rx: <b>{prescriptionNumber}</b></span>}
                        </div>
                        {paymentMethod === 'CREDIT' && selectedCustomer && (
                            <div className="p-3 bg-amber-50 border border-amber-200 rounded text-xs text-amber-800">
                                Balance will increase to <b>{formatCurrency((selectedCustomer.outstandingBalance || 0) + total)}</b>
                            </div>
                        )}
                    </div>
                    <DialogFooter>
                        <Button variant="ghost" onClick={() => setShowCheckout(false)} disabled={processing}>Cancel</Button>
                        <Button
                            className="bg-emerald-600 hover:bg-emerald-700 font-bold"
                            onClick={handleCheckout}
                            disabled={processing}
                        >
                            {processing ? 'Processing...' : '✅ Confirm & Generate Receipt'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Last Receipt Banner */}
            {lastReceipt && (
                <div className="fixed bottom-6 right-6 z-50 animate-in slide-in-from-bottom-4">
                    <Card className="w-80 shadow-2xl border-emerald-200 bg-white">
                        <CardContent className="p-4">
                            <div className="flex items-start gap-3">
                                <CheckCircle2 className="h-5 w-5 text-emerald-500 mt-0.5 shrink-0" />
                                <div className="flex-1">
                                    <p className="font-bold text-sm text-gray-800">Sale Complete</p>
                                    <p className="text-xs text-gray-500">{lastReceipt.saleCode} • {formatCurrency(lastReceipt.total)}</p>
                                    <p className="text-xs text-emerald-600 mt-0.5">Inventory deducted • Revenue logged</p>
                                </div>
                                <Button variant="ghost" size="sm" className="h-6 w-6 p-0" onClick={() => setLastReceipt(null)}>×</Button>
                            </div>
                        </CardContent>
                    </Card>
                </div>
            )}
        </div>
    );
};

export default POSTerminal;
