## Requirements

### Requirement: 解析结果进入切块前执行内容清洗
系统 SHALL 在文档解析结果进入切块、入库和索引前执行可配置的内容清洗管线。

#### Scenario: 原生解析文本执行清洗
- **WHEN** 授权用户上传文档并选择原生解析
- **THEN** 系统 MUST 在文本抽取完成后、固定字符切块前执行内容清洗
- **THEN** 系统 MUST 使用清洗后的文本生成 `kb_chunk` 内容

#### Scenario: MinerU Markdown 执行清洗
- **WHEN** 授权用户上传文档并选择 MinerU 精准解析
- **THEN** 系统 MUST 在下载 MinerU Markdown 后、语义切块前执行内容清洗
- **THEN** 系统 MUST 使用清洗后的 Markdown 生成语义切片

### Requirement: 过滤页眉页脚和重复低价值行
系统 SHALL 识别并过滤重复出现的低价值页眉、页脚、页码和官网导出元数据。

#### Scenario: 文档包含重复页眉页脚
- **WHEN** 解析后的文档内容在多个页面首尾重复出现相同行或相似行
- **THEN** 系统 MUST 基于出现频率、页面比例、行位置和低价值模式识别页眉页脚候选
- **THEN** 系统 MUST 从切块输入中移除被判定为噪声的页眉页脚行

#### Scenario: 文档包含页码和导出信息
- **WHEN** 解析内容包含页码、版权声明、下载时间、打印时间、来源链接或系统导出说明
- **THEN** 系统 MUST 按配置的内置规则过滤这些低价值行
- **THEN** 系统 MUST 保留正文中具有业务语义的 URL、日期和来源描述

### Requirement: 保护高价值法律结构内容
系统 SHALL 在清洗过程中保护合同标题、章节标题、编号条款、表格和法律条款正文。

#### Scenario: 文档包含短章节标题
- **WHEN** 解析内容包含“违约责任”“争议解决”“付款条款”等短章节标题
- **THEN** 系统 MUST 不得仅因内容较短而删除这些标题
- **THEN** 系统 MUST 将这些标题保留给后续语义切片流程

#### Scenario: 文档包含重复正文条款
- **WHEN** 正文中存在多处重复或相似的合同条款表述
- **THEN** 系统 MUST 不得仅凭重复次数删除正文条款
- **THEN** 系统 MUST 仅在满足页首页尾位置或低价值模式条件时删除重复行

### Requirement: 切块后过滤低质量切片
系统 SHALL 在 `kb_chunk` 持久化前过滤明显低质量切片。

#### Scenario: 切片只包含页码或导出说明
- **WHEN** 切片内容仅包含页码、孤立 URL、版权声明、下载时间或系统导出说明
- **THEN** 系统 MUST 丢弃该切片
- **THEN** 系统 MUST 不将该切片写入 `kb_chunk` 或 Elasticsearch 索引

#### Scenario: 切片包含有效法律正文
- **WHEN** 切片包含合同条款、法律解释、业务字段或表格证据
- **THEN** 系统 MUST 保留该切片
- **THEN** 系统 MUST 允许该切片继续入库和索引

### Requirement: 输出清洗摘要
系统 SHALL 为每次文档清洗生成可用于排查和测试的清洗摘要。

#### Scenario: 清洗完成后生成摘要
- **WHEN** 文档清洗管线完成处理
- **THEN** 系统 MUST 记录清洗前字符数、清洗后字符数、删除行数和删除原因统计
- **THEN** 系统 MUST 支持在日志或测试断言中查看清洗摘要

### Requirement: 支持可配置清洗策略
系统 SHALL 通过后端配置控制文档清洗能力和关键阈值。

#### Scenario: 清洗能力开启
- **WHEN** `legal.document-processing.cleaning.enabled` 配置为开启
- **THEN** 系统 MUST 对支持的解析结果执行内容清洗和低质量切片过滤

#### Scenario: 清洗能力关闭
- **WHEN** `legal.document-processing.cleaning.enabled` 配置为关闭
- **THEN** 系统 MUST 保持原有解析、切块和入库行为
- **THEN** 系统 MUST 不执行额外内容清洗规则
