package com.legal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 智能小法庭（合同纠纷模拟庭审 + 证据链图谱 + 补证建议）配置。
 * 所有字段均通过 application.yaml 中 legal.smart-court.* 注入。
 */
@Data
@ConfigurationProperties(prefix = "legal.smart-court")
public class SmartCourtProperties {

    /** 是否启用智能小法庭功能总开关，关闭后相关接口拒绝服务。 */
    private boolean enabled = false;

    /** 单案件最大允许的庭审轮数，超过后阻止开启新轮并引导生成报告。 */
    private int maxRounds = 8;

    /** 单轮庭审 LLM 累计 token 上限，超限后终止本轮剩余 LLM 调用并标记失败。 */
    private int maxTokensPerRound = 6000;

    /** 单案件累计 token 上限，超限后阻止开启新轮以控制成本。 */
    private int maxTokensPerCase = 60000;

    /** AI 角色发言 schema 校验失败后的最大整轮重试次数。 */
    private int evidenceRefRetry = 2;

    /** AI 法官输出双向不利点缺失时的整体重生成最大次数。 */
    private int judgeBilateralRetry = 2;

    /** 模拟裁判报告导出时附加的合规水印文案。 */
    private String reportWatermark = "仅供模拟参考、不构成法律意见";

    /** 智能小法庭专用大模型配置。 */
    private Llm llm = new Llm();

    /** 庭审流程控制配置。 */
    private Hearing hearing = new Hearing();

    /** 多角色 AI 模型与提示词配置。 */
    private Agent agent = new Agent();

    /** 庭审 RAG 检索上下文配置。 */
    private Retrieval retrieval = new Retrieval();

    /** 证据引用真实性校验配置。 */
    private EvidenceValidation evidenceValidation = new EvidenceValidation();

    /** 补证建议配置。 */
    private Supplement supplement = new Supplement();

    /** SSE 流式庭审输出配置。 */
    private Stream stream = new Stream();

    /** 智能小法庭降级策略配置。 */
    private Degrade degrade = new Degrade();

    /** 智能小法庭审计与观测配置。 */
    private Observability observability = new Observability();

    /** 证据链知识图谱相关配置。 */
    private Graph graph = new Graph();

    /** 智能小法庭专用大模型配置项。 */
    @Data
    public static class Llm {

        /** 智能小法庭模型 OpenAI 兼容接口地址。 */
        private String baseUrl;

        /** 智能小法庭模型 API Key。 */
        private String apiKey;

        /** 智能小法庭模型名称。 */
        private String modelName;

        /** 智能小法庭模型采样温度。 */
        private Double temperature;

        /** 智能小法庭模型单次最大输出 token 数。 */
        private Integer maxOutputTokens;

        /** 智能小法庭模型调用超时时间。 */
        private Duration timeout;
    }

    /** 庭审流程控制配置项。 */
    @Data
    public static class Hearing {

        /** 默认庭审起始阶段，创建案件后首次进入的庭审阶段。 */
        private String initialStage = "OPENING_PLAINTIFF";

        /** 是否允许用户手动推进庭审阶段，关闭后只能由系统状态机推进。 */
        private boolean manualStageTransitionEnabled = true;

        /** 单个庭审阶段允许的最大消息数量，超过后提示进入下一阶段。 */
        private int maxMessagesPerStage = 20;

        /** 单轮庭审最大执行时长，超过后中断本轮并标记失败。 */
        private Duration roundTimeout = Duration.ofSeconds(180);

        /** 庭审轮次失败后的默认重试延迟时间。 */
        private Duration retryDelay = Duration.ofSeconds(10);
    }

    /** 多角色 AI 模型与提示词配置项。 */
    @Data
    public static class Agent {

        /** 法官角色使用的模型名称。 */
        private String judgeModel = "deepseek-v4-pro";

        /** 对方代理人角色使用的模型名称。 */
        private String opponentModel = "deepseek-v4-pro";

        /** 用户辅助律师角色使用的模型名称。 */
        private String userAdvisorModel = "deepseek-v4-pro";

        /** 观众或旁听视角角色使用的模型名称。 */
        private String audienceModel = "deepseek-v4-pro";

        /** 法官角色采样温度。 */
        private double judgeTemperature = 0.1D;

        /** 对方代理人角色采样温度。 */
        private double opponentTemperature = 0.2D;

        /** 用户辅助律师角色采样温度。 */
        private double userAdvisorTemperature = 0.15D;

        /** 观众视角采样温度。 */
        private double audienceTemperature = 0.3D;

        /** 单次角色发言最大输出 token 数。 */
        private int maxOutputTokens = 1800;

        /** 是否启用角色提示词隔离。 */
        private boolean promptIsolationEnabled = true;

