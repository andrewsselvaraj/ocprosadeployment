#!/usr/bin/env bash
# End-to-end smoke test of the M2ITS microservices through the api-gateway.
# Same checks as verify-deployment.ps1. Needs: curl, jq (and oc for the optional cluster checks).
#
#   ./scripts/verify-deployment.sh http://localhost:8080
#   ./scripts/verify-deployment.sh "https://$(oc get route api-gateway -n m2its-dev -o jsonpath='{.spec.host}')" m2its-dev
set -uo pipefail

BASE_URL="${1:?usage: $0 <gateway-base-url> [namespace]}"
BASE_URL="${BASE_URL%/}"
NAMESPACE="${2:-}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@demo.local}"
STUDENT_EMAIL="${STUDENT_EMAIL:-student@demo.local}"
PASSWORD="${PASSWORD:-Demo@123}"

passed=0; failed=0
pass() { echo -e "  \e[32m[PASS]\e[0m $1"; passed=$((passed + 1)); }
fail() { echo -e "  \e[31m[FAIL]\e[0m $1"; failed=$((failed + 1)); }

# api METHOD PATH [JSON_BODY] [TOKEN] -> prints body, sets $status
api() {
  local method=$1 path=$2 body=${3:-} token=${4:-} out
  local args=(-s -k -o /tmp/m2its_body.$$ -w '%{http_code}' -X "$method" "$BASE_URL$path" --max-time 30)
  [[ -n $token ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n $body ]] && args+=(-H 'Content-Type: application/json' -d "$body")
  status=$(curl "${args[@]}")
  cat /tmp/m2its_body.$$; rm -f /tmp/m2its_body.$$
}

echo "Verifying $BASE_URL"

echo "1. OpenShift resources"
if [[ -n $NAMESPACE ]] && command -v oc >/dev/null; then
  for d in identity-service academic-service question-service exam-service api-gateway; do
    if oc rollout status "deployment/$d" -n "$NAMESPACE" --timeout=120s >/dev/null 2>&1; then
      pass "rollout $d ($(oc get deployment "$d" -n "$NAMESPACE" -o jsonpath='{.status.readyReplicas}/{.spec.replicas}') ready)"
    else
      fail "rollout $d"
    fi
  done
  tls=$(oc get route api-gateway -n "$NAMESPACE" -o jsonpath='{.spec.tls.termination}')
  [[ -n $tls ]] && pass "route api-gateway has TLS ($tls)" || fail "route api-gateway has no TLS"
else
  echo "  skipped (pass a namespace and install oc to enable)"
fi

echo "2. Health"
h=$(api GET /actuator/health/readiness)
[[ $(jq -r .status <<<"$h" 2>/dev/null) == UP ]] && pass "gateway readiness (UP)" || fail "gateway readiness: $status $h"

echo "3. Security"
api GET /api/orgs >/dev/null;                   [[ $status == 401 ]] && pass "no token -> 401" || fail "no token -> got $status"
api GET /internal/answers/evaluate >/dev/null;  [[ $status == 404 ]] && pass "internal API not exposed -> 404" || fail "internal API -> got $status"
api POST /api/auth/login "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"wrong-password\"}" >/dev/null
[[ $status == 401 ]] && pass "wrong password -> 401" || fail "wrong password -> got $status"

echo "4. Exam flow"
r=$(api POST /api/auth/login "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$PASSWORD\"}")
ADMIN_TOKEN=$(jq -r .accessToken <<<"$r" 2>/dev/null)
[[ $status == 200 && -n $ADMIN_TOKEN ]] && pass "admin login" || fail "admin login: $status $r"

r=$(api GET /api/classes "" "$ADMIN_TOKEN")
jq -e 'map(select(.id=="demo-class-10")) | length == 1' <<<"$r" >/dev/null 2>&1 \
  && pass "list classes" || fail "list classes: $status $r"

r=$(api GET /api/classes/demo-class-10/tree "" "$ADMIN_TOKEN")
[[ $status == 200 ]] && pass "class tree ($(jq '.subjects | length' <<<"$r") subjects)" || fail "class tree: $status"

QUESTIONS=$(api GET "/api/questions?subjectId=demo-subj-cloud" "" "$ADMIN_TOKEN")
[[ $(jq '.content | length' <<<"$QUESTIONS" 2>/dev/null) -ge 3 ]] && pass "admin sees questions with answers" || fail "questions: $status $QUESTIONS"

r=$(api POST /api/auth/login "{\"email\":\"$STUDENT_EMAIL\",\"password\":\"$PASSWORD\"}")
STUDENT_TOKEN=$(jq -r .accessToken <<<"$r" 2>/dev/null)
[[ $status == 200 && -n $STUDENT_TOKEN ]] && pass "student login" || fail "student login: $status"

r=$(api GET "/api/questions?subjectId=demo-subj-cloud" "" "$STUDENT_TOKEN")
[[ $(jq '.content[0].options[0] | has("correct")' <<<"$r") == false ]] && pass "student does not see answers" || fail "correct flag leaked"

# 2 correct + 1 wrong => 66.67 %
ANSWERS=$(jq -c '[.content[0:3] | to_entries[] |
  {questionId: .value.id,
   answerId: (if .key < 2 then (.value.options | map(select(.correct)) | .[0].id)
              else (.value.options | map(select(.correct | not)) | .[0].id) end)}]' <<<"$QUESTIONS")
SESSION=$(api POST /api/exams/submit "{\"classId\":\"demo-class-10\",\"subjectId\":\"demo-subj-cloud\",\"answers\":$ANSWERS}" "$STUDENT_TOKEN")
SESSION_ID=$(jq -r .sessionId <<<"$SESSION" 2>/dev/null)
SCORE=$(jq -r .scorePercent <<<"$SESSION" 2>/dev/null)
[[ $status == 201 && $(jq .correctAnswers <<<"$SESSION") == 2 ]] && pass "submit exam (score $SCORE%)" || fail "submit exam: $status $SESSION"

r=$(api GET "/api/exams/sessions/$SESSION_ID" "" "$STUDENT_TOKEN")
[[ $(jq -r .scorePercent <<<"$r" 2>/dev/null) == "$SCORE" ]] && pass "read result back ($SCORE%)" || fail "read result: $status $r"

r=$(api GET /api/exams/sessions "" "$ADMIN_TOKEN")
[[ $status == 200 ]] && pass "admin lists sessions ($(jq .totalElements <<<"$r") sessions)" || fail "list sessions: $status"

echo
if [[ $failed -eq 0 ]]; then echo -e "\e[32mALL $passed CHECKS PASSED\e[0m"; exit 0; fi
echo -e "\e[31m$failed FAILED, $passed passed\e[0m"; exit 1
