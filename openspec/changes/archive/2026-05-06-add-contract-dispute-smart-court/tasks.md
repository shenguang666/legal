## 1. 前置依赖与基础设施

- [x] 1.1 确认 `refine-mineru-parent-child-chunking` 已完成或并行推进，本变更进入实现阶段前 MUST 已合入
- [x] 1.2 在 `pom.xml` 引入 `spring-boot-starter-data-neo4j`，并锁定与当前 Spring Boot 版本兼容的 Neo4j 5.x driver
- [x] 1.3 在 `application.yaml` 增加 `spring.neo4j.uri / authentication.username / authentication.password`，从环境变量注入，并加中文注释
- [x] 1.4 在 `application.yaml` 增加 `legal.smart-court.*` 全部配置项（`enabled / max-rounds / max-tokens-per-round / max-tokens-per-case / evidence-ref-retry / graph.max-hops / graph.max-nodes-per-case / graph.projector-batch-size / graph.rebuild-page-size`），每项加中文注释
- [x] 1.5 新增 `SmartCourtProperties` 配置类，所有字段加中文注释
- [x] 1.6 新增 `Neo4jConfig`：声明 `Driver`、`@EnableNeo4jRepositories(basePackages="com.legal.court.graph.repository")`、`@EnableTransactionManagement`、`cypherDslConfiguration` 使用 `Dialect.NEO4J_5`
- [x] 1.7 部署包：在 docker 目录新增 Neo4j 5 容器、数据卷、环境变量样例与备份脚本，更新 README

## 2. 数据库 Schema

- [x] 2.1 在 `src/main/resources/db/schema.sql` 新增表 `court_case`、`court_case_party`、`court_case_evidence`、`court_hearing_round`、`court_hearing_message`、`court_argument`、`court_graph_event`、`court_judgment_report`、`court_supplement_suggestion`，所有字段加中文注释
- [x] 2.2 在 `src/main/resources/db/` 新增增量脚本 `20260506_add_smart_court.sql`，与 schema.sql 保持一致并带中文注释
- [x] 2.3 增量脚本中为高频查询字段建立索引：`court_case(tenant_id, owner_user_id, status)`、`court_hearing_round(case_id, state)`、`court_graph_event(case_id, status, id)`、`court_supplement_suggestion(case_id, severity, status)`
- [x] 2.4 通过 dbhub 在本地 MySQL 应用增量脚本，并验证表结构与注释

## 3. Neo4j Schema 与初始化

- [x] 3.1 新增 `Neo4jSchemaInitializer`，启动时幂等执行：节点 `(tenantId, businessId)` 唯一约束（覆盖 `Case / Party / Claim / Defense / Fact / Evidence / Argument / JudgmentPoint`）
- [x] 3.2 创建索引：`Evidence(tenantId, caseId)`、`Fact(tenantId, caseId, status)`、`Claim(tenantId, caseId)`、`Gap(tenantId, caseId)`、`Risk(tenantId, caseId)`
- [x] 3.3 在节点 payload 中预留 `schemaVersion` 字段，初始化版本号常量

## 4. MySQL 实体与 Mapper

- [x] 4.1 新增实体 `CourtCaseEntity / CourtCasePartyEntity / CourtCaseEvidenceEntity / CourtHearingRoundEntity / CourtHearingMessageEntity / CourtArgumentEntity / CourtGraphEventEntity / CourtJudgmentReportEntity / CourtSupplementSuggestionEntity`，所有字段加中文注释
- [x] 4.2 新增枚举 `CourtCaseStatus / CourtCaseRole / CourtPartyRole / CourtHearingStage / CourtHearingState / CourtArgumentSpeaker / CourtArgumentStance / CourtFactStatus / CourtGraphEventStatus / CourtSuggestionType / CourtSuggestionSeverity / CourtSuggestionStatus`，每个枚举类加中文注释
- [x] 4.3 新增对应 MyBatis-Plus Mapper 接口
- [x] 4.4 新增 DTO：`CourtCaseDto / CourtPartyDto / CourtEvidenceDto / CourtHearingRoundDto / CourtArgumentDto / CourtGraphSnapshotDto / CourtSuggestionDto / CourtJudgmentReportDto`，字段加中文注释

