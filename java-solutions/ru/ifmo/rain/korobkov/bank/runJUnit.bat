@echo off

call compileBank.bat

if %ERRORLEVEL% EQU 0 (
   echo Success
) else (
   exit /b %errorlevel%
)

echo Running tests...

set LIB=../../../../../../lib

java -jar %LIB%/junit-platform-console-standalone-1.4.2.jar --class-path _build --scan-class-path
if %ERRORLEVEL% EQU 0 (
   echo Success
) else (
   exit /b %errorlevel%
)