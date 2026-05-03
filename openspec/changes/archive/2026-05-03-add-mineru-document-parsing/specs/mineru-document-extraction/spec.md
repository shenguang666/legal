## ADDED Requirements

### Requirement: Submit local files to MinerU precise extraction
The system SHALL support submitting uploaded local documents to MinerU v4 Precision Extract API from the backend.

#### Scenario: MinerU upload URL creation succeeds
- **WHEN** a document is selected for MinerU precise parsing
- **THEN** the backend requests a MinerU batch upload URL using configured authentication and parsing parameters
- **THEN** the backend uploads the file bytes to the signed upload URL returned by MinerU

#### Scenario: MinerU upload URL creation fails
- **WHEN** MinerU rejects the upload URL creation request or returns an error code
- **THEN** the system records the parsing failure
- **THEN** the system exposes a failure reason suitable for user troubleshooting

### Requirement: Poll MinerU batch results asynchronously
The system SHALL poll MinerU extraction results asynchronously until completion, failure, or configured timeout.

#### Scenario: MinerU extraction completes successfully
- **WHEN** MinerU reports a document extraction result with state done
- **THEN** the system downloads the result package or Markdown output
- **THEN** the system makes the parsed content available to the chunking pipeline

#### Scenario: MinerU extraction times out
- **WHEN** MinerU does not finish before the configured timeout
- **THEN** the system marks the parsing task as failed or retryable according to retry policy
- **THEN** the system records the timeout reason

### Requirement: Protect MinerU credentials
The system SHALL keep MinerU API credentials exclusively in backend configuration and server-side requests.

#### Scenario: Frontend requests parsing capabilities
- **WHEN** the frontend requests available document parsing methods or upload configuration
- **THEN** the system returns capability flags and limits
- **THEN** the system does not return the MinerU API token or signed upload URLs intended for backend use

### Requirement: Support retryable MinerU parsing failures
The system SHALL retain enough task metadata to retry MinerU parsing when failures are transient.

#### Scenario: User retries a failed MinerU parsing task
- **WHEN** a document parsing task failed due to a retryable MinerU or network error
- **THEN** an authorized user can trigger parsing again for the same document version
- **THEN** the system records the new attempt without losing the previous failure reason
