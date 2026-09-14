import React, { useEffect, useState } from 'react';
import { financeApi, flockApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../../components/ui/table';
import { formatCurrency } from '../../utils';
import { Bird, Calculator, TrendingDown, TrendingUp, DollarSign, ArrowRight } from 'lucide-react';

const FlockProfitability = () => {
  const [flocks, setFlocks] = useState([]);
  const [selectedFlock, setSelectedFlock] = useState(null);
  const [cogsData, setCogsData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchFlocks = async () => {
      try {
        const res = await flockApi.getAll();
        setFlocks(res.data.data || []);
      } catch {
        setError('Failed to load flocks.');
      } finally {
        setLoading(false);
      }
    };
    fetchFlocks();
  }, []);

  const fetchCOGS = async (id) => {
    setSelectedFlock(flocks.find((f) => f.id === id));
    setError(null);
    try {
      const res = await financeApi.getFlockProfitLoss(id);
      setCogsData(res.data.data || res.data);
    } catch {
      setError('Failed to load profitability data.');
      setCogsData(null);
    }
  };

  const totalCost = Number(cogsData?.totalCost ?? cogsData?.cogs ?? 0);
  const costPerBird = cogsData?.costPerBird != null ? Number(cogsData.costPerBird) : null;
  const revenue = cogsData?.revenue != null ? Number(cogsData.revenue) : null;
  const profit = cogsData?.profit ?? cogsData?.netProfit;
  const breakdown = cogsData?.breakdown && typeof cogsData.breakdown === 'object'
    ? Object.entries(cogsData.breakdown)
    : [];
  const hasCost = totalCost > 0;
  const profitabilityAvailable = revenue != null && hasCost && profit != null;

  return (
    <EnterprisePage
      title="Flock Cost Accounting"
      subtitle="Costs come only from FarmCostAllocation records — no estimated profit is invented"
      breadcrumbs={['Finance', 'Flock Profitability']}
      loading={loading}
      error={error}
      stats={cogsData ? [
        { title: 'Allocated Cost', value: formatCurrency(totalCost), icon: TrendingDown, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
        { title: 'Cost / Bird', value: costPerBird != null ? formatCurrency(costPerBird) : 'Unavailable', icon: Calculator, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
        { title: 'Revenue', value: revenue != null ? formatCurrency(revenue) : 'Unavailable', icon: DollarSign, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
        { title: 'Profit', value: profitabilityAvailable ? formatCurrency(profit) : 'Unavailable', icon: TrendingUp, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
      ] : []}
    >
      {!selectedFlock ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {flocks.map((flock) => (
            <Card key={flock.id} className="cursor-pointer hover:border-primary/50 transition-all border-none shadow-sm" onClick={() => fetchCOGS(flock.id)}>
              <CardContent className="p-4 flex items-center gap-4">
                <div className="bg-amber-100 p-3 rounded-full text-amber-600">
                  <Bird className="h-6 w-6" />
                </div>
                <div>
                  <p className="font-bold text-slate-800">{flock.batchCode || flock.flockCode}</p>
                  <p className="text-[10px] text-slate-500 uppercase">{flock.breed || flock.birdType}</p>
                </div>
                <ArrowRight className="h-4 w-4 text-slate-300 ml-auto" />
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
        <div className="space-y-6">
          <div className="flex items-center gap-4">
            <Button variant="outline" size="sm" onClick={() => { setSelectedFlock(null); setCogsData(null); }}>← All Flocks</Button>
            <h2 className="text-2xl font-black text-slate-900">{selectedFlock.batchCode || selectedFlock.flockCode}</h2>
          </div>

          {!hasCost && (
            <Card className="border-amber-200 bg-amber-50">
              <CardContent className="p-4 text-sm text-amber-900">
                No farm cost allocations are recorded for this flock yet. Revenue may exist from chicken sales,
                but gross profit is unavailable until real cost data is allocated. Missing cost is not treated as zero.
              </CardContent>
            </Card>
          )}

          <Card className="border-none shadow-sm">
            <CardHeader>
              <CardTitle className="text-lg font-bold">Cost allocation breakdown</CardTitle>
              <CardDescription>From persisted FarmCostAllocation rows only</CardDescription>
            </CardHeader>
            <CardContent>
              {breakdown.length === 0 ? (
                <p className="text-sm text-slate-500">No allocated cost categories.</p>
              ) : (
                <Table>
                  <TableHeader className="bg-slate-50">
                    <TableRow>
                      <TableHead>Category</TableHead>
                      <TableHead className="text-right">Amount</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {breakdown.map(([type, amt]) => (
                      <TableRow key={type}>
                        <TableCell className="font-bold">{type}</TableCell>
                        <TableCell className="text-right">{formatCurrency(amt)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </EnterprisePage>
  );
};

export default FlockProfitability;
