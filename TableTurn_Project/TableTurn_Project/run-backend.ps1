Set-Location "$PSScriptRoot\backend"
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "Maven was not found in PATH. Install Maven 3.9+ or open backend/pom.xml in IntelliJ."
    exit 1
}
mvn spring-boot:run
