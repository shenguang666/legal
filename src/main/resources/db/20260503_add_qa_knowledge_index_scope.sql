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
