import React from 'react';
import PageContainer from './PageContainer';
import PageHeader from '../shared/PageHeader';
import LoadingState from '../shared/LoadingState';
import { Button } from '../ui/button';
import { Card, CardContent } from '../ui/card';
import { AlertCircle, ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

/**
 * Standard shell for create/edit forms across the ERP.
 */
const EnterpriseFormPage = ({
  title,
  subtitle,
  breadcrumbs = [],
  backTo,
  loading = false,
  error = null,
  children,
}) => {
  const navigate = useNavigate();

  if (loading) {
    return (
      <PageContainer>
        <PageHeader title={title} subtitle={subtitle} breadcrumbs={breadcrumbs} />
        <LoadingState count={4} message="Loading form..." />
      </PageContainer>
    );
  }

  return (
    <PageContainer className="max-w-3xl mx-auto">
      <PageHeader title={title} subtitle={subtitle} breadcrumbs={breadcrumbs}>
        {backTo && (
          <Button variant="outline" onClick={() => navigate(backTo)} className="h-11 rounded-xl">
            <ArrowLeft className="h-4 w-4 mr-2" /> Back
          </Button>
        )}
      </PageHeader>

      {error && (
        <div className="flex items-center gap-3 p-4 bg-rose-50 border border-rose-200 rounded-xl text-sm text-rose-700">
          <AlertCircle className="h-5 w-5 shrink-0" />
          {error}
        </div>
      )}

      <Card className="rounded-2xl shadow-soft">
        <CardContent className="p-6 md:p-8 space-y-6">{children}</CardContent>
      </Card>
    </PageContainer>
  );
};

export default EnterpriseFormPage;
