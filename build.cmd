@echo off
setlocal

if exist "%JAVA_HOME%" goto gothome

:nohome

for /F %%i in ('dir /b "c:\program files\java\jdk1.6*"') do set JAVA_HOME=c:\program files\java\%%i

if exist "%JAVA_HOME%" goto gothome

echo please set JAVA_HOME pointing to the Java 1.6 JDK installation directory 

goto end

:gothome

set classpath=%JAVA_HOME%/lib/tools.jar;contrib/ant/ant.jar

goto build


:build

"%JAVA_HOME%\bin\java" org.apache.tools.ant.Main %1 %2 %3 %4

:end

endlocal

