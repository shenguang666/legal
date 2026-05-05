## Why

MinerU 精准解析后的 Markdown 可能生成超过目标 `chunk-size` 的长语义块；当前兜底字符窗口容易截断句子，也会让超长证据直接进入向量库和大模型上下文，影响检索精度、上下文可读性和审计可解释性。

同时，文档清洗配置 `removed-sample-limit` 与 `removed-sample-max-chars` 的现有注释容易被理解为“只清洗部分内容”，需要澄清它们仅控制清洗日志预览样例，不影响全文清洗和原始文档完整保留。

## What Changes

- 对 MinerU 超长语义块引入父子分块：父分块保存完整超长语义块，子分块按段落、句子或符号边界拆分，并允许子分块之间最多 15% 的语义重叠。
- 父分块仅保存到 MySQL，不写入 Elasticsearch；子分块保存到 MySQL 并写入 Elasticsearch，用于向量和 BM25 检索。
- 子分块携带父分块 `chunk_id`，RAG 检索命中子分块时使用 `Set` 收集父分块 ID，并通过批量查询加载父分块，用父分块替代子分块进入大模型上下文。
- 新增检索日志字段记录 Elasticsearch 原始命中的切片 ID；现有最终命中字段记录实际进入大模型上下文的父分块或普通分块 ID。
- 调整索引构建逻辑，仅索引普通分块和子分块，并在子分块索引文档中携带父分块元数据。
- 更新文档清洗配置与属性注释，明确 `removed-sample-limit`、`removed-sample-max-chars` 只影响清洗日志中被移除内容预览样例的数量和长度，不限制全文清洗范围，也不影响原始文档完整保留。

## Capabilities

### New Capabilities
- `rag-parent-context-expansion`: 定义 RAG 检索命中子分块后批量加载父分块、用父分块替换子分块构建大模型上下文，并同时保留原始命中审计信息的能力。

### Modified Capabilities
- `semantic-document-chunking`: MinerU 超长语义块需要生成父子分块，子分块应尽量保持完整句子，并使用 15% 重叠率替代原字符窗口 overlap 语义。
- `kb-index-routing-by-parse-method`: MinerU 父分块不得写入 Elasticsearch，子分块写入 MinerU 知识库索引并携带父分块标识。
- `document-cleaning-pipeline`: 文档清洗日志样例配置的语义需要明确为日志预览控制，而非清洗范围控制。
- `rag-retrieval-daily-metrics`: RAG 指标与日志需要区分“最终进入大模型上下文的命中切片”和“Elasticsearch 原始命中切片”。

## Impact

- 后端数据库：`kb_chunk` 需要新增父子分块相关字段；`retrieval_log` 需要新增原始命中切片 ID 字段；相关 schema 与增量 SQL 必须包含中文注释。
- 后端实体与 Mapper：`KbChunkEntity`、`RetrievalLogEntity` 及批量查询父分块的 Mapper 方法需要更新，新增字段属性必须包含中文注释。
- 分块流程：MinerU Markdown 语义切片需要输出父子分块结构，普通解析流程保持兼容。
- 索引流程：`KnowledgeIndexService`、`ChunkIndexPayload`、`ElasticsearchChunkStore` 需要支持跳过父分块、索引子分块父级元数据、解析原始命中元数据。
- RAG 流程：`RetrievedChunk`、`ChunkRetriever`、`RagAnswerService`、`ChatService` 的上下文构建、引用、缓存和检索日志写入需要区分原始命中与最终上下文命中。
- 指标流程：RAG 检索指标评估需要能够读取原始命中 ID，并继续保留最终上下文命中 ID 便于回答复盘。
- 配置文档：`application.yaml` 与 `DocumentProcessingProperties.Cleaning` 注释需要澄清清洗样例配置含义。
