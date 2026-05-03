# 部署与回滚说明

## Token 配置

- MinerU Token 不写入仓库配置文件，只通过环境变量 `LEGAL_MINERU_API_TOKEN` 注入。
- 启用 MinerU 时设置 `LEGAL_MINERU_ENABLED=true`，并配置 `LEGAL_MINERU_API_TOKEN`。
- 未设置 Token 或 `LEGAL_MINERU_ENABLED=false` 时，后端能力接口只返回原生解析方式，前端不会暴露 MinerU 选项。

## 数据库变更

- 新环境可直接使用 `src/main/resources/db/schema.sql`。
- 现有环境执行 `src/main/resources/db/20260502_add_mineru_document_parsing.sql`，为 `kb_document` 增加解析字段，并创建 `kb_document_parse_task`。
- 执行前如已有同名字段，需要按实际库结构调整 `ALTER TABLE`，避免重复添加字段。

## 运行模式

- 默认模式：`LEGAL_MINERU_ENABLED=false`，保持原生 Tika 抽取与本地切片兼容。
- MinerU 模式：`LEGAL_MINERU_ENABLED=true` 且设置 Token，上传时可选择 `MINERU_PRECISE`，后台 worker 提交 MinerU、轮询结果并写入切片。
- Worker 可通过 `LEGAL_DOCUMENT_PARSE_WORKER_ENABLED=false` 临时关闭。

## 回滚步骤

- 将 `LEGAL_MINERU_ENABLED=false`，前端会自动隐藏 MinerU 解析选项。
- 如需停止异步解析，将 `LEGAL_DOCUMENT_PARSE_WORKER_ENABLED=false`。
- 已创建的解析任务可保留，不影响原生解析链路；如需彻底回滚数据库，需评估并手动移除新增字段和 `kb_document_parse_task` 表。
