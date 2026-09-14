/** Canonical roles — mirrors backend RolePermissionCatalog / Role enum */
export const ROLES = {
  ADMIN: 'ADMIN',
  GENERAL_MANAGER: 'GENERAL_MANAGER',
  OPERATIONS_MANAGER: 'OPERATIONS_MANAGER',
  SECRETARY: 'SECRETARY',
  FINANCE: 'FINANCE',
  HR: 'HR',
  FARM_MANAGER: 'FARM_MANAGER',
  FARM_WORKER: 'FARM_WORKER',
  VETERINARIAN: 'VETERINARIAN',
  INVENTORY_MANAGER: 'INVENTORY_MANAGER',
  PHARMACIST: 'PHARMACIST',
  CRM_MANAGER: 'CRM_MANAGER',
  MARKETING_MANAGER: 'MARKETING_MANAGER',
  TRAINING_COORDINATOR: 'TRAINING_COORDINATOR',
  CONSULTANT: 'CONSULTANT',
  EXTENSION_WORKER: 'EXTENSION_WORKER',
  // Legacy keys → canonical values (compat)
  MARKETING: 'MARKETING_MANAGER',
  TRAINING_OFFICER: 'TRAINING_COORDINATOR',
  STORE_KEEPER: 'INVENTORY_MANAGER',
  PHARMACY_SALES: 'PHARMACIST',
  VETERINARY_OFFICER: 'VETERINARIAN',
  FINANCE_OFFICER: 'FINANCE',
};

/** Canonical role IDs for UserForm and admin role pickers (no legacy aliases). */
export const CANONICAL_ROLES = [
  ROLES.ADMIN,
  ROLES.GENERAL_MANAGER,
  ROLES.OPERATIONS_MANAGER,
  ROLES.SECRETARY,
  ROLES.FINANCE,
  ROLES.HR,
  ROLES.FARM_MANAGER,
  ROLES.FARM_WORKER,
  ROLES.VETERINARIAN,
  ROLES.INVENTORY_MANAGER,
  ROLES.PHARMACIST,
  ROLES.CRM_MANAGER,
  ROLES.MARKETING_MANAGER,
  ROLES.TRAINING_COORDINATOR,
  ROLES.CONSULTANT,
  ROLES.EXTENSION_WORKER,
];

export const ROLE_LABELS = {
  ADMIN: 'Admin',
  GENERAL_MANAGER: 'General Manager',
  OPERATIONS_MANAGER: 'Operations Manager',
  SECRETARY: 'Secretary',
  FINANCE: 'Finance',
  HR: 'HR',
  FARM_MANAGER: 'Farm Manager',
  FARM_WORKER: 'Farm Worker',
  VETERINARIAN: 'Veterinarian',
  INVENTORY_MANAGER: 'Inventory Manager',
  PHARMACIST: 'Pharmacist',
  CRM_MANAGER: 'CRM Manager',
  MARKETING_MANAGER: 'Marketing Manager',
  TRAINING_COORDINATOR: 'Training Coordinator',
  CONSULTANT: 'Consultant',
  EXTENSION_WORKER: 'Extension Worker',
};

/**
 * Permission constants — values are backend MODULE:ACTION strings.
 * Exported names kept for call-site compatibility.
 */
