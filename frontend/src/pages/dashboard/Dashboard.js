import React, { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useLanguage } from '../../context/LanguageContext';
import { dashboardApi } from '../../api';
import { ROLES, formatCurrency } from '../../utils';
import AdminDashboard from './AdminDashboard';
import StatsCard from '../../components/shared/StatsCard';
import PageContainer from '../../components/layout/PageContainer';
import PageHeader from '../../components/shared/PageHeader';
import { Card, CardContent } from '../../components/ui/card';
import {
  Building2,
  Users,
  Activity,
  Package,
  DollarSign,
  HeartPulse,
  ShoppingCart,
  TrendingUp,
  AlertTriangle
} from 'lucide-react';

const Dashboard = () => {
  const { user, hasRole } = useAuth();
  const { t } = useLanguage();
  const [data, setData] = useState({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        let res;
        if (hasRole(ROLES.ADMIN, ROLES.GENERAL_MANAGER)) res = await dashboardApi.admin();
        else if (hasRole(ROLES.FARM_MANAGER, ROLES.OPERATIONS_MANAGER)) res = await dashboardApi.farmManager();
        else if (hasRole(ROLES.STORE_KEEPER)) res = await dashboardApi.store();
        else if (hasRole(ROLES.VETERINARY_OFFICER)) res = await dashboardApi.vet();
        else if (hasRole(ROLES.PHARMACY_SALES)) res = await dashboardApi.pharmacy();
        else if (hasRole(ROLES.FINANCE_OFFICER)) res = await dashboardApi.finance();
        if (res) setData(res.data.data || {});
      } catch (e) {
        console.error('Failed to fetch dashboard data:', e);
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
  }, [hasRole]);

  if (hasRole(ROLES.ADMIN, ROLES.GENERAL_MANAGER)) {
    return <AdminDashboard />;
  }

  const farmCards = [
    { title: t('dashboard.activeFarms'), value: data.totalFarms ?? '-', icon: Building2 },
    { title: t('dashboard.activeFlocks'), value: data.activeFlocks ?? '-', icon: Users, iconColor: 'text-indigo-600', bgColor: 'bg-indigo-50' },
    { title: t('dashboard.todaysMortality'), value: data.todayMortality ?? 0, icon: Activity, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    { title: t('dashboard.upcomingVaccinations'), value: data.upcomingVaccinations ?? 0, icon: HeartPulse, iconColor: 'text-blue-600', bgColor: 'bg-blue-50' },
    { title: t('dashboard.openHealthReports'), value: data.openHealthReports ?? 0, icon: AlertTriangle, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
  ];

  const storeCards = [
    { title: t('dashboard.totalItems'), value: data.totalItems ?? '-', icon: Package },
    { title: t('dashboard.expiringItems'), value: data.expiringItems ?? 0, icon: AlertTriangle, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
  ];

  const vetCards = [
    { title: t('dashboard.upcomingVaccinations'), value: data.upcomingVaccinations ?? 0, icon: HeartPulse },
    { title: t('dashboard.missedVaccinations'), value: data.missedVaccinations ?? 0, icon: AlertTriangle, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    { title: t('dashboard.activeDiseaseCases'), value: data.activeDiseaseCases ?? 0, icon: Activity, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    { title: t('dashboard.openHealthReports'), value: data.openHealthReports ?? 0, icon: AlertTriangle, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: t('dashboard.pendingPrescriptions'), value: data.pendingPrescriptions ?? 0, icon: TrendingUp, iconColor: 'text-indigo-600', bgColor: 'bg-indigo-50' },
  ];

  const pharmacyCards = [
    { title: t('dashboard.todaysSales'), value: formatCurrency(data.todaySales), icon: DollarSign, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: t('dashboard.totalSales'), value: data.totalSalesCount ?? 0, icon: ShoppingCart },
    { title: t('dashboard.pendingPrescriptions'), value: data.pendingPrescriptions ?? 0, icon: TrendingUp, iconColor: 'text-amber-600', bgColor: 'bg-amber-50' },
    { title: t('dashboard.internalDrugUsage'), value: data.internalDrugUsage ?? 0, icon: Package, iconColor: 'text-indigo-600', bgColor: 'bg-indigo-50' },
  ];

  const financeCards = [
    { title: t('dashboard.totalIncome'), value: formatCurrency(data.totalIncome), icon: DollarSign, iconColor: 'text-emerald-600', bgColor: 'bg-emerald-50' },
    { title: t('dashboard.totalExpenses'), value: formatCurrency(data.totalExpenses), icon: AlertTriangle, iconColor: 'text-rose-600', bgColor: 'bg-rose-50' },
    {
      title: t('dashboard.netProfitLoss'),
      value: formatCurrency(data.netProfitLoss),
      icon: TrendingUp,
      iconColor: data.netProfitLoss >= 0 ? 'text-emerald-600' : 'text-rose-600',
      bgColor: data.netProfitLoss >= 0 ? 'bg-emerald-50' : 'bg-rose-50'
    },
  ];

  let cards = [];
  if (hasRole(ROLES.FARM_MANAGER, ROLES.OPERATIONS_MANAGER)) cards = farmCards;
  else if (hasRole(ROLES.STORE_KEEPER)) cards = storeCards;
  else if (hasRole(ROLES.VETERINARY_OFFICER)) cards = vetCards;
  else if (hasRole(ROLES.PHARMACY_SALES)) cards = pharmacyCards;
  else if (hasRole(ROLES.FINANCE_OFFICER)) cards = financeCards;

  return (
    <PageContainer>
      <PageHeader
        title={t('dashboard.title')}
        subtitle={t('dashboard.welcomeBack', { name: user?.fullName || '' })}
        breadcrumbs={['Home', 'Dashboard']}
      />

      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="h-32 bg-gray-100 rounded-2xl animate-pulse" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {cards.map((card, i) => (
            <StatsCard key={i} {...card} />
          ))}
        </div>
      )}
    </PageContainer>
  );
};

export default Dashboard;
