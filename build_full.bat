@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d D:\AI_Tools\OpenCode\IntentBridge
echo Using Java: 
java -version
echo Building...
call gradlew.bat assembleDebug --no-daemon
echo.
echo Build complete.
pause
