## Requirements

### Requirement: 抽取 MinerU 结果包中的图片资产
系统 SHALL 在 MinerU 精准解析成功后，从结果 zip 中抽取 Markdown 和被 Markdown 引用的图片资源。

#### Scenario: 结果包包含 Markdown 和图片
- **WHEN** MinerU 返回的结果 zip 中包含 `full.md` 和 `images/` 目录图片
- **THEN** 系统 MUST 读取 `full.md` 作为主 Markdown 内容
- **THEN** 系统 MUST 收集 Markdown 中引用的本地图片资源及其 zip 内相对路径

#### Scenario: 图片资源异常或不受支持
- **WHEN** zip 中图片路径非法、格式不受支持、单图过大或超过配置数量限制
- **THEN** 系统 MUST 跳过该图片或将其标记为失败
- **THEN** 系统 MUST 不因单张图片失败而中断整个 MinerU 文本解析流程

### Requirement: 上传 MinerU 图片到 OSS 并分类存储
系统 SHALL 将 MinerU 结果包中的有效图片上传到阿里云 OSS，并按照业务类型、用户名、租户、文档和版本生成可追溯对象路径。

#### Scenario: 知识库文档图片上传成功
- **WHEN** `KNOWLEDGE` 类型 MinerU 文档包含有效图片
- **THEN** 系统 MUST 将图片上传到 `mineru-assets/knowledge/{username}-{ownerUserId}/tenant-{tenantId}/doc-{documentId}/v{docVersion}/images/` 分类路径下
- **THEN** 系统 MUST 保存 OSS bucket、object key、访问 URL、原始路径、文件类型、文件大小和内容哈希

#### Scenario: 天眼审查文档图片上传成功
- **WHEN** `TIANYAN_REVIEW` 类型 MinerU 文档包含有效图片
- **THEN** 系统 MUST 将图片上传到 `mineru-assets/tianyan-review/{username}-{ownerUserId}/tenant-{tenantId}/doc-{documentId}/v{docVersion}/images/` 分类路径下

#### Scenario: 风险规则文档图片上传成功
- **WHEN** `RISK_RULE` 类型 MinerU 文档包含有效图片
- **THEN** 系统 MUST 将图片上传到 `mineru-assets/risk-rule/{username}-{ownerUserId}/tenant-{tenantId}/doc-{documentId}/v{docVersion}/images/` 分类路径下

### Requirement: 为 MinerU 图片生成中文描述
系统 SHALL 在配置启用时调用图生文模型，为 MinerU 图片生成适合 RAG 检索的中文描述。

#### Scenario: 图片描述生成成功
- **WHEN** MinerU 图片上传成功且图生文能力已配置启用
- **THEN** 系统 MUST 将图片字节或可访问图片 URL 提交给图生文模型
- **THEN** 系统 MUST 保存模型返回的中文描述
- **THEN** 描述 MUST 客观表达图片中的法律文档、表格、截图、签章或证据含义

#### Scenario: 图片描述生成失败
- **WHEN** 图生文模型未配置、超时、限流或返回无效结果
- **THEN** 系统 MUST 记录失败原因
- **THEN** 系统 MUST 使用默认描述或保留原图片引用继续解析文档

### Requirement: 替换 Markdown 图片引用为可访问且可检索的图片语法
系统 SHALL 将 MinerU Markdown 中的本地图片引用替换为包含图片描述和 OSS 访问地址的 Markdown 图片语法。

#### Scenario: 空 alt 相对路径被替换
- **WHEN** Markdown 中存在 `![](images/example.jpg)` 且该图片已上传 OSS
- **THEN** 系统 MUST 将其替换为 `![图片描述](OSS访问地址)`
- **THEN** 替换后的图片描述 MUST 进入后续清洗、切片、Embedding 和索引流程

#### Scenario: Markdown 引用未匹配到图片资产
- **WHEN** Markdown 中的图片路径无法在 zip 图片资产中找到
- **THEN** 系统 MUST 保留原始 Markdown 文本或替换为可诊断占位描述
- **THEN** 系统 MUST 记录未匹配路径用于排查

### Requirement: 持久化图片资产和切片关联
系统 SHALL 持久化 MinerU 图片资产，并在切片入库后建立 chunk 与图片资产的关联关系。

#### Scenario: 切片包含一个或多个图片引用
- **WHEN** `kb_chunk.content` 中包含已处理图片引用
- **THEN** 系统 MUST 建立该 `chunk_id` 与对应图片资产的关联记录
- **THEN** 系统 MUST 保留图片在该 chunk 中的顺序

#### Scenario: 文档重新解析或版本更新
- **WHEN** 同一文档新版本重新处理 MinerU 图片
- **THEN** 系统 MUST 按 `document_id` 和 `doc_version` 隔离图片资产与 chunk 关联
- **THEN** 系统 MUST 避免旧版本图片关系污染新版本检索结果
