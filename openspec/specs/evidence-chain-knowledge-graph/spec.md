## Requirements

### Requirement: MySQL 涓烘潈濞佹簮 Neo4j 涓哄紓姝ュ浘璋辨姇褰?绯荤粺 SHALL 鎶?MySQL 浣滀负妗堜欢銆佸涵瀹°€佽瘉鎹€佸浘璋变簨浠剁殑鍞竴鏉冨▉婧愶紝Neo4j 浠呬綔涓哄熀浜?`court_graph_event` 寮傛骞傜瓑鎶曞奖鍑虹殑鍥捐氨瑙嗗浘锛孧UST 姘歌繙鍙粠 MySQL 浜嬩欢娴佸叏閲忛噸寤恒€?
#### Scenario: 浜嬩欢鍏堣惤 MySQL 鍐嶅紓姝ユ姇褰?- **WHEN** 搴鏌愯疆浜х敓鏂扮殑鍥捐氨鑺傜偣鎴栧叧绯?- **THEN** 绯荤粺 MUST 鍦ㄥ悓涓€ MySQL 浜嬪姟鍐呭啓鍏ュ涵瀹¤褰曚笌涓€鏉℃垨澶氭潯 `court_graph_event(payload_json, status='PENDING')`
- **THEN** 绯荤粺 MUST 鐢辩嫭绔?`CourtGraphProjector` 寮傛娑堣垂 PENDING 浜嬩欢骞跺箓绛夊啓鍏?Neo4j
- **THEN** 绯荤粺 MUST NOT 鍦ㄤ富娴佺▼涓洿鎺ヨ皟鐢?Neo4j 鍐欏叆

#### Scenario: 鎶曞奖澶辫触閲嶈瘯涓庢淇?- **WHEN** Neo4j 鎶曞奖澶辫触
- **THEN** 绯荤粺 MUST 鎸夋寚鏁伴€€閬块噸璇曪紝閲嶈瘯娆℃暟涓庤秴鏃剁敱 YAML 閰嶇疆
- **THEN** 绯荤粺 MUST 鍦ㄩ噸璇曠敤灏藉悗鎶婁簨浠舵爣璁颁负 `DEAD`锛屽苟瑙﹀彂鍛婅鏃ュ織

#### Scenario: Neo4j 鏁呴殰涓嶉樆濉炲涵瀹?- **WHEN** Neo4j 鏁翠綋涓嶅彲鐢?- **THEN** 绯荤粺 MUST 鍏佽搴缁х画杩涜锛圡ySQL 鍐欏叆銆丩LM 璋冪敤銆丼SE 杈撳嚭锛?- **THEN** 绯荤粺 MUST 鍦ㄥ墠绔浘璋卞尯鍩熻繑鍥?`graphState='UNAVAILABLE'`锛屾彁绀哄浘璋辨殏鏃朵笉鍙敤

#### Scenario: 鍏ㄩ噺閲嶅缓鍥捐氨
- **WHEN** 绠＄悊鍛樿Е鍙戞煇妗堜欢鍥捐氨閲嶅缓
- **THEN** 绯荤粺 MUST 鍏堝湪 Neo4j 瀵硅妗堜欢鎵ц `DETACH DELETE` 娓呯悊瀛愬浘
- **THEN** 绯荤粺 MUST 鎸変簨浠堕『搴忓洖鏀?`court_graph_event`锛屾寜 `legal.smart-court.graph.rebuild-page-size` 鍒嗛〉澶勭悊
- **THEN** 绯荤粺 MUST 鍦ㄩ噸寤哄畬鎴愬悗鎶婃墍鏈変簨浠剁姸鎬佽涓?`APPLIED`

