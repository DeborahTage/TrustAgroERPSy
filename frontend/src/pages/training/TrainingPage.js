import React from 'react';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent } from '../../components/ui/card';
import { GraduationCap, Timer } from 'lucide-react';

const TrainingPage = () => (
  <EnterprisePage
    title="Training & Workshops"
    subtitle="Participant registrations and professional certification programs"
    breadcrumbs={['Training', 'Overview']}
  >
    <Card className="border-none shadow-sm bg-white rounded-2xl overflow-hidden py-12">
      <CardContent className="flex flex-col items-center justify-center text-center p-12">
        <div className="h-24 w-24 bg-primary/10 text-primary rounded-3xl flex items-center justify-center mb-6">
          <GraduationCap className="h-12 w-12" />
        </div>
        <h2 className="text-2xl font-bold text-gray-900">Training & Certification</h2>
        <p className="text-gray-500 text-sm mt-2 flex items-center gap-2">
          <Timer className="h-4 w-4" />
          Scheduled for a future ERP phase
        </p>
        <p className="max-w-md text-gray-400 mt-4 leading-relaxed">
          Workshop enrollment, attendance tracking, and automated fee posting to finance will be integrated here.
        </p>
      </CardContent>
    </Card>
  </EnterprisePage>
);

export default TrainingPage;
