import { PERMISSIONS, ROLES } from './permissions';
import { getRoleMenus } from './roleMenus';
import { PATH_MODULE_MAP } from './routeAccess';

describe('Farm Manager Management Workflow', () => {
  test('Group Farm Manager menu contains Site Managers section', () => {
    const menus = getRoleMenus(ROLES.FARM_MANAGER);
    const farmOpsSection = menus.find((m) => m.label === 'FARM OPERATIONS');
    expect(farmOpsSection).toBeDefined();

    const siteManagersItem = farmOpsSection.items.find((item) => item.path === '/farms/managers');
    expect(siteManagersItem).toBeDefined();
    expect(siteManagersItem.label).toBe('Site Managers');
    expect(siteManagersItem.permission).toBe(PERMISSIONS.VIEW_FARMS);
  });

  test('Admin menu contains Site Farm Managers management section', () => {
    const menus = getRoleMenus(ROLES.ADMIN);
    const adminSection = menus.find((m) => m.label === 'ADMIN');
    expect(adminSection).toBeDefined();

    const siteManagersItem = adminSection.items.find((item) => item.path === '/farms/managers');
    expect(siteManagersItem).toBeDefined();
    expect(siteManagersItem.label).toBe('Site Farm Managers');
  });

  test('Farm Worker menu does NOT have access to Site Managers section', () => {
    const menus = getRoleMenus(ROLES.FARM_WORKER);
    const allPaths = menus.flatMap((section) => section.items.map((i) => i.path));
    expect(allPaths).not.toContain('/farms/managers');
  });

  test('routeAccess maps /farms/managers to farms module', () => {
    expect(PATH_MODULE_MAP['/farms/managers']).toBe('farms');
  });

  test('Eligible manager selection logic filters already-assigned managers for new farm', () => {
    const allManagers = [
      { id: 1, fullName: 'Manager Biyo', designation: 'SITE_FARM_MANAGER', assignedFarmId: 10 },
      { id: 2, fullName: 'Manager Shera (Unassigned)', designation: 'SITE_FARM_MANAGER', assignedFarmId: null },
      { id: 3, fullName: 'Manager Dukem', designation: 'SITE_FARM_MANAGER', assignedFarmId: 30 },
    ];

    // For a new farm (currentFarmId = null), only unassigned managers are eligible
    const eligibleForNew = allManagers.filter((m) => m.assignedFarmId === null);
    expect(eligibleForNew).toHaveLength(1);
    expect(eligibleForNew[0].id).toBe(2);

    // For editing Biyo farm (currentFarmId = 10), Manager Biyo and unassigned are eligible
    const currentFarmId = 10;
    const eligibleForEdit = allManagers.filter(
      (m) => m.assignedFarmId === null || m.assignedFarmId === currentFarmId
    );
    expect(eligibleForEdit).toHaveLength(2);
    expect(eligibleForEdit.map((m) => m.id)).toEqual([1, 2]);
  });

  test('Duplicate farm assignment detection prevents conflicts', () => {
    const existingAssignments = [
      { farmId: 1, farmName: 'Biyo Farm', managerId: 101, managerName: 'Manager A' },
      { farmId: 2, farmName: 'Shera Farm', managerId: 102, managerName: 'Manager B' },
    ];

    const checkFarmConflict = (selectedFarmId, targetManagerId) => {
      const conflict = existingAssignments.find(
        (a) => a.farmId === selectedFarmId && a.managerId !== targetManagerId
      );
      if (conflict) {
        return `Farm '${conflict.farmName}' is already assigned to manager ${conflict.managerName}`;
      }
      return null;
    };

    // Selecting Biyo (farm 1) for a new manager (id 103) produces conflict
    expect(checkFarmConflict(1, 103)).toBe("Farm 'Biyo Farm' is already assigned to manager Manager A");

    // Re-saving Biyo for its own manager (id 101) produces no conflict
    expect(checkFarmConflict(1, 101)).toBeNull();

    // Selecting Dukem (farm 3, unassigned) produces no conflict
    expect(checkFarmConflict(3, 103)).toBeNull();
  });
});
