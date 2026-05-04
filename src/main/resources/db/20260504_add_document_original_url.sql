ALTER TABLE kb_document
  ADD COLUMN owner_username VARCHAR(64) DEFAULT NULL COMMENT '上传用户名快照' AFTER owner_user_id,
  ADD COLUMN document_url VARCHAR(1500) DEFAULT NULL COMMENT '文档可访问地址，用于前端查看原文档或MinerU解析原文' AFTER mineru_full_zip_url,
  ADD COLUMN document_oss_bucket VARCHAR(128) DEFAULT NULL COMMENT '文档解析产物所在OSS Bucket名称' AFTER document_url,
  ADD COLUMN document_oss_prefix VARCHAR(1000) DEFAULT NULL COMMENT '文档解析产物所在OSS对象前缀' AFTER document_oss_bucket;
