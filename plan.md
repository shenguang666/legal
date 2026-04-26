# 法律知识问答小助手实施计划（修订版）

## 1. 项目目标与范围

构建一个面向法律知识场景的问答小助手，采用 RAG（检索增强生成）方案。

技术栈：

- 后端：Spring Boot + LangChain4j
- 数据存储：MySQL + MyBatis + MyBatis-Plus
- 缓存与会话上下文：Redis
- 向量与全文检索：Elasticsearch
- 前端：Vue 3 + TypeScript

核心目标：

- 支持多轮法律知识问答。
- 系统基于知识库检索给出答案，并附带可追溯依据片段。
- 支持文档导入、切片、索引、更新、删除。
- 支持会话管理、反馈闭环、运营监控。

非目标（首期不做）：

- 自动生成法律文书。
- 复杂案件工作流自动编排。
- 代替律师专业意见（系统仅提供知识参考）。

## 2. 当前项目基线与差距

当前仓库状态：

- 已有 Spring Boot 骨架项目（Java 17）。
- 依赖极简，仅含基础 starter 与测试依赖。
- 未接入 MySQL/Redis/Elasticsearch/LLM，也未搭建前端工程。

当前差距结论：

- 需同时补齐工程化基础能力与业务能力。
- 安全、可观测性、一致性必须前置到阶段 0，而不是后置到上线前。

## 3. 总体架构设计

### 3.1 架构分层

1. 前端层：Vue 应用。
2. 接入层：REST API、鉴权、限流、输入校验。
3. 应用层：问答编排、会话管理、权限校验、审计。
4. 检索层：向量召回 + BM25 召回 + 融合重排。
5. 数据层：MySQL（业务数据）+ Redis（缓存与上下文）。
6. 模型层：LangChain4j 对接 LLM 与 Embedding。

### 3.2 核心流程（RAG）

1. 用户请求到达 API（携带 traceId 或由系统生成）。
2. 鉴权并校验数据归属（tenant_id + owner_user_id）。
3. 问题预处理（清洗、改写、关键词提取）。
4. 执行 ES 向量检索与 BM25 检索。
5. 对召回结果融合重排（RRF + 可选 rerank）。
6. 构建 Prompt（问题 + 上下文片段 + 输出约束 + 免责声明）。
7. 调用 LLM 生成答案。
8. 返回答案、引用来源、置信度、traceId。
9. 记录消息、检索日志、用户反馈。

### 3.3 必须落地的设计决策

1. 认证与授权前置
   - 首期也必须具备统一身份标识，不允许“无鉴权直接查会话/文档”。
   - 所有读取接口按 tenant_id + owner_user_id 过滤。
   - 数据不存在与无权限统一返回 404，降低资源枚举风险。

2. 文档索引一致性采用 Outbox
   - 文档变更与索引事件在一个数据库事务内提交。
   - 异步索引消费者按 document_id + doc_version 幂等处理。

3. 幂等与防重放
   - 写接口支持 requestId（或幂等键）与有效期控制。
   - Redis 记录短期请求指纹，避免重复写入。

4. 可观测性前置
   - 强制 traceId 打通日志、检索日志、模型调用日志。
   - 暴露延迟、错误率、召回质量、token 成本指标。

5. 降级与熔断
   - 检索超时时执行降级策略：缩短上下文、跳过 rerank 或返回“无法确认”提示。
   - 模型调用失败时提供兜底文案，避免空响应。

## 4. 模块拆分规划

建议后端包结构：

- com.legal.chat：问答接口、会话、Prompt 编排。
- com.legal.knowledge：文档管理、切片、索引任务。
- com.legal.retrieval：召回、融合、重排。
- com.legal.llm：模型适配、调用策略、降级逻辑。
- com.legal.security：鉴权、权限、限流、防重放。
- com.legal.observability：traceId、日志规范、指标埋点。
- com.legal.common：统一响应、异常、工具类。
- com.legal.config：MySQL/Redis/ES/LLM 配置。

建议新增前端目录：

- web/：Vue 独立工程。

## 5. 数据模型规划

### 5.1 MySQL 表（修订）

1. kb_document
   - document_id, tenant_id, owner_user_id, title, source, status, doc_version, index_status, created_at, updated_at。
2. kb_chunk
   - chunk_id, tenant_id, document_id, doc_version, chunk_order, content, content_hash, created_at。
3. kb_index_outbox
   - id, tenant_id, document_id, doc_version, op(UPSERT/DELETE), status(PENDING/SENT/FAILED), retry_count, next_retry_at。
