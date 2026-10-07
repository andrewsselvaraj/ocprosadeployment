-- academic-service schema (database: academic_db)
-- Same tables/columns as the legacy dump. Foreign keys that pointed at org_info were dropped
-- because organisations now live in identity-service's database (database-per-service).
-- fk_org_id stays as a plain column and is enforced by the application from the JWT.

CREATE TABLE class_info (
  pk_class_id   varchar(50)  NOT NULL,
  fk_org_id     varchar(50)  NOT NULL,
  class_name    varchar(50)  NOT NULL,
  class_updated timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  class_status  varchar(50)  NOT NULL DEFAULT 'A',
  PRIMARY KEY (pk_class_id)
);
CREATE INDEX class_org ON class_info (fk_org_id);

CREATE TABLE subject_info (
  pk_subject_id   varchar(50)  NOT NULL,
  fk_org_id       varchar(50)  NOT NULL,
  fk_class_id     varchar(50)  NOT NULL,
  subject_name    varchar(50)  NOT NULL,
  subject_updated timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  subject_status  varchar(50)  NOT NULL DEFAULT 'A',
  PRIMARY KEY (pk_subject_id),
  CONSTRAINT sub_class FOREIGN KEY (fk_class_id) REFERENCES class_info (pk_class_id)
);
CREATE INDEX sub_org ON subject_info (fk_org_id);

CREATE TABLE chapter_info (
  pk_chapter_id   varchar(50)  NOT NULL,
  fk_org_id       varchar(50)  NOT NULL,
  fk_class_id     varchar(50)  NOT NULL,
  fk_subject_id   varchar(50)  NOT NULL,
  chapter_name    varchar(50)  NOT NULL,
  chapter_updated timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  chapter_status  varchar(50)  NOT NULL DEFAULT 'A',
  PRIMARY KEY (pk_chapter_id),
  -- New compared to the legacy schema (which had no FK here); the import script skips orphans.
  CONSTRAINT chap_subject FOREIGN KEY (fk_subject_id) REFERENCES subject_info (pk_subject_id)
);
