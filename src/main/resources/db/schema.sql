CREATE DATABASE IF NOT EXISTS legal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE legal;

CREATE TABLE IF NOT EXISTS legal_user (
  user_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 1001 COMMENT '租户ID',
  username VARCHAR(64) NOT NULL COMMENT '登录用户名',
  password_hash CHAR(64) NOT NULL COMMENT '密码摘要（SHA-256）',
  display_name VARCHAR(64) NOT NULL COMMENT '显示名称',
  role_code VARCHAR(16) NOT NULL COMMENT '角色编码（ADMIN/USER）',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态（ACTIVE/INACTIVE）',
  last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_legal_user_tenant_username (tenant_id, username),
  KEY idx_legal_user_tenant_role (tenant_id, role_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

INSERT INTO legal_user (tenant_id, username, password_hash, display_name, role_code, status)
SELECT 1001, 'admin', SHA2('Admin@123', 256), '系统管理员', 'ADMIN', 'ACTIVE'
WHERE NOT EXISTS (
  SELECT 1 FROM legal_user WHERE tenant_id = 1001 AND username = 'admin'
);

INSERT INTO legal_user (tenant_id, username, password_hash, display_name, role_code, status)
SELECT 1001, 'user', SHA2('User@123', 256), '普通用户', 'USER', 'ACTIVE'
WHERE NOT EXISTS (
  SELECT 1 FROM legal_user WHERE tenant_id = 1001 AND username = 'user'
);

CREATE TABLE IF NOT EXISTS chat_session (
  session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  owner_user_id BIGINT NOT NULL COMMENT '所属用户ID',
  title VARCHAR(120) NOT NULL COMMENT '会话标题',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  last_active_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
  PRIMARY KEY (session_id),
  KEY idx_session_tenant_owner (tenant_id, owner_user_id, last_active_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答会话表';

CREATE TABLE IF NOT EXISTS chat_message (
  message_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息主键ID',
  session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
  role VARCHAR(16) NOT NULL COMMENT '消息角色（user/assistant）',
  content TEXT NOT NULL COMMENT '消息内容',
  token_usage INT DEFAULT 0 COMMENT '本次消息消耗的Token数',
  latency_ms INT DEFAULT 0 COMMENT '本次消息耗时（毫秒）',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (message_id),
  KEY idx_message_session_time (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答消息表';

CREATE TABLE IF NOT EXISTS chat_hotword (
  hotword_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '热词主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID，用于隔离不同租户的热词池',
  hotword_key VARCHAR(64) NOT NULL COMMENT '热词标记，用于前端提交 askStream 时标识命中的热词',
  content VARCHAR(255) NOT NULL COMMENT '热词内容，展示在聊天页快捷问题区域',
  preset_answer TEXT DEFAULT NULL COMMENT '预设答案，命中热词标记时由后端直接返回',
  category VARCHAR(64) DEFAULT NULL COMMENT '热词分类，如劳动合同、合同审查、报销合规',
  weight INT NOT NULL DEFAULT 0 COMMENT '热词权重，用于管理页排序和后续推荐扩展',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '热词排序号，数值越小越靠前',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用，1启用、0停用',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否逻辑删除，1已删除、0未删除',
  created_by BIGINT NOT NULL COMMENT '创建用户ID',
  updated_by BIGINT NOT NULL COMMENT '最近更新用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (hotword_id),
  UNIQUE KEY uk_chat_hotword_tenant_key (tenant_id, hotword_key),
  KEY idx_chat_hotword_tenant_enabled (tenant_id, enabled, deleted, weight, sort_order),
  KEY idx_chat_hotword_tenant_updated (tenant_id, deleted, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天快捷问题热词表';

INSERT INTO chat_hotword (tenant_id, hotword_key, content, preset_answer, category, weight, sort_order, enabled, deleted, created_by, updated_by)
SELECT 1001, 'labor_contract_expire_compensation', '劳动合同到期未续签是否有经济补偿？', '劳动合同到期后用人单位不续签，通常需要按照劳动者在本单位工作年限支付经济补偿；但用人单位维持或提高劳动合同约定条件续订、劳动者不同意续订的，一般无需支付经济补偿。建议重点核对到期通知、续签条件、工作年限和当地裁审口径。', '劳动合同', 100, 10, 1, 0, 1, 1
WHERE NOT EXISTS (
  SELECT 1 FROM chat_hotword WHERE tenant_id = 1001 AND content = '劳动合同到期未续签是否有经济补偿？' AND deleted = 0
);

INSERT INTO chat_hotword (tenant_id, hotword_key, content, preset_answer, category, weight, sort_order, enabled, deleted, created_by, updated_by)
SELECT 1001, 'contract_payment_liability_review', '合同付款条款和违约责任如何审查？', '审查付款条款时应关注付款节点、验收条件、发票要求、付款期限和逾期后果是否清晰；审查违约责任时应核对违约情形、违约金计算方式、损失赔偿范围、解除权和争议解决条款是否匹配业务风险。建议同步检查付款义务与交付、验收、质量保证之间是否存在冲突。', '合同审查', 90, 20, 1, 0, 1, 1
WHERE NOT EXISTS (
  SELECT 1 FROM chat_hotword WHERE tenant_id = 1001 AND content = '合同付款条款和违约责任如何审查？' AND deleted = 0
);

INSERT INTO chat_hotword (tenant_id, hotword_key, content, preset_answer, category, weight, sort_order, enabled, deleted, created_by, updated_by)
SELECT 1001, 'reimbursement_invoice_risk', '报销制度缺少发票会有哪些风险？', '报销缺少合规发票可能带来税前扣除受限、增值税进项抵扣风险、费用真实性难以证明、内部舞弊和审计整改风险。建议制度中明确发票类型、抬头税号、报销凭证、例外审批、替代证明材料和抽查追责机制。', '报销合规', 80, 30, 1, 0, 1, 1
WHERE NOT EXISTS (
  SELECT 1 FROM chat_hotword WHERE tenant_id = 1001 AND content = '报销制度缺少发票会有哪些风险？' AND deleted = 0
);

CREATE TABLE IF NOT EXISTS kb_document (
  document_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  owner_user_id BIGINT NOT NULL COMMENT '所属用户ID',
  title VARCHAR(255) NOT NULL COMMENT '文档标题',
  source VARCHAR(255) NOT NULL COMMENT '文档来源',
  biz_type VARCHAR(32) NOT NULL DEFAULT 'KNOWLEDGE' COMMENT '文档业务类型（KNOWLEDGE/RISK_RULE/TIANYAN_REVIEW）',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '文档状态（PENDING/PROCESSING/ACTIVE/DELETED）',
  doc_version INT NOT NULL DEFAULT 1 COMMENT '文档版本号',
  index_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '索引状态（PENDING/PROCESSING/COMPLETED/FAILED）',
  parse_method VARCHAR(32) NOT NULL DEFAULT 'NATIVE' COMMENT '文档解析方式（NATIVE原生解析/MINERU_PRECISE MinerU精准解析）',
  parse_status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED' COMMENT '文档解析状态（PENDING待解析/PROCESSING解析中/COMPLETED完成/FAILED失败）',
  parse_failure_reason VARCHAR(1000) DEFAULT NULL COMMENT '文档解析失败原因，用于前端展示和排查',
  mineru_batch_id VARCHAR(128) DEFAULT NULL COMMENT 'MinerU批次ID，用于关联外部精准解析任务',
  mineru_data_id VARCHAR(128) DEFAULT NULL COMMENT 'MinerU文件数据ID，用于关联批次内单个文件',
  mineru_full_zip_url VARCHAR(1000) DEFAULT NULL COMMENT 'MinerU完整解析结果压缩包地址，仅后端使用',
  parse_started_at DATETIME DEFAULT NULL COMMENT '最近一次解析开始时间',
  parse_completed_at DATETIME DEFAULT NULL COMMENT '最近一次解析完成时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (document_id),
  KEY idx_doc_tenant_owner_status (tenant_id, owner_user_id, status),
  KEY idx_doc_tenant_owner_biz_status (tenant_id, owner_user_id, biz_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

CREATE TABLE IF NOT EXISTS kb_document_parse_task (
  task_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档解析任务主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  document_id BIGINT NOT NULL COMMENT '文档ID',
  doc_version INT NOT NULL COMMENT '文档版本号',
  parse_method VARCHAR(32) NOT NULL COMMENT '文档解析方式（NATIVE/MINERU_PRECISE）',
  parse_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '解析任务状态（PENDING/PROCESSING/COMPLETED/FAILED）',
  file_name VARCHAR(255) NOT NULL COMMENT '上传文件名',
  file_content LONGBLOB DEFAULT NULL COMMENT '待解析文件内容，仅用于异步提交 MinerU',
  mineru_batch_id VARCHAR(128) DEFAULT NULL COMMENT 'MinerU批次ID',
  mineru_data_id VARCHAR(128) DEFAULT NULL COMMENT 'MinerU文件数据ID',
  mineru_full_zip_url VARCHAR(1000) DEFAULT NULL COMMENT 'MinerU完整解析结果压缩包地址',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '解析任务失败后的重试次数',
  error_message VARCHAR(1000) DEFAULT NULL COMMENT '解析任务失败原因',
  started_at DATETIME DEFAULT NULL COMMENT '解析任务开始时间',
  completed_at DATETIME DEFAULT NULL COMMENT '解析任务完成时间',
  next_retry_at DATETIME DEFAULT NULL COMMENT '下次允许重试时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (task_id),
  KEY idx_parse_task_status_retry (parse_status, next_retry_at, retry_count),
  KEY idx_parse_task_document (tenant_id, document_id, doc_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档解析任务表';

CREATE TABLE IF NOT EXISTS kb_chunk (
  chunk_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '切片主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  document_id BIGINT NOT NULL COMMENT '文档ID',
  doc_version INT NOT NULL COMMENT '文档版本号',
  chunk_order INT NOT NULL COMMENT '切片顺序号',
  content MEDIUMTEXT NOT NULL COMMENT '切片内容',
  content_hash VARCHAR(64) NOT NULL COMMENT '切片内容哈希',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (chunk_id),
  KEY idx_chunk_doc_ver_order (document_id, doc_version, chunk_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库切片表';

CREATE TABLE IF NOT EXISTS kb_index_outbox (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '出站事件主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  document_id BIGINT NOT NULL COMMENT '文档ID',
  doc_version INT NOT NULL COMMENT '文档版本号',
  op VARCHAR(16) NOT NULL COMMENT '索引操作类型（UPSERT/DELETE）',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '出站任务状态（PENDING/PROCESSING/DONE/FAILED）',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  next_retry_at DATETIME DEFAULT NULL COMMENT '下次重试时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_doc_version_op (document_id, doc_version, op),
  KEY idx_outbox_status_retry (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='索引任务出站表';

CREATE TABLE IF NOT EXISTS knowledge_qa_index_config (
  config_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '智能问答知识库索引配置主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID，用于隔离不同租户的智能问答检索配置',
  index_scope VARCHAR(32) NOT NULL DEFAULT 'NATIVE_ONLY' COMMENT '智能问答知识库检索索引范围（NATIVE_ONLY原生索引/MINERU_ONLY MinerU索引/BOTH双索引）',
  created_by BIGINT NOT NULL COMMENT '创建配置的用户ID',
  updated_by BIGINT NOT NULL COMMENT '最近更新配置的用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (config_id),
  UNIQUE KEY uk_knowledge_qa_index_config_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能问答知识库索引配置表';

CREATE TABLE IF NOT EXISTS qa_feedback (
  feedback_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '反馈主键ID',
  message_id BIGINT NOT NULL COMMENT '消息ID',
  helpful TINYINT(1) NOT NULL COMMENT '是否有帮助',
  comment VARCHAR(1000) DEFAULT NULL COMMENT '反馈补充说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (feedback_id),
  KEY idx_feedback_message (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答反馈表';

CREATE TABLE IF NOT EXISTS retrieval_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志主键ID',
  trace_id VARCHAR(64) NOT NULL COMMENT '链路追踪ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  query_text TEXT NOT NULL COMMENT '检索查询文本',
  hit_chunk_ids VARCHAR(1024) DEFAULT NULL COMMENT '命中的切片ID列表',
  rerank_score DECIMAL(10,4) DEFAULT NULL COMMENT '重排得分',
  model_name VARCHAR(128) DEFAULT NULL COMMENT '使用的模型名称',
  latency_ms INT DEFAULT 0 COMMENT '检索耗时（毫秒）',
  rag_metric_scan_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'RAG指标扫描状态（PENDING待扫描/PROCESSING处理中/SUCCESS成功/FAILED失败/FILTERED已过滤）',
  rag_metric_scanned_at DATETIME DEFAULT NULL COMMENT 'RAG指标最近扫描完成时间',
  rag_metric_summary_id BIGINT DEFAULT NULL COMMENT 'RAG指标日汇总记录ID',
  rag_metric_retry_count INT NOT NULL DEFAULT 0 COMMENT 'RAG指标扫描失败后的重试次数',
  rag_metric_error_message VARCHAR(1000) DEFAULT NULL COMMENT 'RAG指标扫描失败或过滤原因',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_retrieval_trace (trace_id),
  KEY idx_retrieval_tenant_time (tenant_id, created_at),
  KEY idx_retrieval_rag_metric_scan (tenant_id, created_at, rag_metric_scan_status, rag_metric_retry_count),
  KEY idx_retrieval_rag_metric_summary (rag_metric_summary_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检索日志表';

CREATE TABLE IF NOT EXISTS rag_retrieval_metric_daily_summary (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'RAG检索指标日汇总主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  metric_date DATE NOT NULL COMMENT '指标业务日期',
  evaluation_version VARCHAR(128) NOT NULL COMMENT '评估版本，用于区分提示词、TopN和精筛口径',
  total_rag_message_count BIGINT NOT NULL DEFAULT 0 COMMENT '当天使用过RAG检索的消息总数',
  valid_message_count BIGINT NOT NULL DEFAULT 0 COMMENT '当天未被过滤且进入评估流程的有效消息数',
  filtered_message_count BIGINT NOT NULL DEFAULT 0 COMMENT '当天被低质量Query过滤的消息数',
  success_count BIGINT NOT NULL DEFAULT 0 COMMENT '当天评估成功的消息数',
  failed_count BIGINT NOT NULL DEFAULT 0 COMMENT '当天评估失败的消息数',
  skipped_count BIGINT NOT NULL DEFAULT 0 COMMENT '当天跳过评估的消息数',
  average_recall DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '当天成功样本的平均召回率',
  average_precision DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '当天成功样本的平均精确率',
  candidate_top_n INT NOT NULL DEFAULT 200 COMMENT '当天评估使用的粗召回TopN配置',
  status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING' COMMENT '日汇总任务状态（PROCESSING处理中/COMPLETED已完成/PARTIAL_FAILED部分失败/FAILED失败）',
  error_message VARCHAR(1000) DEFAULT NULL COMMENT '日汇总任务失败或部分失败说明',
  started_at DATETIME DEFAULT NULL COMMENT '日汇总任务首次开始处理时间',
  completed_at DATETIME DEFAULT NULL COMMENT '日汇总任务最近完成时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_rag_metric_daily_tenant_date_version (tenant_id, metric_date, evaluation_version),
  KEY idx_rag_metric_daily_date_status (metric_date, status),
  KEY idx_rag_metric_daily_tenant_date (tenant_id, metric_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG检索质量日汇总表';

CREATE TABLE IF NOT EXISTS rag_retrieval_metric_evaluation (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '评估记录主键ID',
  daily_summary_id BIGINT DEFAULT NULL COMMENT '所属RAG检索质量日汇总ID，历史数据允许为空',
  retrieval_log_id BIGINT NOT NULL COMMENT '被评估的检索日志ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  query_text TEXT NOT NULL COMMENT '原始检索问题文本',
  original_hit_chunk_ids TEXT COMMENT '原始命中的切片ID列表（JSON数组）',
  missed_relevant_chunk_ids TEXT COMMENT '评估发现的漏召回相关切片ID列表（JSON数组）',
  relevant_original_chunk_ids TEXT COMMENT '原始命中且被判定相关的切片ID列表（JSON数组）',
  irrelevant_original_chunk_ids TEXT COMMENT '原始命中但被判定不相关的切片ID列表（JSON数组）',
  true_positive INT NOT NULL DEFAULT 0 COMMENT '真阳性数量',
  false_negative INT NOT NULL DEFAULT 0 COMMENT '假阴性数量',
  false_positive INT NOT NULL DEFAULT 0 COMMENT '假阳性数量',
  recall_score DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '召回率分数',
  precision_score DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '精确率分数',
  evaluated_candidate_count INT NOT NULL DEFAULT 0 COMMENT '本次评估实际召回的候选切片数量',
  evaluated_top_n INT NOT NULL DEFAULT 200 COMMENT '本次评估配置的候选召回TopN',
  model_name VARCHAR(128) DEFAULT NULL COMMENT '执行判断的大模型名称',
  prompt_version VARCHAR(64) NOT NULL COMMENT '评估提示词版本',
  judge_summary MEDIUMTEXT COMMENT '大模型判断摘要或原始结构化结果',
  explanation MEDIUMTEXT COMMENT '大模型给出的可解释判断说明',
  status VARCHAR(32) NOT NULL COMMENT '评估状态（SUCCESS成功/FAILED失败/SKIPPED跳过）',
  error_message VARCHAR(1000) DEFAULT NULL COMMENT '失败时记录的错误信息',
  evaluation_version VARCHAR(128) NOT NULL COMMENT '评估版本，用于幂等和重跑',
  evaluated_at DATETIME NOT NULL COMMENT '评估完成时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_rag_metric_log_version (retrieval_log_id, evaluation_version),
  KEY idx_rag_metric_daily_summary (daily_summary_id),
  KEY idx_rag_metric_tenant_time (tenant_id, evaluated_at),
  KEY idx_rag_metric_status (status),
  CONSTRAINT fk_rag_metric_eval_daily_summary FOREIGN KEY (daily_summary_id) REFERENCES rag_retrieval_metric_daily_summary (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG检索质量评估结果表';

CREATE TABLE IF NOT EXISTS token_usage_metric_daily_summary (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Token消耗日汇总主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  metric_date DATE NOT NULL COMMENT '指标业务日期',
  metric_version VARCHAR(128) NOT NULL COMMENT '统计版本，用于隔离不同统计口径',
  total_token_usage BIGINT NOT NULL DEFAULT 0 COMMENT '当天统计到的Token总消耗',
  message_count BIGINT NOT NULL DEFAULT 0 COMMENT '当天参与Token统计的助手消息数量',
  active_user_count BIGINT NOT NULL DEFAULT 0 COMMENT '当天产生Token消耗的活跃用户数量',
  average_tokens_per_message DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '当天平均每条助手消息消耗的Token数',
  top_user_limit INT NOT NULL DEFAULT 10 COMMENT '本次日汇总保留的Top用户数量配置',
  top_user_count INT NOT NULL DEFAULT 0 COMMENT '实际写入的Top用户明细数量',
  status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING' COMMENT '日汇总任务状态（PROCESSING处理中/COMPLETED已完成/PARTIAL_FAILED部分失败/FAILED失败）',
  error_message VARCHAR(1000) DEFAULT NULL COMMENT '日汇总任务失败或部分失败说明',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '日汇总任务失败重试次数',
  started_at DATETIME DEFAULT NULL COMMENT '日汇总任务首次开始处理时间',
  completed_at DATETIME DEFAULT NULL COMMENT '日汇总任务最近完成时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_token_usage_daily_tenant_date_version (tenant_id, metric_date, metric_version),
  KEY idx_token_usage_daily_date_status (metric_date, status),
  KEY idx_token_usage_daily_tenant_date (tenant_id, metric_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Token消耗指标日汇总表';

CREATE TABLE IF NOT EXISTS token_usage_metric_top_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Token消耗Top用户明细主键ID',
  daily_summary_id BIGINT NOT NULL COMMENT '所属Token消耗日汇总ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  metric_date DATE NOT NULL COMMENT '指标业务日期',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  username VARCHAR(64) DEFAULT NULL COMMENT '登录用户名快照',
  display_name VARCHAR(128) DEFAULT NULL COMMENT '用户显示名称快照',
  rank_no INT NOT NULL COMMENT '用户在该日Token消耗排行榜中的名次',
  token_usage BIGINT NOT NULL DEFAULT 0 COMMENT '该用户当天Token消耗总量',
  message_count BIGINT NOT NULL DEFAULT 0 COMMENT '该用户当天参与Token统计的助手消息数量',
  usage_ratio DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '该用户Token消耗占当日租户总消耗比例',
  metric_version VARCHAR(128) NOT NULL COMMENT '统计版本，用于隔离不同统计口径',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_token_usage_top_summary_user (daily_summary_id, user_id),
  KEY idx_token_usage_top_summary_rank (daily_summary_id, rank_no),
  KEY idx_token_usage_top_tenant_date (tenant_id, metric_date, token_usage),
  KEY idx_token_usage_top_user (tenant_id, user_id, metric_date),
  CONSTRAINT fk_token_usage_top_daily_summary FOREIGN KEY (daily_summary_id) REFERENCES token_usage_metric_daily_summary (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Token消耗指标Top用户明细表';

CREATE TABLE IF NOT EXISTS chat_memory_summary (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '摘要主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
  summary_text MEDIUMTEXT NOT NULL COMMENT '摘要文本（建议JSON）',
  round_count INT NOT NULL DEFAULT 0 COMMENT '已摘要到第几轮',
  version INT NOT NULL DEFAULT 1 COMMENT '摘要版本',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_summary_session (tenant_id, user_id, session_id),
  KEY idx_summary_tenant_user (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话摘要记忆表';

CREATE TABLE IF NOT EXISTS memory_task_outbox (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  session_id VARCHAR(64) DEFAULT NULL COMMENT '会话ID',
  task_type VARCHAR(32) NOT NULL COMMENT '任务类型',
  payload MEDIUMTEXT NOT NULL COMMENT '任务参数JSON',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  next_retry_at DATETIME DEFAULT NULL COMMENT '下次重试时间',
  last_error VARCHAR(1000) DEFAULT NULL COMMENT '最近失败原因',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_task_status_retry (status, next_retry_at),
  KEY idx_task_tenant_user (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记忆异步任务outbox';

CREATE TABLE IF NOT EXISTS user_memory_item (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '记忆主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  memory_type VARCHAR(32) NOT NULL COMMENT '记忆类型',
  memory_key VARCHAR(64) NOT NULL COMMENT '记忆键',
  memory_value MEDIUMTEXT NOT NULL COMMENT '记忆值JSON',
  sensitivity_level VARCHAR(8) NOT NULL DEFAULT 'P0' COMMENT '敏感等级',
  confidence DECIMAL(5,4) NOT NULL DEFAULT 0.0 COMMENT '置信度',
  confirmation_count INT NOT NULL DEFAULT 0 COMMENT '连续命中确认次数',
  stable_threshold INT NOT NULL DEFAULT 2 COMMENT '稳定升级阈值',
  status VARCHAR(16) NOT NULL DEFAULT 'CANDIDATE' COMMENT '状态',
  source_session_id VARCHAR(64) DEFAULT NULL COMMENT '来源会话ID',
  source_message_id BIGINT DEFAULT NULL COMMENT '来源消息ID',
  confirmed_by_user TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否用户确认',
  expires_at DATETIME DEFAULT NULL COMMENT '过期时间',
  last_seen_at DATETIME DEFAULT NULL COMMENT '最近出现时间',
  stable_since DATETIME DEFAULT NULL COMMENT '稳定生效时间',
  model_version VARCHAR(64) DEFAULT NULL COMMENT '模型版本',
  prompt_version VARCHAR(64) DEFAULT NULL COMMENT 'Prompt版本',
  version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_memory_tenant_user (tenant_id, user_id),
  KEY idx_memory_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户长期记忆事实表';

CREATE TABLE IF NOT EXISTS user_knowledge (
  knowledge_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户知识主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  session_id VARCHAR(64) NOT NULL COMMENT '来源会话ID',
  question TEXT NOT NULL COMMENT '用户问题',
  answer MEDIUMTEXT NOT NULL COMMENT '助手回答',
  content MEDIUMTEXT NOT NULL COMMENT '用于索引的知识内容',
  source VARCHAR(255) NOT NULL COMMENT '知识来源说明',
  knowledge_level VARCHAR(16) NOT NULL DEFAULT 'OPTIONAL' COMMENT '知识等级（MUST/OPTIONAL/FORBIDDEN）',
  reason VARCHAR(64) DEFAULT NULL COMMENT '分类理由',
  core_content MEDIUMTEXT DEFAULT NULL COMMENT '提炼后的核心内容',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态（PENDING/ACTIVE/REJECTED/DELETED）',
  index_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '索引状态（PENDING/COMPLETED/FAILED）',
  source_user_message_id BIGINT DEFAULT NULL COMMENT '来源用户消息ID',
  source_assistant_message_id BIGINT DEFAULT NULL COMMENT '来源助手消息ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (knowledge_id),
  KEY idx_user_knowledge_owner (tenant_id, user_id, created_at),
  KEY idx_user_knowledge_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户外挂知识库表';

CREATE TABLE IF NOT EXISTS contract_field_definition (
  field_definition_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '抽取字段定义主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '所属租户ID，0表示平台默认字段定义',
  field_code VARCHAR(64) NOT NULL COMMENT '字段编码，如 party_a、contract_amount',
  field_name VARCHAR(128) NOT NULL COMMENT '字段名称',
  extractor_kind VARCHAR(32) NOT NULL COMMENT '抽取器类型（PARTY_PATTERN/AMOUNT_PATTERN/DATE_KEYWORD/KEYWORD_LINE）',
  pattern_expr VARCHAR(1000) DEFAULT NULL COMMENT '正则表达式配置',
  keyword_config VARCHAR(1000) DEFAULT NULL COMMENT '关键字配置，支持换行分隔',
  repeatable TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否允许抽取多个结果',
  deduplicate_by_normalized TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否按归一化值去重',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '字段定义是否启用',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '字段抽取顺序',
  description VARCHAR(500) DEFAULT NULL COMMENT '字段定义说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (field_definition_id),
  UNIQUE KEY uk_contract_field_definition_tenant_code (tenant_id, field_code),
  KEY idx_contract_field_definition_enabled (tenant_id, enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同抽取字段定义表';

CREATE TABLE IF NOT EXISTS contract_review (
  review_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '合同审阅记录主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  document_id BIGINT NOT NULL COMMENT '被审阅文档ID',
  doc_version INT NOT NULL COMMENT '审阅绑定的文档版本号',
  owner_user_id BIGINT NOT NULL COMMENT '文档所属用户ID',
  triggered_by_user_id BIGINT NOT NULL COMMENT '触发审阅的用户ID',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '审阅状态（PENDING/PROCESSING/COMPLETED/FAILED）',
  risk_level VARCHAR(16) DEFAULT NULL COMMENT '总体风险等级（LOW/MEDIUM/HIGH）',
  risk_count INT NOT NULL DEFAULT 0 COMMENT '风险项数量',
  hit_rule_count INT NOT NULL DEFAULT 0 COMMENT '命中规则数量',
  total_field_count INT NOT NULL DEFAULT 0 COMMENT '总字段数量',
  extracted_field_count INT NOT NULL DEFAULT 0 COMMENT '成功抽取字段数量',
  missing_field_count INT NOT NULL DEFAULT 0 COMMENT '缺失字段数量',
  summary_text VARCHAR(1000) DEFAULT NULL COMMENT '审阅摘要',
  failure_reason VARCHAR(1000) DEFAULT NULL COMMENT '审阅失败原因',
  started_at DATETIME DEFAULT NULL COMMENT '开始处理时间',
  completed_at DATETIME DEFAULT NULL COMMENT '完成处理时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (review_id),
  KEY idx_contract_review_doc (tenant_id, document_id, created_at),
  KEY idx_contract_review_status (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同审阅主表';

CREATE TABLE IF NOT EXISTS contract_review_field (
  field_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '字段结果主键ID',
  review_id BIGINT NOT NULL COMMENT '所属审阅记录ID',
  field_code VARCHAR(64) NOT NULL COMMENT '字段编码',
  field_name VARCHAR(128) NOT NULL COMMENT '字段名称',
  raw_value TEXT DEFAULT NULL COMMENT '原始抽取值',
  normalized_value VARCHAR(512) DEFAULT NULL COMMENT '归一化字段值',
  status VARCHAR(16) NOT NULL COMMENT '字段抽取状态（EXTRACTED/MISSING/UNCERTAIN）',
  confidence DECIMAL(5,4) DEFAULT NULL COMMENT '抽取置信度',
  evidence_text MEDIUMTEXT DEFAULT NULL COMMENT '字段证据片段',
  source_chunk_ref VARCHAR(128) DEFAULT NULL COMMENT '来源切片引用',
  extractor_type VARCHAR(32) DEFAULT NULL COMMENT '抽取器类型',
  field_order INT NOT NULL DEFAULT 0 COMMENT '字段展示顺序',
  group_key VARCHAR(64) DEFAULT NULL COMMENT '多值字段分组键',
  explanation VARCHAR(500) DEFAULT NULL COMMENT '字段解释说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (field_id),
  KEY idx_contract_field_review (review_id, field_code, field_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同审阅字段结果表';

CREATE TABLE IF NOT EXISTS contract_risk_item (
  risk_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '风险项主键ID',
  review_id BIGINT NOT NULL COMMENT '所属审阅记录ID',
  rule_code VARCHAR(64) NOT NULL COMMENT '规则编码',
  rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
  rule_type VARCHAR(32) NOT NULL COMMENT '规则类型',
  severity VARCHAR(16) NOT NULL COMMENT '风险严重级别',
  execution_status VARCHAR(16) NOT NULL COMMENT '规则执行状态（HIT/PASSED/SKIPPED）',
  message VARCHAR(1000) NOT NULL COMMENT '风险说明信息',
  evidence_text MEDIUMTEXT DEFAULT NULL COMMENT '风险证据片段',
  affected_field_codes VARCHAR(255) DEFAULT NULL COMMENT '受影响字段编码列表',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (risk_id),
  KEY idx_contract_risk_review (review_id, severity, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同风险项表';

CREATE TABLE IF NOT EXISTS contract_review_task (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '异步任务主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  review_id BIGINT NOT NULL COMMENT '合同审阅记录ID',
  document_id BIGINT NOT NULL COMMENT '文档ID',
  doc_version INT NOT NULL COMMENT '文档版本号',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态（PENDING/PROCESSING/DONE/FAILED）',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  next_retry_at DATETIME DEFAULT NULL COMMENT '下次重试时间',
  last_error VARCHAR(1000) DEFAULT NULL COMMENT '最近失败原因',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_contract_review_task_review (review_id),
  KEY idx_contract_review_task_status (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同审阅异步任务表';

CREATE TABLE IF NOT EXISTS contract_rule_definition (
  rule_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则定义主键ID',
  tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '规则所属租户ID，0表示默认规则',
  rule_code VARCHAR(64) NOT NULL COMMENT '规则编码',
  rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
  rule_type VARCHAR(32) NOT NULL COMMENT '规则类型（含 DOCUMENT_RETRIEVAL 检索型规则）',
  rule_source_type VARCHAR(32) NOT NULL DEFAULT 'STRUCTURED' COMMENT '规则来源类型（STRUCTURED/MANUAL_TEXT/IMPORTED_DOCUMENT）',
  field_code VARCHAR(64) DEFAULT NULL COMMENT '关联字段编码',
  document_id BIGINT DEFAULT NULL COMMENT '关联的风险规则文档ID',
  severity VARCHAR(16) NOT NULL COMMENT '命中后的严重级别',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  hit_threshold DECIMAL(6,4) DEFAULT NULL COMMENT '检索型规则命中阈值',
  rule_content MEDIUMTEXT DEFAULT NULL COMMENT '手工录入的规则原文',
  rule_params VARCHAR(1000) DEFAULT NULL COMMENT '规则参数JSON',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '规则排序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (rule_id),
  UNIQUE KEY uk_contract_rule_tenant_code (tenant_id, rule_code),
  KEY idx_contract_rule_enabled (tenant_id, enabled, sort_order),
  KEY idx_contract_rule_document (tenant_id, rule_source_type, enabled, document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同规则定义表';

INSERT INTO contract_field_definition (tenant_id, field_code, field_name, extractor_kind, pattern_expr, keyword_config, repeatable, deduplicate_by_normalized, enabled, sort_order, description)
SELECT 0, 'party_a', '甲方', 'PARTY_PATTERN', '(?:甲方|采购方|发包方|委托方)\\s*[：:]\\s*([^\\n，。,；;]{2,40})', NULL, 0, 1, 1, 10, '抽取合同甲方主体'
WHERE NOT EXISTS (
  SELECT 1 FROM contract_field_definition WHERE tenant_id = 0 AND field_code = 'party_a'
);

INSERT INTO contract_field_definition (tenant_id, field_code, field_name, extractor_kind, pattern_expr, keyword_config, repeatable, deduplicate_by_normalized, enabled, sort_order, description)
SELECT 0, 'party_b', '乙方', 'PARTY_PATTERN', '(?:乙方|供应商|承包方|受托方)\\s*[：:]\\s*([^\\n，。,；;]{2,40})', NULL, 0, 1, 1, 20, '抽取合同乙方主体'
WHERE NOT EXISTS (
  SELECT 1 FROM contract_field_definition WHERE tenant_id = 0 AND field_code = 'party_b'
);

INSERT INTO contract_field_definition (tenant_id, field_code, field_name, extractor_kind, pattern_expr, keyword_config, repeatable, deduplicate_by_normalized, enabled, sort_order, description)
SELECT 0, 'contract_amount', '合同金额', 'AMOUNT_PATTERN', '((?:人民币)?\\s*[0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|(?:人民币)?\\s*[0-9]+(?:\\.[0-9]{1,2})?)\\s*(万元|元)', NULL, 1, 1, 1, 30, '抽取合同金额并进行归一化'
WHERE NOT EXISTS (
  SELECT 1 FROM contract_field_definition WHERE tenant_id = 0 AND field_code = 'contract_amount'
);

INSERT INTO contract_field_definition (tenant_id, field_code, field_name, extractor_kind, pattern_expr, keyword_config, repeatable, deduplicate_by_normalized, enabled, sort_order, description)
SELECT 0, 'effective_date', '生效日期', 'DATE_KEYWORD', '(20\\d{2})[年/.-](0?[1-9]|1[0-2])[月/.-](0?[1-9]|[12]\\d|3[01])日?', '生效日期\n签订日期\n签署日期\n合同日期', 0, 1, 1, 40, '结合关键字抽取合同生效日期'
WHERE NOT EXISTS (
  SELECT 1 FROM contract_field_definition WHERE tenant_id = 0 AND field_code = 'effective_date'
);

INSERT INTO contract_field_definition (tenant_id, field_code, field_name, extractor_kind, pattern_expr, keyword_config, repeatable, deduplicate_by_normalized, enabled, sort_order, description)
SELECT 0, 'payment_date', '付款日期', 'DATE_KEYWORD', '(20\\d{2})[年/.-](0?[1-9]|1[0-2])[月/.-](0?[1-9]|[12]\\d|3[01])日?', '付款日期\n支付日期\n结算日期\n报销日期\n付款时间', 1, 0, 1, 50, '结合关键字抽取付款或结算日期'
WHERE NOT EXISTS (
  SELECT 1 FROM contract_field_definition WHERE tenant_id = 0 AND field_code = 'payment_date'
);

INSERT INTO contract_field_definition (tenant_id, field_code, field_name, extractor_kind, pattern_expr, keyword_config, repeatable, deduplicate_by_normalized, enabled, sort_order, description)
SELECT 0, 'liability_clause', '责任条款', 'KEYWORD_LINE', NULL, '违约责任\n责任承担\n赔偿责任\n承担责任\n免责', 1, 0, 1, 60, '抽取责任承担与免责相关条款'
WHERE NOT EXISTS (
  SELECT 1 FROM contract_field_definition WHERE tenant_id = 0 AND field_code = 'liability_clause'
);

INSERT INTO contract_field_definition (tenant_id, field_code, field_name, extractor_kind, pattern_expr, keyword_config, repeatable, deduplicate_by_normalized, enabled, sort_order, description)
SELECT 0, 'reimbursement_item', '报销项目', 'KEYWORD_LINE', NULL, '报销\n费用\n差旅\n交通\n住宿\n发票', 1, 0, 1, 70, '抽取报销、费用与票据相关字段'
WHERE NOT EXISTS (
  SELECT 1 FROM contract_field_definition WHERE tenant_id = 0 AND field_code = 'reimbursement_item'
);

INSERT INTO contract_rule_definition (tenant_id, rule_code, rule_name, rule_type, field_code, severity, enabled, rule_params, sort_order)
SELECT 0, 'REQUIRED_PARTY_A', '甲方必填', 'REQUIRED_FIELD', 'party_a', 'HIGH', 1, NULL, 10
WHERE NOT EXISTS (
  SELECT 1 FROM contract_rule_definition WHERE tenant_id = 0 AND rule_code = 'REQUIRED_PARTY_A'
);

INSERT INTO contract_rule_definition (tenant_id, rule_code, rule_name, rule_type, field_code, severity, enabled, rule_params, sort_order)
SELECT 0, 'REQUIRED_PARTY_B', '乙方必填', 'REQUIRED_FIELD', 'party_b', 'HIGH', 1, NULL, 20
WHERE NOT EXISTS (
  SELECT 1 FROM contract_rule_definition WHERE tenant_id = 0 AND rule_code = 'REQUIRED_PARTY_B'
);

INSERT INTO contract_rule_definition (tenant_id, rule_code, rule_name, rule_type, field_code, severity, enabled, rule_params, sort_order)
SELECT 0, 'REQUIRED_CONTRACT_AMOUNT', '合同金额必填', 'REQUIRED_FIELD', 'contract_amount', 'HIGH', 1, NULL, 30
WHERE NOT EXISTS (
  SELECT 1 FROM contract_rule_definition WHERE tenant_id = 0 AND rule_code = 'REQUIRED_CONTRACT_AMOUNT'
);

INSERT INTO contract_rule_definition (tenant_id, rule_code, rule_name, rule_type, field_code, severity, enabled, rule_params, sort_order)
SELECT 0, 'REQUIRED_EFFECTIVE_DATE', '生效日期必填', 'REQUIRED_FIELD', 'effective_date', 'MEDIUM', 1, NULL, 40
WHERE NOT EXISTS (
  SELECT 1 FROM contract_rule_definition WHERE tenant_id = 0 AND rule_code = 'REQUIRED_EFFECTIVE_DATE'
);

INSERT INTO contract_rule_definition (tenant_id, rule_code, rule_name, rule_type, field_code, severity, enabled, rule_params, sort_order)
SELECT 0, 'AMOUNT_CONSISTENCY', '金额一致性校验', 'AMOUNT_CONSISTENCY', 'contract_amount', 'HIGH', 1, NULL, 50
WHERE NOT EXISTS (
  SELECT 1 FROM contract_rule_definition WHERE tenant_id = 0 AND rule_code = 'AMOUNT_CONSISTENCY'
);

INSERT INTO contract_rule_definition (tenant_id, rule_code, rule_name, rule_type, field_code, severity, enabled, rule_params, sort_order)
SELECT 0, 'DATE_ORDER', '日期顺序校验', 'DATE_ORDER', 'payment_date', 'MEDIUM', 1, '{"leftField":"effective_date","rightField":"payment_date"}', 60
WHERE NOT EXISTS (
  SELECT 1 FROM contract_rule_definition WHERE tenant_id = 0 AND rule_code = 'DATE_ORDER'
);

INSERT INTO contract_rule_definition (tenant_id, rule_code, rule_name, rule_type, field_code, severity, enabled, rule_params, sort_order)
SELECT 0, 'AMOUNT_THRESHOLD', '金额阈值校验', 'AMOUNT_THRESHOLD', 'contract_amount', 'MEDIUM', 1, '{"maxAmount":"1000000"}', 70
WHERE NOT EXISTS (
  SELECT 1 FROM contract_rule_definition WHERE tenant_id = 0 AND rule_code = 'AMOUNT_THRESHOLD'
);

INSERT INTO contract_rule_definition (tenant_id, rule_code, rule_name, rule_type, field_code, severity, enabled, rule_params, sort_order)
SELECT 0, 'LIABILITY_CONFLICT', '责任条款冲突校验', 'LIABILITY_CONFLICT', 'liability_clause', 'HIGH', 1, NULL, 80
WHERE NOT EXISTS (
  SELECT 1 FROM contract_rule_definition WHERE tenant_id = 0 AND rule_code = 'LIABILITY_CONFLICT'
);
-- --------------------

--
--