## Context

当前 MinerU 精准解析链路在 `MineruClient.downloadMarkdown(fullZipUrl)` 中下载 zip 后只读取 Markdown 文件，未抽取 zip 内 `images/` 图片资源。实际数据库中已有 MinerU 切片包含 `![](images/xxx.jpg)` 相对路径，这类路径离开 zip 后无法访问，RAG 命中后 `CitationDto` 也只返回 `documentId/source/fragment` 文本，前端只能展示文字片段。

本变更需要跨越文档解析、对象存储、图生文模型、MySQL 元数据、Elasticsearch 索引、RAG 引用和前端展示。阿里云 OSS Java SDK v2 支持通过 `OSSClient.newBuilder()` 配置 region/endpoint/credentials 后调用 `putObject` 上传对象；凭证应通过环境变量或后端配置注入，不得暴露给前端。

## Goals / Non-Goals

**Goals:**
- MinerU 结果 zip 下载后同时抽取 Markdown 与图片资源。
- 将图片上传到 OSS，并按业务类型、用户名、租户、文档、版本分类保存。
- 为图片生成中文描述，将 Markdown 中 `![](images/xxx.jpg)` 替换为 `![图片描述](OSS_URL)`。
- 将图片资产和 chunk 关联关系持久化，使 RAG 命中 chunk 时可以返回图片 URL 与描述。
- 让图片描述参与切片、Embedding、BM25 和 RAG 上下文，提升图片类证据可检索性。
- 前端在本轮引用中展示命中图片证据。
- 支持配置开关、数量/大小限制和失败降级，避免 OSS 或图生文调用成本失控。

**Non-Goals:**
- 不改变原生解析文档的处理方式。
- 不在前端直传 OSS 或暴露 OSS/MinerU/图生文 API Key。
- 不强制一次性修复所有历史 MinerU 文档；历史修复作为可选任务或管理接口。
- 不在第一阶段实现复杂图片检索排序或单独图片搜索入口。
- 不要求答案正文自动内嵌图片；第一阶段优先在引用面板展示图片。

## Decisions

### 1. MinerU 下载结果从字符串升级为结果包对象

将 MinerU zip 下载逻辑从“只返回 Markdown 字符串”升级为“返回 Markdown + 图片条目”的结果包，例如 `MineruParsePackage`。

原因：图片字节只在 zip 下载时可完整获取，若继续只返回 Markdown，后续服务无法上传 OSS 或生成图片描述。

备选方案：在 Markdown 中保留相对路径，RAG 命中时再回源下载 zip 并解图。该方案会把用户问答延迟和 MinerU CDN 可用性耦合，且 zip 可能过期，因此不采用。

### 2. OSS 对象路径使用业务类型和用户维度分类

推荐 OSS key 结构：

```text
mineru-assets/{bizCategory}/{username}-{ownerUserId}/tenant-{tenantId}/doc-{documentId}/v{docVersion}/images/{sha256}.{ext}
```

业务分类映射：

| bizType | bizCategory |
|---|---|
| KNOWLEDGE | knowledge |
| TIANYAN_REVIEW | tianyan-review |
| RISK_RULE | risk-rule |

原因：满足业务隔离、用户维度排查和后续生命周期管理需要。用户名需要路径安全化，且建议附加 `ownerUserId` 以避免用户名变更或重复造成歧义。

### 3. Markdown 使用可读 URL，同时 MySQL 保存结构化资产

Markdown 替换为：

```markdown
![图片描述](https://cdn.example.com/mineru-assets/.../image.jpg)
```

同时新增图片资产表和 chunk 图片引用表。

原因：Markdown 中的描述可直接进入 embedding/BM25；结构化表可在 RAG 引用阶段可靠返回图片列表，并支持私有 OSS 签名 URL、重建索引、历史修复和审计。

### 4. 图生文作为可降级增强能力

新增独立图生文配置，优先使用 OpenAI 兼容接口或阿里 DashScope 兼容接口。图片描述失败时不应导致整个文档解析失败，系统可以使用默认描述或跳过装饰性图片。

原因：图生文模型可能未配置、限流或成本较高。文档文本解析仍是主流程，图片增强应降级而非阻塞。

### 5. RAG 图片返回从 chunkId 查 MySQL 资产表

