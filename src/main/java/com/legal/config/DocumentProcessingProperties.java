package com.legal.config;

import com.legal.enums.DocumentParseMethod;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

    /** 文档内容清洗配置。 */
    private Cleaning cleaning = new Cleaning();

    /** MinerU 精准解析配置。 */
    private Mineru mineru = new Mineru();

    /** 文档解析后台 worker 配置。 */
    private Worker worker = new Worker();

    /** 语义块超长后使用安全字符窗口兜底时的重叠字符数。 */
    private int fallbackOverlap = 80;

    /**
     * 文档内容清洗配置。
     */
    @Data
    public static class Cleaning {

        /** 是否开放文档清洗能力，关闭后即使前端传入开启参数也不会清洗。 */
        private boolean enabled = true;

        /** 重复页眉页脚检测时扫描每页开头和结尾的行数。 */
        private int headerFooterScanLines = 3;

        /** 重复行被判定为页眉页脚候选时需要达到的最小出现次数。 */
        private int repeatedLineMinOccurrences = 3;

        /** 重复行被判定为页眉页脚候选时需要覆盖的最小页面比例。 */
        private double repeatedLinePageRatio = 0.6d;

        /** 重复短行参与页眉页脚候选判断的最大字符长度。 */
        private int repeatedLineMaxLength = 80;

        /** 切块后保留切片的最小有效字符数，法律短标题和编号条款不受该限制。 */
        private int minEffectiveChunkLength = 20;

        /** 每个文档最多保存多少条被清洗内容样例。 */
        private int removedSampleLimit = 50;

        /** 每条被清洗内容样例最多保存多少个字符。 */
        private int removedSampleMaxChars = 300;

        /** 是否启用页码类噪声过滤规则。 */
        private boolean removePageNumbers = true;

        /** 是否启用版权声明类噪声过滤规则。 */
        private boolean removeCopyrightLines = true;

        /** 是否启用官网或系统导出元数据过滤规则。 */
        private boolean removeExportMetadata = true;

        /** 是否启用孤立 URL 行过滤规则。 */
        private boolean removeStandaloneUrls = true;

        /** 是否启用重复页眉页脚过滤规则。 */
        private boolean removeRepeatedHeaderFooter = true;
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

        /** MinerU Markdown 语义切片目标最大字符数，独立于原生解析切片大小。 */
        private int chunkSize = 800;

        /** MinerU Markdown 相邻短语义块合并时的目标最小字符数，独立于原生解析切片大小。 */
        private int minChunkSize = 180;

        /** MinerU 结果包图片资产处理配置。 */
        private ImageAsset imageAsset = new ImageAsset();

        /** MinerU 图片图生文描述配置。 */
        private ImageCaption imageCaption = new ImageCaption();

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
     * MinerU 图片资产处理配置。
     */
    @Data
    public static class ImageAsset {

        /** 是否启用 MinerU 结果包图片资产处理。 */
        private boolean enabled = true;

        /** 单个文档最多处理的图片数量。 */
        private int maxImagesPerDocument = 50;

        /** 单张图片允许的最大字节数。 */
        private long maxImageBytes = 5 * 1024 * 1024L;

        /** 单个文档图片允许的最大总字节数。 */
        private long maxTotalImageBytes = 50 * 1024 * 1024L;

        /** 允许处理的图片扩展名列表。 */
        private List<String> supportedExtensions = new ArrayList<>(List.of("jpg", "jpeg", "png", "webp"));

        /** 图片增强失败时是否降级继续解析 Markdown 文本。 */
        private boolean continueOnFailure = true;

        /** 被视为装饰图的最小图片字节数，小于该值会跳过图生文。 */
        private long minMeaningfulImageBytes = 2048L;
    }

    /**
     * MinerU 图片图生文描述配置。
     */
    @Data
    public static class ImageCaption {

        /** 是否启用 MinerU 图片图生文描述。 */
        private boolean enabled = true;

        /** 图生文模型 OpenAI 兼容接口地址。 */
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

        /** 图生文模型 API Key。 */
        private String apiKey;

        /** 图生文模型名称。 */
        private String modelName = "qwen3.5-omni-plus";

        /** 图生文模型调用超时时间。 */
        private Duration timeout = Duration.ofSeconds(60);

        /** 单张图片描述最大输出 Token 数。 */
        private Integer maxOutputTokens = 256;

        /** 单个文档最多调用图生文模型处理的图片数量。 */
        private int maxImagesPerDocument = 50;
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
