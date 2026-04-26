CREATE DATABASE IF NOT EXISTS legal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE legal;

CREATE TABLE IF NOT EXISTS legal_user (
  user_id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 1001,
  username VARCHAR(64) NOT NULL,
  password_hash CHAR(64) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  role_code VARCHAR(16) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  last_login_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_legal_user_tenant_username (tenant_id, username),
  KEY idx_legal_user_tenant_role (tenant_id, role_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
  session_id VARCHAR(64) NOT NULL,
  tenant_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_active_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (session_id),
  KEY idx_session_tenant_owner (tenant_id, owner_user_id, last_active_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_message (
  message_id BIGINT NOT NULL AUTO_INCREMENT,
  session_id VARCHAR(64) NOT NULL,
  role VARCHAR(16) NOT NULL,
  content TEXT NOT NULL,
  token_usage INT DEFAULT 0,
  latency_ms INT DEFAULT 0,
  trace_id VARCHAR(64) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (message_id),
  KEY idx_message_session_time (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kb_document (
  document_id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  source VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  doc_version INT NOT NULL DEFAULT 1,
  index_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (document_id),
  KEY idx_doc_tenant_owner_status (tenant_id, owner_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kb_chunk (
  chunk_id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  document_id BIGINT NOT NULL,
  doc_version INT NOT NULL,
  chunk_order INT NOT NULL,
  content MEDIUMTEXT NOT NULL,
  content_hash VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (chunk_id),
  KEY idx_chunk_doc_ver_order (document_id, doc_version, chunk_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kb_index_outbox (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  document_id BIGINT NOT NULL,
  doc_version INT NOT NULL,
  op VARCHAR(16) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  retry_count INT NOT NULL DEFAULT 0,
  next_retry_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_doc_version_op (document_id, doc_version, op),
  KEY idx_outbox_status_retry (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS qa_feedback (
  feedback_id BIGINT NOT NULL AUTO_INCREMENT,
  message_id BIGINT NOT NULL,
  helpful TINYINT(1) NOT NULL,
  comment VARCHAR(1000) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (feedback_id),
  KEY idx_feedback_message (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS retrieval_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  trace_id VARCHAR(64) NOT NULL,
  tenant_id BIGINT NOT NULL,
  query_text TEXT NOT NULL,
  hit_chunk_ids VARCHAR(1024) DEFAULT NULL,
  rerank_score DECIMAL(10,4) DEFAULT NULL,
  model_name VARCHAR(128) DEFAULT NULL,
  latency_ms INT DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_retrieval_trace (trace_id),
  KEY idx_retrieval_tenant_time (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
