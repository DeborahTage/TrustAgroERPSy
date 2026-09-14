import {
  LayoutDashboard,
  Users,
  Building2,
  Layers,
  FileText,
  Package,
  HeartPulse,
  Pill,
  DollarSign,
  Users2,
  BarChart3,
  Settings,
  GraduationCap,
  Megaphone,
  Briefcase,
  Shield,
  Warehouse,
  Truck,
  ScanLine,
  ShoppingCart,
  ClipboardList,
  Stethoscope,
  Syringe,
  Activity,
  Receipt,
  Wallet,
  BookOpen,
  Target,
  Mail,
  Calendar,
  TrendingUp,
} from 'lucide-react';
import { PERMISSIONS, normalizeRole } from './permissions';
/** @typedef {{ label: string, items: MenuItem[] }} MenuSection */

const adminMenus = [
  {
    label: 'MAIN',
    items: [
      { path: '/dashboard', labelKey: 'dashboard', icon: LayoutDashboard },
    ],
  },
  {
    label: 'ADMIN',
    items: [
      { path: '/users', labelKey: 'users', icon: Users, permission: PERMISSIONS.VIEW_USERS },
      { path: '/farm/chicken-sales', label: 'Chicken Sales', icon: ShoppingCart, permission: PERMISSIONS.VIEW_CHICKEN_SALES },
      { path: '/reports', labelKey: 'reports', icon: BarChart3, permission: PERMISSIONS.VIEW_REPORTS },
      { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
    ],
  },
];

const farmManagerMenus = [
  {
    label: 'MAIN',
    items: [{ path: '/dashboard', labelKey: 'dashboard', icon: LayoutDashboard }],
  },
  {
    label: 'FARM OPERATIONS',
    items: [
      { path: '/farms', labelKey: 'farms', icon: Building2 },
      { path: '/farms/barn-dashboard', label: 'Barns', icon: Warehouse },
      { path: '/flocks', labelKey: 'flocks', icon: Layers },
      { path: '/daily-records', labelKey: 'dailyRecords', icon: FileText },
      { path: '/farm/chicken-sales', label: 'Chicken Sales', icon: ShoppingCart, permission: PERMISSIONS.VIEW_CHICKEN_SALES },
      { path: '/feed/silos', label: 'Feed Usage', icon: Package },
      { path: '/reports', label: 'Reports', icon: BarChart3, permission: PERMISSIONS.VIEW_REPORTS },
      { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
    ],
  },
];

const inventoryManagerMenus = [
  {
    label: 'MAIN',
    items: [{ path: '/dashboard', labelKey: 'dashboard', icon: LayoutDashboard }],
  },
  {
    label: 'INVENTORY',
    items: [
      { path: '/inventory/dashboard', label: 'Overview', icon: Package },
      { path: '/inventory/items', label: 'Categories', icon: ClipboardList },
      { path: '/inventory/stock-in', label: 'Stock In', icon: Truck, permission: PERMISSIONS.STOCK_IN },
      { path: '/inventory/stock-out', label: 'Stock Out', icon: Package, permission: PERMISSIONS.STOCK_OUT },
      { path: '/inventory/requisitions', label: 'Transfers', icon: ClipboardList },
      { path: '/inventory/po', label: 'Purchase Orders', icon: Receipt },
      { path: '/inventory/suppliers', label: 'Suppliers', icon: Users },
      { path: '/inventory/scan', label: 'Barcode/QR', icon: ScanLine, permission: PERMISSIONS.STOCK_IN },
      { path: '/inventory/forecast', label: 'Forecasting', icon: TrendingUp },
      { path: '/inventory/count', label: 'Cycle Count', icon: ClipboardList, permission: PERMISSIONS.STOCK_IN },
      { path: '/inventory/reports', label: 'Reports', icon: BarChart3, permission: PERMISSIONS.VIEW_INVENTORY_REPORTS },
      { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
    ],
  },
];

const pharmacistMenus = [
  {
    label: 'MAIN',
    items: [{ path: '/dashboard', labelKey: 'dashboard', icon: LayoutDashboard }],
  },
  {
    label: 'PHARMACY',
    items: [
      { path: '/pharmacy/overview', label: 'Dashboard', icon: Pill },
      { path: '/pharmacy/process-rx', label: 'Prescription Queue', icon: ClipboardList, permission: PERMISSIONS.DISPENSE_PRESCRIPTIONS },
      { path: '/pharmacy/pos', label: 'POS Sale', icon: ShoppingCart, permission: PERMISSIONS.CREATE_PHARMACY_SALES },
      { path: '/pharmacy/customers', label: 'Customers', icon: Users, permission: PERMISSIONS.MANAGE_CUSTOMERS },
      { path: '/pharmacy/reports', label: 'Reports', icon: BarChart3 },
      { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
    ],
  },
];

const veterinarianMenus = [
  {
    label: 'MAIN',
    items: [{ path: '/dashboard', labelKey: 'dashboard', icon: LayoutDashboard }],
  },
  {
    label: 'VETERINARY',
    items: [
      { path: '/vet', label: 'Dashboard', icon: HeartPulse },
      { path: '/vet/flocks', label: 'Active Flocks', icon: Layers },
      { path: '/vet/flock-emr/1', label: 'Health Records', icon: Stethoscope },
      { path: '/veterinary/disease-cases/new', label: 'Diagnosis', icon: Activity, permission: PERMISSIONS.CREATE_DISEASE_CASES },
      { path: '/vet/vaccinations', label: 'Vaccination', icon: Syringe, permission: PERMISSIONS.MANAGE_VACCINATIONS },
      { path: '/veterinary/treatments/new', label: 'Treatment Plans', icon: ClipboardList, permission: PERMISSIONS.CREATE_TREATMENTS },
      { path: '/veterinary/prescriptions/new', label: 'Prescription Requests', icon: Pill, permission: PERMISSIONS.CREATE_PRESCRIPTIONS },
      { path: '/veterinary', label: 'Disease Monitoring', icon: Activity },
      { path: '/veterinary/reports', label: 'Mortality Reports', icon: BarChart3 },
      { path: '/vet/necropsy', label: 'Necropsy', icon: Stethoscope },
      { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
    ],
  },
];

const financeMenus = [
  {
    label: 'MAIN',
    items: [{ path: '/dashboard', labelKey: 'dashboard', icon: LayoutDashboard }],
  },
  {
    label: 'FINANCE',
    items: [
      { path: '/finance/dashboard', label: 'Dashboard', icon: DollarSign },
      { path: '/finance/new', label: 'Revenue & Expenses', icon: TrendingUp, permission: PERMISSIONS.CREATE_INCOME },
      { path: '/finance/coa', label: 'Chart of Accounts', icon: BookOpen },
      { path: '/finance/invoices', label: 'Invoice Center', icon: Receipt },
      { path: '/farm/chicken-sales', label: 'Chicken Sales', icon: ShoppingCart, permission: PERMISSIONS.VIEW_CHICKEN_SALES },
      { path: '/finance/wallets', label: 'Digital Wallets', icon: Wallet },
      { path: '/finance/reconciliation', label: 'Cash Flow', icon: DollarSign },
      { path: '/finance/reports', label: 'Reports', icon: BarChart3, permission: PERMISSIONS.VIEW_FINANCIAL_REPORTS },
      { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
    ],
  },
];

const crmManagerMenus = [
  {
    label: 'MAIN',
    items: [{ path: '/dashboard', labelKey: 'dashboard', icon: LayoutDashboard }],
  },
  {
    label: 'CRM',
    items: [
      { path: '/crm', label: 'Dashboard', icon: Users2 },
      { path: '/crm/leads', label: 'Leads', icon: Target },
      { path: '/crm/clients', label: 'Customers', icon: Users },
      { path: '/crm/pipeline', label: 'Sales Pipeline', icon: TrendingUp },
      { path: '/crm/orders', label: 'Orders', icon: ShoppingCart },
      { path: '/farm/chicken-sales', label: 'Chicken Sales', icon: ShoppingCart, permission: PERMISSIONS.VIEW_CHICKEN_SALES },
      { path: '/crm/follow-ups', label: 'Follow-ups', icon: Calendar },
      { path: '/crm/tasks', label: 'Tasks', icon: ClipboardList },
      { path: '/crm/reports', label: 'Reports', icon: BarChart3 },
      { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
    ],
  },
  {
    label: 'SERVICES',
    items: [
      { path: '/consultancy', label: 'Consultancy', icon: Briefcase },
      { path: '/training', label: 'Training', icon: GraduationCap },
    ],
  },
];

const marketingMenus = [
  {
    label: 'MAIN',
    items: [{ path: '/dashboard', labelKey: 'dashboard', icon: LayoutDashboard }],
  },
  {
    label: 'MARKETING',
    items: [
      { path: '/marketing', label: 'Dashboard', icon: Megaphone },
      { path: '/marketing/campaigns', label: 'Campaigns', icon: Target },
      { path: '/marketing/leads', label: 'Lead Generation', icon: Users },
      { path: '/marketing/email', label: 'Email Campaigns', icon: Mail },
      { path: '/marketing/events', label: 'Events', icon: Calendar },
      { path: '/marketing/segments', label: 'Customer Segments', icon: Users2 },
      { path: '/marketing/reports', label: 'Reports', icon: BarChart3 },
      { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
    ],
  },
];

const trainingOfficerMenus = [
  {
    label: 'MAIN',
    items: [{ path: '/dashboard', labelKey: 'dashboard', icon: LayoutDashboard }],
  },
  {
    label: 'TRAINING',
    items: [
      { path: '/training', label: 'Dashboard', icon: GraduationCap },
      { path: '/training/programs', label: 'Programs', icon: Calendar },
      { path: '/training/sessions', label: 'Sessions', icon: Calendar },
      { path: '/training/registrations', label: 'Registrations', icon: Users },
      { path: '/training/certificates', label: 'Certificates', icon: Shield },
      { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
    ],
  },
];

const ROLE_MENU_MAP = {
  ADMIN: adminMenus,
  GENERAL_MANAGER: adminMenus,
  OPERATIONS_MANAGER: adminMenus,
  FARM_MANAGER: farmManagerMenus,
  INVENTORY_MANAGER: inventoryManagerMenus,
  PHARMACIST: pharmacistMenus,
  VETERINARIAN: veterinarianMenus,
  FINANCE: financeMenus,
  CRM_MANAGER: crmManagerMenus,
  MARKETING_MANAGER: marketingMenus,
  TRAINING_COORDINATOR: trainingOfficerMenus,
  EXTENSION_WORKER: crmManagerMenus,
};

export { normalizeRole, ROLE_ALIASES } from './permissions';

export const getRoleMenus = (role) => {
  const normalized = normalizeRole(role);
  return ROLE_MENU_MAP[normalized] || [{ label: 'MAIN', items: [{ path: '/dashboard', labelKey: 'dashboard', icon: LayoutDashboard }] }];
};
