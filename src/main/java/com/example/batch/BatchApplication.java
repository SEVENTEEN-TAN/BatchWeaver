package com.example.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BatchApplication {

    /**
     * 默认 Job 名称（当没有指定 jobName 参数时使用）
     */
    private static final String DEFAULT_JOB_NAME = "demoJob";

    public static void main(String[] args) {
        // 检查是否提供了 jobName 参数
        boolean hasJobName = false;
        for (String arg : args) {
            if (arg.startsWith("jobName=")) {
                hasJobName = true;
                break;
            }
        }

        // 如果没有提供 jobName，添加默认的 jobName
        if (!hasJobName && args.length == 0) {
            System.out.println("========================================");
            System.out.println("No jobName provided. Using default job: " + DEFAULT_JOB_NAME);
            System.out.println("To run a specific job, use: java -jar app.jar jobName=<jobName>");
            System.out.println("Or set program arguments in your IDE: jobName=<jobName>");
            System.out.println("========================================");
            args = new String[]{"jobName=" + DEFAULT_JOB_NAME};
        } else if (!hasJobName && args.length > 0) {
            // 有其他参数但没有 jobName，添加默认 jobName
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "jobName=" + DEFAULT_JOB_NAME;
            System.arraycopy(args, 0, newArgs, 1, args.length);
            args = newArgs;
            System.out.println("No jobName provided. Using default job: " + DEFAULT_JOB_NAME);
        }

        SpringApplication.run(BatchApplication.class, args);
    }

}
