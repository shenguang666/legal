## Requirements

### Requirement: Daily summary table for RAG retrieval metrics
The system SHALL maintain one RAG retrieval metric daily summary record per tenant, metric date, and evaluation version.

#### Scenario: Create daily summary for a metric date
- **WHEN** the scheduled metric job starts processing a tenant and metric date that has no summary record for the current evaluation version
- **THEN** the system creates a daily summary record with tenant ID, metric date, evaluation version, task status, message counters, average recall, average precision, and timestamps

#### Scenario: Reuse existing daily summary
- **WHEN** the scheduled metric job continues processing a tenant and metric date that already has a daily summary record for the current evaluation version
- **THEN** the system reuses that summary record instead of creating a duplicate

### Requirement: Evaluation detail records belong to a daily summary
系统 SHALL 将每条消息级 RAG 指标评估记录作为日汇总的子记录保存；当检索日志同时存在最终上下文命中 ID 和 Elasticsearch 原始命中 ID 时，评估明细 MUST 能保留并展示这两类 ID 的语义差异。

#### Scenario: Persist child evaluation
- **WHEN** a retrieval log is evaluated for a metric date
- **THEN** the evaluation detail record references the daily summary ID and stores the query, original hit chunk IDs, judged relevant IDs, missed relevant IDs, TP, FN, FP, recall, precision, model metadata, status, explanation, and error message

#### Scenario: Query details by summary
- **WHEN** an admin opens the detail list for a daily summary
- **THEN** the system returns only child evaluation records that belong to that daily summary, with pagination

#### Scenario: Evaluate retrieval log with raw hit IDs
- **WHEN** a retrieval log contains both `hit_chunk_ids` and `raw_hit_chunk_ids`
- **THEN** 系统 MUST 将 `raw_hit_chunk_ids` 作为 Elasticsearch 原始命中切片 ID 参与检索召回审计
- **THEN** 系统 MUST 保留 `hit_chunk_ids` 作为实际进入大模型上下文的切片 ID 供回答复盘

#### Scenario: Evaluate legacy retrieval log without raw hit IDs
- **WHEN** a retrieval log does not contain `raw_hit_chunk_ids` or the field is empty
- **THEN** 系统 MUST 兼容使用 `hit_chunk_ids` 作为原始命中 ID
- **THEN** 系统 MUST 不因历史日志缺少原始命中字段而导致整批指标评估失败

### Requirement: Retrieval logs have metric scan state
系统 SHALL 在每条 `retrieval_log` 行上跟踪 RAG 指标扫描进度，并在日志字段中区分最终上下文命中和 Elasticsearch 原始命中，确保父子分块替换不会丢失检索审计信息。

#### Scenario: Mark retrieval log as completed
- **WHEN** the metric job finishes evaluating or filtering a retrieval log
- **THEN** the system updates that retrieval log with a terminal metric scan status, scan time, and related daily summary identifier

#### Scenario: Retry failed retrieval log
- **WHEN** a retrieval log has a failed metric scan status and the job retries the same metric date
- **THEN** the system may retry that log according to the configured retry policy and update its status based on the new result

#### Scenario: Store final and raw hit IDs for new RAG logs
- **WHEN** a new online RAG request writes `retrieval_log`
- **THEN** `hit_chunk_ids` MUST store final context chunk IDs after parent expansion
- **THEN** `raw_hit_chunk_ids` MUST store Elasticsearch raw hit chunk IDs before parent expansion

### Requirement: Metric statuses use enumerated semantics
The system SHALL represent metric scan, daily summary, and evaluation statuses using Java enum classes and documented database field values.

#### Scenario: Store enum status value
- **WHEN** the system persists a metric status
- **THEN** it stores a value defined by the corresponding Java enum and documented in the database field comment

#### Scenario: Reject unknown status in service logic
- **WHEN** service code maps a persisted status value that is not defined by the corresponding enum
- **THEN** the system treats it as invalid and surfaces a safe failure instead of silently processing it as success

### Requirement: Scheduled job covers all eligible RAG messages
The system SHALL process all eligible RAG retrieval logs for the metric date and SHALL NOT enforce a daily maximum sample count.

#### Scenario: Process all eligible messages over multiple batches
- **WHEN** a metric date contains more eligible retrieval logs than one job batch can process
- **THEN** repeated low-peak scheduled runs continue processing remaining logs until all eligible logs reach a terminal scan status

#### Scenario: Exclude filtered messages from evaluation but count them
- **WHEN** a retrieval log query matches the configured low-quality query filter
- **THEN** the system marks it as filtered or skipped, excludes it from LLM evaluation, and includes it in daily filtered/skipped counters

### Requirement: Low-peak scheduled execution
The system SHALL run the automatic RAG metric job every five minutes during the configured low-peak window by default, and each run SHALL prioritize the configured metric date before backfilling older unprocessed RAG logs.

#### Scenario: Default low-peak schedule
- **WHEN** default configuration is used
- **THEN** the metric worker runs with a cron expression equivalent to every five minutes from 02:00 through 05:59 server time, unless disabled

