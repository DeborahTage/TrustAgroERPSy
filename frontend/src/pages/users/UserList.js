import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '../../api';
import EnterprisePage from '../../components/layout/EnterprisePage';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import { formatDate } from '../../utils';
import { PERMISSIONS, ROLE_LABELS } from '../../lib/permissions';
import { cn } from '../../lib/utils';
import { Button } from '../../components/ui/button';
import { MoreHorizontal, Edit, UserCheck, UserX, UserPlus, Users, UserMinus } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../../components/ui/dropdown-menu';
import { toast } from 'react-toastify';

const UserList = () => {
  const { t } = useLanguage();
  const { hasPermission } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const r = await userApi.getAll();
      setUsers(r.data.data || []);
    } catch (e) {
      setError('Failed to load users.');
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchUsers(); }, [fetchUsers]);

  const handleToggleStatus = useCallback(async (user) => {
    const activating = user.status !== 'ACTIVE';
    if (activating && !hasPermission(PERMISSIONS.ACTIVATE_USERS)) return;
    if (!activating && !hasPermission(PERMISSIONS.DEACTIVATE_USERS)) return;
    try {
      const newStatus = activating ? 'ACTIVE' : 'INACTIVE';
      await userApi.updateStatus(user.id, newStatus);
      setUsers((prev) => prev.map((u) => (u.id === user.id ? { ...u, status: newStatus } : u)));
      toast.success(`User ${activating ? 'activated' : 'deactivated'}`);
    } catch (err) {
      toast.error('Failed to update user status');
    }
  }, [hasPermission]);

  const stats = useMemo(() => [
    { title: 'Total Users', value: users.length, icon: Users, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Active', value: users.filter((u) => u.status === 'ACTIVE').length, icon: UserCheck, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: 'Inactive', value: users.filter((u) => u.status !== 'ACTIVE').length, icon: UserMinus, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
  ], [users]);

  const columns = useMemo(() => [
    {
      accessorKey: 'fullName',
      header: 'Identity',
      cell: ({ row }) => (
        <div className="flex items-center gap-3">
          <div className="h-9 w-9 rounded-xl bg-primary/10 flex items-center justify-center font-black text-xs text-primary shrink-0">
            {row.original.fullName?.charAt(0).toUpperCase()}
          </div>
          <div className="flex flex-col">
            <span className="font-black text-gray-900 leading-tight">{row.original.fullName}</span>
            <span className="text-[10px] font-bold text-gray-400 truncate max-w-[150px]">{row.original.email}</span>
          </div>
        </div>
      ),
    },
    {
      accessorKey: 'role',
      header: 'Role',
      cell: ({ row }) => (
        <span className="inline-flex items-center px-3 py-1 rounded-lg text-[10px] font-black uppercase tracking-widest bg-primary/10 text-primary">
          {ROLE_LABELS[row.original.role] || row.original.role?.replace(/_/g, ' ')}
        </span>
      ),
    },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ row }) => <StatusBadge status={row.original.status?.toLowerCase()} />,
    },
    {
      accessorKey: 'phone',
      header: 'Contact',
      cell: ({ row }) => <span className="text-sm font-medium text-gray-500">{row.original.phone || '—'}</span>,
    },
    {
      accessorKey: 'createdAt',
      header: 'Joined',
      cell: ({ row }) => <span className="text-xs font-bold text-gray-400">{formatDate(row.original.createdAt)}</span>,
    },
    {
      id: 'actions',
      cell: ({ row }) => (
        <div className="flex justify-end">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="sm" className="h-9 w-9 p-0 rounded-xl">
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56 p-2 rounded-2xl">
              {hasPermission(PERMISSIONS.EDIT_USERS) && (
                <DropdownMenuItem onClick={() => navigate(`/users/${row.original.id}/edit`)} className="rounded-xl p-3 cursor-pointer">
                  <Edit className="mr-3 h-4 w-4" />
                  <span className="font-bold text-sm">{t('common.edit')}</span>
                </DropdownMenuItem>
              )}
              {(hasPermission(PERMISSIONS.ACTIVATE_USERS) || hasPermission(PERMISSIONS.DEACTIVATE_USERS)) && (
                <DropdownMenuItem
                  onClick={() => handleToggleStatus(row.original)}
                  className={cn('rounded-xl p-3 cursor-pointer', row.original.status === 'ACTIVE' ? 'hover:bg-rose-50' : 'hover:bg-emerald-50')}
                >
                  {row.original.status === 'ACTIVE' ? (
                    <><UserX className="mr-3 h-4 w-4" /><span className="font-bold text-sm">Deactivate</span></>
                  ) : (
                    <><UserCheck className="mr-3 h-4 w-4" /><span className="font-bold text-sm">Activate</span></>
                  )}
                </DropdownMenuItem>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      ),
    },
  ], [navigate, t, hasPermission, handleToggleStatus]);

  const exportColumns = [
    { header: 'Name', accessorKey: 'fullName' },
    { header: 'Email', accessorKey: 'email' },
    { header: 'Role', accessorKey: 'role' },
    { header: 'Status', accessorKey: 'status' },
    { header: 'Phone', accessorKey: 'phone' },
  ];

  return (
    <EnterprisePage
      title="User Management"
      subtitle="Provision credentials, assign roles, and control system access"
      breadcrumbs={['Administration', 'Users']}
      loading={loading}
      error={error}
      onRetry={fetchUsers}
      stats={stats}
      onRefresh={fetchUsers}
      exportData={users}
      exportColumns={exportColumns}
      exportFilename="users"
      exportElementId="users-table"
      exportTitle="User Directory"
      empty={!users.length ? { title: 'No users found', description: 'Provision the first system user to get started.' } : null}
      actions={hasPermission(PERMISSIONS.CREATE_USERS) && (
        <Button onClick={() => navigate('/users/new')} className="h-11 bg-primary hover:bg-primary/90 rounded-xl">
          <UserPlus className="mr-2 h-4 w-4" /> Add User
        </Button>
      )}
    >
      <div id="users-table">
        <DataTable columns={columns} data={users} searchable pagination pageSize={15} emptyMessage="No users found" />
      </div>
    </EnterprisePage>
  );
};

export default UserList;