### Requirement: 鍥捐氨鑺傜偣涓庡叧绯荤被鍨?绯荤粺 SHALL 鎸変笟鍔¤涔夊缓妯″浘璋辫妭鐐逛笌鍏崇郴锛孧UST 鑷冲皯瑕嗙洊妗堜欢銆佸綋浜嬩汉銆佽瘔姹傘€佹姉杈┿€佷簨瀹炪€佽瘉鎹€佹枃妗ｃ€佺墖娈点€佹潯娆俱€佷箟鍔°€佽繚绾︺€侀噾棰濄€佹棩鏈熴€佽鐐广€侀闄┿€佺己鍙ｃ€佹硶婧愪緷鎹€佹ā鎷熻鍒よ鐐圭瓑鑺傜偣绫诲瀷銆?
#### Scenario: 鑺傜偣绫诲瀷瑕嗙洊
- **WHEN** 绯荤粺鍒濆鍖?Neo4j schema
- **THEN** 绯荤粺 MUST 鍒涘缓鏍囩 `Case / Party / Claim / Defense / Fact / Evidence / Document / Chunk / Clause / Obligation / Breach / Amount / Date / Argument / Risk / Gap / LegalBasis / JudgmentPoint`

#### Scenario: 鍏崇郴绫诲瀷瑕嗙洊
- **WHEN** 绯荤粺鍒濆鍖?Neo4j schema
- **THEN** 绯荤粺 MUST 鏀寔鍏崇郴 `HAS_PARTY / RAISES_CLAIM / RAISES_DEFENSE / ASSERTS_FACT / SUPPORTED_BY / CONTRADICTED_BY / DERIVED_FROM / QUOTES_CLAUSE / PROVES_AMOUNT / PROVES_DATE / CREATES_OBLIGATION / BREACHES_OBLIGATION / SUPPORTS_ARGUMENT / CHALLENGES_ARGUMENT / HAS_RISK / HAS_GAP / NEEDS_EVIDENCE / SUPPORTS_JUDGMENT`

### Requirement: 澶氱鎴烽殧绂讳笌鍞竴绾︽潫
绯荤粺 SHALL 鍦ㄦ墍鏈?Neo4j 鑺傜偣涓婂啑浣?`tenantId` 涓?`caseId` 灞炴€э紝骞?MUST 涓烘瘡绫绘牳蹇冭妭鐐瑰缓绔嬪惈 `tenantId` 鐨勫敮涓€绾︽潫涓庣储寮曘€?
#### Scenario: 鑺傜偣鍐椾綑 tenantId
- **WHEN** 浠绘剰鍥捐氨鑺傜偣琚垱寤?- **THEN** 绯荤粺 MUST 鎶?`tenantId` 涓?`caseId` 鍐欏叆鑺傜偣灞炴€?- **THEN** 绯荤粺 MUST NOT 鍒涘缓璺?`tenantId` 鐨勫叧绯?
#### Scenario: 鍞竴绾︽潫涓庣储寮?- **WHEN** 绯荤粺鍒濆鍖?Neo4j schema
- **THEN** 绯荤粺 MUST 涓?`Case / Party / Claim / Defense / Fact / Evidence / Argument / JudgmentPoint` 绛夎妭鐐瑰垱寤?`(tenantId, businessId)` 鍞竴绾︽潫
- **THEN** 绯荤粺 MUST 涓洪珮棰戞绱㈣妭鐐癸紙濡?`Evidence / Fact / Claim`锛夊垱寤?`(tenantId, caseId)` 绱㈠紩

#### Scenario: Cypher 寮哄埗娉ㄥ叆 tenantId
- **WHEN** 绯荤粺鎵ц浠绘剰 Cypher 鏌ヨ
- **THEN** 绯荤粺 MUST 閫氳繃缁熶竴鍒囬潰鎶?`tenantId` 浣滀负鍙傛暟娉ㄥ叆
- **THEN** 绯荤粺 MUST 鎷掔粷涓氬姟浠ｇ爜瑁告墽琛?Cypher

