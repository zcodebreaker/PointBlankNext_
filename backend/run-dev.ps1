$ErrorActionPreference = "Stop"

$environmentFile = Join-Path $PSScriptRoot ".env"
if (Test-Path -LiteralPath $environmentFile) {
    Get-Content -LiteralPath $environmentFile | ForEach-Object {
        if ($_ -match '^\s*([^#][A-Z0-9_]*)=(.*)$') {
            $name = $matches[1]
            $value = $matches[2].Trim()
            if ($value.Length -ge 2 -and $value[0] -eq '"' -and $value[-1] -eq '"') {
                $value = $value.Substring(1, $value.Length - 2)
            }
            if ([Environment]::GetEnvironmentVariable($name, "Process") -eq $null) {
                [Environment]::SetEnvironmentVariable($name, $value, "Process")
            }
        }
    }
}

$userJavaHome = [Environment]::GetEnvironmentVariable("JAVA_HOME", "User")
if (-not $env:JAVA_HOME -and $userJavaHome) {
    $env:JAVA_HOME = $userJavaHome
}

if (-not $env:JAVA_HOME) {
    $localJdk = Get-ChildItem -LiteralPath (Join-Path $env:USERPROFILE ".jdks") -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName "bin\java.exe") } |
        Sort-Object Name -Descending |
        Select-Object -First 1
    if ($localJdk) {
        $env:JAVA_HOME = $localJdk.FullName
    }
}

if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    throw "JAVA_HOME não aponta para um JDK válido. Abra um novo terminal e tente novamente."
}

if (-not $env:DB_USERNAME) {
    $env:DB_USERNAME = "postgres"
}

if (-not $env:DB_PASSWORD) {
    $databasePassword = Read-Host "Senha do banco de dadoos:" -AsSecureString
    $env:DB_PASSWORD = [Net.NetworkCredential]::new("", $databasePassword).Password
}

if (-not $env:APP_JWT_SECRET) {
    $randomBytes = [Security.Cryptography.RandomNumberGenerator]::GetBytes(48)
    $env:APP_JWT_SECRET = [Convert]::ToBase64String($randomBytes)
}

# Chave oficial de teste do Turnstile; defina TURNSTILE_SECRET_KEY para usar o widget real.
if (-not $env:TURNSTILE_SECRET_KEY) {
    $env:TURNSTILE_SECRET_KEY = "1x0000000000000000000000000000000AA"
}
if (-not $env:TURNSTILE_ALLOWED_HOSTNAMES) {
    $env:TURNSTILE_ALLOWED_HOSTNAMES = "localhost,127.0.0.1"
}

# A variável DEBUG é usada pelo frontend, mas ativa logs extensos no Spring Boot.
$previousDebug = $env:DEBUG
$env:DEBUG = $null

Push-Location $PSScriptRoot
try {
    & .\mvnw.cmd spring-boot:run
}
finally {
    Pop-Location
    $env:DEBUG = $previousDebug
}
