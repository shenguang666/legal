package com.legal.knowledge.service;

import com.legal.config.DocumentProcessingProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticDocumentChunkerTest {

    @Test
    void shouldKeepHeadingsClausesAndTablesAsSemanticBlocks() {
        DocumentProcessingProperties properties = new DocumentProcessingProperties();
        properties.getChunking().setMaxChunkSize(120);
        properties.getChunking().setMinChunkSize(30);
        SemanticDocumentChunker chunker = new SemanticDocumentChunker(properties, new DocumentChunker());

        String markdown = """
                # 合同付款条款
                甲方应在验收完成后十日内支付首期款。

                第一条 付款安排
                乙方开具合规发票后，甲方按照约定支付。

                | 项目 | 金额 |
                | --- | --- |
                | 服务费 | 1000 |
                """;

        List<String> chunks = chunker.chunkMarkdown(markdown);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).anySatisfy(chunk -> assertThat(chunk).contains("# 合同付款条款"));
        assertThat(chunks).anySatisfy(chunk -> assertThat(chunk).contains("第一条 付款安排"));
        assertThat(chunks).anySatisfy(chunk -> assertThat(chunk).contains("| 项目 | 金额 |", "| 服务费 | 1000 |"));
    }

    @Test
    void shouldSplitOversizedBlockBySentenceBoundary() {
        DocumentProcessingProperties properties = new DocumentProcessingProperties();
        properties.getChunking().setMaxChunkSize(35);
        properties.getChunking().setMinChunkSize(10);
        SemanticDocumentChunker chunker = new SemanticDocumentChunker(properties, new DocumentChunker());

        String markdown = "第一句内容用于测试。第二句内容用于测试。第三句内容用于测试。第四句内容用于测试。第五句内容用于测试。"
                + "第六句内容用于测试。第七句内容用于测试。第八句内容用于测试。第九句内容用于测试。第十句内容用于测试。"
                + "第十一句内容用于测试。第十二句内容用于测试。第十三句内容用于测试。";

        List<String> chunks = chunker.chunkMarkdown(markdown);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(100));
    }
}
