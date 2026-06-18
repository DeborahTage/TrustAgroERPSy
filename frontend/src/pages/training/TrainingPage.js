import React from 'react';
import PageContainer from '../../components/layout/PageContainer';
import PageHeader from '../../components/shared/PageHeader';
import { Card, CardContent } from '../../components/ui/card';
import { GraduationCap, Timer } from 'lucide-react';

const TrainingPage = () => {
    return (
        <PageContainer>
            <PageHeader
                title="Training & Workshops"
                subtitle="Manage participant registrations and professional certification programs."
                breadcrumbs={['Training', 'Overview']}
            />

            <Card className="border-none shadow-sm bg-white rounded-2xl overflow-hidden py-12">
                <CardContent className="flex flex-col items-center justify-center text-center p-12">
                    <div className="h-24 w-24 bg-indigo-50 text-indigo-600 rounded-3xl flex items-center justify-center mb-6 animate-bounce">
                        <GraduationCap className="h-12 w-12" />
                    </div>
                    <h2 className="text-3xl font-black text-gray-900 uppercase tracking-tight">Phase 5: Training Module</h2>
                    <p className="text-gray-500 font-bold uppercase tracking-widest text-xs mt-2 flex items-center gap-2">
                        <Timer className="h-4 w-4" />
                        Coming soon to your ERP
                    </p>
                    <p className="max-w-md text-gray-400 mt-4 font-medium leading-relaxed">
                        We are currently engineering the integration between participant registration and
                        financial ledger automation. This module will be live in the final phase.
                    </p>
                </CardContent>
            </Card>
        </PageContainer>
    );
};

export default TrainingPage;
