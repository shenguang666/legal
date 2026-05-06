## Requirements

### Requirement: 鏅鸿兘灏忔硶搴浠剁敓鍛藉懆鏈?绯荤粺 SHALL 鎻愪緵鏅鸿兘灏忔硶搴浠剁殑鍒涘缓銆佹煡璇€佹洿鏂般€佸綊妗ｄ笌杞垹闄よ兘鍔涳紝涓旀浠?MUST 涓ユ牸鎸?`tenant_id + owner_user_id` 闅旂锛屾浠剁被鍨嬪湪 MVP 闃舵浠呮敮鎸佸悎鍚岀籂绾枫€?
#### Scenario: 鍒涘缓鍚堝悓绾犵悍妗堜欢
- **WHEN** 宸茬櫥褰曠敤鎴锋彁浜ゆ浠跺垱寤鸿姹傦紝鍖呭惈妗堜欢鏍囬銆佺敤鎴风珛鍦猴紙鍘熷憡鎴栬鍛婏級銆佸悎鍚屼笌璇佹嵁鏂囨。 ID 鍒楄〃
- **THEN** 绯荤粺 MUST 鏍￠獙鎵€鏈夊紩鐢ㄦ枃妗ｅ睘浜庡綋鍓嶇鎴峰拰褰撳墠鐢ㄦ埛
- **THEN** 绯荤粺 MUST 鍦?`court_case` 涓垱寤虹姸鎬佷负 `DRAFT` 鐨勬浠惰褰曪紝骞跺湪 `court_case_party` 涓垱寤哄師鍛婁笌琚憡涓や釜褰撲簨浜?- **THEN** 绯荤粺 MUST 鎶婂紩鐢ㄦ枃妗ｇ櫥璁板埌 `court_case_evidence` 琛紝骞舵爣璁板叾瑙掕壊锛堝悎鍚?/ 璇佹嵁 / 瑙勫垯锛?
#### Scenario: 璺ㄧ鎴疯闂鎷掔粷
- **WHEN** 鐢ㄦ埛灏濊瘯璁块棶銆佷慨鏀规垨鍒犻櫎涓嶅睘浜庡叾绉熸埛鐨勬浠?- **THEN** 绯荤粺 MUST 杩斿洖 404锛岃€屼笉鏄?403锛岄伩鍏嶆浠?ID 鏋氫妇

#### Scenario: 妗堜欢杞垹闄?- **WHEN** 鐢ㄦ埛鍒犻櫎妗堜欢
- **THEN** 绯荤粺 MUST 鎶?`court_case.status` 缃负 `DELETED`锛屼繚鐣欏簳灞傝瘉鎹笌鍥捐氨浜嬩欢
- **THEN** 绯荤粺 MUST 瑙﹀彂瀵瑰簲 Neo4j 瀛愬浘鐨勬竻鐞嗕换鍔★紙寮傛骞傜瓑锛?
### Requirement: 妗堜欢瑕佺礌鎶藉彇涓庣敤鎴风‘璁?绯荤粺 SHALL 鍦ㄥ紑搴墠瀵规浠惰绱狅紙褰撲簨浜恒€佸悎鍚屾潯娆俱€侀噾棰濄€佸饱琛屾椂闂淬€佽繚绾﹁涓恒€佷簤璁劍鐐癸級杩涜 AI 鎶藉彇锛屽苟 MUST 鍦ㄧ敤鎴风‘璁ゅ悗鎵嶅厑璁歌繘鍏ュ涵瀹￠樁娈点€?
#### Scenario: AI 鎶藉彇瑕佺礌骞跺尯鍒嗕富寮犱笌宸茶瘉浜嬪疄
- **WHEN** 鐢ㄦ埛鎻愪氦妗堜欢骞惰Е鍙戣绱犳娊鍙?- **THEN** 绯荤粺 MUST 璋冪敤 LLM 杈撳嚭缁撴瀯鍖栨浠惰绱?JSON锛屾瘡鏉′簨瀹炲甫 `status` 瀛楁锛屽垵濮嬪€间负 `CLAIMED`
- **THEN** 绯荤粺 MUST 鍦ㄥ墠绔彁渚涒€滀綘鐨勪富寮犫€濅笌鈥滃彲璇佷簨瀹炩€濆垎缁勫睍绀猴紝瑕佹眰鐢ㄦ埛閫愰」纭鎴栦慨鏀?
#### Scenario: 鏈‘璁よ绱犱笉寰楀紑搴?- **WHEN** 鐢ㄦ埛鍦ㄦ湭纭蹇呭～瑕佺礌鐨勬儏鍐典笅灏濊瘯寮€搴?- **THEN** 绯荤粺 MUST 鎷掔粷璇锋眰锛屽苟鎻愮ず鍝簺瑕佺礌鏈‘璁?
#### Scenario: 鐢ㄦ埛淇敼宸茬‘璁や簨瀹?- **WHEN** 鐢ㄦ埛鍦ㄥ涵瀹¤繘琛屼腑淇敼宸茬‘璁や簨瀹?- **THEN** 绯荤粺 MUST 鎶婂悗缁涵瀹¤疆鏍囪涓?`STALE`
- **THEN** 绯荤粺 MUST 鎻愮ず鐢ㄦ埛蹇呴』寮€鍚柊涓€杞垨閲嶆柊寮€搴紝绂佹闈欓粯鏀瑰浘

