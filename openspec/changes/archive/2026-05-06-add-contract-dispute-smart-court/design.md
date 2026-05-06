## Context

当前 `legal` 项目已具备文档解析（Native + MinerU 精准解析）、文档清洗、父子语义分块、RAG 检索（ES + 父级上下文扩展）、风险规则识别、token 计量、租户隔离、文档资产私有访问等能力。合同审查页能给出条款风险，但用户进入实际纠纷前，无法对自己一方的事实主张和证据链做结构化攻防演练，也看不到“哪些诉求没有证据闭环”“哪些金额或时间存在矛盾”“对方可能从哪里反击”。

智能小法庭定位为面向合同纠纷的“可重复、可解释、可审计的庭审推演系统”，核心数据是“证据链知识图谱”。Neo4j 是图谱底座，MySQL 仍是业务权威源。本设计回答两个核心问题：

- 如何在不破坏现有单体架构与租户隔离的前提下，安全地引入 Neo4j 并保证 MySQL 与 Neo4j 双写一致性。
- 如何让多 Agent 庭审不出现“立场串戏 / 引用幻觉 / 用户主张当事实 / 法官隐性偏向用户”的合规事故。

主要约束：

- Spring Boot 单体，沿用现有租户隔离（`tenantId`）与权限（`AuthPrincipal`）模型。
- LLM 调用按现有 token 计量入账。
- 配套部署包基于 Docker，需新增 Neo4j 服务但不能阻塞主流程（Neo4j 故障时庭审仍可继续，仅图谱功能降级）。
- 新增 YAML、实体属性、数据库字段必须带中文注释（项目规则）。

## Goals / Non-Goals

**Goals:**

- 用户在低成本环境下完成一次合同纠纷的“庭审级”推演，得到争议焦点、模拟裁判观点、补证建议清单。
- 证据链知识图谱可视化，节点/关系含义清晰，支持按诉求/争议焦点/缺口过滤。
- AI 法官中立、AI 对方有理有据、AI 不得编造证据；所有 AI 观点可回溯到真实文档片段。
- MySQL 永远是权威源，Neo4j 可从 MySQL 事件流全量重建。
- 单租户隔离严格、单案件预算（轮次/token）可控、Cypher 查询可控（跳数/节点上限）。
- 与现有 MinerU 精准解析、父子分块、RAG、token 计量、文档资产私有访问无缝复用。

**Non-Goals:**

- 不做真实法律裁判、不做胜诉率预测、不出具正式法律意见书；所有输出强制带“仅供模拟参考”水印。
- 不做实时多人在线庭审、不做语音/视频庭审、不做对外开放的“公审”形态。
- 不在 MVP 接入外部法规库 / 类案库；法源依据仅限用户上传的合同/证据/规则文档（沿用现有 `RISK_RULE` 文档体系）。
- 不在 MVP 引入图数据库高级算法（GDS、社区发现、嵌入推理）；只做基础节点/关系建模与多跳查询。
- 不在 MVP 做观众/陪审团 Agent；预留扩展位但默认关闭。
- 不替换现有 MySQL/ES，不影响合同审查、风险规则、知识库、Tianyan 等已有页面行为。

## Decisions

### 决策 1：MySQL 为权威源 + Neo4j 异步图谱投影

**选择**：所有庭审写入先落 MySQL（含 `court_graph_event` 事件表），由独立 `CourtGraphProjector` 异步幂等投影到 Neo4j；Neo4j 永远可重建。

**为什么不**：

- ChainedTransactionManager 已弃用，无法跨 MySQL/Neo4j 形成强一致性；XA 引入运维复杂度且不被现有部署支持。
- Neo4j 直接作为业务主存：会破坏租户隔离体系、审计追踪、报表查询、与现有列表/分页页面的一致性。
- 同步双写：LLM 长耗时事务 + Neo4j 故障会拖垮庭审主流程。

**怎么做**：

- 庭审每轮在一个 MySQL 事务内写入：`court_hearing_round / court_hearing_message / court_argument / court_case_evidence` + 一条或多条 `court_graph_event(payload_json, status=PENDING)`。
- `CourtGraphProjector` 单线程批量消费 PENDING 事件，幂等写入 Neo4j（按 `eventId` 在节点/关系属性上 MERGE）。
- 失败重试 + 死信告警；提供管理接口按 `caseId` 全量重投，重建前先 `DETACH DELETE` 该案件所有节点。  
- 前端图谱接口在 PENDING 事件残留时显式返回 `"graphState":"PROJECTING"`，UI 显示“图谱更新中”，避免脏数据。

### 决策 2：证据引用三层校验

