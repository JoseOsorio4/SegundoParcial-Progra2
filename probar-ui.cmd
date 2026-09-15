@echo off
setlocal
cd /d "%~dp0"
if not exist "agenda-citas-ui\target\agenda-citas-ui-1.0-SNAPSHOT.jar" (
    echo Primero ejecuta compilar.cmd.
    exit /b 1
)
java -Dfile.encoding=UTF-8 --class-path "agenda-citas-ui\target\agenda-citas-ui-1.0-SNAPSHOT.jar" scripts\PruebaInterfaz.java
exit /b %errorlevel%
