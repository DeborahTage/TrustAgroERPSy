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
import {
  Search,
  ChevronLeft,
  ChevronRight,
  FileX2,
  Inbox,
  ArrowUpDown,
  Filter
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
        <div className="flex justify-between items-center">
          <div className="h-10 w-64 bg-gray-100 animate-pulse rounded-lg" />
          <div className="h-10 w-32 bg-gray-100 animate-pulse rounded-lg" />
        </div>
        <Card className="border-none shadow-sm bg-white overflow-hidden">
          <CardContent className="p-0">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="h-16 w-full border-b border-gray-50 bg-gray-50/20 animate-pulse" />
            ))}
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className={cn("space-y-4", className)}>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        {searchable && (
          <div className="relative w-full sm:w-80 group">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 group-focus-within:text-indigo-500 transition-colors" />
            <Input
              placeholder="Quick search..."
              value={globalFilter ?? ''}
              onChange={(e) => setGlobalFilter(e.target.value)}
              className="pl-10 h-10 bg-white border-gray-200 rounded-xl focus:ring-2 focus:ring-indigo-500/20"
            />
          </div>
        )}
        {headerActions && <div className="flex items-center gap-2 ml-auto">{headerActions}</div>}
      </div>

      <Card className="border-none shadow-sm overflow-hidden bg-white">
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                {table.getHeaderGroups().map((headerGroup) => (
                  <tr key={headerGroup.id} className="bg-gray-50/50 border-b border-gray-100">
                    {headerGroup.headers.map((header) => (
                      <th
                        key={header.id}
                        className="py-4 px-6 text-[11px] font-bold text-gray-500 uppercase tracking-wider"
                      >
                        {header.isPlaceholder ? null : (
                          <div
                            className={cn(
                              "flex items-center gap-2",
                              header.column.getCanSort() && "cursor-pointer select-none hover:text-indigo-600 transition-colors"
                            )}
                            onClick={header.column.getToggleSortingHandler()}
                          >
                            {flexRender(header.column.columnDef.header, header.getContext())}
                            {header.column.getCanSort() && (
                              <ArrowUpDown className={cn(
                                "h-3 w-3",
                                header.column.getIsSorted() ? "text-indigo-600" : "text-gray-300"
                              )} />
                            )}
                          </div>
                        )}
                      </th>
                    ))}
                  </tr>
                ))}
              </thead>
              <tbody className="divide-y divide-gray-50">
                {table.getRowModel().rows.length > 0 ? (
                  table.getRowModel().rows.map((row) => (
                    <tr key={row.id} className="hover:bg-gray-50/40 transition-all group">
                      {row.getVisibleCells().map((cell) => (
                        <td key={cell.id} className="py-4 px-6 text-sm text-gray-600 font-medium">
                          {flexRender(cell.column.columnDef.cell, cell.getContext())}
                        </td>
                      ))}
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={columns.length} className="py-24">
                      <div className="flex flex-col items-center justify-center text-center">
                        <div className="h-16 w-16 bg-gray-50 rounded-full flex items-center justify-center mb-4">
                          <Inbox className="h-8 w-8 text-gray-300" />
                        </div>
                        <h3 className="text-base font-bold text-gray-900">{emptyMessage}</h3>
                        <p className="text-sm text-gray-500 max-w-xs mt-1">{emptySubtitle}</p>
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
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 px-2">
          <p className="text-xs font-bold text-gray-400 order-2 sm:order-1 capitalize">
            Page {table.getState().pagination.pageIndex + 1} of {table.getPageCount()}
          </p>
          <div className="flex items-center gap-1.5 order-1 sm:order-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => table.previousPage()}
              disabled={!table.getCanPreviousPage()}
              className="h-8 w-8 p-0 rounded-lg border-gray-100"
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <div className="flex gap-1">
              {[...Array(table.getPageCount())].map((_, i) => {
                // Simple page numbers
                if (table.getPageCount() > 5 && (i > 0 && i < table.getPageCount() - 1 && Math.abs(i - table.getState().pagination.pageIndex) > 1)) {
                  if (Math.abs(i - table.getState().pagination.pageIndex) === 2) return <span key={i} className="text-gray-300">...</span>;
                  return null;
                }
                return (
                  <Button
                    key={i}
                    variant={table.getState().pagination.pageIndex === i ? "default" : "ghost"}
                    size="sm"
                    className={cn("h-8 w-8 p-0 rounded-lg text-xs font-bold", table.getState().pagination.pageIndex === i && "bg-indigo-600")}
                    onClick={() => table.setPageIndex(i)}
                  >
                    {i + 1}
                  </Button>
                )
              })}
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => table.nextPage()}
              disabled={!table.getCanNextPage()}
              className="h-8 w-8 p-0 rounded-lg border-gray-100"
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
