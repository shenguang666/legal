package com.legal.config;

import com.legal.enums.DocumentParseMethod;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 文档解析与上传处理配置。
 */
@Data
@ConfigurationProperties(prefix = "legal.document-processing")
public class DocumentProcessingProperties {

    /** 默认文档解析方式，未显式传入解析方式时使用。 */
    private DocumentParseMethod defaultParseMethod = DocumentParseMethod.NATIVE;

    /** 单次上传请求允许的最大文档数量。 */
    private int maxUploadDocuments = 5;

    /** 结构化语义切片配置。 */
    private Chunking chunking = new Chunking();

    /** MinerU 精准解析配置。 */
    private Mineru mineru = new Mineru();

    /** 文档解析后台 worker 配置。 */
    private Worker worker = new Worker();

    /**
     * 结构化语义切片配置。
     */
    @Data
    public static class Chunking {

        /** 语义切片目标最大字符数。 */
        private int maxChunkSize = 800;

        /** 相邻短语义块合并时的目标最小字符数。 */
        private int minChunkSize = 180;

        /** 语义块超长后使用安全字符窗口兜底时的重叠字符数。 */
        private int fallbackOverlap = 80;
    }

    /**
     * MinerU 精准解析配置。
     */
    @Data
    public static class Mineru {

        /** 是否启用 MinerU 精准解析能力。 */
        private boolean enabled = false;

        /** MinerU API 基础地址。 */
        private String baseUrl = "https://mineru.net";

        /** MinerU API Token，仅允许通过后端环境变量配置。 */
        private String apiToken;

        /** MinerU 模型版本，默认使用 v4 精准解析推荐的 vlm。 */
        private String modelVersion = "vlm";

        /** 文档语言，用于 OCR 与版面识别。 */
        private String language = "ch";

        /** 是否启用表格识别。 */
        private boolean enableTable = true;

        /** 是否启用公式识别。 */
        private boolean enableFormula = true;

        /** 是否启用 OCR，适用于扫描件或图片型 PDF。 */
        private boolean ocr = false;

        /** MinerU HTTP 连接超时时间。 */
        private Duration connectTimeout = Duration.ofSeconds(10);

        /** MinerU HTTP 读取超时时间。 */
        private Duration readTimeout = Duration.ofSeconds(60);

        /** MinerU 解析轮询总超时时间。 */
        private Duration timeout = Duration.ofMinutes(5);

        /** MinerU 解析结果轮询间隔。 */
        private Duration pollInterval = Duration.ofSeconds(3);
    }

    /**
     * 文档解析后台 worker 配置。
     */
    @Data
    public static class Worker {

        /** 是否启用文档解析后台 worker。 */
        private boolean enabled = true;

        /** 文档解析 worker 轮询间隔毫秒数。 */
        private long pollIntervalMs = 3000L;

        /** 单次 worker 最多处理的解析任务数量。 */
        private int batchSize = 5;

        /** 文档解析失败后允许的最大重试次数。 */
        private int maxRetries = 3;
    }
}
