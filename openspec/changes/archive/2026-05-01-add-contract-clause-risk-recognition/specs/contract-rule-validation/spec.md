## ADDED Requirements

### Requirement: Validate extracted fields against enterprise rules
The system SHALL evaluate extracted contract fields against configured enterprise rules for required fields, format checks, threshold checks, consistency checks, and conflict checks.

#### Scenario: Missing required field triggers a rule hit
- **WHEN** a required contract field is marked as missing after extraction
- **THEN** the system creates a rule hit for the matching required-field rule
- **THEN** the rule hit includes severity, rule code, and affected field references

### Requirement: Detect inconsistent and abnormal values
The system SHALL identify inconsistent amounts, abnormal date relationships, and other conflicting field values defined by enterprise rules.

#### Scenario: Amount values conflict across clauses
- **WHEN** the contract contains multiple amount fields that are expected to match under an enabled consistency rule
- **THEN** the system marks the rule as hit when the normalized values do not match
- **THEN** the resulting risk record references the conflicting fields and the supporting evidence

### Requirement: Produce explainable validation results
The system SHALL persist each rule evaluation result with a human-readable explanation, related evidence, severity, and execution status so that users can understand why a risk was identified.

#### Scenario: User reviews a triggered validation result
- **WHEN** a validation rule is hit during contract review
- **THEN** the system stores an explanation describing the rule condition and the observed contract values
- **THEN** the explanation is available in the contract review detail response
