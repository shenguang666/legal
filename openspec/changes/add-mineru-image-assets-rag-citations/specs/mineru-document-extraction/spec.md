## MODIFIED Requirements

### Requirement: 异步轮询 MinerU 批次结果
系统 SHALL 异步轮询 MinerU 解析结果，直到完成、失败或达到配置超时时间；当解析成功后，系统 MUST 下载 MinerU 结果包，处理其中的 Markdown 和图片资产，将图片引用替换为可访问、可检索的 Markdown 内容后，再交给文档清洗管线和切片流水线。

#### Scenario: MinerU 解析成功完成
- **WHEN** MinerU 返回文档解析状态为完成
- **THEN** 系统 MUST 下载结果包或 Markdown 输出
- **THEN** 系统 MUST 抽取结果包中的 Markdown 和被 Markdown 引用的图片资源
- **THEN** 系统 MUST 对可处理图片执行 OSS 上传和图片描述替换
- **THEN** 系统 MUST 对替换后的 Markdown 输出执行适用的文档清洗策略
- **THEN** 系统 MUST 将清洗后的解析内容提供给切片流水线

#### Scenario: MinerU 图片增强失败但文本可用
- **WHEN** MinerU 结果包中的图片上传、描述生成或引用替换失败但 Markdown 文本可读取
- **THEN** 系统 MUST 记录图片增强失败原因
- **THEN** 系统 MUST 降级使用可用 Markdown 文本继续清洗和切片

#### Scenario: MinerU 解析超时
- **WHEN** MinerU 在配置超时时间前未完成解析
- **THEN** 系统 MUST 按重试策略将解析任务标记为失败或可重试
- **THEN** 系统 MUST 记录超时原因
