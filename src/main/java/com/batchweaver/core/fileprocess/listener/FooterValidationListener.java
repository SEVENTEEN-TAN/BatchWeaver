package com.batchweaver.core.fileprocess.listener;

import com.batchweaver.core.fileprocess.function.PostImportValidator;
import com.batchweaver.core.fileprocess.model.FooterInfo;
import com.batchweaver.core.fileprocess.reader.HeaderFooterAwareReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;

/**
 * 导入后校验监听器
 * <p>
 * 在 Step 执行完成后，从 ExecutionContext 获取 Footer 声明的记录数和实际处理统计，
 * 调用 {@link PostImportValidator} 进行校验。
 * <p>
 * <b>工作流程：</b>
 * <ol>
 *   <li>从 Step ExecutionContext 获取 {@link FooterInfo}</li>
 *   <li>从 StepExecution 获取 readCount, writeCount, skipCount</li>
 *   <li>调用 {@link PostImportValidator#validate(long, long, long, long)} 进行校验</li>
 *   <li>如果校验失败，抛出异常导致 Step 失败</li>
 * </ol>
 */
@Slf4j
public class FooterValidationListener implements StepExecutionListener {

    private final PostImportValidator validator;

    public FooterValidationListener(PostImportValidator validator) {
        this.validator = validator;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        // 从 Step ExecutionContext 获取 FooterInfo 对象
        ExecutionContext stepContext = stepExecution.getExecutionContext();
        FooterInfo footerInfo = (FooterInfo) stepContext.get(HeaderFooterAwareReader.FOOTER_INFO_KEY);
        long declaredCount = (footerInfo != null) ? footerInfo.getCount() : 0L;

        // 从 StepExecution 获取实际处理统计
        long readCount = stepExecution.getReadCount();
        long writeCount = stepExecution.getWriteCount();
        long skipCount = stepExecution.getSkipCount();

        log.info("Post-import validation: declared={}, read={}, write={}, skip={}",
                declaredCount, readCount, writeCount, skipCount);

        try {
            validator.validate(declaredCount, readCount, writeCount, skipCount);
            log.info("Post-import validation passed");
        } catch (Exception e) {
            log.error("Post-import validation failed: {}", e.getMessage());
            throw e;  // 抛出异常导致 Step 失败
        }

        return stepExecution.getExitStatus();
    }
}
