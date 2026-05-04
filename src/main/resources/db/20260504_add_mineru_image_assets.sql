CREATE TABLE IF NOT EXISTS kb_document_image_asset (
  image_asset_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片资产主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  document_id BIGINT NOT NULL COMMENT '文档ID',
  doc_version INT NOT NULL COMMENT '文档版本号',
  biz_type VARCHAR(32) NOT NULL COMMENT '文档业务类型（KNOWLEDGE/RISK_RULE/TIANYAN_REVIEW）',
  owner_user_id BIGINT NOT NULL COMMENT '所属用户ID',
  username VARCHAR(64) NOT NULL COMMENT '上传用户名快照',
  original_path VARCHAR(1000) NOT NULL COMMENT 'MinerU结果包内图片原始相对路径',
  oss_bucket VARCHAR(128) DEFAULT NULL COMMENT 'OSS Bucket名称',
  oss_object_key VARCHAR(1000) DEFAULT NULL COMMENT 'OSS对象Key',
  public_url VARCHAR(1500) DEFAULT NULL COMMENT '图片公开访问地址或后端生成的可访问地址',
  mime_type VARCHAR(64) DEFAULT NULL COMMENT '图片MIME类型',
  file_ext VARCHAR(16) DEFAULT NULL COMMENT '图片文件扩展名',
  size_bytes BIGINT NOT NULL DEFAULT 0 COMMENT '图片文件大小字节数',
  content_hash VARCHAR(64) NOT NULL COMMENT '图片内容SHA-256哈希',
  description VARCHAR(1000) DEFAULT NULL COMMENT '图生文模型生成的图片描述',
  caption_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '图片描述状态（PENDING待处理/SUCCESS成功/FAILED失败/SKIPPED跳过）',
  caption_error VARCHAR(1000) DEFAULT NULL COMMENT '图片描述失败或跳过原因',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (image_asset_id),
  UNIQUE KEY uk_image_asset_doc_path_hash (tenant_id, document_id, doc_version, original_path(255), content_hash),
  KEY idx_image_asset_doc_version (tenant_id, document_id, doc_version),
  KEY idx_image_asset_hash (tenant_id, content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MinerU文档图片资产表';

CREATE TABLE IF NOT EXISTS kb_chunk_image_ref (
  chunk_image_ref_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '切片图片引用主键ID',
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  chunk_id BIGINT NOT NULL COMMENT '切片ID',
  image_asset_id BIGINT NOT NULL COMMENT '图片资产ID',
  image_order INT NOT NULL DEFAULT 0 COMMENT '图片在切片中的出现顺序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (chunk_image_ref_id),
  UNIQUE KEY uk_chunk_image_ref_order (tenant_id, chunk_id, image_asset_id, image_order),
  KEY idx_chunk_image_ref_chunk (tenant_id, chunk_id, image_order),
  KEY idx_chunk_image_ref_asset (tenant_id, image_asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库切片图片引用表';
