ALTER TABLE kb_chunk
  ADD COLUMN chunk_type VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT '切片类型（NORMAL普通分块/PARENT父分块/CHILD子分块）' AFTER chunk_order,
  ADD COLUMN parent_chunk_id BIGINT DEFAULT NULL COMMENT '父分块ID，仅子分块填写，普通分块和父分块为空' AFTER chunk_type;

UPDATE kb_chunk
SET chunk_type = 'NORMAL'
WHERE chunk_type IS NULL OR chunk_type = '';

ALTER TABLE kb_chunk
  ADD KEY idx_chunk_doc_ver_type_order (tenant_id, document_id, doc_version, chunk_type, chunk_order),
  ADD KEY idx_chunk_parent (tenant_id, parent_chunk_id);

ALTER TABLE retrieval_log
  MODIFY COLUMN hit_chunk_ids VARCHAR(1024) DEFAULT NULL COMMENT '最终进入大模型上下文的切片ID列表',
  ADD COLUMN raw_hit_chunk_ids VARCHAR(1024) DEFAULT NULL COMMENT 'Elasticsearch原始命中的切片ID列表' AFTER hit_chunk_ids;

ALTER TABLE rag_retrieval_metric_evaluation
  ADD COLUMN final_hit_chunk_ids TEXT COMMENT '最终进入大模型上下文的切片ID列表（JSON数组）' AFTER original_hit_chunk_ids;
