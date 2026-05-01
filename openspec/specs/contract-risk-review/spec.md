## ADDED Requirements

### Requirement: Manage the contract review lifecycle
The system SHALL create and track a contract review record for each triggered analysis request with statuses for pending, processing, completed, and failed execution.

#### Scenario: User starts a contract review
- **WHEN** a user submits a valid contract review request for an imported document version
- **THEN** the system creates a contract review record and marks it as pending or processing
- **THEN** the user can query the current review status by review ID or document ID

### Requirement: Aggregate review findings into a risk summary
The system SHALL aggregate extraction results and validation hits into a review summary that includes overall risk level, total risk count, key warnings, and field coverage information.

#### Scenario: Completed review returns a summary
- **WHEN** contract extraction and rule validation finish successfully
- **THEN** the system marks the review as completed
- **THEN** the review detail response includes the overall risk level, summary indicators, extracted fields, and risk items

### Requirement: Support re-run for new document versions or user retries
The system SHALL allow a contract review to be re-triggered when the underlying document version changes or when a user explicitly requests another analysis run.

#### Scenario: Document version changes after a previous review
- **WHEN** a new document version is available for a document that already has a completed review
- **THEN** the system creates a new review run tied to the latest document version
- **THEN** previous review results remain queryable for audit and comparison purposes

### Requirement: Surface failed reviews with actionable information
The system SHALL retain failure status and failure reason when contract review processing cannot complete.

#### Scenario: Review processing fails
- **WHEN** field extraction or rule validation throws a non-recoverable processing error
- **THEN** the system marks the review as failed
- **THEN** the review detail response includes a failure reason that helps the user decide whether to retry
