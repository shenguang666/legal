package com.legal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 合同审阅功能配置。
*/
@Data
@ConfigurationProperties(prefix = "legal.contract-review")
public class ContractReviewProperties {

    /** 合同审阅功能总开关。 */
    private boolean enabled = true;
    /** 字段抽取相关配置。 */
    private Extraction extraction = new Extraction();
    /** 风险规则检索匹配配置。 */
    private RiskRuleRetrieval riskRuleRetrieval = new RiskRuleRetrieval();
    /** 合同审阅异步任务 worker 配置。 */
    private Worker worker = new Worker();

    @Data
    public static class Extraction {
        /** 单个多值字段最多保留多少条抽取结果。 */
        private int maxRepeatableFields = 8;
        /** 证据片段最大保留长度。 */
        private int maxEvidenceLength = 180;
    }

    @Data
    public static class RiskRuleRetrieval {
        /** 是否启用导入风险规则的向量命中能力。 */
        private boolean enabled = true;
        /** 单个审查切片最多召回多少条风险规则候选。 */
        private int topK = 3;
        /** 向量命中的最小分数阈值。 */
        private double minScore = 0.78d;
        /** 单次审查最多保留多少条导入规则命中结果。 */
        private int maxHitsPerReview = 5;
    }

    @Data
    public static class Worker {
        /** 合同审阅 worker 是否启用。 */
        private boolean enabled = true;
        /** worker 轮询任务间隔，单位毫秒。 */
        private int pollIntervalMs = 3000;
        /** 每次轮询最多拉取多少条任务。 */
        private int batchSize = 10;
        /** 单条任务最大重试次数。 */
        private int maxRetries = 5;
        /** 任务失败后重试延迟秒数。 */
        private int retryDelaySeconds = 30;
    }
}
