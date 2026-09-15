@echo off
setlocal
cd /d "%~dp0"
if not exist "agenda-citas-ui\target\agenda-citas-ui-1.0-SNAPSHOT.jar" (
    echo Primero ejecuta compilar.cmd o Maven install desde Eclipse.
    pause
    exit /b 1
)
java -jar "agenda-citas-ui\target\agenda-citas-ui-1.0-SNAPSHOT.jar"
if errorlevel 1 pause
