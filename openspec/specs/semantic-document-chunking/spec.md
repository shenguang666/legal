## Requirements

### Requirement: 使用语义边界切分解析文档
系统 SHALL 在从结构化解析结果或 Markdown 解析结果生成切片时，优先使用清洗后的语义文档边界而非固定字符窗口。

#### Scenario: Markdown 包含标题和段落
- **WHEN** 清洗后的 Markdown 包含标题及其后续段落内容
- **THEN** 系统 MUST 尽可能创建保留标题上下文及相关段落内容的切片
- **THEN** 除非切片超过配置最大长度，否则系统 MUST 避免拆分句子

#### Scenario: 文档包含编号条款或列表项
- **WHEN** 清洗后的解析内容包含编号条款或列表项
- **THEN** 系统 MUST 在可行时保持每个条款或列表项完整
- **THEN** 当不超过最大切片长度时，系统 MUST 能够合并相邻短条款

#### Scenario: MinerU 文档使用独立切片大小策略
- **WHEN** 文档通过 MinerU 精准解析生成 Markdown
- **THEN** 系统 MUST 使用 MinerU 专属切片大小配置控制 Markdown 语义切片目标最大字符数
- **THEN** 系统 MUST 使用 MinerU 专属最小切片大小配置控制相邻短语义块合并策略
- **THEN** 该配置 MUST 不影响原生解析文档的切片大小

### Requirement: 保留表格和结构块证据完整性
系统 SHALL 避免以丢失检索和审查证据含义的方式拆分表格和结构化块。

#### Scenario: 解析内容包含表格块
- **WHEN** 解析文档包含以 Markdown 或结构化内容表示的表格
- **THEN** 系统 MUST 在可行时保持表头和相关行位于同一切片
- **THEN** 生成的切片 MUST 适合作为 RAG、规则匹配和天眼审查的证据

### Requirement: 对超长或非结构化内容安全兜底
系统 SHALL 在语义边界不可用或单个块超过最大切片长度时，使用确定性的安全拆分策略兜底，并在持久化前过滤低质量切片。

#### Scenario: 单个段落超过最大切片长度
- **WHEN** 一个语义块超过配置的最大切片长度
- **THEN** 系统 MUST 尽可能按句子或行边界拆分该块
- **THEN** 仅当没有更好边界时，系统 MUST 退回字符窗口拆分

#### Scenario: 原生解析器仅返回纯文本
- **WHEN** 原生解析返回没有结构元数据的纯文本
- **THEN** 系统 MUST 能使用文本清洗、文本规范化和现有切片行为
- **THEN** 系统 MUST 保持与现有文档导入流程兼容

#### Scenario: 切片结果包含低质量噪声
- **WHEN** 切片结果包含仅由页码、版权声明、下载时间、孤立链接或系统导出说明组成的内容
- **THEN** 系统 MUST 在写入 `kb_chunk` 前过滤该切片
- **THEN** 系统 MUST 不让该切片进入后续 Elasticsearch 索引流程
