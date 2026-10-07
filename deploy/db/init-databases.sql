-- One MySQL server, one database + one user per microservice (database-per-service).
-- Each service can only touch its own schema; Flyway creates the tables on startup.
-- Used by docker-compose (local only, fixed dev passwords). Kubernetes uses
-- deploy/k8s/components/mysql/init-databases.sh with passwords from Secrets instead.

CREATE DATABASE IF NOT EXISTS identity_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS academic_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS question_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS exam_db     CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

CREATE USER IF NOT EXISTS 'identity'@'%' IDENTIFIED BY 'identity-dev-pw';
CREATE USER IF NOT EXISTS 'academic'@'%' IDENTIFIED BY 'academic-dev-pw';
CREATE USER IF NOT EXISTS 'question'@'%' IDENTIFIED BY 'question-dev-pw';
CREATE USER IF NOT EXISTS 'exam'@'%'     IDENTIFIED BY 'exam-dev-pw';

GRANT ALL PRIVILEGES ON identity_db.* TO 'identity'@'%';
GRANT ALL PRIVILEGES ON academic_db.* TO 'academic'@'%';
GRANT ALL PRIVILEGES ON question_db.* TO 'question'@'%';
GRANT ALL PRIVILEGES ON exam_db.*     TO 'exam'@'%';
FLUSH PRIVILEGES;
