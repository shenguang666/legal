package com.legal.chat.cache;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.Normalizer;

/**
 * 问题归一化：用于生成稳定的缓存 key（Exact / Semantic）。
 */
@Component
public class QuestionNormalizer {

    public String normalize(String question) {
        if (!StringUtils.hasText(question)) {
            return "";
        }
        String s = question.trim();
        // Unicode 规范化，兼容全角/半角、组合字符等。
        s = Normalizer.normalize(s, Normalizer.Form.NFKC);
        s = s.toLowerCase();
        // 统一空白
        s = s.replaceAll("\\s+", " ");
        // 统一常见中文/英文标点（保守处理：只做替换不删除）
        s = s.replace('，', ',')
                .replace('。', '.')
                .replace('；', ';')
                .replace('：', ':')
                .replace('？', '?')
                .replace('！', '!')
                .replace('（', '(')
                .replace('）', ')')
                .replace('【', '[')
                .replace('】', ']');
        return s;
    }
}
