## ADDED Requirements

### Requirement: 智能小法庭案件生命周期
系统 SHALL 提供智能小法庭案件的创建、查询、更新、归档与软删除能力，且案件 MUST 严格按 `tenant_id + owner_user_id` 隔离，案件类型在 MVP 阶段仅支持合同纠纷。

#### Scenario: 创建合同纠纷案件
- **WHEN** 已登录用户提交案件创建请求，包含案件标题、用户立场（原告或被告）、合同与证据文档 ID 列表
- **THEN** 系统 MUST 校验所有引用文档属于当前租户和当前用户
- **THEN** 系统 MUST 在 `court_case` 中创建状态为 `DRAFT` 的案件记录，并在 `court_case_party` 中创建原告与被告两个当事人
- **THEN** 系统 MUST 把引用文档登记到 `court_case_evidence` 表，并标记其角色（合同 / 证据 / 规则）

#### Scenario: 跨租户访问被拒绝
- **WHEN** 用户尝试访问、修改或删除不属于其租户的案件
- **THEN** 系统 MUST 返回 404，而不是 403，避免案件 ID 枚举

#### Scenario: 案件软删除
- **WHEN** 用户删除案件
- **THEN** 系统 MUST 把 `court_case.status` 置为 `DELETED`，保留底层证据与图谱事件
- **THEN** 系统 MUST 触发对应 Neo4j 子图的清理任务（异步幂等）

### Requirement: 案件要素抽取与用户确认
系统 SHALL 在开庭前对案件要素（当事人、合同条款、金额、履行时间、违约行为、争议焦点）进行 AI 抽取，并 MUST 在用户确认后才允许进入庭审阶段。

#### Scenario: AI 抽取要素并区分主张与已证事实
- **WHEN** 用户提交案件并触发要素抽取
- **THEN** 系统 MUST 调用 LLM 输出结构化案件要素 JSON，每条事实带 `status` 字段，初始值为 `CLAIMED`
- **THEN** 系统 MUST 在前端提供“你的主张”与“可证事实”分组展示，要求用户逐项确认或修改

#### Scenario: 未确认要素不得开庭
- **WHEN** 用户在未确认必填要素的情况下尝试开庭
- **THEN** 系统 MUST 拒绝请求，并提示哪些要素未确认

#### Scenario: 用户修改已确认事实
- **WHEN** 用户在庭审进行中修改已确认事实
- **THEN** 系统 MUST 把后续庭审轮标记为 `STALE`
- **THEN** 系统 MUST 提示用户必须开启新一轮或重新开庭，禁止静默改图

### Requirement: 庭审阶段状态机
系统 SHALL 以阶段状态机推进模拟庭审，阶段 MUST 顺序覆盖：原告陈述、被告答辩、举证、质证、法庭辩论、最后陈述、模拟裁判意见。

#### Scenario: 单轮乐观锁切换
- **WHEN** 编排器尝试把某一轮从 `PENDING` 推进到 `RUNNING`
- **THEN** 系统 MUST 使用 `WHERE state='PENDING' AND lock_version=?` 的乐观锁更新
- **THEN** 系统 MUST 拒绝其他并发请求，返回“庭审进行中”错误

#### Scenario: 失败重试幂等
- **WHEN** 同一轮因服务异常被重试
- **THEN** 系统 MUST 为重试分配新的 `attempt_id`
- **THEN** 系统 MUST 按 `(round_id, attempt_id)` 幂等写入 LLM 输出与 `court_graph_event`

#### Scenario: 不允许回滚已结束轮
- **WHEN** 用户请求回滚或重做已完成的庭审轮
- **THEN** 系统 MUST 拒绝该操作
- **THEN** 系统 MUST 仅允许追加“补充庭审轮”，并在审计日志中保留原轮记录

### Requirement: 多角色 Agent 隔离
系统 SHALL 为 AI 法官、AI 对方代理人、（可选）AI 用户辅助律师配置独立 prompt 与上下文，MUST NOT 在单次 LLM 调用中混合多角色立场。

#### Scenario: 法官输入去用户立场化
- **WHEN** 编排器调用 AI 法官
- **THEN** 系统 MUST 把当事人统一替换为 `PartyA / PartyB`
- **THEN** 系统 MUST NOT 在法官 prompt 中出现“user”“原告是用户”“被告是用户”等措辞

#### Scenario: 对方代理人不得编造证据
- **WHEN** AI 对方代理人发言
- **THEN** 系统 MUST 在 prompt 中显式约束：只能引用本案已登记证据，不得编造合同条款或事实
- **THEN** 系统 MUST 对其输出执行证据引用三层校验

### Requirement: 法官输出双向不利点强制
系统 SHALL 强制 AI 法官输出结构化 JSON，包含 `focusIssues / acceptedFacts / rejectedFacts / unfavorableToPartyA / unfavorableToPartyB / openQuestions` 字段，且 `unfavorableToPartyA` 与 `unfavorableToPartyB` MUST 各包含至少一条要点。

#### Scenario: 双向要点缺失时整体重生成
- **WHEN** AI 法官输出缺少 `unfavorableToPartyA` 或 `unfavorableToPartyB`
- **THEN** 系统 MUST 触发整体重生成，最多 2 次

