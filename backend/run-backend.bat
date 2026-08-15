@echo off
echo Starting TruthLens Spring Boot Backend Server...
cd /d "%~dp0"
".\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run
pause
