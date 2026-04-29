package com.legal.chat.cache;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 租户共享缓存的安全门禁。
 *
 * <p>规则：
 * - 高风险：绕过读写缓存（由上层策略处理）
 * - 含敏感信息：不允许进入租户共享缓存（可进入用户级缓存）
 * - 强上下文依赖：不允许进入租户共享缓存</p>
 */
@Component
public class CacheSafetyGuard {

    // 这些规则是“保守兜底”，后续可逐步增强。
    private static final Pattern PHONE = Pattern.compile("1[3-9][0-9]{9}");
    private static final Pattern ID_CARD = Pattern.compile("[0-9]{17}[0-9Xx]");
    private static final Pattern BANK = Pattern.compile("[0-9]{13,19}");
    private static final Pattern CASE_NO = Pattern.compile("[\u4e00-\u9fa5]{1,6}[0-9]{2,4}[\u4e00-\u9fa5][0-9]{2,6}号");

    public boolean allowTenantCache(String question) {
        if (!StringUtils.hasText(question)) {
            return false;
        }
        String q = question.trim();
        if (containsSensitive(q)) {
            return false;
        }
        if (isContextDependent(q)) {
            return false;
        }
        return true;
    }

    public boolean containsSensitive(String q) {
        return PHONE.matcher(q).find()
                || ID_CARD.matcher(q).find()
                || BANK.matcher(q).find()
                || CASE_NO.matcher(q).find();
    }

    public boolean isContextDependent(String q) {
        String s = q.trim();
        if (s.length() < 8) {
            return true;
        }
        // 常见强上下文代词/指代
        return s.contains("这个")
                || s.contains("那个")
                || s.contains("这种")
                || s.contains("那种")
                || s.contains("上面")
                || s.contains("前面")
                || s.contains("这样")
                || s.contains("上述");
    }
}
