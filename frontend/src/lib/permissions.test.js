import {
  PERMISSIONS,
  ROLES,
  hasPermission,
  hasPermissionForUser,
  normalizeRole,
  ROLE_PERMISSIONS,
} from './permissions';

describe('RBAC permissions alignment', () => {
  test('pharmacist cannot stock-in or transfer', () => {
    expect(hasPermission(ROLES.PHARMACIST, PERMISSIONS.STOCK_IN)).toBe(false);
    expect(hasPermission(ROLES.PHARMACIST, PERMISSIONS.STOCK_OUT)).toBe(false);
    expect(hasPermission(ROLES.PHARMACIST, 'inventory:transfer')).toBe(false);
    expect(hasPermission(ROLES.PHARMACIST, PERMISSIONS.CREATE_PHARMACY_SALES)).toBe(true);
    expect(hasPermission(ROLES.PHARMACIST, PERMISSIONS.DISPENSE_PRESCRIPTIONS)).toBe(true);
    expect(hasPermission(ROLES.PHARMACIST, PERMISSIONS.VIEW_INVENTORY)).toBe(true);
  });

  test('finance cannot prescribe', () => {
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.CREATE_PRESCRIPTIONS)).toBe(false);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.VIEW_FINANCE)).toBe(true);
  });

  test('farm manager can sell birds but cannot invoice or pay', () => {
    expect(hasPermission(ROLES.FARM_MANAGER, PERMISSIONS.CREATE_CHICKEN_SALES)).toBe(true);
    expect(hasPermission(ROLES.FARM_MANAGER, PERMISSIONS.CONFIRM_CHICKEN_SALES)).toBe(true);
    expect(hasPermission(ROLES.FARM_MANAGER, PERMISSIONS.INVOICE_CHICKEN_SALES)).toBe(false);
    expect(hasPermission(ROLES.FARM_MANAGER, PERMISSIONS.PAY_CHICKEN_SALES)).toBe(false);
  });

  test('finance can invoice chicken sales but cannot confirm flock decrement', () => {
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.INVOICE_CHICKEN_SALES)).toBe(true);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.PAY_CHICKEN_SALES)).toBe(true);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.CONFIRM_CHICKEN_SALES)).toBe(false);
  });

  test('farm manager cannot reconcile finance', () => {
    expect(hasPermission(ROLES.FARM_MANAGER, PERMISSIONS.VIEW_FINANCE)).toBe(false);
    expect(hasPermission(ROLES.FARM_MANAGER, 'finance:reconcile')).toBe(false);
  });

  test('legacy roles normalize', () => {
    expect(normalizeRole('STORE_KEEPER')).toBe('INVENTORY_MANAGER');
    expect(normalizeRole('CRM_AGENT')).toBe('CRM_AGENT'); // unknown stays as-is; not a product role
    expect(normalizeRole('MARKETING')).toBe('MARKETING_MANAGER');
  });

  test('user.permissions preferred over role matrix', () => {
    const user = { role: ROLES.PHARMACIST, permissions: ['pharmacy:read'] };
    expect(hasPermissionForUser(user, 'pharmacy:read')).toBe(true);
    expect(hasPermissionForUser(user, 'pharmacy:sale')).toBe(false);
  });

  test('permission values use MODULE:ACTION', () => {
    expect(PERMISSIONS.STOCK_IN).toBe('inventory:stock_in');
    expect(PERMISSIONS.CREATE_PHARMACY_SALES).toBe('pharmacy:sale');
    expect(ROLE_PERMISSIONS[ROLES.ADMIN]).not.toContain('*');
  });
});
