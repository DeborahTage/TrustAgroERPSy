import {
  PERMISSIONS,
  ROLES,
  hasPermission,
  hasPermissionForUser,
  normalizeRole,
  ROLE_PERMISSIONS,
} from './permissions';
import { getRoleMenus } from './roleMenus';

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

  test('consultant has activity and deliverable permissions', () => {
    expect(hasPermission(ROLES.CONSULTANT, PERMISSIONS.VIEW_CONSULTANCY)).toBe(true);
    expect(hasPermission(ROLES.CONSULTANT, PERMISSIONS.CONSULTANCY_ACTIVITY)).toBe(true);
    expect(hasPermission(ROLES.CONSULTANT, PERMISSIONS.CONSULTANCY_DELIVERABLE)).toBe(true);
    expect(hasPermission(ROLES.CONSULTANT, PERMISSIONS.APPROVE_CONSULTANCY)).toBe(false);
  });

  test('finance has consultancy invoice and payment', () => {
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.VIEW_CONSULTANCY)).toBe(true);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.CONSULTANCY_INVOICE)).toBe(true);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.CONSULTANCY_PAYMENT)).toBe(true);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.CONSULTANCY_REPORT)).toBe(true);
  });

  test('crm manager can create and assign consultancy', () => {
    expect(hasPermission(ROLES.CRM_MANAGER, PERMISSIONS.MANAGE_CONSULTANCY)).toBe(true);
    expect(hasPermission(ROLES.CRM_MANAGER, PERMISSIONS.ASSIGN_CONSULTANCY)).toBe(true);
  });

  test('crm manager has lead_convert and quotation', () => {
    expect(hasPermission(ROLES.CRM_MANAGER, PERMISSIONS.CRM_LEAD_CONVERT)).toBe(true);
    expect(hasPermission(ROLES.CRM_MANAGER, PERMISSIONS.CRM_QUOTATION)).toBe(true);
    expect(hasPermission(ROLES.CRM_MANAGER, PERMISSIONS.CRM_QUOTATION_APPROVE)).toBe(true);
    expect(hasPermission(ROLES.CRM_MANAGER, PERMISSIONS.CRM_ORDER_CONFIRM)).toBe(true);
  });

  test('finance has order invoice/pay but not lead_convert', () => {
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.VIEW_CRM)).toBe(true);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.CRM_ORDER_INVOICE)).toBe(true);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.CRM_ORDER_PAY)).toBe(true);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.CRM_REPORT)).toBe(true);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.CRM_LEAD_CONVERT)).toBe(false);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.CRM_ORDER_CONFIRM)).toBe(false);
  });

  test('farm manager has no CRM commercial write', () => {
    expect(hasPermission(ROLES.FARM_MANAGER, PERMISSIONS.VIEW_CRM)).toBe(false);
    expect(hasPermission(ROLES.FARM_MANAGER, PERMISSIONS.CRM_LEAD)).toBe(false);
    expect(hasPermission(ROLES.FARM_MANAGER, PERMISSIONS.CRM_QUOTATION)).toBe(false);
    expect(hasPermission(ROLES.FARM_MANAGER, PERMISSIONS.CRM_ORDER)).toBe(false);
    expect(hasPermission(ROLES.FARM_MANAGER, PERMISSIONS.CRM_LEAD_CONVERT)).toBe(false);
  });

  test('extension worker has lead and task but not convert or invoice', () => {
    expect(hasPermission(ROLES.EXTENSION_WORKER, PERMISSIONS.CRM_LEAD)).toBe(true);
    expect(hasPermission(ROLES.EXTENSION_WORKER, PERMISSIONS.CRM_TASK)).toBe(true);
    expect(hasPermission(ROLES.EXTENSION_WORKER, PERMISSIONS.CRM_LEAD_CONVERT)).toBe(false);
    expect(hasPermission(ROLES.EXTENSION_WORKER, PERMISSIONS.CRM_ORDER_INVOICE)).toBe(false);
    expect(hasPermission(ROLES.EXTENSION_WORKER, PERMISSIONS.CRM_QUOTATION_APPROVE)).toBe(false);
  });

  test('general manager has CRM report and quotation approve but not order pay', () => {
    expect(hasPermission(ROLES.GENERAL_MANAGER, PERMISSIONS.CRM_REPORT)).toBe(true);
    expect(hasPermission(ROLES.GENERAL_MANAGER, PERMISSIONS.CRM_QUOTATION_APPROVE)).toBe(true);
    expect(hasPermission(ROLES.GENERAL_MANAGER, PERMISSIONS.CRM_ORDER_PAY)).toBe(false);
  });

  test('hr has payroll and leave approve', () => {
    expect(hasPermission(ROLES.HR, PERMISSIONS.VIEW_HR)).toBe(true);
    expect(hasPermission(ROLES.HR, PERMISSIONS.HR_PAYROLL)).toBe(true);
    expect(hasPermission(ROLES.HR, PERMISSIONS.HR_PAYROLL_APPROVE)).toBe(true);
    expect(hasPermission(ROLES.HR, PERMISSIONS.HR_PAYROLL_PROCESS)).toBe(true);
    expect(hasPermission(ROLES.HR, PERMISSIONS.HR_LEAVE)).toBe(true);
    expect(hasPermission(ROLES.HR, PERMISSIONS.HR_LEAVE_APPROVE)).toBe(true);
    expect(hasPermission(ROLES.HR, PERMISSIONS.HR_ATTENDANCE)).toBe(true);
    expect(hasPermission(ROLES.HR, PERMISSIONS.TERMINATE_HR)).toBe(true);
  });

  test('finance has payroll process but not terminate', () => {
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.VIEW_HR)).toBe(true);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.HR_PAYROLL)).toBe(true);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.HR_PAYROLL_PROCESS)).toBe(true);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.HR_REPORT)).toBe(true);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.TERMINATE_HR)).toBe(false);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.CREATE_HR)).toBe(false);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.HR_ATTENDANCE)).toBe(false);
  });

  test('general manager approves leave and payroll without editing employees', () => {
    expect(hasPermission(ROLES.GENERAL_MANAGER, PERMISSIONS.HR_LEAVE_APPROVE)).toBe(true);
    expect(hasPermission(ROLES.GENERAL_MANAGER, PERMISSIONS.HR_PAYROLL_APPROVE)).toBe(true);
    expect(hasPermission(ROLES.GENERAL_MANAGER, PERMISSIONS.HR_REPORT)).toBe(true);
    expect(hasPermission(ROLES.GENERAL_MANAGER, PERMISSIONS.EDIT_HR)).toBe(false);
  });

  test('consultant has no hr:read', () => {
    expect(hasPermission(ROLES.CONSULTANT, PERMISSIONS.VIEW_HR)).toBe(false);
    expect(hasPermission(ROLES.CONSULTANT, PERMISSIONS.HR_PAYROLL)).toBe(false);
    expect(hasPermission(ROLES.CONSULTANT, PERMISSIONS.HR_SELF)).toBe(true);
  });

  test('pharmacist has no hr:read', () => {
    expect(hasPermission(ROLES.PHARMACIST, PERMISSIONS.VIEW_HR)).toBe(false);
    expect(hasPermission(ROLES.PHARMACIST, PERMISSIONS.HR_ATTENDANCE)).toBe(false);
    expect(hasPermission(ROLES.PHARMACIST, PERMISSIONS.HR_SELF)).toBe(true);
  });

  test('every role can read its own hr record', () => {
    Object.keys(ROLE_PERMISSIONS).forEach((role) => {
      expect(hasPermission(role, PERMISSIONS.HR_SELF)).toBe(true);
    });
  });

  test('inventory manager can receive procurement but not payment', () => {
    expect(hasPermission(ROLES.INVENTORY_MANAGER, PERMISSIONS.VIEW_PROCUREMENT)).toBe(true);
    expect(hasPermission(ROLES.INVENTORY_MANAGER, PERMISSIONS.RECEIVE_PROCUREMENT)).toBe(true);
    expect(hasPermission(ROLES.INVENTORY_MANAGER, PERMISSIONS.APPROVE_PROCUREMENT)).toBe(true);
    expect(hasPermission(ROLES.INVENTORY_MANAGER, PERMISSIONS.PROCUREMENT_PAYMENT)).toBe(false);
    expect(hasPermission(ROLES.INVENTORY_MANAGER, PERMISSIONS.PROCUREMENT_INVOICE)).toBe(false);
  });

  test('finance can pay procurement but not receive', () => {
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.VIEW_PROCUREMENT)).toBe(true);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.PROCUREMENT_PAYMENT)).toBe(true);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.PROCUREMENT_INVOICE)).toBe(true);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.RECEIVE_PROCUREMENT)).toBe(false);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.CREATE_PROCUREMENT)).toBe(false);
  });

  test('farm manager can create procurement requisitions', () => {
    expect(hasPermission(ROLES.FARM_MANAGER, PERMISSIONS.VIEW_PROCUREMENT)).toBe(true);
    expect(hasPermission(ROLES.FARM_MANAGER, PERMISSIONS.CREATE_PROCUREMENT)).toBe(true);
    expect(hasPermission(ROLES.FARM_MANAGER, PERMISSIONS.APPROVE_PROCUREMENT)).toBe(false);
    expect(hasPermission(ROLES.FARM_MANAGER, PERMISSIONS.RECEIVE_PROCUREMENT)).toBe(false);
  });

  test('executive rbac boundaries', () => {
    expect(hasPermission(ROLES.GENERAL_MANAGER, PERMISSIONS.EXECUTIVE_FINANCE)).toBe(true);
    expect(hasPermission(ROLES.GENERAL_MANAGER, PERMISSIONS.EXECUTIVE_HR)).toBe(true);
    expect(hasPermission(ROLES.OPERATIONS_MANAGER, PERMISSIONS.EXECUTIVE_DASHBOARD)).toBe(true);
    expect(hasPermission(ROLES.OPERATIONS_MANAGER, PERMISSIONS.EXECUTIVE_FINANCE)).toBe(false);
    expect(hasPermission(ROLES.OPERATIONS_MANAGER, PERMISSIONS.EXECUTIVE_HR)).toBe(false);
    expect(hasPermission(ROLES.CRM_MANAGER, PERMISSIONS.EXECUTIVE_SALES)).toBe(true);
    expect(hasPermission(ROLES.CRM_MANAGER, PERMISSIONS.EXECUTIVE_HR)).toBe(false);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.EXECUTIVE_FINANCE)).toBe(true);
    expect(hasPermission(ROLES.FINANCE, PERMISSIONS.EXECUTIVE_HR)).toBe(false);
  });

  test('role menus: secretary and farm worker have module nav; extension is not full CRM', () => {
    const paths = (role) => getRoleMenus(role).flatMap((s) => s.items.map((i) => i.path));
    expect(paths(ROLES.SECRETARY)).toEqual(expect.arrayContaining(['/crm', '/farms', '/training']));
    expect(paths(ROLES.FARM_WORKER)).toEqual(expect.arrayContaining(['/farms', '/flocks', '/daily-records']));
    expect(paths(ROLES.EXTENSION_WORKER)).toEqual(expect.arrayContaining(['/crm/leads', '/crm/tasks']));
    expect(paths(ROLES.EXTENSION_WORKER)).not.toEqual(expect.arrayContaining(['/crm/quotations', '/crm/orders']));
    expect(paths(ROLES.CRM_MANAGER)).toEqual(expect.arrayContaining(['/crm/quotations', '/crm/orders']));
  });
});