export const PERMISSIONS = {
  VIEW_ADMIN_DASHBOARD: 'users:read',
  VIEW_FARM_DASHBOARD: 'farm:read',
  VIEW_VETERINARY_DASHBOARD: 'veterinary:read',
  VIEW_INVENTORY_DASHBOARD: 'inventory:read',
  VIEW_PHARMACY_DASHBOARD: 'pharmacy:read',
  VIEW_FINANCE_DASHBOARD: 'finance:read',
  VIEW_CRM_DASHBOARD: 'crm:read',
  VIEW_MARKETING_DASHBOARD: 'marketing:read',
  VIEW_TRAINING_DASHBOARD: 'training:read',

  VIEW_USERS: 'users:read',
  CREATE_USERS: 'users:create',
  EDIT_USERS: 'users:update',
  DELETE_USERS: 'users:delete',
  ASSIGN_ROLES: 'roles:manage',
  ACTIVATE_USERS: 'users:update',
  DEACTIVATE_USERS: 'users:update',

  VIEW_FARMS: 'farm:read',
  CREATE_FARMS: 'farm:create',
  EDIT_FARMS: 'farm:update',
  DELETE_FARMS: 'farm:delete',
  VIEW_FLOCKS: 'farm:read',
  CREATE_FLOCKS: 'farm:create',
  EDIT_FLOCKS: 'farm:update',
  DELETE_FLOCKS: 'farm:delete',
  VIEW_DAILY_RECORDS: 'farm:read',
  CREATE_DAILY_RECORDS: 'farm:create',
  EDIT_DAILY_RECORDS: 'farm:update',
  DELETE_DAILY_RECORDS: 'farm:update',
  VIEW_CHICKEN_SALES: 'chickensale:read',
  CREATE_CHICKEN_SALES: 'chickensale:create',
  EDIT_CHICKEN_SALES: 'chickensale:update',
  CONFIRM_CHICKEN_SALES: 'chickensale:confirm',
  CANCEL_CHICKEN_SALES: 'chickensale:cancel',
  INVOICE_CHICKEN_SALES: 'chickensale:invoice',
  PAY_CHICKEN_SALES: 'chickensale:pay',

  VIEW_VETERINARY: 'veterinary:read',
  CREATE_DISEASE_CASES: 'veterinary:create',
  EDIT_DISEASE_CASES: 'veterinary:update',
  DELETE_DISEASE_CASES: 'veterinary:update',
  CREATE_TREATMENTS: 'veterinary:create',
  EDIT_TREATMENTS: 'veterinary:update',
  DELETE_TREATMENTS: 'veterinary:update',
  CREATE_PRESCRIPTIONS: 'veterinary:prescribe',
  EDIT_PRESCRIPTIONS: 'veterinary:prescribe',
  DELETE_PRESCRIPTIONS: 'veterinary:prescribe',
  MANAGE_VACCINATIONS: 'veterinary:update',
  VIEW_HEALTH_REPORTS: 'veterinary:read',

  VIEW_INVENTORY: 'inventory:read',
  CREATE_INVENTORY_ITEMS: 'inventory:create',
  EDIT_INVENTORY_ITEMS: 'inventory:update',
  DELETE_INVENTORY_ITEMS: 'inventory:update',
  STOCK_IN: 'inventory:stock_in',
  STOCK_OUT: 'inventory:stock_out',
  STOCK_TRANSFER: 'inventory:transfer',
  MANAGE_PO: 'inventory:po',
  VIEW_INVENTORY_REPORTS: 'inventory:read',
  VIEW_EXPIRY_MONITORING: 'inventory:read',

  VIEW_PHARMACY: 'pharmacy:read',
  VIEW_PHARMACY_STOCK: 'pharmacy:read',
  CREATE_PHARMACY_SALES: 'pharmacy:sale',
  EDIT_PHARMACY_SALES: 'pharmacy:sale',
  DELETE_PHARMACY_SALES: 'pharmacy:sale',
  MANAGE_CUSTOMERS: 'pharmacy:sale',
  DISPENSE_PRESCRIPTIONS: 'pharmacy:dispense',
  VIEW_RECEIPTS: 'pharmacy:read',
  PHARMACY_RETURN: 'pharmacy:return',

  VIEW_FINANCE: 'finance:read',
  CREATE_INCOME: 'finance:create',
  EDIT_INCOME: 'finance:create',
  DELETE_INCOME: 'finance:create',
  CREATE_EXPENSES: 'finance:create',
  EDIT_EXPENSES: 'finance:create',
  DELETE_EXPENSES: 'finance:create',
  APPROVE_FINANCE: 'finance:approve',
  RECONCILE_FINANCE: 'finance:reconcile',
  VIEW_FINANCIAL_REPORTS: 'reports:read',

  VIEW_CRM: 'crm:read',
  MANAGE_CLIENTS: 'crm:update',
  MANAGE_LEADS: 'crm:create',
  MANAGE_PIPELINE: 'crm:update',
  CREATE_FARM_VISITS: 'crm:create',
  EDIT_FARM_VISITS: 'crm:update',
  DELETE_FARM_VISITS: 'crm:update',
  MANAGE_FOLLOW_UPS: 'crm:update',
  MANAGE_CRM_TASKS: 'crm:update',
  VIEW_CRM_ORDERS: 'crm:read',

  VIEW_MARKETING: 'marketing:read',
  MANAGE_CAMPAIGNS: 'marketing:create',
  MANAGE_MARKETING_LEADS: 'marketing:create',
  VIEW_CAMPAIGN_ANALYTICS: 'marketing:read',
  EDIT_MARKETING: 'marketing:update',

  VIEW_TRAINING: 'training:read',
  MANAGE_TRAINING: 'training:create',
  EDIT_TRAINING: 'training:update',
  PUBLISH_TRAINING: 'training:publish',
  REGISTER_TRAINING: 'training:register',
  TRAINING_ATTENDANCE: 'training:attendance',
  COMPLETE_TRAINING: 'training:complete',
  TRAINING_CERTIFICATE: 'training:certificate',
  TRAINING_PAYMENT: 'training:payment',
  TRAINING_REPORT: 'training:report',
  CANCEL_TRAINING: 'training:cancel',
  VIEW_CONSULTANCY: 'consultancy:read',
  MANAGE_CONSULTANCY: 'consultancy:create',
  EDIT_CONSULTANCY: 'consultancy:update',

  VIEW_HR: 'hr:read',
  CREATE_HR: 'hr:create',
  EDIT_HR: 'hr:update',

  VIEW_REPORTS: 'reports:read',
  EXPORT_REPORTS: 'reports:read',
  VIEW_SETTINGS: 'settings:read',
  MANAGE_SETTINGS: 'settings:update',
  VIEW_AUDIT_LOGS: 'audit:read',
  VIEW_MASTER_DATA: 'masterdata:read',
  MANAGE_MASTER_DATA: 'masterdata:update',
  VIEW_NOTIFICATIONS: 'notifications:read',
};

