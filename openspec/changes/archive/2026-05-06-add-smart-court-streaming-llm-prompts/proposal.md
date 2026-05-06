## Why

当前智能小法庭已经具备案件、轮次、Agent 校验、图谱和补证建议能力，但前端开庭交互尚未真正形成可感知的多角色大模型庭审对话；同时 AI 法官、对方代理人、用户辅助律师等系统提示词仍写在代码中，不利于审计、调优和角色边界管理。

本变更用于补齐“开庭即流式生成完整一轮多角色对话”的用户体验，并把每个 AI 对话对象的系统提示词迁移到 `src/main/resources/prompts`，统一采用项目已有提示词模板风格维护。

## What Changes

- 新增智能小法庭专用 LLM 配置：优先使用 `LEGAL_SMART_COURT_LLM_API_KEY`，未配置时默认复用在线问答模型 `LEGAL_LLM_API_KEY`。
- 开庭采用方案 A：一次点击自动按 `对方代理人 -> 用户辅助律师 -> 法官` 顺序生成完整一轮庭审对话。
- 仅提供流式庭审接口；模型、SSE 或服务不可用时向前端返回统一拒绝信息：`服务繁忙，请稍后重试`。
- 每个 AI 对话对象必须拥有独立系统提示词文件，放在 `src/main/resources/prompts`，格式参考现有 `legal-rag-system.txt`。
- 将 `CourtRoleAgentService` 中内联 system prompt 改为从资源提示词模板加载，保留证据白名单、JSON 输出、法官中立和双向不利点要求。
- 前端点击 `开庭` 时消费 SSE 事件，按角色增量展示庭审发言，并展示忙碌、失败和完成状态。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `smart-court-simulation`：智能小法庭开庭必须通过流式大模型多角色对话完成，并要求每个 AI 对话对象使用资源目录下独立系统提示词模板。

## Impact

- 影响后端配置：`application.yaml`、智能小法庭配置属性、LLM Bean 装配。
- 影响后端服务：`CourtRoleAgentService`、庭审编排器、SSE Controller、token 计量、日志与异常处理。
- 影响资源目录：新增 `src/main/resources/prompts/court-*.txt` 角色系统提示词模板。
- 影响前端：`SmartCourtView.vue` 和 `web/src/api/court.ts/js` 的开庭流式交互。
- 影响测试：新增提示词加载、Key 回退、流式事件、多角色对话保存和异常 fallback 测试。
