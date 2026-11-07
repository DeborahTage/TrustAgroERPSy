import React, { useState, useMemo } from 'react';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../../components/ui/dialog';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { Printer, Download, Plus, Edit2 } from 'lucide-react';
import { exportToCsv } from '../../lib/exportTable';
import {
  PULLET_FEED_SCHEDULE_DATA,
  renderPulletFeedScheduleHtml,
  printHtml,
} from '../../lib/trustDocumentTemplates';
import { toast } from 'react-toastify';

const FEED_TYPES = ['Pullet Developer', 'Pre-Lay Feed', 'Layer Phase 1'];

const PulletFeedSchedule = () => {
  const [schedule, setSchedule] = useState(PULLET_FEED_SCHEDULE_DATA);
  const [feedTypeFilter, setFeedTypeFilter] = useState('ALL');
  const [editingItem, setEditingItem] = useState(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState({
    week: '',
    days: '',
    std_intake_g: '',
    recommended_g: '',
    body_weight_g: '',
    feed_type: 'Pullet Developer',
    light_on: '06:00',
    light_off: '18:00',
  });

  const filtered = useMemo(() => {
    if (feedTypeFilter === 'ALL') return schedule;
    return schedule.filter((s) => s.feed_type === feedTypeFilter);
  }, [schedule, feedTypeFilter]);

  const handlePrint = () => {
    printHtml(renderPulletFeedScheduleHtml(schedule), 'Pullet Feed Schedule');
  };

  const handleExport = () => {
    const cols = [
      { header: 'Week', accessorKey: 'week' },
      { header: 'Days', accessorKey: 'days' },
      { header: 'Standard Intake (g)', accessorKey: 'std_intake_g' },
      { header: 'Recommended Intake (g)', accessorKey: 'recommended_g' },
      { header: 'Target Body Weight (g)', accessorKey: 'body_weight_g' },
      { header: 'Feed Type', accessorKey: 'feed_type' },
      { header: 'Light On', accessorKey: 'light_on' },
      { header: 'Light Off', accessorKey: 'light_off' },
    ];
    exportToCsv(schedule, cols, 'pullet_feed_schedule');
    toast.success('Schedule exported to CSV');
  };

  const openEdit = (item) => {
    setEditingItem(item);
    setForm({
      week: item.week,
      days: item.days,
      std_intake_g: item.std_intake_g,
      recommended_g: item.recommended_g,
      body_weight_g: item.body_weight_g,
      feed_type: item.feed_type,
      light_on: item.light_on || '06:00',
      light_off: item.light_off || '18:00',
    });
    setDialogOpen(true);
  };

  const handleSave = (e) => {
    e.preventDefault();
    const updated = {
      ...form,
      week: Number(form.week),
      std_intake_g: Number(form.std_intake_g),
      recommended_g: Number(form.recommended_g),
    };

    if (editingItem) {
      setSchedule((prev) => prev.map((s) => (s.week === editingItem.week ? updated : s)));
      toast.success(`Week ${updated.week} schedule updated`);
    } else {
      setSchedule((prev) => [...prev, updated].sort((a, b) => a.week - b.week));
      toast.success(`Week ${updated.week} added to schedule`);
    }
    setDialogOpen(false);
  };

  const columns = [
    {
      accessorKey: 'week',
      header: 'Week',
      cell: ({ row }) => (
        <span className="font-bold text-foreground">Week {row.original.week}</span>
      ),
    },
    { accessorKey: 'days', header: 'Days' },
    {
      accessorKey: 'std_intake_g',
      header: 'Std Intake (g)',
      cell: ({ row }) => `${row.original.std_intake_g} g`,
    },
    {
      accessorKey: 'recommended_g',
      header: 'Recommended (g)',
      cell: ({ row }) => (
        <span className="font-bold text-emerald-600 dark:text-emerald-400">
          {row.original.recommended_g} g
        </span>
      ),
    },
    {
      accessorKey: 'body_weight_g',
      header: 'Target Body Wt (g)',
      cell: ({ row }) => (
        <Badge variant="outline" className="font-mono text-xs">
          {row.original.body_weight_g}
        </Badge>
      ),
    },
    {
      accessorKey: 'feed_type',
      header: 'Feed Type',
      cell: ({ row }) => (
        <Badge
          className={
            row.original.feed_type.includes('Layer')
              ? 'bg-amber-100 text-amber-800'
              : row.original.feed_type.includes('Pre-Lay')
              ? 'bg-blue-100 text-blue-800'
              : 'bg-emerald-100 text-emerald-800'
          }
        >
          {row.original.feed_type}
        </Badge>
      ),
    },
    {
      id: 'lighting',
      header: 'Lighting Program',
      cell: ({ row }) => (
        <span className="text-xs text-muted-foreground">
          {row.original.light_on} — {row.original.light_off}
        </span>
      ),
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <Button size="sm" variant="ghost" onClick={() => openEdit(row.original)}>
          <Edit2 className="h-3.5 w-3.5" />
        </Button>
      ),
    },
  ];

  return (
    <EnterprisePage
      title="Pullet Feeding Schedule (Weeks 8–24)"
      subtitle="Standard feed intake, target growth trajectories, and lighting management for Bovans Brown pullets"
      breadcrumbs={['Farm', 'Feeding Schedules']}
      actions={
        <div className="flex flex-wrap gap-2">
          <Button size="sm" variant="outline" onClick={handlePrint}>
            <Printer className="h-4 w-4 mr-1.5" /> Print Official Schedule
          </Button>
          <Button size="sm" variant="outline" onClick={handleExport}>
            <Download className="h-4 w-4 mr-1.5" /> Export CSV
          </Button>
          <Button
            size="sm"
            onClick={() => {
              setEditingItem(null);
              setForm({
                week: '25',
                days: '169-175',
                std_intake_g: '115',
                recommended_g: '118',
                body_weight_g: '1850-1900',
                feed_type: 'Layer Phase 1',
                light_on: '04:00',
                light_off: '20:00',
              });
              setDialogOpen(true);
            }}
          >
            <Plus className="h-4 w-4 mr-1.5" /> Add Week
          </Button>
        </div>
      }
    >
      <div className="flex flex-wrap items-center justify-between gap-4 mb-4">
        <div className="flex items-center gap-3">
          <Label className="text-xs text-muted-foreground">Filter by Feed Type:</Label>
          <Select value={feedTypeFilter} onValueChange={setFeedTypeFilter}>
            <SelectTrigger className="w-52 h-9">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All Feed Stages</SelectItem>
              {FEED_TYPES.map((t) => (
                <SelectItem key={t} value={t}>
                  {t}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="text-xs text-muted-foreground">
          Showing <strong>{filtered.length}</strong> of <strong>{schedule.length}</strong> growth weeks
        </div>
      </div>

      <div className="bg-card rounded-xl border border-border/70 p-1 mb-6">
        <DataTable columns={columns} data={filtered} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="p-4 rounded-xl border border-emerald-500/20 bg-emerald-500/5">
          <h4 className="text-sm font-bold text-emerald-800 dark:text-emerald-400 mb-1">
            Weeks 8–15: Developer Stage
          </h4>
          <p className="text-xs text-muted-foreground">
            Focus on skeletal and muscular development. Uniformity is critical; target body weight:
            620g to 1,280g.
          </p>
        </div>
        <div className="p-4 rounded-xl border border-blue-500/20 bg-blue-500/5">
          <h4 className="text-sm font-bold text-blue-800 dark:text-blue-400 mb-1">
            Weeks 16–18: Pre-Lay Transition
          </h4>
          <p className="text-xs text-muted-foreground">
            Stimulate reproductive tract development. Calcium enriched diet; extend lighting by 30
            minutes weekly up to 14.5 hours.
          </p>
        </div>
        <div className="p-4 rounded-xl border border-amber-500/20 bg-amber-500/5">
          <h4 className="text-sm font-bold text-amber-800 dark:text-amber-400 mb-1">
            Weeks 19–24: Onset of Lay
          </h4>
          <p className="text-xs text-muted-foreground">
            Rapid transition to 16 hours photoperiod. Transition to Layer Phase 1 feed as flock
            exceeds 5% laying rate.
          </p>
        </div>
      </div>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>{editingItem ? `Edit Week ${editingItem.week}` : 'Add Week Schedule'}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSave} className="space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label>Week Number</Label>
                <Input
                  type="number"
                  min="1"
                  max="50"
                  required
                  value={form.week}
                  onChange={(e) => setForm({ ...form, week: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label>Days Range</Label>
                <Input
                  placeholder="e.g. 50-56"
                  required
                  value={form.days}
                  onChange={(e) => setForm({ ...form, days: e.target.value })}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label>Std Intake (g)</Label>
                <Input
                  type="number"
                  step="0.1"
                  required
                  value={form.std_intake_g}
                  onChange={(e) => setForm({ ...form, std_intake_g: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label>Recommended (g)</Label>
                <Input
                  type="number"
                  step="0.1"
                  required
                  value={form.recommended_g}
                  onChange={(e) => setForm({ ...form, recommended_g: e.target.value })}
                />
              </div>
            </div>

            <div className="space-y-1">
              <Label>Target Body Weight (g)</Label>
              <Input
                placeholder="e.g. 620-650"
                value={form.body_weight_g}
                onChange={(e) => setForm({ ...form, body_weight_g: e.target.value })}
              />
            </div>

            <div className="space-y-1">
              <Label>Feed Type</Label>
              <Select
                value={form.feed_type}
                onValueChange={(v) => setForm({ ...form, feed_type: v })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {FEED_TYPES.map((t) => (
                    <SelectItem key={t} value={t}>
                      {t}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label>Light On</Label>
                <Input
                  type="time"
                  value={form.light_on}
                  onChange={(e) => setForm({ ...form, light_on: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label>Light Off</Label>
                <Input
                  type="time"
                  value={form.light_off}
                  onChange={(e) => setForm({ ...form, light_off: e.target.value })}
                />
              </div>
            </div>

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                Cancel
              </Button>
              <Button type="submit">Save</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default PulletFeedSchedule;
