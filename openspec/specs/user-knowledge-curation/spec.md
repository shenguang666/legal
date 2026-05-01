## ADDED Requirements

### Requirement: User knowledge SHALL be filtered before classification
The system SHALL apply lightweight rule-based pre-filtering before sending dialogue content to LLM-based user-knowledge classification.

#### Scenario: Low-value content filtered out
- **WHEN** a dialogue block is too short, greeting-only, temporary-instruction-only, or otherwise clearly low value
- **THEN** the system SHALL discard it before LLM classification and SHALL NOT write it into the user knowledge candidate store

### Requirement: User knowledge SHALL be classified into must, optional, or forbidden
The system SHALL classify each candidate dialogue block into `must`, `optional`, or `forbidden`, and SHALL store the classification result with reason and distilled core content.

#### Scenario: Must knowledge immediate write
- **WHEN** a candidate is classified as `must`
- **THEN** the system SHALL persist it with its core content and SHALL index it into the user knowledge Elasticsearch index immediately

#### Scenario: Forbidden knowledge rejected
- **WHEN** a candidate is classified as `forbidden`
- **THEN** the system SHALL NOT store it as active user knowledge and SHALL NOT index it into Elasticsearch

#### Scenario: Optional knowledge delayed review
- **WHEN** a candidate is classified as `optional`
- **THEN** the system SHALL store it as pending knowledge and SHALL defer Elasticsearch indexing until later summary-based review

### Requirement: Optional user knowledge SHALL be re-reviewed with summary context
The system SHALL review pending optional user knowledge together with refreshed session summary before deciding whether to activate or reject it.

#### Scenario: Optional becomes active after summary review
- **WHEN** a pending optional item is re-reviewed at summary refresh time and is judged to be stable or reusable high-value knowledge
- **THEN** the system SHALL promote it to active knowledge and SHALL index it into the user knowledge Elasticsearch index

#### Scenario: Optional rejected after summary review
- **WHEN** a pending optional item is re-reviewed at summary refresh time and is judged to be low value or session-fragment-only
- **THEN** the system SHALL mark it as rejected and SHALL keep it out of Elasticsearch

### Requirement: User knowledge SHALL be deduplicated by distilled core content
The system SHALL perform duplicate checks on distilled core content before writing active user knowledge.

#### Scenario: Duplicate core content not reinserted
- **WHEN** a new candidate's core content is substantially the same as existing user knowledge
- **THEN** the system SHALL avoid creating a duplicate active record and SHALL avoid duplicate Elasticsearch indexing
