@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  food-order startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and FOOD_ORDER_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\food-order.jar;%APP_HOME%\lib\kotlin-stdlib-jdk8-1.3.41.jar;%APP_HOME%\lib\telegrambots-4.4.0.1.jar;%APP_HOME%\lib\exposed-0.17.3.jar;%APP_HOME%\lib\postgresql-42.2.6.jar;%APP_HOME%\lib\kotlin-stdlib-jdk7-1.3.41.jar;%APP_HOME%\lib\kotlinx-coroutines-core-1.3.0-M1.jar;%APP_HOME%\lib\kotlin-reflect-1.3.50.jar;%APP_HOME%\lib\kotlin-stdlib-1.3.50.jar;%APP_HOME%\lib\kotlin-stdlib-common-1.3.50.jar;%APP_HOME%\lib\telegrambots-meta-4.4.0.1.jar;%APP_HOME%\lib\jackson-jaxrs-json-provider-2.9.9.jar;%APP_HOME%\lib\jersey-media-json-jackson-2.29.jar;%APP_HOME%\lib\jackson-module-jaxb-annotations-2.9.9.jar;%APP_HOME%\lib\jackson-jaxrs-base-2.9.9.jar;%APP_HOME%\lib\jackson-databind-2.9.9.jar;%APP_HOME%\lib\jackson-annotations-2.9.9.jar;%APP_HOME%\lib\jersey-hk2-2.29.jar;%APP_HOME%\lib\jersey-container-grizzly2-http-2.29.jar;%APP_HOME%\lib\jersey-server-2.29.jar;%APP_HOME%\lib\json-20180813.jar;%APP_HOME%\lib\httpmime-4.5.9.jar;%APP_HOME%\lib\httpclient-4.5.9.jar;%APP_HOME%\lib\commons-io-2.6.jar;%APP_HOME%\lib\log4j-core-2.12.0.jar;%APP_HOME%\lib\joda-time-2.10.2.jar;%APP_HOME%\lib\slf4j-api-1.7.25.jar;%APP_HOME%\lib\h2-1.4.199.jar;%APP_HOME%\lib\guice-4.2.2.jar;%APP_HOME%\lib\guava-28.0-jre.jar;%APP_HOME%\lib\jackson-core-2.9.9.jar;%APP_HOME%\lib\jersey-client-2.29.jar;%APP_HOME%\lib\jersey-media-jaxb-2.29.jar;%APP_HOME%\lib\jersey-common-2.29.jar;%APP_HOME%\lib\hk2-locator-2.5.0.jar;%APP_HOME%\lib\jersey-entity-filtering-2.29.jar;%APP_HOME%\lib\hk2-api-2.5.0.jar;%APP_HOME%\lib\hk2-utils-2.5.0.jar;%APP_HOME%\lib\jakarta.inject-2.5.0.jar;%APP_HOME%\lib\grizzly-http-server-2.4.4.jar;%APP_HOME%\lib\jakarta.ws.rs-api-2.1.5.jar;%APP_HOME%\lib\jakarta.annotation-api-1.3.4.jar;%APP_HOME%\lib\validation-api-2.0.1.Final.jar;%APP_HOME%\lib\httpcore-4.4.11.jar;%APP_HOME%\lib\commons-logging-1.2.jar;%APP_HOME%\lib\commons-codec-1.11.jar;%APP_HOME%\lib\log4j-api-2.12.0.jar;%APP_HOME%\lib\annotations-13.0.jar;%APP_HOME%\lib\javax.inject-1.jar;%APP_HOME%\lib\aopalliance-1.0.jar;%APP_HOME%\lib\failureaccess-1.0.1.jar;%APP_HOME%\lib\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;%APP_HOME%\lib\jsr305-3.0.2.jar;%APP_HOME%\lib\checker-qual-2.8.1.jar;%APP_HOME%\lib\error_prone_annotations-2.3.2.jar;%APP_HOME%\lib\j2objc-annotations-1.3.jar;%APP_HOME%\lib\animal-sniffer-annotations-1.17.jar;%APP_HOME%\lib\osgi-resource-locator-1.0.3.jar;%APP_HOME%\lib\aopalliance-repackaged-2.5.0.jar;%APP_HOME%\lib\javassist-3.22.0-CR2.jar;%APP_HOME%\lib\grizzly-http-2.4.4.jar;%APP_HOME%\lib\grizzly-framework-2.4.4.jar

@rem Execute food-order
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %FOOD_ORDER_OPTS%  -classpath "%CLASSPATH%" org.order.BotLauncherKt %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable FOOD_ORDER_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%FOOD_ORDER_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
