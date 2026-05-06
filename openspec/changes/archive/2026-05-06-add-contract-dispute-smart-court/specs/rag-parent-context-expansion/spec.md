## ADDED Requirements

### Requirement: 智能小法庭庭审上下文强制使用父级语义块
系统 SHALL 在 `bizType=SMART_COURT` 的 RAG 检索调用路径上强制启用父级上下文扩展，MUST NOT 把子分块内容直接作为 LLM 庭审上下文，MUST 在庭审证据引用记录中同时保留父分块 ID 与命中的子分块 ID 用于审计。

#### Scenario: 庭审 Agent 检索强制启用父级扩展
- **WHEN** 任意智能小法庭角色 Agent（法官、对方代理人、用户辅助律师）通过 RAG 检索接口召回证据，且 `bizType=SMART_COURT`
- **THEN** 系统 MUST 启用父级上下文扩展能力，把命中的子分块替换为对应父分块进入 LLM 上下文
- **THEN** 系统 MUST NOT 接受调用方关闭父级扩展的参数

#### Scenario: 庭审证据引用同时记录父子分块 ID
- **WHEN** 庭审某轮 AI 输出引用了被父级扩展替换后的分块
- **THEN** 系统 MUST 在 `court_argument.evidence_refs` 与 `court_case_evidence` 中同时保存 `parent_chunk_id` 与原始命中的 `child_chunk_id`
- **THEN** 系统 MUST 在审计日志中保留父子映射，便于后续证据回溯

#### Scenario: 父分块缺失时禁止回退到子分块
- **WHEN** 庭审检索的子分块 `parent_chunk_id` 在当前租户下找不到对应父分块
- **THEN** 系统 MUST 跳过该命中而不是回退到子分块作为庭审上下文
- **THEN** 系统 MUST 记录可排查日志，并在该轮发言降级为“证据不足”
