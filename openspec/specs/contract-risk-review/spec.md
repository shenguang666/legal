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

### Requirement: 使用所选解析结果执行天眼合同审查
系统 SHALL 使用所选文档解析方式生成的切片作为天眼合同审查的证据来源。

#### Scenario: 天眼文档使用 MinerU 精准解析导入
- **WHEN** 授权用户使用 MinerU 精准解析导入天眼审查文档
- **THEN** 系统 MUST 等待 MinerU 解析和语义切片完成后，才将文档视为可审查
- **THEN** 合同字段抽取和风险规则匹配 MUST 使用 MinerU 生成的切片作为审查证据

#### Scenario: 天眼文档使用原生解析导入
- **WHEN** 授权用户使用原生解析导入天眼审查文档
- **THEN** 系统 MUST 使用原生解析生成的切片作为审查证据
- **THEN** 现有合同审查行为 MUST 保持可用

### Requirement: 解析完成前禁止启动审查
系统 SHALL 阻止对解析未成功完成的文档执行天眼审查。

#### Scenario: 用户尝试审查仍在解析中的文档
- **WHEN** 用户尝试启动解析状态为待解析或解析中的天眼审查文档
- **THEN** 系统 MUST 拒绝或延迟启动审查
- **THEN** 用户 MUST 能看到文档仍在解析中的提示

#### Scenario: 用户尝试审查解析失败的文档
- **WHEN** 用户尝试启动解析状态为失败的天眼审查文档
- **THEN** 系统 MUST 不执行字段抽取或风险规则匹配
- **THEN** 用户 MUST 能看到解析失败原因，并决定是否重试解析或重新上传
