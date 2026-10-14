import React, { useCallback, useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Select } from '../../components/ui/select';
import { Search, Loader2, ChevronDown, ChevronUp, ChevronLeft, ChevronRight } from 'lucide-react';
import { auditApi } from '../../api';
import { toast } from 'react-toastify';

const AuditLogViewer = () => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [filters, setFilters] = useState({
    module: '',
    entityType: '',
    action: '',
    userId: '',
    keyword: '',
    startDate: '',
    endDate: '',
  });
  const [expandedId, setExpandedId] = useState(null);
  const [detail, setDetail] = useState(null);

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size: 20 };
      if (filters.module) params.module = filters.module;
      if (filters.entityType) params.entityType = filters.entityType;
      if (filters.action) params.action = filters.action;
      if (filters.userId) params.userId = filters.userId;
      if (filters.keyword) params.keyword = filters.keyword;
      if (filters.startDate) params.startDate = new Date(filters.startDate).toISOString();
      if (filters.endDate) params.endDate = new Date(filters.endDate).toISOString();

      const response = await auditApi.getAll(params);
      if (response.data?.success) {
        const data = response.data.data || {};
        setLogs(Array.isArray(data) ? data : (data.content || []));
        setTotalPages(data.totalPages || 0);
        setTotalElements(data.totalElements || 0);
      }
    } catch {
      toast.error('Failed to load audit logs.');
    } finally {
      setLoading(false);
    }
  }, [filters, page]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const toggleExpand = async (id) => {
    if (expandedId === id) {
      setExpandedId(null);
      setDetail(null);
      return;
    }
    setExpandedId(id);
    try {
      const res = await auditApi.getById(id);
      if (res.data?.success) {
        setDetail(res.data.data);
      }
    } catch {
      setDetail(logs.find((l) => l.id === id) || null);
    }
  };

  const parseJson = (str) => {
    if (!str) return null;
    try {
      return JSON.parse(str);
    } catch {
      return { raw: str };
    }
  };

  const onFilterChange = (patch) => {
    setPage(0);
    setFilters((prev) => ({ ...prev, ...patch }));
  };

  return (
    <Card className="erp-card">
      <CardHeader>
        <CardTitle>System Audit Trail</CardTitle>
      </CardHeader>
      <CardContent>
          <div className="flex flex-wrap gap-4 mb-6">
            <div className="w-44">
              <label className="text-xs font-medium text-muted-foreground mb-1 block">Module</label>
              <Select
                value={filters.module}
                onChange={(e) => onFilterChange({ module: e.target.value })}
                options={[
                  { value: '', label: 'All Modules' },
                  { value: 'SECURITY', label: 'Security' },
                  { value: 'VETERINARY', label: 'Veterinary' },
                  { value: 'INVENTORY', label: 'Inventory' },
                  { value: 'FARM', label: 'Farm' },
                  { value: 'FINANCE', label: 'Finance' },
                  { value: 'CRM', label: 'CRM' },
                  { value: 'PROCUREMENT', label: 'Procurement' },
                  { value: 'MDM', label: 'Master Data' },
                ]}
              />
            </div>
            <div className="w-44">
              <label className="text-xs font-medium text-muted-foreground mb-1 block">Action</label>
              <Select
                value={filters.action}
                onChange={(e) => onFilterChange({ action: e.target.value })}
                options={[
                  { value: '', label: 'All Actions' },
                  { value: 'CREATE', label: 'Create' },
                  { value: 'UPDATE', label: 'Update' },
                  { value: 'DEACTIVATE', label: 'Deactivate' },
                  { value: 'LOGIN_SUCCESS', label: 'Login Success' },
                  { value: 'LOGIN_FAILED', label: 'Login Failed' },
                  { value: 'PASSWORD_CHANGE', label: 'Password Change' },
                  { value: 'STOCK_IN', label: 'Stock In' },
                  { value: 'STOCK_OUT', label: 'Stock Out' },
                  { value: 'FINANCE_POSTED', label: 'Finance Posted' },
                ]}
              />
            </div>
            <div className="w-40">
              <label className="text-xs font-medium text-muted-foreground mb-1 block">Entity Type</label>
              <Input
                placeholder="e.g. User"
                value={filters.entityType}
                onChange={(e) => onFilterChange({ entityType: e.target.value })}
              />
            </div>
            <div className="w-32">
              <label className="text-xs font-medium text-muted-foreground mb-1 block">User ID</label>
              <Input
                type="number"
                placeholder="ID"
                value={filters.userId}
                onChange={(e) => onFilterChange({ userId: e.target.value })}
              />
            </div>
            <div className="w-48">
              <label className="text-xs font-medium text-muted-foreground mb-1 block">Keyword</label>
              <Input
                placeholder="Search..."
                value={filters.keyword}
                onChange={(e) => onFilterChange({ keyword: e.target.value })}
              />
            </div>
            <div className="w-44">
              <label className="text-xs font-medium text-muted-foreground mb-1 block">Start</label>
              <Input
                type="datetime-local"
                value={filters.startDate}
                onChange={(e) => onFilterChange({ startDate: e.target.value })}
              />
            </div>
            <div className="w-44">
              <label className="text-xs font-medium text-muted-foreground mb-1 block">End</label>
              <Input
                type="datetime-local"
                value={filters.endDate}
                onChange={(e) => onFilterChange({ endDate: e.target.value })}
              />
            </div>
            <div className="flex items-end">
              <Button variant="outline" onClick={fetchLogs} className="gap-2">
                <Search className="w-4 h-4" /> Refresh
              </Button>
            </div>
          </div>

          <div className="overflow-x-auto border border-border rounded-xl">
            <table className="w-full text-sm text-left">
              <thead className="erp-table-head font-medium">
                <tr>
                  <th className="px-4 py-3 border-b border-border">Timestamp</th>
                  <th className="px-4 py-3 border-b border-border">User</th>
                  <th className="px-4 py-3 border-b border-border">Action</th>
                  <th className="px-4 py-3 border-b border-border">Module & Entity</th>
                  <th className="px-4 py-3 border-b border-border">Result</th>
                  <th className="px-4 py-3 border-b border-border">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {loading && logs.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="px-4 py-8 text-center text-muted-foreground">
                      <Loader2 className="w-5 h-5 animate-spin mx-auto mb-2" />
                      Loading audit trail...
                    </td>
                  </tr>
                ) : logs.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="px-4 py-8 text-center text-muted-foreground">No logs found matching the criteria.</td>
                  </tr>
                ) : (
                  logs.map((log) => {
                    const isExpanded = expandedId === log.id;
                    const row = isExpanded && detail?.id === log.id ? detail : log;
                    const oldParsed = parseJson(row.oldValue);
                    const newParsed = parseJson(row.newValue);
                    const hasDetails = oldParsed || newParsed || row.ipAddress;

                    return (
                      <React.Fragment key={log.id}>
                        <tr className={`${isExpanded ? 'bg-primary/10' : 'hover:bg-muted/40'}`}>
                          <td className="px-4 py-3 whitespace-nowrap text-muted-foreground">
                            {new Date(log.createdAt).toLocaleString()}
                          </td>
                          <td className="px-4 py-3">
                            <div className="font-medium text-foreground">{log.userEmail || 'System'}</div>
                            {log.userId && <div className="text-xs text-muted-foreground">#{log.userId}</div>}
                          </td>
                          <td className="px-4 py-3">
                            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-muted text-foreground">
                              {log.action}
                            </span>
                          </td>
                          <td className="px-4 py-3">
                            <div className="font-medium text-foreground">{log.module}</div>
                            <div className="text-xs text-muted-foreground">
                              {log.entityType} {log.entityId ? `#${log.entityId}` : ''}
                            </div>
                          </td>
                          <td className="px-4 py-3 text-xs">{log.result || 'SUCCESS'}</td>
                          <td className="px-4 py-3 text-right">
                            {hasDetails || true ? (
                              <Button variant="ghost" size="sm" onClick={() => toggleExpand(log.id)}>
                                {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                              </Button>
                            ) : null}
                          </td>
                        </tr>
                        {isExpanded && (
                          <tr>
                            <td colSpan="6" className="px-4 py-4 bg-muted/30 border-t border-border">
                              {row.ipAddress && (
                                <div className="text-xs text-muted-foreground mb-2">IP: {row.ipAddress}</div>
                              )}
                              <div className="grid grid-cols-2 gap-4">
                                {oldParsed && (
                                  <div className="border border-red-500/20 bg-card rounded-md p-3">
                                    <div className="text-xs font-semibold text-rose-500 mb-2 uppercase tracking-wider">Previous</div>
                                    <pre className="text-xs text-foreground bg-muted/50 p-2 rounded overflow-auto max-h-48 border border-border">
                                      {JSON.stringify(oldParsed, null, 2)}
                                    </pre>
                                  </div>
                                )}
                                {newParsed && (
                                  <div className="border border-emerald-500/20 bg-card rounded-md p-3">
                                    <div className="text-xs font-semibold text-emerald-500 mb-2 uppercase tracking-wider">New</div>
                                    <pre className="text-xs text-foreground bg-muted/50 p-2 rounded overflow-auto max-h-48 border border-border">
                                      {JSON.stringify(newParsed, null, 2)}
                                    </pre>
                                  </div>
                                )}
                              </div>
                            </td>
                          </tr>
                        )}
                      </React.Fragment>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          <div className="flex items-center justify-between mt-4 text-sm text-muted-foreground">
            <span>{totalElements} total</span>
            <div className="flex items-center gap-2">
              <Button variant="outline" size="sm" disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>
                <ChevronLeft className="w-4 h-4" />
              </Button>
              <span>Page {page + 1} / {Math.max(totalPages, 1)}</span>
              <Button variant="outline" size="sm" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>
                <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
  );
};

export default AuditLogViewer;
