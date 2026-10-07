# Sourced by the Red Hat MySQL image the first time it starts with an empty data directory.
# $mysql_flags (provided by the image) connects as root through the local socket.
# Creates one database and one user per remaining microservice (identity_db is created by
# the image itself from MYSQL_DATABASE / MYSQL_USER).
mysql $mysql_flags <<EOSQL
CREATE DATABASE IF NOT EXISTS academic_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS question_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS exam_db     CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

CREATE USER IF NOT EXISTS '${ACADEMIC_DB_USER}'@'%' IDENTIFIED BY '${ACADEMIC_DB_PASSWORD}';
CREATE USER IF NOT EXISTS '${QUESTION_DB_USER}'@'%' IDENTIFIED BY '${QUESTION_DB_PASSWORD}';
CREATE USER IF NOT EXISTS '${EXAM_DB_USER}'@'%'     IDENTIFIED BY '${EXAM_DB_PASSWORD}';

GRANT ALL PRIVILEGES ON academic_db.* TO '${ACADEMIC_DB_USER}'@'%';
GRANT ALL PRIVILEGES ON question_db.* TO '${QUESTION_DB_USER}'@'%';
GRANT ALL PRIVILEGES ON exam_db.*     TO '${EXAM_DB_USER}'@'%';
FLUSH PRIVILEGES;
EOSQL