Elasticsearch 仍主要索引 `content` 和向量，不强制在 ES 中保存完整图片 metadata。RAG 命中 chunk 后，后端通过 `chunkId` 查询 `kb_chunk_image_ref` 和 `kb_document_image_asset`，构造 `CitationDto.images`。

原因：MySQL 是图片资产权威来源；ES mapping 改动越少，迁移和重建风险越低。图片描述已进入 `content`，可满足召回需求。

### 6. 前端优先在引用面板展示图片

`ChatView` 的 Citation 类型增加 `images`，引用面板在文本 fragment 下展示缩略图、描述和来源链接。

原因：当前流式答案正文只拼接文本，引用面板已经是“本轮命中片段实时展示”的位置，改造成本最小且符合证据追溯场景。

## Risks / Trade-offs

- **MinerU zip 中图片路径与 Markdown 引用不一致** → 需要以 zip entry 标准化路径建立映射，支持 `images/x.jpg`、`./images/x.jpg`、URL 编码路径和大小写差异。
- **Zip slip 或异常大 zip/图片导致安全与内存风险** → 解压时校验路径、限制总图片数量、单图大小、总图片字节数和支持格式。
- **OSS 上传成功但后续解析失败造成孤儿对象** → 资产表记录状态，必要时异步清理；对象 key 使用内容 hash 支持幂等覆盖。
- **图生文成本和延迟较高** → 增加开关、最大处理图片数、超时、失败降级和装饰图过滤。
- **公开 URL 与私有桶策略冲突** → 第一阶段支持 `public-base-url`；若使用私有桶，Citation 返回时动态生成临时 URL，缓存中避免保存长期签名 URL。
- **历史文档 zip URL 失效** → 历史修复只能处理仍可下载 zip 的文档；失败时提示需重新上传或重新解析。
- **Citation 缓存兼容性** → `images` 字段默认空数组，旧缓存反序列化必须兼容缺失字段。

## Migration Plan

1. 新增 OSS、图生文和 MinerU 图片处理配置，所有 YAML 项加中文注释。
2. 新增 `kb_document_image_asset` 和 `kb_chunk_image_ref` 表、实体和 Mapper，所有表字段和实体字段加注释。
3. 扩展 MinerU zip 读取逻辑，新增结果包对象和图片条目对象。
4. 新增 OSS 上传服务和图生文服务，支持开关、限制、降级和错误记录。
5. 在 MinerU 解析成功后、清洗和切片前执行图片上传、描述生成和 Markdown 替换。
6. 持久化 chunk 后根据 chunk 内容或处理过程中的图片引用映射写入 chunk-image 关系。
7. 扩展 RAG Citation DTO、AnswerCachePayload 和前端 Citation 类型，返回并展示图片列表。
8. 为新上传 MinerU 文档验证图片 URL 可访问、图片描述可检索、RAG 引用可展示。
9. 可选新增历史修复入口或任务，处理仍可下载 zip 的 MinerU 文档并触发重新索引。

## Rollback Strategy

- 关闭 MinerU 图片处理开关后，系统回退为只处理 Markdown 文本，不影响原生解析和现有 MinerU 文本解析。
- 若 OSS 或图生文不可用，解析链路应降级保留 Markdown 文本并记录失败原因。
- 数据库新增表可保留为空，不影响旧逻辑；Citation `images` 缺失时前端按空数组处理。

## Open Questions

- OSS bucket 是否公开访问，还是必须通过后端生成临时签名 URL？ answer：建议使用私有桶并通过后端生成临时签名 URL，增强安全性和访问控制；前端只处理短期有效的 URL，避免长期暴露。
- 图生文模型最终使用 `qwen-vl-plus/qwen-vl-max` 还是现有 OpenAI 兼容网关中的其他视觉模型？ answer：建议优先使用 `qwen3.5-omni-plus`全模态模型。后续可调整为更适合图生文描述的专用模型，如 `qwen-vl-plus`，但需要评估模型性能、成本和描述质量。
- 历史 MinerU 文档是否需要提供管理端批量修复按钮，还是仅提供后端任务/API？ answer：暂时不需要
- 装饰性图片过滤阈值是否按尺寸、文件大小、caption 内容共同判断？  answer：建议先按尺寸和文件大小过滤，再对剩余图片生成描述并根据描述内容判断是否装饰性图片。
