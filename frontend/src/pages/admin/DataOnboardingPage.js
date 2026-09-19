import React, { useCallback, useEffect, useState } from 'react';
import { dataImportApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Button } from '../../components/ui/button';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { Badge } from '../../components/ui/badge';

const DataOnboardingPage = () => {
  const [summary, setSummary] = useState(null);
  const [templates, setTemplates] = useState(null);
  const [importType, setImportType] = useState('FARMS');
  const [csvText, setCsvText] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [s, t] = await Promise.all([
        dataImportApi.onboardingSummary(),
        dataImportApi.templates(),
      ]);
      setSummary(s.data?.data || s.data);
      setTemplates(t.data?.data || t.data);
    } catch {
      setError('Failed to load onboarding summary');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const runImport = async (dryRun, confirm = false) => {
    setError(null);
    setResult(null);
    try {
      const res = await dataImportApi.importData({
        type: importType,
        csv: csvText,
        dryRun,
        confirm,
      });
      setResult(res.data?.data || res.data);
      if (!dryRun && confirm) await load();
    } catch (e) {
      setError(e?.response?.data?.message || 'Import failed');
    }
  };

  const applyTemplate = () => {
    const sample = templates?.[importType];
    if (typeof sample === 'string') setCsvText(sample + '\n');
  };

  return (
    <EnterprisePage
      title="Data Onboarding"
      description="Dry-run CSV import for farms, batches (flocks), and inventory items. Expected targets: 4 farms / 83 batches — source data required."
      loading={loading}
      error={error}
      onRetry={load}
    >
      {summary && (
        <div className="grid gap-3 md:grid-cols-4 mb-6">
          <div className="border rounded-md p-3"><div className="text-xs text-slate-500">Farms in DB</div><div className="text-2xl font-semibold">{summary.farmCount ?? summary.farmsImported ?? 0}</div><div className="text-xs">Expected 4</div></div>
          <div className="border rounded-md p-3"><div className="text-xs text-slate-500">Batches in DB</div><div className="text-2xl font-semibold">{summary.flockCount ?? summary.batchesImported ?? 0}</div><div className="text-xs">Expected 83</div></div>
          <div className="border rounded-md p-3"><div className="text-xs text-slate-500">Inventory items</div><div className="text-2xl font-semibold">{summary.inventoryItemCount ?? 0}</div></div>
          <div className="border rounded-md p-3">
            <div className="text-xs text-slate-500">SENE inventory file</div>
            <Badge className="mt-2 shadow-none">{summary.inventorySourceFilePresent ? 'Present' : 'Not present'}</Badge>
          </div>
        </div>
      )}

      <div className="flex flex-wrap gap-2 mb-3">
        <Select value={importType} onValueChange={setImportType}>
          <SelectTrigger className="w-48"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="FARMS">FARMS</SelectItem>
            <SelectItem value="BATCHES">BATCHES</SelectItem>
            <SelectItem value="INVENTORY_ITEMS">INVENTORY_ITEMS</SelectItem>
          </SelectContent>
        </Select>
        <Button variant="outline" onClick={applyTemplate}>Load CSV header</Button>
        <Button variant="outline" onClick={() => runImport(true, false)}>Dry run</Button>
        <Button onClick={() => runImport(false, true)}>Confirm import</Button>
      </div>

      <Textarea
        className="min-h-[180px] font-mono text-sm mb-4"
        placeholder="Paste CSV rows here (header + data). Do not invent farm/batch values."
        value={csvText}
        onChange={(e) => setCsvText(e.target.value)}
      />

      {result && (
        <div className="border rounded-md p-4 bg-slate-50 space-y-1 text-sm">
          <div><strong>Dry run:</strong> {String(result.dryRun)}</div>
          <div>Rows found: {result.rowsFound} · Valid: {result.valid} · Invalid: {result.invalid}</div>
          <div>Will create: {result.willCreate} · Will update: {result.willUpdate} · Duplicates: {result.duplicates}</div>
          <div>Created: {result.created} · Updated: {result.updated} · Missing refs: {result.missingRefs}</div>
          {Array.isArray(result.errors) && result.errors.length > 0 && (
            <ul className="list-disc pl-5 text-rose-700">
              {result.errors.slice(0, 20).map((e, i) => <li key={i}>{typeof e === 'string' ? e : JSON.stringify(e)}</li>)}
            </ul>
          )}
        </div>
      )}
    </EnterprisePage>
  );
};

export default DataOnboardingPage;
