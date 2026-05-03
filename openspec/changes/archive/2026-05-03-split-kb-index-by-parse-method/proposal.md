## Why

当前知识库文档无论使用原生解析还是 MinerU 精准解析，都会写入同一个知识库 Elasticsearch 索引，智能问答无法按解析质量、来源策略或灰度需求选择检索范围。随着 MinerU 精准解析引入，需要将知识库索引按解析方式隔离，并让管理员可控地决定智能问答查询哪些索引。

## What Changes

- 知识库文档按照解析方式写入不同 Elasticsearch 索引：原生解析继续使用现有知识库索引，MinerU 精准解析使用新索引 `legal_kb_chunks_mineru`。
- 仅知识库文档执行解析方式分索引；风险规则文档、天眼审查文档和用户外挂知识索引保持现有行为。
- 智能问答检索支持按管理配置选择查询原生知识库索引、MinerU 知识库索引或两者合并结果。
- 管理页新增可视化控制入口，用于查看和保存智能问答知识库检索索引范围。
- 新增配置与持久化能力，确保默认兼容现有知识库检索行为，并支持后续运行时调整。

## Capabilities

### New Capabilities

- `kb-index-routing-by-parse-method`: 定义知识库文档根据解析方式写入不同 Elasticsearch 索引的行为。
- `qa-knowledge-index-scope-management`: 定义管理员控制智能问答知识库检索索引范围的行为。

### Modified Capabilities

- 无。

## Impact

- 后端：Elasticsearch 配置、知识库索引服务、Elasticsearch 检索服务、RAG 检索器、管理接口、数据库配置表或等价持久化结构。
- 前端：管理员知识库管理页或新增管理区域，用于配置智能问答检索索引范围。
- 数据库：新增保存智能问答知识库检索索引范围的配置项或表结构，字段和实体属性需带中文注释。
- Elasticsearch：新增知识库 MinerU 索引 `legal_kb_chunks_mineru`，使用与现有知识库 chunk 索引一致的 mapping。
