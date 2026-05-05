package com.legal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 记忆管理配置（见 docs/memory-management-design.md）。
 */
@Data
@ConfigurationProperties(prefix = "legal.memory")
public class LegalMemoryProperties {

    /** 总开关。 */
    private boolean enabled = true;
    /** 缓存配置。 */
    private Cache cache = new Cache();
    /** 短期记忆配置。 */
    private ShortTerm shortTerm = new ShortTerm();
    /** 摘要配置。 */
    private Summary summary = new Summary();
    /** 长期记忆配置。 */
    private LongTerm longTerm = new LongTerm();
    /** 用户外挂知识库配置。 */
    private UserKnowledge userKnowledge = new UserKnowledge();
    /** Worker 配置。 */
    private Worker worker = new Worker();

    @Data public static class Cache { private int messagesTtlDays = 7; }
    @Data public static class ShortTerm { private int maxMessages = 12; }
    @Data public static class Summary { private boolean enabled = true; private int refreshRounds = 5; private int lastMessages = 20; }

    @Data
    public static class LongTerm {
        /** 是否启用长期记忆自动抽取与读取。 */
        private boolean enabled = true;
        /** 上下文最多注入多少条长期记忆。 */
        private int maxItems = 20;
        /** 自动抽取结果最小置信度。 */
        private double minConfidence = 0.6d;
        /** 候选态升级为稳定记忆所需的连续命中阈值。 */
        private int stableThreshold = 2;
    }

    @Data
    public static class UserKnowledge {
        /** 是否启用用户外挂知识库。 */
        private boolean enabled = true;
        /** 上下文最多注入多少条用户知识检索结果。 */
        private int topK = 3;
        /** 多少字符以上的问题可进入用户外挂知识候选。 */
        private int minQuestionLength = 8;
    }

    @Data public static class Worker { private boolean enabled = true; private int pollIntervalMs = 3000; private int batchSize = 10; private int maxRetries = 5; private int retryDelaySeconds = 30; }
}
