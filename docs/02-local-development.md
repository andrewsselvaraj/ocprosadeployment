# 2. Local development

## 2.1 Prerequisites

| Tool | Needed? | Notes |
|---|---|---|
| Java 17 (JDK) | **yes** | `java -version` |
| Maven | no | The **Maven Wrapper** (`mvnw.cmd` / `./mvnw`) downloads Maven 3.9.16 on first use |
| MySQL | no | Without `DB_URL` every service uses an in-memory **H2** database in MySQL mode, with demo data |
| Docker / Podman | no | Only for [docker-compose](03-docker.md#35-run-the-whole-stack-with-docker-compose) |
| IDE | optional | IntelliJ IDEA or VS Code (Extension Pack for Java): open the root `pom.xml` |

## 2.2 Build and test

```powershell
.\mvnw.cmd verify                       # compile + run all tests (about 2 minutes)
.\mvnw.cmd -pl question-service -am verify   # one service (+ the modules it depends on)
.\mvnw.cmd package -DskipTests          # just build the jars into */target/
```

What the tests cover (all run against H2 with the real Flyway migrations):

| Test | Proves |
|---|---|
| `IdentityApiTest` | real JWT login → token accepted by other endpoints; legacy `{noop}` password upgraded to BCrypt; role rules; validation errors as problem+json; health endpoints |
| `AcademicApiTest` | class → subject → chapter creation, tenant isolation, delete rules, `JOIN FETCH` tree |
| `QuestionApiTest` | students never see answers, option order, retired options still score, legacy `DEL` rows load |
| `ExamApiTest` / `QuestionClientTest` | scoring flow, 503 when question-service is down, token relay over real HTTP |
| `GatewayRoutingTest` | each URL prefix reaches the right service; `/internal/**` is 404 |

## 2.3 Run the services

Each service is a normal Spring Boot app. Install the shared modules once, then start each service in
its own terminal:

```powershell
.\mvnw.cmd install -DskipTests          # puts common-lib into your local ~/.m2 repository
.\mvnw.cmd -pl identity-service spring-boot:run
.\mvnw.cmd -pl academic-service spring-boot:run
.\mvnw.cmd -pl question-service spring-boot:run
.\mvnw.cmd -pl exam-service     spring-boot:run
.\mvnw.cmd -pl api-gateway      spring-boot:run
```

Or run the packaged jars, which uses less memory than five Maven processes:

```powershell
.\mvnw.cmd package -DskipTests
$env:JAVA_TOOL_OPTIONS = "-Xmx256m -XX:+UseSerialGC"     # 5 JVMs need ~1.5 GB RAM in total
Start-Process java "-jar identity-service/target/identity-service-1.0.0-SNAPSHOT.jar"
Start-Process java "-jar academic-service/target/academic-service-1.0.0-SNAPSHOT.jar"
Start-Process java "-jar question-service/target/question-service-1.0.0-SNAPSHOT.jar"
Start-Process java "-jar exam-service/target/exam-service-1.0.0-SNAPSHOT.jar"
Start-Process java "-jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar"
```

Default ports: gateway **8080**, identity 8081, academic 8082, question 8083, exam 8084.
**If a port is taken** (another app already uses 8080/8081), move everything with environment variables:

```powershell
$env:SERVER_PORT = "28081"   # before starting identity-service, and so on per service
# the gateway must then be told where the services are:
$env:IDENTITY_SERVICE_URL = "http://localhost:28081"   # ACADEMIC_/QUESTION_/EXAM_SERVICE_URL likewise
# exam-service needs QUESTION_SERVICE_URL too
```

Then run the same smoke test that is used on ROSA:

```powershell
.\scripts\verify-deployment.ps1 -BaseUrl http://localhost:8080
```

## 2.4 Explore the APIs

- **Swagger UI** per service: http://localhost:8081/swagger-ui.html (8082, 8083, 8084 likewise).
  Click **Authorize** and paste a token from `/api/auth/login`.
- **Health / metrics:** `/actuator/health`, `/actuator/health/readiness`, `/actuator/prometheus`, `/actuator/info`.
- **Gateway routes:** http://localhost:8080/actuator/gateway/routes

PowerShell walkthrough (through the gateway):

```powershell
$base = "http://localhost:8080"
$login = Invoke-RestMethod "$base/api/auth/login" -Method Post -ContentType application/json `
         -Body '{"email":"admin@demo.local","password":"Demo@123"}'
$h = @{ Authorization = "Bearer $($login.accessToken)" }

Invoke-RestMethod "$base/api/auth/me" -Headers $h                    # what is inside my token?
Invoke-RestMethod "$base/api/classes" -Headers $h                    # classes of my organisation
Invoke-RestMethod "$base/api/classes/demo-class-10/tree" -Headers $h # class -> subjects -> chapters
(Invoke-RestMethod "$base/api/questions?subjectId=demo-subj-cloud" -Headers $h).content

# create a question (ADMIN or DATA_ENTRY)
$q = @{ classId="demo-class-10"; subjectId="demo-subj-cloud"; text="What does ROSA stand for?"; type="text"
        options=@(@{text="Red Hat OpenShift Service on AWS"; correct=$true}, @{text="Remote Ops Service"; correct=$false}) }
Invoke-RestMethod "$base/api/questions" -Method Post -Headers $h -ContentType application/json -Body ($q | ConvertTo-Json -Depth 5)
```

curl equivalent:

```bash
TOKEN=$(curl -s localhost:8080/api/auth/login -H 'Content-Type: application/json' \
        -d '{"email":"student@demo.local","password":"Demo@123"}' | jq -r .accessToken)
curl -s localhost:8080/api/questions?subjectId=demo-subj-cloud -H "Authorization: Bearer $TOKEN" | jq
```

## 2.5 API overview

| Method & path | Who | Purpose |
|---|---|---|
| `POST /api/auth/login` | public | email + password → JWT |
| `GET /api/auth/me` | any | token contents |
| `GET/POST/PUT/DELETE /api/orgs[/{id}]` | read: own org, write: SUPERADMIN | organisations |
| `GET/POST/PUT/DELETE /api/users[/{id}]` | ADMIN, SUPERADMIN | org members (paged) |
| `POST /api/contact` / `GET /api/contact` | public / SUPERADMIN | contact form |
| `GET /api/classes`, `/api/classes/{id}`, `/api/classes/{id}/tree`, `/api/classes/{id}/subjects` | any | catalogue |
| `POST/PUT/DELETE /api/classes`, `/api/subjects`, `/api/chapters` | ADMIN, DATA_ENTRY | edit catalogue |
| `GET /api/subjects/{id}/chapters` | any | chapters of a subject |
| `GET /api/questions?subjectId=&chapterId=&page=&size=` | any (answers hidden for USER) | question bank |
| `POST/PUT/DELETE /api/questions[/{id}]` | ADMIN, DATA_ENTRY | edit questions |
| `POST /api/exams/submit` | any | submit answers → score |
| `GET /api/exams/sessions[?takenBy=]` | any (USER: own) | exam history |
| `GET /api/exams/sessions/{id}` | owner or ADMIN | detailed result |
| `POST /internal/answers/evaluate` | services only (not routed) | scoring used by exam-service |

Errors are always `application/problem+json`, for example
`{"status":403,"detail":"Record belongs to another organisation"}`.

## 2.6 Use a real MySQL locally (optional)

```powershell
# after running deploy/db/init-databases.sql on your MySQL server
$env:DB_URL = "jdbc:mysql://localhost:3306/identity_db?allowPublicKeyRetrieval=true&sslMode=DISABLED"
$env:DB_USERNAME = "identity"; $env:DB_PASSWORD = "identity-dev-pw"
.\mvnw.cmd -pl identity-service spring-boot:run
```

Flyway creates the tables on first start. `FLYWAY_LOCATIONS=classpath:db/migration` skips the demo seed.

## 2.7 Configuration reference

All settings are environment variables with local defaults (see each `application.yml`):

| Variable | Default | Used by |
|---|---|---|
| `SERVER_PORT` | 8080–8084 | all |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_POOL_SIZE` | H2 in memory, `sa`, empty, 10 | domain services |
| `FLYWAY_LOCATIONS` | `classpath:db/migration,classpath:db/seed` | domain services |
| `JWT_SECRET` (≥ 32 chars), `JWT_ISSUER`, `JWT_TTL` | dev-only secret, `m2its-identity`, `PT1H` | domain services |
| `QUESTION_SERVICE_URL` | `http://localhost:8083` | exam-service, gateway |
| `IDENTITY_/ACADEMIC_/EXAM_SERVICE_URL` | `http://localhost:808x` | gateway |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | gateway |
| `API_DOCS_ENABLED` | `true` | domain services (Swagger on/off) |
| `LOGGING_STRUCTURED_FORMAT_CONSOLE` | (plain text) | all; `ecs` = JSON logs |
