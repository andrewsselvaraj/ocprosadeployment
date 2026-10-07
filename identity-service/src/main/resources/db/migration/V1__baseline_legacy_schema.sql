-- identity-service schema (database: identity_db)
-- Tables and columns are kept identical to the legacy m2its dump so existing data can be
-- copied over unchanged (see deploy/db/legacy-import.sql). Written in portable SQL so the
-- same script runs on MySQL 8 and on H2 (MODE=MySQL) for local runs and tests.

CREATE TABLE org_info (
  pk_org_id      varchar(50)  NOT NULL,
  org_name       varchar(50)  NOT NULL,
  org_email      varchar(50)  NOT NULL,
  org_password   varchar(50)  NOT NULL,
  org_updated_by varchar(50)  NOT NULL,
  org_updated_on timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  org_status     varchar(50)  NOT NULL DEFAULT 'A',
  PRIMARY KEY (pk_org_id)
);

CREATE TABLE user_info (
  pk_user_id             varchar(50)  NOT NULL,
  user_org_id            varchar(50)  NOT NULL,
  user_name              varchar(50)  NOT NULL,
  user_email             varchar(50)  NOT NULL,
  user_password          varchar(50)  NOT NULL,
  user_role              varchar(50)  NOT NULL,
  user_updated_by        varchar(50)  NOT NULL,
  user_updated_date_time timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  user_status            varchar(50)  NOT NULL DEFAULT 'A',
  PRIMARY KEY (pk_user_id)
);
-- No FK to org_info: the legacy schema had none and legacy rows may reference missing orgs.
-- UserService validates the organisation for new users instead.
CREATE INDEX idx_user_email ON user_info (user_email);
CREATE INDEX idx_user_org ON user_info (user_org_id);

CREATE TABLE admin_info (
  pk_admin_id             varchar(50)  NOT NULL,
  admin_name              varchar(50)  NOT NULL,
  admin_email             varchar(50)  NOT NULL,
  admin_password          varchar(50)  NOT NULL,
  admin_role              varchar(50)  NOT NULL,
  admin_updated_date_time timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  admin_status            varchar(50)  NOT NULL DEFAULT 'A',
  PRIMARY KEY (pk_admin_id)
);
CREATE INDEX idx_admin_email ON admin_info (admin_email);

CREATE TABLE contact_m2i (
  m_contact_id                varchar(50)  NOT NULL,
  m_name                      varchar(50)  NOT NULL,
  m_email                     varchar(50)  NOT NULL,
  m_subject                   varchar(50)  NOT NULL,
  m_message                   varchar(500) NOT NULL,
  m_message_updated_date_time timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  m_message_status            varchar(50)  NOT NULL DEFAULT 'A',
  PRIMARY KEY (m_contact_id)
);
