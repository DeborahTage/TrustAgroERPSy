/**
 * Page-by-page upgrade tracker for Trust Agro ERP.
 * Status: done | pending | partial
 */
export const PAGE_UPGRADE_REGISTRY = [
  // ── Auth & Dashboard ──
  { step: 1, path: '/login', name: 'Login', module: 'Auth', status: 'done' },
  { step: 2, path: '/dashboard', name: 'Role Dashboard', module: 'Dashboard', status: 'done' },
  { step: 3, path: '/dashboard', name: 'Executive Dashboard (Admin)', module: 'Dashboard', status: 'done' },

  // ── Administration ──
  { step: 4, path: '/users', name: 'User List', module: 'Users', status: 'done' },
  { step: 5, path: '/users/new', name: 'User Form', module: 'Users', status: 'done' },

  // ── Farm ──
  { step: 6, path: '/farms', name: 'Farm List', module: 'Farm', status: 'done' },
  { step: 7, path: '/farms/new', name: 'Farm Form', module: 'Farm', status: 'done' },
  { step: 8, path: '/farms/barn-dashboard', name: 'Barn Dashboard', module: 'Farm', status: 'done' },
  { step: 9, path: '/flocks', name: 'Flock List', module: 'Farm', status: 'done' },
  { step: 10, path: '/flocks/new', name: 'Flock Form', module: 'Farm', status: 'done' },
  { step: 11, path: '/flocks/:id/eggs', name: 'Egg Analytics', module: 'Farm', status: 'done' },
  { step: 12, path: '/daily-records', name: 'Daily Records', module: 'Farm', status: 'done' },
  { step: 13, path: '/daily-records/new', name: 'Daily Record Form', module: 'Farm', status: 'done' },
  { step: 14, path: '/feed/silos', name: 'Silo Dashboard', module: 'Feed', status: 'done' },
  { step: 15, path: '/feed/recipes', name: 'Feed Recipes', module: 'Feed', status: 'done' },

  // ── Inventory ──
  { step: 16, path: '/inventory', name: 'Inventory Dashboard', module: 'Inventory', status: 'done' },
  { step: 17, path: '/inventory/stock-in', name: 'Stock In (GRN)', module: 'Inventory', status: 'done' },
  { step: 18, path: '/inventory/stock-out', name: 'Stock Out', module: 'Inventory', status: 'done' },
  { step: 19, path: '/inventory/requisitions', name: 'Requisition Center', module: 'Inventory', status: 'done' },
  { step: 20, path: '/inventory/suppliers', name: 'Suppliers', module: 'Inventory', status: 'done' },
  { step: 21, path: '/inventory/po', name: 'Purchase Orders', module: 'Inventory', status: 'done' },
  { step: 22, path: '/inventory/expiry', name: 'Expiry Tracking', module: 'Inventory', status: 'done' },
  { step: 23, path: '/inventory/scan', name: 'Barcode Scan', module: 'Inventory', status: 'done' },
  { step: 24, path: '/inventory/count', name: 'Cycle Count', module: 'Inventory', status: 'done' },
  { step: 25, path: '/inventory/forecast', name: 'Forecasting', module: 'Inventory', status: 'done' },
  { step: 26, path: '/inventory/reports', name: 'Inventory Reports', module: 'Inventory', status: 'done' },
  { step: 27, path: '/inventory/items/new', name: 'Item Form', module: 'Inventory', status: 'done' },

  // ── Veterinary ──
  { step: 28, path: '/veterinary', name: 'Veterinary Hub', module: 'Veterinary', status: 'done' },
  { step: 29, path: '/vet', name: 'Vet Dashboard', module: 'Veterinary', status: 'done' },
  { step: 30, path: '/vet/flock-emr/:id', name: 'Flock EMR', module: 'Veterinary', status: 'done' },
  { step: 31, path: '/vet/vaccinations', name: 'Vaccination Calendar', module: 'Veterinary', status: 'done' },
  { step: 32, path: '/vet/necropsy', name: 'Necropsy Terminal', module: 'Veterinary', status: 'done' },
  { step: 33, path: '/veterinary/prescriptions/new', name: 'Prescription Form', module: 'Veterinary', status: 'done' },
  { step: 34, path: '/veterinary/reports', name: 'Drug Usage Report', module: 'Veterinary', status: 'done' },

  // ── Pharmacy ──
  { step: 35, path: '/pharmacy', name: 'Pharmacy Overview', module: 'Pharmacy', status: 'done' },
  { step: 36, path: '/pharmacy/pos', name: 'POS Terminal', module: 'Pharmacy', status: 'done' },
  { step: 37, path: '/pharmacy/process-rx', name: 'Process Prescription', module: 'Pharmacy', status: 'done' },
  { step: 38, path: '/pharmacy/customers', name: 'Customers', module: 'Pharmacy', status: 'done' },
  { step: 39, path: '/pharmacy/reports', name: 'Pharmacy Reports', module: 'Pharmacy', status: 'done' },

  // ── Finance ──
  { step: 40, path: '/finance', name: 'Finance Dashboard', module: 'Finance', status: 'done' },
  { step: 41, path: '/finance/coa', name: 'Chart of Accounts', module: 'Finance', status: 'done' },
  { step: 42, path: '/finance/invoices', name: 'Invoice Center', module: 'Finance', status: 'done' },
  { step: 43, path: '/finance/wallets', name: 'Digital Wallets', module: 'Finance', status: 'done' },
  { step: 44, path: '/finance/reconciliation', name: 'Cash Reconciliation', module: 'Finance', status: 'done' },
  { step: 45, path: '/finance/reports', name: 'Report Center', module: 'Finance', status: 'done' },
  { step: 46, path: '/finance/flock-cogs', name: 'Flock Profitability', module: 'Finance', status: 'done' },
  { step: 47, path: '/finance/new', name: 'Journal Entry', module: 'Finance', status: 'done' },

  // ── CRM & Other ──
  { step: 48, path: '/crm', name: 'CRM Hub', module: 'CRM', status: 'done' },
  { step: 49, path: '/crm/orders', name: 'Customer Orders', module: 'CRM', status: 'done' },
  { step: 50, path: '/training', name: 'Training', module: 'Training', status: 'done' },
  { step: 51, path: '/reports', name: 'Reports', module: 'Reports', status: 'done' },
  { step: 52, path: '/settings', name: 'Settings & Audit', module: 'Settings', status: 'done' },
];

export const getUpgradeProgress = () => {
  const total = PAGE_UPGRADE_REGISTRY.length;
  const done = PAGE_UPGRADE_REGISTRY.filter((p) => p.status === 'done').length;
  const partial = PAGE_UPGRADE_REGISTRY.filter((p) => p.status === 'partial').length;
  return { total, done, partial, pending: total - done - partial, percent: Math.round((done / total) * 100) };
};
