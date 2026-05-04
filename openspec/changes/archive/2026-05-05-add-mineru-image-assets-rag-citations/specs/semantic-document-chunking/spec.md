## MODIFIED Requirements

### Requirement: 使用语义边界切分解析文档
系统 SHALL 在从结构化解析结果或 Markdown 解析结果生成切片时，优先使用清洗后的语义文档边界而非固定字符窗口，并且 MUST 保留已替换 Markdown 图片语法中的图片描述文本，使图片语义能够进入切片内容。

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

#### Scenario: Markdown 包含已处理图片引用
- **WHEN** 清洗后的 Markdown 包含 `![图片描述](OSS访问地址)` 图片语法
- **THEN** 系统 MUST 将图片描述文本保留在对应切片内容中
- **THEN** 系统 MUST 不因图片 URL 过长而删除图片描述或破坏相邻法律文本结构
