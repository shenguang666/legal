## ADDED Requirements

### Requirement: 基于图谱规则的补证缺口识别
系统 SHALL 基于证据链知识图谱执行规则化缺口检查，MUST 至少覆盖：诉求未闭环、金额冲突、关键时间节点缺失、对方抗辩未被反驳四类。

#### Scenario: 诉求未闭环
- **WHEN** 某 `Claim` 节点不存在 `RAISES_CLAIM -> ASSERTS_FACT -> SUPPORTED_BY -> Evidence` 的可达路径，或路径上事实状态非 `EVIDENCED/ACCEPTED`
- **THEN** 系统 MUST 创建 `Gap` 节点并以 `HAS_GAP` 关系连到该 `Claim`
- **THEN** 系统 MUST 输出补证建议条目，类型为 `CLAIM_NOT_CLOSED`

#### Scenario: 金额冲突
- **WHEN** 同一争议事项的 `Amount` 节点之间存在不一致（合同金额、发票金额、流水金额、诉求金额差异超过阈值）
- **THEN** 系统 MUST 创建 `Risk` 节点并以 `HAS_RISK` 连到对应 `Claim`
- **THEN** 系统 MUST 输出补证建议条目，类型为 `AMOUNT_INCONSISTENT`，并附带涉及的金额节点 ID 列表

#### Scenario: 关键时间节点缺失
- **WHEN** 存在 `Obligation` 但缺少对应 `Date` 节点（履约期限、验收时间、解除通知送达时间等）
- **THEN** 系统 MUST 创建 `Gap` 节点
- **THEN** 系统 MUST 输出补证建议条目，类型为 `KEY_DATE_MISSING`

#### Scenario: 对方抗辩未被反驳
- **WHEN** 某 `Defense` 节点未被任意 `CHALLENGES_ARGUMENT` 关系连入，或未关联到反驳证据
- **THEN** 系统 MUST 输出补证建议条目，类型为 `DEFENSE_NOT_REBUTTED`
- **THEN** 系统 MUST 提示用户该抗辩可能影响哪些诉求

### Requirement: 补证建议结构化输出
系统 SHALL 把每条补证建议持久化到 `court_supplement_suggestion` 表，结构 MUST 至少包含：建议 ID、案件 ID、类型、严重等级、影响的诉求 ID 列表、缺失证据描述、推荐补充材料、可解释依据、对模拟裁判观点的潜在影响。

#### Scenario: 建议条目字段完整
- **WHEN** 系统生成一条补证建议
- **THEN** 该条目 MUST 包含上述全部字段
- **THEN** 该条目 MUST 关联到对应的 `Gap` 或 `Risk` 节点 ID

#### Scenario: 严重等级取值
- **WHEN** 系统设置补证建议严重等级
- **THEN** 取值 MUST 为 `HIGH / MEDIUM / LOW` 之一
- **THEN** `HIGH` MUST 至少包含一条直接影响某 `Claim` 是否成立的依据

### Requirement: LLM 仅做解释 不做缺口判定
系统 SHALL 用 Cypher 规则查询识别缺口与冲突，MUST NOT 由 LLM 直接判断缺口；LLM 仅可用于把规则结果改写为面向用户的可解释建议文案。

#### Scenario: 规则先于 LLM
- **WHEN** 系统生成补证建议
- **THEN** 系统 MUST 先执行命名 Cypher 规则查询得到结构化缺口列表
- **THEN** 系统 MUST 将该列表（含证据 ID）作为输入交给 LLM 改写为自然语言文案

#### Scenario: LLM 文案不得新增缺口
- **WHEN** LLM 输出建议文案
- **THEN** 系统 MUST 校验文案中引用的证据/缺口/诉求 ID 与规则结果一致
- **THEN** 系统 MUST 丢弃 LLM 新增的、规则结果中不存在的缺口

### Requirement: 与庭审阶段联动
系统 SHALL 在每轮庭审结束时增量刷新补证建议，MUST 在最后陈述前向用户高亮新增的高严重度建议。

#### Scenario: 增量刷新
- **WHEN** 一轮庭审完成且 `court_graph_event` 全部投影完毕
- **THEN** 系统 MUST 重新执行规则查询，并对补证建议做差量更新（新增 / 解除 / 升级）
- **THEN** 系统 MUST NOT 全量重写历史建议，已被解除的建议 MUST 保留为 `RESOLVED` 状态用于审计

#### Scenario: 最后陈述前提示
- **WHEN** 系统准备进入“最后陈述”阶段
- **THEN** 系统 MUST 把当前 `HIGH` 等级补证建议作为提示项返回给前端
- **THEN** 系统 MUST 允许用户在最后陈述前补充材料或显式忽略该建议

### Requirement: 补证建议引用真实证据
系统 SHALL 保证补证建议中引用的所有证据 ID 与文档 ID 真实存在且属于当前租户/案件，MUST 复用智能小法庭的证据引用三层校验机制。

#### Scenario: 不存在的证据引用被拒绝
- **WHEN** LLM 改写后的建议文案引用了不存在的证据 ID
- **THEN** 系统 MUST 丢弃该引用
- **THEN** 系统 MUST 在审计日志中记录幻觉次数指标

#### Scenario: 跨租户证据引用被拒绝
- **WHEN** 建议文案引用了跨租户或跨案件的证据 ID
- **THEN** 系统 MUST 丢弃该引用，并把建议严重等级降级一级
