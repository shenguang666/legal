package com.legal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import com.legal.enums.QaKnowledgeIndexScope;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "elasticsearch")
@Data
public class ElasticsearchProperties {

    private boolean enabled = true;
    private List<String> uris = new ArrayList<>();
    private String username;
    private String password;
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration socketTimeout = Duration.ofSeconds(10);
    private Duration connectionRequestTimeout = Duration.ofSeconds(2);
    private int maxConnections = 100;
    private int maxConnectionsPerRoute = 100;
    private Index index = new Index();
    private Search search = new Search();
    private Worker worker = new Worker();

    public String getPrimaryUri() {
        if (uris == null || uris.isEmpty() || !StringUtils.hasText(uris.get(0))) {
            return "http://127.0.0.1:9200";
        }
        return uris.get(0);
    }

    @Data
    public static class Index {

        /** 知识库文档切片索引名称。 */
        private String kbChunks = "legal_kb_chunks_v1";
        /** MinerU 精准解析知识库文档切片索引名称。 */
        private String kbChunksMineru = "legal_kb_chunks_mineru";
        /** 风险规则文档切片索引名称。 */
        private String riskRule = "legal_risk_rule";
        /** 用户外挂知识索引名称。 */
        private String userKnowledge = "legal_kb_user_knowledge";
        /** 向量维度。 */
        private int vectorDims = 1024;
    }

    @Data
    public static class Search {

        private int vectorTopK = 80;
        private int bm25TopK = 80;
        private int rrfK = 60;
        /** 智能问答默认知识库检索索引范围。 */
        private QaKnowledgeIndexScope defaultKnowledgeIndexScope = QaKnowledgeIndexScope.NATIVE_ONLY;
    }

    @Data
    public static class Worker {

        private boolean enabled = true;
        private int batchSize = 10;
        private Duration pollInterval = Duration.ofSeconds(3);
        private int maxRetries = 5;
        private Duration retryDelay = Duration.ofSeconds(30);
    }
}
