import React from 'react';
import PageContainer from './PageContainer';
import PageHeader from '../shared/PageHeader';
import StatsCard from '../shared/StatsCard';
import LoadingState from '../shared/LoadingState';
import EmptyState from '../shared/EmptyState';
import { Button } from '../ui/button';
import { RefreshCw, Download, Printer, AlertCircle } from 'lucide-react';
import { exportToCsv, exportToPdf } from '../../lib/exportTable';
import { cn } from '../../lib/utils';

/**
 * Standard enterprise page shell used across all ERP modules.
 * Provides: header, breadcrumbs, KPI row, toolbar, loading/error/empty states.
 */
const EnterprisePage = ({
  title,
  subtitle,
  breadcrumbs = [],
  loading = false,
  error = null,
  onRetry,
  stats = [],
  actions,
  onRefresh,
  exportData,
  exportColumns,
  exportFilename,
  exportElementId,
  exportTitle,
  empty,
  children,
  className,
}) => {
  if (loading) {
    return (
      <PageContainer className={className}>
        <PageHeader title={title} subtitle={subtitle} breadcrumbs={breadcrumbs} />
        <LoadingState count={6} />
      </PageContainer>
    );
  }

  if (error) {
    return (
      <PageContainer className={className}>
        <PageHeader title={title} subtitle={subtitle} breadcrumbs={breadcrumbs} />
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <AlertCircle className="h-12 w-12 text-rose-500 mb-4" />
          <p className="text-rose-600 font-semibold mb-4">{error}</p>
          {onRetry && (
            <Button onClick={onRetry} variant="outline">
              <RefreshCw className="h-4 w-4 mr-2" /> Retry
            </Button>
          )}
        </div>
      </PageContainer>
    );
  }

  const toolbar = (onRefresh || exportData) && (
    <div className="flex items-center gap-2 flex-wrap">
      {onRefresh && (
        <Button variant="outline" size="sm" onClick={onRefresh} className="h-9 rounded-lg">
          <RefreshCw className="h-4 w-4 mr-1" /> Refresh
        </Button>
      )}
      {exportData?.length > 0 && exportColumns && (
        <Button
          variant="outline"
          size="sm"
          className="h-9 rounded-lg"
          onClick={() => exportToCsv(exportData, exportColumns, exportFilename || title)}
        >
          <Download className="h-4 w-4 mr-1" /> Excel
        </Button>
      )}
      {exportElementId && (
        <Button
          variant="outline"
          size="sm"
          className="h-9 rounded-lg"
          onClick={() => exportToPdf(exportTitle || title, exportElementId)}
        >
          <Printer className="h-4 w-4 mr-1" /> PDF
        </Button>
      )}
    </div>
  );

  return (
    <PageContainer className={cn('space-y-6', className)}>
      <PageHeader title={title} subtitle={subtitle} breadcrumbs={breadcrumbs}>
        {toolbar}
        {actions}
      </PageHeader>

      {stats.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 lg:gap-5">
          {stats.map((card, i) => (
            <StatsCard key={i} {...card} />
          ))}
        </div>
      )}

      {empty ? (
        <EmptyState {...empty} />
      ) : (
        children
      )}
    </PageContainer>
  );
};

export default EnterprisePage;
