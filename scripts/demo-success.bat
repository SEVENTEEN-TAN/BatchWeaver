@echo off
REM ========================================
REM Multi-DataSource Job - Success Scenario
REM ========================================
REM
REM 场景：所有 Step 正常执行完成
REM 预期：DB1/DB2/DB3/DB4 均有数据插入
REM
echo.
echo ========================================
echo 多数据源批处理作业 - 成功场景
echo ========================================
echo.
echo [提示] 此脚本将运行 multiDataSourceJob
echo [提示] simulateFail=false（Step3 不会失败）
echo.
pause

echo.
echo [执行] 正在启动作业...
echo.

java -jar target\batch-weaver-0.0.1-SNAPSHOT.jar ^
  jobName=multiDataSourceJob ^
  simulateFail=false

echo.
echo ========================================
echo 作业执行完成！
echo ========================================
echo.
echo [验证] 请检查日志确认：
echo   - Step 1 (DB1): 已插入 5 条记录
echo   - Step 2 (DB2): 已激活 3 条记录
echo   - Step 3 (DB3): 发票对账成功
echo   - Step 4 (DB4): 汇总数据已推送
echo.
pause
