## ADDED Requirements

### Requirement: RAG 引用 SHALL 返回命中切片关联的图片证据
当检索命中的切片包含已处理的 MinerU 图片时，系统 SHALL 在 RAG 引用中返回与该切片关联的图片证据。

#### Scenario: Retrieved chunk has associated image assets
- **WHEN** RAG 检索返回的切片关联了一个或多个 MinerU 图片资产
- **THEN** 引用载荷 SHALL 为每个关联图片包含图片 URL、图片描述和稳定的图片资产标识
- **THEN** 引用载荷 SHALL 保留图片在切片中的顺序

#### Scenario: Retrieved chunk has no associated image assets
- **WHEN** RAG 检索返回的切片没有图片关联
- **THEN** 引用载荷 SHALL 保持与现有纯文本引用兼容
- **THEN** 图片列表 SHALL 为空或省略，且不得导致前端错误

### Requirement: Chat UI SHALL display citation images
系统 SHALL 将 RAG 引用返回的图片与文本片段一起展示，使用户能够查看回答使用的视觉证据。

#### Scenario: Citation contains images
- **WHEN** 前端收到包含一个或多个图片的引用
- **THEN** 引用面板 SHALL 在引用文本下方渲染图片缩略图或预览
- **THEN** 每张图片 SHALL 使用生成的图片描述作为可访问 alt 文本

#### Scenario: Citation image URL is unavailable
- **WHEN** 图片 URL 缺失、过期或加载失败
- **THEN** 前端 SHALL 继续展示引用文本和图片描述
- **THEN** 前端 SHALL 不得中断聊天交互