/** Legacy role names → canonical role (normalize only; never offer in ROLE_OPTIONS). */
export const ROLE_ALIASES = {
  STORE_KEEPER: 'INVENTORY_MANAGER',
  PHARMACY_SALES: 'PHARMACIST',
  FINANCE_OFFICER: 'FINANCE',
  VETERINARY_OFFICER: 'VETERINARIAN',
  MARKETING: 'MARKETING_MANAGER',
  TRAINING_OFFICER: 'TRAINING_COORDINATOR',
};

export const normalizeRole = (role) => {
  if (!role) return role;
  return ROLE_ALIASES[role] || role;
};

/** Mirrors backend RolePermissionCatalog */
export const ROLE_PERMISSIONS = {
  [ROLES.ADMIN]: [
    PERMISSIONS.VIEW_USERS, PERMISSIONS.CREATE_USERS, PERMISSIONS.EDIT_USERS, PERMISSIONS.DELETE_USERS,
    PERMISSIONS.ASSIGN_ROLES,
    PERMISSIONS.VIEW_SETTINGS, PERMISSIONS.MANAGE_SETTINGS,
    PERMISSIONS.VIEW_AUDIT_LOGS, PERMISSIONS.VIEW_REPORTS,
    PERMISSIONS.VIEW_MASTER_DATA, PERMISSIONS.MANAGE_MASTER_DATA,
    PERMISSIONS.VIEW_NOTIFICATIONS,
    PERMISSIONS.VIEW_FARMS, PERMISSIONS.VIEW_VETERINARY, PERMISSIONS.VIEW_INVENTORY, PERMISSIONS.VIEW_PHARMACY,
    PERMISSIONS.VIEW_FINANCE, PERMISSIONS.VIEW_CRM, PERMISSIONS.VIEW_MARKETING, PERMISSIONS.VIEW_TRAINING,
    PERMISSIONS.VIEW_CONSULTANCY, PERMISSIONS.VIEW_HR, PERMISSIONS.VIEW_CHICKEN_SALES,
  ],

  [ROLES.GENERAL_MANAGER]: [
    PERMISSIONS.VIEW_REPORTS, PERMISSIONS.VIEW_NOTIFICATIONS, PERMISSIONS.VIEW_SETTINGS, PERMISSIONS.VIEW_AUDIT_LOGS,
    PERMISSIONS.VIEW_FARMS, PERMISSIONS.VIEW_VETERINARY, PERMISSIONS.VIEW_INVENTORY, PERMISSIONS.VIEW_PHARMACY,
    PERMISSIONS.VIEW_FINANCE, PERMISSIONS.APPROVE_FINANCE, PERMISSIONS.VIEW_CRM, PERMISSIONS.VIEW_MARKETING,
    PERMISSIONS.VIEW_TRAINING, PERMISSIONS.VIEW_CONSULTANCY, PERMISSIONS.VIEW_HR, PERMISSIONS.VIEW_CHICKEN_SALES,
  ],

  [ROLES.OPERATIONS_MANAGER]: [
    PERMISSIONS.VIEW_REPORTS, PERMISSIONS.VIEW_NOTIFICATIONS, PERMISSIONS.VIEW_SETTINGS,
    PERMISSIONS.VIEW_FARMS, PERMISSIONS.EDIT_FARMS, PERMISSIONS.VIEW_VETERINARY, PERMISSIONS.VIEW_INVENTORY,
    PERMISSIONS.VIEW_PHARMACY, PERMISSIONS.VIEW_CRM,
    PERMISSIONS.VIEW_CHICKEN_SALES, PERMISSIONS.CONFIRM_CHICKEN_SALES,
  ],

  [ROLES.SECRETARY]: [
    PERMISSIONS.VIEW_NOTIFICATIONS, PERMISSIONS.VIEW_SETTINGS, PERMISSIONS.VIEW_CRM, PERMISSIONS.VIEW_REPORTS,
    PERMISSIONS.VIEW_FARMS, PERMISSIONS.VIEW_TRAINING,
  ],

  [ROLES.FINANCE]: [
    PERMISSIONS.VIEW_FINANCE, PERMISSIONS.CREATE_INCOME, PERMISSIONS.APPROVE_FINANCE, PERMISSIONS.RECONCILE_FINANCE,
    PERMISSIONS.VIEW_REPORTS, PERMISSIONS.VIEW_NOTIFICATIONS, PERMISSIONS.VIEW_SETTINGS,
    PERMISSIONS.VIEW_CHICKEN_SALES, PERMISSIONS.INVOICE_CHICKEN_SALES, PERMISSIONS.PAY_CHICKEN_SALES,
  ],

  [ROLES.HR]: [
    PERMISSIONS.VIEW_HR, PERMISSIONS.CREATE_HR, PERMISSIONS.EDIT_HR, PERMISSIONS.VIEW_REPORTS,
    PERMISSIONS.VIEW_NOTIFICATIONS, PERMISSIONS.VIEW_SETTINGS,
  ],

  [ROLES.FARM_MANAGER]: [
    PERMISSIONS.VIEW_FARMS, PERMISSIONS.CREATE_FARMS, PERMISSIONS.EDIT_FARMS, PERMISSIONS.DELETE_FARMS,
    PERMISSIONS.VIEW_REPORTS, PERMISSIONS.VIEW_NOTIFICATIONS, PERMISSIONS.VIEW_SETTINGS, PERMISSIONS.VIEW_INVENTORY,
    PERMISSIONS.VIEW_CHICKEN_SALES, PERMISSIONS.CREATE_CHICKEN_SALES, PERMISSIONS.EDIT_CHICKEN_SALES,
    PERMISSIONS.CONFIRM_CHICKEN_SALES, PERMISSIONS.CANCEL_CHICKEN_SALES,
  ],

  [ROLES.FARM_WORKER]: [
    PERMISSIONS.VIEW_FARMS, PERMISSIONS.EDIT_FARMS, PERMISSIONS.VIEW_NOTIFICATIONS, PERMISSIONS.VIEW_SETTINGS,
    PERMISSIONS.VIEW_CHICKEN_SALES,
  ],

  [ROLES.VETERINARIAN]: [
    PERMISSIONS.VIEW_VETERINARY, PERMISSIONS.CREATE_DISEASE_CASES, PERMISSIONS.EDIT_DISEASE_CASES,
    PERMISSIONS.CREATE_TREATMENTS, PERMISSIONS.CREATE_PRESCRIPTIONS, PERMISSIONS.MANAGE_VACCINATIONS,
    PERMISSIONS.VIEW_FARMS, PERMISSIONS.VIEW_REPORTS, PERMISSIONS.VIEW_NOTIFICATIONS, PERMISSIONS.VIEW_SETTINGS,
  ],

  [ROLES.INVENTORY_MANAGER]: [
    PERMISSIONS.VIEW_INVENTORY, PERMISSIONS.CREATE_INVENTORY_ITEMS, PERMISSIONS.EDIT_INVENTORY_ITEMS,
    PERMISSIONS.STOCK_IN, PERMISSIONS.STOCK_OUT, PERMISSIONS.STOCK_TRANSFER, PERMISSIONS.MANAGE_PO,
    PERMISSIONS.VIEW_REPORTS, PERMISSIONS.VIEW_NOTIFICATIONS, PERMISSIONS.VIEW_SETTINGS,
  ],

  // Pharmacy ops + inventory READ only (no stock_in / transfer / po)
  [ROLES.PHARMACIST]: [
    PERMISSIONS.VIEW_PHARMACY, PERMISSIONS.CREATE_PHARMACY_SALES, PERMISSIONS.DISPENSE_PRESCRIPTIONS,
    PERMISSIONS.PHARMACY_RETURN,
    PERMISSIONS.VIEW_INVENTORY, PERMISSIONS.VIEW_NOTIFICATIONS, PERMISSIONS.VIEW_SETTINGS, PERMISSIONS.VIEW_REPORTS,
  ],

  [ROLES.CRM_MANAGER]: [
    PERMISSIONS.VIEW_CRM, PERMISSIONS.MANAGE_LEADS, PERMISSIONS.MANAGE_CLIENTS, PERMISSIONS.MANAGE_PIPELINE,
    PERMISSIONS.VIEW_CONSULTANCY, PERMISSIONS.VIEW_TRAINING,
    PERMISSIONS.VIEW_REPORTS, PERMISSIONS.VIEW_NOTIFICATIONS, PERMISSIONS.VIEW_SETTINGS,
    PERMISSIONS.VIEW_CHICKEN_SALES, PERMISSIONS.CREATE_CHICKEN_SALES, PERMISSIONS.VIEW_FARMS,
  ],

  [ROLES.MARKETING_MANAGER]: [
    PERMISSIONS.VIEW_MARKETING, PERMISSIONS.MANAGE_CAMPAIGNS, PERMISSIONS.EDIT_MARKETING,
    PERMISSIONS.VIEW_CRM, PERMISSIONS.VIEW_REPORTS, PERMISSIONS.VIEW_NOTIFICATIONS, PERMISSIONS.VIEW_SETTINGS,
  ],

  [ROLES.TRAINING_COORDINATOR]: [
    PERMISSIONS.VIEW_TRAINING, PERMISSIONS.MANAGE_TRAINING, PERMISSIONS.EDIT_TRAINING,
    PERMISSIONS.VIEW_REPORTS, PERMISSIONS.VIEW_NOTIFICATIONS, PERMISSIONS.VIEW_SETTINGS,
  ],

  [ROLES.CONSULTANT]: [
    PERMISSIONS.VIEW_CONSULTANCY, PERMISSIONS.MANAGE_CONSULTANCY, PERMISSIONS.EDIT_CONSULTANCY,
    PERMISSIONS.VIEW_CRM, PERMISSIONS.VIEW_NOTIFICATIONS, PERMISSIONS.VIEW_SETTINGS,
  ],

  [ROLES.EXTENSION_WORKER]: [
    PERMISSIONS.VIEW_CRM, PERMISSIONS.MANAGE_CLIENTS, PERMISSIONS.VIEW_FARMS, PERMISSIONS.VIEW_TRAINING,
    PERMISSIONS.VIEW_NOTIFICATIONS, PERMISSIONS.VIEW_SETTINGS,
  ],
};

