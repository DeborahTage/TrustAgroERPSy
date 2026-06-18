import React, { useState, useEffect } from 'react';
import { financeApi } from '../../api';
import PageHeader from '../../components/shared/PageHeader';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../../components/ui/table';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/ui/tabs';
import { formatCurrency } from '../../utils';
import { Calendar, Download, FileText, Printer, RefreshCw } from 'lucide-react';

const ReportCenter = () => {
    const [fromDate, setFromDate] = useState(new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0]);
    const [toDate, setToDate] = useState(new Date().toISOString().split('T')[0]);
    const [pnL, setPnL] = useState(null);
    const [balanceSheet, setBalanceSheet] = useState(null);
    const [loading, setLoading] = useState(false);

    const fetchReports = async () => {
        setLoading(true);
        try {
            const [pnlRes, bsRes] = await Promise.all([
                financeApi.getReports(fromDate, toDate),
                financeApi.getBalanceSheet()
            ]);
            setPnL(pnlRes.data.data);
            setBalanceSheet(bsRes.data.data);
        } catch (e) {
            console.error('Failed to fetch reports', e);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchReports();
    }, [fromDate, toDate]);

    return (
        <div className="flex flex-col gap-6 p-4 md:p-8 bg-slate-50 min-h-screen">
            <div className="flex justify-between items-center">
                <PageHeader
                    title="Financial Report Center"
                    subtitle="Generate and analyze professional financial statements"
                />
                <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={() => window.print()}>
                        <Printer className="mr-2 h-4 w-4" /> Print
                    </Button>
                    <Button size="sm" className="bg-indigo-600">
                        <Download className="mr-2 h-4 w-4" /> Export PDF
                    </Button>
                </div>
            </div>

            <Card className="border-none shadow-sm">
                <CardContent className="pt-6">
                    <div className="flex flex-wrap gap-4 items-end">
                        <div className="space-y-2">
                            <Label>From Date</Label>
                            <Input type="date" value={fromDate} onChange={(e) => setFromDate(e.target.value)} className="w-40" />
                        </div>
                        <div className="space-y-2">
                            <Label>To Date</Label>
                            <Input type="date" value={toDate} onChange={(e) => setToDate(e.target.value)} className="w-40" />
                        </div>
                        <Button variant="ghost" size="sm" onClick={fetchReports} disabled={loading}>
                            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
                        </Button>
                    </div>
                </CardContent>
            </Card>

            <Tabs defaultValue="pnl" className="w-full">
                <TabsList className="bg-white border p-1">
                    <TabsTrigger value="pnl">Profit & Loss</TabsTrigger>
                    <TabsTrigger value="bs">Balance Sheet</TabsTrigger>
                    <TabsTrigger value="ledger">General Ledger</TabsTrigger>
                </TabsList>

                <TabsContent value="pnl" className="mt-6">
                    <Card className="border-none shadow-sm pb-8">
                        <CardHeader className="text-center border-b mb-6">
                            <CardTitle className="text-2xl font-serif">Statement of Profit and Loss</CardTitle>
                            <p className="text-sm text-slate-500 font-mono uppercase tracking-widest pt-2">
                                Period: {fromDate} to {toDate}
                            </p>
                        </CardHeader>
                        <CardContent className="max-w-4xl mx-auto space-y-8">
                            {/* Revenue Section */}
                            <section>
                                <h3 className="text-xs font-bold uppercase text-indigo-600 tracking-wider mb-3 flex items-center gap-2">
                                    <div className="h-1 w-4 bg-indigo-600 rounded-full" />
                                    Revenue
                                </h3>
                                <Table>
                                    <TableBody>
                                        {pnL && Object.entries(pnL.revenues || {}).map(([name, val]) => (
                                            <TableRow key={name} className="hover:bg-transparent border-slate-100">
                                                <TableCell className="py-2 text-slate-700">{name}</TableCell>
                                                <TableCell className="py-2 text-right font-mono">{formatCurrency(val)}</TableCell>
                                            </TableRow>
                                        ))}
                                        <TableRow className="bg-slate-50 font-bold border-t-2 border-slate-200">
                                            <TableCell className="py-3 text-slate-900 uppercase text-[10px] tracking-widest">Total Revenue</TableCell>
                                            <TableCell className="py-3 text-right font-mono">{formatCurrency(pnL?.totalRevenue || 0)}</TableCell>
                                        </TableRow>
                                    </TableBody>
                                </Table>
                            </section>

                            {/* Expenses Section */}
                            <section>
                                <h3 className="text-xs font-bold uppercase text-rose-600 tracking-wider mb-3 flex items-center gap-2">
                                    <div className="h-1 w-4 bg-rose-600 rounded-full" />
                                    Expenses
                                </h3>
                                <Table>
                                    <TableBody>
                                        {pnL && Object.entries(pnL.expenses || {}).map(([name, val]) => (
                                            <TableRow key={name} className="hover:bg-transparent border-slate-100">
                                                <TableCell className="py-2 text-slate-700">{name}</TableCell>
                                                <TableCell className="py-2 text-right font-mono text-rose-600">({formatCurrency(val)})</TableCell>
                                            </TableRow>
                                        ))}
                                        <TableRow className="bg-slate-50 font-bold border-t-2 border-slate-200">
                                            <TableCell className="py-3 text-slate-900 uppercase text-[10px] tracking-widest">Total Operating Expenses</TableCell>
                                            <TableCell className="py-3 text-right font-mono text-rose-600">({formatCurrency(pnL?.totalExpenses || 0)})</TableCell>
                                        </TableRow>
                                    </TableBody>
                                </Table>
                            </section>

                            {/* Net Profit Section */}
                            <div className="pt-6 border-t-4 border-double border-slate-300">
                                <div className="flex justify-between items-center px-4 py-4 bg-indigo-50 rounded-lg">
                                    <span className="text-base font-bold text-indigo-900 uppercase tracking-tighter italic">Net Operating Income / (Loss)</span>
                                    <span className={`text-xl font-bold font-mono ${pnL?.netProfit >= 0 ? 'text-emerald-700' : 'text-rose-700'}`}>
                                        {formatCurrency(pnL?.netProfit || 0)}
                                    </span>
                                </div>
                            </div>
                        </CardContent>
                    </Card>
                </TabsContent>

                <TabsContent value="bs" className="mt-6">
                    <Card className="border-none shadow-sm">
                        <CardHeader className="text-center border-b mb-6">
                            <CardTitle className="text-2xl font-serif">Balance Sheet</CardTitle>
                            <p className="text-sm text-slate-500 font-mono uppercase tracking-widest pt-2">
                                As of {toDate}
                            </p>
                        </CardHeader>
                        <CardContent className="max-w-4xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-12">
                            {/* Left Side: Assets */}
                            <div className="space-y-6">
                                <h3 className="text-sm font-bold border-b pb-2 flex justify-between uppercase">
                                    <span>Assets</span>
                                </h3>
                                <div className="space-y-2">
                                    {balanceSheet && Object.entries(balanceSheet.assets || {}).map(([name, val]) => (
                                        <div key={name} className="flex justify-between text-sm py-1 border-b border-dashed border-slate-200">
                                            <span className="text-slate-600">{name}</span>
                                            <span className="font-mono">{formatCurrency(val)}</span>
                                        </div>
                                    ))}
                                    <div className="flex justify-between font-bold pt-4 text-indigo-900">
                                        <span>Total Assets</span>
                                        <span className="font-mono">{formatCurrency(balanceSheet?.totalAssets || 0)}</span>
                                    </div>
                                </div>
                            </div>

                            {/* Right Side: Liabilities & Equity */}
                            <div className="space-y-10">
                                <div className="space-y-6">
                                    <h3 className="text-sm font-bold border-b pb-2 uppercase">Liabilities</h3>
                                    <div className="space-y-2">
                                        {balanceSheet && Object.entries(balanceSheet.liabilities || {}).map(([name, val]) => (
                                            <div key={name} className="flex justify-between text-sm py-1 border-b border-dashed border-slate-200">
                                                <span className="text-slate-600">{name}</span>
                                                <span className="font-mono">{formatCurrency(val)}</span>
                                            </div>
                                        ))}
                                        <div className="flex justify-between font-bold pt-4 text-rose-900">
                                            <span>Total Liabilities</span>
                                            <span className="font-mono">{formatCurrency(balanceSheet?.totalLiabilities || 0)}</span>
                                        </div>
                                    </div>
                                </div>

                                <div className="space-y-6">
                                    <h3 className="text-sm font-bold border-b pb-2 uppercase">Equity</h3>
                                    <div className="space-y-2">
                                        {balanceSheet && Object.entries(balanceSheet.equity || {}).map(([name, val]) => (
                                            <div key={name} className="flex justify-between text-sm py-1 border-b border-dashed border-slate-200">
                                                <span className="text-slate-600">{name}</span>
                                                <span className="font-mono">{formatCurrency(val)}</span>
                                            </div>
                                        ))}
                                        <div className="flex justify-between font-bold pt-4 text-emerald-900">
                                            <span>Total Equity</span>
                                            <span className="font-mono">{formatCurrency(balanceSheet?.totalEquity || 0)}</span>
                                        </div>
                                    </div>
                                </div>

                                <div className="pt-6 border-t font-bold flex justify-between bg-slate-50 p-4 rounded-lg">
                                    <span className="uppercase text-xs tracking-widest">Total Liabilities & Equity</span>
                                    <span className="font-mono text-lg">{formatCurrency((balanceSheet?.totalLiabilities || 0) + (balanceSheet?.totalEquity || 0))}</span>
                                </div>
                            </div>
                        </CardContent>
                    </Card>
                </TabsContent>

                <TabsContent value="ledger">
                    <Card className="border-none shadow-sm">
                        <CardHeader>
                            <CardTitle>General Ledger Stream</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <p className="text-sm text-slate-500 p-8 text-center bg-slate-50 rounded-lg border border-dashed">
                                Detailed transaction log with audit trail coming soon in Phase 4 refinement.
                            </p>
                        </CardContent>
                    </Card>
                </TabsContent>
            </Tabs>
        </div>
    );
};

export default ReportCenter;
