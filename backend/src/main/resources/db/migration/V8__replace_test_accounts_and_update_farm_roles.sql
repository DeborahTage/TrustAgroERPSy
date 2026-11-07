-- V8: Replace Test Accounts with Approved Employee Accounts and Update Farm Management Roles

-- ============================================================================
-- PART 1: ADD must_change_password COLUMN TO users TABLE
-- ============================================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT false;

-- ============================================================================
-- PART 2: DEACTIVATE OBSOLETE TEST ACCOUNTS (PRESERVE AUDIT & TRANSACTION HISTORY)
-- ============================================================================
UPDATE users 
SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP
WHERE email IN (
    'gm@staging.trustagro.internal',
    'finance@staging.trustagro.internal',
    'abu@trustagro.com',
    'farmmanager@trust.com',
    'group.manager@trustagro.internal',
    'solomon.biyo@trustagro.internal',
    'abebe@trustagro.com',
    'mekdes@trust.com',
    'tariku.bekele@trustagro.internal',
    'crmmanager@trust.com',
    'hrmanager@trust.com'
);

-- ============================================================================
-- PART 3: PROVISION APPROVED EMPLOYEE LOGIN ACCOUNTS (9 EXPLICITLY APPROVED ROLES)
-- ============================================================================
-- 1. Dr. Tekle Shiferaw (General Manager)
INSERT INTO users (full_name, email, phone, password, role, status, token_version, must_change_password, created_at, updated_at)
SELECT 'Dr. Tekle Shiferaw', 'tekle@trust.com', '+251919076607',
       '$2a$10$XUSSuL82AWoo0tmnRa.kiO04f5WkS6xsMiL1Zw/5YqrOzQ7VbluzO',
       'GENERAL_MANAGER', 'ACTIVE', 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'tekle@trust.com');

-- 2. Dr. Abunu Andarge (Operations Manager - Supervises all 4 farm sites)
INSERT INTO users (full_name, email, phone, password, role, designation, reports_to_id, status, token_version, must_change_password, created_at, updated_at)
SELECT 'Dr. Abunu Andarge', 'abunu@trust.com', '+251919074407',
       '$2a$10$IQ5o5gGIcTH.q4hTfB8D2.g3HwxuKoObwBy.8wQCeBoRQ9j6q4Z/.',
       'OPERATIONS_MANAGER', 'GROUP_FARM_MANAGER',
       (SELECT id FROM users WHERE email = 'tekle@trust.com'),
       'ACTIVE', 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'abunu@trust.com');

-- 3. Dr. Kemil Hayru (Site Farm Manager - Biyo Farm)
INSERT INTO users (full_name, email, phone, password, role, designation, reports_to_id, status, token_version, must_change_password, created_at, updated_at)
SELECT 'Dr. Kemil Hayru', 'kemil@trust.com', '+251937612222',
       '$2a$10$WwUvgYcGi.FYd9uuO9cjV.7ta6iWQ8mq8RpegXZUIpJuMlO9u.Moe',
       'FARM_MANAGER', 'SITE_FARM_MANAGER',
       (SELECT id FROM users WHERE email = 'abunu@trust.com'),
       'ACTIVE', 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'kemil@trust.com');

-- 4. Daniel Kibre (Site Farm Manager - Dukem Experimental Farm)
INSERT INTO users (full_name, email, phone, password, role, designation, reports_to_id, status, token_version, must_change_password, created_at, updated_at)
SELECT 'Daniel Kibre', 'daniel@trust.com', '+251939572222',
       '$2a$10$jR10X8oo8ytWa.1KSM864.7aY0G7xjziQ6eltfcghWLqYetLYlty6',
       'FARM_MANAGER', 'SITE_FARM_MANAGER',
       (SELECT id FROM users WHERE email = 'abunu@trust.com'),
       'ACTIVE', 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'daniel@trust.com');

-- 5. Meaza Solomon (Accountant - Finance)
INSERT INTO users (full_name, email, phone, password, role, status, token_version, must_change_password, created_at, updated_at)
SELECT 'Meaza Solomon', 'meaza@trust.com', '+251946303182',
       '$2a$10$XPxjRih0cdShN2BaZk8K1OugFlqsGCEgxXHUrrl1TlNuVB2AB6FQC',
       'FINANCE', 'ACTIVE', 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'meaza@trust.com');

-- 6. Ms. Selamawit Mebratu (Sales / Training Coordinator)
INSERT INTO users (full_name, email, phone, password, role, status, token_version, must_change_password, created_at, updated_at)
SELECT 'Ms. Selamawit Mebratu', 'selamawit@trust.com', '+251932042222',
       '$2a$10$.ZtyDGS1B9S9hhq2.OKBYeoKkcC5C0IVNZw.XWRF/0RbJBf1Hv5E2',
       'TRAINING_COORDINATOR', 'ACTIVE', 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'selamawit@trust.com');

