package com.legal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "rag.metric.retrieval")
public class RagRetrievalMetricProperties {

    /** RAG 检索质量评估任务总开关。 */
    private boolean enabled = false;
    /** 定时评估任务配置。 */
    private Worker worker = new Worker();
    /** 候选召回配置。 */
    private CandidateRecall candidateRecall = new CandidateRecall();
    /** 待评估 query 过滤配置。 */
    private QueryFilter queryFilter = new QueryFilter();
    /** 大模型评估配置。 */
    private JudgeModel judgeModel = new JudgeModel();

    @Data
    public static class Worker {
        /** 定时任务 Cron 表达式。 */
        private String cron = "0 0/5 2-5 * * ?";
        /** 单次任务最多处理多少条检索日志。 */
        private int batchSize = 20;
        /** 定时任务默认处理距离当前日期多少天的业务日期，1 表示处理上一自然日。 */
        private int metricDateOffsetDays = 1;
        /** 检索日志扫描失败后允许的最大重试次数。 */
        private int maxRetries = 3;
        /** 评估任务并发数量上限。 */
        private int concurrency = 1;
        /** 单条检索日志评估超时时间。 */
        private Duration timeout = Duration.ofSeconds(90);
    }

    @Data
    public static class CandidateRecall {
        /** 每条 query 再次召回的候选切片数量。 */
        private int topN = 200;
        /** 从粗召回候选中送入大模型精筛的比例。 */
        private double judgeCandidateRatio = 0.05D;
        /** 送入大模型精筛的候选切片数量下限。 */
        private int minJudgeCandidates = 10;
        /** 送入大模型精筛的候选切片数量上限。 */
        private int maxJudgeCandidates = 20;
        /** 单个切片送入大模型判断时保留的最大字符数。 */
        private int maxChunkChars = 500;
    }

    @Data
    public static class QueryFilter {
        /** 待评估 query 的最小有效字符数，低于该长度会跳过。 */
        private int minQueryLength = 6;
        /** 低价值或寒暄类 query 黑名单，命中后会跳过评估。 */
        private List<String> excludedQueries = List.of();
    }

    @Data
    public static class JudgeModel {
        /** 评估提示词版本，用于区分不同评估口径。 */
        private String promptVersion = "rag-metric-v2";
        /** 专用评估模型名称；为空时复用在线问答模型。 */
        private String modelName;
        /** 单次模型判断最多输出 token 数。 */
        private int maxOutputTokens = 1200;
    }
}
