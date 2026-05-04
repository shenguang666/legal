## Why

当前 MinerU 精准解析只从结果 zip 中读取 Markdown 字符串，忽略 zip 内 `images/` 图片文件，导致 `kb_chunk.content` 中保留的是 `![](images/xxx.jpg)` 相对路径，RAG 命中后前端无法访问或展示图片证据。随着知识库、天眼审查和风险规则文档逐步使用 MinerU，图片、截图、表格截图、盖章签字等视觉证据需要被持久化、可检索并可追溯展示。

## What Changes

- 下载 MinerU 结果 zip 时同时抽取 Markdown 和图片资源，而不再只读取 Markdown。
- 将 MinerU zip 内图片上传到阿里云 OSS，并按业务类型、用户名、租户、文档和版本进行对象路径分类。
- 将 Markdown 图片语法从空 alt 的相对路径替换为带图片描述的 OSS 可访问地址，例如 `![图片描述](https://cdn.example.com/...)`。
- 使用图生文模型为 MinerU 图片生成中文描述，使图片语义参与清洗、切片、Embedding 和 RAG 检索。
- 新增图片资产和 chunk 图片引用的持久化能力，支持 RAG 命中 chunk 后返回关联图片 URL 和描述给前端。
- 扩展 RAG Citation 返回结构和前端引用展示，使用户在本轮引用中看到被命中的图片证据。
- 支持对已有 MinerU 文档进行可选图片资产补偿处理：重新下载仍有效的 `mineru_full_zip_url`，上传图片、替换 chunk 内容并重建索引。
- 不改变原生解析文档的现有处理链路；MinerU 图片增强失败时应可降级为保留文本解析结果。

## Capabilities

### New Capabilities
- `mineru-image-asset-processing`: 覆盖 MinerU 结果 zip 中图片资源抽取、OSS 上传、图生文描述、Markdown 替换和图片资产持久化。

### Modified Capabilities
- `mineru-document-extraction`: MinerU 成功解析后不只下载 Markdown，还必须处理 zip 内图片资源并输出可访问、可检索的 Markdown 内容。
- `semantic-document-chunking`: 语义切片输入中的 Markdown 图片应保留图片描述文本，使图片语义可以参与切片和检索。
- `kb-index-routing-by-parse-method`: MinerU 知识库索引内容需要包含替换后的图片描述，确保图片语义进入对应 MinerU 知识库索引。
- `user-knowledge-curation`: RAG 引用需要返回命中 chunk 关联的图片证据，并支持前端展示图片 URL 与描述。

## Impact

- 后端解析链路：`MineruClient`、`DocumentParseTaskService`、清洗和切片前的 MinerU Markdown 处理流程。
- 后端存储：新增图片资产表、chunk 图片引用表及对应实体、Mapper、增量 SQL；所有新增字段和实体属性需要注释。
- OSS 集成：新增阿里云 OSS SDK 依赖和 `legal.storage.oss` 配置；凭证必须通过环境变量或后端安全配置提供，不得暴露给前端。
- 图生文模型：新增 Vision 模型配置与客户端，支持 OpenAI 兼容接口或阿里 DashScope 兼容接口。
- RAG 链路：扩展 `CitationDto`、缓存载荷、检索结果到引用构建逻辑；必要时根据 `chunkId` 查图片引用表。
- 前端：`ChatView` 的本轮引用面板需要展示图片缩略图、图片描述和来源。
- 运维与成本：需要图片数量、大小、总处理量、图生文调用开关和失败降级策略，避免单个文档导致 OSS 或模型调用成本失控。