        /** 是否强制法官输出原被告双方不利点。 */
        private boolean judgeBilateralAdversePointsRequired = true;
    }

    /** 庭审 RAG 检索上下文配置项。 */
    @Data
    public static class Retrieval {

        /** 庭审上下文检索是否启用父级语义块扩展。 */
        private boolean parentContextEnabled = true;

        /** 庭审每次检索返回给模型的最大上下文数量。 */
        private int topK = 8;

        /** 庭审检索向量召回的最小相似度阈值。 */
        private double minVectorSimilarity = 0.75D;

        /** 单条证据上下文发送给模型前允许的最大字符数。 */
        private int maxContextCharsPerEvidence = 3000;

        /** 单次角色发言允许携带的最大证据上下文总字符数。 */
        private int maxTotalContextChars = 12000;
    }

    /** 证据引用真实性校验配置项。 */
    @Data
    public static class EvidenceValidation {

        /** 是否启用证据引用 schema 校验。 */
        private boolean schemaCheckEnabled = true;

        /** 是否启用案件证据白名单校验。 */
        private boolean whitelistCheckEnabled = true;

        /** 是否启用租户和用户归属校验。 */
        private boolean ownershipCheckEnabled = true;

        /** 是否丢弃未通过校验的证据引用。 */
        private boolean dropInvalidReferences = true;

        /** 单条观点允许引用的最大证据数量。 */
        private int maxReferencesPerArgument = 10;
    }

    /** 补证建议配置项。 */
    @Data
    public static class Supplement {

        /** 是否启用补证建议生成。 */
        private boolean enabled = true;

        /** 是否允许 LLM 参与补证建议解释。 */
        private boolean llmExplanationEnabled = true;

        /** 单案件最多保留的开放补证建议数量。 */
        private int maxOpenSuggestionsPerCase = 30;

        /** 补证建议生成时默认最小严重等级。 */
        private String minVisibleSeverity = "LOW";

        /** 图谱更新后是否自动重新计算补证建议。 */
        private boolean refreshOnGraphUpdate = true;
    }

    /** SSE 流式庭审输出配置项。 */
    @Data
    public static class Stream {

        /** 是否启用庭审 SSE 流式输出。 */
        private boolean enabled = true;

        /** SSE 心跳发送间隔。 */
        private Duration heartbeatInterval = Duration.ofSeconds(15);

        /** SSE 连接最大保持时间。 */
        private Duration maxDuration = Duration.ofMinutes(5);

        /** 用户停止流式输出后是否保存已生成的部分内容。 */
        private boolean savePartialOnCancel = true;
    }

    /** 智能小法庭降级策略配置项。 */
    @Data
    public static class Degrade {

        /** Neo4j 不可用时是否允许继续进行纯文本模拟庭审。 */
        private boolean allowHearingWithoutGraph = true;

        /** LLM 调用失败时是否允许返回规则化兜底提示。 */
        private boolean allowRuleFallbackOnLlmError = true;

        /** RAG 检索无结果时是否允许继续生成程序性发言。 */
        private boolean allowHearingWithoutRagHit = false;
    }

    /** 智能小法庭审计与观测配置项。 */
    @Data
    public static class Observability {

        /** 是否记录庭审角色调用摘要日志。 */
        private boolean auditLogEnabled = true;

        /** 是否记录图谱投影指标。 */
        private boolean graphMetricsEnabled = true;

        /** 是否记录证据引用校验统计。 */
        private boolean evidenceValidationMetricsEnabled = true;
    }

    /** 证据链图谱相关配置项。 */
    @Data
    public static class Graph {

        /** 是否启用 Neo4j 投影器，关闭后所有图谱事件保持 PENDING 状态等待人工启用。 */
        private boolean projectorEnabled = true;

        /** Cypher 多跳路径查询允许的最大跳数，硬性约束查询代价。 */
        private int maxHops = 4;

        /** 单案件图谱节点数量上限，超过后拒绝写入新节点并告警。 */
        private int maxNodesPerCase = 2000;

        /** 投影器单批消费 PENDING 事件的数量。 */
        private int projectorBatchSize = 50;

        /** 投影器轮询间隔毫秒数。 */
        private long projectorPollIntervalMs = 2000L;

        /** 投影器单事件最大重试次数，超过后置为 DEAD 并告警。 */
        private int projectorMaxRetries = 5;

        /** 案件级全量重建时按事件分页的页大小。 */
        private int rebuildPageSize = 200;

        /** 前端默认子图查询围绕争议焦点的跳数。 */
        private int defaultFocusHops = 2;

        /** 前端默认子图查询返回节点数量上限。 */
        private int defaultFocusNodeLimit = 200;
    }
}
