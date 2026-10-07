-- DEMO DATA for local runs and the "dev" environment only.
-- Subject "Cloud Computing" (demo-subj-cloud) gets three single-choice questions.
INSERT IGNORE INTO question_info_v_two
  (pk_question_id, fk_org_id, fk_class_id, fk_subject_id, fk_chapter_id, question_name, question_type, question_updated_by, question_status)
VALUES
  ('demo-q1', 'demo-org', 'demo-class-10', 'demo-subj-cloud', 'demo-chap-aws', 'Which AWS service provides managed OpenShift clusters?', 'text', 'seed', 'A'),
  ('demo-q2', 'demo-org', 'demo-class-10', 'demo-subj-cloud', 'demo-chap-aws', 'Which Kubernetes object keeps a set of identical pods running?', 'text', 'seed', 'A'),
  ('demo-q3', 'demo-org', 'demo-class-10', 'demo-subj-cloud', 'demo-chap-aws', 'Which OpenShift object exposes a service outside the cluster?', 'text', 'seed', 'A');

INSERT IGNORE INTO answer_info_v_two
  (pk_answer_id, fk_org_id, fk_class_id, fk_subject_id, fk_chapter_id, fk_question_id, answers, correct_answer, answer_updated_by, answer_status)
VALUES
  ('demo-q1-a', 'demo-org', 'demo-class-10', 'demo-subj-cloud', 'demo-chap-aws', 'demo-q1', 'ROSA', 'Yes', 'seed', 'A'),
  ('demo-q1-b', 'demo-org', 'demo-class-10', 'demo-subj-cloud', 'demo-chap-aws', 'demo-q1', 'Amazon S3', 'No', 'seed', 'A'),
  ('demo-q1-c', 'demo-org', 'demo-class-10', 'demo-subj-cloud', 'demo-chap-aws', 'demo-q1', 'AWS Lambda', 'No', 'seed', 'A'),
  ('demo-q1-d', 'demo-org', 'demo-class-10', 'demo-subj-cloud', 'demo-chap-aws', 'demo-q1', 'Amazon SQS', 'No', 'seed', 'A'),
  ('demo-q2-a', 'demo-org', 'demo-class-10', 'demo-subj-cloud', 'demo-chap-aws', 'demo-q2', 'ConfigMap', 'No', 'seed', 'A'),
  ('demo-q2-b', 'demo-org', 'demo-class-10', 'demo-subj-cloud', 'demo-chap-aws', 'demo-q2', 'Deployment', 'Yes', 'seed', 'A'),
  ('demo-q2-c', 'demo-org', 'demo-class-10', 'demo-subj-cloud', 'demo-chap-aws', 'demo-q2', 'Secret', 'No', 'seed', 'A'),
  ('demo-q2-d', 'demo-org', 'demo-class-10', 'demo-subj-cloud', 'demo-chap-aws', 'demo-q2', 'Namespace', 'No', 'seed', 'A'),
  ('demo-q3-a', 'demo-org', 'demo-class-10', 'demo-subj-cloud', 'demo-chap-aws', 'demo-q3', 'Route', 'Yes', 'seed', 'A'),
  ('demo-q3-b', 'demo-org', 'demo-class-10', 'demo-subj-cloud', 'demo-chap-aws', 'demo-q3', 'PersistentVolume', 'No', 'seed', 'A'),
  ('demo-q3-c', 'demo-org', 'demo-class-10', 'demo-subj-cloud', 'demo-chap-aws', 'demo-q3', 'BuildConfig', 'No', 'seed', 'A'),
  ('demo-q3-d', 'demo-org', 'demo-class-10', 'demo-subj-cloud', 'demo-chap-aws', 'demo-q3', 'ImageStream', 'No', 'seed', 'A');
