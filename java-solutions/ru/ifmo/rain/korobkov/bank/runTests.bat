@echo off

call compileBank.bat

if %ERRORLEVEL% EQU 0 (
   echo Success
) else (
   exit /b %errorlevel%
)

echo Running tests...


set LIB=../../../../../../lib
set CP=./_build;%LIB%/junit-platform-launcher-1.4.2.jar;%LIB%/junit-platform-commons-1.4.2.jar;%LIB%/junit-platform-engine-1.4.2.jar;^
%LIB%/junit-jupiter-engine-5.4.2.jar;%LIB%/junit-jupiter-params-5.4.2.jar;%LIB%/junit-jupiter-api-5.4.2.jar;^
%LIB%/opentest4j-1.1.1.jar

java -cp %CP% ru.ifmo.rain.korobkov.bank.BankTests

if %ERRORLEVEL% EQU 0 (
   echo Success
) else (
   echo Fail
   exit /b %errorlevel%
)

