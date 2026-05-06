## Requirements

### Requirement: RAG 检索命中子分块后使用父分块构建上下文
系统 SHALL 在 RAG 检索阶段区分 Elasticsearch 原始命中切片和最终进入大模型上下文的切片；当原始命中为子分块时，系统 MUST 批量加载其父分块，并使用父分块替代子分块进入大模型上下文。

#### Scenario: 子分块命中后批量加载父分块
- **WHEN** Elasticsearch 检索结果包含一个或多个带有 `parent_chunk_id` 的子分块
- **THEN** 系统 MUST 使用去重集合收集这些父分块 ID
- **THEN** 系统 MUST 按租户批量查询对应父分块
- **THEN** 系统 MUST 使用父分块内容构建大模型上下文，而不是使用子分块内容

#### Scenario: 多个子分块指向同一父分块
- **WHEN** Elasticsearch 原始命中列表中多个子分块指向同一个父分块
- **THEN** 系统 MUST 只将该父分块加入最终上下文一次
- **THEN** 系统 MUST 按该父分块首次被子分块命中的顺序保留上下文排序

#### Scenario: 普通分块命中
- **WHEN** Elasticsearch 检索结果包含普通分块
- **THEN** 系统 MUST 直接将该普通分块作为最终上下文候选
- **THEN** 系统 MUST 不要求该普通分块存在父分块 ID

#### Scenario: 父分块缺失
- **WHEN** 子分块携带的 `parent_chunk_id` 无法在当前租户下查询到父分块
- **THEN** 系统 MUST 不将该子分块内容回退发送给大模型
- **THEN** 系统 MUST 跳过该异常父分块并记录可排查日志

### Requirement: 同时保留原始命中和最终上下文命中
系统 SHALL 在 RAG 回答、缓存和检索日志中保留最终进入大模型上下文的切片 ID，并在新增字段中记录 Elasticsearch 原始命中的切片 ID。

#### Scenario: 写入非流式 RAG 检索日志
- **WHEN** 非流式 RAG 回答完成并写入 `retrieval_log`
- **THEN** `hit_chunk_ids` MUST 记录最终进入大模型上下文的普通分块或父分块 ID
- **THEN** `raw_hit_chunk_ids` MUST 记录 Elasticsearch 原始命中的普通分块或子分块 ID

#### Scenario: 写入流式 RAG 检索日志
- **WHEN** 流式 RAG 回答完成并写入 `retrieval_log`
- **THEN** `hit_chunk_ids` MUST 记录最终进入大模型上下文的普通分块或父分块 ID
- **THEN** `raw_hit_chunk_ids` MUST 记录 Elasticsearch 原始命中的普通分块或子分块 ID

#### Scenario: 写入回答缓存
- **WHEN** 系统将 RAG 回答写入回答缓存
- **THEN** 缓存中的命中切片 ID MUST 使用最终进入大模型上下文的普通分块或父分块 ID
- **THEN** 系统 MUST 保持缓存命中后的回答复盘语义与原始回答一致

### Requirement: 引用证据使用最终上下文分块
系统 SHALL 基于最终进入大模型上下文的分块构建回答引用，避免向用户展示未发送给大模型的子分块作为主要上下文证据。

#### Scenario: 子分块被父分块替换后生成引用
- **WHEN** 原始命中子分块被父分块替换为最终上下文
- **THEN** 系统 MUST 使用父分块的 `chunk_id`、文档 ID、来源和内容摘要生成引用
- **THEN** 系统 MUST 按父分块 ID 查询关联图片证据

#### Scenario: 普通分块直接生成引用
- **WHEN** 普通分块进入最终上下文
- **THEN** 系统 MUST 使用该普通分块生成引用
- **THEN** 系统 MUST 保持现有引用内容和图片证据查询行为

### Requirement: 鏅鸿兘灏忔硶搴涵瀹′笂涓嬫枃寮哄埗浣跨敤鐖剁骇璇箟鍧?绯荤粺 SHALL 鍦?`bizType=SMART_COURT` 鐨?RAG 妫€绱㈣皟鐢ㄨ矾寰勪笂寮哄埗鍚敤鐖剁骇涓婁笅鏂囨墿灞曪紝MUST NOT 鎶婂瓙鍒嗗潡鍐呭鐩存帴浣滀负 LLM 搴涓婁笅鏂囷紝MUST 鍦ㄥ涵瀹¤瘉鎹紩鐢ㄨ褰曚腑鍚屾椂淇濈暀鐖跺垎鍧?ID 涓庡懡涓殑瀛愬垎鍧?ID 鐢ㄤ簬瀹¤銆?
#### Scenario: 搴 Agent 妫€绱㈠己鍒跺惎鐢ㄧ埗绾ф墿灞?- **WHEN** 浠绘剰鏅鸿兘灏忔硶搴鑹?Agent锛堟硶瀹樸€佸鏂逛唬鐞嗕汉銆佺敤鎴疯緟鍔╁緥甯堬級閫氳繃 RAG 妫€绱㈡帴鍙ｅ彫鍥炶瘉鎹紝涓?`bizType=SMART_COURT`
- **THEN** 绯荤粺 MUST 鍚敤鐖剁骇涓婁笅鏂囨墿灞曡兘鍔涳紝鎶婂懡涓殑瀛愬垎鍧楁浛鎹负瀵瑰簲鐖跺垎鍧楄繘鍏?LLM 涓婁笅鏂?- **THEN** 绯荤粺 MUST NOT 鎺ュ彈璋冪敤鏂瑰叧闂埗绾ф墿灞曠殑鍙傛暟

#### Scenario: 搴璇佹嵁寮曠敤鍚屾椂璁板綍鐖跺瓙鍒嗗潡 ID
- **WHEN** 搴鏌愯疆 AI 杈撳嚭寮曠敤浜嗚鐖剁骇鎵╁睍鏇挎崲鍚庣殑鍒嗗潡
- **THEN** 绯荤粺 MUST 鍦?`court_argument.evidence_refs` 涓?`court_case_evidence` 涓悓鏃朵繚瀛?`parent_chunk_id` 涓庡師濮嬪懡涓殑 `child_chunk_id`
- **THEN** 绯荤粺 MUST 鍦ㄥ璁℃棩蹇椾腑淇濈暀鐖跺瓙鏄犲皠锛屼究浜庡悗缁瘉鎹洖婧?
#### Scenario: 鐖跺垎鍧楃己澶辨椂绂佹鍥為€€鍒板瓙鍒嗗潡
- **WHEN** 搴妫€绱㈢殑瀛愬垎鍧?`parent_chunk_id` 鍦ㄥ綋鍓嶇鎴蜂笅鎵句笉鍒板搴旂埗鍒嗗潡
- **THEN** 绯荤粺 MUST 璺宠繃璇ュ懡涓€屼笉鏄洖閫€鍒板瓙鍒嗗潡浣滀负搴涓婁笅鏂?- **THEN** 绯荤粺 MUST 璁板綍鍙帓鏌ユ棩蹇楋紝骞跺湪璇ヨ疆鍙戣█闄嶇骇涓衡€滆瘉鎹笉瓒斥€?
