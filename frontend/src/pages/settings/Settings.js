import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import EnterprisePage from '../../components/layout/EnterprisePage';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/ui/tabs';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Switch } from '../../components/ui/switch';
import { useLanguage } from '../../context/LanguageContext';
import { toast } from 'react-toastify';
import { User, Lock, Bell, Shield, Monitor, Moon, Sun, Database, Activity } from 'lucide-react';
import { Badge } from '../../components/ui/badge';
import MasterDataTab from './MasterDataTab';
import AuditLogViewer from './AuditLogViewer';
import { PERMISSIONS } from '../../lib/permissions';
import { authApi } from '../../api';

const Settings = () => {
  const { user, hasPermission } = useAuth();
  const canViewAudit = hasPermission(PERMISSIONS.VIEW_AUDIT_LOGS);
  const canManageMasterData = hasPermission(PERMISSIONS.MANAGE_MASTER_DATA);
  const { t } = useLanguage();
  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [notifications, setNotifications] = useState({
    email: true,
    push: true,
    sms: false,
    weekly: true
  });
  const [preferences, setPreferences] = useState({
    darkMode: false,
    compactMode: false,
    autoSave: true
  });

  const handlePasswordChange = async (e) => {
    e.preventDefault();
    if (passwords.newPassword !== passwords.confirmPassword) {
      setError(t('settings.newPasswordsDoNotMatch'));
      return;
    }
    setLoading(true);
    setError('');
    try {
      await authApi.changePassword({
        currentPassword: passwords.currentPassword,
        newPassword: passwords.newPassword,
      });
      toast.success(t('settings.passwordUpdated'));
      setPasswords({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      setError(err.response?.data?.message || t('settings.failedToUpdatePassword'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <EnterprisePage
      title="Settings"
      subtitle="Manage your account settings, preferences, and notifications"
      breadcrumbs={['Settings']}
    >
      <Tabs defaultValue="profile" className="space-y-4">
        <TabsList>
          <TabsTrigger value="profile">
            <User className="mr-2 h-4 w-4" />
            Profile
          </TabsTrigger>
          <TabsTrigger value="security">
            <Lock className="mr-2 h-4 w-4" />
            Security
          </TabsTrigger>
          <TabsTrigger value="notifications">
            <Bell className="mr-2 h-4 w-4" />
            Notifications
          </TabsTrigger>
          <TabsTrigger value="preferences">
            <Monitor className="mr-2 h-4 w-4" />
            Preferences
          </TabsTrigger>
          {canManageMasterData && (
            <TabsTrigger value="master-data">
              <Database className="mr-2 h-4 w-4" />
              Master Data
            </TabsTrigger>
          )}
          {canViewAudit && (
            <TabsTrigger value="audit-trail">
              <Activity className="mr-2 h-4 w-4" />
              Audit Trail
            </TabsTrigger>
          )}
        </TabsList>

        <TabsContent value="profile">
          <Card>
            <CardHeader>
              <CardTitle>Profile Information</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-2">
                  <Label className="text-sm text-gray-500">Full Name</Label>
                  <div className="font-semibold text-lg">{user?.fullName || '-'}</div>
                </div>
                <div className="space-y-2">
                  <Label className="text-sm text-gray-500">Email Address</Label>
                  <div className="font-semibold text-lg">{user?.email || '-'}</div>
                </div>
                <div className="space-y-2">
                  <Label className="text-sm text-gray-500">Phone Number</Label>
                  <div className="font-semibold text-lg">{user?.phone || '-'}</div>
                </div>
                <div className="space-y-2">
                  <Label className="text-sm text-gray-500">Role</Label>
                  <Badge variant="success" className="text-sm px-3 py-1">
                    {user?.role ? t(`roles.${user.role}`) : '-'}
                  </Badge>
                </div>
                <div className="space-y-2">
                  <Label className="text-sm text-gray-500">Status</Label>
                  <Badge variant={user?.status === 'ACTIVE' ? 'success' : 'destructive'} className="text-sm px-3 py-1">
                    {user?.status || '-'}
                  </Badge>
                </div>
                <div className="space-y-2">
                  <Label className="text-sm text-gray-500">Member Since</Label>
                  <div className="font-semibold text-lg">
                    {user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : '-'}
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="security">
          <Card>
            <CardHeader>
              <CardTitle>Change Password</CardTitle>
            </CardHeader>
            <CardContent>
              {error && (
                <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-700">
                  {error}
                </div>
              )}
              <form onSubmit={handlePasswordChange} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="currentPassword">Current Password</Label>
                  <Input
                    id="currentPassword"
                    type="password"
                    value={passwords.currentPassword}
                    onChange={(e) => setPasswords({ ...passwords, currentPassword: e.target.value })}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="newPassword">New Password</Label>
                  <Input
                    id="newPassword"
                    type="password"
                    value={passwords.newPassword}
                    onChange={(e) => setPasswords({ ...passwords, newPassword: e.target.value })}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="confirmPassword">Confirm New Password</Label>
                  <Input
                    id="confirmPassword"
                    type="password"
                    value={passwords.confirmPassword}
                    onChange={(e) => setPasswords({ ...passwords, confirmPassword: e.target.value })}
                    required
                  />
                </div>
                <Button type="submit" disabled={loading}>
                  {loading ? 'Updating...' : 'Update Password'}
                </Button>
              </form>
            </CardContent>
          </Card>

          <Card className="mt-6">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Shield className="h-5 w-5" />
                Two-Factor Authentication
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium">Enable 2FA</p>
                  <p className="text-sm text-gray-500">Add an extra layer of security to your account</p>
                </div>
                <Switch />
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="notifications">
          <Card>
            <CardHeader>
              <CardTitle>Notification Preferences</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium">Email Notifications</p>
                  <p className="text-sm text-gray-500">Receive updates via email</p>
                </div>
                <Switch
                  checked={notifications.email}
                  onCheckedChange={(checked) => setNotifications({ ...notifications, email: checked })}
                />
              </div>
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium">Push Notifications</p>
                  <p className="text-sm text-gray-500">Receive push notifications on your device</p>
                </div>
                <Switch
                  checked={notifications.push}
                  onCheckedChange={(checked) => setNotifications({ ...notifications, push: checked })}
                />
              </div>
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium">SMS Notifications</p>
                  <p className="text-sm text-gray-500">Receive important alerts via SMS</p>
                </div>
                <Switch
                  checked={notifications.sms}
                  onCheckedChange={(checked) => setNotifications({ ...notifications, sms: checked })}
                />
              </div>
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium">Weekly Summary</p>
                  <p className="text-sm text-gray-500">Receive weekly activity summary</p>
                </div>
                <Switch
                  checked={notifications.weekly}
                  onCheckedChange={(checked) => setNotifications({ ...notifications, weekly: checked })}
                />
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="preferences">
          <Card>
            <CardHeader>
              <CardTitle>App Preferences</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  {preferences.darkMode ? <Moon className="h-5 w-5" /> : <Sun className="h-5 w-5" />}
                  <div>
                    <p className="font-medium">Dark Mode</p>
                    <p className="text-sm text-gray-500">Switch between light and dark theme</p>
                  </div>
                </div>
                <Switch
                  checked={preferences.darkMode}
                  onCheckedChange={(checked) => setPreferences({ ...preferences, darkMode: checked })}
                />
              </div>
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium">Compact Mode</p>
                  <p className="text-sm text-gray-500">Use compact layout for better screen space</p>
                </div>
                <Switch
                  checked={preferences.compactMode}
                  onCheckedChange={(checked) => setPreferences({ ...preferences, compactMode: checked })}
                />
              </div>
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium">Auto-Save</p>
                  <p className="text-sm text-gray-500">Automatically save changes without confirmation</p>
                </div>
                <Switch
                  checked={preferences.autoSave}
                  onCheckedChange={(checked) => setPreferences({ ...preferences, autoSave: checked })}
                />
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {canManageMasterData && (
          <TabsContent value="master-data">
            <MasterDataTab />
          </TabsContent>
        )}

        {canViewAudit && (
          <TabsContent value="audit-trail">
            <AuditLogViewer />
          </TabsContent>
        )}
      </Tabs>
    </EnterprisePage>
  );
};

export default Settings;
