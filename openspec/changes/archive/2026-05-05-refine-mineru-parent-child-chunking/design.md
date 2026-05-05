## Context

当前 MinerU 精准解析流程会将 Markdown 清洗后交给 `SemanticDocumentChunker` 生成字符串切片，再由 `DocumentParseTaskService` 写入 `kb_chunk`，随后 `KnowledgeIndexService` 读取该文档版本下的全部切片并写入对应 Elasticsearch 知识库索引。RAG 查询时，`ElasticsearchHybridChunkRetriever` 返回 ES 命中的 `RetrievedChunk`，`RagAnswerService` 直接将这些切片内容拼入大模型上下文，并由 `ChatService` 将命中切片 ID 写入 `retrieval_log.hit_chunk_ids`。

这个链路目前缺少“检索粒度”和“上下文粒度”的分离：超长 MinerU 语义块如果直接入 ES，会降低召回定位精度；如果硬切为固定字符窗口，又容易破坏句子和条款完整性。新的设计需要让子分块承担检索职责，让父分块承担大模型上下文职责，同时保留原始命中 ID 供审计和指标评估。

文档清洗配置方面，`removed-sample-limit` 和 `removed-sample-max-chars` 实际只控制清洗日志预览样例，但现有注释容易误解为限制清洗范围；本次只澄清注释，不删除配置项。

## Goals / Non-Goals

**Goals:**
- MinerU 超长语义块生成父分块和子分块，父分块保留完整语义块，子分块按自然边界拆分并用于检索。
- 父分块仅保存到 MySQL；普通分块和子分块写入 Elasticsearch。
- RAG 命中子分块后批量加载父分块，并用父分块替代子分块进入大模型上下文。
- 检索日志同时记录 ES 原始命中切片 ID 和最终进入大模型上下文的切片 ID。
- RAG 指标评估和审计能够区分原始命中与上下文命中。
- 澄清文档清洗样例配置注释，避免误解为只清洗部分内容。

**Non-Goals:**
- 不改变原生解析 `NATIVE` 的固定切片策略。
- 不引入新的向量数据库或外部依赖。
- 不改变 MinerU API 调用协议、图片资产上传协议或解析任务重试机制。
- 不删除 `removed-sample-limit` 和 `removed-sample-max-chars` 配置。
- 不重做前端上传流程；除非需要展示新增日志字段，否则前端不是本次重点。

## Decisions

### 1. 在 `kb_chunk` 内表达父子关系，而不是新建父块表

新增字段：
- `chunk_type`：区分 `NORMAL`、`PARENT`、`CHILD`。
- `parent_chunk_id`：仅子分块指向父分块，普通分块和父分块为空。

选择理由：
- 当前索引、图片引用、引用展示和文档版本查询都围绕 `kb_chunk.chunk_id` 工作，保留单表可以最小化调用链改动。
- 父分块和子分块都属于同一文档版本的内容证据，使用同一表便于删除、复制版本和审计。
- 新增枚举比仅靠 `parent_chunk_id` 是否为空更清晰，能区分普通分块和父分块。

备选方案：新建 `kb_parent_chunk` 表。该方案数据模型更强约束，但会增加版本复制、删除清理、引用查询和 RAG 上下文构建的复杂度，本次不采用。

### 2. 子分块入 ES，父分块不入 ES

索引构建时只处理：
- `NORMAL`：普通分块，内容直接入 ES。
- `CHILD`：子分块，内容入 ES，并携带 `parent_chunk_id`、`chunk_type`。

跳过：
- `PARENT`：仅在 MySQL 中作为上下文载体保存。

选择理由：
- 子分块更短，更适合 embedding 和 BM25 精准召回。
- 父分块更完整，更适合大模型生成答案时引用上下文。
- 避免父块和子块同时入 ES 造成重复召回、重复计分和上下文冗余。

### 3. RAG 上下文构建前做父块展开

检索返回后，将切片分为两类：
- 原始命中：ES 直接返回的普通分块或子分块。
- 上下文命中：实际进入大模型上下文的普通分块或父分块。

