import React, { useEffect, useState } from 'react';
import { dailyRecordApi, inventoryApi, financeApi, pharmacyApi } from '../../api';
import DataTable from '../../components/shared/DataTable';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { exportToCsv, exportToPdf } from '../../lib/exportTable';
import { useLanguage } from '../../context/LanguageContext';
import { formatDate, formatCurrency } from '../../utils';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { 
  Download, 
  Printer, 
  TrendingUp, 
  Package, 
  DollarSign, 
  Activity,
  AlertTriangle,
  Calendar
} from 'lucide-react';

const PERIOD_OPTIONS = ['daily', 'weekly', 'monthly'];

const getPeriodRange = (period) => {
  const today = new Date();
  const start = new Date(today);
  const end = new Date(today);

  if (period === 'weekly') {
    const day = today.getDay();
    const diffToMonday = day === 0 ? -6 : 1 - day;
    start.setDate(today.getDate() + diffToMonday);
    end.setDate(start.getDate() + 6);
  } else if (period === 'monthly') {
    start.setDate(1);
    end.setMonth(today.getMonth() + 1, 0);
  }

  start.setHours(0, 0, 0, 0);
  end.setHours(23, 59, 59, 999);
  return { start, end };
};

const normalizeDate = (value) => {
  if (!value) return null;
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
};