### Requirement: 浜嬪疄鑺傜偣鐘舵€佸垎绾?绯荤粺 SHALL 鍦?`Fact` 鑺傜偣涓婄淮鎶?`status` 灞炴€э紝鍙栧€?MUST 涓?`CLAIMED / EVIDENCED / DISPUTED / ACCEPTED` 涔嬩竴锛屼笖 `CLAIMED` 鐘舵€佺殑浜嬪疄 MUST NOT 鐩存帴杩涘叆妯℃嫙瑁佸垽鎺ㄧ悊銆?
#### Scenario: 涓诲紶鍗囩骇涓哄凡璇佷簨瀹?- **WHEN** 鏌?`Fact` 鑺傜偣鑷冲皯瀛樺湪涓€鏉?`SUPPORTED_BY` 鍏崇郴杩炲埌閫氳繃鏍￠獙鐨?`Evidence`
- **THEN** 绯荤粺 MUST 鎶?`status` 鐢?`CLAIMED` 鍗囩骇涓?`EVIDENCED`

#### Scenario: 宸茶瘉浜嬪疄琚弽椹宠繘鍏ヤ簤璁?- **WHEN** 鏌?`Fact` 鑺傜偣鍚屾椂瀛樺湪 `SUPPORTED_BY` 涓?`CONTRADICTED_BY` 鍏崇郴
- **THEN** 绯荤粺 MUST 鎶?`status` 缃负 `DISPUTED`
- **THEN** 绯荤粺 MUST 鍦ㄨˉ璇佸缓璁腑鏍囪闇€瑕佽繘涓€姝ヨ瘉鎹秷闄ゅ啿绐?
#### Scenario: 浠呬富寮犱笉杩涘叆瑁佸垽鎺ㄧ悊
- **WHEN** 绯荤粺鐢熸垚妯℃嫙瑁佸垽瑙傜偣
- **THEN** 绯荤粺 MUST 鎺掗櫎 `status=CLAIMED` 鐨勪簨瀹炶妭鐐?- **THEN** 绯荤粺 MUST 鍦ㄨ鍒よ鐐硅鏄庝腑鎻愮ず鍝簺涓诲紶灏氭湭琚瘉鎹敮鎸?
### Requirement: Cypher 鏌ヨ纭害鏉?绯荤粺 SHALL 闄愬埗 Neo4j 鏌ヨ浠ｄ环锛孧UST 閫氳繃 `legal.smart-court.graph.max-hops` 鎺у埗璺緞璺虫暟涓婇檺锛屽苟閫氳繃 `legal.smart-court.graph.max-nodes-per-case` 闄愬埗鍗曟浠惰妭鐐规€绘暟銆?
#### Scenario: 澶氳烦璺緞璺虫暟鍙楅檺
- **WHEN** 浠绘剰 Court 鍥捐氨鏌ヨ浣跨敤鍙彉闀垮害璺緞
- **THEN** 绯荤粺 MUST 鏄惧紡澹版槑 `*1..N`锛屼笖 N MUST NOT 瓒呰繃 `legal.smart-court.graph.max-hops` 閰嶇疆鍊?- **THEN** 绯荤粺 MUST NOT 浣跨敤鏃犱笂闄愮殑鍙彉闀垮害璺緞

#### Scenario: 鍗曟浠惰妭鐐逛笂闄?- **WHEN** 鎶曞奖鍣ㄥ嵆灏嗗啓鍏ユ柊鑺傜偣瀵艰嚧妗堜欢鑺傜偣鎬绘暟瓒呰繃 `legal.smart-court.graph.max-nodes-per-case`
- **THEN** 绯荤粺 MUST 鎷掔粷鍐欏叆璇ユ柊鑺傜偣
- **THEN** 绯荤粺 MUST 鍦ㄥ憡璀︽棩蹇椾腑璁板綍妗堜欢 ID 涓庡綋鍓嶈妭鐐规暟

#### Scenario: 鍛藉悕 Cypher 涓庡洖褰掑熀绾?- **WHEN** 瀹炵幇鍏抽敭鏌ヨ锛堣瘉鎹摼銆佺己鍙ｃ€佷簤璁劍鐐癸級
- **THEN** 绯荤粺 MUST 浣跨敤鍛藉悕 Cypher锛圧epository `@Query` 鎴栧父閲忥級锛屼笉寰楀姩鎬佹嫾鎺?- **THEN** 绯荤粺 MUST 鎻愪緵闆嗘垚娴嬭瘯鍩虹嚎锛屽洖褰掓椂鑻?dbHits 瓒呴槇鍊?MUST 瑙﹀彂澶辫触

