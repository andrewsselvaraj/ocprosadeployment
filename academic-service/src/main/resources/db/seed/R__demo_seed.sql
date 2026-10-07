-- DEMO DATA for local runs and the "dev" environment only (see identity-service seed for logins).
INSERT IGNORE INTO class_info (pk_class_id, fk_org_id, class_name, class_status)
VALUES ('demo-class-10', 'demo-org', 'Class 10', 'A');

INSERT IGNORE INTO subject_info (pk_subject_id, fk_org_id, fk_class_id, subject_name, subject_status)
VALUES ('demo-subj-sci', 'demo-org', 'demo-class-10', 'Science', 'A'),
       ('demo-subj-cloud', 'demo-org', 'demo-class-10', 'Cloud Computing', 'A');

INSERT IGNORE INTO chapter_info (pk_chapter_id, fk_org_id, fk_class_id, fk_subject_id, chapter_name, chapter_status)
VALUES ('demo-chap-physics', 'demo-org', 'demo-class-10', 'demo-subj-sci', 'Physics Basics', 'A'),
       ('demo-chap-aws', 'demo-org', 'demo-class-10', 'demo-subj-cloud', 'AWS Fundamentals', 'A');
