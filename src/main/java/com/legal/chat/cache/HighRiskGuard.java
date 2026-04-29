package com.legal.chat.cache;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 高风险问题判定。
 *
 * <p>当前实现为保守的规则兜底，可按业务逐步增强。</p>
 */
@Component
public class HighRiskGuard {

    public boolean isHighRisk(String question) {
        if (!StringUtils.hasText(question)) {
            return false;
        }
        String q = question.trim();
        // 典型高风险：刑事量刑/规避执法/明确违法指引
        return q.contains("判几年")
                || q.contains("量刑")
                || q.contains("怎么逃")
                || q.contains("规避")
                || q.contains("伪造")
                || q.contains("洗钱")
                || q.contains("诈骗")
                || q.contains("贩毒")
                || q.contains("抢劫")
                || q.contains("爆炸")
                || q.contains("枪")
                || q.contains("杀人")
                || q.contains("毒品")
                || q.contains("黑客")
                || q.contains("入侵");
    }
}
