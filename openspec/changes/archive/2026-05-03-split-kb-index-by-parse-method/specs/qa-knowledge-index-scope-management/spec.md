## ADDED Requirements

### Requirement: 管理员可配置智能问答知识库检索索引范围
系统 SHALL 提供管理员接口和管理页入口，用于查看并保存智能问答知识库检索索引范围。范围 MUST 至少包含只查原生索引、只查 MinerU 索引、同时查询两个索引三种模式。

#### Scenario: 管理员查看当前检索范围
- **WHEN** 管理员打开知识库管理页
- **THEN** 系统 MUST 展示当前智能问答知识库检索索引范围

#### Scenario: 管理员保存检索范围
- **WHEN** 管理员选择新的检索范围并保存
- **THEN** 系统 MUST 持久化该配置，并让后续智能问答按新范围检索

### Requirement: 智能问答按配置选择知识库索引
系统 SHALL 在智能问答 RAG 检索时，根据管理员保存的索引范围查询对应知识库索引，并返回合并后的检索结果。

#### Scenario: 只查询原生索引
- **WHEN** 智能问答知识库检索范围为 `NATIVE_ONLY`
- **THEN** 系统 MUST 只查询原知识库索引

#### Scenario: 只查询 MinerU 索引
- **WHEN** 智能问答知识库检索范围为 `MINERU_ONLY`
- **THEN** 系统 MUST 只查询 MinerU 知识库索引

#### Scenario: 同时查询两个索引
- **WHEN** 智能问答知识库检索范围为 `BOTH`
- **THEN** 系统 MUST 查询原知识库索引和 MinerU 知识库索引，并合并排序后返回结果

### Requirement: 默认配置兼容现有行为
系统 SHALL 在数据库未保存智能问答知识库检索范围时，使用默认范围，默认范围 MUST 兼容现有只查询原知识库索引的行为。

#### Scenario: 配置未初始化
- **WHEN** 系统启动后数据库中没有智能问答知识库检索范围配置
- **THEN** 智能问答 MUST 默认只查询原知识库索引
