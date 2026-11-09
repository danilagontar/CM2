@echo off
rem Build (Maven) and run depviz on Windows.
rem All arguments are passed to the program, e.g.:
rem   run.bat --package Newtonsoft.Json --package-version 13.0.3
setlocal
chcp 65001 >nul
set "DIR=%~dp0"
call mvn -q -f "%DIR%pom.xml" package -DskipTests
if errorlevel 1 exit /b 1
java -jar "%DIR%target\depviz.jar" %*
exit /b %errorlevel%
