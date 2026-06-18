import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Table, TableBody, TableCell, TableHead, TableHeader, TableRow
} from '../../components/ui/table';
import { Badge } from '../../components/ui/badge';
import { ArrowUpRight, ClipboardList } from 'lucide-react';
import { cn } from '../../lib/utils';

const PrescriptionQueue = ({ prescriptions, loading }) => {
    const navigate = useNavigate();

    if (loading) {
        return (
            <div className="py-12 flex justify-center items-center">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
            </div>
        );
    }

    return (
        <Table>
            <TableHeader>
                <TableRow className="bg-gray-50/50">
                    <TableHead>ORDER #</TableHead>
                    <TableHead>FARM / FLOCK</TableHead>
                    <TableHead>MEDICATION</TableHead>
                    <TableHead>QTY</TableHead>
                    <TableHead>PRIORITY</TableHead>
                    <TableHead className="text-right">ACTION</TableHead>
                </TableRow>
            </TableHeader>
            <TableBody>
                {prescriptions.map((rx) => (
                    <TableRow key={rx.id} className="hover:bg-gray-50/50 transition-colors cursor-pointer" onClick={() => navigate(`/pharmacy/process-rx?rxId=${rx.id}`)}>
                        <TableCell className="font-mono text-[11px] font-bold text-gray-500">{rx.prescriptionNumber || `RX-${rx.id}`}</TableCell>
                        <TableCell>
                            <div className="flex flex-col">
                                <span className="font-bold text-gray-900">{rx.farmName || 'Internal Farm'}</span>
                                <span className="text-[10px] text-gray-400 font-bold uppercase tracking-widest">{rx.flockName || 'General Stock'}</span>
                            </div>
                        </TableCell>
                        <TableCell>
                            <div className="flex flex-col">
                                <span className="font-medium text-gray-700">{rx.drugName}</span>
                                <span className="text-[10px] text-gray-400 italic">via {rx.administrationRoute}</span>
                            </div>
                        </TableCell>
                        <TableCell className="font-bold text-indigo-600">{rx.quantity} <span className="text-[10px] text-gray-400 font-normal">units</span></TableCell>
                        <TableCell>
                            <Badge className={cn(
                                "text-[10px] h-5",
                                rx.priority === 'EMERGENCY' ? "bg-rose-500" : "bg-amber-500"
                            )}>
                                {rx.priority || 'NORMAL'}
                            </Badge>
                        </TableCell>
                        <TableCell className="text-right">
                            <button
                                onClick={(e) => {
                                    e.stopPropagation();
                                    navigate(`/pharmacy/process-rx?rxId=${rx.id}`);
                                }}
                                className="bg-indigo-600 hover:bg-indigo-700 text-white px-3 py-1.5 rounded-lg text-xs font-bold transition-all shadow-sm flex items-center gap-1.5 ml-auto"
                            >
                                <ArrowUpRight className="h-3 w-3" /> Process
                            </button>
                        </TableCell>
                    </TableRow>
                ))}
                {prescriptions.length === 0 && (
                    <TableRow>
                        <TableCell colSpan={6} className="text-center py-12 text-gray-400 italic">
                            No pending medical orders detected in queue.
                        </TableCell>
                    </TableRow>
                )}
            </TableBody>
        </Table>
    );
};

export default PrescriptionQueue;