const Reports = () => {
  const { t } = useLanguage();
  const [activeReport, setActiveReport] = useState('mortality');
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [reportPeriod, setReportPeriod] = useState('daily');

  useEffect(() => {
    const loadReport = async () => {
      setLoading(true);
      try {
        let res;
        if (activeReport === 'mortality' || activeReport === 'daily') res = await dailyRecordApi.getAll();
        else if (activeReport === 'stock') res = await inventoryApi.getCurrentStock();
        else if (activeReport === 'lowstock') res = await inventoryApi.getLowStock();
        else if (activeReport === 'expiry') res = await inventoryApi.getExpiryAlerts();
        else if (activeReport === 'sales') res = await pharmacyApi.getSales();
        else if (activeReport === 'income' || activeReport === 'expenses') {
          const txRes = await financeApi.getTransactions();
          const txs = txRes?.data?.data || [];
          const type = activeReport === 'income' ? 'INCOME' : 'EXPENSE';
          setData(txs.filter((tx) => tx.transactionType === type));
          return;
        }
        setData(res?.data?.data || []);
      } finally {
        setLoading(false);
      }
    };

    loadReport();
  }, [activeReport]);

  const getReportDateValue = (row) => {
    if (['daily', 'mortality'].includes(activeReport)) return row.date;
    if (activeReport === 'sales') return row.saleDate;
    if (['income', 'expenses'].includes(activeReport)) return row.transactionDate;
    if (activeReport === 'expiry') return row.expiryDate;
    return null;
  };

  const reportTypes = [
    { key: 'daily', label: t('reportsPage.daily'), icon: Calendar, description: 'Daily farm operations and production data' },
    { key: 'mortality', label: t('reportsPage.mortality'), icon: AlertTriangle, description: 'Mortality rates and death analysis' },
    { key: 'stock', label: t('reportsPage.stock'), icon: Package, description: 'Current inventory stock levels' },
    { key: 'lowstock', label: t('reportsPage.lowstock'), icon: AlertTriangle, description: 'Items below minimum stock level' },
    { key: 'expiry', label: t('reportsPage.expiry'), icon: Calendar, description: 'Items approaching expiry date' },
    { key: 'sales', label: t('reportsPage.sales'), icon: DollarSign, description: 'Pharmacy sales and revenue' },
    { key: 'income', label: t('reportsPage.income'), icon: TrendingUp, description: 'Income and revenue transactions' },
    { key: 'expenses', label: t('reportsPage.expenses'), icon: Activity, description: 'Expense tracking and analysis' },
  ];

  const dailyCols = [
    { accessorKey: 'date', header: t('reportsPage.date'), cell: ({ row }) => formatDate(row.original.date) },
    { accessorKey: 'farmName', header: t('reportsPage.farm') }, { accessorKey: 'batchCode', header: t('reportsPage.batch') },
    { accessorKey: 'openingBirdCount', header: t('reportsPage.opening') }, { accessorKey: 'mortality', header: t('reportsPage.mortalityCol') },
    { accessorKey: 'mortalityRate', header: t('reportsPage.rate'), cell: ({ row }) => row.original.mortalityRate ? `${row.original.mortalityRate.toFixed(2)}%` : '-' },
    { accessorKey: 'feedConsumed', header: t('reportsPage.feed') },
  ];

  const stockCols = [
    { accessorKey: 'itemName', header: t('reportsPage.item') }, { accessorKey: 'category', header: t('reportsPage.category') },
    { accessorKey: 'unit', header: t('reportsPage.unit') }, { accessorKey: 'currentStock', header: t('reportsPage.currentStock') },
    { accessorKey: 'minimumStockLevel', header: t('reportsPage.minLevel') },
  ];

  const expiryCols = [
    { accessorKey: 'item', header: t('reportsPage.item'), cell: ({ row }) => row.original.item?.itemName },
    { accessorKey: 'batchNumber', header: t('reportsPage.batch') }, { accessorKey: 'quantityRemaining', header: t('reportsPage.qty') },
    { accessorKey: 'expiryDate', header: t('reportsPage.expiryCol'), cell: ({ row }) => formatDate(row.original.expiryDate) },
  ];

  const salesCols = [
    { accessorKey: 'receiptNumber', header: t('reportsPage.receipt') }, { accessorKey: 'customerName', header: t('reportsPage.customer') },
    { accessorKey: 'saleDate', header: t('reportsPage.date'), cell: ({ row }) => formatDate(row.original.saleDate) },
    { accessorKey: 'totalAmount', header: t('reportsPage.amount'), cell: ({ row }) => formatCurrency(row.original.totalAmount) },
    { accessorKey: 'paymentMethod', header: t('reportsPage.payment') },
  ];

  const financeCols = [
    { accessorKey: 'transactionDate', header: t('reportsPage.date'), cell: ({ row }) => formatDate(row.original.transactionDate) },
    { accessorKey: 'category', header: t('reportsPage.category') }, { accessorKey: 'amount', header: t('reportsPage.amount'), cell: ({ row }) => formatCurrency(row.original.amount) },
    { accessorKey: 'description', header: t('reportsPage.description') },
  ];

  const getColumns = () => {
    if (['daily', 'mortality'].includes(activeReport)) return dailyCols;
    if (['stock', 'lowstock'].includes(activeReport)) return stockCols;
    if (activeReport === 'expiry') return expiryCols;
    if (activeReport === 'sales') return salesCols;
    return financeCols;
  };

  const supportsPeriodFilter = ['daily', 'mortality', 'sales', 'income', 'expenses', 'expiry'].includes(activeReport);
  const periodRange = getPeriodRange(reportPeriod);
  const filteredData = supportsPeriodFilter
    ? data.filter((row) => {
      const rowDate = normalizeDate(getReportDateValue(row));
      return rowDate && rowDate >= periodRange.start && rowDate <= periodRange.end;
    })
    : data;
  const columns = getColumns();
  const selectedReportLabel = reportTypes.find((report) => report.key === activeReport)?.label || t('reportsPage.title');

  const exportToExcel = () => {
    exportToCsv(filteredData, columns, `${activeReport}-${reportPeriod}-report`);
  };

  const handleExportPdf = () => {
    exportToPdf(selectedReportLabel, 'reports-data-table');
  };

  return (
    <EnterprisePage
      title="Report Center"
      subtitle="Generate and export comprehensive reports across all modules"
      breadcrumbs={['Reports']}
      exportData={filteredData}
      exportColumns={columns}
      exportFilename={`${activeReport}-report`}
      exportElementId="reports-data-table"
      exportTitle={selectedReportLabel}
    >
      {/* Report Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {reportTypes.map((report) => (
          <Card
            key={report.key}
            className={`cursor-pointer transition-all hover:shadow-md ${
              activeReport === report.key ? 'ring-2 ring-blue-500 bg-blue-50' : ''
            }`}
            onClick={() => setActiveReport(report.key)}
          >
            <CardContent className="p-4">
              <report.icon className={`h-8 w-8 mb-2 ${activeReport === report.key ? 'text-blue-600' : 'text-gray-400'}`} />
              <h3 className="font-semibold text-sm mb-1">{report.label}</h3>
              <p className="text-xs text-gray-500">{report.description}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Report Content */}
      <Card>
        <CardHeader>
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div>
              <CardTitle>{selectedReportLabel}</CardTitle>
              <p className="text-sm text-gray-500 mt-1">{filteredData.length} records found</p>
            </div>
            <div className="flex flex-col sm:flex-row gap-2">
              {supportsPeriodFilter && (
                <Select value={reportPeriod} onValueChange={setReportPeriod}>
                  <SelectTrigger className="w-[180px]">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {PERIOD_OPTIONS.map((period) => (
                      <SelectItem key={period} value={period}>
                        {t(`reportsPage.${period}Period`)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
              <Button variant="outline" onClick={exportToExcel}>
                <Download className="mr-2 h-4 w-4" />
                Excel
              </Button>
              <Button onClick={handleExportPdf}>
                <Printer className="mr-2 h-4 w-4" />
                PDF
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent id="reports-data-table">
          <DataTable columns={columns} data={filteredData} loading={loading} searchable pagination />
        </CardContent>
      </Card>
    </EnterprisePage>
  );
};

export default Reports;
