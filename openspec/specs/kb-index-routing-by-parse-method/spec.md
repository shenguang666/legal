## Requirements

### Requirement: 知识库文档按解析方式写入不同索引
系统 SHALL 在知识库文档建立 Elasticsearch 索引时，根据文档解析方式选择目标索引：原生解析文档写入原知识库索引，MinerU 精准解析文档写入 MinerU 知识库索引 `legal_kb_chunks_mineru` 或其配置值。

#### Scenario: 原生知识库文档写入原索引
- **WHEN** 知识库文档的解析方式为 `NATIVE` 并触发索引任务
- **THEN** 系统 MUST 将该文档切片写入原知识库索引

#### Scenario: MinerU 知识库文档写入 MinerU 索引
- **WHEN** 知识库文档的解析方式为 `MINERU_PRECISE` 并触发索引任务
- **THEN** 系统 MUST 将该文档切片写入 MinerU 知识库索引

### Requirement: 非知识库文档不受解析方式分索引影响
系统 SHALL 仅对知识库文档执行按解析方式分索引；风险规则文档、天眼审查文档和用户外挂知识 MUST 保持现有索引行为。

#### Scenario: 风险规则文档仍写入风险规则索引
- **WHEN** 风险规则文档使用任意解析方式并触发索引任务
- **THEN** 系统 MUST 继续将该文档切片写入风险规则索引

### Requirement: 删除知识库文档时清理所有知识库索引
系统 SHALL 在删除知识库文档或重置知识库索引时，清理该文档可能存在的原知识库索引和 MinerU 知识库索引数据，避免解析方式切换后产生残留。

#### Scenario: 删除曾经切换解析方式的知识库文档
- **WHEN** 管理员删除一个知识库文档
- **THEN** 系统 MUST 同时尝试从原知识库索引和 MinerU 知识库索引删除该文档切片
