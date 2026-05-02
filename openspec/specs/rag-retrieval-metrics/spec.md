## ADDED Requirements

### Requirement: Retrieval metric evaluation job
The system SHALL evaluate RAG retrieval quality from persisted retrieval logs using a scheduled asynchronous job.

#### Scenario: Scheduled job evaluates eligible logs
- **WHEN** the retrieval metric job runs for a configured date range
- **THEN** the system SHALL load eligible `retrieval_log` records and evaluate them without blocking online RAG requests

#### Scenario: Disabled job does not evaluate logs
- **WHEN** the retrieval metric job is disabled by YAML configuration
- **THEN** the system SHALL skip scheduled evaluation and leave online RAG requests unaffected

### Requirement: Configurable TopN candidate recall
The system SHALL recall a configurable number of candidate documents for each evaluated query, with a default value of 200.

#### Scenario: Default candidate size is used
- **WHEN** no custom candidate size is configured
- **THEN** the system SHALL recall Top200 candidate documents for each `query_text`

#### Scenario: Configured candidate size is used
- **WHEN** YAML config sets a custom candidate TopN value
- **THEN** the system SHALL use that value when recalling candidate documents for metric evaluation

### Requirement: LLM-based recall estimation
The system SHALL estimate Recall by asking a large language model to identify relevant candidate documents that were not included in the original hit document set.

#### Scenario: Candidate contains missed relevant document
- **WHEN** the LLM judges a TopN candidate document relevant to the query and the document is not in the original hit document set
- **THEN** the system SHALL count it as FN for Recall estimation

#### Scenario: Original hit document is relevant
- **WHEN** the LLM judges an original hit document relevant to the query
- **THEN** the system SHALL count it as TP for Recall and Precision calculation

### Requirement: LLM-based precision estimation
The system SHALL estimate Precision by asking a large language model whether each original hit document is relevant to the query.

#### Scenario: Original hit document is irrelevant
- **WHEN** the LLM judges an original hit document irrelevant to the query
- **THEN** the system SHALL count it as FP for Precision calculation

#### Scenario: Precision is calculated
- **WHEN** TP and FP have been counted for an evaluated retrieval log
- **THEN** the system SHALL calculate Precision as `TP / (TP + FP)`

### Requirement: Recall calculation
The system SHALL calculate Recall using the standard formula `TP / (TP + FN)`.

#### Scenario: Recall is calculated with missed documents
- **WHEN** TP and FN have been counted for an evaluated retrieval log
- **THEN** the system SHALL calculate Recall as `TP / (TP + FN)`

#### Scenario: Recall denominator is zero
- **WHEN** TP plus FN equals zero
- **THEN** the system SHALL store Recall as zero and preserve the evaluation explanation

### Requirement: Evaluation result persistence
The system SHALL persist retrieval metric evaluation results with status, counts, scores, model metadata, and explanations.

#### Scenario: Evaluation succeeds
- **WHEN** a retrieval log is evaluated successfully
- **THEN** the system SHALL persist TP, FN, FP, Recall, Precision, evaluated candidate count, LLM explanation, and SUCCESS status

#### Scenario: Evaluation fails
- **WHEN** candidate recall or LLM judgment fails
- **THEN** the system SHALL persist FAILED status and an error message without stopping the whole batch

### Requirement: Date-filtered metric statistics API
The system SHALL expose APIs for querying aggregated and detailed retrieval metrics by date range.

#### Scenario: User queries date range statistics
- **WHEN** a user selects a start date and end date
- **THEN** the system SHALL return average Recall, average Precision, evaluated sample count, failed count, and trend data for that range

#### Scenario: User queries evaluation details
- **WHEN** a user opens metric details for a date range
- **THEN** the system SHALL return per-query metric results, original hit documents, missed relevant documents, and LLM explanations

### Requirement: Retrieval metric dashboard
The system SHALL provide a statistics page for RAG Recall and Precision.

#### Scenario: Dashboard displays metrics
- **WHEN** a user opens the RAG retrieval metric page
- **THEN** the system SHALL display date filters, average Recall, average Precision, sample counts, failure counts, trend charts, and evaluation detail rows

#### Scenario: Dashboard filters by date
- **WHEN** a user changes the selected date range
- **THEN** the system SHALL refresh metric cards, trend charts, and detail rows for the selected range