4. chat_session
   - session_id, tenant_id, owner_user_id, title, created_at, last_active_at。
5. chat_message
   - message_id, session_id, role, content, token_usage, latency_ms, trace_id, created_at。
6. qa_feedback
   - feedback_id, message_id, helpful, comment, created_at。
7. retrieval_log
   - id, trace_id, tenant_id, query_text, hit_chunk_ids, rerank_score, model_name, latency_ms, created_at。

关键约束（必须）：

- chat_session 建索引：idx_session_tenant_owner(tenant_id, owner_user_id, last_active_at)。
- kb_document 建索引：idx_doc_tenant_owner_status(tenant_id, owner_user_id, status)。
- kb_index_outbox 唯一索引：uk_doc_version_op(document_id, doc_version, op)。

### 5.2 Elasticsearch 索引（修订）

索引名建议：legal_kb_chunks_v1。

字段建议：

- chunk_id（keyword）
- tenant_id（keyword）
- document_id（keyword）
- doc_version（integer）
- content（text，中文分词器）
- content_vector（dense_vector，需固定维度）
- tags（keyword）
- updated_at（date）

检索策略（默认参数）：

- 向量召回 topK = 80。
- BM25 召回 topK = 80。
- RRF 融合（k = 60）。
- 可选 rerank topN = 20。
- 所有查询都按 tenant_id 过滤。

### 5.3 Redis Key 规划（修订）

- chat:context:{sessionId}：最近 N 轮上下文（建议 TTL 24h）。
- chat:rate_limit:{userId}:{minute}：分钟级限流计数。
- kb:doc:lock:{documentId}：文档处理分布式锁。
- api:idempotency:{userId}:{requestId}：接口幂等键（建议 TTL 10m）。
- qa:hot:{date}：热点问题统计。

## 6. 接口设计（MVP）

通用约束：

- 全部接口要求认证身份。
- 全部读写按 tenant_id + owner_user_id 做数据归属校验。
- 所有写接口支持 requestId 幂等键。

### 6.1 问答接口

1. POST /api/chat/ask
   - 入参：sessionId, question, requestId。
   - 出参：answer, citations, traceId, confidence, warning。
   - 规则：若 session 不属于当前用户，返回 404。

2. POST /api/chat/session
   - 创建会话。
   - 出参：sessionId, title, createdAt。

3. GET /api/chat/session/{sessionId}/messages
   - 查询历史消息。
   - 规则：只允许读取本人会话。

### 6.2 知识库接口

1. POST /api/knowledge/documents
   - 上传文档（pdf/docx/md/txt）。
   - 限制：大小、格式、魔数校验。

2. POST /api/knowledge/documents/{id}/index
   - 触发切片与索引。
   - 规则：写入 outbox，不直接同步阻塞建立索引。

3. GET /api/knowledge/documents
   - 文档列表与状态（仅本租户可见）。

4. DELETE /api/knowledge/documents/{id}
   - 逻辑删除文档并投递删除索引事件。

### 6.3 反馈接口

1. POST /api/feedback
   - 对答案进行有用/无用反馈。
   - 入参：messageId, helpful, comment, requestId。

## 7. 前端功能规划（Vue）

页面与模块：

1. 登录页（MVP 保留最小身份体系，不跳过）。
2. 问答页（核心）
   - 输入框、消息流、引用来源展示。
   - 支持追问与多轮会话。
3. 会话列表页。
4. 知识库管理页。
5. 反馈与运营视图（可后置到 V2）。

交互重点：

- 支持流式输出。
- 每条答案显示引用片段、来源文档、置信提示。
- 输出必须显示“法律免责声明”。

## 8. 分阶段里程碑（修订）

### 阶段 0：基础工程与安全基线（2-3 天）

- 引入依赖：Web、Validation、MyBatis、MySQL、Redis、ES、LangChain4j。
- 配置分环境（dev/test/prod）与密钥外置。
- 搭建 docker-compose（mysql/redis/elasticsearch）。
- 落地认证骨架、权限拦截、限流、requestId 幂等中间件。
- 落地 traceId 透传、结构化日志、基础监控埋点。

阶段验收（DoD）：

- 未登录请求无法访问业务接口。
- 关键接口有 traceId 与统一错误码。
- 本地环境可一键启动并通过 smoke test。

### 阶段 1：知识库入库链路（4-6 天）