处理规则：
1. 遍历原始命中列表。
2. 对 `CHILD` 命中收集非空 `parent_chunk_id` 到 `LinkedHashSet`，保持首次命中顺序并去重。
3. 对 `NORMAL` 命中直接保留为上下文候选。
4. 通过 Mapper 按租户批量查询父分块。
5. 用父分块替换子分块，按原始命中的首次顺序生成最终上下文列表。
6. 若父分块缺失或不属于同租户，应跳过该父分块并记录安全日志，不回退发送子分块给大模型。

选择理由：
- 批量查询避免每个命中子块触发一次数据库查询。
- 使用 `Set` 去重避免同一父分块被多个子块重复注入上下文。
- 不回退发送子分块可以保持“子分块只检索、不进大模型”的产品语义。

### 4. 检索日志采用双 ID 字段

字段语义：
- `hit_chunk_ids`：最终进入大模型上下文的切片 ID，包含普通分块和父分块。
- `raw_hit_chunk_ids`：Elasticsearch 原始命中的切片 ID，包含普通分块和子分块。

选择理由：
- `hit_chunk_ids` 继续支持回答复盘和缓存语义。
- `raw_hit_chunk_ids` 支持检索审计、召回分析和 RAG 指标评估。
- 避免用一个字段同时表达两个阶段，降低后续指标统计歧义。

### 5. 清洗样例配置只改注释，不改行为

`removed-sample-limit` 和 `removed-sample-max-chars` 保留现有配置键、环境变量和默认值，仅更新 YAML 与属性注释，明确它们仅影响清洗日志的预览样例数量和长度。

选择理由：
- 避免破坏已有部署环境变量。
- 消除“清洗范围被限制”的误解。
- 保持日志体积保护能力，避免大文档清洗日志过大。

## Risks / Trade-offs

- **父分块过长导致提示词膨胀** → 使用 RAG `topK` 和父块去重控制数量；必要时后续可增加父块最大上下文长度策略，但本次不引入截断以避免破坏完整证据语义。
- **同一父块多个子块命中导致排序不稳定** → 使用原始命中列表中首次出现的位置确定父块顺序，并使用 `LinkedHashSet` 保序去重。
- **父块丢失或历史数据无父子字段** → 新增字段提供默认普通分块语义；父块缺失时跳过异常父块并记录日志，历史普通分块仍可检索和进入上下文。
- **ES 索引映射需要兼容新增字段** → 索引写入和解析均应允许旧索引无 `chunk_type`、`parent_chunk_id` 字段；新增字段应作为普通 keyword/long 源字段参与返回。
- **RAG 指标口径变化** → 明确指标评估优先使用 `raw_hit_chunk_ids` 衡量检索阶段，保留 `hit_chunk_ids` 衡量最终上下文阶段，避免父子替换后指标失真。
- **清洗配置注释变更容易遗漏 Java/YAML 同步** → `application.yaml` 和 `DocumentProcessingProperties.Cleaning` 的注释必须同步修改，且不改变配置键。

## Migration Plan

1. 新增数据库字段：`kb_chunk.chunk_type`、`kb_chunk.parent_chunk_id`、`retrieval_log.raw_hit_chunk_ids`，字段和索引注释必须为中文。
2. 为历史 `kb_chunk` 数据设置 `chunk_type = 'NORMAL'`，历史 `parent_chunk_id` 为空。
3. 为历史 `retrieval_log` 保持 `raw_hit_chunk_ids` 为空；新请求开始写入该字段。
4. 更新实体、Mapper、枚举和 schema.sql，同时提供增量 SQL。
5. 更新索引写入逻辑后，重新索引 MinerU 文档可获得父子分块检索效果；历史 ES 数据未重建前仍按普通块兼容。
6. 回滚时可停止使用新增字段并恢复普通分块索引逻辑；数据库新增字段可保留，不影响旧代码读取核心字段。

## Open Questions

- 对“单个句子本身超过 `chunk-size`”的极端场景，是否允许按逗号、顿号、冒号、括号、换行等次级边界拆分？本设计倾向允许，并要求不再使用旧 `fallback-overlap` 字符窗口语义。 answr:允许，优先使用自然边界拆分，避免无边界字符截断导致的上下文不连贯。
- `raw_hit_chunk_ids` 是否需要同步展示到前端 RAG 指标详情页？本设计要求后端持久化与接口可扩展，前端展示可在实现时根据现有页面复杂度决定。
