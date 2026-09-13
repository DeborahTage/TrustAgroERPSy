/**
 * Export tabular data to CSV (Excel-compatible) or trigger browser print for PDF.
 */
export function exportToCsv(rows, columns, filename = 'export') {
  if (!rows?.length) return;

  const headers = columns.map((c) => c.header || c.accessorKey || c.id);
  const keys = columns.map((c) => c.accessorKey || c.id);

  const escape = (val) => {
    const str = val == null ? '' : String(val);
    return str.includes(',') || str.includes('"') || str.includes('\n')
      ? `"${str.replace(/"/g, '""')}"`
      : str;
  };

  const lines = [
    headers.map(escape).join(','),
    ...rows.map((row) => keys.map((k) => escape(typeof k === 'function' ? k(row) : row[k])).join(',')),
  ];

  const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${filename}-${new Date().toISOString().slice(0, 10)}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

export function exportToPdf(title, elementId) {
  const el = document.getElementById(elementId);
  if (!el) {
    window.print();
    return;
  }
  const printWindow = window.open('', '_blank');
  printWindow.document.write(`
    <html><head><title>${title}</title>
    <style>
      body { font-family: system-ui, sans-serif; padding: 24px; color: #111; }
      table { width: 100%; border-collapse: collapse; font-size: 12px; }
      th, td { border: 1px solid #e5e7eb; padding: 8px; text-align: left; }
      th { background: #f9fafb; font-weight: 600; }
      h1 { font-size: 18px; margin-bottom: 16px; }
    </style></head><body>
    <h1>${title}</h1>
    ${el.outerHTML}
    </body></html>
  `);
  printWindow.document.close();
  printWindow.focus();
  printWindow.print();
}
