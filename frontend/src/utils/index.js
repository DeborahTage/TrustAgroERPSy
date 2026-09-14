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
  // Legacy aliases → canonical string (compat for old comparisons)
  MARKETING: 'MARKETING_MANAGER',
  TRAINING_OFFICER: 'TRAINING_COORDINATOR',
  STORE_KEEPER: 'INVENTORY_MANAGER',
  PHARMACY_SALES: 'PHARMACIST',
  VETERINARY_OFFICER: 'VETERINARIAN',
  FINANCE_OFFICER: 'FINANCE',
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
