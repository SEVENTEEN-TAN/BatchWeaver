package com.batchweaver.core.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Job 启动入口 - 支持通过命令行参数触发 Job 和断点续传
 * <p>
 * 实现 ApplicationRunner 接口，使用 Spring Boot 标准的 ApplicationArguments 解析命令行参数
 * <p>
 * 使用方式：
 * <pre>
 * # 执行指定 Job
 * java -jar batchweaver.jar --job.name=conditionalFlowJob
 *
 * # 执行 Job 并传递业务参数
 * java -jar batchweaver.jar --job.name=conditionalFlowJob --data=20250625 --input.file=data/input/users.txt
 *
 * # 断点续传（重启失败的 Job，使用原参数）
 * java -jar batchweaver.jar --job.name=conditionalFlowJob --job.id=12345
 * </pre>
 * <p>
 * 参数说明：
 * <ul>
 *   <li>--job.name: Job 名称（必填）</li>
 *   <li>--job.id: Job 执行 ID（可选，用于断点续传）</li>
 *   <li>其他参数: 作为 JobParameters 传递给 Job（仅执行新Job时有效）</li>
 * </ul>
 * <p>
 * 执行模式：
 * <ul>
 *   <li>新Job执行: --job.name=xxx [--key=value ...] - 启动新的 Job 实例</li>
 *   <li>断点续传: --job.name=xxx --job.id=xxx - 使用原 JobParameters 重启</li>
 * </ul>
 * <p>
 * 断点续传机制：
 * <ul>
 *   <li>使用 JobOperator.restart() 重启指定的 Job 实例</li>
 *   <li>只有状态为 FAILED 或 STOPPED 的 Job 才能重启</li>
 *   <li>Spring Batch 会使用原 JobParameters，从上次失败的 checkpoint 继续</li>
 *   <li>断点续传时不允许传入新参数（需要新参数应启动新Job实例）</li>
 * </ul>
 * <p>
 * Job 内部获取参数：
 * <pre>
 * // 方式1: @Value 注入（推荐）
 * &#64;Value("#{jobParameters['data']}")
 * private String dataDate;
 *
 * // 方式2: ChunkContext 获取
 * JobParameters params = chunkContext.getStepContext().getStepExecution()
 *     .getJobExecution().getJobParameters();
 * String dataDate = params.getString("data");
 * </pre>
 */
