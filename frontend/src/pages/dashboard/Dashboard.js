import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useLanguage } from '../../context/LanguageContext';
import { dashboardApi } from '../../api';
import { ROLES, formatCurrency } from '../../utils';
import { normalizeRole } from '../../lib/permissions';
import AdminDashboard from './AdminDashboard';
import EnterprisePage from '../../components/layout/EnterprisePage';
import {
  Building2, Users, Activity, Package, DollarSign, HeartPulse, ShoppingCart, TrendingUp, AlertTriangle, Target, Megaphone,
} from 'lucide-react';

const Dashboard = () => {
  const { user, hasRole } = useAuth();
  const { t } = useLanguage();
  const [data, setData] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const role = normalizeRole(user?.role);

  const fetchDashboard = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      let res;
      if (role === ROLES.ADMIN) res = await dashboardApi.admin();
      else if (role === ROLES.FARM_MANAGER) res = await dashboardApi.farmManager();
      else if (role === ROLES.INVENTORY_MANAGER || user?.role === ROLES.STORE_KEEPER) res = await dashboardApi.store();
      else if (role === ROLES.VETERINARIAN || user?.role === ROLES.VETERINARY_OFFICER) res = await dashboardApi.vet();
      else if (role === ROLES.PHARMACIST || user?.role === ROLES.PHARMACY_SALES) res = await dashboardApi.pharmacy();
      else if (role === ROLES.FINANCE || user?.role === ROLES.FINANCE_OFFICER) res = await dashboardApi.finance();
      else if (role === ROLES.CRM_MANAGER || user?.role === ROLES.EXTENSION_WORKER) res = await dashboardApi.crm();
      else if (role === ROLES.MARKETING) res = await dashboardApi.marketing();
      if (res) setData(res.data.data || {});
    } catch {
      setError('Failed to load dashboard data.');
    } finally {
      setLoading(false);
    }
  }, [role, user?.role]);

  useEffect(() => { fetchDashboard(); }, [fetchDashboard]);

  if (hasRole(ROLES.ADMIN, ROLES.GENERAL_MANAGER, ROLES.OPERATIONS_MANAGER)) {
    return <AdminDashboard />;
  }

  const farmCards = [
    { title: t('dashboard.activeFarms'), value: data.totalFarms ?? '—', icon: Building2, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: t('dashboard.activeFlocks'), value: data.activeFlocks ?? '—', icon: Users, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: t('dashboard.todaysMortality'), value: data.todayMortality ?? 0, icon: Activity, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    { title: t('dashboard.upcomingVaccinations'), value: data.upcomingVaccinations ?? 0, icon: HeartPulse, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
  ];

  const inventoryCards = [
    { title: t('dashboard.totalItems'), value: data.totalItems ?? '—', icon: Package, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: t('dashboard.expiringItems'), value: data.expiringItems ?? 0, icon: AlertTriangle, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
  ];

  const vetCards = [
    { title: t('dashboard.upcomingVaccinations'), value: data.upcomingVaccinations ?? 0, icon: HeartPulse, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: t('dashboard.missedVaccinations'), value: data.missedVaccinations ?? 0, icon: AlertTriangle, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    { title: t('dashboard.activeDiseaseCases'), value: data.activeDiseaseCases ?? 0, icon: Activity, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    { title: t('dashboard.pendingPrescriptions'), value: data.pendingPrescriptions ?? 0, icon: TrendingUp, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
  ];

  const pharmacyCards = [
    { title: t('dashboard.todaysSales'), value: formatCurrency(data.todaySales), icon: DollarSign, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: t('dashboard.totalSales'), value: data.totalSalesCount ?? 0, icon: ShoppingCart, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: t('dashboard.pendingPrescriptions'), value: data.pendingPrescriptions ?? 0, icon: TrendingUp, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
  ];

  const financeCards = [
    { title: t('dashboard.totalIncome'), value: formatCurrency(data.totalIncome), icon: DollarSign, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: t('dashboard.totalExpenses'), value: formatCurrency(data.totalExpenses), icon: AlertTriangle, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    { title: t('dashboard.netProfitLoss'), value: formatCurrency(data.netProfitLoss), icon: TrendingUp, iconColor: (data.netProfitLoss ?? 0) >= 0 ? 'text-emerald-600' : 'text-rose-600', bgColor: (data.netProfitLoss ?? 0) >= 0 ? 'bg-emerald-50' : 'bg-rose-50' },
  ];

  const crmCards = [
    { title: 'Total Clients', value: data.totalClients ?? 0, icon: Users, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Leads', value: data.leads ?? 0, icon: Target, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: 'Active Clients', value: data.activeClients ?? 0, icon: Activity, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: 'Due Follow-ups', value: data.dueFollowUps ?? 0, icon: AlertTriangle, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
  ];

  const marketingCards = [
    { title: 'Active Campaigns', value: data.activeCampaigns ?? 0, icon: Megaphone, iconColor: 'text-primary', bgColor: 'bg-primary/10' },
    { title: 'Total Leads', value: data.totalLeads ?? 0, icon: Target, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: 'Open Leads', value: data.openLeads ?? 0, icon: Users, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: 'Conversion Rate', value: `${data.conversionRate ?? 0}%`, icon: TrendingUp, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
  ];

  let cards = [];
  if (role === ROLES.FARM_MANAGER) cards = farmCards;
  else if (role === ROLES.INVENTORY_MANAGER) cards = inventoryCards;
  else if (role === ROLES.VETERINARIAN) cards = vetCards;
  else if (role === ROLES.PHARMACIST) cards = pharmacyCards;
  else if (role === ROLES.FINANCE) cards = financeCards;
  else if (role === ROLES.CRM_MANAGER) cards = crmCards;
  else if (role === ROLES.MARKETING) cards = marketingCards;

  return (
    <EnterprisePage
      title={t('dashboard.title')}
      subtitle={t('dashboard.welcomeBack', { name: user?.fullName || '' })}
      breadcrumbs={['Home', 'Dashboard']}
      loading={loading}
      error={error}
      onRetry={fetchDashboard}
      onRefresh={fetchDashboard}
      stats={cards}
      empty={!cards.length && !loading ? { title: 'No dashboard metrics', description: 'Your role does not have a configured dashboard view.' } : null}
    />
  );
};

export default Dashboard;
