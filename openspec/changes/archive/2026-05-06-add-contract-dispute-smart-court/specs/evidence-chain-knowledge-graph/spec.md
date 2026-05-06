## ADDED Requirements

### Requirement: MySQL 为权威源 Neo4j 为异步图谱投影
系统 SHALL 把 MySQL 作为案件、庭审、证据、图谱事件的唯一权威源，Neo4j 仅作为基于 `court_graph_event` 异步幂等投影出的图谱视图，MUST 永远可从 MySQL 事件流全量重建。

#### Scenario: 事件先落 MySQL 再异步投影
- **WHEN** 庭审某轮产生新的图谱节点或关系
- **THEN** 系统 MUST 在同一 MySQL 事务内写入庭审记录与一条或多条 `court_graph_event(payload_json, status='PENDING')`
- **THEN** 系统 MUST 由独立 `CourtGraphProjector` 异步消费 PENDING 事件并幂等写入 Neo4j
- **THEN** 系统 MUST NOT 在主流程中直接调用 Neo4j 写入

#### Scenario: 投影失败重试与死信
- **WHEN** Neo4j 投影失败
- **THEN** 系统 MUST 按指数退避重试，重试次数与超时由 YAML 配置
- **THEN** 系统 MUST 在重试用尽后把事件标记为 `DEAD`，并触发告警日志

#### Scenario: Neo4j 故障不阻塞庭审
- **WHEN** Neo4j 整体不可用
- **THEN** 系统 MUST 允许庭审继续进行（MySQL 写入、LLM 调用、SSE 输出）
- **THEN** 系统 MUST 在前端图谱区域返回 `graphState='UNAVAILABLE'`，提示图谱暂时不可用

#### Scenario: 全量重建图谱
- **WHEN** 管理员触发某案件图谱重建
- **THEN** 系统 MUST 先在 Neo4j 对该案件执行 `DETACH DELETE` 清理子图
- **THEN** 系统 MUST 按事件顺序回放 `court_graph_event`，按 `legal.smart-court.graph.rebuild-page-size` 分页处理
- **THEN** 系统 MUST 在重建完成后把所有事件状态设为 `APPLIED`

### Requirement: 图谱节点与关系类型
系统 SHALL 按业务语义建模图谱节点与关系，MUST 至少覆盖案件、当事人、诉求、抗辩、事实、证据、文档、片段、条款、义务、违约、金额、日期、观点、风险、缺口、法源依据、模拟裁判观点等节点类型。

#### Scenario: 节点类型覆盖
- **WHEN** 系统初始化 Neo4j schema
- **THEN** 系统 MUST 创建标签 `Case / Party / Claim / Defense / Fact / Evidence / Document / Chunk / Clause / Obligation / Breach / Amount / Date / Argument / Risk / Gap / LegalBasis / JudgmentPoint`

#### Scenario: 关系类型覆盖
- **WHEN** 系统初始化 Neo4j schema
- **THEN** 系统 MUST 支持关系 `HAS_PARTY / RAISES_CLAIM / RAISES_DEFENSE / ASSERTS_FACT / SUPPORTED_BY / CONTRADICTED_BY / DERIVED_FROM / QUOTES_CLAUSE / PROVES_AMOUNT / PROVES_DATE / CREATES_OBLIGATION / BREACHES_OBLIGATION / SUPPORTS_ARGUMENT / CHALLENGES_ARGUMENT / HAS_RISK / HAS_GAP / NEEDS_EVIDENCE / SUPPORTS_JUDGMENT`

### Requirement: 多租户隔离与唯一约束
系统 SHALL 在所有 Neo4j 节点上冗余 `tenantId` 与 `caseId` 属性，并 MUST 为每类核心节点建立含 `tenantId` 的唯一约束与索引。

#### Scenario: 节点冗余 tenantId
- **WHEN** 任意图谱节点被创建
- **THEN** 系统 MUST 把 `tenantId` 与 `caseId` 写入节点属性
- **THEN** 系统 MUST NOT 创建跨 `tenantId` 的关系

#### Scenario: 唯一约束与索引
- **WHEN** 系统初始化 Neo4j schema
- **THEN** 系统 MUST 为 `Case / Party / Claim / Defense / Fact / Evidence / Argument / JudgmentPoint` 等节点创建 `(tenantId, businessId)` 唯一约束
- **THEN** 系统 MUST 为高频检索节点（如 `Evidence / Fact / Claim`）创建 `(tenantId, caseId)` 索引

#### Scenario: Cypher 强制注入 tenantId
- **WHEN** 系统执行任意 Cypher 查询
- **THEN** 系统 MUST 通过统一切面把 `tenantId` 作为参数注入
- **THEN** 系统 MUST 拒绝业务代码裸执行 Cypher

