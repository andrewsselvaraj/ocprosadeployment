<#
.SYNOPSIS
  Creates deploy/k8s/overlays/dev/secrets/*.env with random passwords (git-ignored).
  Existing files are kept unless -Force is given (changing DB passwords after MySQL has
  initialised its volume would lock the services out).
#>
param([switch]$Force)

$dir = Join-Path $PSScriptRoot "..\deploy\k8s\overlays\dev\secrets"

function New-RandomString([int]$length) {
    # Alphanumeric only: safe inside SQL, shell and .env files without quoting
    $chars = [char[]]"ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789"
    $bytes = New-Object byte[] $length
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    -join ($bytes | ForEach-Object { $chars[$_ % $chars.Length] })
}

function Write-EnvFile([string]$name, [string]$content) {
    $path = Join-Path $dir $name
    if ((Test-Path $path) -and -not $Force) {
        Write-Host "kept     $name"
        return
    }
    # UTF-8 without BOM: kustomize must not see a BOM in the first key name
    [System.IO.File]::WriteAllText($path, $content, (New-Object System.Text.UTF8Encoding $false))
    Write-Host "created  $name"
}

Write-EnvFile "jwt.env" "JWT_SECRET=$(New-RandomString 48)`n"
foreach ($svc in "identity", "academic", "question", "exam") {
    Write-EnvFile "$svc-db.env" "DB_USERNAME=$svc`nDB_PASSWORD=$(New-RandomString 24)`n"
}
Write-EnvFile "mysql-admin.env" "MYSQL_ROOT_PASSWORD=$(New-RandomString 24)`n"
