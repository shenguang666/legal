## ADDED Requirements

### Requirement: 智能小法庭复用知识库与文档资产体系
系统 SHALL 复用现有用户知识与文档资产体系为智能小法庭提供合同与证据材料，MUST NOT 修改既有 `KNOWLEDGE / RISK_RULE` 文档的 CRUD 行为，MUST 通过现有私有资产访问端点回溯证据原文。

#### Scenario: 案件证据来源仅限本租户文档
- **WHEN** 用户在创建小法庭案件时选择合同与证据文档
- **THEN** 系统 MUST 仅允许选择当前租户和当前用户可见的 `KNOWLEDGE` 或 `RISK_RULE` 文档
- **THEN** 系统 MUST 在 `court_case_evidence` 中登记该引用，但不修改原文档表

#### Scenario: 证据原文回溯复用私有资产端点
- **WHEN** 用户在庭审界面或证据链图谱中点击证据节点查看原文
- **THEN** 系统 MUST 通过现有 `GET /api/document-assets/{documentId}/{assetName}` 端点提供访问
- **THEN** 系统 MUST 校验该文档归属当前租户与用户

#### Scenario: 知识库 CRUD 行为不变
- **WHEN** 用户对参与案件的文档执行修改、重新索引、删除等操作
- **THEN** 系统 MUST 保持现有 `KNOWLEDGE / RISK_RULE` 行为不变
- **THEN** 系统 MUST 在文档被软删除时把相关证据节点在 Neo4j 中标记为 `INVALID` 而不是直接级联删除案件
