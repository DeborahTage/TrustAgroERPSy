import { PERMISSIONS } from './permissions';

/** Sidebar path → module key for canAccessModule() */
export const PATH_MODULE_MAP = {
  '/dashboard': 'dashboard',
  '/users': 'users',
  '/farms': 'farms',
  '/flocks': 'flocks',
  '/daily-records': 'dailyRecords',
  '/inventory': 'inventory',
  '/veterinary': 'veterinary',
  '/pharmacy': 'pharmacy',
  '/finance': 'finance',
  '/crm': 'crm',
  '/marketing': 'marketing',
  '/training': 'training',
  '/consultancy': 'consultancy',
  '/reports': 'reports',
  '/settings': 'settings',
};

/** Create/edit form routes → required permission */
export const FORM_ROUTE_PERMISSIONS = {
  '/users/new': PERMISSIONS.CREATE_USERS,
  '/users/:id/edit': PERMISSIONS.EDIT_USERS,
  '/farms/new': PERMISSIONS.CREATE_FARMS,
  '/farms/:id/edit': PERMISSIONS.EDIT_FARMS,
  '/flocks/new': PERMISSIONS.CREATE_FLOCKS,
  '/flocks/:id/edit': PERMISSIONS.EDIT_FLOCKS,
  '/daily-records/new': PERMISSIONS.CREATE_DAILY_RECORDS,
  '/daily-records/:id/edit': PERMISSIONS.EDIT_DAILY_RECORDS,
  '/inventory/items/new': PERMISSIONS.CREATE_INVENTORY_ITEMS,
  '/inventory/items/:id/edit': PERMISSIONS.EDIT_INVENTORY_ITEMS,
  '/inventory/stock-in': PERMISSIONS.STOCK_IN,
  '/inventory/stock-out': PERMISSIONS.STOCK_OUT,
  '/veterinary/vaccinations/new': PERMISSIONS.MANAGE_VACCINATIONS,
  '/veterinary/health-reports/new': PERMISSIONS.CREATE_DISEASE_CASES,
  '/veterinary/disease-cases/new': PERMISSIONS.CREATE_DISEASE_CASES,
  '/veterinary/treatments/new': PERMISSIONS.CREATE_TREATMENTS,
  '/veterinary/prescriptions/new': PERMISSIONS.CREATE_PRESCRIPTIONS,
  '/pharmacy/sales/new': PERMISSIONS.CREATE_PHARMACY_SALES,
  '/pharmacy/customers/new': PERMISSIONS.MANAGE_CUSTOMERS,
  '/finance/new': PERMISSIONS.CREATE_INCOME,
  '/crm/clients/new': PERMISSIONS.MANAGE_CLIENTS,
  '/crm/clients/:id/edit': PERMISSIONS.MANAGE_CLIENTS,
  '/crm/visits/new': PERMISSIONS.CREATE_FARM_VISITS,
};
