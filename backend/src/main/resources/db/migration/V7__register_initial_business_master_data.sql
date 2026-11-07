-- V7: Register Initial Business Master Data (Suppliers, Bank Accounts, HR Positions & Employee Directory)

-- ============================================================================
-- PART 1: HR POSITIONS
-- ============================================================================
INSERT INTO hr_positions (code, name, description, active, created_at, updated_at) VALUES
('POS_GM', 'General Manager', 'Overall strategic and operational executive leadership', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POS_MGR', 'Manager', 'Operational and administrative management', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POS_FM', 'Farm Manager', 'Site and flock production management', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POS_EXT_WORKER', 'Extension Worker', 'Technical field advice, farmer support, and outreach', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POS_SALES_TECH_ADV', 'Sales & Technical Adviser', 'Commercial sales and poultry technical advisory', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POS_SALES', 'Sales', 'Customer commercial sales and order fulfillment', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POS_SOCIAL_MEDIA', 'Social Media Manager', 'Digital media, marketing, and public relations', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POS_SALES_TRAIN_COORD', 'Sales / Training Coordinator', 'Sales coordination and farmer training programs', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POS_ACCOUNTANT', 'Accountant', 'Financial accounting, ledger reconciliation, and reporting', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POS_CHEM_SPRAY', 'Chemical Spray', 'Biosecurity, sanitation, and chemical spraying operations', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POS_LABORER', 'Laborer', 'Farm manual operations, feeding, and house maintenance', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POS_SEC_RECEPT', 'Secretary & Reception', 'Executive assistance, front desk reception, and communication', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POS_PROD_MGR', 'Production Manager', 'Farm production planning, feed logistics, and yield oversight', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POS_FARM_COORD', 'Farm Coordinator', 'Site coordination, daily recording, and flock supervision', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POS_PURCHASER', 'Purchaser', 'Material procurement, vendor sourcing, and purchasing', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================================
-- PART 2: SUPPLIER MASTER RECORDS (PART A)
-- ============================================================================
INSERT INTO suppliers (
    supplier_code, name, phone, category, currency, active, created_at, updated_at
) VALUES
('SUP-000001', 'Yilkal Adamu Import', '+251923167777', 'Medical supplies and vitamins.', 'ETB', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SUP-000002', 'Temeche Poultry', '+251930097666', 'Day-old chicks (የአንድ ቀን ጫጩት).', 'ETB', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SUP-000003', 'Agri Farm', '+251963656565', 'Agricultural imports and supplies.', 'ETB', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SUP-000004', 'Okay Agro Industry', '+251986000003', 'Animal feed.', 'ETB', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SUP-000005', 'Woljeji Agriculture Industry PLC', '+251932134874', 'Agricultural supplies. Specific products require confirmation.', 'ETB', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SUP-000006', 'Muday Mena Trading PLC', '+251911167766', 'Not yet confirmed.', 'ETB', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Keep sequence synchronized for subsequent manual/API additions
UPDATE proc_number_sequences SET next_value = 7 WHERE name = 'SUP';

-- ============================================================================
-- PART 3: COMPANY CBE BANK ACCOUNTS IN FINANCE LEDGER (PART B)
-- Registered as sub-ledger accounts under General Bank Account (1010)
-- ============================================================================
INSERT INTO chart_of_accounts (
    account_code, account_name, account_type, business_unit, balance, is_header, parent_id, description
) VALUES
('1010-01', 'CBE - Abunu Tekele and Their Friends Poultry Farm (1000799913376)', 'ASSET', 'GENERAL', 0.00, false,
 (SELECT id FROM chart_of_accounts WHERE account_code = '1010' LIMIT 1),
 'CBE Account: 1000799913376. Purpose: Receiving business payments. Verified opening statement pending.'),
('1010-02', 'CBE - Trust Afro Consulting PLC (1000451324814)', 'ASSET', 'GENERAL', 0.00, false,
 (SELECT id FROM chart_of_accounts WHERE account_code = '1010' LIMIT 1),
 'CBE Account: 1000451324814. Purpose: Receiving business payments. Verified opening statement pending.');

-- ============================================================================
-- PART 4: HR EMPLOYEE DIRECTORY REGISTRATION (25 EMPLOYEES) (PART C)
-- All employees registered with user_id = NULL (no login accounts auto-provisioned)
-- ============================================================================
INSERT INTO hr_employees (
    employee_number, first_name, last_name, phone, position_id,
    basic_salary, currency, employment_status, employment_type, notes,
    created_at, updated_at
) VALUES
('EMP-000001', 'Dr. Tekle', 'Shiferaw', '+251919076607',
 (SELECT id FROM hr_positions WHERE code = 'POS_GM'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: General Manager. Preliminary role mapping: GENERAL_MANAGER.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000002', 'Dr. Abunu', 'Andarge', '+251919074407',
 (SELECT id FROM hr_positions WHERE code = 'POS_MGR'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Manager. Preliminary role mapping: OPERATIONS_MANAGER.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000003', 'Dr. Kemil', 'Hayru', '+251937612222',
 (SELECT id FROM hr_positions WHERE code = 'POS_FM'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Farm Manager. Preliminary role mapping: FARM_MANAGER.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000004', 'Dr. Betelhem', 'Jima', '+251960785341',
 (SELECT id FROM hr_positions WHERE code = 'POS_EXT_WORKER'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Extension Worker. Preliminary role mapping: EXTENSION_WORKER.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000005', 'Dr. Mareag', 'Amare', '+251932732222',
 (SELECT id FROM hr_positions WHERE code = 'POS_SALES_TECH_ADV'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Sales & Technical Adviser. System access pending CEO approval.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000006', 'Dr. Yordanos', 'Fikadu', '+251937932633',
 (SELECT id FROM hr_positions WHERE code = 'POS_SALES'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Sales. System access pending CEO approval.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000007', 'Daniel', 'Kibre', '+251939572222',
 (SELECT id FROM hr_positions WHERE code = 'POS_FM'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Farm Manager. Preliminary role mapping: FARM_MANAGER.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000008', 'Habtu', 'Kalayu (NA)', '+251942724867',
 (SELECT id FROM hr_positions WHERE code = 'POS_SOCIAL_MEDIA'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Social Media Manager. System access pending CEO approval.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000009', 'Meron', 'Abebe', '+251919074475',
 (SELECT id FROM hr_positions WHERE code = 'POS_SALES'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Sales. System access pending CEO approval.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000010', 'Ms. Selamawit', 'Mebratu', '+251932042222',
 (SELECT id FROM hr_positions WHERE code = 'POS_SALES_TRAIN_COORD'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Sales / Training Coordinator. System access pending CEO approval.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000011', 'Meaza', 'Solomon', '+251946303182',
 (SELECT id FROM hr_positions WHERE code = 'POS_ACCOUNTANT'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Accountant. Preliminary role mapping: FINANCE.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000012', 'Habtamu', 'Yirdaw', '+251986238882',
 (SELECT id FROM hr_positions WHERE code = 'POS_CHEM_SPRAY'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Chemical Spray. System access pending CEO approval.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000013', 'Mesganaw', 'Assefa', '+251970636713',
 (SELECT id FROM hr_positions WHERE code = 'POS_LABORER'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Laborer. Operational personnel - no ERP account.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000014', 'Dereje', 'Tesfaye', '+251953465440',
 (SELECT id FROM hr_positions WHERE code = 'POS_LABORER'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Laborer. Operational personnel - no ERP account.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000015', 'Ms. Efad', 'Estifanos', '+251927440559',
 (SELECT id FROM hr_positions WHERE code = 'POS_SEC_RECEPT'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Secretary & Reception. Preliminary role mapping: SECRETARY.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000016', 'Dr. Amanuel', '[Surname Not Provided]', '+251985892431',
 (SELECT id FROM hr_positions WHERE code = 'POS_PROD_MGR'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Production Manager. Note: Single name provided. System access pending CEO approval.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000017', 'Melaku', 'Getaneh', '+251948851793',
 (SELECT id FROM hr_positions WHERE code = 'POS_FARM_COORD'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Farm Coordinator. System access pending CEO approval.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000018', 'Yared', 'Tesfaye', '+251942887770',
 (SELECT id FROM hr_positions WHERE code = 'POS_FARM_COORD'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Farm Coordinator. System access pending CEO approval.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000019', 'Abreham', 'Tazeb', NULL,
 (SELECT id FROM hr_positions WHERE code = 'POS_FARM_COORD'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Farm Coordinator. Note: Phone number not provided. System access pending CEO approval.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000020', 'Kasahun', 'Abera', '+251972780926',
 (SELECT id FROM hr_positions WHERE code = 'POS_PURCHASER'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Purchaser. System access pending CEO approval.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000021', 'Chanaybelu', 'Terefe', '+251923159365',
 (SELECT id FROM hr_positions WHERE code = 'POS_FARM_COORD'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Farm Coordinator. System access pending CEO approval.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000022', 'Meseret', 'Alehgn', '+251975119605',
 (SELECT id FROM hr_positions WHERE code = 'POS_FARM_COORD'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Farm Coordinator. System access pending CEO approval.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000023', 'Dr. Tarekegn', '[Surname Not Provided]', '+251937752222',
 (SELECT id FROM hr_positions WHERE code = 'POS_EXT_WORKER'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Extension Worker. Note: Single name provided. Preliminary role mapping: EXTENSION_WORKER.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000024', 'Dawit', 'Aychew', '+251911674364',
 (SELECT id FROM hr_positions WHERE code = 'POS_ACCOUNTANT'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Accountant. System access pending CEO approval.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('EMP-000025', 'Nigatu', '[Surname Unclear]', '+251957853975',
 (SELECT id FROM hr_positions WHERE code = 'POS_FARM_COORD'), 0.00, 'ETB', 'ACTIVE', 'FULL_TIME',
 'Position: Farm Coordinator. Note: Surname unclear. System access pending CEO approval.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Keep sequence synchronized for subsequent employee creation
UPDATE hr_number_sequences SET next_value = 26 WHERE name = 'EMPLOYEE';
