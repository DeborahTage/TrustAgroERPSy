import React, { useState, useEffect, useCallback } from 'react';
import { financeApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { exportToPdf } from '../../lib/exportTable';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Table, TableBody, TableCell, TableRow } from '../../components/ui/table';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/ui/tabs';
import { formatCurrency } from '../../utils';
import { Printer, RefreshCw } from 'lucide-react';

const ReportCenter = () => {
    const [fromDate, setFromDate] = useState(new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0]);
    const [toDate, setToDate] = useState(new Date().toISOString().split('T')[0]);
    const [pnL, setPnL] = useState(null);
    const [balanceSheet, setBalanceSheet] = useState(null);
    const [cashFlow, setCashFlow] = useState(null);
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(false);

    const fetchReports = useCallback(async () => {
        setLoading(true);
        try {
            const [pnlRes, bsRes, cfRes, txRes] = await Promise.all([
                financeApi.getReports(fromDate, toDate),
                financeApi.getBalanceSheet(),
                financeApi.getCashFlow(fromDate, toDate).catch(() => ({ data: { data: null } })),
                financeApi.getTransactions().catch(() => ({ data: { data: [] } })),
            ]);
            setPnL(pnlRes.data.data);
            setBalanceSheet(bsRes.data.data);
            setCashFlow(cfRes.data?.data || null);
            setTransactions(txRes.data?.data || []);
        } catch (e) {
            console.error('Failed to fetch reports', e);
        } finally {
            setLoading(false);
        }
    }, [fromDate, toDate]);

    useEffect(() => {
        fetchReports();
    }, [fetchReports]);

    return (
        <EnterprisePage
            title="Financial Report Center"
            subtitle="Generate and analyze professional financial statements"
            breadcrumbs={['Finance', 'Reports']}
            loading={loading}
            onRefresh={fetchReports}
            actions={
                <Button variant="outline" size="sm" onClick={() => exportToPdf('Financial Reports', 'finance-reports')}>
                    <Printer className="mr-2 h-4 w-4" /> Print
                </Button>
            }
        >
            <div id="finance-reports" className="space-y-6">
            <Card className="erp-card rounded-2xl print:hidden">
                <CardContent className="pt-6">
                    <div className="flex flex-wrap gap-4 items-end">
                        <div className="space-y-2">
                            <Label>From Date</Label>
                            <Input type="date" value={fromDate} onChange={(e) => setFromDate(e.target.value)} className="w-40 bg-card border-border text-foreground" />
                        </div>
                        <div className="space-y-2">
                            <Label>To Date</Label>
                            <Input type="date" value={toDate} onChange={(e) => setToDate(e.target.value)} className="w-40 bg-card border-border text-foreground" />
                        </div>
                        <Button variant="ghost" size="sm" onClick={fetchReports} disabled={loading}>
                            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
                        </Button>
                    </div>
                </CardContent>
            </Card>

            <Tabs defaultValue="pnl" className="w-full">
                <TabsList className="bg-muted/40 border border-border p-1 rounded-xl">
                    <TabsTrigger value="pnl">Profit & Loss</TabsTrigger>
                    <TabsTrigger value="bs">Balance Sheet</TabsTrigger>
                    <TabsTrigger value="cash">Cash Flow</TabsTrigger>
                    <TabsTrigger value="ledger">General Ledger</TabsTrigger>
                </TabsList>

                <TabsContent value="pnl" className="mt-6">
                    <Card className="erp-card rounded-2xl pb-8 print:bg-white print:text-black print:border-none">
                        <CardHeader className="text-center border-b border-border mb-6">
                            <CardTitle className="text-2xl font-serif">Statement of Profit and Loss</CardTitle>
                            <p className="text-sm text-muted-foreground font-mono uppercase tracking-widest pt-2">
                                Period: {fromDate} to {toDate}
                            </p>
                        </CardHeader>
                        <CardContent className="max-w-4xl mx-auto space-y-8">
                            {/* Revenue Section */}
                            <section>
                                <h3 className="text-xs font-bold uppercase text-primary tracking-wider mb-3 flex items-center gap-2">
                                    <div className="h-1 w-4 bg-primary rounded-full" />
                                    Revenue
                                </h3>
                                <Table>
                                    <TableBody>
                                        {pnL && Object.entries(pnL.revenues || {}).map(([name, val]) => (
                                            <TableRow key={name} className="hover:bg-muted/30 border-border">
                                                <TableCell className="py-2 text-foreground">{name}</TableCell>
                                                <TableCell className="py-2 text-right font-mono">{formatCurrency(val)}</TableCell>
                                            </TableRow>
                                        ))}
                                        <TableRow className="bg-muted/50 font-bold border-t-2 border-border">
                                            <TableCell className="py-3 text-foreground uppercase text-[10px] tracking-widest">Total Revenue</TableCell>
                                            <TableCell className="py-3 text-right font-mono">{formatCurrency(pnL?.totalRevenue || 0)}</TableCell>
                                        </TableRow>
                                    </TableBody>
                                </Table>
                            </section>

                            {/* Expenses Section */}
                            <section>
                                <h3 className="text-xs font-bold uppercase text-rose-500 tracking-wider mb-3 flex items-center gap-2">
                                    <div className="h-1 w-4 bg-rose-500 rounded-full" />
                                    Expenses
                                </h3>
                                <Table>
                                    <TableBody>
                                        {pnL && Object.entries(pnL.expenses || {}).map(([name, val]) => (
                                            <TableRow key={name} className="hover:bg-muted/30 border-border">
                                                <TableCell className="py-2 text-foreground">{name}</TableCell>
                                                <TableCell className="py-2 text-right font-mono text-rose-500 dark:text-rose-400">({formatCurrency(val)})</TableCell>
                                            </TableRow>
                                        ))}
                                        <TableRow className="bg-muted/50 font-bold border-t-2 border-border">
                                            <TableCell className="py-3 text-foreground uppercase text-[10px] tracking-widest">Total Operating Expenses</TableCell>
                                            <TableCell className="py-3 text-right font-mono text-rose-500 dark:text-rose-400">({formatCurrency(pnL?.totalExpenses || 0)})</TableCell>
                                        </TableRow>
                                    </TableBody>
                                </Table>
                            </section>

                            {/* Net Profit Section */}
                            <div className="pt-6 border-t-4 border-double border-border">
                                <div className="flex justify-between items-center px-4 py-4 bg-primary/10 rounded-lg">
                                    <span className="text-base font-bold text-emerald-600 dark:text-emerald-400 uppercase tracking-tighter italic">Net Operating Income / (Loss)</span>
                                    <span className={`text-xl font-bold font-mono ${pnL?.netProfit >= 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'}`}>
                                        {formatCurrency(pnL?.netProfit || 0)}
                                    </span>
                                </div>
                                {pnL?.limitationNote && (
                                    <p className="text-xs text-muted-foreground mt-3 px-2 leading-relaxed">{pnL.limitationNote}</p>
                                )}
                                {pnL?.revenueByModule && Object.keys(pnL.revenueByModule).length > 0 && (
                                    <div className="mt-4 px-2">
                                        <p className="text-[10px] font-bold uppercase text-muted-foreground mb-2">Revenue by source module</p>
                                        <div className="space-y-1">
                                            {Object.entries(pnL.revenueByModule).map(([mod, val]) => (
                                                <div key={mod} className="flex justify-between text-xs">
                                                    <span className="font-mono">{mod}</span>
                                                    <span>{formatCurrency(val)}</span>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        </CardContent>
                    </Card>
                </TabsContent>

                <TabsContent value="bs" className="mt-6">
                    <Card className="erp-card rounded-2xl print:bg-white print:text-black print:border-none">
                        <CardHeader className="text-center border-b border-border mb-6">
                            <CardTitle className="text-2xl font-serif">Balance Sheet</CardTitle>
                            <p className="text-sm text-muted-foreground font-mono uppercase tracking-widest pt-2">
                                As of {toDate}
                            </p>
                        </CardHeader>
                        <CardContent className="max-w-4xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-12">
                            {/* Left Side: Assets */}
                            <div className="space-y-6">
                                <h3 className="text-sm font-bold border-b border-border pb-2 flex justify-between uppercase">
                                    <span>Assets</span>
                                </h3>
                                <div className="space-y-2">
                                    {balanceSheet && Object.entries(balanceSheet.assets || {}).map(([name, val]) => (
                                        <div key={name} className="flex justify-between text-sm py-1 border-b border-dashed border-border">
                                            <span className="text-muted-foreground">{name}</span>
                                            <span className="font-mono">{formatCurrency(val)}</span>
                                        </div>
                                    ))}
                                    <div className="flex justify-between font-bold pt-4 text-emerald-600 dark:text-emerald-400">
                                        <span>Total Assets</span>
                                        <span className="font-mono">{formatCurrency(balanceSheet?.totalAssets || 0)}</span>
                                    </div>
                                </div>
                            </div>

                            {/* Right Side: Liabilities & Equity */}
                            <div className="space-y-10">
                                <div className="space-y-6">
                                    <h3 className="text-sm font-bold border-b border-border pb-2 uppercase">Liabilities</h3>
                                    <div className="space-y-2">
                                        {balanceSheet && Object.entries(balanceSheet.liabilities || {}).map(([name, val]) => (
                                            <div key={name} className="flex justify-between text-sm py-1 border-b border-dashed border-border">
                                                <span className="text-muted-foreground">{name}</span>
                                                <span className="font-mono">{formatCurrency(val)}</span>
                                            </div>
                                        ))}
                                        <div className="flex justify-between font-bold pt-4 text-rose-600 dark:text-rose-400">
                                            <span>Total Liabilities</span>
                                            <span className="font-mono">{formatCurrency(balanceSheet?.totalLiabilities || 0)}</span>
                                        </div>
                                    </div>
                                </div>

                                <div className="space-y-6">
                                    <h3 className="text-sm font-bold border-b border-border pb-2 uppercase">Equity</h3>
                                    <div className="space-y-2">
                                        {balanceSheet && Object.entries(balanceSheet.equity || {}).map(([name, val]) => (
                                            <div key={name} className="flex justify-between text-sm py-1 border-b border-dashed border-border">
                                                <span className="text-muted-foreground">{name}</span>
                                                <span className="font-mono">{formatCurrency(val)}</span>
                                            </div>
                                        ))}
                                        <div className="flex justify-between font-bold pt-4 text-emerald-600 dark:text-emerald-400">
                                            <span>Total Equity</span>
                                            <span className="font-mono">{formatCurrency(balanceSheet?.totalEquity || 0)}</span>
                                        </div>
                                    </div>
                                </div>

                                <div className="pt-6 border-t border-border font-bold flex justify-between bg-muted/50 p-4 rounded-lg">
                                    <span className="uppercase text-xs tracking-widest">Total Liabilities & Equity</span>
                                    <span className="font-mono text-lg">{formatCurrency((balanceSheet?.totalLiabilities || 0) + (balanceSheet?.totalEquity || 0))}</span>
                                </div>
                            </div>
                        </CardContent>
                    </Card>
                </TabsContent>

                <TabsContent value="cash" className="mt-6">
                    <Card className="erp-card rounded-2xl print:bg-white print:text-black print:border-none">
                        <CardHeader className="text-center border-b border-border mb-6">
                            <CardTitle className="text-2xl font-serif">Cash Flow</CardTitle>
                            <p className="text-sm text-muted-foreground font-mono uppercase tracking-widest pt-2">
                                Period: {fromDate} to {toDate}
                            </p>
                        </CardHeader>
                        <CardContent className="max-w-2xl mx-auto space-y-4">
                            <div className="flex justify-between text-sm border-b border-border py-2">
                                <span>Cash / bank inflows</span>
                                <span className="font-mono text-emerald-600 dark:text-emerald-400">{formatCurrency(cashFlow?.cashInflows || 0)}</span>
                            </div>
                            <div className="flex justify-between text-sm border-b border-border py-2">
                                <span>Cash / bank outflows</span>
                                <span className="font-mono text-rose-600 dark:text-rose-400">{formatCurrency(cashFlow?.cashOutflows || 0)}</span>
                            </div>
                            <div className="flex justify-between font-bold pt-2">
                                <span>Net cash change</span>
                                <span className="font-mono">{formatCurrency(cashFlow?.netCashChange || 0)}</span>
                            </div>
                            {cashFlow?.note && <p className="text-xs text-muted-foreground pt-4">{cashFlow.note}</p>}
                        </CardContent>
                    </Card>
                </TabsContent>

                <TabsContent value="ledger">
                    <Card className="erp-card rounded-2xl">
                        <CardHeader>
                            <CardTitle>General Ledger Stream</CardTitle>
                        </CardHeader>
                        <CardContent>
                            {transactions.length === 0 ? (
                                <p className="text-sm text-muted-foreground p-8 text-center bg-muted/40 rounded-lg border border-dashed border-border">
                                    No finance transactions posted yet.
                                </p>
                            ) : (
                                <Table>
                                    <TableBody>
                                        {transactions.slice(0, 50).map((tx) => (
                                            <TableRow key={tx.id} className="hover:bg-muted/30 border-border">
                                                <TableCell className="text-xs font-mono">{tx.transactionDate}</TableCell>
                                                <TableCell className="text-xs">{tx.description}</TableCell>
                                                <TableCell className="text-xs font-mono">{tx.sourceModule} / {tx.referenceType}{tx.referenceId != null ? `#${tx.referenceId}` : ''}</TableCell>
                                                <TableCell className="text-xs">{tx.transactionType}</TableCell>
                                                <TableCell className="text-right font-mono text-xs">{formatCurrency(tx.amount)}</TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            )}
                        </CardContent>
                    </Card>
                </TabsContent>
            </Tabs>
            </div>
        </EnterprisePage>
    );
};

export default ReportCenter;
