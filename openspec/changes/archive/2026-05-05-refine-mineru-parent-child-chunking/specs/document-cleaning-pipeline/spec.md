## MODIFIED Requirements

### Requirement: 输出清洗摘要
系统 SHALL 为每次文档清洗生成可用于排查和测试的清洗摘要；清洗摘要中的被移除内容样例 MUST 仅作为日志预览，不代表全部被移除内容，也不得限制全文清洗范围。

#### Scenario: 清洗完成后生成摘要
- **WHEN** 文档清洗管线完成处理
- **THEN** 系统 MUST 记录清洗前字符数、清洗后字符数、删除行数和删除原因统计
- **THEN** 系统 MUST 支持在日志或测试断言中查看清洗摘要

#### Scenario: 被移除内容样例仅用于日志预览
- **WHEN** 文档清洗管线记录被移除内容样例
- **THEN** 系统 MUST 明确该样例仅用于审计日志预览
- **THEN** 系统 MUST 不将样例数量或样例字符长度解释为清洗处理范围
- **THEN** 系统 MUST 保持原始文档资产完整保留

### Requirement: 支持可配置清洗策略
系统 SHALL 通过后端配置控制文档清洗能力和关键阈值；`removed-sample-limit` 与 `removed-sample-max-chars` 配置 MUST 仅控制清洗日志中被移除内容预览样例的数量和长度，不得影响全文清洗范围。

#### Scenario: 清洗能力开启
- **WHEN** `legal.document-processing.cleaning.enabled` 配置为开启
- **THEN** 系统 MUST 对支持的解析结果执行内容清洗和低质量切片过滤

#### Scenario: 清洗能力关闭
- **WHEN** `legal.document-processing.cleaning.enabled` 配置为关闭
- **THEN** 系统 MUST 保持原有解析、切块和入库行为
- **THEN** 系统 MUST 不执行额外内容清洗规则

#### Scenario: 清洗日志预览样例数量配置
- **WHEN** `legal.document-processing.cleaning.removed-sample-limit` 配置了数值
- **THEN** 系统 MUST 仅将该值用于限制清洗日志保存的被移除内容预览样例条数
- **THEN** 系统 MUST 对整份解析内容执行清洗，不得只清洗该数量范围内的内容

#### Scenario: 清洗日志预览样例长度配置
- **WHEN** `legal.document-processing.cleaning.removed-sample-max-chars` 配置了数值
- **THEN** 系统 MUST 仅将该值用于限制清洗日志中单条被移除内容预览样例的字符数
- **THEN** 系统 MUST 不截断原始文档资产或实际参与清洗的解析内容
