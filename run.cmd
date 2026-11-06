@echo off
REM Starts the JavaFX application (KR2) with Maven: mvnw javafx:run
REM Usage (from the project folder): .\run.cmd

chcp 65001 >nul <nul
cd /d "%~dp0"

if not exist "src\main\resources\database.properties" (
    echo src\main\resources\database.properties not found.
    echo Copy src\main\resources\database.properties.example to database.properties and set your PostgreSQL password.
    exit /b 1
)

REM Java search order: JAVA_HOME, then PATH, then the JDK bundled with the VS Code Java extension
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto run
for /f "delims=" %%P in ('where java 2^>nul ^<nul') do (
    for %%D in ("%%~dpP..") do set "JAVA_HOME=%%~fD"
    goto run
)
for /d %%D in ("%USERPROFILE%\.vscode\extensions\redhat.java-*") do for /d %%J in ("%%D\jre\*") do if exist "%%J\bin\java.exe" set "JAVA_HOME=%%J"
if not defined JAVA_HOME (
    echo Java not found. Install JDK 21 or newer.
    exit /b 1
)

:run
echo Using Java: %JAVA_HOME%
call "%~dp0mvnw.cmd" -q javafx:run
