ALTER TABLE kb_document
  ADD COLUMN parse_method VARCHAR(32) NOT NULL DEFAULT 'NATIVE' COMMENT '文档解析方式（NATIVE原生解析/MINERU_PRECISE MinerU精准解析）' AFTER index_status,
  ADD COLUMN parse_status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED' COMMENT '文档解析状态（PENDING待解析/PROCESSING解析中/COMPLETED完成/FAILED失败）' AFTER parse_method,
  ADD COLUMN parse_failure_reason VARCHAR(1000) DEFAULT NULL COMMENT '文档解析失败原因，用于前端展示和排查' AFTER parse_status,
  ADD COLUMN mineru_batch_id VARCHAR(128) DEFAULT NULL COMMENT 'MinerU批次ID，用于关联外部精准解析任务' AFTER parse_failure_reason,
  ADD COLUMN mineru_data_id VARCHAR(128) DEFAULT NULL COMMENT 'MinerU文件数据ID，用于关联批次内单个文件' AFTER mineru_batch_id,
  ADD COLUMN mineru_full_zip_url VARCHAR(1000) DEFAULT NULL COMMENT 'MinerU完整解析结果压缩包地址，仅后端使用' AFTER mineru_data_id,
  ADD COLUMN parse_started_at DATETIME DEFAULT NULL COMMENT '最近一次解析开始时间' AFTER mineru_full_zip_url,
  ADD COLUMN parse_completed_at DATETIME DEFAULT NULL COMMENT '最近一次解析完成时间' AFTER parse_started_at;

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
