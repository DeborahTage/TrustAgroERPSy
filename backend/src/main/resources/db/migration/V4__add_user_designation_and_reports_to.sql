-- V4: Add organizational designation and reports_to reporting relationship on users table
ALTER TABLE users ADD COLUMN designation VARCHAR(50);
ALTER TABLE users ADD CONSTRAINT chk_users_designation 
    CHECK (designation IS NULL OR designation IN ('GROUP_FARM_MANAGER', 'SITE_FARM_MANAGER'));
ALTER TABLE users ADD COLUMN reports_to_id BIGINT;
ALTER TABLE users ADD CONSTRAINT fk_users_reports_to 
    FOREIGN KEY (reports_to_id) REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX idx_users_reports_to ON users(reports_to_id);
