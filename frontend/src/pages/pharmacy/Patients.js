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
        <div className="p-6">
            <h1 className="text-2xl font-bold mb-6">Patient Profiles (Flocks)</h1>
            <div className="bg-white rounded shadow-sm border p-4">
                <table className="w-full text-left border-collapse">
                    <thead>
                        <tr className="border-b">
                            <th className="py-2 px-4">Flock / Patient Name</th>
                            <th className="py-2 px-4">Breed</th>
                            <th className="py-2 px-4">Age (days)</th>
                            <th className="py-2 px-4">Avg Weight (kg)</th>
                            <th className="py-2 px-4">Farm Manager</th>
                            <th className="py-2 px-4">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {patients.map((p, idx) => (
                            <tr key={idx} className="border-b hover:bg-gray-50 transition">
                                <td className="py-3 px-4 font-medium text-gray-800">{p.patientName}</td>
                                <td className="py-3 px-4">{p.breed || '-'}</td>
                                <td className="py-3 px-4">{p.ageDays || '-'}</td>
                                <td className="py-3 px-4">{p.weightAvgKg || '-'}</td>
                                <td className="py-3 px-4">{p.farmManagerName || '-'}</td>
                                <td className="py-3 px-4">
                                    <button onClick={() => viewHistory(p.id)} className="text-blue-600 hover:text-blue-800 font-medium">
                                        View History
                                    </button>
                                </td>
                            </tr>
                        ))}
                        {patients.length === 0 && (
                            <tr>
                                <td colSpan="6" className="py-8 text-center text-gray-500">
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
