## MODIFIED Requirements

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
