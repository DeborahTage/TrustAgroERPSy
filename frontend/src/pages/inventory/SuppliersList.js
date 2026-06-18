import React, { useState, useEffect } from 'react';
import { inventoryApi } from '../../api';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import PageHeader from '../../components/shared/PageHeader';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { useLanguage } from '../../context/LanguageContext';
import { Truck, Phone, Mail, MapPin, ExternalLink, Plus, Star } from 'lucide-react';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';

const SuppliersList = () => {
    const { t } = useLanguage();
    const [suppliers, setSuppliers] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const res = await inventoryApi.getMovements();
                const movements = res.data.data || [];
                // Extract unique suppliers from movements
                const uniqueSuppliers = [...new Set(movements.map(m => m.supplier).filter(Boolean))];

                // Mocking some professional data for extracted suppliers
                const mockSuppliers = uniqueSuppliers.map((name, index) => ({
                    id: index + 1,
                    name,
                    category: index % 2 === 0 ? 'Veterinary' : 'Feed',
                    contactPerson: 'Contact ' + (index + 1),
                    phone: '+251 911 223 344',
                    email: `${name.toLowerCase().replace(/ /g, '.')}@example.com`,
                    rating: 4.5,
                    status: 'Active',
                    lastOrder: '2026-05-15'
                }));

                setSuppliers(mockSuppliers);
            } catch (e) {
                console.error('Failed to fetch suppliers', e);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    const columns = [
        {
            accessorKey: 'name',
            header: 'Supplier',
            cell: ({ row }) => (
                <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-full bg-indigo-50 flex items-center justify-center border border-indigo-100">
                        <Truck className="h-5 w-5 text-indigo-600" />
                    </div>
                    <div className="flex flex-col">
                        <span className="font-bold text-gray-900">{row.original.name}</span>
                        <span className="text-xs text-gray-500">{row.original.category}</span>
                    </div>
                </div>
            )
        },
        {
            accessorKey: 'contact',
            header: 'Contact Info',
            cell: ({ row }) => (
                <div className="space-y-1">
                    <div className="flex items-center text-xs text-gray-600 gap-1.5">
                        <Phone className="h-3 w-3" /> {row.original.phone}
                    </div>
                    <div className="flex items-center text-xs text-gray-600 gap-1.5">
                        <Mail className="h-3 w-3" /> {row.original.email}
                    </div>
                </div>
            )
        },
        {
            accessorKey: 'performance',
            header: 'Performance',
            cell: ({ row }) => (
                <div className="flex items-center gap-1 text-amber-500">
                    <Star className="h-3.5 w-3.5 fill-current" />
                    <span className="text-xs font-bold">{row.original.rating}</span>
                </div>
            )
        },
        { accessorKey: 'lastOrder', header: 'Last Order', cell: ({ row }) => row.original.lastOrder },
        { accessorKey: 'status', header: 'Status', cell: () => <StatusBadge status="active" /> },
        {
            id: 'actions',
            cell: () => (
                <Button variant="ghost" size="sm">
                    <ExternalLink className="h-4 w-4" />
                </Button>
            )
        }
    ];

    return (
        <div className="space-y-6 pb-8">
            <PageHeader
                title="Supplier Partnership Management"
                subtitle="Manage vendor relationships, monitor procurement performance, and track delivery reliability."
            >
                <Button>
                    <Plus className="mr-2 h-4 w-4" />
                    Add New Supplier
                </Button>
            </PageHeader>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <Card className="bg-indigo-600 border-none">
                    <CardContent className="pt-6">
                        <p className="text-indigo-100 text-sm font-medium uppercase tracking-wider">Total Partners</p>
                        <p className="text-3xl font-bold text-white mt-1">{suppliers.length}</p>
                        <div className="mt-4 flex items-center gap-2">
                            <Badge className="bg-indigo-500 text-white border-none">+2 this month</Badge>
                        </div>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader className="pb-2">
                        <CardTitle className="text-sm font-medium text-gray-500 uppercase">Avg Delivery Reliability</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <p className="text-3xl font-bold text-gray-900">94.2%</p>
                        <p className="text-xs text-emerald-600 mt-1 font-medium">↑ 1.5% improvement</p>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader className="pb-2">
                        <CardTitle className="text-sm font-medium text-gray-500 uppercase">Open Procurement Issues</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <p className="text-3xl font-bold text-red-600">3</p>
                        <p className="text-xs text-red-400 mt-1 font-medium">Requires immediate attention</p>
                    </CardContent>
                </Card>
            </div>

            <Card>
                <CardHeader>
                    <CardTitle>Approved Vendor List</CardTitle>
                </CardHeader>
                <CardContent>
                    <DataTable
                        columns={columns}
                        data={suppliers}
                        loading={loading}
                        searchable
                        pagination
                    />
                </CardContent>
            </Card>
        </div>
    );
};

export default SuppliersList;