## 5. Neo4j 节点/关系实体与仓储

- [x] 5.1 在 `com.legal.court.graph.node` 下定义 `@Node` 实体：`CaseNode / PartyNode / ClaimNode / DefenseNode / FactNode / EvidenceNode / DocumentNode / ChunkNode / ClauseNode / ObligationNode / BreachNode / AmountNode / DateNode / ArgumentNode / RiskNode / GapNode / LegalBasisNode / JudgmentPointNode`，所有属性加中文注释，含 `tenantId / caseId / schemaVersion`
- [x] 5.2 定义 `@RelationshipProperties`：`SupportedByRel / ContradictedByRel / DerivedFromRel / SupportsArgumentRel / ChallengesArgumentRel / NeedsEvidenceRel / SupportsJudgmentRel`，含证据来源、置信度、`eventId` 等属性
- [x] 5.3 新增 `Neo4jTenantContext` 切面，禁止业务代码裸执行 Cypher，统一注入 `tenantId`
- [x] 5.4 新增 Repository（如 `CaseGraphRepository / EvidenceGraphRepository / ClaimGraphRepository`），关键查询使用命名 `@Query`

## 6. 证据引用三层校验

- [x] 6.1 新增 `CourtEvidenceService.allowedRefsForRound(caseId, tenantId, roundId)`，按租户、案件、轮次返回允许引用的 `evidenceId / chunkId` 集合
- [x] 6.2 新增 `CourtAgentResponseValidator`：schema 校验、白名单 `Set.contains` 过滤、归属（`tenantId + caseId`）校验三层
- [x] 6.3 校验失败时按 `legal.smart-court.evidence-ref-retry` 整轮重试；重试用尽降级为 `PENDING_PROOF`，写入 `court_argument.evidence_dropped_count` 并记录指标 `court.agent.evidence_hallucination`
- [x] 6.4 单元测试：覆盖 schema 失败、白名单不命中、跨租户引用、跨案件引用、合法引用四种路径

## 7. 多角色 Agent 与编排

- [x] 7.1 新增 `CourtRoleAgentService`：分别封装 `JudgeAgent / OpponentAgent / UserAdvisorAgent`，各自独立 prompt 与上下文构造器
- [x] 7.2 法官输入去 user_side 化：编排器把当事人替换为 `PartyA / PartyB`，前端渲染层再回填实际身份
- [x] 7.3 法官输出强制 JSON schema：`focusIssues / acceptedFacts / rejectedFacts / unfavorableToPartyA / unfavorableToPartyB / openQuestions`；任一不利点列表为空触发整体重生成（最多 2 次），仍失败回退为“证据不足，无法形成倾向性意见”
- [x] 7.4 新增 `CourtOrchestrator`：阶段状态机推进、`(round_id, attempt_id)` 幂等、乐观锁切换 `PENDING -> RUNNING`、SSE 流式输出、用户停止支持
- [x] 7.5 复用现有 SSE 异常处理规范，禁止 `completeWithError` 触发 Spring MVC error dispatch
- [x] 7.6 集成项目现有 token 计量，所有 LLM 调用按 `bizType=SMART_COURT` 入账 `token_usage_metric_*`
- [x] 7.7 单元测试：法官双向不利点强约束、用户立场遮蔽、stage 状态机切换、并发乐观锁

## 8. 庭审上下文使用父级语义块