**选择**：所有 AI 发言中的 `evidenceIds`/`chunkIds` 必须经过 schema 校验、白名单校验、归属校验三层后才能落库。

**为什么**：LLM 极易编造 ID 或跨案件/跨租户引用，一旦写入图谱会污染补证建议链路。

**怎么做**：

- LLM 输出强制 JSON Schema：`{ content, stance, evidenceIds[], chunkIds[], rationale }`，结构错误整轮重试，超过重试上限本轮发言降级为“待证明”。
- 本轮发言前由 `CourtEvidenceService.allowedRefsForRound(caseId, tenantId, roundId)` 生成白名单 Set，并将白名单 ID 注入 prompt。
- 解析后逐一 `Set.contains` 过滤；同时对每个 ID 执行 `WHERE case_id=? AND tenant_id=?` 归属校验；不通过的引用被丢弃，并把发言 `stance` 降级为 `PENDING_PROOF`。
- 校验结果写入 `court_argument.evidence_verified_count / evidence_dropped_count` 字段，用于审计和回归。

### 决策 3：法官中立性的机制约束

**选择**：法官 Agent 的输入完全去“user_side”化，并在输出层强制双向不利点。

**怎么做**：

- 编排器调用法官前，把当事人统一替换为 `PartyA / PartyB`，到前端渲染再回填；法官 prompt 中不出现“user/原告是用户”等措辞。
- 法官输出强制结构：`focusIssues / acceptedFacts / rejectedFacts / unfavorableToPartyA / unfavorableToPartyB / openQuestions`；`unfavorableToPartyA` 与 `unfavorableToPartyB` 至少各一条，缺失则整体重生成（最多 2 次），仍缺失则回退为“证据不足，无法形成倾向性意见”。
- 模拟裁判报告同样要求双向要点，前端固定附带“仅供模拟参考、不构成法律意见”水印与 disclaimer。

### 决策 4：庭审上下文统一使用父级语义块

**选择**：庭审 Agent 检索接口固定按父级上下文返回；子块仅参与召回，不进入 prompt。

**怎么做**：

- 复用 `rag-parent-context-expansion` 能力，在 `bizType=SMART_COURT` 调用路径强制启用父级扩展。
- `court_case_evidence` 与 `court_argument.evidence_refs` 同时记录 `parent_chunk_id` 和命中的 `child_chunk_id`，便于审计回溯。
- 前置依赖：`refine-mineru-parent-child-chunking` 必须先合入；本变更 tasks 显式声明该依赖。

### 决策 5：Neo4j 节点/关系建模与租户隔离

**选择**：所有节点冗余 `tenantId / caseId`，唯一约束含 `tenantId`，Cypher 强制注入。

**节点类型**：`Case / Party / Claim / Defense / Fact / Evidence / Document / Chunk / Clause / Obligation / Breach / Amount / Date / Argument / Risk / Gap / LegalBasis / JudgmentPoint`。

**关系类型**：`HAS_PARTY / RAISES_CLAIM / RAISES_DEFENSE / ASSERTS_FACT / SUPPORTED_BY / CONTRADICTED_BY / DERIVED_FROM / QUOTES_CLAUSE / PROVES_AMOUNT / PROVES_DATE / CREATES_OBLIGATION / BREACHES_OBLIGATION / SUPPORTS_ARGUMENT / CHALLENGES_ARGUMENT / HAS_RISK / HAS_GAP / NEEDS_EVIDENCE / SUPPORTS_JUDGMENT`。

**约束**：

- 唯一性：`CONSTRAINT` 形如 `(n:Case) REQUIRE (n.tenantId, n.id) IS UNIQUE`，对每类节点都建立 `(tenantId, caseId, businessId)` 唯一约束。
- 索引：`INDEX ON :Evidence(tenantId, caseId)`、`:Fact(tenantId, caseId, status)`、`:Claim(tenantId, caseId)` 等。
- 所有 Cypher 经统一 `Neo4jSession`/`Repository` 切面，禁止裸 Cypher；`tenantId` 由 `AuthPrincipal` 注入参数。
- `Fact` 节点带 `status` 属性（`CLAIMED / EVIDENCED / DISPUTED / ACCEPTED`），`CLAIMED` 不参与模拟裁判推理。

### 决策 6：性能与预算护栏（YAML 可调）

**选择**：通过 `legal.smart-court.*` 配置统一控制单案件成本和查询代价。

**配置项**（均带中文注释）：