### Requirement: 搴闃舵鐘舵€佹満
绯荤粺 SHALL 浠ラ樁娈电姸鎬佹満鎺ㄨ繘妯℃嫙搴锛岄樁娈?MUST 椤哄簭瑕嗙洊锛氬師鍛婇檲杩般€佽鍛婄瓟杈┿€佷妇璇併€佽川璇併€佹硶搴京璁恒€佹渶鍚庨檲杩般€佹ā鎷熻鍒ゆ剰瑙併€?
#### Scenario: 鍗曡疆涔愯閿佸垏鎹?- **WHEN** 缂栨帓鍣ㄥ皾璇曟妸鏌愪竴杞粠 `PENDING` 鎺ㄨ繘鍒?`RUNNING`
- **THEN** 绯荤粺 MUST 浣跨敤 `WHERE state='PENDING' AND lock_version=?` 鐨勪箰瑙傞攣鏇存柊
- **THEN** 绯荤粺 MUST 鎷掔粷鍏朵粬骞跺彂璇锋眰锛岃繑鍥炩€滃涵瀹¤繘琛屼腑鈥濋敊璇?
#### Scenario: 澶辫触閲嶈瘯骞傜瓑
- **WHEN** 鍚屼竴杞洜鏈嶅姟寮傚父琚噸璇?- **THEN** 绯荤粺 MUST 涓洪噸璇曞垎閰嶆柊鐨?`attempt_id`
- **THEN** 绯荤粺 MUST 鎸?`(round_id, attempt_id)` 骞傜瓑鍐欏叆 LLM 杈撳嚭涓?`court_graph_event`

#### Scenario: 涓嶅厑璁稿洖婊氬凡缁撴潫杞?- **WHEN** 鐢ㄦ埛璇锋眰鍥炴粴鎴栭噸鍋氬凡瀹屾垚鐨勫涵瀹¤疆
- **THEN** 绯荤粺 MUST 鎷掔粷璇ユ搷浣?- **THEN** 绯荤粺 MUST 浠呭厑璁歌拷鍔犫€滆ˉ鍏呭涵瀹¤疆鈥濓紝骞跺湪瀹¤鏃ュ織涓繚鐣欏師杞褰?
### Requirement: 澶氳鑹?Agent 闅旂
绯荤粺 SHALL 涓?AI 娉曞畼銆丄I 瀵规柟浠ｇ悊浜恒€侊紙鍙€夛級AI 鐢ㄦ埛杈呭姪寰嬪笀閰嶇疆鐙珛 prompt 涓庝笂涓嬫枃锛孧UST NOT 鍦ㄥ崟娆?LLM 璋冪敤涓贩鍚堝瑙掕壊绔嬪満銆?
#### Scenario: 娉曞畼杈撳叆鍘荤敤鎴风珛鍦哄寲
- **WHEN** 缂栨帓鍣ㄨ皟鐢?AI 娉曞畼
- **THEN** 绯荤粺 MUST 鎶婂綋浜嬩汉缁熶竴鏇挎崲涓?`PartyA / PartyB`
- **THEN** 绯荤粺 MUST NOT 鍦ㄦ硶瀹?prompt 涓嚭鐜扳€渦ser鈥濃€滃師鍛婃槸鐢ㄦ埛鈥濃€滆鍛婃槸鐢ㄦ埛鈥濈瓑鎺緸

