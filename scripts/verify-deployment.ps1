<#
.SYNOPSIS
  End-to-end smoke test of the M2ITS microservices through the api-gateway.

.DESCRIPTION
  Works against a local run, a plain Kubernetes cluster or ROSA. Requires the demo seed data
  (dev environment). Exit code 0 = all checks passed.

.EXAMPLE
  # Local (services started with mvnw)
  .\scripts\verify-deployment.ps1 -BaseUrl http://localhost:8080

.EXAMPLE
  # ROSA / OpenShift: also checks pods and rollouts with oc
  $gw = oc get route api-gateway -n m2its-dev -o jsonpath='{.spec.host}'
  .\scripts\verify-deployment.ps1 -BaseUrl "https://$gw" -Namespace m2its-dev
#>
param(
    [Parameter(Mandatory = $true)][string]$BaseUrl,
    [string]$Namespace = "",
    [string]$AdminEmail = "admin@demo.local",
    [string]$StudentEmail = "student@demo.local",
    [string]$Password = "Demo@123"
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd("/")
$script:passed = 0
$script:failed = 0

function Check([string]$name, [scriptblock]$test) {
    try {
        $detail = & $test
        Write-Host ("  [PASS] {0} {1}" -f $name, $detail) -ForegroundColor Green
        $script:passed++
    } catch {
        Write-Host ("  [FAIL] {0}: {1}" -f $name, $_.Exception.Message) -ForegroundColor Red
        $script:failed++
    }
}

function Api([string]$method, [string]$path, $body = $null, [string]$token = "") {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $params = @{ Method = $method; Uri = "$BaseUrl$path"; Headers = $headers; TimeoutSec = 30 }
    if ($null -ne $body) {
        $params["Body"] = ($body | ConvertTo-Json -Depth 10)
        $params["ContentType"] = "application/json"
    }
    Invoke-RestMethod @params
}

function StatusOf([string]$path, [string]$token = "") {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    try {
        (Invoke-WebRequest -Method Get -Uri "$BaseUrl$path" -Headers $headers -UseBasicParsing -TimeoutSec 30).StatusCode
    } catch {
        if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { throw }
    }
}

Write-Host "Verifying $BaseUrl" -ForegroundColor Cyan

# ---- 1. Platform checks (only when a namespace is given and oc is installed) -------------
if ($Namespace -and (Get-Command oc -ErrorAction SilentlyContinue)) {
    Write-Host "1. OpenShift resources in namespace $Namespace"
    foreach ($d in "identity-service", "academic-service", "question-service", "exam-service", "api-gateway") {
        Check "rollout $d" {
            $out = oc rollout status "deployment/$d" -n $Namespace --timeout=120s 2>&1
            if ($LASTEXITCODE -ne 0) { throw "$out" }
            $ready = oc get deployment $d -n $Namespace -o jsonpath='{.status.readyReplicas}/{.spec.replicas}'
            "($ready ready)"
        }
    }
    Check "route api-gateway has TLS" {
        $tls = oc get route api-gateway -n $Namespace -o jsonpath='{.spec.tls.termination}'
        if (-not $tls) { throw "route has no TLS termination" }
        "($tls)"
    }
} else {
    Write-Host "1. OpenShift resource checks skipped (pass -Namespace and install oc to enable)"
}

# ---- 2. Health -------------------------------------------------------------------------
Write-Host "2. Health"
Check "gateway readiness" {
    $h = Api GET "/actuator/health/readiness"
    if ($h.status -ne "UP") { throw "status=$($h.status)" }
    "(UP)"
}

# ---- 3. Security -----------------------------------------------------------------------
Write-Host "3. Security"
Check "no token -> 401" {
    $s = StatusOf "/api/orgs"
    if ($s -ne 401) { throw "got $s" }
    "(401)"
}
Check "internal API not exposed -> 404" {
    $s = StatusOf "/internal/answers/evaluate"
    if ($s -ne 404) { throw "got $s" }
    "(404)"
}
Check "wrong password -> 401" {
    try {
        Api POST "/api/auth/login" @{ email = $AdminEmail; password = "wrong-password" } | Out-Null
        throw "login unexpectedly succeeded"
    } catch {
        if (-not $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 401) { throw }
    }
    "(401)"
}

# ---- 4. Business flow ------------------------------------------------------------------
Write-Host "4. Exam flow (identity -> academic -> question -> exam)"
$adminToken = $null; $studentToken = $null; $questions = $null; $session = $null

Check "admin login (identity-service)" {
    $r = Api POST "/api/auth/login" @{ email = $AdminEmail; password = $Password }
    $script:adminToken = $r.accessToken
    "(roles=$($r.user.roles -join ','), org=$($r.user.orgId))"
}
Check "list classes (academic-service)" {
    $classes = Api GET "/api/classes" $null $adminToken
    if (-not ($classes | Where-Object id -eq "demo-class-10")) { throw "demo-class-10 missing" }
    "($(@($classes).Count) classes)"
}
Check "class tree (academic-service, JOIN FETCH)" {
    $tree = Api GET "/api/classes/demo-class-10/tree" $null $adminToken
    "($(@($tree.subjects).Count) subjects)"
}
Check "admin sees answers (question-service)" {
    $page = Api GET "/api/questions?subjectId=demo-subj-cloud" $null $adminToken
    $script:questions = $page.content
    if (@($questions).Count -lt 3) { throw "expected 3 demo questions, got $(@($questions).Count)" }
    "($($page.totalElements) questions)"
}
Check "student login" {
    $r = Api POST "/api/auth/login" @{ email = $StudentEmail; password = $Password }
    $script:studentToken = $r.accessToken
    "(roles=$($r.user.roles -join ','))"
}
Check "student does not see answers" {
    $page = Api GET "/api/questions?subjectId=demo-subj-cloud" $null $studentToken
    if ($null -ne $page.content[0].options[0].correct) { throw "correct flag leaked to student" }
    "(hidden)"
}
Check "submit exam (exam-service -> question-service)" {
    # 2 correct answers + 1 wrong answer => 66.67 %
    $answers = @()
    for ($i = 0; $i -lt 3; $i++) {
        $q = $questions[$i]
        $pick = if ($i -lt 2) { $q.options | Where-Object correct -eq $true | Select-Object -First 1 }
                else { $q.options | Where-Object correct -eq $false | Select-Object -First 1 }
        $answers += @{ questionId = $q.id; answerId = $pick.id }
    }
    $script:session = Api POST "/api/exams/submit" @{ classId = "demo-class-10"; subjectId = "demo-subj-cloud"; answers = $answers } $studentToken
    if ($session.correctAnswers -ne 2 -or $session.totalQuestions -ne 3) {
        throw "score $($session.correctAnswers)/$($session.totalQuestions)"
    }
    "(score $($session.scorePercent)%, session $($session.sessionId))"
}
Check "read result back (re-scored)" {
    $r = Api GET "/api/exams/sessions/$($session.sessionId)" $null $studentToken
    if ($r.scorePercent -ne $session.scorePercent) { throw "score changed: $($r.scorePercent)" }
    "($($r.scorePercent)%)"
}
Check "admin lists sessions" {
    $p = Api GET "/api/exams/sessions" $null $adminToken
    "($($p.totalElements) sessions)"
}

# ---- Summary ---------------------------------------------------------------------------
Write-Host ""
if ($script:failed -eq 0) {
    Write-Host "ALL $($script:passed) CHECKS PASSED" -ForegroundColor Green
    exit 0
}
Write-Host "$($script:failed) FAILED, $($script:passed) passed" -ForegroundColor Red
exit 1