- 文档上传与元数据入库。
- 文档切片（长度、重叠、清洗策略）。
- Embedding 入 ES。
- 文档状态机 + outbox 事件驱动索引。

阶段验收（DoD）：

- 同一文档重复触发索引不产生脏数据。
- 文档删除后检索结果不再命中旧片段。

### 阶段 2：问答主链路（5-7 天）

- 混合检索 + Prompt 编排 + 答案生成。
- Redis 会话上下文管理。
- 降级策略（检索超时、模型失败、低置信提示）。
- 返回可追溯引用与免责声明。

阶段验收（DoD）：

- 关键问答场景可稳定返回答案和引用。
- 失败场景可回退，不出现空白响应。

### 阶段 3：前端联调与体验优化（4-6 天）

- 聊天页面、会话管理、来源展示。
- 流式响应、错误提示、重试体验。
- 反馈闭环（有用/无用）。

阶段验收（DoD）：

- 端到端链路可演示。
- 前后端日志可按 traceId 串联排障。

### 阶段 4：稳定性与上线准备（4-6 天）

- 压测与容量评估。
- 告警阈值配置与故障演练。
- 安全测试（越权、注入、提示词攻击）。
- 发布脚本与部署文档。

阶段验收（DoD）：

- 达成 SLO，且高危安全问题为 0。
- 发布回滚路径明确并演练通过。

## 9. 质量与验收标准（量化）

### 9.1 功能验收

- 提问可返回结构化答案与引用来源。
- 文档上传后 95% 在 5 分钟内完成索引并参与检索。
- 会话历史可查询、可追问、可审计。

### 9.2 性能与容量目标

首期容量假设：

- 峰值并发在线会话：80。
- 峰值问答 QPS：30。
- 平均每问上下文窗口：8 个片段。

SLO：

- 问答接口 P95 <= 5s（常规上下文）。
- 检索链路 P95 <= 1.5s。
- 服务可用性 >= 99.5%。
- 日志与指标上报成功率 >= 99%。

### 9.3 安全与合规

- 必须通过对象级权限校验测试（会话/文档越权）。
- 上传文件必须校验格式、大小、魔数。
- 输出统一附法律免责声明。
- 审计日志可追溯到用户、会话、traceId。

### 9.4 可运维性

- 提供 API 延迟、错误率、模型调用成本、召回命中率看板。
- 关键告警（接口错误率、模型失败率、索引积压）可触达值班。

## 10. 风险与应对

1. 法律知识准确性风险
   - 应对：强制引用来源 + 低置信提示 + 人工抽检。

2. 向量检索效果不稳定
   - 应对：混合检索 + 重排；维护评测集持续评估召回率。

3. LLM 成本风险
   - 应对：热点缓存、上下文裁剪、模型分级路由。

4. 数据安全风险
   - 应对：传输加密、最小权限、脱敏、审计、定期权限巡检。

5. 索引一致性风险
   - 应对：outbox + 幂等消费 + 失败重试 + 死信补偿。

## 11. 开发与运行建议命令

后端（Windows）：

- ./mvnw.cmd clean test
- ./mvnw.cmd spring-boot:run

前端：

- npm install
- npm run dev

## 12. 启动前待确认事项（阶段 0 结束前冻结）

1. LLM 与 Embedding 供应商，采用方案A
   - 方案 A：同厂商一体化。
   - 方案 B：主备双供应商。
   - 方案 C：网关路由多模型。

2. 法律知识范围，采用方案C
   - 方案 A：劳动法优先。
   - 方案 B：合同法 + 民法典并行。
   - 方案 C：按业务线分批上线。

3. 文档来源与更新频率，采用方案C
   - 方案 A：手工上传。
   - 方案 B：定时同步。
   - 方案 C：手工 + 定时混合。

4. 账号体系与权限分级，采用方案B
   - 方案 A：单角色 MVP。
   - 方案 B：RBAC（管理员/运营/普通用户）。
   - 方案 C：RBAC + 租户隔离。

5. 答案审核流，采用方案C
   - 方案 A：先不上审核，低置信强提示。
   - 方案 B：高风险问题人工复核。
   - 方案 C：全量抽检 + 反馈闭环。

## 13. 上线门禁（Go/No-Go）

- 高危安全问题为 0（越权、注入、敏感信息泄露）。
- 索引一致性场景全部通过回归测试。
- 关键 SLO 连续 7 天达标。
- 故障回滚流程演练通过。

以上计划按 MVP 到可上线版本设计，可在需求收敛后拆解为双周迭代计划（Sprint Backlog）。