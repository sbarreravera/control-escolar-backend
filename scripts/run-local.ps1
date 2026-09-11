[CmdletBinding()]
param(
    [switch]$ResetDatabasePassword,
    [switch]$SetupOnly
)

$ErrorActionPreference = "Stop"

if ($PSVersionTable.PSVersion.Major -lt 5) {
    throw "Este script requiere Windows PowerShell 5.1 o PowerShell 7."
}

$databaseUrl = "jdbc:postgresql://localhost:5432/control_escolar"
$databaseUsername = "sambarve"
$guardianCookieSecure = "false"

function Set-UserEnvironmentVariable {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,

        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    [Environment]::SetEnvironmentVariable($Name, $Value, "User")
    Set-Item -Path "Env:$Name" -Value $Value
}

Set-UserEnvironmentVariable -Name "DB_URL" -Value $databaseUrl
Set-UserEnvironmentVariable -Name "DB_USERNAME" -Value $databaseUsername
Set-UserEnvironmentVariable -Name "GUARDIAN_COOKIE_SECURE" -Value $guardianCookieSecure

$databasePassword = [Environment]::GetEnvironmentVariable("DB_PASSWORD", "User")

if ($ResetDatabasePassword -or [string]::IsNullOrWhiteSpace($databasePassword)) {
    $securePassword = Read-Host "Contraseña local de PostgreSQL" -AsSecureString
    $credential = [System.Management.Automation.PSCredential]::new("local-database", $securePassword)
    $databasePassword = $credential.GetNetworkCredential().Password

    if ([string]::IsNullOrWhiteSpace($databasePassword)) {
        throw "DB_PASSWORD no puede quedar vacía."
    }

    Set-UserEnvironmentVariable -Name "DB_PASSWORD" -Value $databasePassword
    Remove-Variable securePassword, credential
}
else {
    Set-Item -Path "Env:DB_PASSWORD" -Value $databasePassword
}

Write-Host "Configuración local lista:"
Write-Host "  DB_URL: configurada"
Write-Host "  DB_USERNAME: configurada"
Write-Host "  DB_PASSWORD: configurada (valor oculto)"
Write-Host "  GUARDIAN_COOKIE_SECURE: configurada"

if ($SetupOnly) {
    Write-Host "Abre una terminal nueva para que otras aplicaciones hereden las variables."
    return
}

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$mavenWrapper = Join-Path $repositoryRoot "mvnw.cmd"

if (-not (Test-Path -LiteralPath $mavenWrapper)) {
    throw "No se encontró mvnw.cmd en $repositoryRoot."
}

Push-Location $repositoryRoot
try {
    & $mavenWrapper spring-boot:run
}
finally {
    Pop-Location
    Remove-Variable databasePassword -ErrorAction SilentlyContinue
}