#### Scenario: 瀵规柟浠ｇ悊浜轰笉寰楃紪閫犺瘉鎹?- **WHEN** AI 瀵规柟浠ｇ悊浜哄彂瑷€
- **THEN** 绯荤粺 MUST 鍦?prompt 涓樉寮忕害鏉燂細鍙兘寮曠敤鏈宸茬櫥璁拌瘉鎹紝涓嶅緱缂栭€犲悎鍚屾潯娆炬垨浜嬪疄
- **THEN** 绯荤粺 MUST 瀵瑰叾杈撳嚭鎵ц璇佹嵁寮曠敤涓夊眰鏍￠獙

### Requirement: 娉曞畼杈撳嚭鍙屽悜涓嶅埄鐐瑰己鍒?绯荤粺 SHALL 寮哄埗 AI 娉曞畼杈撳嚭缁撴瀯鍖?JSON锛屽寘鍚?`focusIssues / acceptedFacts / rejectedFacts / unfavorableToPartyA / unfavorableToPartyB / openQuestions` 瀛楁锛屼笖 `unfavorableToPartyA` 涓?`unfavorableToPartyB` MUST 鍚勫寘鍚嚦灏戜竴鏉¤鐐广€?
#### Scenario: 鍙屽悜瑕佺偣缂哄け鏃舵暣浣撻噸鐢熸垚
- **WHEN** AI 娉曞畼杈撳嚭缂哄皯 `unfavorableToPartyA` 鎴?`unfavorableToPartyB`
- **THEN** 绯荤粺 MUST 瑙﹀彂鏁翠綋閲嶇敓鎴愶紝鏈€澶?2 娆?
#### Scenario: 閲嶇敓鎴愪粛澶辫触鐨勫洖閫€
- **WHEN** 閲嶇敓鎴愯揪鍒颁笂闄愪粛鏃犳硶婊¤冻鍙屽悜瑕佺偣
- **THEN** 绯荤粺 MUST 鎶婃湰杞硶瀹樿緭鍑哄洖閫€涓衡€滆瘉鎹笉瓒筹紝鏃犳硶褰㈡垚鍊惧悜鎬ф剰瑙佲€?- **THEN** 绯荤粺 MUST 鍦ㄥ璁℃棩蹇椾腑璁板綍澶辫触鍘熷洜涓庨噸璇曟鏁?
### Requirement: 璇佹嵁寮曠敤涓夊眰鏍￠獙
绯荤粺 SHALL 瀵规墍鏈?AI 瑙掕壊鍙戣█涓殑 `evidenceIds` 涓?`chunkIds` 寮曠敤鎵ц schema銆佺櫧鍚嶅崟銆佸綊灞炰笁灞傛牎楠屽悗鎵嶈兘鍐欏叆 `court_argument` 涓?Neo4j 鍥捐氨銆?
#### Scenario: 鐧藉悕鍗曟敞鍏ヤ笌鍛戒腑鏍￠獙
- **WHEN** 缂栨帓鍣ㄤ负鏌愯疆鍙戣█鍑嗗 prompt
- **THEN** 绯荤粺 MUST 閫氳繃 `CourtEvidenceService.allowedRefsForRound` 鐢熸垚鍏佽寮曠敤鐨?ID 闆嗗悎
- **THEN** 绯荤粺 MUST 鎶婂厑璁稿紩鐢ㄧ殑 ID 闆嗗悎鏄惧紡娉ㄥ叆 prompt
- **THEN** 绯荤粺 MUST 瀵硅В鏋愬嚭鐨勫紩鐢ㄦ墽琛?`Set.contains` 杩囨护

#### Scenario: 璺ㄦ浠舵垨璺ㄧ鎴峰紩鐢ㄨ鎷掔粷
- **WHEN** AI 杈撳嚭寮曠敤浜嗕笉灞炰簬鏈浠舵垨涓嶅睘浜庢湰绉熸埛鐨勮瘉鎹?ID
- **THEN** 绯荤粺 MUST 涓㈠純璇ュ紩鐢?- **THEN** 绯荤粺 MUST 鎶婂彂瑷€ `stance` 闄嶇骇涓?`PENDING_PROOF`

