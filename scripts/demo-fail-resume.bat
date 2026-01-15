@echo off
REM ===============================================
REM Multi-DataSource Job - Failure & Resume Scenario
REM ===============================================
REM
REM 场景：Step3 失败，然后断点续传
REM 验证：元数据记录失败状态，重跑时从 Step3 继续
REM
echo.
echo ===============================================
echo 多数据源批处理作业 - 失败恢复场景
echo ===============================================
echo.
echo [阶段 1/2] 模拟 Step3 失败
echo.
pause

echo.
echo [执行] 运行作业（Step3 将抛出异常）...
echo.

java -jar target\batch-weaver-0.0.1-SNAPSHOT.jar ^
  jobName=multiDataSourceJob ^
  simulateFail=true ^
  id=9999

echo.
echo ===============================================
echo [预期结果] 作业应该失败！
echo ===============================================
echo.
echo [验证点]
echo   1. Step 1 (DB1): 成功完成
echo   2. Step 2 (DB2): 成功完成
echo   3. Step 3 (DB3): 失败（抛出异常）
echo   4. Step 4 (DB4): 未执行
echo   5. DB3 数据已回滚（事务撤销）
echo   6. 元数据表 BATCH_STEP_EXECUTION 记录 db3Step=FAILED
echo.
echo [数据库验证]
echo   - DB1: 有 5 条 DEMO_USER 记录
echo   - DB2: 有 3 条 DEMO_USER 记录（状态=ACTIVE）
echo   - DB3: 无数据（回滚）
echo   - DB4: 无数据（未执行）
echo.
pause

echo.
echo ===============================================
echo [阶段 2/2] 断点续传（修复后重跑）
echo ===============================================
echo.
echo [说明] 使用相同的 jobName + id，但设置 simulateFail=false
echo [预期] Spring Batch 将从 Step3 继续执行（跳过 Step1/2）
echo.
pause

echo.
echo [执行] 重新运行作业（不模拟失败）...
echo.

java -jar target\batch-weaver-0.0.1-SNAPSHOT.jar ^
  jobName=multiDataSourceJob ^
  simulateFail=false ^
  id=9999

echo.
echo ===============================================
echo [预期结果] 作业应该成功！
echo ===============================================
echo.
echo [验证点]
echo   1. Step 1 (DB1): 跳过（已完成）
echo   2. Step 2 (DB2): 跳过（已完成）
echo   3. Step 3 (DB3): 重新执行并成功
echo   4. Step 4 (DB4): 首次执行并成功
echo   5. 所有数据库均有数据
echo.
echo [数据库最终状态]
echo   - DB1: 5 条记录（第一次运行插入）
echo   - DB2: 3 条记录（第一次运行插入）
echo   - DB3: 1 条记录（第二次运行插入）
echo   - DB4: 1 条记录（第二次运行插入）
echo.
echo [元数据验证]
echo   - BATCH_JOB_EXECUTION: 两条记录（id=9999）
echo   - BATCH_STEP_EXECUTION: db1Step/db2Step/db3Step/db4Step 均为 COMPLETED
echo.
pause
