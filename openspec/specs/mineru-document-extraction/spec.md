## Requirements

### Requirement: 提交本地文件到 MinerU 精准解析
系统 SHALL 支持由后端将上传的本地文档提交到 MinerU v4 Precision Extract API。

#### Scenario: MinerU 上传地址创建成功
- **WHEN** 文档被选择使用 MinerU 精准解析
- **THEN** 后端 MUST 使用配置的认证信息和解析参数请求 MinerU 批量上传地址
- **THEN** 后端 MUST 将文件字节上传到 MinerU 返回的签名上传地址

#### Scenario: MinerU 上传地址创建失败
- **WHEN** MinerU 拒绝上传地址创建请求或返回错误码
- **THEN** 系统 MUST 记录解析失败
- **THEN** 系统 MUST 暴露适合用户排查的失败原因

### Requirement: 异步轮询 MinerU 批次结果
系统 SHALL 异步轮询 MinerU 解析结果，直到完成、失败或达到配置超时时间。

#### Scenario: MinerU 解析成功完成
- **WHEN** MinerU 返回文档解析状态为完成
- **THEN** 系统 MUST 下载结果包或 Markdown 输出
- **THEN** 系统 MUST 将解析内容提供给切片流水线

#### Scenario: MinerU 解析超时
- **WHEN** MinerU 在配置超时时间前未完成解析
- **THEN** 系统 MUST 按重试策略将解析任务标记为失败或可重试
- **THEN** 系统 MUST 记录超时原因

### Requirement: 保护 MinerU 凭证
系统 SHALL 仅在后端配置和服务端请求中使用 MinerU API 凭证。

#### Scenario: 前端请求解析能力
- **WHEN** 前端请求可用文档解析方式或上传配置
- **THEN** 系统 MUST 返回能力标记和限制信息
- **THEN** 系统 MUST 不返回 MinerU API Token 或仅供后端使用的签名上传地址

### Requirement: 支持可重试的 MinerU 解析失败
系统 SHALL 保留足够的任务元数据，用于在瞬时失败时重试 MinerU 解析。

#### Scenario: 用户重试失败的 MinerU 解析任务
- **WHEN** 文档解析任务因可重试的 MinerU 或网络错误失败
- **THEN** 授权用户 MUST 能对同一文档版本重新触发解析
- **THEN** 系统 MUST 记录新的尝试且不丢失前一次失败原因
