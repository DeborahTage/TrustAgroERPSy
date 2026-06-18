import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Plus, CheckCircle, XCircle, Loader2 } from 'lucide-react';
import { masterDataApi } from '../../api';
import DataTable from '../../components/shared/DataTable';
import { toast } from 'react-toastify';
import { useMasterData } from '../../context/MasterDataContext';

const CATEGORIES = [
    { id: 'BREED', name: 'Breeds' },
    { id: 'DRUG_TYPE', name: 'Drug Types' },
    { id: 'FEED_TYPE', name: 'Feed Types' },
    { id: 'DISEASE_TYPE', name: 'Disease Types' },
    { id: 'VACCINE_TYPE', name: 'Vaccine Types' },
    { id: 'FLOCK_PURPOSE', name: 'Flock Purposes' },
];

const MasterDataTab = () => {
    const [activeCategory, setActiveCategory] = useState(CATEGORIES[0].id);
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(false);

    // Refresh global context when we modify MDM
    const { refresh } = useMasterData(activeCategory);

    const fetchItems = async () => {
        setLoading(true);
        try {
            const response = await masterDataApi.getByCategory(activeCategory, false);
            if (response.data?.success) {
                setItems(response.data.data);
            }
        } catch (error) {
            toast.error('Failed to fetch master data items');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchItems();
    }, [activeCategory]);

    const [newItemParams, setNewItemParams] = useState({ value: '', label: '', description: '' });

    const handleAdd = async () => {
        if (!newItemParams.value || !newItemParams.label) {
            toast.warning('Value and Label are required');
            return;
        }
        setLoading(true);
        try {
            const resp = await masterDataApi.create({
                category: activeCategory,
                value: newItemParams.value,
                label: newItemParams.label,
                description: newItemParams.description
            });
            if (resp.data?.success) {
                toast.success('Master Data Item added');
                setNewItemParams({ value: '', label: '', description: '' });
                fetchItems();
                refresh(); // Refresh global context
            }
        } catch (error) {
            toast.error(error.response?.data?.message || 'Failed to add item');
        } finally {
            setLoading(false);
        }
    };

    const handleToggleStatus = async (id) => {
        try {
            const resp = await masterDataApi.toggleStatus(id);
            if (resp.data?.success) {
                toast.success('Status updated');
                fetchItems();
                refresh();
            }
        } catch (error) {
            toast.error('Failed to update status');
        }
    };

    const columns = [
        { accessorKey: 'label', header: 'Label', cell: ({ row }) => <span className="font-medium text-gray-900">{row.original.label}</span> },
        { accessorKey: 'value', header: 'System Value', cell: ({ row }) => <code className="text-xs bg-gray-100 px-2 py-1 rounded">{row.original.value}</code> },
        { accessorKey: 'description', header: 'Description', cell: ({ row }) => <span className="text-gray-500 text-sm">{row.original.description || '-'}</span> },
        {
            accessorKey: 'active',
            header: 'Status',
            cell: ({ row }) => (
                <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium ${row.original.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                    {row.original.active ? <CheckCircle className="w-3.5 h-3.5" /> : <XCircle className="w-3.5 h-3.5" />}
                    {row.original.active ? 'Active' : 'Inactive'}
                </span>
            ),
        },
        {
            id: 'actions',
            header: 'Actions',
            cell: ({ row }) => (
                <Button variant="ghost" size="sm" onClick={() => handleToggleStatus(row.original.id)}>
                    {row.original.active ? 'Deactivate' : 'Activate'}
                </Button>
            ),
        },
    ];

    return (
        <Card>
            <CardHeader>
                <CardTitle>Master Data Management</CardTitle>
            </CardHeader>
            <CardContent>
                <div className="flex flex-col md:flex-row gap-6">
                    {/* Sidebar */}
                    <div className="w-full md:w-64 flex-shrink-0 border-r border-gray-100 pr-4 space-y-1">
                        <div className="text-sm font-medium text-gray-500 uppercase tracking-wider mb-3 px-3">Categories</div>
                        {CATEGORIES.map((cat) => (
                            <button
                                key={cat.id}
                                onClick={() => setActiveCategory(cat.id)}
                                className={`w-full text-left px-3 py-2 rounded-lg transition-colors duration-200 text-sm font-medium ${activeCategory === cat.id ? 'bg-indigo-50 text-indigo-700' : 'text-gray-600 hover:bg-gray-50'
                                    }`}
                            >
                                {cat.name}
                            </button>
                        ))}
                    </div>

                    {/* Main Content */}
                    <div className="flex-1 min-w-0 flex flex-col items-center">
                        {/* Add new form */}
                        <div className="bg-gray-50 p-4 rounded-xl mb-6 flex flex-wrap gap-3 items-end border border-gray-200 shadow-sm w-[90%] md:w-full">
                            <div className="flex-1 min-w-[200px]">
                                <label className="block text-xs font-medium text-gray-500 mb-1">Display Label</label>
                                <Input
                                    placeholder="e.g. Broiler Chicken"
                                    value={newItemParams.label}
                                    onChange={e => setNewItemParams({ ...newItemParams, label: e.target.value })}
                                />
                            </div>
                            <div className="flex-1 min-w-[150px]">
                                <label className="block text-xs font-medium text-gray-500 mb-1">System Value</label>
                                <Input
                                    placeholder="e.g. BROILER"
                                    value={newItemParams.value}
                                    onChange={e => setNewItemParams({ ...newItemParams, value: e.target.value.toUpperCase().replace(/\s+/g, '_') })}
                                />
                            </div>
                            <div className="flex-1 min-w-[200px]">
                                <label className="block text-xs font-medium text-gray-500 mb-1">Description (Optional)</label>
                                <Input
                                    placeholder="Brief description"
                                    value={newItemParams.description}
                                    onChange={e => setNewItemParams({ ...newItemParams, description: e.target.value })}
                                />
                            </div>
                            <Button onClick={handleAdd} disabled={loading} className="gap-2">
                                {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
                                Add
                            </Button>
                        </div>

                        {/* Data Table */}
                        <div className="w-[90%] md:w-full">
                            <DataTable
                                columns={columns}
                                data={items}
                                loading={loading}
                                emptyMessage="No items found for this category."
                                hoverable
                            />
                        </div>
                    </div>
                </div>
            </CardContent>
        </Card>
    );
};

export default MasterDataTab;
