@echo off
set "JAVA_HOME=C:\Users\123\AppData\Roaming\.minecraft\runtime\java-runtime-delta\windows-x64\java-runtime-delta"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call gradlew.bat clean build %*
