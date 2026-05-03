## Context

当前知识库 Elasticsearch 写入由 `KnowledgeIndexService` 根据文档业务类型选择索引：知识库文档统一写入 `elasticsearch.index.kb-chunks`，风险规则写入 `elasticsearch.index.risk-rule`。智能问答 RAG 检索由 `ElasticsearchHybridChunkRetriever` 调用 `ElasticsearchChunkStore.hybridSearchRrf`，默认只查询 `kb-chunks` 索引。

MinerU 精准解析已经接入文档导入链路，`kb_document.parse_method` 可以标识文档来自原生解析或 MinerU 精准解析。新需求要求只在知识库场景将 MinerU 文档写入独立索引 `legal_kb_chunks_mineru`，并让管理员在管理页控制智能问答检索哪些知识库索引。

## Goals / Non-Goals

**Goals:**

- 知识库文档按解析方式路由到不同 ES 索引：原生解析使用原索引，MinerU 使用 `legal_kb_chunks_mineru`。
- 智能问答检索支持原生索引、MinerU 索引、双索引三种范围。
- 管理页提供可保存的运行时控制入口，管理员可配置智能问答知识库检索范围。
- 默认行为兼容现有系统：未配置时仍查询原生知识库索引。

**Non-Goals:**

- 不改变风险规则文档索引 `legal_risk_rule` 的写入和查询行为。
- 不改变天眼审查文档的非 ES 知识库索引行为。
- 不做历史数据自动重建；历史原生索引中的 MinerU 文档如需迁移，由管理员重新索引或后续单独提供迁移任务。
- 不改变 embedding 模型、向量维度或 ES mapping 结构。

## Decisions

### 1. 使用配置项定义 MinerU 知识库索引名称

新增 `elasticsearch.index.kb-chunks-mineru`，默认值 `legal_kb_chunks_mineru`。这样可以保持代码不硬编码索引名，并支持不同环境自定义。

备选方案是直接在索引服务写死 `legal_kb_chunks_mineru`，但不利于测试环境和生产环境隔离。

### 2. 索引路由放在 `KnowledgeIndexService`

`KnowledgeIndexService` 已经掌握文档业务类型和 `KbDocumentEntity`，适合根据 `bizType + parseMethod` 决定目标索引。只有 `bizType=KNOWLEDGE` 且 `parseMethod=MINERU_PRECISE` 时使用 MinerU 索引；其他知识库文档继续使用原生索引。

备选方案是在 `ElasticsearchChunkStore` 根据 payload 决策，但 payload 当前没有解析方式，会扩大改动面。

### 3. RAG 检索通过“索引范围”配置控制

新增智能问答知识库索引范围枚举，包含：

- `NATIVE_ONLY`：只查原生知识库索引。
- `MINERU_ONLY`：只查 MinerU 知识库索引。
- `BOTH`：同时查询两个索引并合并结果。

配置持久化到数据库，提供管理接口读取和保存；配置缺失时使用 YAML 默认值，确保部署后无需立即初始化数据也能运行。

### 4. 双索引检索在服务层合并

`ElasticsearchChunkStore` 增加支持指定多个索引检索的能力。双索引模式分别执行向量和 BM25 查询，再用现有 RRF 合并，避免依赖 ES 多索引查询在 kNN 场景下的版本差异。

备选方案是直接请求 `indexA,indexB/_search`，实现更短，但不同 ES 版本对多索引 kNN 的兼容性风险更高。

### 5. 管理入口复用知识库管理页

当前已有管理员知识库页面和 `/api/knowledge/admin` 控制器。新增配置读取/保存接口并在知识库管理页展示，可减少新增路由和导航成本。

## Risks / Trade-offs

- **历史 MinerU 文档仍在旧索引** → 本需求不自动迁移历史数据；管理员可对文档重新索引，或后续补独立迁移任务。
- **双索引检索成本增加** → 只在管理员选择 `BOTH` 时发生，且仍受 topK、vectorTopK、bm25TopK 限制。
- **配置缺失导致问答无结果** → 后端保存配置时校验范围枚举，读取失败时回退到默认 `NATIVE_ONLY`。
- **索引删除不完整** → 删除知识库文档时需要同时清理原生索引和 MinerU 索引，避免历史路由变更后残留。

## Migration Plan

1. 新增数据库配置表或配置行，保存智能问答知识库检索索引范围。
2. 新增 `elasticsearch.index.kb-chunks-mineru` 与默认检索范围配置。
3. 部署后默认只查询原生索引，保持现有智能问答行为。
4. 管理员确认 MinerU 文档重新索引完成后，可在管理页切换到 `MINERU_ONLY` 或 `BOTH`。
5. 回滚时将检索范围设置为 `NATIVE_ONLY`，保留 MinerU 索引不影响原系统。

## Open Questions

- 是否需要后续提供历史 MinerU 文档批量重建到 `legal_kb_chunks_mineru` 的管理按钮？当前不纳入本次实现。