- `legal.smart-court.enabled`：是否启用智能小法庭功能。
- `legal.smart-court.max-rounds`：单案件最大庭审轮数。
- `legal.smart-court.max-tokens-per-round`：单轮 LLM 总 token 上限。
- `legal.smart-court.max-tokens-per-case`：单案件累计 token 上限，超限收尾。
- `legal.smart-court.evidence-ref-retry`：证据引用 schema 校验失败后的最大整轮重试次数。
- `legal.smart-court.graph.max-hops`：Cypher 路径最大跳数（默认 4）。
- `legal.smart-court.graph.max-nodes-per-case`：单案件图谱节点数上限。
- `legal.smart-court.graph.projector-batch-size`：投影器单批事件数。
- `legal.smart-court.graph.rebuild-page-size`：重建图谱时 MySQL 事件分页大小。
- `spring.neo4j.uri / authentication.username / authentication.password`：Neo4j 连接配置（生产从环境变量注入）。

### 决策 7：庭审状态机与并发

**选择**：每个庭审轮采用 `PENDING -> RUNNING -> SUCCEEDED / FAILED / CANCELLED` 状态机，乐观锁切换。

**怎么做**：

- `court_hearing_round` 含 `(state, attempt_id, lock_version)`；`PENDING -> RUNNING` 用 `UPDATE ... WHERE state='PENDING' AND lock_version=?` 切换，防止双跑。
- 每次 LLM 调用、Neo4j 事件按 `(round_id, attempt_id)` 幂等。
- 用户修改已确认事实时，自动把后续轮标记为 `STALE`，并要求“开启新一轮”或“重新开庭”，不静默改图。
- 不允许回滚已结束轮，仅支持追加“补充庭审轮”。

### 决策 8：前端三栏布局与图谱可视化

**选择**：左栏材料/证据，中栏庭审对话（SSE 流式，复用现有 `askStream` 思路），右栏证据链图谱（按争议焦点过滤，2 跳子图）。

**怎么做**：

- 优先复用 ECharts `graph` 系列实现节点/关系可视化，避免新增重型依赖；颜色按支持/反驳/冲突/缺失/条款/裁判分类。
- 默认只渲染主路径子图（围绕当前争议焦点 2 跳内），其他按需展开；后端在 `GET /api/smart-court/cases/{id}/graph` 接受 `focusClaimId / hops / limit` 参数。
- 模拟裁判报告 PDF 导出固定水印 “仅供模拟参考、不构成法律意见”。
- `web/src/router/index.js` 必须与 `index.ts` 同步（项目既有约束）。

## Risks / Trade-offs

- **[风险] Neo4j 引入增加部署复杂度** → 设计为可降级：Neo4j 故障时智能小法庭功能整体置为“图谱不可用”，庭审主流程（MySQL 写入、LLM 调用、SSE 输出）仍可继续；部署包提供 Neo4j 容器与备份脚本，并文档化重建步骤。
- **[风险] 多 Agent 串行 LLM 导致单轮延迟与成本失控** → 合并“证据校验 + 图谱抽取”为一次结构化输出；观众反馈异步生成不阻塞下一轮；强制 YAML 预算护栏与流式输出；同案件事实抽取阶段做内容指纹缓存。
- **[风险] LLM 编造证据 ID 污染图谱** → 三层校验（schema + 白名单 + 归属）兜底；不通过的引用降级为待证明而不是丢弃发言，确保审计可见。
- **[风险] LLM 把用户主张当已证事实** → `Fact.status` 区分 `CLAIMED / EVIDENCED / DISPUTED / ACCEPTED`，仅 `EVIDENCED+` 进入裁判推理；前端用户确认页强制区分“主张”与“可证事实”。
- **[风险] 法官隐性偏向用户** → 法官输入去 user_side 化 + 双向不利点强约束 + 模拟裁判报告双向要点强制；不达标整体重生成。
- **[风险] Cypher 多跳查询退化** → 唯一约束 + 索引 + 跳数上限 + 节点上限 + 命名 Cypher（避免动态拼接）；关键查询写入集成测试，dbHits 超阈值告警。
- **[风险] 父子分块未就绪** → 显式作为前置依赖；未合入时本变更不进入实现阶段，避免庭审引用碎片化子块。
- **[风险] 多租户跨案件污染** → 节点冗余 `tenantId/caseId` + 唯一约束含 `tenantId` + Cypher 切面强制注入 + 跨租户 MERGE 直接拒绝。
- **[权衡] 不在 MVP 引入观众/陪审 Agent** → 通过角色枚举预留扩展位（`AUDIENCE/JURY` 默认禁用），避免一次性 Agent 数量过多导致编排难以稳定。
- **[权衡] 不在 MVP 接入外部法规库** → 法源依据仅限用户上传材料和 `RISK_RULE` 文档；保证 MVP 范围聚焦，后续作为独立变更扩展。
