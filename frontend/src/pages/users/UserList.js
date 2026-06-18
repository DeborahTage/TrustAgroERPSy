import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '../../api';
import DataTable from '../../components/shared/DataTable';
import StatusBadge from '../../components/shared/StatusBadge';
import PageHeader from '../../components/shared/PageHeader';
import PageContainer from '../../components/layout/PageContainer';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import { formatDate } from '../../utils';
import { PERMISSIONS } from '../../lib/permissions';
import { cn } from '../../lib/utils';
import { Button } from '../../components/ui/button';
import { MoreHorizontal, Edit, UserCheck, UserX, Plus, UserPlus } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../../components/ui/dropdown-menu';

const UserList = () => {
  const { t } = useLanguage();
  const { hasPermission } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    userApi.getAll().then(r => setUsers(r.data.data || [])).finally(() => setLoading(false));
  }, []);

  const handleToggleStatus = async (user) => {
    const activating = user.status !== 'ACTIVE';
    if (activating && !hasPermission(PERMISSIONS.ACTIVATE_USERS)) return;
    if (!activating && !hasPermission(PERMISSIONS.DEACTIVATE_USERS)) return;
    try {
      const newStatus = activating ? 'ACTIVE' : 'INACTIVE';
      await userApi.updateStatus(user.id, newStatus);
      setUsers(users.map(u => u.id === user.id ? { ...u, status: newStatus } : u));
    } catch (error) {
      console.error('Failed to update user status:', error);
    }
  };

  const columns = useMemo(() => [
    {
      accessorKey: 'fullName',
      header: 'Identity',
      cell: ({ row }) => (
        <div className="flex items-center gap-3">
          <div className="h-9 w-9 rounded-xl bg-indigo-50 flex items-center justify-center font-black text-xs text-indigo-600 shrink-0">
            {row.original.fullName?.charAt(0).toUpperCase()}
          </div>
          <div className="flex flex-col">
            <span className="font-black text-gray-900 leading-tight">{row.original.fullName}</span>
            <span className="text-[10px] font-bold text-gray-400 truncate max-w-[150px]">{row.original.email}</span>
          </div>
        </div>
      )
    },
    {
      accessorKey: 'role',
      header: 'Department / Role',
      cell: ({ row }) => (
        <span className="inline-flex items-center px-3 py-1 rounded-lg text-[10px] font-black uppercase tracking-widest bg-indigo-50 text-indigo-700">
          {row.original.role?.replace('_', ' ')}
        </span>
      )
    },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ row }) => <StatusBadge status={row.original.status?.toLowerCase()} />
    },
    {
      accessorKey: 'phone',
      header: 'Contact',
      cell: ({ row }) => <span className="text-sm font-medium text-gray-500">{row.original.phone || '-'}</span>
    },
    {
      accessorKey: 'createdAt',
      header: 'Joined',
      cell: ({ row }) => <span className="text-xs font-bold text-gray-400">{formatDate(row.original.createdAt)}</span>
    },
    {
      id: 'actions',
      cell: ({ row }) => (
        <div className="flex justify-end">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="sm" className="h-9 w-9 p-0 rounded-xl hover:bg-gray-100 transition-all">
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56 p-2 rounded-2xl border-none shadow-2xl bg-white/95 backdrop-blur-md">
              {hasPermission(PERMISSIONS.EDIT_USERS) && (
                <DropdownMenuItem onClick={() => navigate(`/users/${row.original.id}/edit`)} className="rounded-xl p-3 cursor-pointer hover:bg-indigo-50 hover:text-indigo-600 transition-all">
                  <Edit className="mr-3 h-4 w-4" />
                  <span className="font-bold text-sm">{t('common.edit')}</span>
                </DropdownMenuItem>
              )}
              {(hasPermission(PERMISSIONS.ACTIVATE_USERS) || hasPermission(PERMISSIONS.DEACTIVATE_USERS)) && (
                <DropdownMenuItem onClick={() => handleToggleStatus(row.original)} className={cn(
                  "rounded-xl p-3 cursor-pointer transition-all",
                  row.original.status === 'ACTIVE' ? "hover:bg-rose-50 hover:text-rose-600" : "hover:bg-emerald-50 hover:text-emerald-600"
                )}>
                  {row.original.status === 'ACTIVE' ? (
                    <>
                      <UserX className="mr-3 h-4 w-4" />
                      <span className="font-bold text-sm">Deactivate User</span>
                    </>
                  ) : (
                    <>
                      <UserCheck className="mr-3 h-4 w-4" />
                      <span className="font-bold text-sm">Activate User</span>
                    </>
                  )}
                </DropdownMenuItem>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      )
    },
  ], [navigate, t, hasPermission]);

  return (
    <PageContainer>
      <PageHeader
        title="Access Control"
        subtitle="Manage professional credentials and system permissions."
        breadcrumbs={['Security', 'User Directory']}
      >
        {hasPermission(PERMISSIONS.CREATE_USERS) && (
          <Button onClick={() => navigate('/users/new')} className="h-11 px-5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl shadow-lg shadow-indigo-200 transition-all active:scale-95">
            <UserPlus className="mr-2 h-4 w-4 stroke-[3px]" />
            Provision User
          </Button>
        )}
      </PageHeader>

      <DataTable
        columns={columns}
        data={users}
        loading={loading}
        emptyMessage="No credentials found"
      />
    </PageContainer>
  );
};

export default UserList;
