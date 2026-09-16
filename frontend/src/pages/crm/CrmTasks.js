import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { crmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import { Button } from '../../components/ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../../components/ui/dialog';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/ui/tabs';
import { useAuth } from '../../context/AuthContext';
import { PERMISSIONS } from '../../lib/permissions';
import { formatDate } from '../../utils';
import { Plus } from 'lucide-react';
import { toast } from 'react-toastify';

const ACTIVITY_TYPES = ['CALL', 'MEETING', 'EMAIL', 'VISIT', 'NOTE', 'FOLLOW_UP'];
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

const emptyTask = { title: '', description: '', clientId: '', dueDate: '', priority: 'MEDIUM', status: 'TODO' };
const emptyActivity = { type: 'CALL', subject: '', body: '', clientId: '', activityDate: '' };

const CrmTasks = () => {
  const { hasPermission } = useAuth();
  const [tasks, setTasks] = useState([]);
  const [activities, setActivities] = useState([]);
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [taskOpen, setTaskOpen] = useState(false);
  const [activityOpen, setActivityOpen] = useState(false);
  const [taskForm, setTaskForm] = useState(emptyTask);
  const [activityForm, setActivityForm] = useState(emptyActivity);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [tRes, aRes, cRes] = await Promise.all([
        crmApi.getTasks(),
        crmApi.getActivities().catch(() => ({ data: { data: [] } })),
        crmApi.getClients().catch(() => ({ data: { data: [] } })),
      ]);
      setTasks(tRes.data.data || []);
      setActivities(aRes.data?.data || []);
      setClients(cRes.data?.data || []);
    } catch {
      setError('Failed to load tasks.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const createTask = async (e) => {
    e.preventDefault();
    try {
      await crmApi.createTask({
        title: taskForm.title,
        description: taskForm.description || undefined,
        clientId: taskForm.clientId ? Number(taskForm.clientId) : undefined,
        dueDate: taskForm.dueDate || undefined,
        priority: taskForm.priority,
        status: taskForm.status,
      });
      toast.success('Task created');
      setTaskOpen(false);
      setTaskForm(emptyTask);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Create failed');
    }
  };

  const createActivity = async (e) => {
    e.preventDefault();
    try {
      await crmApi.createActivity({
        type: activityForm.type,
        subject: activityForm.subject,
        body: activityForm.body || undefined,
        clientId: activityForm.clientId ? Number(activityForm.clientId) : undefined,
        activityDate: activityForm.activityDate || undefined,
      });
      toast.success('Activity recorded');
      setActivityOpen(false);
      setActivityForm(emptyActivity);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Create failed');
    }
  };

  const markDone = async (task) => {
    try {
      await crmApi.updateTask(task.id, {
        title: task.title,
        description: task.description,
        clientId: task.clientId,
        leadId: task.leadId,
        opportunityId: task.opportunityId,
        assignedUserId: task.assignedUserId,
        dueDate: task.dueDate,
        priority: task.priority,
        status: 'COMPLETED',
      });
      toast.success('Task completed');
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Update failed');
    }
  };

  const filteredTasks = useMemo(() => tasks.filter((r) => {
    if (!search.trim()) return true;
    const q = search.trim().toLowerCase();
    return `${r.title || ''} ${r.assignedUserName || ''} ${r.status || ''}`.toLowerCase().includes(q);
  }), [tasks, search]);

  const taskCols = [
    { accessorKey: 'title', header: 'Task' },
    { accessorKey: 'dueDate', header: 'Due', cell: ({ row }) => formatDate(row.original.dueDate) },
    { accessorKey: 'priority', header: 'Priority' },
    { accessorKey: 'assignedUserName', header: 'Assignee' },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <StatusBadge status={row.original.status} /> },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => {
        const t = row.original;
        if (!hasPermission(PERMISSIONS.CRM_TASK) || t.status === 'COMPLETED' || t.status === 'CANCELLED') return null;
        return <Button size="sm" variant="outline" onClick={() => markDone(t)}>Complete</Button>;
      },
    },
  ];

  const activityCols = [
    { accessorKey: 'type', header: 'Type' },
    { accessorKey: 'subject', header: 'Subject' },
    { accessorKey: 'activityDate', header: 'Date', cell: ({ row }) => formatDate(row.original.activityDate) },
    { accessorKey: 'performedByName', header: 'By' },
  ];

  return (
    <EnterprisePage
      title="Tasks & Activities"
      subtitle="Follow-up tasks and logged commercial activities"
      breadcrumbs={['CRM', 'Tasks']}
      loading={loading}
      error={error}
      onRetry={load}
      onRefresh={load}
      actions={
        <>
          {hasPermission(PERMISSIONS.CRM_TASK) && (
            <Button size="sm" onClick={() => setTaskOpen(true)}><Plus className="h-4 w-4 mr-1" /> New Task</Button>
          )}
          {(hasPermission(PERMISSIONS.CRM_TASK) || hasPermission(PERMISSIONS.MANAGE_CRM) || hasPermission(PERMISSIONS.EDIT_CRM)) && (
            <Button size="sm" variant="outline" onClick={() => setActivityOpen(true)}><Plus className="h-4 w-4 mr-1" /> Log Activity</Button>
          )}
        </>
      }
    >
      <div className="mb-4">
        <Input className="max-w-xs" placeholder="Search tasks…" value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>

      <Tabs defaultValue="tasks">
        <TabsList>
          <TabsTrigger value="tasks">Tasks ({tasks.length})</TabsTrigger>
          <TabsTrigger value="activities">Activities ({activities.length})</TabsTrigger>
        </TabsList>
        <TabsContent value="tasks">
          {filteredTasks.length === 0 && !loading ? (
            <p className="text-sm text-slate-500 py-10 text-center">No tasks yet.</p>
          ) : (
            <DataTable columns={taskCols} data={filteredTasks} />
          )}
        </TabsContent>
        <TabsContent value="activities">
          {activities.length === 0 && !loading ? (
            <p className="text-sm text-slate-500 py-10 text-center">No activities yet.</p>
          ) : (
            <DataTable columns={activityCols} data={activities} />
          )}
        </TabsContent>
      </Tabs>

      <Dialog open={taskOpen} onOpenChange={setTaskOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader><DialogTitle>Create Task</DialogTitle></DialogHeader>
          <form onSubmit={createTask} className="space-y-3">
            <div className="space-y-1"><Label>Title</Label><Input required value={taskForm.title} onChange={(e) => setTaskForm({ ...taskForm, title: e.target.value })} /></div>
            <div className="space-y-1"><Label>Description</Label><Textarea value={taskForm.description} onChange={(e) => setTaskForm({ ...taskForm, description: e.target.value })} /></div>
            <div className="space-y-1">
              <Label>Customer (optional)</Label>
              <Select value={taskForm.clientId || 'none'} onValueChange={(v) => setTaskForm({ ...taskForm, clientId: v === 'none' ? '' : v })}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">None</SelectItem>
                  {clients.map((c) => <SelectItem key={c.id} value={String(c.id)}>{c.clientName || `#${c.id}`}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Due Date</Label><Input type="date" value={taskForm.dueDate} onChange={(e) => setTaskForm({ ...taskForm, dueDate: e.target.value })} /></div>
              <div className="space-y-1">
                <Label>Priority</Label>
                <Select value={taskForm.priority} onValueChange={(v) => setTaskForm({ ...taskForm, priority: v })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>{PRIORITIES.map((p) => <SelectItem key={p} value={p}>{p}</SelectItem>)}</SelectContent>
                </Select>
              </div>
            </div>
            <DialogFooter><Button type="submit">Create</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={activityOpen} onOpenChange={setActivityOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader><DialogTitle>Log Activity</DialogTitle></DialogHeader>
          <form onSubmit={createActivity} className="space-y-3">
            <div className="space-y-1">
              <Label>Type</Label>
              <Select value={activityForm.type} onValueChange={(v) => setActivityForm({ ...activityForm, type: v })}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>{ACTIVITY_TYPES.map((t) => <SelectItem key={t} value={t}>{t}</SelectItem>)}</SelectContent>
              </Select>
            </div>
            <div className="space-y-1"><Label>Subject</Label><Input required value={activityForm.subject} onChange={(e) => setActivityForm({ ...activityForm, subject: e.target.value })} /></div>
            <div className="space-y-1"><Label>Notes</Label><Textarea value={activityForm.body} onChange={(e) => setActivityForm({ ...activityForm, body: e.target.value })} /></div>
            <div className="space-y-1">
              <Label>Customer (optional)</Label>
              <Select value={activityForm.clientId || 'none'} onValueChange={(v) => setActivityForm({ ...activityForm, clientId: v === 'none' ? '' : v })}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">None</SelectItem>
                  {clients.map((c) => <SelectItem key={c.id} value={String(c.id)}>{c.clientName || `#${c.id}`}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <DialogFooter><Button type="submit">Save</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default CrmTasks;
