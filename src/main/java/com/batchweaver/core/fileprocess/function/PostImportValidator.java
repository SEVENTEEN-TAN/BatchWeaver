package com.batchweaver.core.fileprocess.function;

/**
 * 导入后校验器
 * <p>
 * 在 Step 执行完成后，对实际导入结果进行校验。
 * 与 {@link FooterValidator} 不同，此校验器在所有数据处理完成后执行，
 * 可以验证实际写入数据库的条数是否符合预期。
 * <p>
 * 使用场景：
 * <ul>
 *   <li>验证实际写入条数 = Footer 声明的记录数（有Footer场景）</li>
 *   <li>验证实际写入条数 = Job 参数传入的期望值（无Footer场景）</li>
 *   <li>验证是否有 skip 记录（部分导入失败场景）</li>
 * </ul>
 */
@FunctionalInterface
public interface PostImportValidator {

    /**
     * 在 Step 执行完成后进行校验
     *
     * @param declaredCount Footer 声明的记录数（无Footer时为0）
     * @param readCount     实际从文件读取的数据行数（不含Header/Footer）
     * @param writeCount    实际写入数据库的条数
     * @param skipCount     跳过的记录数（解析失败/处理失败）
     * @throws IllegalStateException 校验失败时抛出
     */
    void validate(long declaredCount, long readCount, long writeCount, long skipCount);
}
