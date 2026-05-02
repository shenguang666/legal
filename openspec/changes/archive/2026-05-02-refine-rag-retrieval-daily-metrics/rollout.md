## 日指标口径

- 日汇总以 `tenant_id + metric_date + evaluation_version` 为唯一口径。
- `metric_date` 是业务日期；定时任务默认处理上一自然日，手动接口 `/api/rag-metrics/run?date=yyyy-MM-dd` 可指定日期。
- `total_rag_message_count` 统计当天有 `hit_chunk_ids` 的 RAG 检索日志。
- `valid_message_count` 统计进入评估流程的消息数，等于成功数加失败数。
- `filtered_message_count` 统计低质量 query 过滤消息。
- 平均召回率和平均精确率只基于 `SUCCESS` 明细计算，失败和过滤单独计数。

## 状态生命周期

- `retrieval_log.rag_metric_scan_status`：`PENDING` 待扫描，`PROCESSING` 处理中，`SUCCESS` 成功，`FAILED` 失败可重试，`FILTERED` 被过滤。
- `rag_retrieval_metric_daily_summary.status`：`PROCESSING` 处理中，`COMPLETED` 已完成，`PARTIAL_FAILED` 已完成但存在失败，`FAILED` 系统级失败。
- `rag_retrieval_metric_evaluation.status`：`SUCCESS` 成功，`FAILED` 大模型或评估失败，`SKIPPED` 低质量 query 跳过。

## 配置说明

- `rag.metric.retrieval.worker.cron` 默认是 `0 0/5 2-5 * * ?`，表示低峰期每 5 分钟运行。
- `rag.metric.retrieval.worker.batch-size` 仅控制单批处理量，不限制每日总消息量。
- `max-daily-samples` 已移除，不再作为每日样本上限。
- `rag.metric.retrieval.worker.metric-date-offset-days` 默认 `1`，表示定时任务处理上一自然日。
- `rag.metric.retrieval.worker.max-retries` 控制失败日志最多重试次数。
- `rag.metric.retrieval.query-filter` 保留低质量 query 过滤策略。
- `rag.metric.retrieval.candidate-recall` 保留粗召回 + 精筛候选控制策略。
- `rag.metric.retrieval.judge-model.prompt-version` 当前默认 `rag-metric-v3`，用于隔离旧评估明细。

## 迁移步骤

1. 创建 `rag_retrieval_metric_daily_summary` 日汇总表。
2. 为 `rag_retrieval_metric_evaluation` 增加 `daily_summary_id` 并更新 `status` 字段注释。
3. 为 `retrieval_log` 增加 `rag_metric_scan_status`、`rag_metric_scanned_at`、`rag_metric_summary_id`、`rag_metric_retry_count`、`rag_metric_error_message`。
4. 增加租户、日期、状态、汇总 ID 相关索引。
5. 旧明细不强制回填；新版本通过 `rag-metric-v3` 与旧数据隔离。

## 回滚方案

- 关闭 `RAG_METRIC_RETRIEVAL_ENABLED=false` 停止定时任务。
- 前端可隐藏 RAG 指标菜单或回退到旧构建。
- 新增表和字段可保留不影响在线检索写入。
- 如需数据库回滚，可删除新增索引/字段和日汇总表，但应先备份 `rag_retrieval_metric_daily_summary` 和新版本明细。

## 验证方式

- 后端编译：`mvn -DskipTests compile`。
- 指标单元测试：`mvn -Dtest=RagRetrievalMetricServicesTest test`。
- 前端构建：`npm run build`。
- 数据库验证：检查日汇总表、`retrieval_log` 扫描字段、`rag_retrieval_metric_evaluation.daily_summary_id` 和相关索引。
