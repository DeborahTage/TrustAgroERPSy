export const ROLES = {
  ADMIN: 'ADMIN',
  FARM_MANAGER: 'FARM_MANAGER',
  INVENTORY_MANAGER: 'INVENTORY_MANAGER',
  PHARMACIST: 'PHARMACIST',
  VETERINARIAN: 'VETERINARIAN',
  FINANCE: 'FINANCE',
  CRM_MANAGER: 'CRM_MANAGER',
  MARKETING: 'MARKETING',
  TRAINING_OFFICER: 'TRAINING_OFFICER',
  // Legacy
  GENERAL_MANAGER: 'GENERAL_MANAGER',
  OPERATIONS_MANAGER: 'OPERATIONS_MANAGER',
  STORE_KEEPER: 'STORE_KEEPER',
  PHARMACY_SALES: 'PHARMACY_SALES',
  VETERINARY_OFFICER: 'VETERINARY_OFFICER',
  FINANCE_OFFICER: 'FINANCE_OFFICER',
  EXTENSION_WORKER: 'EXTENSION_WORKER',
};

export const ADMIN_ROLES = [ROLES.ADMIN];

export const formatCurrency = (amount) =>
  new Intl.NumberFormat('en-ET', { style: 'currency', currency: 'ETB' }).format(amount || 0);

export const formatDate = (date) =>
  date ? new Date(date).toLocaleDateString('en-GB') : '-';

export const statusBadge = (status) => {
  const map = {
    ACTIVE: 'success', INACTIVE: 'secondary', CLOSED: 'dark',
    PENDING: 'warning', DISPENSED: 'success', CANCELLED: 'danger',
    SCHEDULED: 'primary', COMPLETED: 'success', MISSED: 'danger',
    LEAD: 'info', LOST: 'danger', LOW: 'success', MODERATE: 'warning',
    HIGH: 'danger', CRITICAL: 'dark', CONTROLLED: 'warning', RESOLVED: 'success',
    OPEN: 'danger', REVIEWED: 'info',
  };
  return map[status] || 'secondary';
};
