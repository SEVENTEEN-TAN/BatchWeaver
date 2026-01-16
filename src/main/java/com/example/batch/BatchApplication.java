package com.example.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 注意: 如果需要在IDE中启动请使用DEBUGGER模式!!!
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
public class BatchApplication {

    /**
     * IDE 调试默认参数（仅在检测到 IDE 环境时生效）
     *
     * <p>通过直接修改这些常量，可以在 IDE 中一键配置调试场景：</p>
     * <ul>
     *   <li>{@code IDE_DEFAULT_JOB_NAME}：默认 jobName（为空则不自动注入）</li>
     *   <li>{@code IDE_DEFAULT_ID}：历史执行 id（例如 RESUME / ISOLATED 需要指定的 executionId）</li>
     *   <li>{@code IDE_DEFAULT_MODE}：执行模式，例如 STANDARD / RESUME / SKIP_FAIL / ISOLATED</li>
     *   <li>{@code IDE_DEFAULT_TARGET_STEPS}：ISOLATED 模式下的 _target_steps，示例："step1,step2"</li>
     *   <li>{@code IDE_DEFAULT_SIMULATE_FAIL}：demo 用的 simulateFail 参数，示例："step2"</li>
     * </ul>
     */
    private static final String IDE_DEFAULT_JOB_NAME = "demoJob";
    private static final Long   IDE_DEFAULT_ID = null;
    private static final String IDE_DEFAULT_MODE = null;
    private static final String IDE_DEFAULT_TARGET_STEPS = null;
    private static final String IDE_DEFAULT_SIMULATE_FAIL = null;

    public static void main(String[] args) {
        // 1. 检测启动环境 (IDE vs CLI)
        boolean isIde = System.getProperty("java.class.path").contains("idea_rt.jar")
                || System.getProperty("sun.java.command").contains("com.intellij")
                || System.getenv("IntelliJ_IDEA_SERVER_PORT") != null;

        // 2. 解析参数
        boolean hasJobName = false;
        boolean hasId = false;
        boolean hasMode = false;
        boolean hasTargetSteps = false;
        boolean hasSimulateFail = false;
        for (String arg : args) {
            if (arg.startsWith("jobName=")) hasJobName = true;
            if (arg.startsWith("id=")) hasId = true;
            if (arg.startsWith("_mode=")) hasMode = true;
            if (arg.startsWith("_target_steps=")) hasTargetSteps = true;
            if (arg.startsWith("simulateFail=")) hasSimulateFail = true;
        }

        java.util.List<String> finalArgs = new java.util.ArrayList<>(java.util.Arrays.asList(args));

        // 3. 核心逻辑：区分环境处理
        if (isIde) {
            // IDE模式：为了开发方便，如果没有提供参数，自动注入默认值
            log.info(">>> IDE Environment Detected <<<");
            if (!hasJobName && IDE_DEFAULT_JOB_NAME != null) {
                log.info("IDE Mode: Auto-injecting jobName={}", IDE_DEFAULT_JOB_NAME);
                finalArgs.add("jobName=" + IDE_DEFAULT_JOB_NAME);
            }
            if (!hasId && IDE_DEFAULT_ID != null) {
                log.info("IDE Mode: Auto-injecting id={}", IDE_DEFAULT_ID);
                finalArgs.add("id=" + IDE_DEFAULT_ID);
            }
            if (!hasMode && IDE_DEFAULT_MODE != null) {
                log.info("IDE Mode: Auto-injecting _mode={}", IDE_DEFAULT_MODE);
                finalArgs.add("_mode=" + IDE_DEFAULT_MODE);
            }
            if (!hasTargetSteps && IDE_DEFAULT_TARGET_STEPS != null) {
                log.info("IDE Mode: Auto-injecting _target_steps={}", IDE_DEFAULT_TARGET_STEPS);
                finalArgs.add("_target_steps=" + IDE_DEFAULT_TARGET_STEPS);
            }
            if (!hasSimulateFail && IDE_DEFAULT_SIMULATE_FAIL != null) {
                log.info("IDE Mode: Auto-injecting simulateFail={}", IDE_DEFAULT_SIMULATE_FAIL);
                finalArgs.add("simulateFail=" + IDE_DEFAULT_SIMULATE_FAIL);
            }
        } else {
            // CLI模式：严格校验
            if (!hasJobName) {
                printUsage();
                System.exit(1);
                return;
            }
            // CLI模式下 id 是可选的，不自动注入 IDE 的调试 ID
            // 如果用户没传 id，DynamicJobRunner 会自动生成新 id (新实例)
        }

        SpringApplication.run(BatchApplication.class, finalArgs.toArray(new String[0]));
    }

    private static void printUsage() {
        log.error("========================================");
        log.error("ERROR: jobName is required!");
        log.error("Usage: java -jar app.jar jobName=<name> [id=<id>]");
        log.error("Example 1 (New Instance): java -jar app.jar jobName=demoJob");
        log.error("Example 2 (Retry/Resume): java -jar app.jar jobName=demoJob id=12345");
        log.error("========================================");
    }

}
