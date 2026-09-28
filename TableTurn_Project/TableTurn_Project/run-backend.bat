@echo off
cd /d "%~dp0backend"
where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven was not found in PATH. Install Maven 3.9+ or open backend\pom.xml in IntelliJ.
  pause
  exit /b 1
)
mvn spring-boot:run