@Component
@Profile("!test") // 测试环境不启用，避免与 JUnit 测试冲突
public class JobLauncherRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(JobLauncherRunner.class);

    private static final String PARAM_JOB_NAME = "job.name";
    private static final String PARAM_JOB_ID = "job.id";

    // Spring Batch 元数据表
    private static final String[] BATCH_METADATA_TABLES = {
            "BATCH_JOB_INSTANCE",
            "BATCH_JOB_EXECUTION",
            "BATCH_JOB_EXECUTION_PARAMS",
            "BATCH_STEP_EXECUTION",
            "BATCH_STEP_EXECUTION_CONTEXT",
            "BATCH_JOB_EXECUTION_CONTEXT"
    };

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private JobRegistry jobRegistry;

    @Autowired
    @Qualifier("dataSource1")
    private DataSource dataSource1;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        printBanner();
        validateMetadataTables();

        // 检查是否提供了 --job.name
        if (!args.getOptionNames().contains(PARAM_JOB_NAME)) {
            printUsage();
            printAvailableJobs();
            return;
        }

        String jobName = getRequiredOption(args, PARAM_JOB_NAME);
        Job job = findJobByName(jobName);

        if (job == null) {
            log.error("Job not found: {}", jobName);
            printAvailableJobs();
            System.exit(1);
            return;
        }

        // 模式2: 断点续传（重启指定的 Job 实例）
        if (args.getOptionNames().contains(PARAM_JOB_ID)) {
            validateRestartParams(args);
            long jobExecutionId = parseJobExecutionId(getRequiredOption(args, PARAM_JOB_ID));
            restartJob(jobExecutionId);
        } else {
            // 模式1: 执行新的 Job 实例
            Map<String, String> jobParams = extractJobParameters(args, PARAM_JOB_NAME);
            launchNewJob(job, jobParams);
        }
    }

    /**
     * 提取 Job 参数（排除控制参数）
     */
    private Map<String, String> extractJobParameters(ApplicationArguments args, String... excludeKeys) {
        Set<String> exclude = Set.of(excludeKeys);
        Map<String, String> params = new LinkedHashMap<>();

        args.getOptionNames().forEach(key -> {
            if (!exclude.contains(key)) {
                List<String> values = args.getOptionValues(key);
                if (values != null && !values.isEmpty()) {
                    params.put(key, values.get(0));
                }
            }
        });

        return params;
    }

    /**
     * 打印 Banner
     */
    private void printBanner() {
        log.info("");
        log.info("================================================================================");
        log.info("                                                                              ");
        log.info("   ____       _                   _        ____  __  __           _          ");
        log.info("  |  _ \\ ___ | | _____  _ __     / \\      / ___||  \\/  | ___   __| |         ");
        log.info("  | |_) / _ \\| |/ / _ \\| '_ \\   / _ \\    \\___ \\| |\\/| |/ _ \\ / _` |        ");
        log.info("  |  _ < (_) |   < (_) | | | | / ___ \\    ___) || |  | | (_) | (_| |        ");
        log.info("  |_| \\_\\___/|_|\\_\\___/|_| |_|/_/   \\_\\  |____/ |_|  |_|\\___/ \\__,_|        ");
        log.info("                                                                              ");
        log.info("  Spring Batch 5.x Multi-Datasource Job Execution Framework                   ");
        log.info("                                                                              ");
        log.info("  Version: 1.0.0                                                              ");
        log.info("  Author: BatchWeaver Team                                                   ");
        log.info("                                                                              ");
        log.info("================================================================================");
        log.info("");
    }

    /**
     * 验证 Spring Batch 元数据表是否存在
     */
    private void validateMetadataTables() {
        log.info("Validating Spring Batch metadata tables...");

        List<String> missingTables = new ArrayList<>();

        try (Connection conn = dataSource1.getConnection()) {
            for (String table : BATCH_METADATA_TABLES) {
                if (!tableExists(conn, table)) {
                    missingTables.add(table);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to validate metadata tables: {}", e.getMessage());
            log.error("Please check database connection and configuration");
            System.exit(1);
            return;
        }

        if (!missingTables.isEmpty()) {
            log.error("================================================================================");
            log.error("ERROR: Spring Batch metadata tables are missing!");
            log.error("================================================================================");
            log.error("Missing tables:");
            missingTables.forEach(table -> log.error("  - {}", table));
            log.error("");
            log.error("Please create Spring Batch metadata tables first.");
            log.error("You can use the following SQL script (for SQL Server):");
            log.error("");
            log.error("  https://github.com/spring-projects/spring-batch/blob/main/spring-batch-core/src/main/resources/org/springframework/batch/core/schema-drop-sqlserver.sql");
            log.error("  https://github.com/spring-projects/spring-batch/blob/main/spring-batch-core/src/main/resources/org/springframework/batch/core/schema-sqlserver.sql");
            log.error("");
            log.error("Or execute: java -jar batchweaver.jar --spring.batch.jdbc.initialize-schema=always");
            log.error("================================================================================");
            System.exit(1);
        }

        log.info("All Spring Batch metadata tables are present.");
        log.info("");
    }

    /**
     * 检查表是否存在
     */
    private boolean tableExists(Connection conn, String tableName) {
        try {
            // SQL Server: 检查表是否存在
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?";
            try (var ps = conn.prepareStatement(sql)) {
                ps.setString(1, tableName);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            log.warn("Failed to check table existence for {}: {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * 获取必填参数
     */
    private String getRequiredOption(ApplicationArguments args, String key) {
        List<String> values = args.getOptionValues(key);
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Missing required parameter: --" + key);
        }
        return values.get(0);
    }

    /**
     * 验证断点续传参数（只允许 --job.name 和 --job.id）
     */
    private void validateRestartParams(ApplicationArguments args) {
        Set<String> allowedParams = Set.of(PARAM_JOB_NAME, PARAM_JOB_ID);
        Set<String> providedParams = args.getOptionNames();

        for (String param : providedParams) {
            if (!allowedParams.contains(param)) {
                log.error("Invalid parameter for restart: --{}", param);
                log.error("Restart mode only allows: --job.name and --job.id");
                log.error("If you need to run with new parameters, start a new Job instance instead");
                System.exit(1);
            }
        }
    }

    /**
     * 执行新的 Job 实例
     */
    private void launchNewJob(Job job, Map<String, String> params) throws Exception {
        String jobName = job.getName();
        log.info("================================================================================");
        log.info("Starting Job: {}", jobName);
        log.info("================================================================================");

        // 打印参数
        if (!params.isEmpty()) {
            log.info("Job Parameters:");
            params.forEach((key, value) -> log.info("  --{} = {}", key, value));
        }

        // 构建 JobParameters
        JobParametersBuilder builder = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("job.name", jobName);

        // 添加自定义参数（自动类型检测）
        params.forEach((key, value) -> addParameterWithTypeDetection(builder, key, value));

        JobParameters jobParameters = builder.toJobParameters();
        log.info("Final JobParameters: {}", jobParameters);

        JobExecution jobExecution = jobLauncher.run(job, jobParameters);

        log.info("================================================================================");
        log.info("Job execution completed");
        log.info("   Job Name: {}", jobExecution.getJobInstance().getJobName());
        log.info("   Job ID: {}", jobExecution.getId());
        log.info("   Status: {}", jobExecution.getStatus());
        log.info("   Exit Code: {}", jobExecution.getExitStatus().getExitCode());
        log.info("================================================================================");

        // 非 COMPLETED 状态返回错误码
        if (!BatchStatus.COMPLETED.equals(jobExecution.getStatus())) {
            log.warn("Job finished with status: {}", jobExecution.getStatus());
            System.exit(1);
        }
    }

    /**
     * 添加参数并自动检测类型
     */
    private void addParameterWithTypeDetection(JobParametersBuilder builder, String key, String value) {
        try {
            builder.addLong(key, Long.parseLong(value));
        } catch (NumberFormatException e1) {
            try {
                builder.addDouble(key, Double.parseDouble(value));
            } catch (NumberFormatException e2) {
                builder.addString(key, value);
            }
        }
    }

    /**
     * 断点续传：重启指定的 Job 实例
     * 使用 Spring Batch 原生 restart 机制，保持原 JobParameters
     */
    private void restartJob(long jobExecutionId) throws Exception {
        log.info("================================================================================");
        log.info("Restarting Job Execution ID: {}", jobExecutionId);
        log.info("================================================================================");

        // 检查 Job 实例是否存在
        JobExecution originalExecution = jobExplorer.getJobExecution(jobExecutionId);
        if (originalExecution == null) {
            log.error("Job execution not found: {}", jobExecutionId);
            System.exit(1);
            return;
        }

        BatchStatus status = originalExecution.getStatus();
        log.info("Original Job Status: {}", status);
        log.info("Original Job Name: {}", originalExecution.getJobInstance().getJobName());

        // 打印原 JobParameters
        log.info("Original JobParameters (will be reused):");
        originalExecution.getJobParameters().getParameters().forEach((key, value) ->
                log.info("  - {} = {}", key, value));

        // 检查是否可以重启
        if (status == BatchStatus.COMPLETED) {
            log.warn("Job is already COMPLETED. No need to restart.");
            printStepExecutions(jobExecutionId);
            return;
        }

        if (status != BatchStatus.FAILED && status != BatchStatus.STOPPED) {
            log.error("Job cannot be restarted. Current status: {}", status);
            log.error("Only FAILED or STOPPED jobs can be restarted.");
            System.exit(1);
            return;
        }

        // 打印失败原因
        if (originalExecution.getStatus() == BatchStatus.FAILED) {
            List<Throwable> failureExceptions = originalExecution.getAllFailureExceptions();
            if (!failureExceptions.isEmpty()) {
                log.error("Original failure cause:");
                for (Throwable ex : failureExceptions) {
                    log.error("  - {}", ex.getMessage());
                }
            }
        }

        printStepExecutions(jobExecutionId);

        // 使用 JobOperator 重启（自动使用原 JobParameters）
        Long newJobExecutionId = jobOperator.restart(jobExecutionId);

        log.info("================================================================================");
        log.info("Job restarted successfully");
        log.info("   New Job Execution ID: {}", newJobExecutionId);

        JobExecution newExecution = jobExplorer.getJobExecution(newJobExecutionId);
        log.info("   Status: {}", newExecution.getStatus());
        log.info("   Exit Code: {}", newExecution.getExitStatus().getExitCode());
        log.info("================================================================================");

        if (!BatchStatus.COMPLETED.equals(newExecution.getStatus())) {
            log.warn("Restarted Job finished with status: {}", newExecution.getStatus());
            System.exit(1);
        }
    }

    /**
     * 根据 Job 名称从 JobRegistry 查找 Job
     */
    private Job findJobByName(String jobName) {
        try {
            return jobRegistry.getJob(jobName);
        } catch (NoSuchJobException e) {
            return null;
        }
    }

    /**
     * 解析 Job Execution ID
     */
    private long parseJobExecutionId(String idStr) {
        try {
            return Long.parseLong(idStr.trim());
        } catch (NumberFormatException e) {
            log.error("Invalid job execution ID: {}", idStr);
            System.exit(1);
            return 0;
        }
    }

    /**
     * 打印 Step 执行详情
     */
    private void printStepExecutions(long jobExecutionId) {
        JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);
        if (jobExecution != null) {
            Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
            if (!stepExecutions.isEmpty()) {
                log.info("Step executions:");
                for (StepExecution stepExecution : stepExecutions) {
                    log.info("  - Step: {}, Status: {}, Read: {}, Write: {}, Skip: {}",
                            stepExecution.getStepName(),
                            stepExecution.getStatus(),
                            stepExecution.getReadCount(),
                            stepExecution.getWriteCount(),
                            stepExecution.getSkipCount());
                }
            }
        }
    }

    /**
     * 打印使用说明
     */
    private void printUsage() {
        log.info("================================================================================");
        log.info("BatchWeaver Job Launcher");
        log.info("================================================================================");
        log.info("Usage:");
        log.info("  # Execute new Job (with optional parameters)");
        log.info("  java -jar batchweaver.jar --job.name=<jobName> [--key=value] ...");
        log.info("");
        log.info("  # Restart Job (resume from failure, NO extra parameters allowed)");
        log.info("  java -jar batchweaver.jar --job.name=<jobName> --job.id=<executionId>");
        log.info("");
        log.info("Examples:");
        log.info("  # Execute Job");
        log.info("  java -jar batchweaver.jar --job.name=conditionalFlowJob");
        log.info("");
        log.info("  # Execute Job with business parameters");
        log.info("  java -jar batchweaver.jar --job.name=chunkProcessingJob --data=20250625");
        log.info("");
        log.info("  # Execute Job with custom input file");
        log.info("  java -jar batchweaver.jar --job.name=conditionalFlowJob --input.file=data/input/users.txt");
        log.info("");
        log.info("  # Restart failed Job (uses original JobParameters)");
        log.info("  java -jar batchweaver.jar --job.name=conditionalFlowJob --job.id=123");
        log.info("================================================================================");
    }

    /**
     * 打印可用的 Job 列表
     */
    private void printAvailableJobs() {
        log.info("Available Jobs:");
        try {
            jobRegistry.getJobNames().forEach(name ->
                log.info("  - {}", name));
        } catch (Exception e) {
            // 如果 JobRegistry 未初始化，显示静态列表
            log.info("  - conditionalFlowJob     # Job1: 条件流程测试");
            log.info("  - chunkProcessingJob     # Job2: 批处理模式测试");
            log.info("  - format1ImportJob       # Job3: 格式1文件导入");
            log.info("  - format2ImportJob       # Job3: 格式2文件导入");
            log.info("  - format3ImportJob       # Job3: 格式3文件导入");
            log.info("  - format1ExportJob       # Job4: 格式1文件导出");
            log.info("  - format2ExportJob       # Job4: 格式2文件导出");
            log.info("  - complexWorkflowJob     # Job5: 复杂工作流测试");
        }
    }
}
