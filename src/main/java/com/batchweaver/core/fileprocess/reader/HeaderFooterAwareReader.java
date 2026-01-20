package com.batchweaver.core.fileprocess.reader;

import com.batchweaver.core.fileprocess.function.FooterParser;
import com.batchweaver.core.fileprocess.function.FooterValidator;
import com.batchweaver.core.fileprocess.function.HeaderParser;
import com.batchweaver.core.fileprocess.function.HeaderValidator;
import com.batchweaver.core.fileprocess.model.FooterInfo;
import com.batchweaver.core.fileprocess.model.HeaderInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 延迟决策 Reader - 基于"单次顺序扫描 + 延迟行确认"模式
 * <p>
 * <b>核心设计思想：</b>
 * <ul>
 *   <li>单次顺序扫描 - 文件只被读取一次，完全遵循操作系统预读机制</li>
 *   <li>延迟决策 - 当前行不立即处理，只在确认它不是最后一行后才处理</li>
 *   <li>最小状态缓存 - 只缓存当前行和上一行（O(1)内存）</li>
 * </ul>
 * <p>
 * <b>处理流程：</b>
 * <pre>
 * prevLine = null
 * for each line in file:
 *     if firstLine and isHeader(line):
 *         continue  # 跳过Header
 *
 *     if prevLine != null:
 *         processDataLine(prevLine)  # 确认不是Footer后才处理
 *
 *     prevLine = line  # 缓存当前行，等待下一行确认
 *
 * # EOF: 处理最后一行
 * if prevLine != null and !isTrailer(prevLine):
 *     processDataLine(prevLine)
 * else:
 *     validateTrailer(prevLine)
 * </pre>
 * <p>
 * <b>性能优势：</b>
 * <ul>
 *   <li>零启动延迟 - 不需要在open()时预先扫描整个文件</li>
 *   <li>O(1)内存占用 - 只缓存两行数据</li>
 *   <li>适合超大文件（GB级）处理</li>
 * </ul>
 *
 * @param <T> item type
 * @see <a href="https://github.com/spring-projects/spring-batch">Spring Batch</a>
 */
@Slf4j
public class HeaderFooterAwareReader<T> implements ItemReader<T>, ItemStream {

    public static final String HEADER_INFO_KEY = "headerInfo";
    public static final String FOOTER_INFO_KEY = "footerInfo";
    public static final String DECLARED_RECORD_COUNT_KEY = "declaredRecordCount";
    public static final String ACTUAL_RECORD_COUNT_KEY = "actualRecordCount";

    // ============================================================================
    // 核心状态 - 延迟决策的关键
    // ============================================================================
    private final Resource resource;
    private final Charset charset;
    private final HeaderParser headerParser;
    private final HeaderValidator headerValidator;
    private final FooterParser footerParser;
    private final FooterValidator footerValidator;
    private final FooterLineDetector footerLineDetector;
    private final LineTokenizer lineTokenizer;
    private final FieldSetMapper<T> fieldSetMapper;

    /**
     * 上一行（延迟处理）
     * <p>
     * 只有在读取到下一行时，才能确认上一行不是Footer，此时才处理上一行
     */
    private String prevLine;

    /**
     * 当前行（待确认）
     */
    private String currentLine;

    /**
     * 是否已读取过第一行
     */
    private boolean firstLineRead = false;

    /**
     * 实际处理的记录数（用于Footer校验）
     */
    private long actualRecordCount = 0;

    /**
     * 原始BufferedReader（用于读取文件）
     */
    private BufferedReader reader;

    // ============================================================================
    // 解析结果缓存
    // ============================================================================
    @Getter
    private HeaderInfo headerInfo = HeaderInfo.empty();

    @Getter
    private FooterInfo footerInfo = FooterInfo.empty();

    // ============================================================================
    // 构造器
    // ============================================================================

