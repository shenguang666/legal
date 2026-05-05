## MODIFIED Requirements

### Requirement: 知识库文档按解析方式写入不同索引
系统 SHALL 在知识库文档建立 Elasticsearch 索引时，根据文档解析方式选择目标索引：原生解析文档写入原知识库索引，MinerU 精准解析文档写入 MinerU 知识库索引 `legal_kb_chunks_mineru` 或其配置值；当 MinerU 切片包含图片描述时，图片描述 MUST 作为切片内容的一部分进入对应索引；当 MinerU 文档产生父子分块时，父分块 MUST 仅保存在数据库，子分块 MUST 写入 MinerU 知识库索引用于检索。

#### Scenario: 原生知识库文档写入原索引
- **WHEN** 知识库文档的解析方式为 `NATIVE` 并触发索引任务
- **THEN** 系统 MUST 将该文档切片写入原知识库索引

#### Scenario: MinerU 知识库文档写入 MinerU 索引
- **WHEN** 知识库文档的解析方式为 `MINERU_PRECISE` 并触发索引任务
- **THEN** 系统 MUST 将该文档可检索切片写入 MinerU 知识库索引

#### Scenario: MinerU 知识库切片包含图片描述
- **WHEN** MinerU 知识库文档切片内容包含由图片生成的中文描述
- **THEN** 系统 MUST 将该描述随切片正文一起生成 Embedding
- **THEN** 系统 MUST 允许 BM25 和向量检索命中该图片描述所在切片

#### Scenario: MinerU 父分块不写入 Elasticsearch
- **WHEN** MinerU 知识库文档生成父分块并触发索引任务
- **THEN** 系统 MUST 将父分块保留在 MySQL `kb_chunk` 表中
- **THEN** 系统 MUST 不为父分块生成 Embedding
- **THEN** 系统 MUST 不将父分块写入任何 Elasticsearch 知识库索引

#### Scenario: MinerU 子分块写入 Elasticsearch 并携带父级标识
- **WHEN** MinerU 知识库文档生成子分块并触发索引任务
- **THEN** 系统 MUST 为子分块生成 Embedding 并写入 MinerU 知识库索引
- **THEN** Elasticsearch 文档源数据 MUST 包含子分块自身 `chunk_id`
- **THEN** Elasticsearch 文档源数据 MUST 包含对应父分块的 `parent_chunk_id`
- **THEN** Elasticsearch 文档源数据 MUST 包含可区分普通分块、父分块和子分块的分块类型