-- 7. Ms. Efad Estifanos (Secretary & Reception)
INSERT INTO users (full_name, email, phone, password, role, status, token_version, must_change_password, created_at, updated_at)
SELECT 'Ms. Efad Estifanos', 'efad@trust.com', '+251927440559',
       '$2a$10$eD6cOXnSIBPed5TLTQcTI.LKQ4RoS4PMmYl0uTT8rUC2AbSRdiVrC',
       'SECRETARY', 'ACTIVE', 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'efad@trust.com');

-- 8. Dr. Betelhem Jima (Extension Worker)
INSERT INTO users (full_name, email, phone, password, role, status, token_version, must_change_password, created_at, updated_at)
SELECT 'Dr. Betelhem Jima', 'betelhem@trust.com', '+251960785341',
       '$2a$10$kqXQgN/syMJ3kb5fpNZcv.sGxtwgl72O0o2miuZlZUGACaMedVSuO',
       'EXTENSION_WORKER', 'ACTIVE', 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'betelhem@trust.com');

-- 9. Meron Abebe (Sales - CRM Manager)
INSERT INTO users (full_name, email, phone, password, role, status, token_version, must_change_password, created_at, updated_at)
SELECT 'Meron Abebe', 'meron@trust.com', '+251919074475',
       '$2a$10$OkuhCQtw/H.zkEqHmIxn9eZKv9Uho0YZ8Ciz7s64nzmRVb35whWJ6',
       'CRM_MANAGER', 'ACTIVE', 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'meron@trust.com');

-- ============================================================================
-- PART 4: LINK HR EMPLOYEE DIRECTORY TO APPROVED LOGIN ACCOUNTS
-- ============================================================================
UPDATE hr_employees SET user_id = (SELECT id FROM users WHERE email = 'tekle@trust.com') WHERE employee_number = 'EMP-000001';
UPDATE hr_employees SET user_id = (SELECT id FROM users WHERE email = 'abunu@trust.com') WHERE employee_number = 'EMP-000002';
UPDATE hr_employees SET user_id = (SELECT id FROM users WHERE email = 'kemil@trust.com') WHERE employee_number = 'EMP-000003';
UPDATE hr_employees SET user_id = (SELECT id FROM users WHERE email = 'betelhem@trust.com') WHERE employee_number = 'EMP-000004';
UPDATE hr_employees SET user_id = (SELECT id FROM users WHERE email = 'daniel@trust.com') WHERE employee_number = 'EMP-000007';
UPDATE hr_employees SET user_id = (SELECT id FROM users WHERE email = 'meron@trust.com') WHERE employee_number = 'EMP-000009';
UPDATE hr_employees SET user_id = (SELECT id FROM users WHERE email = 'selamawit@trust.com') WHERE employee_number = 'EMP-000010';
UPDATE hr_employees SET user_id = (SELECT id FROM users WHERE email = 'meaza@trust.com') WHERE employee_number = 'EMP-000011';
UPDATE hr_employees SET user_id = (SELECT id FROM users WHERE email = 'efad@trust.com') WHERE employee_number = 'EMP-000015';

-- ============================================================================
-- PART 5: CONFIGURE 4 COMPANY FARMS AND FARM MANAGER ASSIGNMENTS
-- ============================================================================
-- Ensure Baugaya Farm exists
INSERT INTO farms (farm_name, location, farm_type, capacity, status, created_at, updated_at)
SELECT 'Baugaya Farm', 'Baugaya Site Plot D', 'LAYER', 15000, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM farms WHERE farm_name = 'Baugaya Farm');

-- Deactivate Staging Container Farm Alpha
UPDATE farms SET status = 'INACTIVE', assigned_farm_manager_id = NULL WHERE farm_name ILIKE '%Staging%';

-- Assign Site Farm Managers:
-- Kemil Hayru -> Biyo Farm
UPDATE farms 
SET assigned_farm_manager_id = (SELECT id FROM users WHERE email = 'kemil@trust.com'),
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP
WHERE farm_name ILIKE '%Biyo%';

-- Daniel Kibre -> Dukem Experimental Farm
UPDATE farms 
SET assigned_farm_manager_id = (SELECT id FROM users WHERE email = 'daniel@trust.com'),
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP
WHERE farm_name ILIKE '%Dukem%';

-- Unassign test managers from other farms
UPDATE farms 
SET assigned_farm_manager_id = NULL 
WHERE farm_name NOT ILIKE '%Biyo%' AND farm_name NOT ILIKE '%Dukem%';

-- Populate user_farm_access for site farm managers
DELETE FROM user_farm_access 
WHERE user_id IN (SELECT id FROM users WHERE email IN ('kemil@trust.com', 'daniel@trust.com'));

INSERT INTO user_farm_access (user_id, farm_id)
SELECT u.id, f.id
FROM users u
JOIN farms f ON f.farm_name ILIKE '%Biyo%'
WHERE u.email = 'kemil@trust.com';

INSERT INTO user_farm_access (user_id, farm_id)
SELECT u.id, f.id
FROM users u
JOIN farms f ON f.farm_name ILIKE '%Dukem%'
WHERE u.email = 'daniel@trust.com';