    /**
     * 创建延迟决策Reader
     *
     * @param resource        文件资源
     * @param headerParser    Header解析器（可选）
     * @param headerValidator Header校验器（可选）
     * @param footerParser    Footer解析器（可选）
     * @param footerValidator Footer校验器（可选）
     * @param lineTokenizer   行分词器
     * @param fieldSetMapper  字段映射器
     */
    public HeaderFooterAwareReader(Resource resource,
                                   HeaderParser headerParser,
                                   HeaderValidator headerValidator,
                                   FooterParser footerParser,
                                   FooterValidator footerValidator,
                                   LineTokenizer lineTokenizer,
                                   FieldSetMapper<T> fieldSetMapper) {
        this(resource, headerParser, headerValidator, footerParser, footerValidator,
                lineTokenizer, fieldSetMapper, null);
    }

    /**
     * 创建延迟决策Reader（自定义Footer检测器）
     */
    public HeaderFooterAwareReader(Resource resource,
                                   HeaderParser headerParser,
                                   HeaderValidator headerValidator,
                                   FooterParser footerParser,
                                   FooterValidator footerValidator,
                                   LineTokenizer lineTokenizer,
                                   FieldSetMapper<T> fieldSetMapper,
                                   FooterLineDetector footerLineDetector) {
        this.resource = Objects.requireNonNull(resource, "resource must not be null");
        this.headerParser = headerParser;
        this.headerValidator = headerValidator;
        this.footerParser = footerParser;
        this.footerValidator = footerValidator;
        this.footerLineDetector = footerLineDetector;
        this.lineTokenizer = Objects.requireNonNull(lineTokenizer, "lineTokenizer must not be null");
        this.fieldSetMapper = Objects.requireNonNull(fieldSetMapper, "fieldSetMapper must not be null");
        this.charset = StandardCharsets.UTF_8;

        // 配置校验：如果有 footerValidator 或自定义 detector，必须有 footerParser
        if ((footerValidator != null || footerLineDetector != null) && footerParser == null) {
            throw new IllegalArgumentException(
                    "footerParser must be provided when footerValidator or footerLineDetector is configured");
        }
    }

    // ============================================================================
    // ItemReader 实现 - 延迟决策核心逻辑
    // ============================================================================

    /**
     * 读取并处理数据行
     * <p>
     * <b>延迟决策逻辑：</b>
     * <ol>
     *   <li>读取下一行</li>
     *   <li>如果有prevLine，确认它不是Footer，处理后返回</li>
     *   <li>将当前行缓存为prevLine</li>
     * </ol>
     */
    @Override
    public T read() throws Exception {
        // 读取下一行
        currentLine = readRawLine();

        // 如果没有下一行了（EOF）
        if (currentLine == null) {
            return handleEndOfFile();
        }

        // 处理第一行（可能是Header）
        if (!firstLineRead) {
            firstLineRead = true;
            if (isHeaderLine(currentLine)) {
                log.debug("First line detected as header: {}", currentLine);
                // 解析Header
                if (headerParser != null) {
                    try {
                        headerInfo = headerParser.parse(currentLine);
                        log.info("Header parsed: {}", headerInfo);
                    } catch (Exception e) {
                        throw new ItemStreamException("Header parsing failed: " + e.getMessage(), e);
                    }
                }
                // 校验Header（Reader自包含）
                if (headerValidator != null) {
                    try {
                        headerValidator.validate(headerInfo);
                        log.info("Header validation passed");
                    } catch (Exception e) {
                        throw new ItemStreamException("Header validation failed: " + e.getMessage(), e);
                    }
                }
                // 清空prevLine，继续读取下一行
                prevLine = null;
                return read();  // 递归调用以获取第一行数据
            }
            // 第一行不是Header，直接作为prevLine缓存
            prevLine = currentLine;
            return read();  // 继续读取以确认
        }

        // 有prevLine，确认它不是Footer，处理后返回
        if (prevLine != null) {
            String lineToProcess = prevLine;
            // 先处理，成功后再前移状态（支持 retry）
            T result = processLine(lineToProcess);
            prevLine = currentLine;  // 处理成功后才缓存当前行
            actualRecordCount++;
            return result;
        }

        // 没有prevLine，缓存当前行
        prevLine = currentLine;
        return read();  // 继续读取以确认
    }

