@echo off 

if "%~1" == "" (goto stdin) else (goto arguments)

:arguments
if defined TEST_VARIABLE (echo Greetings %1! From ARG #1. TEST_VARIABLE=%TEST_VARIABLE%.) else (echo Greetings %1! From ARG #1.)
goto end

:stdin
set /p name=
if defined TEST_VARIABLE (echo Greetings %name%! From STDIN. TEST_VARIABLE=%TEST_VARIABLE%.) else (echo Greetings %name%! From STDIN.)

:end