#### Scenario: schema 鏍￠獙澶辫触閲嶈瘯
- **WHEN** AI 杈撳嚭 JSON 涓嶇鍚?schema
- **THEN** 绯荤粺 MUST 鏁磋疆閲嶈瘯锛岄噸璇曟鏁板彈 `legal.smart-court.evidence-ref-retry` 鎺у埗
- **THEN** 绯荤粺 MUST 鍦ㄩ噸璇曠敤灏芥椂鎶婃湰杞彂瑷€闄嶇骇涓衡€滃緟璇佹槑鈥濊€屼笉鏄涪寮?
### Requirement: 鍗曟浠堕绠楁姢鏍?绯荤粺 SHALL 閫氳繃 `legal.smart-court.*` 閰嶇疆鎺у埗鍗曟浠舵渶澶ц疆鏁般€佸崟杞渶澶?token銆佸崟妗堜欢绱 token 涓婇檺锛孧UST 鍦ㄨ秴闄愭椂鍋滄鍚庣画 LLM 璋冪敤骞惰嚜鍔ㄦ敹灏俱€?
#### Scenario: 鍗曡疆 token 瓒呴檺
- **WHEN** 鍗曡疆绱 token 瓒呰繃 `legal.smart-court.max-tokens-per-round`
- **THEN** 绯荤粺 MUST 缁堟鏈疆鍓╀綑 LLM 璋冪敤
- **THEN** 绯荤粺 MUST 鎶婃湰杞姸鎬佺疆涓?`FAILED`锛屽苟鍦ㄥ璁℃棩蹇椾腑璁板綍鍘熷洜

#### Scenario: 鍗曟浠剁疮璁?token 瓒呴檺
- **WHEN** 鍗曟浠剁疮璁?token 瓒呰繃 `legal.smart-court.max-tokens-per-case`
- **THEN** 绯荤粺 MUST 闃绘寮€鍚柊搴杞?- **THEN** 绯荤粺 MUST 鍦ㄥ墠绔彁绀虹敤鎴峰凡杈鹃绠椾笂闄?
#### Scenario: 鍗曟浠舵渶澶ц疆鏁板埌杈?- **WHEN** 宸插畬鎴愯疆鏁拌揪鍒?`legal.smart-court.max-rounds`
- **THEN** 绯荤粺 MUST 闃绘寮€鍚柊搴杞?- **THEN** 绯荤粺 MUST 寮曞鐢ㄦ埛杩涘叆鈥滅敓鎴愭ā鎷熻鍒ゆ姤鍛娾€濋樁娈?
### Requirement: 搴涓婁笅鏂囦娇鐢ㄧ埗绾ц涔夊潡
绯荤粺 SHALL 鍦ㄨ皟鐢ㄤ换鎰忔櫤鑳藉皬娉曞涵 Agent 鍓嶏紝浣跨敤 RAG 鐖剁骇涓婁笅鏂囨墿灞曡兘鍔涜幏鍙栬瘉鎹笂涓嬫枃锛孧UST NOT 鎶婂瓙鍒嗗潡鍐呭鐩存帴浣滀负 LLM 涓婁笅鏂囥€?
#### Scenario: 搴 Agent 妫€绱㈠己鍒朵娇鐢ㄧ埗鍧?- **WHEN** 浠绘剰 Court Agent 璋冪敤 RAG 妫€绱㈡帴鍙ｏ紝涓?`bizType=SMART_COURT`
- **THEN** 绯荤粺 MUST 鍚敤鐖剁骇涓婁笅鏂囨墿灞?- **THEN** 绯荤粺 MUST 鍦?`court_argument.evidence_refs` 涓悓鏃惰褰?`parent_chunk_id` 涓庡懡涓殑 `child_chunk_id`

### Requirement: 妯℃嫙瑁佸垽鎶ュ憡涓庡悎瑙勬按鍗?绯荤粺 SHALL 鍦ㄥ涵瀹℃敹灏炬椂鐢熸垚缁撴瀯鍖栨ā鎷熻鍒ゆ姤鍛婏紙浜夎鐒︾偣銆佷簨瀹炶瀹氥€佽瘉鎹噰淇°€佹ā鎷熻鍒よ鐐广€侀闄╃瓑绾с€佽ˉ璇佸缓璁憳瑕侊級锛屽苟 MUST 鍦ㄦ墍鏈夊睍绀轰笌瀵煎嚭褰㈡€佸浐瀹氶檮甯︹€滀粎渚涙ā鎷熷弬鑰冦€佷笉鏋勬垚娉曞緥鎰忚鈥濇按鍗般€?
#### Scenario: 鎶ュ憡蹇呴』鍖呭惈鍙屾柟瑕佺偣
- **WHEN** 绯荤粺鐢熸垚妯℃嫙瑁佸垽鎶ュ憡
- **THEN** 鎶ュ憡 MUST 鍚屾椂鍖呭惈鏀寔鍘熷憡涓庢敮鎸佽鍛婄殑鑷冲皯涓€鏉¤鐐?- **THEN** 绯荤粺 MUST 鍦ㄥ墠绔笌 PDF 瀵煎嚭涓浐瀹氭覆鏌撳悎瑙勬按鍗?
#### Scenario: 鎶ュ憡瀵煎嚭瀹¤
- **WHEN** 鐢ㄦ埛瀵煎嚭妯℃嫙瑁佸垽鎶ュ憡
- **THEN** 绯荤粺 MUST 鍦ㄥ璁℃棩蹇椾腑璁板綍 `caseId / userId / tenantId / exportFormat / timestamp`

