-- Legacy passwords were stored in plain text in varchar(50) columns.
-- 1) Widen the columns so they can hold "{bcrypt}$2a$10$..." hashes (68 chars).
-- 2) Tag any existing plain-text value with "{noop}" so Spring Security's
--    DelegatingPasswordEncoder can verify it; AuthService re-hashes it with BCrypt on the
--    next successful login.
ALTER TABLE org_info   MODIFY org_password   varchar(255) NOT NULL;
ALTER TABLE user_info  MODIFY user_password  varchar(255) NOT NULL;
ALTER TABLE admin_info MODIFY admin_password varchar(255) NOT NULL;

UPDATE org_info   SET org_password   = CONCAT('{noop}', org_password)   WHERE org_password   NOT LIKE '{%';
UPDATE user_info  SET user_password  = CONCAT('{noop}', user_password)  WHERE user_password  NOT LIKE '{%';
UPDATE admin_info SET admin_password = CONCAT('{noop}', admin_password) WHERE admin_password NOT LIKE '{%';
