# 8. Migrating the legacy data

The new services keep the legacy table and column names, so the data moves with
[deploy/db/legacy-import.sql](../deploy/db/legacy-import.sql): plain `INSERT ... SELECT` from a staging
copy of the old database into the four service databases.

## 8.1 Handle the dump as sensitive data

`m2itssql_complete.sql` contains **real e-mail addresses and plain-text passwords** of about 50 people.

- It is in `.gitignore` and `.dockerignore`: never commit it, never bake it into an image.
- Copy it to the cluster only for the import, then delete it there and drop the staging database.
- After migration, ask users to change their passwords (the old ones were exposed in plain text for years).

## 8.2 Steps (dev cluster, in-cluster MySQL)

The services must have started once, so Flyway has created the tables.

```powershell
oc project m2its-dev
oc cp m2itssql_complete.sql mysql-0:/tmp/legacy.sql
oc cp deploy/db/legacy-import.sql mysql-0:/tmp/legacy-import.sql
oc rsh mysql-0
```

Inside the pod:

```bash
mysql -u root -p"$MYSQL_ROOT_PASSWORD" -e "CREATE DATABASE m2its_legacy CHARACTER SET utf8mb4"
mysql -u root -p"$MYSQL_ROOT_PASSWORD" m2its_legacy < /tmp/legacy.sql
mysql -u root -p"$MYSQL_ROOT_PASSWORD" --table < /tmp/legacy-import.sql     # prints a count report
mysql -u root -p"$MYSQL_ROOT_PASSWORD" -e "DROP DATABASE m2its_legacy"
rm /tmp/legacy.sql /tmp/legacy-import.sql
exit
```

The report at the end compares legacy and migrated row counts per table. In dev, "migrated" includes the
handful of demo rows.

## 8.3 Steps (prod, Amazon RDS)

Stream the files from your PC through a temporary client pod, so nothing is stored in the cluster:

```powershell
oc project m2its-prod
oc run mysql-client --image=registry.redhat.io/rhel9/mysql-84 --restart=Never --command -- sleep 3600
$m = "mysql -h <rds-endpoint> -u dbadmin -p'<admin-password>'"
oc exec mysql-client -- bash -c "$m -e 'CREATE DATABASE m2its_legacy CHARACTER SET utf8mb4'"
Get-Content m2itssql_complete.sql -Raw -Encoding UTF8 | oc exec -i mysql-client -- bash -c "$m m2its_legacy"
Get-Content deploy/db/legacy-import.sql -Raw | oc exec -i mysql-client -- bash -c "$m --table"
oc exec mysql-client -- bash -c "$m -e 'DROP DATABASE m2its_legacy'"
oc delete pod mysql-client
```

Take an RDS snapshot first: `aws rds create-db-snapshot --db-instance-identifier m2its-mysql --db-snapshot-identifier before-legacy-import`.

## 8.4 What the import does

| Step | Detail |
|---|---|
| Passwords | `CONCAT('{noop}', password)`. Users log in with their old password, and it is re-hashed with BCrypt on that login |
| Orphans | subjects without a class, chapters without a subject, and answers without a question are skipped (the new schema enforces these FKs) |
| Not copied | `answer_info_v_two_bk`, `question_info` (v1), `main_table`, `dynamic_table`, `test_table_one`, `role_master` |
| Re-runnable | `INSERT IGNORE`: running it twice does not duplicate rows |

## 8.5 After the import

```powershell
$gw = oc get route api-gateway -o jsonpath='{.spec.host}'
# log in as a legacy user (e.g. a school admin), then list classes and questions of that school
```

Known data-quality issues carried over (handled by the code, see [01-architecture.md §1.6](01-architecture.md#16-legacy-data-quality-found-and-handled)):
duplicate e-mails in `user_info`, the `Data Entry Operator` role, status codes `D`/`DEL`/`I`/`C`, and empty
`fk_chapter_id` values in answers.
