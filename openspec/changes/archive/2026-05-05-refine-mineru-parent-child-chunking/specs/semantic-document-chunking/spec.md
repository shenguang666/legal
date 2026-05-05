## MODIFIED Requirements

### Requirement: 使用语义边界切分解析文档
系统 SHALL 在从结构化解析结果或 Markdown 解析结果生成切片时，优先使用清洗后的语义文档边界而非固定字符窗口；对于 MinerU 超长语义块，系统 MUST 将完整父分块语义和子分块检索粒度分离。

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

#### Scenario: MinerU 超长语义块生成父子分块
- **WHEN** MinerU Markdown 中一个语义块超过配置的最大切片长度
- **THEN** 系统 MUST 保存一个包含完整语义块内容的父分块
- **THEN** 系统 MUST 基于该父分块内容生成一个或多个子分块作为检索粒度
- **THEN** 每个子分块 MUST 记录其父分块 `chunk_id`

### Requirement: 对超长或非结构化内容安全兜底
系统 SHALL 在语义边界不可用或单个块超过最大切片长度时，使用确定性的安全拆分策略兜底，并在持久化前过滤低质量切片；对于 MinerU Markdown 中超过最大切片长度的语义块，系统 MUST 使用父子分块策略，父分块保留完整语义块，子分块按自然边界拆分并作为检索粒度。

#### Scenario: 单个段落超过最大切片长度
- **WHEN** 一个语义块超过配置的最大切片长度且不是 MinerU 父子分块处理场景
- **THEN** 系统 MUST 尽可能按句子或行边界拆分该块
- **THEN** 仅当没有更好边界时，系统 MUST 退回字符窗口拆分

#### Scenario: MinerU 语义块超过最大切片长度
- **WHEN** MinerU Markdown 中一个语义块超过配置的最大切片长度
- **THEN** 系统 MUST 保存一个包含完整语义块内容的父分块
- **THEN** 系统 MUST 基于段落、句号、分号、问号、感叹号或其他自然符号边界生成子分块
- **THEN** 子分块 MUST 尽可能保持完整句子或完整段落

#### Scenario: MinerU 子分块使用重叠率
- **WHEN** 系统为 MinerU 超长语义块生成多个子分块
- **THEN** 相邻子分块之间的重叠内容 MUST 不超过目标最大切片长度的 15%
- **THEN** 该重叠 MUST 应用于子分块组合，不得继续沿用旧字符窗口 `fallback-overlap` 语义

#### Scenario: MinerU 单句超过最大切片长度
- **WHEN** MinerU 超长语义块中单个句子本身超过最大切片长度
- **THEN** 系统 MUST 优先使用逗号、顿号、冒号、括号、换行或其他次级自然边界拆分该句子
- **THEN** 系统 MUST 避免无边界的固定字符窗口截断，除非没有任何自然边界可用

#### Scenario: 原生解析器仅返回纯文本
- **WHEN** 原生解析返回没有结构元数据的纯文本
- **THEN** 系统 MUST 能使用文本清洗、文本规范化和现有切片行为
- **THEN** 系统 MUST 保持与现有文档导入流程兼容

#### Scenario: 切片结果包含低质量噪声
- **WHEN** 切片结果包含仅由页码、版权声明、下载时间、孤立链接或系统导出说明组成的内容
- **THEN** 系统 MUST 在写入 `kb_chunk` 前过滤该切片
- **THEN** 系统 MUST 不让该切片进入后续 Elasticsearch 索引流程