/**
 * @param {string|string[]} roleOrPermissions - role id, or a permissions array
 * @param {string} permission - MODULE:ACTION string
 */
export const hasPermission = (roleOrPermissions, permission) => {
  if (!permission) return false;
  if (Array.isArray(roleOrPermissions)) {
    return roleOrPermissions.includes(permission);
  }
  if (!roleOrPermissions) return false;
  const perms = ROLE_PERMISSIONS[normalizeRole(roleOrPermissions)] || [];
  return perms.includes(permission);
};

/** Prefer user.permissions from login/me when present; else role matrix. */
export const hasPermissionForUser = (user, permission) => {
  if (!user || !permission) return false;
  if (Array.isArray(user.permissions) && user.permissions.length > 0) {
    return user.permissions.includes(permission);
  }
  return hasPermission(user.role, permission);
};

export const hasAnyPermission = (roleOrPermissions, permissions) => {
  if (!permissions?.length) return false;
  return permissions.some((p) => hasPermission(roleOrPermissions, p));
};

export const hasAnyPermissionForUser = (user, permissions) => {
  if (!user || !permissions?.length) return false;
  if (Array.isArray(user.permissions) && user.permissions.length > 0) {
    return permissions.some((p) => user.permissions.includes(p));
  }
  return hasAnyPermission(user.role, permissions);
};

