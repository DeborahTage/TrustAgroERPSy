import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { farmManagerApi, farmApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import { useAuth } from '../../context/AuthContext';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../../components/ui/dialog';
import { Badge } from '../../components/ui/badge';
import { toast } from 'react-toastify';
import { Users, UserPlus, Building2, UserCheck, UserX, Edit, MapPin } from 'lucide-react';

const FarmManagerManagement = () => {
  const { user: currentUser } = useAuth();
  const [managers, setManagers] = useState([]);
  const [farms, setFarms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Dialog states
  const [createOpen, setCreateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [selectedManager, setSelectedManager] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [dialogError, setDialogError] = useState('');

  // Form states
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phone: '',
    password: '',
    assignedFarmId: '',
  });

  const fetchManagersAndFarms = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [mgrRes, farmRes] = await Promise.all([
        farmManagerApi.getAll(),
        farmApi.getAll(),
      ]);
      setManagers(mgrRes.data?.data || []);
      setFarms(farmRes.data?.data || []);
    } catch (err) {
      console.error('Failed to load farm managers:', err);
      setError('Failed to load site farm managers. Please verify your permissions.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchManagersAndFarms();
  }, [fetchManagersAndFarms]);

  const handleOpenCreate = () => {
    setFormData({
      fullName: '',
      email: '',
      phone: '',
      password: '',
      assignedFarmId: '',
    });
    setDialogError('');
    setCreateOpen(true);
  };

  const handleOpenEdit = (manager) => {
    setSelectedManager(manager);
    setFormData({
      fullName: manager.fullName || '',
      email: manager.email || '',
      phone: manager.phone || '',
      password: '',
      assignedFarmId: manager.assignedFarmId ? String(manager.assignedFarmId) : 'none',
    });
    setDialogError('');
    setEditOpen(true);
  };

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setDialogError('');
    try {
      const payload = {
        fullName: formData.fullName.trim(),
        email: formData.email.trim(),
        phone: formData.phone.trim() || null,
        password: formData.password,
        assignedFarmId: formData.assignedFarmId && formData.assignedFarmId !== 'none'
          ? Number(formData.assignedFarmId)
          : null,
      };

      await farmManagerApi.create(payload);
      toast.success('Site Farm Manager created successfully');
      setCreateOpen(false);
      fetchManagersAndFarms();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to create site farm manager';
      setDialogError(msg);
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    if (!selectedManager) return;
    setSubmitting(true);
    setDialogError('');
    try {
      const payload = {
        fullName: formData.fullName.trim(),
        email: selectedManager.email,
        phone: formData.phone.trim() || null,
        assignedFarmId: formData.assignedFarmId && formData.assignedFarmId !== 'none'
          ? Number(formData.assignedFarmId)
          : null,
      };

      if (formData.password && formData.password.trim().length > 0) {
        payload.password = formData.password.trim();
      }

      await farmManagerApi.update(selectedManager.id, payload);
      toast.success('Site Farm Manager updated successfully');
      setEditOpen(false);
      fetchManagersAndFarms();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to update site farm manager';
      setDialogError(msg);
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleStatus = async (manager) => {
    const isActivating = manager.status !== 'ACTIVE';
    const newStatus = isActivating ? 'ACTIVE' : 'INACTIVE';
    try {
      await farmManagerApi.updateStatus(manager.id, newStatus);
      toast.success(`Manager account ${isActivating ? 'activated' : 'deactivated'}`);
      setManagers((prev) =>
        prev.map((m) => (m.id === manager.id ? { ...m, status: newStatus } : m))
      );
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to update manager status';
      toast.error(msg);
    }
  };

  const stats = useMemo(() => [
    {
      title: 'Site Farm Managers',
      value: managers.length,
      icon: Users,
      iconColor: 'text-blue-600',
      bgColor: 'bg-blue-50',
    },
    {
      title: 'Active Accounts',
      value: managers.filter((m) => m.status === 'ACTIVE').length,
      icon: UserCheck,
      iconColor: 'text-emerald-600',
      bgColor: 'bg-emerald-50',
    },
    {
      title: 'Assigned to Farms',
      value: managers.filter((m) => m.assignedFarmId != null).length,
      icon: Building2,
      iconColor: 'text-purple-600',
      bgColor: 'bg-purple-50',
    },
    {
      title: 'Unassigned',
      value: managers.filter((m) => m.assignedFarmId == null).length,
      icon: MapPin,
      iconColor: 'text-amber-600',
      bgColor: 'bg-amber-50',
    },
  ], [managers]);

  const columns = useMemo(() => [
    {
      accessorKey: 'fullName',
      header: 'Manager Name',
      cell: ({ row }) => (
        <div>
          <div className="font-medium text-slate-900 dark:text-slate-100">{row.original.fullName}</div>
          <div className="text-xs text-slate-500">{row.original.email}</div>
        </div>
      ),
    },
    {
      accessorKey: 'phone',
      header: 'Phone',
      cell: ({ row }) => row.original.phone || '—',
    },
    {
      accessorKey: 'designation',
      header: 'Designation',
      cell: ({ row }) => (
        <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200 font-medium">
          Site Farm Manager
        </Badge>
      ),
    },
    {
      accessorKey: 'assignedFarmName',
      header: 'Assigned Farm',
      cell: ({ row }) => {
        if (!row.original.assignedFarmId) {
          return <span className="text-xs text-slate-400 italic">Unassigned</span>;
        }
        return (
          <div className="flex items-center gap-1.5">
            <Building2 className="h-3.5 w-3.5 text-emerald-600" />
            <span className="font-medium text-slate-800 dark:text-slate-200">
              {row.original.assignedFarmName}
            </span>
            {row.original.assignedFarmLocation && (
              <span className="text-xs text-slate-400">({row.original.assignedFarmLocation})</span>
            )}
          </div>
        );
      },
    },
    {
      accessorKey: 'reportsToName',
      header: 'Supervised By',
      cell: ({ row }) => {
        const sup = row.original.reportsToName;
        return sup ? (
          <div className="text-xs text-slate-600 dark:text-slate-300">
            <span className="font-semibold">{sup}</span>
            <div className="text-[10px] text-slate-400">Group Farm Manager</div>
          </div>
        ) : (
          <span className="text-xs text-slate-400">—</span>
        );
      },
    },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ row }) => <StatusBadge status={row.original.status} />,
    },
    {
      id: 'actions',
      header: 'Actions',
      cell: ({ row }) => {
        const m = row.original;
        const isSelf = currentUser && currentUser.id === m.id;
        return (
          <div className="flex items-center gap-2">
            <Button
              size="sm"
              variant="outline"
              disabled={isSelf}
              onClick={() => handleOpenEdit(m)}
              className="h-8 px-2 text-xs"
            >
              <Edit className="h-3.5 w-3.5 mr-1" /> Edit
            </Button>
            <Button
              size="sm"
              variant={m.status === 'ACTIVE' ? 'ghost' : 'outline'}
              disabled={isSelf}
              onClick={() => handleToggleStatus(m)}
              className={`h-8 px-2 text-xs ${
                m.status === 'ACTIVE' ? 'text-rose-600 hover:text-rose-700' : 'text-emerald-600'
              }`}
            >
              {m.status === 'ACTIVE' ? (
                <>
                  <UserX className="h-3.5 w-3.5 mr-1" /> Deactivate
                </>
              ) : (
                <>
                  <UserCheck className="h-3.5 w-3.5 mr-1" /> Activate
                </>
              )}
            </Button>
          </div>
        );
      },
    },
  ], [currentUser]);

  return (
    <EnterprisePage
      title="Farm Manager Management"
      subtitle="Supervise site farm managers, assign single-site operational scopes, and enforce organizational hierarchy"
      breadcrumbs={['Farm', 'Site Farm Managers']}
      stats={stats}
      actions={
        <Button onClick={handleOpenCreate} className="bg-primary hover:bg-primary/90 flex items-center gap-2">
          <UserPlus className="h-4 w-4" /> Create Site Farm Manager
        </Button>
      }
    >
      {error ? (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-700 rounded-lg text-sm">
          {error}
        </div>
      ) : (
        <DataTable
          columns={columns}
          data={managers}
          loading={loading}
          searchKey="fullName"
          searchPlaceholder="Search site farm managers..."
        />
      )}

      {/* CREATE SITE FARM MANAGER MODAL */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="sm:max-w-[480px]">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <UserPlus className="h-5 w-5 text-primary" /> Create Site Farm Manager
            </DialogTitle>
            <DialogDescription>
              Create an operational ERP account with the Site Farm Manager designation. Farm access will be restricted to their assigned site.
            </DialogDescription>
          </DialogHeader>

          {dialogError && (
            <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded text-xs">
              {dialogError}
            </div>
          )}

          <form onSubmit={handleCreateSubmit} className="space-y-4 py-2">
            <div className="space-y-1.5">
              <Label htmlFor="create-name">Full Name *</Label>
              <Input
                id="create-name"
                value={formData.fullName}
                onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                placeholder="e.g. Abebe Bekele"
                required
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="create-email">Email Address *</Label>
              <Input
                id="create-email"
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                placeholder="e.g. abebe@trustagro.com"
                required
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="create-phone">Phone Number</Label>
              <Input
                id="create-phone"
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                placeholder="+251911223344"
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="create-password">Initial Password * (min 8 characters)</Label>
              <Input
                id="create-password"
                type="password"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                placeholder="••••••••"
                minLength={8}
                required
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="create-farm">Assigned Farm Site</Label>
              <Select
                value={formData.assignedFarmId || 'none'}
                onValueChange={(val) => setFormData({ ...formData, assignedFarmId: val === 'none' ? '' : val })}
              >
                <SelectTrigger id="create-farm">
                  <SelectValue placeholder="Select site (e.g. Biyo, Shera, Dukem, Baugaya)" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">-- Unassigned (Assign later) --</SelectItem>
                  {farms.map((f) => {
                    const alreadyAssigned = managers.find((m) => m.assignedFarmId === f.id);
                    return (
                      <SelectItem
                        key={f.id}
                        value={String(f.id)}
                        disabled={Boolean(alreadyAssigned)}
                      >
                        {f.farmName} {f.location ? `(${f.location})` : ''}{' '}
                        {alreadyAssigned ? `[Already assigned to ${alreadyAssigned.fullName}]` : ''}
                      </SelectItem>
                    );
                  })}
                </SelectContent>
              </Select>
              <p className="text-[11px] text-slate-500">
                A site farm manager manages exactly one location. Conflicting farm assignments are automatically prevented.
              </p>
            </div>

            <DialogFooter className="pt-3 border-t">
              <Button type="button" variant="outline" onClick={() => setCreateOpen(false)} disabled={submitting}>
                Cancel
              </Button>
              <Button type="submit" disabled={submitting} className="bg-primary hover:bg-primary/90">
                {submitting ? 'Creating...' : 'Create Manager'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* EDIT SITE FARM MANAGER MODAL */}
      <Dialog open={editOpen} onOpenChange={setEditOpen}>
        <DialogContent className="sm:max-w-[480px]">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Edit className="h-5 w-5 text-primary" /> Edit Site Farm Manager
            </DialogTitle>
            <DialogDescription>
              Update manager details, phone number, credentials, or reassign farm location.
            </DialogDescription>
          </DialogHeader>

          {dialogError && (
            <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded text-xs">
              {dialogError}
            </div>
          )}

          <form onSubmit={handleEditSubmit} className="space-y-4 py-2">
            <div className="space-y-1.5">
              <Label htmlFor="edit-name">Full Name *</Label>
              <Input
                id="edit-name"
                value={formData.fullName}
                onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                required
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="edit-email">Email (Immutable)</Label>
              <Input
                id="edit-email"
                value={formData.email}
                disabled
                className="bg-slate-100 text-slate-500"
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="edit-phone">Phone Number</Label>
              <Input
                id="edit-phone"
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="edit-password">New Password (leave blank to keep current)</Label>
              <Input
                id="edit-password"
                type="password"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                placeholder="••••••••"
                minLength={8}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="edit-farm">Assigned Farm Site</Label>
              <Select
                value={formData.assignedFarmId || 'none'}
                onValueChange={(val) => setFormData({ ...formData, assignedFarmId: val === 'none' ? '' : val })}
              >
                <SelectTrigger id="edit-farm">
                  <SelectValue placeholder="Select site (e.g. Biyo, Shera, Dukem, Baugaya)" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">-- Unassigned --</SelectItem>
                  {farms.map((f) => {
                    const assignedToOther = managers.find(
                      (m) => m.assignedFarmId === f.id && m.id !== selectedManager?.id
                    );
                    return (
                      <SelectItem
                        key={f.id}
                        value={String(f.id)}
                        disabled={Boolean(assignedToOther)}
                      >
                        {f.farmName} {f.location ? `(${f.location})` : ''}{' '}
                        {assignedToOther ? `[Assigned to ${assignedToOther.fullName}]` : ''}
                      </SelectItem>
                    );
                  })}
                </SelectContent>
              </Select>
            </div>

            <DialogFooter className="pt-3 border-t">
              <Button type="button" variant="outline" onClick={() => setEditOpen(false)} disabled={submitting}>
                Cancel
              </Button>
              <Button type="submit" disabled={submitting} className="bg-primary hover:bg-primary/90">
                {submitting ? 'Saving...' : 'Save Changes'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </EnterprisePage>
  );
};

export default FarmManagerManagement;
