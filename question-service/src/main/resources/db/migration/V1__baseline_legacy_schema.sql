-- question-service schema (database: question_db)
-- Same tables/columns as the legacy "v_two" tables. FKs to org/class/subject were dropped
-- (those live in other services); the question -> answer FK stays inside this service.
-- Legacy tables answer_info_v_two_bk and question_info (v1) are not migrated.

CREATE TABLE question_info_v_two (
  pk_question_id      varchar(50)    NOT NULL,
  fk_org_id           varchar(50)    NOT NULL,
  fk_class_id         varchar(50)    NOT NULL,
  fk_subject_id       varchar(50)    NOT NULL,
  fk_chapter_id       varchar(50)    NOT NULL DEFAULT 'None',
  question_name       varchar(10000) NOT NULL,
  question_type       varchar(50)    NOT NULL,
  question_updated_by varchar(50)    NOT NULL,
  question_updated    timestamp(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  question_status     varchar(50)    NOT NULL DEFAULT 'A',
  PRIMARY KEY (pk_question_id)
);
CREATE INDEX ques_org ON question_info_v_two (fk_org_id);
CREATE INDEX ques_sub ON question_info_v_two (fk_subject_id);

CREATE TABLE answer_info_v_two (
  pk_answer_id             varchar(50)  NOT NULL,
  fk_org_id                varchar(50)  NOT NULL,
  fk_class_id              varchar(50)  NOT NULL,
  fk_subject_id            varchar(50)  NOT NULL,
  fk_chapter_id            varchar(50)  NOT NULL,
  fk_question_id           varchar(50)  NOT NULL,
  answers                  mediumtext   NOT NULL,
  correct_answer           varchar(500) NOT NULL,
  answer_updated_by        varchar(50)  NOT NULL,
  answer_updated_date_time timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  answer_status            varchar(50)  NOT NULL DEFAULT 'A',
  PRIMARY KEY (pk_answer_id),
  CONSTRAINT ans_ques FOREIGN KEY (fk_question_id) REFERENCES question_info_v_two (pk_question_id)
);
CREATE INDEX ans_ques_idx ON answer_info_v_two (fk_question_id);