### Requirement: 搴杈撳嚭娴佸紡涓庡彲涓柇
绯荤粺 SHALL 閫氳繃 SSE 鍚戝墠绔祦寮忚緭鍑?AI 娉曞畼涓?AI 瀵规柟鐨勫彂瑷€锛孧UST 鏀寔鐢ㄦ埛涓诲姩鍋滄褰撳墠杞紝骞?MUST 鍦ㄧ綉缁滄柇寮€鏃舵寜鐜版湁 SSE 瑙勮寖浼橀泤澶勭悊銆?
#### Scenario: 鐢ㄦ埛涓诲姩鍋滄褰撳墠杞?- **WHEN** 鐢ㄦ埛鍦ㄦ煇杞繘琛屼腑鐐瑰嚮鈥滃仠姝⑩€?- **THEN** 绯荤粺 MUST 鎶婅杞姸鎬佷粠 `RUNNING` 鍒囨崲鍒?`CANCELLED`
- **THEN** 绯荤粺 MUST 鎷掔粷鍐欏叆璇ヨ疆鐨?LLM 杈撳嚭涓?`court_graph_event`

#### Scenario: SSE 瀹㈡埛绔柇寮€
- **WHEN** SSE 杩炴帴琚鎴风鏂紑
- **THEN** 绯荤粺 MUST 鎸夌幇鏈?SSE 瑙勮寖鍦?debug 绾у埆璁板綍鏃ュ織锛屽苟鎶婅杞疆涓?`FAILED` 鎴栧厑璁哥◢鍚庨噸璇?- **THEN** 绯荤粺 MUST NOT 瑙﹀彂 Spring MVC error dispatch 鍐欏叆 JSON 閿欒鍝嶅簲

### Requirement: 小法庭 LLM 配置 SHALL 支持专用 Key 并默认复用在线问答 Key
系统 SHALL 为智能小法庭提供独立的 `legal.smart-court.llm.*` 配置，且 MUST 在小法庭专用 API Key 未配置时默认复用在线问答模型 API Key。

#### Scenario: 使用小法庭专用 Key
- **WHEN** 环境变量配置了 `LEGAL_SMART_COURT_LLM_API_KEY`
- **THEN** 智能小法庭 LLM 调用 MUST 使用该专用 Key
- **THEN** 在线问答、Embedding 或其他业务的模型 Key MUST NOT 被该调用覆盖

#### Scenario: 未配置小法庭专用 Key 时复用在线问答 Key
- **WHEN** `LEGAL_SMART_COURT_LLM_API_KEY` 为空且 `LEGAL_LLM_API_KEY` 已配置
- **THEN** 智能小法庭 LLM 调用 MUST 复用在线问答模型 API Key
- **THEN** token 计量仍 MUST 按 `bizType=SMART_COURT` 记录

#### Scenario: 两类 Key 都不可用时拒绝流式开庭
- **WHEN** 小法庭专用 API Key 与在线问答 API Key 都不可用
- **THEN** 系统 MUST 拒绝开庭流式调用
- **THEN** 前端 MUST 展示 `服务繁忙，请稍后重试`

### Requirement: AI 对话对象 SHALL 使用资源目录系统提示词
系统 SHALL 为每个智能小法庭 AI 对话对象提供独立系统提示词文件，文件 MUST 位于 `src/main/resources/prompts`，且格式 MUST 参考现有 `legal-rag-system.txt` 的纯文本模板风格。

#### Scenario: 对方代理人系统提示词来自资源文件
- **WHEN** 系统调用 AI 对方代理人生成庭审发言
- **THEN** 系统 MUST 从 `classpath:prompts/court-opponent-agent-system.txt` 加载系统提示词
- **THEN** 提示词 MUST 明确限制其只能引用本案允许证据，不得编造事实、合同条款或证据编号

