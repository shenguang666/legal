## Why

当前合同审查、风险识别、知识库问答能让用户看到“有什么风险”和“有什么内容”，但无法让用户在真正进入诉讼或谈判前，验证自己一方的事实主张、证据链是否闭环、关键金额/时间/义务是否能被现有材料证明、以及对方可能如何反驳。用户经常在材料齐备的假象下进入纠纷，临场才发现关键证据缺失或证据之间相互矛盾。

智能小法庭以“合同纠纷模拟庭审”为切入点，由 AI 同时扮演法官、对方当事人/代理人，引导用户围绕合同事实进行结构化攻防；以 Neo4j 知识图谱沉淀事实-证据-条款-诉求-抗辩-风险-缺口之间的关系，输出可解释的模拟裁判观点和针对性补证建议，帮助用户在低成本、可重复的环境下提前发现证据缺口和论证漏洞。

## What Changes

- 新增“智能小法庭”模块：支持创建合同纠纷案件、选择用户立场（原告/被告）、绑定合同与证据材料、由 AI 主持模拟庭审完整流程（陈述、答辩、举证、质证、辩论、最后陈述、模拟裁判意见）。
- 新增 Neo4j 证据链知识图谱：图谱节点覆盖案件、当事人、诉求、抗辩、事实、证据、文档、条款、金额、日期、风险、证据缺口、模拟裁判观点等；图谱关系覆盖支持、反驳、引用、来源、违约、削弱、补证等关系；庭审每一轮按事件方式异步投影到 Neo4j。
- 新增基于图谱与规则的“补证建议”能力：识别诉求未闭环、金额冲突、关键时间节点缺失、对方抗辩未被反驳等情形，给出可解释的补证建议清单。
- 新增多角色 AI 编排：法官、对方代理人、（可选）观众/陪审视角的角色 prompt 与上下文严格隔离；法官输入去用户立场化，强制双向不利点输出。
- 新增证据引用三层校验：所有 AI 发言必须引用真实存在的证据/合同片段 ID，经 schema、白名单、归属（租户+案件）三层校验后才能写入图谱与庭审记录；幻觉引用降级为“待证明”。
- 新增预算与流程护栏：单案件最大轮次、单轮最大 token、单案件累计 token 上限、单 Cypher 路径跳数上限均通过 YAML 配置（带中文注释）。
- 新增前端“智能小法庭”页面：三栏布局（材料/证据、庭审对话、证据链图谱），支持案件创建向导、庭审推进、证据链动态可视化、模拟裁判报告导出（带“仅供模拟参考”水印）。
- 复用现有能力：MinerU 精准解析、文档清洗、RAG 检索、token 使用计量、文档资产私有访问、租户隔离体系；庭审上下文优先使用父级语义块。

## Capabilities

### New Capabilities
- `smart-court-simulation`: 智能小法庭案件管理与多角色 AI 模拟庭审编排能力，定义案件生命周期、庭审阶段状态机、多角色发言约束、证据引用校验、预算护栏、模拟裁判报告。
- `evidence-chain-knowledge-graph`: 基于 Neo4j 的证据链知识图谱能力，定义节点/关系类型、租户隔离、双写一致性（MySQL 权威源 + 异步图谱投影）、Cypher 查询硬约束、图谱版本化与重建。
- `supplement-evidence-suggestion`: 基于图谱规则与 LLM 解释的补证建议能力，定义缺口识别规则、建议结构、可解释依据、与庭审阶段联动。

### Modified Capabilities
- `rag-parent-context-expansion`: 增加“智能小法庭庭审 Agent 必须以父级语义块作为发言上下文”的检索行为，子块仅用于召回，最终注入庭审上下文的是父块。
- `user-knowledge-curation`: 增加智能小法庭对合同与证据材料的引用与展示要求（材料列表来源、私有资产访问复用、证据原文回溯、不直接修改知识库 CRUD 行为）。

## Impact

- **后端代码**：新增 `com.legal.court` 包（controller/service/dto/entity/mapper/agent/graph 子包）；新增 Spring Data Neo4j 依赖与配置；新增 `CourtCaseService`、`CourtOrchestrator`、`CourtRoleAgentService`、`CourtEvidenceService`、`CourtGraphService`、`CourtGraphProjector`、`CourtSuggestionService`、`CourtJudgmentService`；扩展 RAG 检索接口在 `bizType=SMART_COURT` 下返回父块上下文。
- **数据库**：新增 MySQL 表 `court_case`、`court_case_party`、`court_case_evidence`、`court_hearing_round`、`court_hearing_message`、`court_argument`、`court_graph_event`、`court_judgment_report`、`court_supplement_suggestion`，所有字段带中文注释；新增 Neo4j 节点/关系唯一约束与 `tenantId+caseId` 索引。
- **配置**：新增 `legal.smart-court.*` YAML 配置项（启用开关、预算护栏、Cypher 跳数上限、Neo4j 连接、图谱重建批大小等），全部带中文注释；新增 `spring.neo4j.*` 连接配置。
- **前端代码**：新增 `web/src/views/SmartCourtView.vue` 与子组件、`web/src/api/court.ts`、路由 `/smart-court`，并同步更新 `router/index.ts` 与 `router/index.js`；新增图谱可视化组件（基于现有项目可用的图可视化库或 ECharts graph）。
- **依赖**：新增 `org.springframework.boot:spring-boot-starter-data-neo4j`；前端按需引入图可视化库（若已具备 ECharts 则复用）。
- **运维**：部署包需新增 Neo4j 服务（容器化），约定备份、升级与从 `court_graph_event` 全量重建图谱的运维流程。
- **合规**：所有模拟裁判输出固定附带“仅供模拟参考、不构成法律意见”水印与 disclaimer；庭审与图谱记录纳入审计。
- **前置依赖**：依赖活跃变更 `refine-mineru-parent-child-chunking` 完成，以保证庭审 Agent 能稳定获取父级上下文；若该变更未完成，本变更进入实现阶段需先完成它。
