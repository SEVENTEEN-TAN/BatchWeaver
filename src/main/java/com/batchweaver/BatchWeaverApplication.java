package com.batchweaver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Batch 5.x Multi-Datasource System
 * <p>
 * 核心特性：
 * - 4 个独立数据源（db1-db4）
 * - 元数据事务（tm1）与业务事务（tm2/tm3/tm4）隔离
 * - 基于注解的 FlatFile 处理框架
 * <p>
 * 启动参数说明（由 {@code JobLauncherRunner} 解析）：
 * <ul>
 *   <li>{@code --job.name=<jobName>}：必填，指定要执行的 Job 名称</li>
 *   <li>{@code --job.id=<executionId>}：可选，断点续传（重启 FAILED/STOPPED 的执行）</li>
 *   <li>{@code --str=<any>}：可选，自定义业务参数（仅在新实例执行时生效）</li>
 *   <li>其他 {@code --key=value}：可选，作为 JobParameters 传入（仅在新实例执行时生效）</li>
 * </ul>
 * 注意：当传入 {@code --job.id} 进入断点续传模式时，只允许 {@code --job.name} 与 {@code --job.id}，
 * 其他参数会被拒绝（这是 {@code JobLauncherRunner} 的校验规则）。
 * <p>
 * IDE 启动说明：
 * <ul>
 *   <li>默认：在检测到 IntelliJ IDEA 环境时，会自动注入本类 main() 中写死的调试默认参数</li>
 *   <li>显式开关（优先级最高）：JVM 参数 {@code -Dbatchweaver.ideDebug=true|false} 或环境变量 {@code BATCHWEAVER_IDE_DEBUG=true|false}</li>
 *   <li>命令行/容器环境：不设置开关时不会注入默认参数，完全使用外部传入的 args</li>
 * </ul>
 */
@SpringBootApplication
public class BatchWeaverApplication {

    public static void main(String[] args) {
        Boolean forcedIdeDebug = parseBooleanNullable(System.getProperty("batchweaver.ideDebug"));
        if (forcedIdeDebug == null) {
            forcedIdeDebug = parseBooleanNullable(System.getenv("BATCHWEAVER_IDE_DEBUG"));
        }
        boolean ideDebug = forcedIdeDebug != null ? forcedIdeDebug : isIdeEnvironment();
        String jobName = "demoJob";
        String jobid = "";
        String str = "debug";

        String[] effectiveArgs = args;
        if (ideDebug) {
            java.util.List<String> mergedArgs = new java.util.ArrayList<>();
            if (args != null) {
                java.util.Collections.addAll(mergedArgs, args);
            }

            if (jobName != null
                    && !jobName.isBlank()
                    && mergedArgs.stream().noneMatch(a -> a.startsWith("--job.name="))) {
                mergedArgs.add("--job.name=" + jobName);
            }
            if (jobid != null && !jobid.isBlank() && mergedArgs.stream().noneMatch(a -> a.startsWith("--job.id="))) {
                mergedArgs.add("--job.id=" + jobid);
            }
            if ((jobid == null || jobid.isBlank())
                    && str != null
                    && !str.isBlank()
                    && mergedArgs.stream().noneMatch(a -> a.startsWith("--str="))) {
                mergedArgs.add("--str=" + str);
            }

            effectiveArgs = mergedArgs.toArray(new String[0]);
        }

        SpringApplication.run(BatchWeaverApplication.class, effectiveArgs);
    }

    private static Boolean parseBooleanNullable(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim().toLowerCase();
        if (v.isEmpty()) {
            return null;
        }
        if (v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("y") || v.equals("on")) {
            return true;
        }
        if (v.equals("false") || v.equals("0") || v.equals("no") || v.equals("n") || v.equals("off")) {
            return false;
        }
        return null;
    }

    private static boolean isIdeEnvironment() {
        String classPath = System.getProperty("java.class.path", "");
        if (classPath.contains("idea_rt.jar")) {
            return true;
        }

        String command = System.getProperty("sun.java.command", "");
        if (command.contains("com.intellij") || command.contains("org.jetbrains")) {
            return true;
        }

        if (System.getenv("IntelliJ_IDEA_SERVER_PORT") != null) {
            return true;
        }

        return false;
    }
}
