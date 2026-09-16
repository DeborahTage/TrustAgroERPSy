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
  UserCog,
  Clock,
  CalendarOff,
  Banknote,
} from 'lucide-react';
import { PERMISSIONS, normalizeRole } from './permissions';
/** @typedef {{ label: string, items: MenuItem[] }} MenuSection */

const adminMenus = [
  {
    label: 'MAIN',
    items: [
      { path: '/dashboard', label: 'Executive', labelKey: 'dashboard', icon: LayoutDashboard, permission: PERMISSIONS.EXECUTIVE_DASHBOARD },
    ],
  },
  {
    label: 'ADMIN',
    items: [
      { path: '/users', labelKey: 'users', icon: Users, permission: PERMISSIONS.VIEW_USERS },
      { path: '/governance', label: 'Governance', icon: Shield, permission: PERMISSIONS.GOVERNANCE_READ },
      { path: '/farm/chicken-sales', label: 'Chicken Sales', icon: ShoppingCart, permission: PERMISSIONS.VIEW_CHICKEN_SALES },
      { path: '/reports', labelKey: 'reports', icon: BarChart3, permission: PERMISSIONS.VIEW_REPORTS },
      { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
    ],
  },
  {
    label: 'SERVICES',
    items: [
      { path: '/crm', label: 'CRM', icon: Users2, permission: PERMISSIONS.VIEW_CRM },
      { path: '/crm/quotations', label: 'Quotations', icon: FileText, permission: PERMISSIONS.VIEW_CRM },
      { path: '/crm/reports', label: 'CRM Reports', icon: BarChart3, permission: PERMISSIONS.CRM_REPORT },
      { path: '/consultancy', label: 'Consultancy', icon: Briefcase, permission: PERMISSIONS.VIEW_CONSULTANCY },
      { path: '/consultancy/requests', label: 'Requests', icon: ClipboardList, permission: PERMISSIONS.VIEW_CONSULTANCY },
      { path: '/consultancy/proposals', label: 'Proposals', icon: FileText, permission: PERMISSIONS.VIEW_CONSULTANCY },
      { path: '/consultancy/engagements', label: 'Engagements', icon: Briefcase, permission: PERMISSIONS.VIEW_CONSULTANCY },
      { path: '/consultancy/reports', label: 'Consultancy Reports', icon: BarChart3, permission: PERMISSIONS.CONSULTANCY_REPORT },
      { path: '/training', label: 'Training', icon: GraduationCap, permission: PERMISSIONS.VIEW_TRAINING },
      { path: '/hr', label: 'HR & Payroll', icon: UserCog, permission: PERMISSIONS.VIEW_HR },
      { path: '/procurement', label: 'Procurement', icon: Truck, permission: PERMISSIONS.VIEW_PROCUREMENT },
    ],
  },
];

const hrMenus = [
  {
    label: 'MAIN',
    items: [{ path: '/dashboard', labelKey: 'dashboard', icon: LayoutDashboard }],
  },
  {
    label: 'HUMAN RESOURCES',
    items: [
      { path: '/hr', label: 'Dashboard', icon: UserCog, permission: PERMISSIONS.VIEW_HR },
      { path: '/hr/employees', label: 'Employees', icon: Users, permission: PERMISSIONS.VIEW_HR },
      { path: '/hr/attendance', label: 'Attendance', icon: Clock, permission: PERMISSIONS.HR_ATTENDANCE },
      { path: '/hr/leave', label: 'Leave Requests', icon: CalendarOff, permission: PERMISSIONS.HR_LEAVE },
      { path: '/hr/leave-types', label: 'Leave Types', icon: ClipboardList, permission: PERMISSIONS.VIEW_HR },
      { path: '/hr/payroll', label: 'Payroll', icon: Banknote, permission: PERMISSIONS.HR_PAYROLL },
      { path: '/hr/payslips', label: 'Payslips', icon: Receipt, permission: PERMISSIONS.HR_PAYROLL },
      { path: '/hr/departments', label: 'Departments', icon: Building2, permission: PERMISSIONS.VIEW_HR },
      { path: '/hr/positions', label: 'Positions', icon: Briefcase, permission: PERMISSIONS.VIEW_HR },
      { path: '/hr/reports', label: 'Reports', icon: BarChart3, permission: PERMISSIONS.HR_REPORT },
      { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
    ],
  },
  {
    label: 'MY SELF-SERVICE',
    items: [
      { path: '/hr/me', label: 'My Self-Service', icon: Users2, permission: PERMISSIONS.HR_SELF },
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
      { path: '/procurement/requisitions', label: 'Requisitions', icon: ClipboardList, permission: PERMISSIONS.VIEW_PROCUREMENT },
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
  {
    label: 'PROCUREMENT',
    items: [
      { path: '/procurement', label: 'Dashboard', icon: LayoutDashboard, permission: PERMISSIONS.VIEW_PROCUREMENT },
      { path: '/procurement/requisitions', label: 'Requisitions', icon: ClipboardList, permission: PERMISSIONS.VIEW_PROCUREMENT },
      { path: '/procurement/purchase-orders', label: 'Purchase Orders', icon: Receipt, permission: PERMISSIONS.VIEW_PROCUREMENT },
      { path: '/procurement/goods-receipts', label: 'GRNs', icon: Package, permission: PERMISSIONS.VIEW_PROCUREMENT },
      { path: '/procurement/suppliers', label: 'Suppliers', icon: Truck, permission: PERMISSIONS.VIEW_PROCUREMENT },
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
      { path: '/governance', label: 'Governance', icon: Shield, permission: PERMISSIONS.GOVERNANCE_READ },
      { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
    ],
  },
  {
    label: 'CONSULTANCY',
    items: [
      { path: '/consultancy', label: 'Dashboard', icon: Briefcase, permission: PERMISSIONS.VIEW_CONSULTANCY },
      { path: '/consultancy/engagements', label: 'Engagements', icon: Briefcase, permission: PERMISSIONS.VIEW_CONSULTANCY },
      { path: '/consultancy/reports', label: 'Consultancy Reports', icon: BarChart3, permission: PERMISSIONS.CONSULTANCY_REPORT },
    ],
  },
  {
    label: 'PAYROLL',
    items: [
      { path: '/hr/payroll', label: 'Payroll', icon: Banknote, permissions: [PERMISSIONS.VIEW_HR, PERMISSIONS.HR_PAYROLL] },
      { path: '/hr/payslips', label: 'Payslips', icon: Receipt, permissions: [PERMISSIONS.VIEW_HR, PERMISSIONS.HR_PAYROLL] },
      { path: '/hr/reports', label: 'HR Reports', icon: BarChart3, permission: PERMISSIONS.HR_REPORT },
    ],
  },
  {
    label: 'PROCUREMENT / AP',
    items: [
      { path: '/procurement/ap', label: 'Accounts Payable', icon: Wallet, permission: PERMISSIONS.VIEW_PROCUREMENT },
      { path: '/procurement/supplier-invoices', label: 'Supplier Invoices', icon: FileText, permission: PERMISSIONS.VIEW_PROCUREMENT },
      { path: '/procurement/reports', label: 'Procurement Reports', icon: BarChart3, permission: PERMISSIONS.PROCUREMENT_REPORT },
    ],
  },
  {
    label: 'CRM',
    items: [
      { path: '/crm', label: 'Dashboard', icon: Users2, permission: PERMISSIONS.VIEW_CRM },
      { path: '/crm/orders', label: 'Orders', icon: ShoppingCart, permission: PERMISSIONS.VIEW_CRM },
      { path: '/crm/reports', label: 'Commercial Reports', icon: BarChart3, permission: PERMISSIONS.CRM_REPORT },
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
      { path: '/crm/opportunities', label: 'Opportunities', icon: TrendingUp },
      { path: '/crm/quotations', label: 'Quotations', icon: FileText },
      { path: '/crm/orders', label: 'Orders', icon: ShoppingCart },
      { path: '/crm/clients', label: 'Customers', icon: Users },
      { path: '/crm/tasks', label: 'Tasks', icon: ClipboardList },
      { path: '/crm/reports', label: 'Reports', icon: BarChart3, permission: PERMISSIONS.CRM_REPORT },
      { path: '/crm/follow-ups', label: 'Visits / Follow-ups', icon: Calendar },
      { path: '/crm/pipeline', label: 'Pipeline', icon: Activity },
      { path: '/farm/chicken-sales', label: 'Chicken Sales', icon: ShoppingCart, permission: PERMISSIONS.VIEW_CHICKEN_SALES },
      { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
    ],
  },
  {
    label: 'SERVICES',
    items: [
      { path: '/consultancy', label: 'Consultancy', icon: Briefcase, permission: PERMISSIONS.VIEW_CONSULTANCY },
      { path: '/consultancy/requests', label: 'Requests', icon: ClipboardList, permission: PERMISSIONS.VIEW_CONSULTANCY },
      { path: '/consultancy/proposals', label: 'Proposals', icon: FileText, permission: PERMISSIONS.VIEW_CONSULTANCY },
      { path: '/consultancy/engagements', label: 'Engagements', icon: Briefcase, permission: PERMISSIONS.VIEW_CONSULTANCY },
      { path: '/training', label: 'Training', icon: GraduationCap },
    ],
  },
];

const consultantMenus = [
  {
    label: 'MAIN',
    items: [{ path: '/dashboard', labelKey: 'dashboard', icon: LayoutDashboard }],
  },
  {
    label: 'CONSULTANCY',
    items: [
      { path: '/consultancy', label: 'Dashboard', icon: Briefcase },
      { path: '/consultancy/requests', label: 'Requests', icon: ClipboardList },
      { path: '/consultancy/engagements', label: 'Engagements', icon: Briefcase },
      { path: '/consultancy/activities', label: 'Activities', icon: Activity },
      { path: '/consultancy/services', label: 'Services', icon: BookOpen },
      { path: '/consultancy/reports', label: 'Reports', icon: BarChart3, permission: PERMISSIONS.CONSULTANCY_REPORT },
      { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
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

const secretaryMenus = [
  {
    label: 'MAIN',
    items: [{ path: '/dashboard', labelKey: 'dashboard', icon: LayoutDashboard }],
  },
  {
    label: 'ASSIST',
    items: [
      { path: '/crm', label: 'CRM', icon: Users2, permission: PERMISSIONS.VIEW_CRM },
      { path: '/crm/clients', label: 'Customers', icon: Users, permission: PERMISSIONS.VIEW_CRM },
      { path: '/farms', label: 'Farms', icon: Warehouse, permission: PERMISSIONS.VIEW_FARMS },
      { path: '/training', label: 'Training', icon: GraduationCap, permission: PERMISSIONS.VIEW_TRAINING },
      { path: '/reports', label: 'Reports', icon: BarChart3, permission: PERMISSIONS.VIEW_REPORTS },
      { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
    ],
  },
];

const farmWorkerMenus = [
  {
    label: 'MAIN',
    items: [{ path: '/dashboard', labelKey: 'dashboard', icon: LayoutDashboard }],
  },
  {
    label: 'FARM OPS',
    items: [
      { path: '/farms', label: 'Farms', icon: Warehouse, permission: PERMISSIONS.VIEW_FARMS },
      { path: '/flocks', label: 'Flocks', icon: Layers, permission: PERMISSIONS.VIEW_FLOCKS },
      { path: '/daily-records', label: 'Daily Records', icon: ClipboardList, permission: PERMISSIONS.VIEW_DAILY_RECORDS },
      { path: '/farm/chicken-sales', label: 'Chicken Sales', icon: ShoppingCart, permission: PERMISSIONS.VIEW_CHICKEN_SALES },
      { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
    ],
  },
];

/** Field extension: leads/tasks/clients only — not full CRM commercial write. */
const extensionWorkerMenus = [
  {
    label: 'MAIN',
    items: [{ path: '/dashboard', labelKey: 'dashboard', icon: LayoutDashboard }],
  },
  {
    label: 'FIELD CRM',
    items: [
      { path: '/crm', label: 'Dashboard', icon: Users2, permission: PERMISSIONS.VIEW_CRM },
      { path: '/crm/leads', label: 'Leads', icon: Target, permission: PERMISSIONS.CRM_LEAD },
      { path: '/crm/clients', label: 'Customers', icon: Users, permission: PERMISSIONS.VIEW_CRM },
      { path: '/crm/tasks', label: 'Tasks', icon: ClipboardList, permission: PERMISSIONS.CRM_TASK },
      { path: '/crm/follow-ups', label: 'Visits / Follow-ups', icon: Calendar, permission: PERMISSIONS.CRM_TASK },
      { path: '/farms', label: 'Farms', icon: Warehouse, permission: PERMISSIONS.VIEW_FARMS },
      { path: '/training', label: 'Training', icon: GraduationCap, permission: PERMISSIONS.VIEW_TRAINING },
      { path: '/settings', labelKey: 'settings', icon: Settings, alwaysVisible: true },
    ],
  },
];

const ROLE_MENU_MAP = {
  ADMIN: adminMenus,
  GENERAL_MANAGER: adminMenus,
  OPERATIONS_MANAGER: adminMenus,
  SECRETARY: secretaryMenus,
  FARM_MANAGER: farmManagerMenus,
  FARM_WORKER: farmWorkerMenus,
  INVENTORY_MANAGER: inventoryManagerMenus,
  PHARMACIST: pharmacistMenus,
  VETERINARIAN: veterinarianMenus,
  FINANCE: financeMenus,
  HR: hrMenus,
  CRM_MANAGER: crmManagerMenus,
  MARKETING_MANAGER: marketingMenus,
  TRAINING_COORDINATOR: trainingOfficerMenus,
  CONSULTANT: consultantMenus,
  EXTENSION_WORKER: extensionWorkerMenus,
};

export { normalizeRole, ROLE_ALIASES } from './permissions';

export const getRoleMenus = (role) => {
  const normalized = normalizeRole(role);
  return ROLE_MENU_MAP[normalized] || [{ label: 'MAIN', items: [{ path: '/dashboard', labelKey: 'dashboard', icon: LayoutDashboard }] }];
};