#### Scenario: 用户辅助律师系统提示词来自资源文件
- **WHEN** 系统调用 AI 用户辅助律师生成庭审发言
- **THEN** 系统 MUST 从 `classpath:prompts/court-user-advisor-agent-system.txt` 加载系统提示词
- **THEN** 提示词 MUST 明确其职责是帮助用户补强主张、提示证据缺口，并禁止编造证据

#### Scenario: 法官系统提示词来自资源文件
- **WHEN** 系统调用 AI 法官生成庭审归纳或模拟裁判观点
- **THEN** 系统 MUST 从 `classpath:prompts/court-judge-agent-system.txt` 加载系统提示词
- **THEN** 提示词 MUST 要求法官保持中立、输入去用户立场化，并输出双方不利点

#### Scenario: 提示词模板变量安全渲染
- **WHEN** 系统渲染任意小法庭系统提示词模板
- **THEN** 系统 MUST 支持注入庭审阶段、案件摘要、本轮指令、历史庭审摘要、证据上下文和允许引用白名单
- **THEN** 任一变量为空时系统 MUST 使用安全默认值而不是输出 `null`

### Requirement: 开庭 SHALL 通过流式接口自动生成完整一轮多角色对话
系统 SHALL 在用户点击开庭后通过 SSE 流式接口自动生成完整一轮庭审对话，角色顺序 MUST 为 `对方代理人 -> 用户辅助律师 -> 法官`。

#### Scenario: 开庭自动执行完整一轮
- **WHEN** 用户在已确认要素的案件上点击开庭
- **THEN** 前端 MUST 调用智能小法庭流式庭审接口
- **THEN** 后端 MUST 按顺序调用 AI 对方代理人、AI 用户辅助律师和 AI 法官
- **THEN** 每个角色的输出 MUST 通过 SSE 事件推送给前端展示

#### Scenario: 角色发言持久化
- **WHEN** 任一 AI 角色完成本轮发言
- **THEN** 系统 MUST 保存对应的庭审消息和论点记录
- **THEN** 系统 MUST 保存模型名称、角色、轮次、尝试编号和 token 消耗

#### Scenario: 保持证据引用校验
- **WHEN** 任一 AI 角色输出包含证据、父分块或子分块引用
- **THEN** 系统 MUST 继续执行 schema、白名单和归属三层校验
- **THEN** 未通过校验的引用 MUST 被丢弃或降级为待证明，且不得污染图谱

#### Scenario: 本轮完成后刷新图谱与建议
- **WHEN** 完整一轮多角色对话生成成功
- **THEN** 系统 MUST 按既有机制发布图谱事件
- **THEN** 系统 MUST 刷新补证建议
- **THEN** 前端 MUST 能在流式完成后刷新图谱和建议区域

### Requirement: 智能小法庭 SHALL 仅提供流式庭审生成路径
系统 SHALL 仅通过 SSE 提供完整庭审轮次生成能力，MUST NOT 新增非流式完整轮次生成接口；当流式生成不可用时 MUST 使用统一拒绝信息作为 fallback。

#### Scenario: 不提供非流式完整轮次生成接口
- **WHEN** 实现智能小法庭大模型庭审对话能力
- **THEN** 系统 MUST NOT 新增用于完整生成一轮多角色对话的非流式 API
- **THEN** 前端开庭交互 MUST 以 SSE 流式接口为唯一生成入口

#### Scenario: 流式初始化失败
- **WHEN** SSE 流式连接初始化失败或服务端无法创建庭审生成任务
- **THEN** 系统 MUST 返回或推送 `服务繁忙，请稍后重试`
- **THEN** 系统 MUST 记录结构化错误日志，且不得泄露完整 prompt 或证据原文

#### Scenario: 模型调用失败
- **WHEN** 任一角色模型调用超时、异常或返回不可处理结果导致本轮无法继续
- **THEN** 系统 MUST 终止本轮剩余角色调用
- **THEN** 前端 MUST 展示 `服务繁忙，请稍后重试`
- **THEN** 系统 MUST 按轮次状态机记录失败原因

#### Scenario: SSE 客户端断开
- **WHEN** 前端在庭审流式生成过程中断开连接或点击停止
- **THEN** 系统 MUST 按现有 SSE 规范处理断开
- **THEN** 系统 MUST NOT 触发 Spring MVC error dispatch 写入额外 JSON 错误响应

