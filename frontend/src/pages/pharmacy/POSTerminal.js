import React, { useState, useEffect, useCallback } from 'react';
import {
    Search, ShoppingCart, User, CreditCard, Trash2,
    Plus, Minus, AlertTriangle, CheckCircle2, Package,
    Zap, RefreshCw, ClipboardCheck, Pencil
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle, CardFooter } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Badge } from '../../components/ui/badge';
import { Label } from '../../components/ui/label';
import { ScrollArea } from '../../components/ui/scroll-area';
import {
    Dialog, DialogContent, DialogHeader, DialogTitle,
    DialogDescription, DialogFooter
} from '../../components/ui/dialog';
import {
    Select, SelectContent, SelectItem, SelectTrigger, SelectValue
} from '../../components/ui/select';
import { pharmacyApi, inventoryApi } from '../../api';
import { formatCurrency } from '../../utils';
import { toast } from 'react-toastify';
import { cn } from '../../lib/utils';
import EnterprisePage from '../../components/layout/EnterprisePage';

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

    // Open item & dynamic price states
    const [showAddModal, setShowAddModal] = useState(false);
    const [newItemForm, setNewItemForm] = useState({
        itemName: '',
        category: 'FEED',
        retailPrice: '',
        unit: 'PIECE',
        description: '',
    });

    const [editingProduct, setEditingProduct] = useState(null);
    const [editForm, setEditForm] = useState({
        itemName: '',
        retailPrice: '',
        category: 'FEED',
        unit: 'PIECE'
    });

    const handleUpdateCartPrice = (id, newPrice) => {
        const val = parseFloat(newPrice) || 0;
        setCart(cart.map(item => item.id === id ? { ...item, price: val } : item));
    };

    const openEditModal = (product) => {
        setEditingProduct(product);
        setEditForm({
            itemName: product.itemName || '',
            retailPrice: product.retailPrice || product.sellingPrice || '',
            category: product.category || 'FEED',
            unit: product.unit || product.unitString || 'PIECE'
        });
    };

    const handleSaveEditProduct = async (e) => {
        e.preventDefault();
        if (!editingProduct) return;
        const price = parseFloat(editForm.retailPrice) || 0;
        try {
            const payload = {
                retailPrice: price,
                sellingPrice: price
            };
            const res = await inventoryApi.updateItem(editingProduct.id, payload);
            const updated = res.data?.data || res.data;
            setProducts(prev => prev.map(p => p.id === editingProduct.id ? { ...p, ...payload, ...updated } : p));
            setCart(prev => prev.map(c => c.id === editingProduct.id ? { ...c, price: price } : c));
            toast.success(`Updated ${editingProduct.itemName} price to ${formatCurrency(price)}`);
            setEditingProduct(null);
        } catch (err) {
            toast.error('Failed to update price: ' + (err.response?.data?.message || err.message));
        }
    };

    const handleSaveNewItem = async (e) => {
        e.preventDefault();
        if (!newItemForm.itemName.trim()) {
            toast.error('Item name is required');
            return;
        }
        const price = parseFloat(newItemForm.retailPrice) || 0;
        try {
            const payload = {
                itemName: newItemForm.itemName.trim(),
                category: newItemForm.category,
                retailPrice: price,
                sellingPrice: price,
                unit: newItemForm.unit,
                description: newItemForm.description,
                storageLocation: 'Pharmacy POS',
                status: 'ACTIVE'
            };
            const res = await inventoryApi.createItem(payload);
            const saved = res.data?.data || res.data;
            setProducts(prev => [saved, ...prev]);
            toast.success(`${saved.itemName} added to catalog!`);
            setShowAddModal(false);
            setNewItemForm({ itemName: '', category: 'FEED', retailPrice: '', unit: 'PIECE', description: '' });
        } catch (err) {
            toast.error('Failed to create product: ' + (err.response?.data?.message || err.message));
        }
    };

    const fetchData = useCallback(async () => {
        setLoading(true);
        try {
            const [prodRes, custRes] = await Promise.all([
                pharmacyApi.getPharmacyCatalog(),
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

        // Stock & price check
        const price = getPriceForCustomer(product);
        const existing = cart.find(item => item.id === product.id);
        const currentQtyInCart = existing ? existing.qty : 0;

        if (product.currentStock != null && product.currentStock > 0 && currentQtyInCart + 1 > product.currentStock) {
            toast.error(`Insufficient stock for ${product.itemName}. Available: ${product.currentStock}`);
            return;
        }

        if (price <= 0) {
            toast.error(`${product.itemName} has no price set. Update inventory pricing before selling.`);
            return;
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
                if (delta > 0 && product?.currentStock != null && product.currentStock > 0 && newQty > product.currentStock) {
                    toast.error(`Cannot add more — only ${product.currentStock} in stock.`);
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
        <EnterprisePage
            title="POS Terminal"
            subtitle="Point-of-sale with live inventory sync, tiered pricing, and controlled substance checks"
            breadcrumbs={['Pharmacy', 'POS']}
            onRefresh={fetchData}
        >
        <div className="flex h-[calc(100vh-220px)] gap-6 animate-in fade-in duration-300 overflow-hidden">

            {/* LEFT: Product Catalog */}
            <div className="flex-1 flex flex-col gap-3 overflow-hidden min-w-0">

                {/* Search + Add Item + Refresh */}
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
                    <Button
                        onClick={() => setShowAddModal(true)}
                        className="h-11 px-4 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold flex items-center gap-1.5 shrink-0 shadow-sm"
                        title="Add a new product to the catalog"
                    >
                        <Plus className="h-4 w-4" />
                        <span>Add Item</span>
                    </Button>
                    <Button variant="outline" size="icon" className="h-11 w-11 shrink-0" onClick={fetchData} disabled={loading} title="Refresh catalog">
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
                                className={cn('h-8 whitespace-nowrap text-xs font-semibold', selectedCategory === cat && 'bg-primary')}
                                onClick={() => setSelectedCategory(cat)}
                            >
                                {cat !== 'ALL' && <span className="mr-1">{getCatIcon(cat)}</span>}
                                {cat === 'ALL' ? 'All Products' : cat.replace('_', ' ')}
                            </Button>
                        ))}
                    </div>
                </ScrollArea>

                {/* Product Grid */}
                <ScrollArea className="flex-1 rounded-xl border border-border/60 bg-muted/20 p-3">
                    {loading ? (
                        <div className="flex items-center justify-center h-48 text-muted-foreground gap-2">
                            <RefreshCw className="animate-spin h-5 w-5" /> Loading products...
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                            {filteredProducts.length === 0 ? (
                                <div className="col-span-full flex flex-col items-center justify-center py-16 text-muted-foreground gap-2">
                                    <Package className="h-12 w-12 opacity-20" />
                                    <p>No products found</p>
                                </div>
                            ) : filteredProducts.map(p => {
                                const price = getPriceForCustomer(p);
                                return (
                                    <Card
                                        key={p.id}
                                        className={cn(
                                            'cursor-pointer hover:border-primary/50 hover:shadow-md transition-all h-fit group'
                                        )}
                                        onClick={() => addToCart(p)}
                                    >
                                        <CardContent className="p-3">
                                            <div className="flex justify-between items-start mb-2">
                                                {p.isControlled
                                                    ? <Badge variant="destructive" className="text-[10px]"><Zap className="w-2 h-2 mr-1" />Controlled</Badge>
                                                    : <Badge variant="secondary" className="text-[10px]">{p.category?.replace('_', ' ')}</Badge>
                                                }
                                                <span className="text-[10px] font-mono text-muted-foreground">{p.sku}</span>
                                            </div>
                                            <div className="font-bold text-foreground line-clamp-1 group-hover:text-primary transition-colors text-sm">
                                                {p.itemName}
                                            </div>
                                            <div className="flex justify-between items-end mt-2">
                                                <div>
                                                    <div className={cn('text-base font-black', price > 0 ? 'text-primary' : 'text-rose-400')}>
                                                        {price > 0 ? formatCurrency(price) : 'No Price'}
                                                    </div>
                                                </div>
                                                <div className="flex items-center gap-1.5">
                                                    <Button
                                                        size="sm"
                                                        variant="outline"
                                                        className="h-6 px-2 text-[11px] text-muted-foreground hover:text-primary hover:border-primary hover:bg-primary/5 flex items-center gap-1"
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            openEditModal(p);
                                                        }}
                                                        title="Increase or decrease price"
                                                    >
                                                        <Pencil className="w-2.5 h-2.5" />
                                                        <span>Price</span>
                                                    </Button>
                                                    {(p.batchNumber || p.expiryDate) && (
                                                        <div className="text-[10px] text-muted-foreground text-right">
                                                            {p.batchNumber && <div>Batch: {p.batchNumber}</div>}
                                                            {p.expiryDate && <div>Exp: {p.expiryDate}</div>}
                                                        </div>
                                                    )}
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
            <Card className="w-[400px] flex flex-col shadow-xl border-border bg-card shrink-0">
                <CardHeader className="bg-gradient-to-br from-primary to-primary/90 text-white rounded-t-xl pb-4">
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
                    <div className="p-4 border-b border-border/60 space-y-3">
                        <div className="space-y-1">
                            <Label className="text-xs font-bold text-muted-foreground flex items-center gap-1"><User className="h-3 w-3" /> Customer</Label>
                            <Select onValueChange={val => setSelectedCustomer(customers.find(c => c.id === parseInt(val)) || null)}>
                                <SelectTrigger className="h-9 bg-card text-sm border-border">
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
                            <Label className="text-xs font-bold text-muted-foreground flex items-center gap-1"><ClipboardCheck className="h-3 w-3" /> Prescription No. (for controlled drugs)</Label>
                            <Input
                                placeholder="e.g. RX-2024-00123"
                                className="h-9 text-sm"
                                value={prescriptionNumber}
                                onChange={e => setPrescriptionNumber(e.target.value)}
                            />
                        </div>
                        {selectedCustomer && (
                            <div className="text-xs flex gap-3 text-muted-foreground bg-primary/10 p-2 rounded">
                                <span>Type: <b>{selectedCustomer.customerType}</b></span>
                                <span>Balance: <b className="text-rose-600 dark:text-rose-400">{formatCurrency(selectedCustomer.outstandingBalance || 0)}</b></span>
                            </div>
                        )}
                    </div>

                    {/* Cart Items */}
                    <ScrollArea className="flex-1 px-3">
                        {cart.length === 0 ? (
                            <div className="flex flex-col items-center justify-center h-52 text-muted-foreground gap-2">
                                <ShoppingCart className="h-10 w-10 opacity-20" />
                                <p className="text-sm">Cart is empty</p>
                                <p className="text-xs text-muted-foreground/60">Click a product to add it</p>
                            </div>
                        ) : (
                            <div className="py-3 space-y-3">
                                {cart.map(item => (
                                    <div key={item.id} className="flex gap-2 items-start p-2 rounded-lg bg-muted/40 border border-border animate-in slide-in-from-right-2">
                                        <div className="flex-1 min-w-0">
                                            <div className="font-semibold text-sm text-foreground truncate">{item.itemName}</div>
                                            <div className="text-xs text-primary font-medium flex items-center gap-1 mt-1">
                                                <span className="text-[11px] text-muted-foreground font-semibold">ETB</span>
                                                <Input
                                                    type="number"
                                                    step="any"
                                                    value={item.price}
                                                    onChange={(e) => handleUpdateCartPrice(item.id, e.target.value)}
                                                    className="h-6 w-20 px-1 py-0 text-xs font-bold text-primary bg-card border-border"
                                                    title="Adjust unit price for this transaction"
                                                />
                                                <span className="text-[10px] text-muted-foreground">/ unit</span>
                                            </div>
                                            <div className="text-xs text-muted-foreground mt-0.5">Subtotal: {formatCurrency(item.price * item.qty)}</div>
                                        </div>
                                        <div className="flex items-center gap-1.5 border border-border rounded-lg p-1 bg-card shrink-0">
                                            <Button variant="ghost" size="sm" className="h-6 w-6 p-0" onClick={() => updateQty(item.id, -1)}><Minus className="h-3 w-3" /></Button>
                                            <span className="text-sm font-bold w-5 text-center">{item.qty}</span>
                                            <Button variant="ghost" size="sm" className="h-6 w-6 p-0" onClick={() => updateQty(item.id, 1)}><Plus className="h-3 w-3" /></Button>
                                        </div>
                                        <Button variant="ghost" size="sm" className="h-7 w-7 p-0 text-muted-foreground hover:text-rose-500 shrink-0" onClick={() => removeFromCart(item.id)}>
                                            <Trash2 className="h-3.5 w-3.5" />
                                        </Button>
                                    </div>
                                ))}
                            </div>
                        )}
                    </ScrollArea>

                    {/* Totals */}
                    {cart.length > 0 && (
                        <div className="p-4 bg-muted/30 border-t border-border space-y-2">
                            {hasUnpricedItems && (
                                <div className="flex items-center gap-2 text-xs text-amber-700 dark:text-amber-400 bg-amber-500/10 border border-amber-500/20 p-2 rounded">
                                    <AlertTriangle className="h-3.5 w-3.5 shrink-0" />
                                    Some items have no price. Update inventory pricing.
                                </div>
                            )}
                            <div className="flex justify-between text-sm text-muted-foreground">
                                <span>Subtotal</span><span>{formatCurrency(subtotal)}</span>
                            </div>
                            <div className="flex justify-between text-sm text-gray-500">
                                <span>VAT (15%)</span><span>{formatCurrency(tax)}</span>
                            </div>
                            <div className="flex justify-between text-xl font-black text-gray-900 border-t pt-2 mt-1">
                                <span>TOTAL</span><span className="text-primary">{formatCurrency(total)}</span>
                            </div>
                        </div>
                    )}
                </CardContent>

                <CardFooter className="p-3 flex flex-col gap-2">
                    <Select value={paymentMethod} onValueChange={setPaymentMethod}>
                        <SelectTrigger className="w-full h-10 font-semibold text-gray-700">
                            <div className="flex items-center gap-2">
                                <CreditCard className="h-3.5 w-3.5 text-primary" />
                                <SelectValue placeholder="Payment Method" />
                            </div>
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="CASH">💵 Cash</SelectItem>
                            <SelectItem value="BANK_TRANSFER">🏦 Bank Transfer</SelectItem>
                        </SelectContent>
                    </Select>
                    <div className="flex gap-2 w-full">
                        {cart.length > 0 && (
                            <Button variant="outline" size="sm" className="h-12 text-gray-500" onClick={clearCart}>
                                Clear
                            </Button>
                        )}
                        <Button
                            className="flex-1 h-12 text-base font-bold bg-primary hover:bg-primary/90 shadow-lg shadow-primary/15"
                            disabled={cart.length === 0 || processing || hasUnpricedItems || total <= 0}
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
                        <div className="bg-muted/40 p-4 rounded-lg border border-border divide-y divide-border">
                            <div className="pb-2 space-y-1">
                                {cart.map(i => (
                                    <div key={i.id} className="flex justify-between text-sm">
                                        <span>{i.itemName} × {i.qty}</span>
                                        <span className="font-semibold">{formatCurrency(i.price * i.qty)}</span>
                                    </div>
                                ))}
                            </div>
                            <div className="pt-2 space-y-1">
                                <div className="flex justify-between text-sm text-muted-foreground"><span>VAT (15%):</span><span>{formatCurrency(tax)}</span></div>
                                <div className="flex justify-between text-lg font-black"><span>Total:</span><span className="text-primary">{formatCurrency(total)}</span></div>
                            </div>
                        </div>

                        {/* Payment Method Dropdown */}
                        <div className="space-y-1.5 bg-muted/40 p-3 rounded-lg border border-border">
                            <Label className="text-xs font-bold text-muted-foreground flex items-center gap-1.5">
                                <CreditCard className="h-3.5 w-3.5 text-primary" />
                                Payment Method
                            </Label>
                            <Select value={paymentMethod} onValueChange={setPaymentMethod}>
                                <SelectTrigger className="w-full h-10 bg-card font-semibold text-foreground text-sm border-border">
                                    <SelectValue placeholder="Select Payment Method" />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="CASH">💵 Cash</SelectItem>
                                    <SelectItem value="BANK_TRANSFER">🏦 Bank Transfer</SelectItem>
                                </SelectContent>
                            </Select>
                            {prescriptionNumber && (
                                <div className="text-xs text-muted-foreground pt-1 flex justify-between border-t border-border mt-2">
                                    <span>Prescription No:</span>
                                    <span className="font-mono font-bold text-foreground">{prescriptionNumber}</span>
                                </div>
                            )}
                        </div>
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

            {/* Modal: Add New Pharmacy Product */}
            <Dialog open={showAddModal} onOpenChange={setShowAddModal}>
                <DialogContent className="max-w-md">
                    <DialogHeader>
                        <DialogTitle className="flex items-center gap-2 text-emerald-700">
                            <Plus className="h-5 w-5 text-emerald-600" />
                            Add New Product to Catalog
                        </DialogTitle>
                        <DialogDescription>
                            Create a new product with custom price and category. It will instantly be available in the POS.
                        </DialogDescription>
                    </DialogHeader>
                    <form onSubmit={handleSaveNewItem} className="space-y-4 py-2">
                        <div className="space-y-1">
                            <Label className="text-xs font-bold">Item Name *</Label>
                            <Input
                                placeholder="e.g. Broiler Starter Feed"
                                value={newItemForm.itemName}
                                onChange={e => setNewItemForm({ ...newItemForm, itemName: e.target.value })}
                                required
                            />
                        </div>
                        <div className="grid grid-cols-2 gap-3">
                            <div className="space-y-1">
                                <Label className="text-xs font-bold">Category *</Label>
                                <Select
                                    value={newItemForm.category}
                                    onValueChange={val => setNewItemForm({ ...newItemForm, category: val })}
                                >
                                    <SelectTrigger className="h-10 text-sm">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="FEED">🌾 FEED</SelectItem>
                                        <SelectItem value="DRUG">💊 DRUG</SelectItem>
                                        <SelectItem value="VACCINE">💉 VACCINE</SelectItem>
                                        <SelectItem value="VITAMIN">🧪 VITAMIN</SelectItem>
                                        <SelectItem value="EQUIPMENT">🔧 EQUIPMENT</SelectItem>
                                        <SelectItem value="FARM_MATERIAL">📦 FARM MATERIAL</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                            <div className="space-y-1">
                                <Label className="text-xs font-bold">Unit *</Label>
                                <Select
                                    value={newItemForm.unit}
                                    onValueChange={val => setNewItemForm({ ...newItemForm, unit: val })}
                                >
                                    <SelectTrigger className="h-10 text-sm">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="PIECE">PIECE (PCS)</SelectItem>
                                        <SelectItem value="KG">KG</SelectItem>
                                        <SelectItem value="LITER">LITER (LIT)</SelectItem>
                                        <SelectItem value="DOSE">DOSE</SelectItem>
                                        <SelectItem value="SACK">SACK / BAG</SelectItem>
                                        <SelectItem value="BOTTLE">BOTTLE</SelectItem>
                                        <SelectItem value="BOX">BOX</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>
                        <div className="space-y-1">
                            <Label className="text-xs font-bold">Price (ETB) *</Label>
                            <Input
                                type="number"
                                step="any"
                                placeholder="e.g. 1500.00"
                                value={newItemForm.retailPrice}
                                onChange={e => setNewItemForm({ ...newItemForm, retailPrice: e.target.value })}
                                required
                            />
                        </div>
                        <div className="space-y-1">
                            <Label className="text-xs font-bold">Description / Notes (Optional)</Label>
                            <Input
                                placeholder="e.g. 50kg bag, poultry supplement"
                                value={newItemForm.description}
                                onChange={e => setNewItemForm({ ...newItemForm, description: e.target.value })}
                            />
                        </div>
                        <DialogFooter className="pt-2">
                            <Button type="button" variant="ghost" onClick={() => setShowAddModal(false)}>Cancel</Button>
                            <Button type="submit" className="bg-emerald-600 hover:bg-emerald-700 text-white font-bold">Save Product</Button>
                        </DialogFooter>
                    </form>
                </DialogContent>
            </Dialog>

            {/* Modal: Change Price for Existing Product */}
            <Dialog open={!!editingProduct} onOpenChange={(open) => !open && setEditingProduct(null)}>
                <DialogContent className="max-w-sm">
                    <DialogHeader>
                        <DialogTitle className="flex items-center gap-2 text-primary">
                            <Pencil className="h-5 w-5 text-primary" />
                            Change Price
                        </DialogTitle>
                        <DialogDescription>
                            Update the selling price for <span className="font-bold text-gray-900">{editingProduct?.itemName}</span>.
                        </DialogDescription>
                    </DialogHeader>
                    <form onSubmit={handleSaveEditProduct} className="space-y-4 py-2">
                        <div className="space-y-2">
                            <div className="flex justify-between items-center text-xs">
                                <Label className="font-bold text-gray-700">New Selling Price (ETB) *</Label>
                                <span className="bg-gray-100 text-gray-600 px-2 py-0.5 rounded text-[11px] font-semibold">
                                    {editingProduct?.unit || editingProduct?.unitString || 'unit'}
                                </span>
                            </div>
                            <Input
                                type="number"
                                step="any"
                                value={editForm.retailPrice}
                                onChange={e => setEditForm({ ...editForm, retailPrice: e.target.value })}
                                className="text-xl font-black text-primary h-12"
                                placeholder="0.00"
                                autoFocus
                                required
                            />
                            <div className="flex gap-2 pt-1">
                                <Button
                                    type="button"
                                    size="sm"
                                    variant="outline"
                                    className="text-xs h-8 flex-1"
                                    onClick={() => {
                                        const curr = parseFloat(editForm.retailPrice) || 0;
                                        setEditForm({ ...editForm, retailPrice: Math.max(0, curr - 50).toString() });
                                    }}
                                >
                                    -50 ETB
                                </Button>
                                <Button
                                    type="button"
                                    size="sm"
                                    variant="outline"
                                    className="text-xs h-8 flex-1"
                                    onClick={() => {
                                        const curr = parseFloat(editForm.retailPrice) || 0;
                                        setEditForm({ ...editForm, retailPrice: (curr + 50).toString() });
                                    }}
                                >
                                    +50 ETB
                                </Button>
                                <Button
                                    type="button"
                                    size="sm"
                                    variant="outline"
                                    className="text-xs h-8 flex-1"
                                    onClick={() => {
                                        const curr = parseFloat(editForm.retailPrice) || 0;
                                        setEditForm({ ...editForm, retailPrice: (curr + 100).toString() });
                                    }}
                                >
                                    +100 ETB
                                </Button>
                            </div>
                        </div>
                        <DialogFooter className="pt-2">
                            <Button type="button" variant="ghost" onClick={() => setEditingProduct(null)}>Cancel</Button>
                            <Button type="submit" className="bg-primary hover:bg-primary/90 text-white font-bold">
                                Update Price
                            </Button>
                        </DialogFooter>
                    </form>
                </DialogContent>
            </Dialog>

            {/* Last Receipt Banner */}
            {lastReceipt && (
                <div className="fixed bottom-6 right-6 z-50 animate-in slide-in-from-bottom-4">
                    <Card className="w-80 shadow-2xl border-emerald-500/30 bg-card">
                        <CardContent className="p-4">
                            <div className="flex items-start gap-3">
                                <CheckCircle2 className="h-5 w-5 text-emerald-500 mt-0.5 shrink-0" />
                                <div className="flex-1">
                                    <p className="font-bold text-sm text-foreground">Sale Complete</p>
                                    <p className="text-xs text-muted-foreground">{lastReceipt.saleCode} • {formatCurrency(lastReceipt.total)}</p>
                                    <p className="text-xs text-emerald-600 dark:text-emerald-400 mt-0.5">Inventory deducted • Revenue logged</p>
                                </div>
                                <Button variant="ghost" size="sm" className="h-6 w-6 p-0" onClick={() => setLastReceipt(null)}>×</Button>
                            </div>
                        </CardContent>
                    </Card>
                </div>
            )}
        </div>
        </EnterprisePage>
    );
};

export default POSTerminal;
