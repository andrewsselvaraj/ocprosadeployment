-- DEMO DATA for local runs and the "dev" environment only.
-- Production sets FLYWAY_LOCATIONS=classpath:db/migration so this file is never applied.
-- Passwords are "{noop}" (plain text) on purpose: the first login upgrades them to BCrypt,
-- which demonstrates the legacy-password migration path.
--
--   superadmin@m2its.local / Demo@123   (SUPERADMIN)
--   admin@demo.local       / Demo@123   (ADMIN of Demo School)
--   author@demo.local      / Demo@123   (DATA_ENTRY of Demo School: edits questions only)
--   student@demo.local     / Demo@123   (USER  of Demo School)

INSERT IGNORE INTO org_info (pk_org_id, org_name, org_email, org_password, org_updated_by, org_status)
VALUES ('demo-org', 'Demo School', 'office@demo.local', '{noop}Demo@123', 'seed', 'A');

INSERT IGNORE INTO admin_info (pk_admin_id, admin_name, admin_email, admin_password, admin_role, admin_status)
VALUES ('demo-superadmin', 'Platform Admin', 'superadmin@m2its.local', '{noop}Demo@123', 'Super Admin', 'A');

INSERT IGNORE INTO user_info (pk_user_id, user_org_id, user_name, user_email, user_password, user_role, user_updated_by, user_status)
VALUES ('demo-admin', 'demo-org', 'Demo Admin', 'admin@demo.local', '{noop}Demo@123', 'Admin', 'seed', 'A'),
       ('demo-author', 'demo-org', 'Demo Author', 'author@demo.local', '{noop}Demo@123', 'Data Entry Operator', 'seed', 'A'),
       ('demo-student', 'demo-org', 'Demo Student', 'student@demo.local', '{noop}Demo@123', 'User', 'seed', 'A');
