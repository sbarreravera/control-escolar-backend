[CmdletBinding()]
param(
    [switch]$ResetDatabasePassword,
    [switch]$ResetMailPassword,
    [switch]$SetupOnly
)

$ErrorActionPreference = "Stop"

if ($PSVersionTable.PSVersion.Major -lt 5) {
    throw "Este script requiere Windows PowerShell 5.1 o PowerShell 7."
}

$databaseUrl = "jdbc:postgresql://localhost:5432/control_escolar"
$databaseUsername = "sambarve"
$guardianCookieSecure = "false"
$mailEnabled = "true"
$mailUsername = "notificaciones.isamar@gmail.com"
$mailPortalLoginUrl = "http://localhost:4200/#/guardian/login"
$mailPortalPasswordResetUrl = "http://localhost:4200/#/guardian/reset-password"

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
Set-UserEnvironmentVariable -Name "MAIL_ENABLED" -Value $mailEnabled
Set-UserEnvironmentVariable -Name "MAIL_USERNAME" -Value $mailUsername
Set-UserEnvironmentVariable -Name "MAIL_PORTAL_LOGIN_URL" -Value $mailPortalLoginUrl
Set-UserEnvironmentVariable -Name "MAIL_PORTAL_PASSWORD_RESET_URL" -Value $mailPortalPasswordResetUrl

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

$mailPassword = [Environment]::GetEnvironmentVariable("MAIL_PASSWORD", "User")

if ($ResetMailPassword -or [string]::IsNullOrWhiteSpace($mailPassword)) {
    $secureMailPassword = Read-Host "Contraseña de aplicación de Gmail" -AsSecureString
    $mailCredential = [System.Management.Automation.PSCredential]::new("gmail-app-password", $secureMailPassword)
    $mailPassword = $mailCredential.GetNetworkCredential().Password
    $mailPassword = ($mailPassword -replace '\s', '')

    if ([string]::IsNullOrWhiteSpace($mailPassword)) {
        throw "MAIL_PASSWORD no puede quedar vacía."
    }

    Set-UserEnvironmentVariable -Name "MAIL_PASSWORD" -Value $mailPassword
    Remove-Variable secureMailPassword, mailCredential
}
else {
    Set-Item -Path "Env:MAIL_PASSWORD" -Value $mailPassword
}

Write-Host "Configuración local lista:"
Write-Host "  DB_URL: configurada"
Write-Host "  DB_USERNAME: configurada"
Write-Host "  DB_PASSWORD: configurada (valor oculto)"
Write-Host "  GUARDIAN_COOKIE_SECURE: configurada"
Write-Host "  MAIL_ENABLED: $env:MAIL_ENABLED"
Write-Host "  MAIL_USERNAME: $env:MAIL_USERNAME"
Write-Host "  MAIL_PASSWORD: configurada (valor oculto)"
Write-Host "  MAIL_PORTAL_LOGIN_URL: $env:MAIL_PORTAL_LOGIN_URL"
Write-Host "  MAIL_PORTAL_PASSWORD_RESET_URL: $env:MAIL_PORTAL_PASSWORD_RESET_URL"

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
    Remove-Variable databasePassword, mailPassword -ErrorAction SilentlyContinue
}