### Requirement: 事实节点状态分级
系统 SHALL 在 `Fact` 节点上维护 `status` 属性，取值 MUST 为 `CLAIMED / EVIDENCED / DISPUTED / ACCEPTED` 之一，且 `CLAIMED` 状态的事实 MUST NOT 直接进入模拟裁判推理。

#### Scenario: 主张升级为已证事实
- **WHEN** 某 `Fact` 节点至少存在一条 `SUPPORTED_BY` 关系连到通过校验的 `Evidence`
- **THEN** 系统 MUST 把 `status` 由 `CLAIMED` 升级为 `EVIDENCED`

#### Scenario: 已证事实被反驳进入争议
- **WHEN** 某 `Fact` 节点同时存在 `SUPPORTED_BY` 与 `CONTRADICTED_BY` 关系
- **THEN** 系统 MUST 把 `status` 置为 `DISPUTED`
- **THEN** 系统 MUST 在补证建议中标记需要进一步证据消除冲突

#### Scenario: 仅主张不进入裁判推理
- **WHEN** 系统生成模拟裁判观点
- **THEN** 系统 MUST 排除 `status=CLAIMED` 的事实节点
- **THEN** 系统 MUST 在裁判观点说明中提示哪些主张尚未被证据支持

### Requirement: Cypher 查询硬约束
系统 SHALL 限制 Neo4j 查询代价，MUST 通过 `legal.smart-court.graph.max-hops` 控制路径跳数上限，并通过 `legal.smart-court.graph.max-nodes-per-case` 限制单案件节点总数。

#### Scenario: 多跳路径跳数受限
- **WHEN** 任意 Court 图谱查询使用可变长度路径
- **THEN** 系统 MUST 显式声明 `*1..N`，且 N MUST NOT 超过 `legal.smart-court.graph.max-hops` 配置值
- **THEN** 系统 MUST NOT 使用无上限的可变长度路径

#### Scenario: 单案件节点上限
- **WHEN** 投影器即将写入新节点导致案件节点总数超过 `legal.smart-court.graph.max-nodes-per-case`
- **THEN** 系统 MUST 拒绝写入该新节点
- **THEN** 系统 MUST 在告警日志中记录案件 ID 与当前节点数

#### Scenario: 命名 Cypher 与回归基线
- **WHEN** 实现关键查询（证据链、缺口、争议焦点）
- **THEN** 系统 MUST 使用命名 Cypher（Repository `@Query` 或常量），不得动态拼接
- **THEN** 系统 MUST 提供集成测试基线，回归时若 dbHits 超阈值 MUST 触发失败

### Requirement: 图谱投影状态对外暴露
系统 SHALL 在图谱查询接口返回 `graphState`，MUST 取值为 `READY / PROJECTING / UNAVAILABLE` 之一。

#### Scenario: 投影未完成
- **WHEN** 案件存在尚未完成的 `court_graph_event(status='PENDING')`
- **THEN** 系统 MUST 在响应中返回 `graphState='PROJECTING'`，并附带剩余事件数

#### Scenario: 投影完成
- **WHEN** 该案件无 PENDING 事件，且 Neo4j 可访问
- **THEN** 系统 MUST 返回 `graphState='READY'`

#### Scenario: Neo4j 不可访问
- **WHEN** Neo4j 不可访问
- **THEN** 系统 MUST 返回 `graphState='UNAVAILABLE'`，并退化为只展示 MySQL 中的事实和证据列表

### Requirement: 图谱前端可视化裁剪
系统 SHALL 在前端图谱接口默认按当前争议焦点 2 跳子图返回，MUST 支持 `focusClaimId / hops / limit` 参数，并 MUST 不一次性返回超过节点上限的整图。

#### Scenario: 默认子图
- **WHEN** 前端调用 `GET /api/smart-court/cases/{id}/graph` 不带参数
- **THEN** 系统 MUST 返回围绕当前争议焦点的 2 跳子图，且节点数不超过 200

#### Scenario: 自定义跳数
- **WHEN** 前端传入 `hops` 参数
- **THEN** 系统 MUST 把请求跳数与 `legal.smart-court.graph.max-hops` 取较小值
- **THEN** 系统 MUST 不允许跳数超过最大值

### Requirement: 图谱节点 Schema 版本化
系统 SHALL 在节点 payload 中携带 `schemaVersion` 属性，MUST 在新增节点类型或字段时通过版本号迁移，而不是破坏式覆盖。

#### Scenario: 新增节点字段
- **WHEN** 系统升级节点 schema 版本
- **THEN** 系统 MUST 把新版本号写入新创建节点的 `schemaVersion`
- **THEN** 系统 MUST 在投影器中兼容低版本节点的读取