#### Scenario: Scheduled run prioritizes configured metric date
- **WHEN** the metric worker starts a scheduled run
- **THEN** the system SHALL first process the configured metric date, which defaults to the previous natural day
- **THEN** after the configured metric date has no remaining eligible logs in the current batch decision, the system SHALL check for older unprocessed RAG logs and process one eligible historical date if present

#### Scenario: Manual metric date run
- **WHEN** an admin manually triggers a metric run for a specific date or historical date range before today
- **THEN** the system processes that requested metric date or range regardless of the default scheduled date offset, unless the requested date is already fully processed

### Requirement: Scheduled job SHALL backfill older unprocessed RAG logs
系统在完成默认归档日期处理后，SHALL 继续检查更早业务日期是否存在未检测或可重试的 RAG 检索日志，并按批次补扫这些历史消息。

#### Scenario: Backfill older unprocessed date after default date
- **WHEN** 定时任务完成默认指标日期处理，且更早日期存在 `retrieval_log` 中 RAG 指标扫描状态为空、待处理或失败但未超过重试次数的消息
- **THEN** 系统 SHALL 选择一个更早业务日期继续处理该日期的待处理消息
- **THEN** 系统 SHALL 将处理结果归入消息 `created_at` 所属业务日期的日汇总记录

#### Scenario: No older unprocessed logs exist
- **WHEN** 定时任务完成默认指标日期处理，且更早日期不存在未检测或可重试的 RAG 消息
- **THEN** 系统 SHALL 结束本轮任务且不创建空的历史日汇总记录

### Requirement: Backfill processing SHALL refresh affected daily summaries
系统处理历史未检测或失败重试消息后，SHALL 重新计算对应业务日期日汇总的计数、平均召回率和平均精确率。

#### Scenario: Recalculate summary after backfill success
- **WHEN** 历史补扫中某条消息评估成功、失败或被跳过
- **THEN** 系统 SHALL 刷新该消息所属日汇总的总消息数、有效消息数、过滤/跳过数、成功数、失败数、平均召回率和平均精确率

#### Scenario: Complete summary after no remaining logs
- **WHEN** 某个业务日期已不存在扫描状态为空、待处理、处理中或可重试失败的 RAG 消息
- **THEN** 系统 SHALL 将该日期对应日汇总标记为完成或部分失败，并记录完成时间

### Requirement: Manual metric evaluation SHALL only allow past dates
手动 RAG 指标评估 SHALL 只允许评估今天之前的历史日期，不允许选择当天或未来日期作为结束日期。

#### Scenario: Reject manual evaluation ending today
- **WHEN** 管理员手动发起 RAG 指标评估，且结束日期大于或等于服务器当前自然日
- **THEN** 系统 SHALL 拒绝请求并返回明确错误，提示只能评估今天之前的历史日期

#### Scenario: Accept manual evaluation before today
- **WHEN** 管理员手动发起 RAG 指标评估，且结束日期早于服务器当前自然日
- **THEN** 系统 SHALL 继续执行已处理检查和后续评估流程

### Requirement: Manual metric evaluation SHALL skip already completed dates
手动 RAG 指标评估 SHALL 在执行前检查目标日期是否已经完整处理；如果已有完成记录且无待处理或可重试消息，SHALL 返回无需重复评估的提示。

#### Scenario: Manual evaluation finds completed date
- **WHEN** 管理员手动评估某个历史日期，且该日期已有当前评估版本的完成日汇总记录，并且不存在扫描状态为空、待处理或可重试失败的 RAG 消息
- **THEN** 系统 SHALL 不重复调用 LLM 评估
- **THEN** 系统 SHALL 返回提示，说明该日期已有指标记录、无需重复评估

#### Scenario: Manual evaluation finds remaining logs
- **WHEN** 管理员手动评估某个历史日期，且该日期仍存在未检测或可重试的 RAG 消息
- **THEN** 系统 SHALL 处理这些消息并刷新该日期日汇总

### Requirement: Daily aggregate API uses summary records
The system SHALL calculate dashboard aggregate metrics from daily summary records, not by scanning all child evaluation rows on every request.

#### Scenario: Date range summary query
- **WHEN** an admin requests RAG metrics for a date range
- **THEN** the API returns total RAG message count, valid message count, filtered count, success count, failed count, skipped count, average recall, average precision, and daily trend points from matching daily summary records

#### Scenario: Empty date range
- **WHEN** no daily summary records exist for the requested date range
- **THEN** the API returns zero-valued counters and an empty trend list without error

### Requirement: ECharts trend visualization
The RAG metrics dashboard SHALL visualize daily recall, precision, RAG message count, and valid message count using ECharts trend charts.

#### Scenario: Render trend chart
- **WHEN** the frontend receives daily trend points from the summary API
- **THEN** it renders recall and precision trends and message volume trends using ECharts

#### Scenario: Select daily summary for details
- **WHEN** an admin selects a date or daily summary row on the dashboard
- **THEN** the frontend loads the corresponding child evaluation details and displays message-level results
