-- exam-service schema (database: exam_db)
-- One row per answered question; rows of one sitting share exam_session_id.
-- All legacy FKs pointed at tables now owned by other services, so they were dropped;
-- the ids are validated through question-service when the exam is submitted.

CREATE TABLE exam_info (
  pk_exam_id                 varchar(50)  NOT NULL,
  exam_session_id            varchar(50)  NOT NULL,
  fk_org_id                  varchar(50)  NOT NULL,
  fk_class_id                varchar(50)  NOT NULL,
  fk_subject_id              varchar(50)  NOT NULL,
  fk_question_id             varchar(50)  NOT NULL,
  fk_user_selected_answer_id varchar(50)  NOT NULL,
  exam_taken_by              varchar(50)  NOT NULL,
  exam_taken_date_time       timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  exam_status                varchar(50)  NOT NULL DEFAULT 'A',
  PRIMARY KEY (pk_exam_id)
);
CREATE INDEX ex_session ON exam_info (exam_session_id);
CREATE INDEX ex_org_taker ON exam_info (fk_org_id, exam_taken_by);
