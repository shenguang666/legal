## Context

智能小法庭已经完成案件生命周期、庭审轮次、Agent 角色服务、证据引用校验、Neo4j 图谱投影、补证建议和前端三栏页面。当前不足主要集中在两点：

- 开庭按钮没有真正驱动多角色大模型对话，用户无法看到“对方代理人、用户辅助律师、法官”依次发言的庭审过程。
- `CourtRoleAgentService` 里的系统提示词仍以内联字符串形式存在，无法像 `prompts/legal-rag-system.txt` 一样被独立审计、编辑和版本化。

本变更在不改变既有案件、图谱和补证建议数据模型的前提下，补齐流式庭审对话，并将每个 AI 对话对象的系统提示词迁移到 `src/main/resources/prompts`。

## Goals / Non-Goals

**Goals:**

- 小法庭支持可选专用 LLM 配置，优先使用小法庭专用 Key，未配置时默认复用在线问答模型 API Key。
- 点击开庭后自动执行完整一轮多角色对话，顺序为：对方代理人、用户辅助律师、法官。
- 只提供 SSE 流式庭审接口，不新增非流式完整轮次接口。
- 每个 AI 对话对象都有独立系统提示词文件，放在 `src/main/resources/prompts`，采用现有纯文本模板风格。
- 保留现有证据引用校验、法官中立、双向不利点、token 预算、日志和图谱事件机制。

**Non-Goals:**

- 不新增真实语音、视频、多人在线庭审。
- 不新增非流式庭审生成接口。
- 不把系统提示词放入数据库或提供后台在线编辑。
- 不改变现有 MySQL/Neo4j 表结构，除非实现中发现必须记录额外审计字段。
- 不默认暴露完整 prompt 或证据原文到日志。

## Decisions

### 决策 1：小法庭 LLM 配置优先专用 Key，默认复用在线问答 Key

**选择：** 新增 `legal.smart-court.llm.*` 配置，`api-key` 默认表达为 `LEGAL_SMART_COURT_LLM_API_KEY` 优先、为空时回退到 `LEGAL_LLM_API_KEY`。`base-url`、`model-name`、`temperature`、`timeout`、`max-output-tokens` 同样允许小法庭独立配置，并可默认继承在线问答模型配置。

**理由：** 用户要求 Key 不强制独立，但要支持法庭单独配 Key。该策略既支持成本隔离，也避免未配置专用 Key 时小法庭直接不可用。

**替代方案：** 强制专用 Key。该方案成本隔离更强，但与当前确认需求不符。

### 决策 2：每个 AI 对话对象使用资源提示词模板

**选择：** 在 `src/main/resources/prompts` 新增角色系统提示词文件，至少包括：

- `court-opponent-agent-system.txt`
- `court-user-advisor-agent-system.txt`
- `court-judge-agent-system.txt`

提示词文件使用与 `legal-rag-system.txt` 一致的纯文本 Markdown 风格，包含角色边界、输入说明、工作流程、输出格式、安全规则。模板变量由后端渲染，常见变量包括 `{{caseSummary}}`、`{{stage}}`、`{{instruction}}`、`{{hearingHistory}}`、`{{evidenceContext}}`、`{{allowedRefs}}`。

**理由：** 系统提示词属于可审计的业务规则，不应散落在 Java 字符串中。资源模板能被代码评审、测试和后续调参稳定追踪。

**替代方案：** 继续在代码内维护 prompt。实现简单，但角色边界难审计，且每次调参都需要改 Java 代码。

### 决策 3：开庭流式接口执行完整一轮方案 A

**选择：** 前端点击开庭后调用 SSE 接口，后端按固定顺序执行 `OPPONENT -> USER_ADVISOR -> JUDGE`。每个角色调用前构建独立 system prompt 和 user prompt，调用后通过 SSE 发送增量事件和完成事件，并保存消息、论点、token 和图谱事件。

**理由：** 一次点击完成完整一轮，符合“小法庭”用户心智；用户可以连续看到对方攻击、己方辅助、法官归纳的完整闭环。

**替代方案：** 每个角色单独按钮推进。控制力更强，但交互割裂，MVP 不采用。

### 决策 4：只保留流式庭审路径，异常统一 fallback

**选择：** 不新增非流式完整轮次接口。SSE 初始化失败、模型不可用、模型异常、预算或状态异常导致无法继续时，向前端推送或返回统一业务提示：`服务繁忙，请稍后重试`。服务端仍记录结构化错误日志和审计信息。

**理由：** 用户明确要求只写流式接口，fallback 只需要拒绝信息，避免维护两套行为不一致的生成链路。

**替代方案：** 同时提供非流式 fallback。可用性更高，但增加测试面和状态一致性风险。

### 决策 5：不在日志中输出完整 prompt

**选择：** 日志只记录 `tenantId / caseId / roundId / attemptId / speaker / modelName / tokenUsage / durationMs / errorCode` 等摘要信息，不记录完整 prompt、证据上下文或模型原文。

**理由：** 法庭材料通常包含合同、金额、履约细节等敏感信息，完整 prompt 日志会扩大泄露面。

## Risks / Trade-offs

- **[风险] 流式接口中途断开导致轮次状态不一致** → 继续沿用现有 SSE 规范，不触发 Spring MVC error dispatch；断开时按轮次状态机标记失败或取消，并保留可审计日志。
- **[风险] 提示词模板变量缺失导致输出不稳定** → 提供模板加载测试，启动或调用时校验关键角色模板存在，渲染时对空变量使用安全默认值。
- **[风险] 复用在线问答 Key 导致成本难区分** → token 计量仍使用 `bizType=SMART_COURT`，日志记录小法庭模型名与角色，便于按业务区分。
- **[风险] 多角色串行调用延迟较高** → 通过 SSE 逐角色输出降低用户等待感；仍保留单轮和单案件 token/轮次预算护栏。
- **[风险] 模型输出 JSON 不合规** → 继续使用现有 schema 校验、证据白名单校验和法官双向不利点校验；失败时按已有重试策略处理，最终返回服务繁忙或降级审计状态。

## Migration Plan

- 新增资源提示词文件，不影响已有接口启动。
- 新增小法庭 LLM 配置项时在 `application.yaml` 为每一项添加中文注释，默认兼容现有 `LEGAL_LLM_*` 环境变量。
- 将 `CourtRoleAgentService` 从内联 prompt 切换为资源模板加载；如模板缺失，测试和启动检查应提前暴露问题。
- 前端开庭按钮切到 SSE 流式接口后，保留已有状态提示和失败提示。
- 回滚时可恢复旧的开庭状态切换路径，但不建议继续保留内联 prompt。

## Open Questions

- 是否需要为后续“观众/旁听视角”预留 `court-audience-agent-system.txt` 文件。本次默认不实现观众角色，只保留扩展约定。 answer：预留，后续需要时再添加即可。
- 法官输出是否继续要求严格 JSON，还是在 SSE 中展示自然语言、后台保存结构化 JSON。本次建议保持后台 JSON 结构化，前端可渲染摘要文本。  answer：保持后台 JSON 结构化，前端可渲染摘要文本。
