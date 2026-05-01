## ADDED Requirements

### Requirement: Extract configured contract fields
The system SHALL extract configured core contract fields from a reviewed contract document and persist each field result with field code, display name, raw value, normalized value, extraction status, confidence score, evidence text, source location, and extractor type.

#### Scenario: Extract core fields from a contract document
- **WHEN** a user triggers contract risk review for a document that contains recognizable contract clauses
- **THEN** the system stores field results for each configured core field it can identify
- **THEN** each stored field result includes evidence text and a confidence score

### Requirement: Preserve missing and uncertain extraction states
The system SHALL mark fields as `MISSING`, `UNCERTAIN`, or `EXTRACTED` instead of fabricating values when the contract text is incomplete, ambiguous, or unsupported.

#### Scenario: Required field cannot be identified
- **WHEN** a reviewed contract does not contain enough information to identify a required amount or date field
- **THEN** the system records the field result with a non-success status instead of generating an unsupported value
- **THEN** the field result includes an explanation or evidence reference describing why extraction was incomplete

### Requirement: Support multi-valued business fields
The system SHALL support multiple extracted values for repeatable business fields such as reimbursement items, clause references, or multiple payment milestones.

#### Scenario: Contract contains multiple reimbursement items
- **WHEN** a contract lists more than one reimbursement item or payment line
- **THEN** the system stores each identified item as a separate field result or child item under the same field code
- **THEN** the review detail response returns the complete list in document order
