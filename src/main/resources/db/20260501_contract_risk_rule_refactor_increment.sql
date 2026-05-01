USE legal;

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

SET @sql := IF(
  EXISTS(
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'contract_rule_definition'
      AND column_name = 'rule_source_type'
  ),
  'SELECT 1',
  "ALTER TABLE contract_rule_definition ADD COLUMN rule_source_type VARCHAR(32) NOT NULL DEFAULT 'STRUCTURED' COMMENT '规则来源类型（STRUCTURED/MANUAL_TEXT/IMPORTED_DOCUMENT）' AFTER rule_type"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'contract_rule_definition'
      AND column_name = 'document_id'
  ),
  'SELECT 1',
  "ALTER TABLE contract_rule_definition ADD COLUMN document_id BIGINT DEFAULT NULL COMMENT '关联的风险规则文档ID' AFTER field_code"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'contract_rule_definition'
      AND column_name = 'hit_threshold'
  ),
  'SELECT 1',
  "ALTER TABLE contract_rule_definition ADD COLUMN hit_threshold DECIMAL(6,4) DEFAULT NULL COMMENT '检索型规则命中阈值' AFTER enabled"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'contract_rule_definition'
      AND column_name = 'rule_content'
  ),
  'SELECT 1',
  "ALTER TABLE contract_rule_definition ADD COLUMN rule_content MEDIUMTEXT DEFAULT NULL COMMENT '手工录入的规则原文' AFTER hit_threshold"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'contract_rule_definition'
      AND index_name = 'idx_contract_rule_document'
  ),
  'SELECT 1',
  'CREATE INDEX idx_contract_rule_document ON contract_rule_definition (tenant_id, rule_source_type, enabled, document_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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

INSERT INTO contract_rule_definition (
  tenant_id,
  rule_code,
  rule_name,
  rule_type,
  rule_source_type,
  field_code,
  document_id,
  severity,
  enabled,
  hit_threshold,
  rule_content,
  rule_params,
  sort_order
)
SELECT d.tenant_id,
       CONCAT('RISK_RULE_DOC_', d.document_id),
       d.title,
       'DOCUMENT_RETRIEVAL',
       'IMPORTED_DOCUMENT',
       NULL,
       d.document_id,
       'MEDIUM',
       1,
       0.7800,
       NULL,
       NULL,
       9000
FROM kb_document d
WHERE d.biz_type = 'RISK_RULE'
  AND d.status <> 'DELETED'
  AND NOT EXISTS (
    SELECT 1
    FROM contract_rule_definition r
    WHERE r.document_id = d.document_id
      AND r.rule_type = 'DOCUMENT_RETRIEVAL'
  );