### Requirement: 鍥捐氨鎶曞奖鐘舵€佸澶栨毚闇?绯荤粺 SHALL 鍦ㄥ浘璋辨煡璇㈡帴鍙ｈ繑鍥?`graphState`锛孧UST 鍙栧€间负 `READY / PROJECTING / UNAVAILABLE` 涔嬩竴銆?
#### Scenario: 鎶曞奖鏈畬鎴?- **WHEN** 妗堜欢瀛樺湪灏氭湭瀹屾垚鐨?`court_graph_event(status='PENDING')`
- **THEN** 绯荤粺 MUST 鍦ㄥ搷搴斾腑杩斿洖 `graphState='PROJECTING'`锛屽苟闄勫甫鍓╀綑浜嬩欢鏁?
#### Scenario: 鎶曞奖瀹屾垚
- **WHEN** 璇ユ浠舵棤 PENDING 浜嬩欢锛屼笖 Neo4j 鍙闂?- **THEN** 绯荤粺 MUST 杩斿洖 `graphState='READY'`

#### Scenario: Neo4j 涓嶅彲璁块棶
- **WHEN** Neo4j 涓嶅彲璁块棶
- **THEN** 绯荤粺 MUST 杩斿洖 `graphState='UNAVAILABLE'`锛屽苟閫€鍖栦负鍙睍绀?MySQL 涓殑浜嬪疄鍜岃瘉鎹垪琛?
### Requirement: 鍥捐氨鍓嶇鍙鍖栬鍓?绯荤粺 SHALL 鍦ㄥ墠绔浘璋辨帴鍙ｉ粯璁ゆ寜褰撳墠浜夎鐒︾偣 2 璺冲瓙鍥捐繑鍥烇紝MUST 鏀寔 `focusClaimId / hops / limit` 鍙傛暟锛屽苟 MUST 涓嶄竴娆℃€ц繑鍥炶秴杩囪妭鐐逛笂闄愮殑鏁村浘銆?
#### Scenario: 榛樿瀛愬浘
- **WHEN** 鍓嶇璋冪敤 `GET /api/smart-court/cases/{id}/graph` 涓嶅甫鍙傛暟
- **THEN** 绯荤粺 MUST 杩斿洖鍥寸粫褰撳墠浜夎鐒︾偣鐨?2 璺冲瓙鍥撅紝涓旇妭鐐规暟涓嶈秴杩?200

#### Scenario: 鑷畾涔夎烦鏁?- **WHEN** 鍓嶇浼犲叆 `hops` 鍙傛暟
- **THEN** 绯荤粺 MUST 鎶婅姹傝烦鏁颁笌 `legal.smart-court.graph.max-hops` 鍙栬緝灏忓€?- **THEN** 绯荤粺 MUST 涓嶅厑璁歌烦鏁拌秴杩囨渶澶у€?
### Requirement: 鍥捐氨鑺傜偣 Schema 鐗堟湰鍖?绯荤粺 SHALL 鍦ㄨ妭鐐?payload 涓惡甯?`schemaVersion` 灞炴€э紝MUST 鍦ㄦ柊澧炶妭鐐圭被鍨嬫垨瀛楁鏃堕€氳繃鐗堟湰鍙疯縼绉伙紝鑰屼笉鏄牬鍧忓紡瑕嗙洊銆?
#### Scenario: 鏂板鑺傜偣瀛楁
- **WHEN** 绯荤粺鍗囩骇鑺傜偣 schema 鐗堟湰
- **THEN** 绯荤粺 MUST 鎶婃柊鐗堟湰鍙峰啓鍏ユ柊鍒涘缓鑺傜偣鐨?`schemaVersion`
- **THEN** 绯荤粺 MUST 鍦ㄦ姇褰卞櫒涓吋瀹逛綆鐗堟湰鑺傜偣鐨勮鍙?