export const hasAllPermissions = (roleOrPermissions, permissions) => {
  if (!permissions?.length) return false;
  return permissions.every((p) => hasPermission(roleOrPermissions, p));
};

export const getRolePermissions = (role) => ROLE_PERMISSIONS[normalizeRole(role)] || [];

const MODULE_PERMISSION_MAP = {
  dashboard: [
    PERMISSIONS.VIEW_USERS, PERMISSIONS.VIEW_FARMS, PERMISSIONS.VIEW_VETERINARY,
    PERMISSIONS.VIEW_INVENTORY, PERMISSIONS.VIEW_PHARMACY, PERMISSIONS.VIEW_FINANCE,
    PERMISSIONS.VIEW_CRM, PERMISSIONS.VIEW_MARKETING, PERMISSIONS.VIEW_TRAINING,
    PERMISSIONS.VIEW_REPORTS, PERMISSIONS.VIEW_NOTIFICATIONS, PERMISSIONS.VIEW_HR,
  ],
  users: [PERMISSIONS.VIEW_USERS],
  farms: [PERMISSIONS.VIEW_FARMS],
  flocks: [PERMISSIONS.VIEW_FLOCKS],
  dailyRecords: [PERMISSIONS.VIEW_DAILY_RECORDS],
  chickenSales: [PERMISSIONS.VIEW_CHICKEN_SALES],
  inventory: [PERMISSIONS.VIEW_INVENTORY],
  veterinary: [PERMISSIONS.VIEW_VETERINARY],
  pharmacy: [PERMISSIONS.VIEW_PHARMACY],
  finance: [PERMISSIONS.VIEW_FINANCE],
  crm: [PERMISSIONS.VIEW_CRM],
  marketing: [PERMISSIONS.VIEW_MARKETING],
  training: [PERMISSIONS.VIEW_TRAINING],
  consultancy: [PERMISSIONS.VIEW_CONSULTANCY],
  reports: [PERMISSIONS.VIEW_REPORTS],
  settings: [PERMISSIONS.VIEW_SETTINGS],
  hr: [PERMISSIONS.VIEW_HR],
};

