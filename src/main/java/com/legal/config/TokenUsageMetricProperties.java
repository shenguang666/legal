package com.legal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "token.metric.usage")
public class TokenUsageMetricProperties {

    /** Token 消耗统计任务总开关。 */
    private boolean enabled = false;
    /** Token 消耗统计版本，用于隔离不同统计口径。 */
    private String version = "token-usage-v1";
    /** Token 消耗统计定时任务配置。 */
    private Worker worker = new Worker();
    /** Token 消耗 Top 用户排行配置。 */
    private TopUsers topUsers = new TopUsers();

    @Data
    public static class Worker {
        /** Token 消耗统计定时任务 Cron 表达式。 */
        private String cron = "0 0/5 2-5 * * ?";
        /** 单次定时任务最多处理的租户数量。 */
        private int batchSize = 20;
        /** 定时任务默认处理距离当前日期多少天的业务日期，1 表示处理上一自然日。 */
        private int metricDateOffsetDays = 1;
        /** 单个租户日期统计失败后允许的最大重试次数。 */
        private int maxRetries = 3;
        /** 单个租户日期统计任务超时时间。 */
        private Duration timeout = Duration.ofSeconds(90);
    }

    @Data
    public static class TopUsers {
        /** 每个指标日期保留 Token 消耗最高的用户数量。 */
        private int limit = 10;
    }
}
