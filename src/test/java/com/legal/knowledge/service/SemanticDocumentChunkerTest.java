package com.legal.knowledge.service;

import com.legal.enums.KbChunkType;
import com.legal.config.DocumentProcessingProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticDocumentChunkerTest {

    @Test
    void shouldKeepHeadingsClausesAndTablesAsSemanticBlocks() {
        DocumentProcessingProperties properties = new DocumentProcessingProperties();
        properties.getMineru().setChunkSize(120);
        properties.getMineru().setMinChunkSize(30);
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
        properties.getMineru().setChunkSize(35);
        properties.getMineru().setMinChunkSize(10);
        SemanticDocumentChunker chunker = new SemanticDocumentChunker(properties, new DocumentChunker());

        String markdown = "第一句内容用于测试。第二句内容用于测试。第三句内容用于测试。第四句内容用于测试。第五句内容用于测试。"
                + "第六句内容用于测试。第七句内容用于测试。第八句内容用于测试。第九句内容用于测试。第十句内容用于测试。"
                + "第十一句内容用于测试。第十二句内容用于测试。第十三句内容用于测试。";

        List<String> chunks = chunker.chunkMarkdown(markdown);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(100));
    }

    @Test
    void shouldUseConfiguredMaxMergeSizeForMineruMarkdown() {
        DocumentProcessingProperties properties = new DocumentProcessingProperties();
        properties.getMineru().setChunkSize(300);
        properties.getMineru().setMaxMergeSize(120);
        properties.getMineru().setMinChunkSize(1);
        SemanticDocumentChunker chunker = new SemanticDocumentChunker(properties, new DocumentChunker());
        String block = "A".repeat(40);
        String markdown = "# Title\n" + block + "\n\n"
                + "1. Payment\n" + block + "\n\n"
                + "2. Invoice\n" + block + "\n\n"
                + "3. Liability\n" + block;

        List<String> chunks = chunker.chunkMarkdown(markdown);

        assertThat(chunks).hasSize(2);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(120));
    }

    @Test
    void shouldIgnoreMergeSizeAndMinChunkSizeWhenMergeDisabled() {
        DocumentProcessingProperties properties = new DocumentProcessingProperties();
        properties.getMineru().setChunkSize(300);
        properties.getMineru().setMergeEnabled(false);
        properties.getMineru().setMaxMergeSize(300);
        properties.getMineru().setMinChunkSize(300);
        SemanticDocumentChunker chunker = new SemanticDocumentChunker(properties, new DocumentChunker());
        String block = "A".repeat(40);
        String markdown = "# Title\n" + block + "\n\n"
                + "1. Payment\n" + block + "\n\n"
                + "2. Invoice\n" + block;

        List<String> chunks = chunker.chunkMarkdown(markdown);

        assertThat(chunks).hasSize(3);
        assertThat(chunks).noneSatisfy(chunk -> assertThat(chunk).contains("Title", "Payment"));
    }

    @Test
    void shouldBuildParentChildChunksForOversizedMineruBlock() {
        DocumentProcessingProperties properties = new DocumentProcessingProperties();
        SemanticDocumentChunker chunker = new SemanticDocumentChunker(properties, new DocumentChunker());
        String sentence = "甲方应当按照合同约定履行付款义务并保留完整凭证。";
        String markdown = "第一条 付款安排\n" + sentence.repeat(30);

        List<SemanticChunk> chunks = chunker.chunkMarkdownStructured(markdown, 120, 10);

        assertThat(chunks).extracting(SemanticChunk::chunkType).contains(KbChunkType.PARENT, KbChunkType.CHILD);
        SemanticChunk parent = chunks.stream().filter(chunk -> chunk.chunkType() == KbChunkType.PARENT).findFirst().orElseThrow();
        assertThat(parent.content()).contains(sentence.repeat(2));
        assertThat(chunks.stream().filter(chunk -> chunk.chunkType() == KbChunkType.CHILD)).allSatisfy(chunk -> {
            assertThat(chunk.parentGroup()).isEqualTo(parent.parentGroup());
            assertThat(chunk.content().length()).isLessThanOrEqualTo(120);
        });
    }

    @Test
    void shouldOverlapChildChunksBySemanticTailWithinFifteenPercent() {
        DocumentProcessingProperties properties = new DocumentProcessingProperties();
        SemanticDocumentChunker chunker = new SemanticDocumentChunker(properties, new DocumentChunker());
        String markdown = "第一条 验收安排\n" + "甲方完成验收。乙方提交票据。丙方保留记录。丁方协助审计。".repeat(12);

        List<SemanticChunk> children = chunker.chunkMarkdownStructured(markdown, 80, 10).stream()
                .filter(chunk -> chunk.chunkType() == KbChunkType.CHILD)
                .toList();

        assertThat(children).hasSizeGreaterThan(1);
        String first = children.get(0).content();
        String second = children.get(1).content();
        assertThat(second).contains(lastNaturalUnit(first));
        assertThat(lastNaturalUnit(first).length()).isLessThanOrEqualTo(12);
    }

    @Test
    void shouldSplitSingleLongSentenceBySecondaryNaturalBoundary() {
        DocumentProcessingProperties properties = new DocumentProcessingProperties();
        SemanticDocumentChunker chunker = new SemanticDocumentChunker(properties, new DocumentChunker());
        String markdown = "第一条 超长句处理\n" + "甲方负责资料提交，乙方负责票据审核，丙方负责金额复核，丁方负责归档确认，".repeat(12);

        List<SemanticChunk> children = chunker.chunkMarkdownStructured(markdown, 100, 10).stream()
                .filter(chunk -> chunk.chunkType() == KbChunkType.CHILD)
                .toList();

        assertThat(children).hasSizeGreaterThan(1);
        assertThat(children).allSatisfy(chunk -> assertThat(chunk.content().length()).isLessThanOrEqualTo(100));
        assertThat(children).anySatisfy(chunk -> assertThat(chunk.content()).contains("，"));
    }

    private String lastNaturalUnit(String content) {
        String[] units = content.split("(?<=[。！？；;!?])|\\n+");
        return units[units.length - 1].trim();
    }}
