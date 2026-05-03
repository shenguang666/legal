## ADDED Requirements

### Requirement: Select parsing method during document upload
The system SHALL allow authorized users to choose a document parsing method when importing knowledge base documents, Tianyan review documents, and risk rule documents.

#### Scenario: User imports a document with native parsing
- **WHEN** an authorized user uploads a supported document and selects native parsing
- **THEN** the system processes the document through the existing local extraction path
- **THEN** the imported document remains available for indexing, review, or rule retrieval according to its business type

#### Scenario: User imports a document with MinerU precise parsing
- **WHEN** an authorized user uploads a supported document and selects MinerU precise parsing
- **THEN** the system records the selected parsing method for the document
- **THEN** the system starts MinerU precise parsing without exposing the MinerU API token to the frontend

### Requirement: Preserve existing upload behavior when no parsing method is supplied
The system SHALL preserve existing document import behavior for clients that do not submit an explicit parsing method.

#### Scenario: Legacy client uploads without parsing method
- **WHEN** a client uploads a valid document without a parsing method parameter
- **THEN** the system uses the configured default parsing method
- **THEN** the upload does not fail solely because the parsing method parameter is absent

### Requirement: Enforce configurable maximum upload document count
The system SHALL enforce the maximum number of documents allowed in one upload request using backend YAML configuration.

#### Scenario: Upload count is within configured limit
- **WHEN** a user uploads a number of documents less than or equal to the configured maximum
- **THEN** the system accepts the request for further file validation and parsing

#### Scenario: Upload count exceeds configured limit
- **WHEN** a user uploads more documents than the configured maximum
- **THEN** the system rejects the request before parsing starts
- **THEN** the response explains the configured maximum document count

### Requirement: Surface parsing availability and status to the user
The system SHALL provide enough information for the frontend to display available parsing methods, upload limits, parsing status, and failure reasons.

#### Scenario: MinerU is disabled or not configured
- **WHEN** MinerU precise parsing is disabled or the backend lacks a valid MinerU token
- **THEN** the system does not present MinerU precise parsing as an available method
- **THEN** native parsing remains available if the user has upload permission

#### Scenario: Parsing fails after document creation
- **WHEN** document parsing fails after a document record has been created
- **THEN** the system marks the document or parsing task as failed
- **THEN** the frontend can display a user-readable failure reason
