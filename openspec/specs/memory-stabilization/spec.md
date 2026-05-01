## ADDED Requirements

### Requirement: Long-term memory SHALL be extracted from summary instead of single-turn dialogue
The system SHALL perform long-term memory extraction only after a session summary refresh, and SHALL use summary text as the primary input for identity, profession, emotion, and other stable-user-state recognition.

#### Scenario: Summary-driven extraction
- **WHEN** a session reaches the configured summary refresh round
- **THEN** the system SHALL refresh the session summary first and only then run long-term memory extraction from the summary text

#### Scenario: No per-turn direct extraction
- **WHEN** a single round of dialogue finishes but no summary refresh is triggered
- **THEN** the system SHALL NOT directly promote any identity, profession, or emotion memory from that single round into stable long-term memory

### Requirement: Long-term memory SHALL use candidate state before stable promotion
The system SHALL place newly extracted long-term memory into a candidate state first, and SHALL promote it to stable memory only after repeated confirmation across summary refresh cycles.

#### Scenario: First extraction becomes candidate
- **WHEN** the system extracts a new long-term memory item from a summary for the first time
- **THEN** the item SHALL be stored as candidate memory rather than stable memory

#### Scenario: Repeated extraction upgrades candidate
- **WHEN** the same canonical memory key and value are extracted again in later summary refresh cycles
- **THEN** the system SHALL increase its confirmation count and promote it to stable memory once the configured threshold is reached

### Requirement: Long-term memory keys SHALL be canonicalized and merged
The system SHALL normalize semantically equivalent keys to one canonical memory key and SHALL avoid storing duplicate keys with different aliases.

#### Scenario: Alias normalization
- **WHEN** extracted keys such as `job`, `职业`, and `profession` appear
- **THEN** the system SHALL normalize them to the same canonical key `profession`

#### Scenario: Conflicting value handling
- **WHEN** an existing canonical memory key already has a different value
- **THEN** the system SHALL apply conflict rules and SHALL NOT overwrite the stored value unless the new evidence is sufficiently stronger
