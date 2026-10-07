# 7. Verifying the deployed microservices on ROSA

Verify in layers, from the platform up to the business flow. Each layer depends on the one before.

## 7.1 Automated smoke test

```powershell
$gw = oc get route api-gateway -n m2its-dev -o jsonpath='{.spec.host}'
.\scripts\verify-deployment.ps1 -BaseUrl "https://$gw" -Namespace m2its-dev
```

```bash
./scripts/verify-deployment.sh "https://$(oc get route api-gateway -n m2its-dev -o jsonpath='{.spec.host}')" m2its-dev
```

What it checks (exit code 0 only if all pass):

| # | Check | Proves |
|---|---|---|
| 1 | `oc rollout status` for all 5 Deployments, route has TLS | pods are running and Ready; HTTPS is configured |
| 2 | gateway `/actuator/health/readiness` = UP | Route → Service → pod path works |
| 3 | no token → 401, `/internal/**` → 404, wrong password → 401 | security works end-to-end |
| 4 | admin login | identity-service + its DB + JWT signing |
| 5 | list classes, class tree | academic-service + DB, JWT validated by another service |
| 6 | questions with answers (admin) / without (student) | question-service, role-based views |
| 7 | submit exam (2 right, 1 wrong) → 66.67 % | exam-service → question-service call with token relay; NetworkPolicy allows it |
| 8 | read the result back, list sessions | data persisted in exam_db |

Expected output:

```
Verifying https://api-gateway-m2its-dev.apps.rosa.m2its.xxxx.openshiftapps.com
1. OpenShift resources in namespace m2its-dev
  [PASS] rollout identity-service (1/1 ready)
  ...
4. Exam flow (identity -> academic -> question -> exam)
  [PASS] submit exam (exam-service -> question-service) (score 66.67%, session 0006...)
ALL 19 CHECKS PASSED
```

The script needs the demo seed, so it is meant for **dev**. For prod use the health checks in 7.3 and a
read-only login with a real account.

## 7.2 Manual checklist

**Platform**

```powershell
oc project m2its-dev
oc get pods                     # 6 pods (5 services + mysql-0), STATUS Running, READY 1/1, low RESTARTS
oc get deploy                   # READY 1/1 for every service
oc get svc,route
oc get pvc                      # data-mysql-0 Bound
oc get networkpolicy
oc get events --sort-by=.lastTimestamp | Select-Object -Last 20
```

**Each service**

```powershell
oc logs deployment/identity-service | Select-String "Started|Flyway|ERROR"
#   → "Successfully applied 3 migrations"  and  "Started IdentityServiceApplication in … seconds"
oc exec deployment/identity-service -- curl -s localhost:8080/actuator/health
oc exec deployment/identity-service -- curl -s localhost:8080/actuator/info     # build version/time
```

**APIs through Swagger** (port-forward bypasses the gateway):

```powershell
oc port-forward svc/question-service 8083:8080
# browse http://localhost:8083/swagger-ui.html, Authorize with a token from /api/auth/login
```

**Service-to-service and network policies**

```powershell
# allowed: exam -> question
oc exec deployment/exam-service -- curl -s -o /dev/null -w "%{http_code}\n" http://question-service:8080/actuator/health
# blocked by NetworkPolicy: gateway -> mysql   (expect a timeout)
oc exec deployment/api-gateway -- timeout 5 bash -c "</dev/tcp/mysql/3306" ; "exit code $LASTEXITCODE (non-zero = blocked)"
```

**Database**

```powershell
oc rsh mysql-0
mysql -u root -p"$MYSQL_ROOT_PASSWORD" -e "SHOW DATABASES; SELECT version, description, success FROM identity_db.flyway_schema_history;"
```

**Metrics**: OpenShift console → Observe → Metrics → `http_server_requests_seconds_count{namespace="m2its-dev"}`.
Or Observe → Targets: the `m2its-services` ServiceMonitor endpoints should be **Up**.

**Resilience drills** (good for learning):

```powershell
oc delete pod -l app.kubernetes.io/name=exam-service    # a new pod replaces it within seconds
oc scale deployment/question-service --replicas=0       # submitting an exam now returns 503 (no hang)
oc scale deployment/question-service --replicas=1
oc rollout restart deployment/identity-service; oc rollout status deployment/identity-service  # zero downtime
```

## 7.3 Production health check

```powershell
$gw = oc get route api-gateway -n m2its-prod -o jsonpath='{.spec.host}'
curl.exe -s https://$gw/actuator/health/readiness     # {"status":"UP"}
oc get hpa,pdb -n m2its-prod                          # targets known, 2+ replicas
oc get pods -n m2its-prod -o wide                     # replicas spread over different nodes
```

## 7.4 Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Pod `ImagePullBackOff` | Build not finished/failed, or wrong image name | `oc get builds`, `oc logs bc/<svc>`; check `images:` in the overlay; prod: pull secret for Quay |
| Build fails at `mvnw` | Windows CRLF in `mvnw` | `.gitattributes` forces LF; re-commit `mvnw` (`git add --renormalize .`) |
| Pod `CrashLoopBackOff`, log `Communications link failure` | MySQL not ready or wrong `DB_HOST` | `oc get pods` (mysql-0 Ready?), `oc get cm m2its-config -o yaml` |
| Log `Access denied for user` | DB password differs from what MySQL was initialised with | MySQL users are created only on the **first** start; changing secrets later needs `ALTER USER` or deleting the PVC (dev) |
| Log `must be set and at least 32 bytes long` | `JWT_SECRET` missing/short | check the `m2its-jwt` secret |
| Log `Schema validation: wrong column type` | Entity and migration disagree | fix the mapping or add a migration; never edit applied migrations |
| Log `Validate failed: Migrations have failed validation` | An applied migration file was changed | revert the file; put changes in a new `V<n>__` file |
| Pod `Pending` | Not enough CPU/memory on workers, or PVC unbound | `oc describe pod` events; `rosa edit machinepool --replicas 3` |
| `OOMKilled` restarts | memory limit too low for the heap | raise `limits.memory` or lower `MaxRAMPercentage` |
| Route returns 503 "Application is not available" | no Ready gateway pod | `oc get pods`, readiness probe events |
| Every API call 401 although login worked | services have different `JWT_SECRET` / `JWT_ISSUER` | all services must read the same `m2its-jwt` secret and ConfigMap |
| Exam submit 503 | question-service down or NetworkPolicy blocks exam → question | `oc get pods`, `oc get networkpolicy allow-exam-to-question -o yaml` |
| mysql-0 fails to pull `rhel9/mysql-84` | image not available to the cluster | use `registry.redhat.io/rhel9/mysql-80:latest` in `components/mysql/mysql.yaml` |