#### Scenario: 重生成仍失败的回退
- **WHEN** 重生成达到上限仍无法满足双向要点
- **THEN** 系统 MUST 把本轮法官输出回退为“证据不足，无法形成倾向性意见”
- **THEN** 系统 MUST 在审计日志中记录失败原因与重试次数

### Requirement: 证据引用三层校验
系统 SHALL 对所有 AI 角色发言中的 `evidenceIds` 与 `chunkIds` 引用执行 schema、白名单、归属三层校验后才能写入 `court_argument` 与 Neo4j 图谱。

#### Scenario: 白名单注入与命中校验
- **WHEN** 编排器为某轮发言准备 prompt
- **THEN** 系统 MUST 通过 `CourtEvidenceService.allowedRefsForRound` 生成允许引用的 ID 集合
- **THEN** 系统 MUST 把允许引用的 ID 集合显式注入 prompt
- **THEN** 系统 MUST 对解析出的引用执行 `Set.contains` 过滤

#### Scenario: 跨案件或跨租户引用被拒绝
- **WHEN** AI 输出引用了不属于本案件或不属于本租户的证据 ID
- **THEN** 系统 MUST 丢弃该引用
- **THEN** 系统 MUST 把发言 `stance` 降级为 `PENDING_PROOF`

#### Scenario: schema 校验失败重试
- **WHEN** AI 输出 JSON 不符合 schema
- **THEN** 系统 MUST 整轮重试，重试次数受 `legal.smart-court.evidence-ref-retry` 控制
- **THEN** 系统 MUST 在重试用尽时把本轮发言降级为“待证明”而不是丢弃

### Requirement: 单案件预算护栏
系统 SHALL 通过 `legal.smart-court.*` 配置控制单案件最大轮数、单轮最大 token、单案件累计 token 上限，MUST 在超限时停止后续 LLM 调用并自动收尾。

#### Scenario: 单轮 token 超限
- **WHEN** 单轮累计 token 超过 `legal.smart-court.max-tokens-per-round`
- **THEN** 系统 MUST 终止本轮剩余 LLM 调用
- **THEN** 系统 MUST 把本轮状态置为 `FAILED`，并在审计日志中记录原因

#### Scenario: 单案件累计 token 超限
- **WHEN** 单案件累计 token 超过 `legal.smart-court.max-tokens-per-case`
- **THEN** 系统 MUST 阻止开启新庭审轮
- **THEN** 系统 MUST 在前端提示用户已达预算上限

#### Scenario: 单案件最大轮数到达
- **WHEN** 已完成轮数达到 `legal.smart-court.max-rounds`
- **THEN** 系统 MUST 阻止开启新庭审轮
- **THEN** 系统 MUST 引导用户进入“生成模拟裁判报告”阶段

### Requirement: 庭审上下文使用父级语义块
系统 SHALL 在调用任意智能小法庭 Agent 前，使用 RAG 父级上下文扩展能力获取证据上下文，MUST NOT 把子分块内容直接作为 LLM 上下文。

#### Scenario: 庭审 Agent 检索强制使用父块
- **WHEN** 任意 Court Agent 调用 RAG 检索接口，且 `bizType=SMART_COURT`
- **THEN** 系统 MUST 启用父级上下文扩展
- **THEN** 系统 MUST 在 `court_argument.evidence_refs` 中同时记录 `parent_chunk_id` 与命中的 `child_chunk_id`

### Requirement: 模拟裁判报告与合规水印
系统 SHALL 在庭审收尾时生成结构化模拟裁判报告（争议焦点、事实认定、证据采信、模拟裁判观点、风险等级、补证建议摘要），并 MUST 在所有展示与导出形态固定附带“仅供模拟参考、不构成法律意见”水印。

#### Scenario: 报告必须包含双方要点
- **WHEN** 系统生成模拟裁判报告
- **THEN** 报告 MUST 同时包含支持原告与支持被告的至少一条要点
- **THEN** 系统 MUST 在前端与 PDF 导出中固定渲染合规水印

#### Scenario: 报告导出审计
- **WHEN** 用户导出模拟裁判报告
- **THEN** 系统 MUST 在审计日志中记录 `caseId / userId / tenantId / exportFormat / timestamp`

### Requirement: 庭审输出流式与可中断
系统 SHALL 通过 SSE 向前端流式输出 AI 法官与 AI 对方的发言，MUST 支持用户主动停止当前轮，并 MUST 在网络断开时按现有 SSE 规范优雅处理。

#### Scenario: 用户主动停止当前轮
- **WHEN** 用户在某轮进行中点击“停止”
- **THEN** 系统 MUST 把该轮状态从 `RUNNING` 切换到 `CANCELLED`
- **THEN** 系统 MUST 拒绝写入该轮的 LLM 输出与 `court_graph_event`

#### Scenario: SSE 客户端断开
- **WHEN** SSE 连接被客户端断开
- **THEN** 系统 MUST 按现有 SSE 规范在 debug 级别记录日志，并把该轮置为 `FAILED` 或允许稍后重试
- **THEN** 系统 MUST NOT 触发 Spring MVC error dispatch 写入 JSON 错误响应
