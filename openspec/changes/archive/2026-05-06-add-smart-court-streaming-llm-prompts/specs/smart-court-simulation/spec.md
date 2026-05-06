## ADDED Requirements

### Requirement: 小法庭 LLM 配置 SHALL 支持专用 Key 并默认复用在线问答 Key
系统 SHALL 为智能小法庭提供独立的 `legal.smart-court.llm.*` 配置，且 MUST 在小法庭专用 API Key 未配置时默认复用在线问答模型 API Key。

#### Scenario: 使用小法庭专用 Key
- **WHEN** 环境变量配置了 `LEGAL_SMART_COURT_LLM_API_KEY`
- **THEN** 智能小法庭 LLM 调用 MUST 使用该专用 Key
- **THEN** 在线问答、Embedding 或其他业务的模型 Key MUST NOT 被该调用覆盖

#### Scenario: 未配置小法庭专用 Key 时复用在线问答 Key
- **WHEN** `LEGAL_SMART_COURT_LLM_API_KEY` 为空且 `LEGAL_LLM_API_KEY` 已配置
- **THEN** 智能小法庭 LLM 调用 MUST 复用在线问答模型 API Key
- **THEN** token 计量仍 MUST 按 `bizType=SMART_COURT` 记录

#### Scenario: 两类 Key 都不可用时拒绝流式开庭
- **WHEN** 小法庭专用 API Key 与在线问答 API Key 都不可用
- **THEN** 系统 MUST 拒绝开庭流式调用
- **THEN** 前端 MUST 展示 `服务繁忙，请稍后重试`

### Requirement: AI 对话对象 SHALL 使用资源目录系统提示词
系统 SHALL 为每个智能小法庭 AI 对话对象提供独立系统提示词文件，文件 MUST 位于 `src/main/resources/prompts`，且格式 MUST 参考现有 `legal-rag-system.txt` 的纯文本模板风格。

#### Scenario: 对方代理人系统提示词来自资源文件
- **WHEN** 系统调用 AI 对方代理人生成庭审发言
- **THEN** 系统 MUST 从 `classpath:prompts/court-opponent-agent-system.txt` 加载系统提示词
- **THEN** 提示词 MUST 明确限制其只能引用本案允许证据，不得编造事实、合同条款或证据编号

#### Scenario: 用户辅助律师系统提示词来自资源文件
- **WHEN** 系统调用 AI 用户辅助律师生成庭审发言
- **THEN** 系统 MUST 从 `classpath:prompts/court-user-advisor-agent-system.txt` 加载系统提示词
- **THEN** 提示词 MUST 明确其职责是帮助用户补强主张、提示证据缺口，并禁止编造证据

#### Scenario: 法官系统提示词来自资源文件
- **WHEN** 系统调用 AI 法官生成庭审归纳或模拟裁判观点
- **THEN** 系统 MUST 从 `classpath:prompts/court-judge-agent-system.txt` 加载系统提示词
- **THEN** 提示词 MUST 要求法官保持中立、输入去用户立场化，并输出双方不利点

#### Scenario: 提示词模板变量安全渲染
- **WHEN** 系统渲染任意小法庭系统提示词模板
- **THEN** 系统 MUST 支持注入庭审阶段、案件摘要、本轮指令、历史庭审摘要、证据上下文和允许引用白名单
- **THEN** 任一变量为空时系统 MUST 使用安全默认值而不是输出 `null`

### Requirement: 开庭 SHALL 通过流式接口自动生成完整一轮多角色对话
系统 SHALL 在用户点击开庭后通过 SSE 流式接口自动生成完整一轮庭审对话，角色顺序 MUST 为 `对方代理人 -> 用户辅助律师 -> 法官`。

#### Scenario: 开庭自动执行完整一轮
- **WHEN** 用户在已确认要素的案件上点击开庭
- **THEN** 前端 MUST 调用智能小法庭流式庭审接口
- **THEN** 后端 MUST 按顺序调用 AI 对方代理人、AI 用户辅助律师和 AI 法官
- **THEN** 每个角色的输出 MUST 通过 SSE 事件推送给前端展示

#### Scenario: 角色发言持久化
- **WHEN** 任一 AI 角色完成本轮发言
- **THEN** 系统 MUST 保存对应的庭审消息和论点记录
- **THEN** 系统 MUST 保存模型名称、角色、轮次、尝试编号和 token 消耗

#### Scenario: 保持证据引用校验
- **WHEN** 任一 AI 角色输出包含证据、父分块或子分块引用
- **THEN** 系统 MUST 继续执行 schema、白名单和归属三层校验
- **THEN** 未通过校验的引用 MUST 被丢弃或降级为待证明，且不得污染图谱

#### Scenario: 本轮完成后刷新图谱与建议
- **WHEN** 完整一轮多角色对话生成成功
- **THEN** 系统 MUST 按既有机制发布图谱事件
- **THEN** 系统 MUST 刷新补证建议
- **THEN** 前端 MUST 能在流式完成后刷新图谱和建议区域

### Requirement: 智能小法庭 SHALL 仅提供流式庭审生成路径
系统 SHALL 仅通过 SSE 提供完整庭审轮次生成能力，MUST NOT 新增非流式完整轮次生成接口；当流式生成不可用时 MUST 使用统一拒绝信息作为 fallback。

#### Scenario: 不提供非流式完整轮次生成接口
- **WHEN** 实现智能小法庭大模型庭审对话能力
- **THEN** 系统 MUST NOT 新增用于完整生成一轮多角色对话的非流式 API
- **THEN** 前端开庭交互 MUST 以 SSE 流式接口为唯一生成入口

#### Scenario: 流式初始化失败
- **WHEN** SSE 流式连接初始化失败或服务端无法创建庭审生成任务
- **THEN** 系统 MUST 返回或推送 `服务繁忙，请稍后重试`
- **THEN** 系统 MUST 记录结构化错误日志，且不得泄露完整 prompt 或证据原文

#### Scenario: 模型调用失败
- **WHEN** 任一角色模型调用超时、异常或返回不可处理结果导致本轮无法继续
- **THEN** 系统 MUST 终止本轮剩余角色调用
- **THEN** 前端 MUST 展示 `服务繁忙，请稍后重试`
- **THEN** 系统 MUST 按轮次状态机记录失败原因

#### Scenario: SSE 客户端断开
- **WHEN** 前端在庭审流式生成过程中断开连接或点击停止
- **THEN** 系统 MUST 按现有 SSE 规范处理断开
- **THEN** 系统 MUST NOT 触发 Spring MVC error dispatch 写入额外 JSON 错误响应
