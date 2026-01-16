package com.example.batch.core.execution;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Job 元数据注解
 * 用于声明 Job 支持高级执行模式（RESUME, SKIP_FAIL, ISOLATED）
 * 
 * <p>未标注此注解的 Job 只能使用 STANDARD 模式执行</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>
 * // 线性流 Job
 * {@literal @}Configuration
 * {@literal @}BatchJob(name = "demoJob", steps = {"importStep", "updateStep", "exportStep"})
 * public class DemoJobConfig {
 *     // ...
 * }
 * 
 * // 条件流 Job
 * {@literal @}Configuration
 * {@literal @}BatchJob(name = "conditionalJob", steps = {"step1", "step2", "step3"}, conditionalFlow = true)
 * public class ConditionalJobConfig {
 *     // ...
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BatchJob {

    /**
     * Job 名称（必须与 @Bean 定义的 Job 名称一致）
     */
    String name();

    /**
     * Step 名称列表
     * <p>仅用于 ISOLATED 模式校验 _target_steps 参数合法性</p>
     * <p>不关心顺序，只需列出该 Job 中所有可用的 Step</p>
     */
    String[] steps();

    /**
     * 是否为条件流 Job
     * <p>默认 false 表示线性流</p>
     * <p>条件流 Job 不支持 SKIP_FAIL 模式</p>
     */
    boolean conditionalFlow() default false;
}
