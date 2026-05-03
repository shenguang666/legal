## MODIFIED Requirements

### Requirement: 异步轮询 MinerU 批次结果
系统 SHALL 异步轮询 MinerU 解析结果，直到完成、失败或达到配置超时时间；当解析成功后，系统 MUST 将 MinerU 输出交给文档清洗管线，再提供给切片流水线。

#### Scenario: MinerU 解析成功完成
- **WHEN** MinerU 返回文档解析状态为完成
- **THEN** 系统 MUST 下载结果包或 Markdown 输出
- **THEN** 系统 MUST 对 Markdown 输出执行适用的文档清洗策略
- **THEN** 系统 MUST 将清洗后的解析内容提供给切片流水线

#### Scenario: MinerU 解析超时
- **WHEN** MinerU 在配置超时时间前未完成解析
- **THEN** 系统 MUST 按重试策略将解析任务标记为失败或可重试
- **THEN** 系统 MUST 记录超时原因
