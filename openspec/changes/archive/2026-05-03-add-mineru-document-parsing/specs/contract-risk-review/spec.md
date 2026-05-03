## ADDED Requirements

### Requirement: Use selected parsing output for Tianyan contract review
The system SHALL use the chunks produced by the selected document parsing method as the evidence source for Tianyan contract review.

#### Scenario: Tianyan document is imported with MinerU precise parsing
- **WHEN** an authorized user imports a Tianyan review document using MinerU precise parsing
- **THEN** the system waits until MinerU parsing and semantic chunking complete before treating the document as review-ready
- **THEN** contract field extraction and risk rule matching use the MinerU-derived chunks as review evidence

#### Scenario: Tianyan document is imported with native parsing
- **WHEN** an authorized user imports a Tianyan review document using native parsing
- **THEN** the system uses the native parsed chunks as review evidence
- **THEN** existing contract review behavior remains available

### Requirement: Prevent review from starting before parsing completes
The system SHALL prevent Tianyan review execution against documents whose parsing has not completed successfully.

#### Scenario: User opens a document still being parsed
- **WHEN** a user attempts to start Tianyan review for a document whose parsing status is pending or processing
- **THEN** the system rejects or delays the review start
- **THEN** the user can see that document parsing is still in progress

#### Scenario: User opens a document whose parsing failed
- **WHEN** a user attempts to start Tianyan review for a document whose parsing status is failed
- **THEN** the system does not run field extraction or risk rule matching
- **THEN** the user can see the parsing failure reason and decide whether to retry parsing or re-upload