export const canAccessModule = (role, module) => {
  if (!role) return false;
  if (module === 'dashboard') {
    return hasAnyPermission(role, MODULE_PERMISSION_MAP.dashboard);
  }
  const required = MODULE_PERMISSION_MAP[module];
  if (!required) return false;
  return hasAnyPermission(role, required);
};

export const canAccessModuleForUser = (user, module) => {
  if (!user) return false;
  const required = MODULE_PERMISSION_MAP[module === 'dashboard' ? 'dashboard' : module];
  if (!required) return false;
  return hasAnyPermissionForUser(user, required);
};

const ACTION_PERMISSION_MAP = {
  create: {
    users: PERMISSIONS.CREATE_USERS,
    farms: PERMISSIONS.CREATE_FARMS,
    flocks: PERMISSIONS.CREATE_FLOCKS,
    dailyRecords: PERMISSIONS.CREATE_DAILY_RECORDS,
    chickenSales: PERMISSIONS.CREATE_CHICKEN_SALES,
    inventoryItems: PERMISSIONS.CREATE_INVENTORY_ITEMS,
    diseaseCases: PERMISSIONS.CREATE_DISEASE_CASES,
    treatments: PERMISSIONS.CREATE_TREATMENTS,
    prescriptions: PERMISSIONS.CREATE_PRESCRIPTIONS,
    pharmacySales: PERMISSIONS.CREATE_PHARMACY_SALES,
    income: PERMISSIONS.CREATE_INCOME,
    expenses: PERMISSIONS.CREATE_EXPENSES,
    farmVisits: PERMISSIONS.CREATE_FARM_VISITS,
    customers: PERMISSIONS.MANAGE_CLIENTS,
    leads: PERMISSIONS.MANAGE_LEADS,
    campaigns: PERMISSIONS.MANAGE_CAMPAIGNS,
  },
  edit: {
    users: PERMISSIONS.EDIT_USERS,
    farms: PERMISSIONS.EDIT_FARMS,
    flocks: PERMISSIONS.EDIT_FLOCKS,
    dailyRecords: PERMISSIONS.EDIT_DAILY_RECORDS,
    chickenSales: PERMISSIONS.EDIT_CHICKEN_SALES,
    inventoryItems: PERMISSIONS.EDIT_INVENTORY_ITEMS,
    diseaseCases: PERMISSIONS.EDIT_DISEASE_CASES,
    treatments: PERMISSIONS.EDIT_TREATMENTS,
    prescriptions: PERMISSIONS.EDIT_PRESCRIPTIONS,
    pharmacySales: PERMISSIONS.EDIT_PHARMACY_SALES,
    income: PERMISSIONS.EDIT_INCOME,
    expenses: PERMISSIONS.EDIT_EXPENSES,
    farmVisits: PERMISSIONS.EDIT_FARM_VISITS,
    customers: PERMISSIONS.MANAGE_CLIENTS,
  },
  delete: {
    users: PERMISSIONS.DELETE_USERS,
    farms: PERMISSIONS.DELETE_FARMS,
    flocks: PERMISSIONS.DELETE_FLOCKS,
    dailyRecords: PERMISSIONS.DELETE_DAILY_RECORDS,
    inventoryItems: PERMISSIONS.DELETE_INVENTORY_ITEMS,
  },
  stockIn: { inventoryItems: PERMISSIONS.STOCK_IN },
  stockOut: { inventoryItems: PERMISSIONS.STOCK_OUT },
};

export const canPerformAction = (role, action, module) => {
  const permission = ACTION_PERMISSION_MAP[action]?.[module];
  if (!permission) return false;
  return hasPermission(role, permission);
};

export const canPerformActionForUser = (user, action, module) => {
  if (!user) return false;
  const permission = ACTION_PERMISSION_MAP[action]?.[module];
  if (!permission) return false;
  return hasPermissionForUser(user, permission);
};
