import React, { useState } from 'react';
import {
  flexRender,
  getCoreRowModel,
  useReactTable,
  getPaginationRowModel,
  getSortedRowModel,
  getFilteredRowModel,
} from '@tanstack/react-table';
import { Card, CardContent } from '../ui/card';
import { Input } from '../ui/input';
import { Button } from '../ui/button';
import { Skeleton } from '../ui/skeleton';
import {
  Search,
  ChevronLeft,
  ChevronRight,
  Inbox,
  ArrowUpDown,
} from 'lucide-react';
import { cn } from '../../lib/utils';

const DataTable = ({
  columns,
  data,
  loading = false,
  searchable = true,
  pagination = true,
  pageSize = 10,
  emptyMessage = "No records found",
  emptySubtitle = "Adjust your search or filters to find what you're looking for.",
  className,
  headerActions
}) => {
  const [sorting, setSorting] = useState([]);
  const [globalFilter, setGlobalFilter] = useState('');

  const table = useReactTable({
    data,
    columns,
    state: {
      sorting,
      globalFilter,
    },
    onSortingChange: setSorting,
    onGlobalFilterChange: setGlobalFilter,
    getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    initialState: {
      pagination: {
        pageSize: pageSize,
      },
    },
  });

  if (loading) {
    return (
      <div className={cn("space-y-4", className)}>
        <div className="flex justify-between items-center gap-4">
          <Skeleton className="h-10 w-full max-w-xs rounded-xl" />
          <Skeleton className="h-10 w-28 rounded-xl" />
        </div>
        <Card className="overflow-hidden">
          <CardContent className="p-0 space-y-0">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-14 w-full rounded-none" />
            ))}
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className={cn("space-y-4", className)}>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        {searchable && (
          <div className="relative w-full sm:max-w-xs group">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground group-focus-within:text-primary transition-colors" />
            <Input
              placeholder="Search records..."
              value={globalFilter ?? ''}
              onChange={(e) => setGlobalFilter(e.target.value)}
              className="pl-10 h-10 erp-input"
            />
          </div>
        )}
        {headerActions && <div className="flex items-center gap-2 ml-auto flex-wrap">{headerActions}</div>}
      </div>

      <Card className="overflow-hidden">
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead className="erp-table-head">
                {table.getHeaderGroups().map((headerGroup) => (
                  <tr key={headerGroup.id}>
                    {headerGroup.headers.map((header) => (
                      <th
                        key={header.id}
                        className="py-3.5 px-4 text-[11px] font-semibold text-muted-foreground uppercase tracking-wider"
                      >
                        {header.isPlaceholder ? null : (
                          <div
                            className={cn(
                              "flex items-center gap-1.5",
                              header.column.getCanSort() && "cursor-pointer select-none hover:text-primary transition-colors"
                            )}
                            onClick={header.column.getToggleSortingHandler()}
                          >
                            {flexRender(header.column.columnDef.header, header.getContext())}
                            {header.column.getCanSort() && (
                              <ArrowUpDown className={cn(
                                "h-3 w-3",
                                header.column.getIsSorted() ? "text-primary" : "text-muted-foreground/40"
                              )} />
                            )}
                          </div>
                        )}
                      </th>
                    ))}
                  </tr>
                ))}
              </thead>
              <tbody className="divide-y divide-border/50">
                {table.getRowModel().rows.length > 0 ? (
                  table.getRowModel().rows.map((row) => (
                    <tr key={row.id} className="hover:bg-muted/30 transition-colors group">
                      {row.getVisibleCells().map((cell) => (
                        <td key={cell.id} className="py-3.5 px-4 text-sm text-foreground/80">
                          {flexRender(cell.column.columnDef.cell, cell.getContext())}
                        </td>
                      ))}
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={columns.length} className="py-20">
                      <div className="flex flex-col items-center justify-center text-center">
                        <div className="h-14 w-14 bg-muted rounded-2xl flex items-center justify-center mb-3">
                          <Inbox className="h-7 w-7 text-muted-foreground/50" />
                        </div>
                        <h3 className="text-sm font-semibold text-foreground">{emptyMessage}</h3>
                        <p className="text-xs text-muted-foreground max-w-xs mt-1">{emptySubtitle}</p>
                      </div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {pagination && table.getPageCount() > 1 && (
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 px-1">
          <p className="text-xs text-muted-foreground order-2 sm:order-1">
            Page {table.getState().pagination.pageIndex + 1} of {table.getPageCount()}
          </p>
          <div className="flex items-center gap-1 order-1 sm:order-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => table.previousPage()}
              disabled={!table.getCanPreviousPage()}
              className="h-8 w-8 p-0 rounded-lg"
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <div className="flex gap-1">
              {[...Array(table.getPageCount())].map((_, i) => {
                if (table.getPageCount() > 5 && (i > 0 && i < table.getPageCount() - 1 && Math.abs(i - table.getState().pagination.pageIndex) > 1)) {
                  if (Math.abs(i - table.getState().pagination.pageIndex) === 2) return <span key={i} className="text-muted-foreground px-1">…</span>;
                  return null;
                }
                return (
                  <Button
                    key={i}
                    variant={table.getState().pagination.pageIndex === i ? "default" : "ghost"}
                    size="sm"
                    className="h-8 w-8 p-0 rounded-lg text-xs"
                    onClick={() => table.setPageIndex(i)}
                  >
                    {i + 1}
                  </Button>
                );
              })}
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => table.nextPage()}
              disabled={!table.getCanNextPage()}
              className="h-8 w-8 p-0 rounded-lg"
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};

export default DataTable;
