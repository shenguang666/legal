## Why

当前知识库、风险规则和天眼审查等文档导入链路会将 PDF、Word、官网导出文件解析后的文本直接切块入库并进入 Elasticsearch 索引。官网导出的 PDF 往往包含重复页眉、页脚、页码、版权声明、下载时间、系统导出标识等低价值内容，这些脏数据会污染向量召回和 BM25 检索结果，降低 RAG 问答准确性。

本变更需要在文档解析后、切块和入库前增加统一的数据清洗能力，减少无关噪声进入 `kb_chunk` 和检索索引，同时保留合同标题、章节标题、法律条款等高价值内容。

## What Changes

- 新增文档内容清洗管线，统一作用于原生解析文本和 MinerU Markdown 解析结果。
- 在切块前执行基础文本规范化、页眉页脚识别、重复低价值行过滤、页码/版权/导出元数据过滤。
- 在切块后增加低质量切片过滤，避免纯页码、孤立链接、导出说明等内容入库。
- 新增可配置的清洗策略开关和阈值，前端默认关闭清洗，用户可在上传时选择开启。
- 新增清洗结果摘要和被清洗内容留存，用于记录清洗前后字符数、删除行数、删除原因统计，便于排查误删和评估清洗效果。
- 保持现有解析方式选择、MinerU 异步解析、知识库索引路由能力不变。

## Capabilities

### New Capabilities

- `document-cleaning-pipeline`: 定义文档解析后、切块入库前的数据清洗能力，包括页眉页脚、重复噪声、官网导出元数据和低质量切片过滤。

### Modified Capabilities

- `document-parsing-method-selection`: 文档解析方式保持不变，但解析结果在进入切块前必须经过统一清洗策略处理。
- `semantic-document-chunking`: 语义切片输入变为清洗后的 Markdown/文本，切片结果需要经过低质量切片过滤后才能持久化。
- `mineru-document-extraction`: MinerU 下载的 Markdown 结果需要接入清洗管线，避免精准解析保留的页眉、页脚、页码等版面噪声进入索引。

## Impact

- 后端文档解析链路：`DocumentImportService`、`NativeDocumentParser`、`DocumentParseTaskService`、`DocumentChunker`、`SemanticDocumentChunker`。
- 配置：`legal.document-processing.cleaning` 下新增清洗开关、阈值和内置规则开关；新增 YAML 配置必须逐条添加中文注释。
- 数据库：新增清洗日志表，保存清洗摘要和被清洗内容样例；新增表/字段必须为每个字段添加数据库注释，并为实体字段添加注释。
- 检索质量：减少 `kb_chunk` 和 Elasticsearch 索引中的低价值噪声，提高 RAG 检索相关性。
- 测试：需要覆盖原生文本、MinerU Markdown、官网导出 PDF 噪声样例、重复页眉页脚、法律章节短标题保留、低质量切片过滤等场景。
