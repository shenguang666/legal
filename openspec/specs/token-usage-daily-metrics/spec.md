## Requirements

### Requirement: Token usage source data SHALL use persisted chat messages
系统 SHALL 以已落库的 `chat_message.token_usage` 作为 Token 消耗统计来源，并通过 `chat_session` 关联租户和会话所属用户。

#### Scenario: Aggregate token usage by tenant and date
- **WHEN** Token 指标任务统计某个租户和指标日期
- **THEN** 系统 SHALL 读取该租户会话在该自然日内已落库消息的 Token 消耗
- **THEN** 系统 SHALL 使用消息创建时间所属自然日作为指标日期

#### Scenario: Ignore missing or null token usage safely
- **WHEN** 某条消息的 Token 消耗为空或未记录
- **THEN** 系统 SHALL 将其按 0 Token 处理，且不得导致统计任务失败

### Requirement: Daily summary records SHALL be maintained per tenant and metric date
系统 SHALL 为每个租户、指标日期和统计版本维护一条 Token 消耗日汇总记录。

#### Scenario: Create daily summary for metric date
- **WHEN** 统计任务处理某个租户和指标日期且当前统计版本没有日汇总记录
- **THEN** 系统 SHALL 创建日汇总记录，保存租户 ID、指标日期、统计版本、总 Token、消息数、活跃用户数、平均每消息 Token、Top 用户数量、状态和时间字段

#### Scenario: Reuse existing daily summary
- **WHEN** 统计任务处理某个租户和指标日期且当前统计版本已有日汇总记录
- **THEN** 系统 SHALL 复用该日汇总记录并按幂等口径更新统计结果，不得创建重复日汇总

### Requirement: Daily top users SHALL be ranked by token usage
系统 SHALL 保存每个日汇总下 Token 消耗最高的用户排行，排行数量由 YAML 配置控制。

#### Scenario: Persist configured top users
- **WHEN** 某日 Token 消耗统计完成
- **THEN** 系统 SHALL 按用户聚合 Token 消耗和消息数
- **THEN** 系统 SHALL 按 Token 消耗降序、消息数降序、用户 ID 升序生成稳定排名
- **THEN** 系统 SHALL 仅保存配置数量内的 Top 用户明细

#### Scenario: Rebuild top users on repeated calculation
- **WHEN** 统计任务重新计算某个日汇总
- **THEN** 系统 SHALL 删除或覆盖该日汇总旧的 Top 用户明细
- **THEN** 系统 SHALL 写入本次计算得到的最新 Top 用户排行

### Requirement: Token usage metric configuration SHALL control worker and ranking behavior
系统 SHALL 提供 `token.metric.usage` YAML 配置，用于控制指标开关、定时任务、统计日期、批大小、重试策略和 Top 用户数量。

#### Scenario: Default low-peak schedule is configured
- **WHEN** 使用默认配置
- **THEN** Token 指标定时任务 SHALL 使用低峰 Cron 配置运行
- **THEN** 默认指标日期 SHALL 为服务器当前日期之前的自然日

#### Scenario: Top user limit is configurable
- **WHEN** 管理员调整 YAML 中的 Top 用户数量配置
- **THEN** 后续统计任务 SHALL 按新的配置数量保存每日 Top 用户明细

### Requirement: Scheduled token usage job SHALL process historical metric dates
系统 SHALL 通过定时任务按批统计 Token 消耗，并优先处理配置的指标日期。

#### Scenario: Scheduled run processes configured metric date
- **WHEN** Token 指标 Worker 在启用状态下触发
- **THEN** 系统 SHALL 处理配置日期偏移对应的指标日期
- **THEN** 系统 SHALL 记录本轮处理的日期、租户数、成功数、失败数和跳过数

#### Scenario: Scheduled run prevents local re-entry
- **WHEN** 上一轮 Token 指标任务仍在运行
- **THEN** 新触发的同实例任务 SHALL 跳过执行并记录安全日志

### Requirement: Manual token usage evaluation SHALL only allow past dates
系统 SHALL 允许管理员手动触发 Token 消耗统计，但手动统计的结束日期必须早于服务器当前自然日。

#### Scenario: Reject manual run ending today
- **WHEN** 管理员手动触发 Token 指标统计且结束日期大于或等于服务器当前自然日
- **THEN** 系统 SHALL 拒绝请求并返回明确错误，提示只能统计今天之前的历史日期

#### Scenario: Accept manual historical date range
- **WHEN** 管理员手动触发今天之前的历史日期范围统计
- **THEN** 系统 SHALL 按日期范围逐日执行幂等统计
- **THEN** 系统 SHALL 返回本次选中、成功、失败、跳过和已处理日期数量

### Requirement: Completed dates SHALL be skipped idempotently
系统 SHALL 在手动或定时统计前检查目标日期是否已有当前统计版本的完成记录，并避免无意义重复统计。

#### Scenario: Skip already completed date
- **WHEN** 目标租户和指标日期已有当前统计版本的完成日汇总记录
- **THEN** 系统 SHALL 返回已有记录、无需统计的结果
- **THEN** 系统 SHALL 不重复写入新的日汇总或 Top 用户记录

#### Scenario: Recalculate unfinished date
- **WHEN** 目标租户和指标日期存在处理中、失败或部分失败的日汇总记录
- **THEN** 系统 SHALL 允许任务重新计算该日期并刷新汇总状态

### Requirement: Token usage APIs SHALL expose summaries, trends, and top users
系统 SHALL 提供管理员 API 查询 Token 消耗统计结果和手动运行统计任务。

#### Scenario: Query date range summary
- **WHEN** 管理员按日期范围查询 Token 指标
- **THEN** API SHALL 返回总 Token、总消息数、活跃用户数、平均每消息 Token、统计版本、趋势点和汇总状态

#### Scenario: Query daily summaries with pagination
- **WHEN** 管理员查询日期范围内的日汇总列表
- **THEN** API SHALL 返回分页后的日汇总记录，包含指标日期、总 Token、消息数、活跃用户数、Top 用户数量、状态和统计时间

#### Scenario: Query top users for one daily summary
- **WHEN** 管理员打开某个日汇总的 Top 用户明细
- **THEN** API SHALL 返回该日汇总下按排名排序的用户 Token 消耗明细

### Requirement: Token usage dashboard SHALL visualize daily cost metrics
前端 SHALL 提供 Token 消耗指标看板，展示日期范围统计、趋势图、日汇总和每日 Top 用户。

#### Scenario: Render daily token trend chart
- **WHEN** 前端收到 Token 指标趋势点
- **THEN** 页面 SHALL 使用趋势图展示每日 Token 消耗和消息数变化

#### Scenario: Select daily summary to view top users
- **WHEN** 管理员在页面选择某条日汇总记录
- **THEN** 页面 SHALL 加载并展示该日期 Token 消耗最多的用户排行

#### Scenario: Trigger manual historical calculation
- **WHEN** 管理员在页面选择今天之前的日期范围并点击手动统计
- **THEN** 页面 SHALL 调用手动统计 API 并展示本次运行结果
