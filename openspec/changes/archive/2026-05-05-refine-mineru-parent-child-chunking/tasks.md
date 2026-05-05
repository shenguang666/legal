## 1. 数据模型与迁移

- [x] 1.1 新增 `KbChunkType` 枚举并为每个枚举值添加中文注释，覆盖 `NORMAL`、`PARENT`、`CHILD` 语义。
- [x] 1.2 更新 `kb_chunk` 表结构，新增带中文注释的 `chunk_type`、`parent_chunk_id` 字段，并为历史数据设置普通分块默认语义。
- [x] 1.3 更新 `retrieval_log` 表结构，新增带中文注释的 `raw_hit_chunk_ids` 字段，用于保存 Elasticsearch 原始命中切片 ID。
- [x] 1.4 同步更新 `schema.sql` 与增量 SQL，确保所有新增数据库字段都有中文注释和必要索引。
- [x] 1.5 更新 `KbChunkEntity`、`RetrievalLogEntity` 及相关 DTO/缓存载荷属性，所有新增实体属性添加中文注释。

## 2. MinerU 父子分块生成

- [x] 2.1 扩展语义切片结果模型，使 MinerU 切片流程能够表达普通分块、父分块和子分块及其父子关系。
- [x] 2.2 调整 `SemanticDocumentChunker` 的 MinerU 超长语义块处理逻辑，超过 `chunk-size` 时生成完整父分块和按自然边界拆分的子分块。
- [x] 2.3 将旧 `fallback-overlap` 字符窗口语义从 MinerU 子分块路径移除，改为子分块之间不超过 15% 的语义重叠。
- [x] 2.4 为单句超长场景实现次级自然边界拆分，优先使用逗号、顿号、冒号、括号、换行等边界，避免无边界字符截断。
- [x] 2.5 调整 `DocumentParseTaskService` 的切片持久化逻辑，确保父分块先入库并将其 `chunk_id` 写入对应子分块。
- [x] 2.6 保持原生解析和普通 MinerU 短块的现有行为兼容，历史普通分块按 `NORMAL` 处理。

## 3. Elasticsearch 索引与检索元数据

- [x] 3.1 更新 `KbChunkMapper` 查询，支持按文档版本读取分块类型和父分块 ID，并新增按租户批量查询父分块的方法。
- [x] 3.2 更新 `KnowledgeIndexService`，仅将普通分块和子分块写入 Elasticsearch，跳过父分块且不为父分块生成 Embedding。
- [x] 3.3 更新 `ChunkIndexPayload` 与 `ElasticsearchChunkStore`，在子分块索引文档中写入并解析 `chunk_type`、`parent_chunk_id`。
- [x] 3.4 确保向量检索、BM25 检索和 RRF 合并返回原始命中切片的父子元数据，兼容旧索引缺少新增字段的情况。

## 4. RAG 父块上下文展开与日志

- [x] 4.1 扩展 `RetrievedChunk` 或新增检索结果模型，区分 Elasticsearch 原始命中切片与最终上下文切片。
- [x] 4.2 在 RAG 上下文构建前实现父块展开：用 `Set` 收集子分块 `parent_chunk_id`，批量查询父分块，并用父分块替换子分块进入大模型上下文。
- [x] 4.3 处理多个子分块命中同一父分块的去重与排序，按父分块首次命中顺序构建最终上下文。
- [x] 4.4 父分块缺失或租户不匹配时跳过异常父分块并记录日志，不将子分块回退发送给大模型。
- [x] 4.5 更新引用构建逻辑，基于最终上下文分块生成 citation，并按最终上下文分块 ID 查询图片证据。
- [x] 4.6 更新非流式与流式 `ChatService` 日志写入：`hit_chunk_ids` 保存最终上下文分块 ID，`raw_hit_chunk_ids` 保存 Elasticsearch 原始命中分块 ID。
- [x] 4.7 更新回答缓存载荷与缓存命中日志逻辑，保持缓存中的命中切片 ID 与最终上下文命中语义一致。

## 5. RAG 指标与清洗注释澄清

- [x] 5.1 更新 RAG 指标评估读取逻辑，优先使用 `raw_hit_chunk_ids` 作为原始检索命中 ID，缺失时兼容回退到 `hit_chunk_ids`。
- [x] 5.2 如接口或页面展示检索明细，补充最终上下文命中与原始命中 ID 的区分展示或响应字段。
- [x] 5.3 更新 `application.yaml` 中 `removed-sample-limit`、`removed-sample-max-chars` 的中文注释，明确它们仅控制清洗日志预览样例，不限制全文清洗范围。
- [x] 5.4 更新 `DocumentProcessingProperties.Cleaning` 中对应属性的中文注释，保持与 YAML 注释一致。

## 6. 验证

- [x] 6.1 新增或更新 `SemanticDocumentChunkerTest`，覆盖 MinerU 父子分块、15% 子分块重叠、单句超长次级边界拆分。
- [x] 6.2 新增或更新文档解析服务测试，验证父分块先入库、子分块带 `parent_chunk_id`、低质量过滤不破坏父子关系。
- [x] 6.3 新增或更新索引服务测试，验证父分块不入 ES，子分块入 ES 且携带父级元数据。
- [x] 6.4 新增或更新 RAG 服务测试，验证命中子分块后批量加载父分块并只将父分块发送给大模型。
- [x] 6.5 新增或更新检索日志与指标测试，验证 `hit_chunk_ids` 与 `raw_hit_chunk_ids` 的写入和兼容回退。
- [x] 6.6 运行后端编译与相关单元测试，确认数据库字段、实体注释、YAML 注释和 OpenSpec 要求一致。