    /**
     * 处理文件结束（EOF）情况
     * <p>
     * <b>关键：</b>此时prevLine就是最后一行，需要判断是否为Footer
     */
    private T handleEndOfFile() throws Exception {
        if (prevLine == null) {
            return null;  // 空文件
        }

        String lineToProcess = prevLine;
        prevLine = null;  // 清空引用，防止死循环

        if (isFooterLine(lineToProcess)) {
            // 是Footer，进行解析和校验（Reader自包含）
            log.info("Last line detected as footer: {}", lineToProcess);
            if (footerParser != null) {
                try {
                    footerInfo = footerParser.parse(lineToProcess);
                    log.info("Footer parsed: {} (actual count: {})", footerInfo, actualRecordCount);
                } catch (Exception e) {
                    throw new ItemStreamException("Footer parsing failed: " + e.getMessage(), e);
                }
                // 校验Footer（Reader自包含）
                if (footerValidator != null) {
                    try {
                        footerValidator.validate(footerInfo, actualRecordCount);
                        log.info("Footer validation passed: expected={}, actual={}",
                                footerInfo.getCount(), actualRecordCount);
                    } catch (Exception e) {
                        throw new ItemStreamException("Footer validation failed: " + e.getMessage(), e);
                    }
                }
            }
            return null;  // Footer不作为数据返回
        } else {
            // 不是Footer，作为数据行处理
            actualRecordCount++;
            return processLine(lineToProcess);
        }
    }

    // ============================================================================
    // ItemStream 实现
    // ============================================================================

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            log.debug("Opening resource: {}", resource);
            reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), charset));
            // 重置所有状态
            resetState();
        } catch (Exception e) {
            throw new ItemStreamException("Failed to open resource: " + resource, e);
        }
    }

    /**
     * 重置所有状态字段
     */
    private void resetState() {
        prevLine = null;
        currentLine = null;
        firstLineRead = false;
        actualRecordCount = 0;
        headerInfo = HeaderInfo.empty();
        footerInfo = FooterInfo.empty();
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // 将解析结果存入ExecutionContext，供后续校验使用
        if (executionContext != null) {
            if (headerInfo != null) {
                executionContext.put(HEADER_INFO_KEY, headerInfo);
            }
            if (footerInfo != null) {
                executionContext.put(FOOTER_INFO_KEY, footerInfo);
                executionContext.putLong(DECLARED_RECORD_COUNT_KEY, footerInfo.getCount());
            }
            executionContext.putLong(ACTUAL_RECORD_COUNT_KEY, actualRecordCount);
        }
    }

    @Override
    public void close() throws ItemStreamException {
        try {
            if (reader != null) {
                log.debug("Closing resource: {} (actual records: {})", resource, actualRecordCount);
                reader.close();
                reader = null;
            }
        } catch (Exception e) {
            throw new ItemStreamException("Failed to close resource: " + resource, e);
        }
    }

    // ============================================================================
    // 私有辅助方法
    // ============================================================================

    /**
     * 读取原始行
     */
    private String readRawLine() throws Exception {
        return reader != null ? reader.readLine() : null;
    }

    /**
     * 判断是否为Header行
     */
    private boolean isHeaderLine(String line) {
        // 简单判断：如果配置了headerParser，第一行就是Header
        return headerParser != null;
    }

    /**
     * 判断是否为Footer行
     * <p>
     * 只有在配置了 footerParser 时才启用 Footer 检测
     */
    private boolean isFooterLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }

        // 只有配置了 footerParser 时才检测 Footer
        if (footerParser == null) {
            return false;
        }

        // 使用检测器判断
        FooterLineDetector detector = footerLineDetector;
        if (detector == null) {
            detector = FooterLineDetector.defaultDetector();
        }

        return detector.isFooterLine(line.trim());
    }

    /**
     * 处理数据行 - 将行转换为Item
     * <p>
     * 注意：直接抛出 BindException 以支持 skip/retry 机制
     */
    private T processLine(String line) throws Exception {
        // Tokenize
        var fieldSet = lineTokenizer.tokenize(line);
        // Map (BindException 会被 Spring Batch 的 skip/retry 捕获)
        return fieldSetMapper.mapFieldSet(fieldSet);
    }
}
