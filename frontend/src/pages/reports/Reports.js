import React, { useEffect, useState } from 'react';
import { dailyRecordApi, inventoryApi, financeApi, pharmacyApi } from '../../api';
import DataTable from '../../components/shared/DataTable';
import PageHeader from '../../components/shared/PageHeader';
import { useLanguage } from '../../context/LanguageContext';
import { formatDate, formatCurrency } from '../../utils';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { 
  FileText, 
  Download, 
  Printer, 
  BarChart3, 
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

const escapeHtml = (value) => String(value ?? '')
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&#39;');

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
        else if (activeReport === 'income') res = await financeApi.getIncome();
        else if (activeReport === 'expenses') res = await financeApi.getExpenses();
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
    { accessorKey: 'feedConsumed', header: t('reportsPage.feed') }, { accessorKey: 'eggProduction', header: t('reportsPage.eggs') },
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

  const getCellText = (row, column) => {
    const rawValue = column.render ? column.render(row) : row[column.key];
    if (rawValue === null || rawValue === undefined || rawValue === '') return '-';
    if (typeof rawValue === 'string' || typeof rawValue === 'number') return String(rawValue);
    return '-';
  };

  const downloadFile = (blob, filename) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  };

  const exportToExcel = () => {
    const headerRow = columns.map((column) => `<th>${escapeHtml(column.label)}</th>`).join('');
    const bodyRows = filteredData.map((row) => (
      `<tr>${columns.map((column) => `<td>${escapeHtml(getCellText(row, column))}</td>`).join('')}</tr>`
    )).join('');
    const html = `
      <html>
        <head><meta charset="utf-8" /></head>
        <body>
          <h2>${escapeHtml(selectedReportLabel)}</h2>
          <p>${escapeHtml(t(`reportsPage.${reportPeriod}Period`))}</p>
          <table border="1">
            <thead><tr>${headerRow}</tr></thead>
            <tbody>${bodyRows}</tbody>
          </table>
        </body>
      </html>
    `;
    downloadFile(
      new Blob(['\ufeff', html], { type: 'application/vnd.ms-excel;charset=utf-8;' }),
      `${activeReport}-${reportPeriod}-report.xls`
    );
  };

  const exportToPdf = () => {
    const printWindow = window.open('', '_blank', 'width=1100,height=800');
    if (!printWindow) return;

    const headerRow = columns.map((column) => `<th>${escapeHtml(column.label)}</th>`).join('');
    const bodyRows = filteredData.map((row) => (
      `<tr>${columns.map((column) => `<td>${escapeHtml(getCellText(row, column))}</td>`).join('')}</tr>`
    )).join('');

    printWindow.document.write(`
      <html>
        <head>
          <title>${escapeHtml(selectedReportLabel)}</title>
          <style>
            body { font-family: "Times New Roman", serif; padding: 24px; color: #1f2937; }
            h2 { margin-bottom: 8px; }
            p { margin-top: 0; color: #4b5563; }
            table { width: 100%; border-collapse: collapse; margin-top: 16px; }
            th, td { border: 1px solid #d1d5db; padding: 8px 10px; text-align: left; font-size: 12px; }
            th { background: #f3f4f6; }
          </style>
        </head>
        <body>
          <h2>${escapeHtml(selectedReportLabel)}</h2>
          <p>${escapeHtml(t(`reportsPage.${reportPeriod}Period`))}</p>
          <table>
            <thead><tr>${headerRow}</tr></thead>
            <tbody>${bodyRows}</tbody>
          </table>
        </body>
      </html>
    `);
    printWindow.document.close();
    printWindow.focus();
    setTimeout(() => {
      printWindow.print();
    }, 300);
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <PageHeader
        title="Report Center"
        description="Generate and export comprehensive reports across all modules."
      />

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
              <Button onClick={exportToPdf}>
                <Printer className="mr-2 h-4 w-4" />
                PDF
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <DataTable columns={columns} data={filteredData} loading={loading} searchable pagination />
        </CardContent>
      </Card>
    </div>
  );
};

export default Reports;
