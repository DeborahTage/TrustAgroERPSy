import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Select } from '../../components/ui/select';
import { Search, Loader2, ChevronDown, ChevronUp } from 'lucide-react';
import { auditApi } from '../../api';
import { toast } from 'react-toastify';

const AuditLogViewer = () => {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(false);
    const [filters, setFilters] = useState({
        module: '',
        entityType: '',
        action: ''
    });
    const [expandedId, setExpandedId] = useState(null);

    const fetchLogs = async () => {
        setLoading(true);
        try {
            // Build simple params object
            const params = {};
            if (filters.module) params.module = filters.module;
            if (filters.entityType) params.entityType = filters.entityType;
            if (filters.action) params.action = filters.action;

            const response = await auditApi.getAll(params);
            if (response.data?.success) {
                setLogs(response.data.data);
            }
        } catch (error) {
            toast.error('Failed to load audit logs.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchLogs();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [filters]);

    const toggleExpand = (id) => {
        setExpandedId(expandedId === id ? null : id);
    };

    const parseJson = (str) => {
        if (!str) return null;
        try {
            return JSON.parse(str);
        } catch {
            return { raw: str }; // fallback 
        }
    };

    return (
        <Card>
            <CardHeader>
                <CardTitle>System Audit Trail</CardTitle>
            </CardHeader>
            <CardContent>
                {/* Filter Bar */}
                <div className="flex flex-wrap gap-4 mb-6">
                    <div className="w-48">
                        <label className="text-xs font-medium text-gray-500 mb-1 block">Module</label>
                        <Select
                            value={filters.module}
                            onChange={(e) => setFilters({ ...filters, module: e.target.value })}
                            options={[
                                { value: '', label: 'All Modules' },
                                { value: 'VETERINARY', label: 'Veterinary' },
                                { value: 'INVENTORY', label: 'Inventory' },
                                { value: 'FARM', label: 'Farm' },
                                { value: 'FINANCE', label: 'Finance' },
                                { value: 'MDM', label: 'Master Data' }
                            ]}
                        />
                    </div>
                    <div className="w-48">
                        <label className="text-xs font-medium text-gray-500 mb-1 block">Action</label>
                        <Select
                            value={filters.action}
                            onChange={(e) => setFilters({ ...filters, action: e.target.value })}
                            options={[
                                { value: '', label: 'All Actions' },
                                { value: 'CREATE', label: 'Create' },
                                { value: 'UPDATE', label: 'Update' },
                                { value: 'DELETE', label: 'Delete' },
                                { value: 'TOGGLE_STATUS', label: 'Toggle Status' },
                                { value: 'STOCK_IN', label: 'Stock In' },
                                { value: 'STOCK_OUT', label: 'Stock Out' }
                            ]}
                        />
                    </div>
                    <div className="w-48">
                        <label className="text-xs font-medium text-gray-500 mb-1 block">Entity Type</label>
                        <Input
                            placeholder="e.g. VACCINATION"
                            value={filters.entityType}
                            onChange={(e) => setFilters({ ...filters, entityType: e.target.value })}
                        />
                    </div>
                    <div className="flex items-end self-end mb-[2px]">
                        <Button variant="outline" onClick={fetchLogs} className="gap-2">
                            <Search className="w-4 h-4" /> Refresh
                        </Button>
                    </div>
                </div>

                {/* Audit Table */}
                <div className="overflow-x-auto border border-gray-200 rounded-lg">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-gray-50 text-gray-500 font-medium">
                            <tr>
                                <th className="px-4 py-3 border-b">Timestamp</th>
                                <th className="px-4 py-3 border-b">User</th>
                                <th className="px-4 py-3 border-b">Action</th>
                                <th className="px-4 py-3 border-b">Module & Entity</th>
                                <th className="px-4 py-3 border-b">Details</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {loading && logs.length === 0 ? (
                                <tr>
                                    <td colSpan="5" className="px-4 py-8 text-center text-gray-500">
                                        <Loader2 className="w-5 h-5 animate-spin mx-auto mb-2" />
                                        Loading audit trail...
                                    </td>
                                </tr>
                            ) : logs.length === 0 ? (
                                <tr>
                                    <td colSpan="5" className="px-4 py-8 text-center text-gray-500">No logs found matching the criteria.</td>
                                </tr>
                            ) : (
                                logs.map((log) => {
                                    const isExpanded = expandedId === log.id;
                                    let oldParsed = parseJson(log.oldValue);
                                    let newParsed = parseJson(log.newValue);
                                    const hasDetails = oldParsed || newParsed;

                                    return (
                                        <React.Fragment key={log.id}>
                                            <tr className={`${isExpanded ? 'bg-primary/10/30' : 'hover:bg-gray-50'}`}>
                                                <td className="px-4 py-3 whitespace-nowrap text-gray-500">
                                                    {new Date(log.createdAt).toLocaleString()}
                                                </td>
                                                <td className="px-4 py-3">
                                                    <div className="font-medium text-gray-900">{log.userEmail || 'System'}</div>
                                                </td>
                                                <td className="px-4 py-3">
                                                    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold
                            ${log.action === 'CREATE' ? 'bg-green-100 text-green-800' :
                                                            log.action === 'UPDATE' ? 'bg-blue-100 text-blue-800' :
                                                                log.action.includes('DELETE') ? 'bg-red-100 text-red-800' :
                                                                    'bg-gray-100 text-gray-800'}`}>
                                                        {log.action}
                                                    </span>
                                                </td>
                                                <td className="px-4 py-3">
                                                    <div className="font-medium text-gray-900">{log.module}</div>
                                                    <div className="text-xs text-gray-500 tracking-wider">
                                                        {log.entityType} {log.entityId ? `#${log.entityId}` : ''}
                                                    </div>
                                                </td>
                                                <td className="px-4 py-3 text-right">
                                                    {hasDetails ? (
                                                        <Button variant="ghost" size="sm" onClick={() => toggleExpand(log.id)}>
                                                            {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                                                        </Button>
                                                    ) : (
                                                        <span className="text-gray-400 text-xs italic">No payload</span>
                                                    )}
                                                </td>
                                            </tr>
                                            {isExpanded && hasDetails && (
                                                <tr>
                                                    <td colSpan="5" className="px-4 py-4 bg-gray-50 border-t border-gray-100">
                                                        <div className="grid grid-cols-2 gap-4">
                                                            {oldParsed && (
                                                                <div className="border border-red-100 bg-white rounded-md p-3">
                                                                    <div className="text-xs font-semibold text-red-600 mb-2 uppercase tracking-wider">Previous State</div>
                                                                    <pre className="text-xs text-gray-700 bg-gray-50 p-2 rounded overflow-auto max-h-48 border border-gray-100">
                                                                        {JSON.stringify(oldParsed, null, 2)}
                                                                    </pre>
                                                                </div>
                                                            )}
                                                            {newParsed && (
                                                                <div className="border border-green-100 bg-white rounded-md p-3">
                                                                    <div className="text-xs font-semibold text-green-600 mb-2 uppercase tracking-wider">New State</div>
                                                                    <pre className="text-xs text-gray-700 bg-gray-50 p-2 rounded overflow-auto max-h-48 border border-gray-100">
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
            </CardContent>
        </Card>
    );
};

export default AuditLogViewer;
