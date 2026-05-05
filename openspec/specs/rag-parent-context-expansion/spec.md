## Requirements

### Requirement: RAG 检索命中子分块后使用父分块构建上下文
系统 SHALL 在 RAG 检索阶段区分 Elasticsearch 原始命中切片和最终进入大模型上下文的切片；当原始命中为子分块时，系统 MUST 批量加载其父分块，并使用父分块替代子分块进入大模型上下文。

#### Scenario: 子分块命中后批量加载父分块
- **WHEN** Elasticsearch 检索结果包含一个或多个带有 `parent_chunk_id` 的子分块
- **THEN** 系统 MUST 使用去重集合收集这些父分块 ID
- **THEN** 系统 MUST 按租户批量查询对应父分块
- **THEN** 系统 MUST 使用父分块内容构建大模型上下文，而不是使用子分块内容

#### Scenario: 多个子分块指向同一父分块
- **WHEN** Elasticsearch 原始命中列表中多个子分块指向同一个父分块
- **THEN** 系统 MUST 只将该父分块加入最终上下文一次
- **THEN** 系统 MUST 按该父分块首次被子分块命中的顺序保留上下文排序

#### Scenario: 普通分块命中
- **WHEN** Elasticsearch 检索结果包含普通分块
- **THEN** 系统 MUST 直接将该普通分块作为最终上下文候选
- **THEN** 系统 MUST 不要求该普通分块存在父分块 ID

#### Scenario: 父分块缺失
- **WHEN** 子分块携带的 `parent_chunk_id` 无法在当前租户下查询到父分块
- **THEN** 系统 MUST 不将该子分块内容回退发送给大模型
- **THEN** 系统 MUST 跳过该异常父分块并记录可排查日志

### Requirement: 同时保留原始命中和最终上下文命中
系统 SHALL 在 RAG 回答、缓存和检索日志中保留最终进入大模型上下文的切片 ID，并在新增字段中记录 Elasticsearch 原始命中的切片 ID。

#### Scenario: 写入非流式 RAG 检索日志
- **WHEN** 非流式 RAG 回答完成并写入 `retrieval_log`
- **THEN** `hit_chunk_ids` MUST 记录最终进入大模型上下文的普通分块或父分块 ID
- **THEN** `raw_hit_chunk_ids` MUST 记录 Elasticsearch 原始命中的普通分块或子分块 ID

#### Scenario: 写入流式 RAG 检索日志
- **WHEN** 流式 RAG 回答完成并写入 `retrieval_log`
- **THEN** `hit_chunk_ids` MUST 记录最终进入大模型上下文的普通分块或父分块 ID
- **THEN** `raw_hit_chunk_ids` MUST 记录 Elasticsearch 原始命中的普通分块或子分块 ID

#### Scenario: 写入回答缓存
- **WHEN** 系统将 RAG 回答写入回答缓存
- **THEN** 缓存中的命中切片 ID MUST 使用最终进入大模型上下文的普通分块或父分块 ID
- **THEN** 系统 MUST 保持缓存命中后的回答复盘语义与原始回答一致

### Requirement: 引用证据使用最终上下文分块
系统 SHALL 基于最终进入大模型上下文的分块构建回答引用，避免向用户展示未发送给大模型的子分块作为主要上下文证据。

#### Scenario: 子分块被父分块替换后生成引用
- **WHEN** 原始命中子分块被父分块替换为最终上下文
- **THEN** 系统 MUST 使用父分块的 `chunk_id`、文档 ID、来源和内容摘要生成引用
- **THEN** 系统 MUST 按父分块 ID 查询关联图片证据

#### Scenario: 普通分块直接生成引用
- **WHEN** 普通分块进入最终上下文
- **THEN** 系统 MUST 使用该普通分块生成引用
- **THEN** 系统 MUST 保持现有引用内容和图片证据查询行为
