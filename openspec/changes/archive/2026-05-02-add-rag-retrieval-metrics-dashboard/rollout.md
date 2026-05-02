## RAG 检索指标发布说明

### 评估方法

- Recall 使用 `TP / (TP + FN)`。
- Precision 使用 `TP / (TP + FP)`。
- TP 表示被 LLM 判定为相关的原始命中 chunk ID 数量。
- FP 表示被 LLM 判定为不相关的原始命中 chunk ID 数量。
- FN 表示进入 LLM 判断的粗召回候选 chunk ID 中，被判定为相关但不在原始命中 chunk ID 列表中的数量。
- Worker 会先粗召回 TopN 候选，再按配置比例和最大候选数量限制发送给 LLM，以降低上下文长度和幻觉风险。
- 空响应、格式错误或非 JSON 的 LLM 输出一律视为 `FAILED`，不作为成功指标样本。
- 问候语、测试语句、过短 prompt 等低质量 query 会在模型判断前被过滤。

### 数据来源

- 来源表：`retrieval_log`。
- 主键：`id`。
- Query 字段：`query_text`。
- 租户字段：`tenant_id`。
- 原始命中字段：`hit_chunk_ids`，以逗号分隔的 chunk ID。
- 时间字段：`created_at`。

### YAML 配置

- `rag.metric.retrieval.enabled`：控制定时 Worker 是否运行。
- `rag.metric.retrieval.worker.cron`：控制定时评估执行时间。
- `rag.metric.retrieval.worker.batch-size`：限制每次运行评估的日志数量。
- `rag.metric.retrieval.worker.max-daily-samples`：限制每日扫描样本池大小。
- `rag.metric.retrieval.worker.concurrency`：预留的并发限制。
- `rag.metric.retrieval.worker.timeout`：预留的单条日志处理超时。
- `rag.metric.retrieval.candidate-recall.top-n`：粗召回 TopN 候选数量，默认 200。
- `rag.metric.retrieval.candidate-recall.judge-candidate-ratio`：发送给 LLM 的粗召回候选比例，默认 5%。
- `rag.metric.retrieval.candidate-recall.min-judge-candidates`：LLM 候选判断数量下限。
- `rag.metric.retrieval.candidate-recall.max-judge-candidates`：LLM 候选判断数量上限。
- `rag.metric.retrieval.candidate-recall.max-chunk-chars`：每个 chunk 发送给 LLM 的最大字符数。
- `rag.metric.retrieval.query-filter.min-query-length`：参与评估的最小有效 query 长度。
- `rag.metric.retrieval.query-filter.excluded-queries`：从评估中跳过的问候语或低价值 query。
- `rag.metric.retrieval.judge-model.prompt-version`：指标评估提示词版本。
- `rag.metric.retrieval.judge-model.model-name`：专用评估模型名称；为空表示复用在线问答模型。

### 成本控制

- 在数据库 schema 应用和小样本验证完成前，保持 `rag.metric.retrieval.enabled=false`。
- 初始使用较小的 `batch-size` 和 `max-daily-samples`。
- 观察模型成本和延迟后，再逐步提高 TopN。

### 回滚

- 关闭 `rag.metric.retrieval.enabled`。
- 保留 `rag_retrieval_metric_evaluation` 数据用于审计；如不再需要，可手动删除。
- 指标评估为离线异步流程，不影响在线 RAG 查询链路。
