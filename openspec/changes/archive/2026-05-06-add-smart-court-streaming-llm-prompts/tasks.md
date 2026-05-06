## 1. 小法庭 LLM 配置

- [x] 1.1 在 `application.yaml` 新增 `legal.smart-court.llm.*` 配置项，并为每一项添加中文注释
- [x] 1.2 扩展 `SmartCourtProperties`，新增 LLM 配置字段，并为每个字段添加中文注释
- [x] 1.3 新增或调整小法庭专用 `ChatModel` Bean，优先使用 `LEGAL_SMART_COURT_LLM_API_KEY`，为空时复用 `LEGAL_LLM_API_KEY`
- [x] 1.4 增加配置单元测试，覆盖专用 Key 优先、默认复用在线问答 Key、两类 Key 都不可用三种场景

## 2. 资源目录系统提示词

- [x] 2.1 在 `src/main/resources/prompts` 新增 `court-opponent-agent-system.txt`，格式参考 `legal-rag-system.txt`
- [x] 2.2 在 `src/main/resources/prompts` 新增 `court-user-advisor-agent-system.txt`，格式参考 `legal-rag-system.txt`
- [x] 2.3 在 `src/main/resources/prompts` 新增 `court-judge-agent-system.txt`，格式参考 `legal-rag-system.txt`
- [x] 2.4 新增小法庭提示词模板加载服务，支持按角色加载、变量渲染和空值安全默认值
- [x] 2.5 为提示词模板加载与渲染增加单元测试，覆盖模板存在性、变量替换和空变量处理

## 3. 多角色 Agent 改造

- [x] 3.1 改造 `CourtRoleAgentService`，移除内联 system prompt，改为使用资源提示词模板
- [x] 3.2 确保对方代理人、用户辅助律师、法官分别使用独立模板和独立上下文
- [x] 3.3 保留证据引用白名单注入、JSON 输出约束、法官中立和双向不利点校验
- [x] 3.4 为 `CourtRoleAgentService` 增加角色模板调用测试和模型不可用 fallback 测试

## 4. 流式庭审接口与轮次编排

- [x] 4.1 新增或调整智能小法庭 SSE 开庭接口，作为完整一轮多角色对话的唯一生成入口
- [x] 4.2 实现方案 A：一次开庭按 `OPPONENT -> USER_ADVISOR -> JUDGE` 顺序自动生成完整一轮
- [x] 4.3 每个角色输出通过 SSE 推送角色开始、增量内容、角色完成和轮次完成事件
- [x] 4.4 每个角色完成后保存庭审消息、论点、模型名称、角色、轮次、尝试编号和 token 消耗
- [x] 4.5 继续执行证据引用三层校验、token 预算护栏、图谱事件发布和补证建议刷新
- [x] 4.6 模型、SSE 或服务不可用时返回或推送 `服务繁忙，请稍后重试`，并按状态机记录失败原因
- [x] 4.7 保持现有 SSE 异常处理规范，客户端断开或用户停止时不得触发 Spring MVC error dispatch

## 5. 前端流式交互

- [x] 5.1 更新 `web/src/api/court.ts` 和 `web/src/api/court.js`，封装小法庭流式开庭接口
- [x] 5.2 更新 `SmartCourtView.vue`，点击开庭后消费 SSE 并按角色展示庭审对话
- [x] 5.3 前端展示庭审生成中、角色发言中、完成、停止和 `服务繁忙，请稍后重试` 状态
- [x] 5.4 流式完成后自动刷新案件状态、图谱和补证建议
- [x] 5.5 确保 `web/src/router/index.ts` 与 `web/src/router/index.js` 如有改动保持同步

## 6. 验证与验收

- [x] 6.1 运行后端定向测试：小法庭配置、提示词模板、Agent、SSE 编排相关测试全部通过
- [x] 6.2 运行 `mvn -DskipTests compile`，确保后端编译通过
- [x] 6.3 运行 `mvn test`，确保全量后端测试通过
- [x] 6.4 运行 `npm run build`，确保前端构建通过
- [x] 6.5 运行 `openspec validate add-smart-court-streaming-llm-prompts --strict`，确保变更规格通过校验
