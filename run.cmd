@echo off
REM Starts the program in UTF-8 so that Russian text is shown correctly.
REM Usage (from the project folder): .\run.cmd

chcp 65001 >nul <nul
cd /d "%~dp0"

REM Java search order: JAVA_HOME, then PATH, then the JRE bundled with the VS Code Java extension
set "JAVA="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA=%JAVA_HOME%\bin\java.exe"
if not defined JAVA for /f "delims=" %%P in ('where java 2^>nul ^<nul') do if not defined JAVA set "JAVA=%%P"
if not defined JAVA for /d %%D in ("%USERPROFILE%\.vscode\extensions\redhat.java-*") do for /d %%J in ("%%D\jre\*") do if exist "%%J\bin\java.exe" set "JAVA=%%J\bin\java.exe"

if not defined JAVA (
    echo Java not found. Install JDK 17 or newer.
    exit /b 1
)

if not exist "target\autoparts-team.jar" (
    echo target\autoparts-team.jar not found. Build the project first: .\mvnw.cmd clean package
    exit /b 1
)

"%JAVA%" -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dstdin.encoding=UTF-8 -jar target\autoparts-team.jar
