-- 智能小法庭（合同纠纷模拟庭审 + 证据链图谱 + 补证建议）功能新增表。
-- 与 schema.sql 中追加的同名建表语句保持一致；仅作增量迁移使用。

-- 智能小法庭案件主表，记录案件元信息、用户立场、状态与预算累计。
CREATE TABLE IF NOT EXISTS court_case (
  case_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '案件主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  owner_user_id BIGINT NOT NULL COMMENT '案件所属用户ID',
  owner_username VARCHAR(64) DEFAULT NULL COMMENT '案件创建用户名快照',
  title VARCHAR(255) NOT NULL COMMENT '案件标题',
  case_type VARCHAR(32) NOT NULL DEFAULT 'CONTRACT_DISPUTE' COMMENT '案件类型（CONTRACT_DISPUTE合同纠纷）',
  user_side VARCHAR(16) NOT NULL COMMENT '用户立场（PLAINTIFF原告/DEFENDANT被告）',
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '案件状态（DRAFT草稿/READY就绪/HEARING庭审中/JUDGED已生成裁判/ARCHIVED归档/DELETED删除）',
  case_summary TEXT COMMENT '案件简要描述',
  user_objective VARCHAR(1000) DEFAULT NULL COMMENT '用户立场目标，例如核心诉求或抗辩目标',
  facts_confirmed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '案件要素是否已被用户确认（0未确认/1已确认）',
  total_rounds INT NOT NULL DEFAULT 0 COMMENT '已完成的庭审轮次数量',
  total_tokens BIGINT NOT NULL DEFAULT 0 COMMENT '案件累计消耗 token 数',
  graph_state VARCHAR(16) NOT NULL DEFAULT 'READY' COMMENT '图谱投影状态（READY就绪/PROJECTING投影中/UNAVAILABLE不可用）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (case_id),
  KEY idx_court_case_tenant_owner_status (tenant_id, owner_user_id, status),
  KEY idx_court_case_tenant_updated (tenant_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能小法庭案件表';

-- 案件当事人表，记录原告与被告基础信息。
CREATE TABLE IF NOT EXISTS court_case_party (
  party_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '当事人主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  case_id BIGINT NOT NULL COMMENT '案件ID',
  party_role VARCHAR(16) NOT NULL COMMENT '当事人角色（PLAINTIFF原告/DEFENDANT被告/THIRD_PARTY第三人）',
  display_name VARCHAR(255) NOT NULL COMMENT '当事人显示名称',
  description VARCHAR(1000) DEFAULT NULL COMMENT '当事人简介或主体信息',
  is_user_side TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为用户所代表的一方（0否/1是）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (party_id),
  UNIQUE KEY uk_court_party_case_role (tenant_id, case_id, party_role),
  KEY idx_court_party_case (tenant_id, case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能小法庭案件当事人表';

-- 案件证据登记表，把案件引用的合同、证据、规则文档登记到本表，作为庭审证据白名单依据。
CREATE TABLE IF NOT EXISTS court_case_evidence (
  evidence_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '证据登记主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  case_id BIGINT NOT NULL COMMENT '案件ID',
  document_id BIGINT NOT NULL COMMENT '关联的知识库或风险规则文档ID',
  evidence_role VARCHAR(16) NOT NULL DEFAULT 'EVIDENCE' COMMENT '证据角色（CONTRACT合同/EVIDENCE一般证据/RULE规则依据）',
  display_name VARCHAR(255) NOT NULL COMMENT '证据展示名称（如文档标题快照）',
  description VARCHAR(1000) DEFAULT NULL COMMENT '证据用途描述，可由用户编辑',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '证据状态（ACTIVE有效/INVALID失效/REVOKED撤回）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (evidence_id),
  UNIQUE KEY uk_court_evidence_case_doc (tenant_id, case_id, document_id),
  KEY idx_court_evidence_case_status (tenant_id, case_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能小法庭案件证据登记表';

-- 庭审轮次表，记录每一轮庭审的阶段、状态机、token 消耗与失败原因。
CREATE TABLE IF NOT EXISTS court_hearing_round (
  round_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '庭审轮次主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  case_id BIGINT NOT NULL COMMENT '案件ID',
  round_no INT NOT NULL COMMENT '案件内庭审轮次顺序号，从1开始',
  stage VARCHAR(32) NOT NULL COMMENT '庭审阶段（OPENING_PLAINTIFF原告陈述/OPENING_DEFENDANT被告答辩/EVIDENCE举证/CROSS_EXAMINATION质证/DEBATE法庭辩论/CLOSING最后陈述/JUDGMENT裁判意见）',
  state VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '轮次状态（PENDING/RUNNING/SUCCEEDED/FAILED/CANCELLED/STALE）',
  attempt_id INT NOT NULL DEFAULT 0 COMMENT '当前轮的尝试次数，重试时累加，用于幂等',
  lock_version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  total_tokens INT NOT NULL DEFAULT 0 COMMENT '本轮 LLM 累计消耗 token 数',
  failure_reason VARCHAR(1000) DEFAULT NULL COMMENT '本轮失败原因或取消原因',
  started_at DATETIME DEFAULT NULL COMMENT '本轮开始时间',
  ended_at DATETIME DEFAULT NULL COMMENT '本轮结束时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (round_id),
  UNIQUE KEY uk_court_round_case_no (tenant_id, case_id, round_no),
  KEY idx_court_round_case_state (tenant_id, case_id, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能小法庭庭审轮次表';

-- 庭审消息表，按时间顺序保存每一条庭审发言，含 SSE 流式事件落库结果。
CREATE TABLE IF NOT EXISTS court_hearing_message (
  message_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '庭审消息主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  case_id BIGINT NOT NULL COMMENT '案件ID',
  round_id BIGINT NOT NULL COMMENT '所属庭审轮次ID',
  attempt_id INT NOT NULL DEFAULT 0 COMMENT '所属轮次的尝试编号',
  speaker_role VARCHAR(16) NOT NULL COMMENT '发言角色（USER用户/JUDGE法官/OPPONENT对方/USER_ADVISOR用户辅助律师/SYSTEM系统）',
  speaker_party VARCHAR(16) DEFAULT NULL COMMENT '发言所代表的当事人角色（PLAINTIFF/DEFENDANT/NONE）',
  message_type VARCHAR(32) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型（TEXT文本/STRUCTURED结构化输出/EVENT事件）',
  content MEDIUMTEXT NOT NULL COMMENT '消息文本内容或结构化 JSON 内容',
  token_input INT NOT NULL DEFAULT 0 COMMENT '本条消息消耗的输入 token 数',
  token_output INT NOT NULL DEFAULT 0 COMMENT '本条消息消耗的输出 token 数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (message_id),
  KEY idx_court_message_round (tenant_id, round_id, created_at),
  KEY idx_court_message_case (tenant_id, case_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能小法庭庭审消息表';

-- 庭审观点表，记录所有 AI/用户发言中可用于事实/证据推理的结构化观点，附带证据引用与立场。
CREATE TABLE IF NOT EXISTS court_argument (
  argument_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '庭审观点主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  case_id BIGINT NOT NULL COMMENT '案件ID',
  round_id BIGINT NOT NULL COMMENT '所属庭审轮次ID',
  message_id BIGINT DEFAULT NULL COMMENT '关联的庭审消息ID（如有）',
  speaker_role VARCHAR(16) NOT NULL COMMENT '发言角色（USER/JUDGE/OPPONENT/USER_ADVISOR/SYSTEM）',
  speaker_party VARCHAR(16) DEFAULT NULL COMMENT '发言所代表的当事人角色',
  stance VARCHAR(16) NOT NULL DEFAULT 'NEUTRAL' COMMENT '观点立场（SUPPORT支持/REBUT反驳/NEUTRAL中立/PENDING_PROOF待证明）',
  content TEXT NOT NULL COMMENT '观点正文',
  rationale TEXT COMMENT '观点理由或推理过程',
  evidence_refs_json TEXT COMMENT '关联的证据引用 JSON，含 evidenceId/parentChunkId/childChunkId',
  challenged_argument_ids_json TEXT COMMENT '被本观点反驳的其他观点 ID JSON 数组',
  evidence_verified_count INT NOT NULL DEFAULT 0 COMMENT '通过三层校验的证据引用条数',
  evidence_dropped_count INT NOT NULL DEFAULT 0 COMMENT '被三层校验丢弃的证据引用条数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (argument_id),
  KEY idx_court_argument_case_round (tenant_id, case_id, round_id),
  KEY idx_court_argument_role (tenant_id, case_id, speaker_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能小法庭庭审观点表';

-- 图谱事件表，作为 MySQL 权威源到 Neo4j 异步投影的事件流，所有图谱写入必须经过此表。
CREATE TABLE IF NOT EXISTS court_graph_event (
  event_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '图谱事件主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  case_id BIGINT NOT NULL COMMENT '案件ID',
  round_id BIGINT DEFAULT NULL COMMENT '产生该事件的庭审轮次ID（如有）',
  event_type VARCHAR(32) NOT NULL COMMENT '事件类型（UPSERT_NODE/UPSERT_RELATION/UPDATE_NODE_STATUS/INVALIDATE_NODE/DELETE_CASE_GRAPH）',
  payload_json MEDIUMTEXT NOT NULL COMMENT '事件载荷 JSON，包含节点/关系类型、业务ID、属性、关系两端等',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '投影状态（PENDING/APPLIED/DEAD）',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '当前事件投影失败重试次数',
  error_message VARCHAR(1000) DEFAULT NULL COMMENT '最近一次投影失败原因',
  next_retry_at DATETIME DEFAULT NULL COMMENT '下次允许重试时间',
  applied_at DATETIME DEFAULT NULL COMMENT '事件被成功投影的时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (event_id),
  KEY idx_court_event_status_retry (status, next_retry_at, event_id),
  KEY idx_court_event_case_status (tenant_id, case_id, status, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能小法庭图谱事件表';

-- 模拟裁判报告表，记录庭审收尾时生成的结构化裁判报告与导出审计。
CREATE TABLE IF NOT EXISTS court_judgment_report (
  report_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '模拟裁判报告主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  case_id BIGINT NOT NULL COMMENT '案件ID',
  round_id BIGINT DEFAULT NULL COMMENT '触发报告生成的庭审轮次ID',
  status VARCHAR(16) NOT NULL DEFAULT 'GENERATED' COMMENT '报告状态（GENERATED已生成/REGENERATING重生成中/INVALID失效）',
  focus_issues_json TEXT COMMENT '争议焦点 JSON 数组',
  accepted_facts_json TEXT COMMENT '事实认定 JSON 数组',
  rejected_facts_json TEXT COMMENT '不予采信事实 JSON 数组',
  unfavorable_to_plaintiff_json TEXT COMMENT '对原告不利要点 JSON 数组（强制非空）',
  unfavorable_to_defendant_json TEXT COMMENT '对被告不利要点 JSON 数组（强制非空）',
  judgment_points_json TEXT COMMENT '模拟裁判观点 JSON 数组',
  open_questions_json TEXT COMMENT '法官提出的尚未澄清问题 JSON 数组',
  watermark VARCHAR(255) NOT NULL COMMENT '导出/展示时附加的合规水印文案',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (report_id),
  KEY idx_court_report_case (tenant_id, case_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能小法庭模拟裁判报告表';

-- 补证建议表，由规则引擎+LLM 解释生成，与图谱缺口节点关联，支持差量更新。
CREATE TABLE IF NOT EXISTS court_supplement_suggestion (
  suggestion_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '补证建议主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  case_id BIGINT NOT NULL COMMENT '案件ID',
  round_id BIGINT DEFAULT NULL COMMENT '产生该建议的庭审轮次ID',
  suggestion_type VARCHAR(32) NOT NULL COMMENT '建议类型（CLAIM_NOT_CLOSED诉求未闭环/AMOUNT_INCONSISTENT金额冲突/KEY_DATE_MISSING时间缺失/DEFENSE_NOT_REBUTTED抗辩未反驳/CUSTOM自定义）',
  severity VARCHAR(8) NOT NULL DEFAULT 'MEDIUM' COMMENT '严重等级（HIGH/MEDIUM/LOW）',
  status VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT '建议状态（OPEN开放/RESOLVED解除/IGNORED用户忽略）',
  affected_claim_ids_json TEXT COMMENT '受影响的诉求节点业务ID JSON 数组',
  affected_evidence_ids_json TEXT COMMENT '相关已有证据登记ID JSON 数组',
  missing_description VARCHAR(1000) NOT NULL COMMENT '缺失证据或履约要素的简要描述',
  recommended_materials_json TEXT COMMENT '推荐补充材料 JSON 数组',
  rationale TEXT COMMENT '可解释依据，由 LLM 基于规则结果改写',
  potential_impact VARCHAR(1000) DEFAULT NULL COMMENT '对模拟裁判观点的潜在影响说明',
  graph_gap_business_id VARCHAR(64) DEFAULT NULL COMMENT '关联的 Gap/Risk 节点业务ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (suggestion_id),
  KEY idx_court_suggestion_case_status (tenant_id, case_id, status, severity),
  KEY idx_court_suggestion_case_type (tenant_id, case_id, suggestion_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能小法庭补证建议表';
