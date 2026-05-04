## MODIFIED Requirements

### Requirement: 知识库文档按解析方式写入不同索引
系统 SHALL 在知识库文档建立 Elasticsearch 索引时，根据文档解析方式选择目标索引：原生解析文档写入原知识库索引，MinerU 精准解析文档写入 MinerU 知识库索引 `legal_kb_chunks_mineru` 或其配置值；当 MinerU 切片包含图片描述时，图片描述 MUST 作为切片内容的一部分进入对应索引。

#### Scenario: 原生知识库文档写入原索引
- **WHEN** 知识库文档的解析方式为 `NATIVE` 并触发索引任务
- **THEN** 系统 MUST 将该文档切片写入原知识库索引

#### Scenario: MinerU 知识库文档写入 MinerU 索引
- **WHEN** 知识库文档的解析方式为 `MINERU_PRECISE` 并触发索引任务
- **THEN** 系统 MUST 将该文档切片写入 MinerU 知识库索引

#### Scenario: MinerU 知识库切片包含图片描述
- **WHEN** MinerU 知识库文档切片内容包含由图片生成的中文描述
- **THEN** 系统 MUST 将该描述随切片正文一起生成 Embedding
- **THEN** 系统 MUST 允许 BM25 和向量检索命中该图片描述所在切片
