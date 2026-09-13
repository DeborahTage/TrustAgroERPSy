import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Calendar as CalendarIcon, ChevronLeft, ChevronRight, Droplet, Zap } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { vetApi } from '../../api';
import { formatDate } from '../../utils';
import { cn } from '../../lib/utils';

const VaccinationCalendar = () => {
  const [vaccinations, setVaccinations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const now = new Date();
  const [currentMonth, setCurrentMonth] = useState(now.getMonth());
  const [currentYear, setCurrentYear] = useState(now.getFullYear());

  const fetchVaccinations = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await vetApi.getVaccinations();
      setVaccinations(res.data?.data || []);
    } catch {
      setError('Failed to load vaccination schedule.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchVaccinations(); }, [fetchVaccinations]);

  const monthLabel = new Date(currentYear, currentMonth).toLocaleString('default', { month: 'long', year: 'numeric' });
  const daysInMonth = new Date(currentYear, currentMonth + 1, 0).getDate();
  const firstDay = new Date(currentYear, currentMonth, 1).getDay();

  const monthVaccinations = useMemo(() =>
    vaccinations.filter((v) => {
      if (!v.scheduledDate) return false;
      const d = new Date(v.scheduledDate);
      return d.getMonth() === currentMonth && d.getFullYear() === currentYear;
    }), [vaccinations, currentMonth, currentYear]);

  const stats = useMemo(() => [
    { title: 'This Month', value: monthVaccinations.length, icon: CalendarIcon, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Pending', value: monthVaccinations.filter((v) => v.status === 'SCHEDULED' || v.status === 'PENDING').length, icon: Zap, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: 'Completed', value: monthVaccinations.filter((v) => v.status === 'COMPLETED').length, icon: Droplet, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
  ], [monthVaccinations]);

  const getStatusStyles = (status) => {
    switch (status) {
      case 'COMPLETED': return 'bg-emerald-100 text-emerald-700';
      case 'SCHEDULED':
      case 'PENDING': return 'bg-primary/10 text-primary';
      default: return 'bg-rose-100 text-rose-700';
    }
  };

  const prevMonth = () => {
    if (currentMonth === 0) { setCurrentMonth(11); setCurrentYear((y) => y - 1); }
    else setCurrentMonth((m) => m - 1);
  };
  const nextMonth = () => {
    if (currentMonth === 11) { setCurrentMonth(0); setCurrentYear((y) => y + 1); }
    else setCurrentMonth((m) => m + 1);
  };

  const renderCalendar = () => {
    const cells = [];
    for (let i = 0; i < firstDay; i++) cells.push(<div key={`e-${i}`} className="h-24 bg-slate-50 rounded-xl" />);
    for (let day = 1; day <= daysInMonth; day++) {
      const dateStr = `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      const dayVacs = monthVaccinations.filter((v) => v.scheduledDate === dateStr);
      const isToday = dateStr === now.toISOString().split('T')[0];
      cells.push(
        <div key={day} className={cn('h-24 p-2 border rounded-xl flex flex-col overflow-hidden', isToday ? 'bg-primary text-white border-primary' : 'bg-white border-slate-100')}>
          <span className={cn('text-xs font-bold', isToday ? 'text-emerald-200' : 'text-slate-400')}>{day}</span>
          <div className="flex-1 overflow-y-auto mt-1 space-y-0.5">
            {dayVacs.map((v) => (
              <div key={v.id} className={cn('text-[9px] px-1 py-0.5 rounded truncate font-semibold', isToday ? 'bg-white/20' : 'bg-primary/10 text-primary')}>
                {v.vaccineName}
              </div>
            ))}
          </div>
        </div>
      );
    }
    return cells;
  };

  return (
    <EnterprisePage
      title="Vaccination Task Calendar"
      subtitle="Scheduled vaccinations from the veterinary system"
      breadcrumbs={['Veterinary', 'Vaccinations']}
      loading={loading}
      error={error}
      onRetry={fetchVaccinations}
      onRefresh={fetchVaccinations}
      stats={stats}
      actions={
        <div className="flex items-center gap-2 border rounded-lg px-2 h-10 bg-white">
          <Button variant="ghost" size="icon" onClick={prevMonth}><ChevronLeft className="h-4 w-4" /></Button>
          <span className="px-3 font-semibold text-sm min-w-[140px] text-center">{monthLabel}</span>
          <Button variant="ghost" size="icon" onClick={nextMonth}><ChevronRight className="h-4 w-4" /></Button>
        </div>
      }
    >
      <div className="grid grid-cols-7 gap-2 mb-6">
        {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((d) => (
          <div key={d} className="text-center text-xs font-bold text-slate-400 py-1">{d}</div>
        ))}
        {renderCalendar()}
      </div>

      <Card className="rounded-2xl border-none shadow-sm overflow-hidden">
        <CardHeader><CardTitle>Vaccination Agenda</CardTitle></CardHeader>
        <CardContent className="p-0">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b">
              <tr>
                <th className="p-4 text-left font-medium">Date</th>
                <th className="p-4 text-left font-medium">Vaccine</th>
                <th className="p-4 text-left font-medium">Flock</th>
                <th className="p-4 text-left font-medium">Route</th>
                <th className="p-4 text-right font-medium">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {monthVaccinations.length > 0 ? monthVaccinations.map((task) => (
                <tr key={task.id} className="hover:bg-slate-50">
                  <td className="p-4">{formatDate(task.scheduledDate)}</td>
                  <td className="p-4 font-semibold">{task.vaccineName}</td>
                  <td className="p-4"><Badge variant="secondary">{task.batchCode || task.farmName || '—'}</Badge></td>
                  <td className="p-4 text-slate-600">{task.administrationRoute || task.route || '—'}</td>
                  <td className="p-4 text-right"><Badge className={getStatusStyles(task.status)}>{task.status}</Badge></td>
                </tr>
              )) : (
                <tr><td colSpan={5} className="p-8 text-center text-slate-500">No vaccinations scheduled this month</td></tr>
              )}
            </tbody>
          </table>
        </CardContent>
      </Card>
    </EnterprisePage>
  );
};

export default VaccinationCalendar;
