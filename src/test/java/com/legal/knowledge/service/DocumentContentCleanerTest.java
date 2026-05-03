package com.legal.knowledge.service;

import com.legal.config.DocumentProcessingProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentContentCleanerTest {

    @Test
    void shouldRemoveBuiltInNoiseAndKeepLegalHeadings() {
        DocumentContentCleaner cleaner = new DocumentContentCleaner(new DocumentProcessingProperties());
        String content = """
                官网导出
                下载时间：2026-05-04
                1 / 10
                违约责任
                甲方逾期付款的，应承担违约责任。
                https://example.com
                版权所有 © 示例公司
                """;

        DocumentCleaningResult result = cleaner.cleanText(content);

        assertThat(result.getContent()).contains("违约责任", "甲方逾期付款");
        assertThat(result.getContent()).doesNotContain("下载时间", "版权所有", "https://example.com", "1 / 10");
        assertThat(result.getReport().getRemovedLineCount()).isGreaterThanOrEqualTo(4);
        assertThat(result.getReport().getReasonSummary()).containsKeys("EXPORT_METADATA", "PAGE_NUMBER", "COPYRIGHT", "STANDALONE_URL");
    }

    @Test
    void shouldRemoveRepeatedHeaderFooterWithoutRemovingRepeatedBodyClause() {
        DocumentCleaningResult result = new DocumentContentCleaner(new DocumentProcessingProperties()).cleanText(
                "某某官网\n第一条 付款义务\n甲方应按期付款。\n甲方应按期付款。\n第 1 页\n"
                        + "\f\n某某官网\n第二条 交付义务\n甲方应按期付款。\n乙方应按期交付。\n第 2 页\n"
                        + "\f\n某某官网\n争议解决\n甲方应按期付款。\n双方提交仲裁。\n第 3 页"
        );

        assertThat(result.getContent()).doesNotContain("某某官网", "第 1 页", "第 2 页", "第 3 页");
        assertThat(result.getContent()).contains("甲方应按期付款。", "争议解决");
        assertThat(result.getReport().getReasonSummary()).containsKey("REPEATED_HEADER_FOOTER");
    }

    @Test
    void shouldKeepMarkdownStructureAndFilterLowQualityChunks() {
        DocumentContentCleaner cleaner = new DocumentContentCleaner(new DocumentProcessingProperties());
        DocumentCleaningResult result = cleaner.cleanMarkdown("""
                # 付款条款
                | 项目 | 金额 |
                | --- | --- |
                | 服务费 | 1000 |
                第 1 页
                打印时间：2026-05-04
                第一条 甲方应在验收后付款。
                """);

        assertThat(result.getContent()).contains("# 付款条款", "| 服务费 | 1000 |", "第一条甲方应在验收后付款。");
        assertThat(result.getContent()).doesNotContain("打印时间", "第 1 页");
        assertThat(cleaner.isLowQualityChunk("第 1 页")).isTrue();
        assertThat(cleaner.isLowQualityChunk("第一条 甲方应在验收后付款。")).isFalse();
    }
}
