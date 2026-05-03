## Why

当前文档导入链路基于 Tika 抽取纯文本并使用固定字符窗口切片，容易把合同条款、长句、表格和列表切断，影响 RAG 检索、风险规则命中和天眼审查的证据完整性。接入 MinerU 精准解析并保留现有原生解析方式，可以在复杂 PDF、扫描件和版式化合同场景下提升解析结构质量，同时让用户按文档类型自主选择成本与效果的平衡。

## What Changes

- 新增可选文档解析方式：在现有原生解析基础上增加 `MinerU 精准解析`，前端上传时可选择解析方式。
- 新增 MinerU 精准解析后端能力：通过 MinerU v4 Precision Extract API 创建上传 URL、上传文件、轮询批次结果、下载解析产物并提取 Markdown/结构化内容。
- 新增统一文档解析抽象：将现有 `DocumentTextExtractor` + `DocumentChunker` 流程封装为可插拔解析策略，知识库、风险规则和天眼审查导入链路复用同一选择机制。
- 优化切片策略：对 MinerU Markdown/结构化结果优先按标题、段落、条款、列表和表格块切分，再按最大长度合并或拆分，降低断句和证据不完整风险。
- 新增上传数量配置：最大上传文档数量由 YAML 配置控制，前后端均应基于该限制进行提示与校验。
- 新增解析状态与失败信息展示：对 MinerU 异步解析过程提供可查询状态、失败原因和重试入口，避免长时间同步请求阻塞。
- 保留兼容路径：未启用 MinerU、未配置 Token 或用户选择原生解析时，继续走现有本地解析和索引流程。

## Capabilities

### New Capabilities
- `document-parsing-method-selection`: 管理文档导入时的解析方式选择、上传数量限制、解析状态展示以及原生解析和 MinerU 精准解析的统一行为。
- `mineru-document-extraction`: 封装 MinerU 精准解析 API 集成、任务状态跟踪、结果下载和解析失败处理。
- `semantic-document-chunking`: 基于文档结构的语义切片能力，减少固定字符切片导致的断句、条款拆散和证据不完整问题。

### Modified Capabilities
- `contract-risk-review`: 天眼审查的输入文档可选择 MinerU 精准解析，并使用更完整的结构化切片作为字段抽取和风险识别证据来源。

## Impact

- 后端：影响 `KnowledgeController`、`TianyanDocumentController`、`RiskRuleManagementController`、`KnowledgeService`、`TianyanDocumentService`、`RiskRuleDocumentService`、`DocumentTextExtractor`、`DocumentChunker` 及新增解析策略、MinerU 客户端、解析任务/worker、配置属性类。
- 前端：影响 `KnowledgeView.vue`、`TianyanView.vue`、`RiskRuleView.vue`，需要增加解析方式选择、批量上传数量提示、解析状态/失败提示和必要的重试入口。
- 数据库：可能扩展 `kb_document` 或新增解析任务表，用于保存解析方式、解析状态、MinerU 批次/任务 ID、失败原因、解析完成时间等信息；新增字段与实体属性必须添加中文注释。
- 配置：在 `application.yaml` 新增文档处理和 MinerU 配置项，包括启用开关、Token、基础地址、模型版本、语言、表格/公式识别、轮询间隔、超时时间、最大上传文档数量；每个 YAML 配置项必须添加中文注释。
- 外部依赖：需要访问 MinerU API，Token 仅允许由后端环境变量配置，不得暴露给前端。
- 兼容性：现有单文件上传和原生解析流程应继续可用，历史文档无需强制重新解析。
