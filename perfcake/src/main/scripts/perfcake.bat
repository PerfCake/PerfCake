@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM PerfCake Start Up Batch script
@REM
@REM Required ENV vars:
@REM JAVA_HOME - location of a JDK home dir
@REM
@REM ----------------------------------------------------------------------------

@echo off

@REM set %HOME% to equivalent of $HOME
if "%HOME%" == "" (set "HOME=%HOMEDRIVE%%HOMEPATH%")

set ERROR_CODE=0

@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal

@REM ==== START VALIDATION ====
if not "%JAVA_HOME%" == "" goto OkJHome

echo.
echo ERROR: JAVA_HOME not found in your environment.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation
echo.
goto error

:OkJHome
if exist "%JAVA_HOME%\bin\java.exe" goto chkMHome

echo.
echo ERROR: JAVA_HOME is set to an invalid directory.
echo JAVA_HOME = "%JAVA_HOME%"
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation
echo.
goto error

:chkMHome
if not "%PERFCAKE_HOME%"=="" goto valPCHome

if "%OS%"=="Windows_NT" SET "PERFCAKE_HOME=%~dp0.."
if "%OS%"=="WINNT" SET "PERFCAKE_HOME=%~dp0.."
if not "%PERFCAKE_HOME%"=="" goto valPCHome

echo.
echo ERROR: PERFCAKE_HOME not found in your environment.
echo Please set the PERFCAKE_HOME variable in your environment to match the
echo location of the PerfCake installation
echo.
goto error

:valPCHome

:stripPCHome
if not "_%PERFCAKE_HOME:~-1%"=="_\" goto checkPCBat
set "PERFCAKE_HOME=%PERFCAKE_HOME:~0,-1%"
goto stripPCHome

:checkPCBat
if exist "%PERFCAKE_HOME%\bin\perfcake.bat" goto init

echo.
echo ERROR: PERFCAKE_HOME is set to an invalid directory.
echo PERFCAKE_HOME = "%PERFCAKE_HOME%"
echo Please set the PERFCAKE_HOME variable in your environment to match the
echo location of the PerfCake installation
echo.
goto error
@REM ==== END VALIDATION ====

:init
@REM Decide how to startup depending on the version of windows

@REM -- Windows NT with Novell Login
if "%OS%"=="WINNT" goto WinNTNovell

@REM -- Win98ME
if NOT "%OS%"=="Windows_NT" goto Win9xArg

:WinNTNovell

@REM -- 4NT shell
if "%@eval[2+2]" == "4" goto 4NTArgs

@REM -- Regular WinNT shell
set PERFCAKE_CMD_LINE_ARGS=%*
goto endInit

@REM The 4NT Shell from jp software
:4NTArgs
set PERFCAKE_CMD_LINE_ARGS=%$
goto endInit

:Win9xArg
@REM Slurp the command line arguments.  This loop allows for an unlimited number
@REM of agruments (up to the command line limit, anyway).
set PERFCAKE_CMD_LINE_ARGS=
:Win9xApp
if %1a==a goto endInit
set PERFCAKE_CMD_LINE_ARGS=%PERFCAKE_CMD_LINE_ARGS% %1
shift
goto Win9xApp

@REM Reaching here means variables are defined and arguments have been captured
:endInit
SET PERFCAKE_JAVA_EXE="%JAVA_HOME%\bin\java.exe"

@REM Check Java version
for /f "tokens=1-2" "delims=.-_" %%j in ('"%PERFCAKE_JAVA_EXE%" -version 2>&1') do set "jver=%%j%%k%%l%%m"
if not %jver% LSS 18 goto runPERFCAKE
echo Unsupported Java version. PerfCake requires Java 8 and higher.
goto error

@REM Start PerfCake
:runPERFCAKE
for /f "delims=" %%i in ('dir /s /b "%PERFCAKE_HOME%"\lib\perfcake*.jar') do set PERFCAKE_JAR=%%i && goto pcJarFound
:pcJarFound
if not "%PERFCAKE_JAR%"=="" goto execPerfCake
echo ERROR: could not find PerfCake jar file (%PERFCAKE_HOME%\lib\perfcake*.jar)
goto error

:execPerfCake
cd "%PERFCAKE_HOME%"

%PERFCAKE_JAVA_EXE% -Dlog4j.configurationFile="file:///%PERFCAKE_HOME%\log4j2.xml" -Djava.ext.dirs="%JAVA_HOME%\lib\ext;%JAVA_HOME%\jre\lib\ext;%PERFCAKE_HOME%\lib\ext" -jar "%PERFCAKE_JAR%" %PERFCAKE_CMD_LINE_ARGS%
if ERRORLEVEL 1 goto error
goto end

:error
if "%OS%"=="Windows_NT" @endlocal
if "%OS%"=="WINNT" @endlocal
set ERROR_CODE=1

:end
@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" goto endNT
if "%OS%"=="WINNT" goto endNT

@REM For old DOS remove the set variables from ENV - we assume they were not set
@REM before we started - at least we don't leave any baggage around
set PERFCAKE_JAVA_EXE=
set PERFCAKE_CMD_LINE_ARGS=
goto postExec

:endNT
@endlocal & set ERROR_CODE=%ERROR_CODE%

:postExec

@REM pause the batch file if PERFCAKE_BATCH_PAUSE is set to 'on'
if "%PERFCAKE_BATCH_PAUSE%" == "on" pause

if "%PERFCAKE_TERMINATE_CMD%" == "on" exit %ERROR_CODE%

cmd /C exit /B %ERROR_CODE%
