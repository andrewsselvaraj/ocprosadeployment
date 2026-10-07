-- Copies the legacy m2its data into the four microservice databases.
--
-- Prerequisites (see docs/08-legacy-data-migration.md):
--   1. The services have started once, so Flyway has created the tables.
--   2. The legacy dump was loaded into a staging database:
--        CREATE DATABASE m2its_legacy;
--        mysql -u root -p m2its_legacy < m2itssql_complete.sql
--   3. Run this file as a user that can read m2its_legacy and write all four databases (root/admin).
--
-- Re-runnable: INSERT IGNORE skips rows whose primary key already exists.
-- Not migrated: answer_info_v_two_bk (backup copy), question_info (v1, replaced by v_two),
--               main_table, dynamic_table, test_table_one (test tables), role_master (empty).

-- ---------------------------------------------------------------- identity-service
-- Plain-text passwords get the "{noop}" prefix; users are re-hashed to BCrypt at next login.
INSERT IGNORE INTO identity_db.org_info
       (pk_org_id, org_name, org_email, org_password, org_updated_by, org_updated_on, org_status)
SELECT  pk_org_id, org_name, org_email, CONCAT('{noop}', org_password), org_updated_by, org_updated_on, org_status
FROM m2its_legacy.org_info;

INSERT IGNORE INTO identity_db.user_info
       (pk_user_id, user_org_id, user_name, user_email, user_password, user_role, user_updated_by,
        user_updated_date_time, user_status)
SELECT  pk_user_id, user_org_id, user_name, user_email, CONCAT('{noop}', user_password), user_role, user_updated_by,
        user_updated_date_time, user_status
FROM m2its_legacy.user_info;

INSERT IGNORE INTO identity_db.admin_info
       (pk_admin_id, admin_name, admin_email, admin_password, admin_role, admin_updated_date_time, admin_status)
SELECT  pk_admin_id, admin_name, admin_email, CONCAT('{noop}', admin_password), admin_role, admin_updated_date_time,
        admin_status
FROM m2its_legacy.admin_info;

INSERT IGNORE INTO identity_db.contact_m2i
       (m_contact_id, m_name, m_email, m_subject, m_message, m_message_updated_date_time, m_message_status)
SELECT  m_contact_id, m_name, m_email, m_subject, m_message, m_message_updated_date_time, m_message_status
FROM m2its_legacy.contact_m2i;

-- ---------------------------------------------------------------- academic-service
INSERT IGNORE INTO academic_db.class_info
       (pk_class_id, fk_org_id, class_name, class_updated, class_status)
SELECT  pk_class_id, fk_org_id, class_name, class_updated, class_status
FROM m2its_legacy.class_info;

INSERT IGNORE INTO academic_db.subject_info
       (pk_subject_id, fk_org_id, fk_class_id, subject_name, subject_updated, subject_status)
SELECT  s.pk_subject_id, s.fk_org_id, s.fk_class_id, s.subject_name, s.subject_updated, s.subject_status
FROM m2its_legacy.subject_info s
WHERE EXISTS (SELECT 1 FROM academic_db.class_info c WHERE c.pk_class_id = s.fk_class_id);

-- chapter_info had no FK in the legacy schema; orphans (unknown subject) are skipped
INSERT IGNORE INTO academic_db.chapter_info
       (pk_chapter_id, fk_org_id, fk_class_id, fk_subject_id, chapter_name, chapter_updated, chapter_status)
SELECT  ch.pk_chapter_id, ch.fk_org_id, ch.fk_class_id, ch.fk_subject_id, ch.chapter_name, ch.chapter_updated,
        ch.chapter_status
FROM m2its_legacy.chapter_info ch
WHERE EXISTS (SELECT 1 FROM academic_db.subject_info s WHERE s.pk_subject_id = ch.fk_subject_id);

-- ---------------------------------------------------------------- question-service
INSERT IGNORE INTO question_db.question_info_v_two
       (pk_question_id, fk_org_id, fk_class_id, fk_subject_id, fk_chapter_id, question_name, question_type,
        question_updated_by, question_updated, question_status)
SELECT  pk_question_id, fk_org_id, fk_class_id, fk_subject_id, fk_chapter_id, question_name, question_type,
        question_updated_by, question_updated, question_status
FROM m2its_legacy.question_info_v_two;

INSERT IGNORE INTO question_db.answer_info_v_two
       (pk_answer_id, fk_org_id, fk_class_id, fk_subject_id, fk_chapter_id, fk_question_id, answers, correct_answer,
        answer_updated_by, answer_updated_date_time, answer_status)
SELECT  a.pk_answer_id, a.fk_org_id, a.fk_class_id, a.fk_subject_id, a.fk_chapter_id, a.fk_question_id, a.answers,
        a.correct_answer, a.answer_updated_by, a.answer_updated_date_time, a.answer_status
FROM m2its_legacy.answer_info_v_two a
WHERE EXISTS (SELECT 1 FROM question_db.question_info_v_two q WHERE q.pk_question_id = a.fk_question_id);

-- ---------------------------------------------------------------- exam-service
INSERT IGNORE INTO exam_db.exam_info
       (pk_exam_id, exam_session_id, fk_org_id, fk_class_id, fk_subject_id, fk_question_id,
        fk_user_selected_answer_id, exam_taken_by, exam_taken_date_time, exam_status)
SELECT  pk_exam_id, exam_session_id, fk_org_id, fk_class_id, fk_subject_id, fk_question_id,
        fk_user_selected_answer_id, exam_taken_by, exam_taken_date_time, exam_status
FROM m2its_legacy.exam_info;

-- ---------------------------------------------------------------- report
SELECT 'org_info' AS tbl, (SELECT COUNT(*) FROM m2its_legacy.org_info) AS legacy, (SELECT COUNT(*) FROM identity_db.org_info) AS migrated
UNION ALL SELECT 'user_info', (SELECT COUNT(*) FROM m2its_legacy.user_info), (SELECT COUNT(*) FROM identity_db.user_info)
UNION ALL SELECT 'admin_info', (SELECT COUNT(*) FROM m2its_legacy.admin_info), (SELECT COUNT(*) FROM identity_db.admin_info)
UNION ALL SELECT 'class_info', (SELECT COUNT(*) FROM m2its_legacy.class_info), (SELECT COUNT(*) FROM academic_db.class_info)
UNION ALL SELECT 'subject_info', (SELECT COUNT(*) FROM m2its_legacy.subject_info), (SELECT COUNT(*) FROM academic_db.subject_info)
UNION ALL SELECT 'chapter_info', (SELECT COUNT(*) FROM m2its_legacy.chapter_info), (SELECT COUNT(*) FROM academic_db.chapter_info)
UNION ALL SELECT 'question_info_v_two', (SELECT COUNT(*) FROM m2its_legacy.question_info_v_two), (SELECT COUNT(*) FROM question_db.question_info_v_two)
UNION ALL SELECT 'answer_info_v_two', (SELECT COUNT(*) FROM m2its_legacy.answer_info_v_two), (SELECT COUNT(*) FROM question_db.answer_info_v_two)
UNION ALL SELECT 'exam_info', (SELECT COUNT(*) FROM m2its_legacy.exam_info), (SELECT COUNT(*) FROM exam_db.exam_info);
-- "migrated" may be higher than "legacy" by the demo seed rows (dev only)
