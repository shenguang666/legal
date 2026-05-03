## MODIFIED Requirements

### Requirement: 上传文档时选择解析方式
系统 SHALL 允许授权用户在导入知识库文档、天眼审查文档和风险规则文档时选择文档解析方式，并且系统 MUST 在所选解析方式产出内容后、切块入库前执行适用的文档清洗策略。

#### Scenario: 用户使用原生解析导入文档
- **WHEN** 授权用户上传受支持文档并选择原生解析
- **THEN** 系统 MUST 通过现有本地抽取链路处理文档
- **THEN** 系统 MUST 在本地抽取文本进入切块前执行适用的文档清洗策略
- **THEN** 导入后的文档 MUST 按其业务类型继续支持索引、审查或规则检索

#### Scenario: 用户使用 MinerU 精准解析导入文档
- **WHEN** 授权用户上传受支持文档并选择 MinerU 精准解析
- **THEN** 系统 MUST 记录文档选择的解析方式
- **THEN** 系统 MUST 启动 MinerU 精准解析，且不得向前端暴露 MinerU API Token
- **THEN** 系统 MUST 在 MinerU 解析内容进入切块前执行适用的文档清洗策略
