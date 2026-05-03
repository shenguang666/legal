ALTER TABLE kb_document
  ADD COLUMN cleaning_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否在本次文档版本解析中启用内容清洗，1启用、0关闭' AFTER parse_failure_reason;

ALTER TABLE kb_document_parse_task
  ADD COLUMN cleaning_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否在该解析任务中启用内容清洗，1启用、0关闭' AFTER parse_status;

CREATE TABLE IF NOT EXISTS kb_document_cleaning_log (
  cleaning_log_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档清洗日志主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  document_id BIGINT NOT NULL COMMENT '文档ID',
  doc_version INT NOT NULL COMMENT '文档版本号',
  parse_method VARCHAR(32) NOT NULL COMMENT '文档解析方式（NATIVE/MINERU_PRECISE）',
  content_format VARCHAR(32) NOT NULL COMMENT '清洗内容格式（TEXT纯文本/MARKDOWN Markdown）',
  original_chars INT NOT NULL DEFAULT 0 COMMENT '清洗前字符数',
  cleaned_chars INT NOT NULL DEFAULT 0 COMMENT '清洗后字符数',
  removed_line_count INT NOT NULL DEFAULT 0 COMMENT '被清洗删除的行数',
  removed_chunk_count INT NOT NULL DEFAULT 0 COMMENT '被过滤删除的低质量切片数量',
  reason_summary_json TEXT COMMENT '清洗原因统计JSON',
  removed_samples_json MEDIUMTEXT COMMENT '被清洗内容样例JSON，按配置限制数量和长度',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (cleaning_log_id),
  KEY idx_cleaning_log_document (tenant_id, document_id, doc_version),
  KEY idx_cleaning_log_created (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档内容清洗日志表';