- [x] 8.1 庭审 RAG 检索接口在 `bizType=SMART_COURT` 时强制启用父级上下文扩展，禁止调用方关闭
- [x] 8.2 在 `court_argument.evidence_refs` 与 `court_case_evidence` 中同时记录 `parent_chunk_id` 与命中的 `child_chunk_id`
- [x] 8.3 父分块缺失时跳过该命中，本轮发言降级为“证据不足”并写入审计
- [x] 8.4 集成测试：构造命中子块的场景，断言上下文使用父块、审计记录包含父子映射

## 9. 图谱事件投影器

- [x] 9.1 新增 `CourtGraphEventPublisher`：所有图谱写入仅通过事件入队，禁止业务代码直接写 Neo4j
- [x] 9.2 新增 `CourtGraphProjector`：单线程批量消费 PENDING 事件，按 `eventId` 在节点/关系属性 MERGE 实现幂等
- [x] 9.3 投影失败按指数退避重试，达上限置 `DEAD` 并告警；事件状态机：`PENDING / APPLIED / DEAD`
- [x] 9.4 节点上限保护：投影前判断当前案件节点数，超过 `legal.smart-court.graph.max-nodes-per-case` 拒绝写入并告警
- [x] 9.5 跨 `tenantId` MERGE 显式拒绝，写入审计指标
- [x] 9.6 全量重建接口：管理员触发后先 `DETACH DELETE` 案件子图，再按 `legal.smart-court.graph.rebuild-page-size` 分页回放
- [x] 9.7 集成测试：单事件幂等、批量乱序、Neo4j 故障下投影器优雅退避、重建后图谱与事件流一致

## 10. 图谱查询服务

- [x] 10.1 新增 `CourtGraphQueryService`：核心查询使用命名 Cypher，路径 `*1..N` 受 `legal.smart-court.graph.max-hops` 限制
- [x] 10.2 默认子图查询：围绕 `focusClaimId` 的 2 跳子图，节点数 ≤ 200
- [x] 10.3 接口返回 `graphState`：`READY / PROJECTING / UNAVAILABLE`，并附带剩余 PENDING 事件数
- [x] 10.4 Neo4j 不可达时返回 `UNAVAILABLE` 并退化为基于 MySQL 的事实/证据列表
- [x] 10.5 性能基线测试：构造典型案件，断言关键查询 dbHits 不超过阈值

## 11. 补证建议

- [x] 11.1 新增 `CourtSuggestionRuleEngine`：用命名 Cypher 实现四类规则（`CLAIM_NOT_CLOSED / AMOUNT_INCONSISTENT / KEY_DATE_MISSING / DEFENSE_NOT_REBUTTED`），返回结构化缺口列表
- [x] 11.2 新增 `CourtSuggestionService`：把规则结果交给 LLM 改写为可解释文案，并复用证据引用三层校验
- [x] 11.3 持久化到 `court_supplement_suggestion`，支持差量更新（新增 / 解除 / 升级），解除的建议保留 `RESOLVED` 状态用于审计
- [x] 11.4 在每轮投影完成后增量刷新；进入“最后陈述”阶段前向前端高亮 `HIGH` 等级建议
- [x] 11.5 单元测试：四类规则触发条件、LLM 不得新增缺口的过滤、跨租户证据引用降级

## 12. 案件生命周期与服务

- [x] 12.1 新增 `CourtCaseService`：创建、查询、更新、归档、软删除、要素抽取与用户确认
- [x] 12.2 案件创建仅允许引用本租户、本用户的 `KNOWLEDGE / RISK_RULE` 文档；跨租户访问返回 404 而非 403
- [x] 12.3 软删除：`court_case.status='DELETED'`，触发 Neo4j 子图清理（异步幂等）
- [x] 12.4 知识库文档软删除时把对应 `EvidenceNode` 标记为 `INVALID`，不级联删除案件
- [x] 12.5 单元测试：跨租户访问、软删除、文档失效

## 13. 模拟裁判报告

