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

### Requirement: Reusable retrieval for offline metric evaluation
The system SHALL expose a reusable retrieval path that can recall TopN candidate documents for a query without invoking answer generation.

#### Scenario: Offline evaluator recalls candidates
- **WHEN** the retrieval metric evaluator submits a `query_text` and configured TopN value
- **THEN** the system SHALL return candidate documents from the RAG retrieval layer without generating a chat answer

#### Scenario: Candidate recall respects tenant scope
- **WHEN** the evaluator recalls candidates for a tenant-specific retrieval log
- **THEN** the system SHALL only return candidate documents visible to that tenant

### Requirement: Retrieval candidate identity consistency
The system SHALL return stable document or chunk identifiers in offline candidate recall results so that candidates can be compared with original hit documents from `retrieval_log`.

#### Scenario: Candidate is compared with original hits
- **WHEN** the evaluator receives TopN candidates and original hit documents
- **THEN** the system SHALL compare them using stable identifiers instead of document text only

### Requirement: 鏅鸿兘灏忔硶搴鐢ㄧ煡璇嗗簱涓庢枃妗ｈ祫浜т綋绯?绯荤粺 SHALL 澶嶇敤鐜版湁鐢ㄦ埛鐭ヨ瘑涓庢枃妗ｈ祫浜т綋绯讳负鏅鸿兘灏忔硶搴彁渚涘悎鍚屼笌璇佹嵁鏉愭枡锛孧UST NOT 淇敼鏃㈡湁 `KNOWLEDGE / RISK_RULE` 鏂囨。鐨?CRUD 琛屼负锛孧UST 閫氳繃鐜版湁绉佹湁璧勪骇璁块棶绔偣鍥炴函璇佹嵁鍘熸枃銆?
#### Scenario: 妗堜欢璇佹嵁鏉ユ簮浠呴檺鏈鎴锋枃妗?- **WHEN** 鐢ㄦ埛鍦ㄥ垱寤哄皬娉曞涵妗堜欢鏃堕€夋嫨鍚堝悓涓庤瘉鎹枃妗?- **THEN** 绯荤粺 MUST 浠呭厑璁搁€夋嫨褰撳墠绉熸埛鍜屽綋鍓嶇敤鎴峰彲瑙佺殑 `KNOWLEDGE` 鎴?`RISK_RULE` 鏂囨。
- **THEN** 绯荤粺 MUST 鍦?`court_case_evidence` 涓櫥璁拌寮曠敤锛屼絾涓嶄慨鏀瑰師鏂囨。琛?
#### Scenario: 璇佹嵁鍘熸枃鍥炴函澶嶇敤绉佹湁璧勪骇绔偣
- **WHEN** 鐢ㄦ埛鍦ㄥ涵瀹＄晫闈㈡垨璇佹嵁閾惧浘璋变腑鐐瑰嚮璇佹嵁鑺傜偣鏌ョ湅鍘熸枃
- **THEN** 绯荤粺 MUST 閫氳繃鐜版湁 `GET /api/document-assets/{documentId}/{assetName}` 绔偣鎻愪緵璁块棶
- **THEN** 绯荤粺 MUST 鏍￠獙璇ユ枃妗ｅ綊灞炲綋鍓嶇鎴蜂笌鐢ㄦ埛

#### Scenario: 鐭ヨ瘑搴?CRUD 琛屼负涓嶅彉
- **WHEN** 鐢ㄦ埛瀵瑰弬涓庢浠剁殑鏂囨。鎵ц淇敼銆侀噸鏂扮储寮曘€佸垹闄ょ瓑鎿嶄綔
- **THEN** 绯荤粺 MUST 淇濇寔鐜版湁 `KNOWLEDGE / RISK_RULE` 琛屼负涓嶅彉
- **THEN** 绯荤粺 MUST 鍦ㄦ枃妗ｈ杞垹闄ゆ椂鎶婄浉鍏宠瘉鎹妭鐐瑰湪 Neo4j 涓爣璁颁负 `INVALID` 鑰屼笉鏄洿鎺ョ骇鑱斿垹闄ゆ浠?
