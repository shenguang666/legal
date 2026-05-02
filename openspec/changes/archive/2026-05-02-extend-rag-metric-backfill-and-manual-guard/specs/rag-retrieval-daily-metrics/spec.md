## ADDED Requirements

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

## MODIFIED Requirements

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
