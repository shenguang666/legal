## Requirements

### Requirement: 上传文档时选择解析方式
系统 SHALL 允许授权用户在导入知识库文档、天眼审查文档和风险规则文档时选择文档解析方式。

#### Scenario: 用户使用原生解析导入文档
- **WHEN** 授权用户上传受支持文档并选择原生解析
- **THEN** 系统 MUST 通过现有本地抽取链路处理文档
- **THEN** 导入后的文档 MUST 按其业务类型继续支持索引、审查或规则检索

#### Scenario: 用户使用 MinerU 精准解析导入文档
- **WHEN** 授权用户上传受支持文档并选择 MinerU 精准解析
- **THEN** 系统 MUST 记录文档选择的解析方式
- **THEN** 系统 MUST 启动 MinerU 精准解析，且不得向前端暴露 MinerU API Token

### Requirement: 未提供解析方式时保留现有上传行为
系统 SHALL 对未提交显式解析方式参数的客户端保留现有文档导入行为。

#### Scenario: 旧客户端上传时不传解析方式
- **WHEN** 客户端上传有效文档但未传入解析方式参数
- **THEN** 系统 MUST 使用配置的默认解析方式
- **THEN** 上传不得仅因缺少解析方式参数而失败

### Requirement: 强制执行可配置的最大上传文档数量
系统 SHALL 使用后端 YAML 配置强制限制单次上传请求允许的最大文档数量。

#### Scenario: 上传数量未超过配置限制
- **WHEN** 用户上传的文档数量小于或等于配置的最大值
- **THEN** 系统 MUST 接受请求并继续执行文件校验和解析

#### Scenario: 上传数量超过配置限制
- **WHEN** 用户上传的文档数量超过配置的最大值
- **THEN** 系统 MUST 在解析开始前拒绝请求
- **THEN** 响应 MUST 说明当前配置允许的最大文档数量

### Requirement: 向用户展示解析能力和解析状态
系统 SHALL 提供足够信息，供前端展示可用解析方式、上传限制、解析状态和失败原因。

#### Scenario: MinerU 禁用或未配置
- **WHEN** MinerU 精准解析被禁用，或后端缺少有效 MinerU Token
- **THEN** 系统 MUST 不向用户展示 MinerU 精准解析为可用方式
- **THEN** 如果用户具备上传权限，原生解析 MUST 仍然可用

#### Scenario: 文档创建后解析失败
- **WHEN** 文档记录创建后解析失败
- **THEN** 系统 MUST 将文档或解析任务标记为失败
- **THEN** 前端 MUST 能展示用户可读的失败原因
