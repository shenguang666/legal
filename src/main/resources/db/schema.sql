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

CREATE TABLE IF NOT EXISTS kb_document (
  document_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  owner_user_id BIGINT NOT NULL COMMENT '所属用户ID',
  title VARCHAR(255) NOT NULL COMMENT '文档标题',
  source VARCHAR(255) NOT NULL COMMENT '文档来源',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '文档状态（PENDING/PROCESSING/ACTIVE/DELETED）',
  doc_version INT NOT NULL DEFAULT 1 COMMENT '文档版本号',
  index_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '索引状态（PENDING/PROCESSING/COMPLETED/FAILED）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (document_id),
  KEY idx_doc_tenant_owner_status (tenant_id, owner_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

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
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_retrieval_trace (trace_id),
  KEY idx_retrieval_tenant_time (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检索日志表';


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
  reviewed_at DATETIME DEFAULT NULL COMMENT '复核时间',
  PRIMARY KEY (knowledge_id),
  KEY idx_user_knowledge_owner (tenant_id, user_id, created_at),
  KEY idx_user_knowledge_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户外挂知识库表';


-- END MEMORY TABLES

-- 注意：短期记忆不落 ES，仅用于对话上下文与消息列表缓存（Redis -> MySQL）。
-- 删除语义：先删 MySQL 再删 Redis，保持最终一致性。

-- 如需扩展：可增加 memory_knowledge_candidate（外挂知识候选）等表。

-- --------------------
-- End of schema
-- --------------------

--
--