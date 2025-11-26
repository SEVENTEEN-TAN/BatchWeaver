@echo off
REM BatchWeaver Job Runner for Windows
REM Usage: run-job.bat jobName=myJob [param1=value1 param2=value2 ...]

cd /d "%~dp0.."
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar %*
