@echo off
setlocal

set JAR=target\batch-weaver-0.0.1-SNAPSHOT.jar

echo ========================================
echo Testing Demo Jobs
echo ========================================

echo.
echo [1/6] multiDsDemoJob - STANDARD
java -jar %JAR% jobName=multiDsDemoJob
timeout /t 2 /nobreak > nul

echo.
echo [2/6] failureRecoveryDemoJob - STANDARD
java -jar %JAR% jobName=failureRecoveryDemoJob
timeout /t 2 /nobreak > nul

echo.
echo [3/6] skipFailDemoJob - SKIP_FAIL
java -jar %JAR% jobName=skipFailDemoJob _mode=SKIP_FAIL
timeout /t 2 /nobreak > nul

echo.
echo [4/6] dataTransferDemoJob - STANDARD
java -jar %JAR% jobName=dataTransferDemoJob
timeout /t 2 /nobreak > nul

echo.
echo [5/6] conditionalFlowDemoJob - flow=success
java -jar %JAR% jobName=conditionalFlowDemoJob flow=success
timeout /t 2 /nobreak > nul

echo.
echo [6/6] chunkJob - STANDARD
java -jar %JAR% jobName=chunkJob
timeout /t 2 /nobreak > nul

echo.
echo ========================================
echo All Demo Jobs tested!
echo ========================================
pause
