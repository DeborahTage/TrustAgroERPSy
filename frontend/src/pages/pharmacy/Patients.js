import React, { useState, useEffect } from 'react';
import axios from '../../api/axios';

export default function PharmacyPatients() {
    const [patients, setPatients] = useState([]);

    useEffect(() => {
        // Attempting to fetch patients
        // Usually via /api/pharmacy/patients (assuming the endpoint exists, we mock if not)
        axios.get('/api/pharmacy/prescriptions')
            .then(res => {
                // Canonical queue returns ApiResponse; no separate pharmacy patient entity in this flow
                const rxList = res.data?.data || res.data || [];
                const flockMap = new Map();
                (Array.isArray(rxList) ? rxList : []).forEach((rx) => {
                    const key = rx.flockId || rx.farmId;
                    if (!key) return;
                    if (!flockMap.has(key)) {
                        flockMap.set(key, {
                            id: key,
                            name: rx.batchCode || rx.flockName || rx.farmName || `Flock ${key}`,
                            farmName: rx.farmName,
                        });
                    }
                });
                setPatients(Array.from(flockMap.values()));
            })
            .catch(e => console.error(e));
    }, []);

    const viewHistory = (patientId) => {
        alert(`Viewing history for patient ${patientId} is not fully implemented yet.`);
    };

    return (
        <div className="p-6 space-y-6">
            <div>
                <h1 className="text-2xl font-black text-foreground tracking-tight">Patient Profiles (Flocks)</h1>
                <p className="text-xs font-medium text-muted-foreground mt-0.5">Clinical flock records derived from veterinary prescriptions</p>
            </div>
            <div className="erp-card rounded-2xl border border-border p-5 overflow-x-auto">
                <table className="w-full text-left border-collapse">
                    <thead className="erp-table-head">
                        <tr className="border-b border-border">
                            <th className="py-2.5 px-4 text-xs font-semibold text-muted-foreground uppercase">Flock / Patient Name</th>
                            <th className="py-2.5 px-4 text-xs font-semibold text-muted-foreground uppercase">Breed</th>
                            <th className="py-2.5 px-4 text-xs font-semibold text-muted-foreground uppercase">Age (days)</th>
                            <th className="py-2.5 px-4 text-xs font-semibold text-muted-foreground uppercase">Avg Weight (kg)</th>
                            <th className="py-2.5 px-4 text-xs font-semibold text-muted-foreground uppercase">Farm Manager</th>
                            <th className="py-2.5 px-4 text-xs font-semibold text-muted-foreground uppercase">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                        {patients.map((p, idx) => (
                            <tr key={idx} className="hover:bg-muted/40 transition-colors">
                                <td className="py-3 px-4 font-semibold text-foreground">{p.name || p.patientName}</td>
                                <td className="py-3 px-4 text-muted-foreground">{p.breed || '-'}</td>
                                <td className="py-3 px-4 text-muted-foreground">{p.ageDays || '-'}</td>
                                <td className="py-3 px-4 text-muted-foreground">{p.weightAvgKg || '-'}</td>
                                <td className="py-3 px-4 text-muted-foreground">{p.farmName || p.farmManagerName || '-'}</td>
                                <td className="py-3 px-4">
                                    <button onClick={() => viewHistory(p.id)} className="text-primary hover:underline font-semibold text-sm">
                                        View History
                                    </button>
                                </td>
                            </tr>
                        ))}
                        {patients.length === 0 && (
                            <tr>
                                <td colSpan="6" className="py-8 text-center text-muted-foreground">
                                    No active pharmacy patients found. (Data flows from populated prescriptions)
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