- [x] 13.1 新增 `CourtJudgmentService`：基于已确认事实、证据采信、争议焦点生成结构化模拟裁判报告
- [x] 13.2 报告强制双向要点（支持原告与支持被告各至少一条），不达标整体重生成
- [x] 13.3 报告渲染（前端与 PDF 导出）固定附带“仅供模拟参考、不构成法律意见”水印
- [x] 13.4 报告导出审计日志（`caseId / userId / tenantId / exportFormat / timestamp`）

## 14. Controller 与对外 API

- [x] 14.1 新增 `SmartCourtController`：案件 CRUD、要素抽取与确认、开庭、单轮推进、用户停止、报告生成与导出
- [x] 14.2 新增 `SmartCourtGraphController`：`GET /api/smart-court/cases/{id}/graph`，支持 `focusClaimId / hops / limit`
- [x] 14.3 新增 `SmartCourtSuggestionController`：列表、忽略、解除、状态历史
- [x] 14.4 全部接口走现有 `AuthPrincipal`，租户/用户隔离严格
- [x] 14.5 SSE 流式接口（庭审推进）严格遵循项目 SSE 异常处理规范

## 15. 前端

- [x] 15.1 新增 `web/src/api/court.ts`：封装案件、庭审、图谱、建议、报告全部接口；保持 `client.ts` 与 `client.js` 一致约定
- [x] 15.2 新增 `web/src/views/SmartCourtView.vue`：三栏布局（材料/证据 / 庭审对话 / 证据链图谱），支持创建向导、SSE 流式、用户停止、要素确认、最后陈述前高严重度建议提示
- [x] 15.3 图谱可视化基于 ECharts `graph` 系列实现，按支持/反驳/冲突/缺失/条款/裁判分色，默认渲染 2 跳子图
- [x] 15.4 模拟裁判报告页：渲染双向要点 + 合规水印，提供 PDF 导出
- [x] 15.5 路由 `/smart-court` 同时更新 `web/src/router/index.ts` 与 `web/src/router/index.js`，并在主导航增加入口
- [x] 15.6 编译产物 `*.vue.js` 仅作为构建输出，提交前还原以保持源代码与 `index.ts` 同步
- [x] 15.7 前端单元/E2E：要素确认、开庭流式、停止、图谱节点点击回溯、补证建议确认/忽略

## 16. 可观测性与运维

- [x] 16.1 新增结构化日志：`court.case.create / round.run / agent.invoke / evidence.validation / graph.event.publish / graph.event.apply / suggestion.refresh`，含 `tenantId / caseId / roundId / attemptId`
- [x] 16.2 新增指标：`court.agent.evidence_hallucination`、`court.graph.event.dead`、`court.graph.event.pending`、`court.graph.query.dbHits`、`court.suggestion.high_count`
- [x] 16.3 健康检查端点：`/actuator/health` 包含 Neo4j 子项；不可达时返回降级状态而非 fail-fast
- [x] 16.4 文档化 Neo4j 备份、升级、案件级图谱重建运维流程

## 17. 测试与验收

- [x] 17.1 单元测试覆盖：`SmartCourtPropertiesTest / CourtAgentResponseValidatorTest / CourtJudgeOutputValidatorTest / CourtOrchestratorStateMachineTest / CourtGraphProjectorTest / CourtSuggestionRuleEngineTest`
- [x] 17.2 集成测试：完整一案多轮流程（含 SSE 流式、用户停止、补证建议、报告生成、图谱重建），断言 MySQL 与 Neo4j 一致
- [x] 17.3 性能/容量测试：单案件预算护栏在最大轮数+最大 token 配置下不被突破；典型查询 dbHits 在阈值内
- [x] 17.4 安全测试：跨租户访问案件/图谱/证据返回 404；非法证据引用降级；越权 Cypher 注入失败
- [x] 17.5 运行 `mvn -DskipTests compile`、`mvn test`、`npm run build`，全部通过
- [x] 17.6 运行 `openspec validate add-contract-dispute-smart-court --strict`，通过后准备进入 `/opsx:apply`
