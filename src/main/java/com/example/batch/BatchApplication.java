package com.example.batch;

import cn.hutool.core.util.StrUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 注意: 如果需要在IDE中启动请使用DEBUGGER模式!!!
 */
@SpringBootApplication
@EnableScheduling
public class BatchApplication {

    /**
     * 默认 Job 名称（当没有指定 jobName 参数时使用）
     */
    private static final String DEFAULT_JOB_NAME = "breakpointJob";

    /**
     * 需要重跑 Job ID（从断点异常处重跑）
     */
    private static final String RUN_JOB_ID = "ABCD1234";

    public static void main(String[] args) {
        // 1. 检测启动环境 (IDE vs CLI)
            boolean isIde = System.getProperty("java.class.path").contains("idea_rt.jar")
                         || System.getProperty("sun.java.command").contains("com.intellij")
                         || System.getenv("IntelliJ_IDEA_SERVER_PORT") != null;

        // 2. 解析参数
        boolean hasJobName = false;
        boolean hasId = false;
        for (String arg : args) {
            if (arg.startsWith("jobName=")) hasJobName = true;
            if (arg.startsWith("id=")) hasId = true;
        }

        java.util.List<String> finalArgs = new java.util.ArrayList<>(java.util.Arrays.asList(args));

        // 3. 核心逻辑：区分环境处理
        if (isIde) {
            // IDE模式：为了开发方便，如果没有提供参数，自动注入默认值
            System.out.println(">>> IDE Environment Detected <<<");
            if (!hasJobName) {
                System.out.println("IDE Mode: Auto-injecting jobName=" + DEFAULT_JOB_NAME);
                finalArgs.add("jobName=" + DEFAULT_JOB_NAME);
            }
            if (!hasId && RUN_JOB_ID != null && !RUN_JOB_ID.trim().isEmpty()) {
                System.out.println("IDE Mode: Auto-injecting id=" + RUN_JOB_ID);
                finalArgs.add("id=" + RUN_JOB_ID);
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
        System.err.println("========================================");
        System.err.println("ERROR: jobName is required!");
        System.err.println("Usage: java -jar app.jar jobName=<name> [id=<id>]");
        System.err.println("Example 1 (New Instance): java -jar app.jar jobName=demoJob");
        System.err.println("Example 2 (Retry/Resume): java -jar app.jar jobName=demoJob id=12345");
        System.err.println("========================================");
    }

}
