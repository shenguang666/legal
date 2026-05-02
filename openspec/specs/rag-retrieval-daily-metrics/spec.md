## ADDED Requirements

### Requirement: Daily summary table for RAG retrieval metrics
The system SHALL maintain one RAG retrieval metric daily summary record per tenant, metric date, and evaluation version.

#### Scenario: Create daily summary for a metric date
- **WHEN** the scheduled metric job starts processing a tenant and metric date that has no summary record for the current evaluation version
- **THEN** the system creates a daily summary record with tenant ID, metric date, evaluation version, task status, message counters, average recall, average precision, and timestamps

#### Scenario: Reuse existing daily summary
- **WHEN** the scheduled metric job continues processing a tenant and metric date that already has a daily summary record for the current evaluation version
- **THEN** the system reuses that summary record instead of creating a duplicate

### Requirement: Evaluation detail records belong to a daily summary
The system SHALL store each message-level RAG metric evaluation as a child record linked to its daily summary.

#### Scenario: Persist child evaluation
- **WHEN** a retrieval log is evaluated for a metric date
- **THEN** the evaluation detail record references the daily summary ID and stores the query, original hit chunk IDs, judged relevant IDs, missed relevant IDs, TP, FN, FP, recall, precision, model metadata, status, explanation, and error message

#### Scenario: Query details by summary
- **WHEN** an admin opens the detail list for a daily summary
- **THEN** the system returns only child evaluation records that belong to that daily summary, with pagination

### Requirement: Retrieval logs have metric scan state
The system SHALL track RAG metric scan progress on each `retrieval_log` row.

#### Scenario: Mark retrieval log as completed
- **WHEN** the metric job finishes evaluating or filtering a retrieval log
- **THEN** the system updates that retrieval log with a terminal metric scan status, scan time, and related daily summary identifier

#### Scenario: Retry failed retrieval log
- **WHEN** a retrieval log has a failed metric scan status and the job retries the same metric date
- **THEN** the system may retry that log according to the configured retry policy and update its status based on the new result

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
The system SHALL run the automatic RAG metric job every five minutes during the configured low-peak window by default.

#### Scenario: Default low-peak schedule
- **WHEN** default configuration is used
- **THEN** the metric worker runs with a cron expression equivalent to every five minutes from 02:00 through 05:59 server time, unless disabled

#### Scenario: Manual metric date run
- **WHEN** an admin manually triggers a metric run for a specific date
- **THEN** the system processes that requested metric date regardless of the default scheduled date offset

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
