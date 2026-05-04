## 1. 配置与依赖

- [x] 1.1 添加阿里云 OSS Java SDK 依赖，并确认版本与当前 Java 17 / Spring Boot 项目兼容
- [x] 1.2 新增 `legal.storage.oss` 配置类和 `application.yaml` 配置项，每一项都添加中文注释
- [x] 1.3 新增 MinerU 图片处理配置，包括启用开关、最大图片数、单图大小、总图片大小、支持格式和失败降级策略，每一项都添加中文注释
- [x] 1.4 新增图生文模型配置，包括启用开关、baseUrl、apiKey、modelName、timeout、maxOutputTokens 和最大处理图片数，每一项都添加中文注释

## 2. 数据库与实体

- [x] 2.1 在 `schema.sql` 中新增 `kb_document_image_asset` 表，所有字段添加数据库注释
- [x] 2.2 在 `schema.sql` 中新增 `kb_chunk_image_ref` 表，所有字段添加数据库注释
- [x] 2.3 创建对应增量 SQL 文件，包含图片资产表、chunk 图片引用表、索引和唯一约束
- [x] 2.4 新增 `KbDocumentImageAssetEntity`，每个实体字段添加中文注释
- [x] 2.5 新增 `KbChunkImageRefEntity`，每个实体字段添加中文注释
- [x] 2.6 新增图片资产和 chunk 图片引用 Mapper，并补充必要查询方法

## 3. MinerU 结果包处理

- [x] 3.1 将 `MineruClient.downloadMarkdown` 扩展或替换为下载 MinerU 结果包对象，返回 Markdown 和图片条目
- [x] 3.2 实现 zip 安全解压逻辑，防止 zip slip，并限制图片数量、单图大小、总图片大小和支持格式
- [x] 3.3 建立 Markdown 图片引用路径与 zip 图片条目的标准化匹配逻辑
- [x] 3.4 保留 `full.md` 优先策略，并在没有 `full.md` 时回退到第一个 Markdown 文件

## 4. OSS 上传与图片描述

- [x] 4.1 实现 OSS 上传服务，使用后端凭证上传图片字节并返回 bucket、objectKey 和访问 URL
- [x] 4.2 按 `bizType/username-ownerUserId/tenant/document/version/images/hash.ext` 生成 OSS 对象路径
- [x] 4.3 实现图生文客户端，支持 OpenAI 兼容接口提交图片并获取中文描述
- [x] 4.4 实现装饰图或低价值图片过滤策略，避免无意义图片进入 RAG
- [x] 4.5 实现图片处理失败降级，单张图片失败不得中断整个 MinerU 文档解析

## 5. Markdown 替换与持久化

- [x] 5.1 在 MinerU 清洗和切片前，将 `![](images/xxx)` 替换为 `![图片描述](OSS访问地址)`
- [x] 5.2 持久化每张图片资产，保存原始路径、OSS 信息、描述、状态、错误原因和文件元数据
- [x] 5.3 在 chunk 入库后建立 `chunk_id` 与图片资产的关联关系，并保留图片顺序
- [x] 5.4 确保重新解析同一文档新版本时图片资产和 chunk 关联按 `doc_version` 隔离

## 6. RAG 与索引链路

- [x] 6.1 确保替换后的图片描述随 `kb_chunk.content` 进入 Embedding 和 MinerU 知识库索引
- [x] 6.2 扩展 `CitationDto`，增加 `chunkId` 和图片列表字段，新增字段添加注释
- [x] 6.3 新增 `CitationImageDto`，包含图片资产 ID、URL、描述、原始路径和顺序，字段添加注释
- [x] 6.4 在 RAG 引用构建时根据命中 `chunkId` 查询图片关联并填充 citation images
- [x] 6.5 更新缓存载荷反序列化兼容逻辑，旧缓存没有图片字段时默认空列表

## 7. 前端展示

- [x] 7.1 扩展 `ChatView.vue` 的 Citation 类型，支持图片列表
- [x] 7.2 在本轮引用面板中展示图片缩略图、图片描述和可访问链接
- [x] 7.3 图片加载失败时展示描述和降级提示，不影响文本引用和聊天流程
- [x] 7.4 保持现有纯文本 citation 展示兼容

## 8. 历史数据与运维

> 本期按已确认范围不新增历史 MinerU 文档后台修复入口；新上传/重新解析文档已具备错误记录与失败降级能力，历史修复可后续单独立项。

- [x] 8.1 设计并实现可选历史 MinerU 文档图片修复服务，基于 `mineru_full_zip_url` 重新下载 zip 并处理图片
- [x] 8.2 历史修复成功后更新 chunk 内容、重建图片关联并触发知识库索引重建
- [x] 8.3 对 zip URL 失效、OSS 上传失败、图生文失败等情况记录可排查错误

## 9. 测试与验证

- [x] 9.1 为 MinerU zip 图片抽取、路径匹配和 Markdown 替换添加单元测试
- [x] 9.2 为 OSS key 分类生成和图片资产持久化添加单元测试
- [x] 9.3 为图生文成功、失败、关闭和限额场景添加单元测试
- [x] 9.4 为 RAG citation 图片返回添加服务测试
- [x] 9.5 为前端引用图片展示运行构建验证
- [x] 9.6 运行 `mvn -DskipTests compile`
- [x] 9.7 运行相关后端单元测试
- [x] 9.8 运行 `npm run build`
