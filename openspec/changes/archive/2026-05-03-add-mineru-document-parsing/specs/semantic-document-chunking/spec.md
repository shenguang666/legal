## ADDED Requirements

### Requirement: Chunk parsed documents using semantic boundaries
The system SHALL prefer semantic document boundaries over fixed character windows when creating chunks from structured or Markdown parsing results.

#### Scenario: Markdown contains headings and paragraphs
- **WHEN** parsed Markdown contains headings followed by paragraph content
- **THEN** the system creates chunks that preserve heading context with related paragraph content where possible
- **THEN** the system avoids splitting sentences unless a chunk exceeds the configured maximum size

#### Scenario: Document contains numbered clauses or list items
- **WHEN** parsed content contains numbered clauses or list items
- **THEN** the system keeps each clause or list item intact where possible
- **THEN** adjacent short clauses can be merged when doing so does not exceed the configured maximum size

### Requirement: Preserve table and block evidence integrity
The system SHALL avoid splitting tables and structured blocks in a way that loses their meaning for retrieval and review evidence.

#### Scenario: Parsed content contains a table block
- **WHEN** a parsed document includes a table represented in Markdown or structured content
- **THEN** the system keeps the table header and related rows together where possible
- **THEN** the generated chunk remains suitable as evidence for RAG, rule matching, and Tianyan review

### Requirement: Fall back safely for oversized or unstructured content
The system SHALL fall back to deterministic safe splitting when semantic boundaries are unavailable or a single block exceeds the maximum chunk size.

#### Scenario: A single paragraph exceeds maximum chunk size
- **WHEN** a semantic block exceeds the configured maximum chunk size
- **THEN** the system splits the block by sentence or line boundary when possible
- **THEN** the system only falls back to character-window splitting when no better boundary exists

#### Scenario: Native parser returns plain text only
- **WHEN** native parsing returns plain text without structural metadata
- **THEN** the system can use existing text normalization and chunking behavior
- **THEN** the system remains compatible with existing document import flows
