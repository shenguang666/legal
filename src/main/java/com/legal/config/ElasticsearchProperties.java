package com.legal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "elasticsearch")
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getUris() {
        return uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(Duration socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public Duration getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(Duration connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getMaxConnectionsPerRoute() {
        return maxConnectionsPerRoute;
    }

    public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
        this.maxConnectionsPerRoute = maxConnectionsPerRoute;
    }

    public Index getIndex() {
        return index;
    }

    public void setIndex(Index index) {
        this.index = index;
    }

    public Search getSearch() {
        return search;
    }

    public void setSearch(Search search) {
        this.search = search;
    }

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public String getPrimaryUri() {
        if (uris == null || uris.isEmpty() || !StringUtils.hasText(uris.get(0))) {
            return "http://127.0.0.1:9200";
        }
        return uris.get(0);
    }

    public static class Index {

        private String kbChunks = "legal_kb_chunks_v1";
        private int vectorDims = 1024;

        public String getKbChunks() {
            return kbChunks;
        }

        public void setKbChunks(String kbChunks) {
            this.kbChunks = kbChunks;
        }

        public int getVectorDims() {
            return vectorDims;
        }

        public void setVectorDims(int vectorDims) {
            this.vectorDims = vectorDims;
        }
    }

    public static class Search {

        private int vectorTopK = 80;
        private int bm25TopK = 80;
        private int rrfK = 60;

        public int getVectorTopK() {
            return vectorTopK;
        }

        public void setVectorTopK(int vectorTopK) {
            this.vectorTopK = vectorTopK;
        }

        public int getBm25TopK() {
            return bm25TopK;
        }

        public void setBm25TopK(int bm25TopK) {
            this.bm25TopK = bm25TopK;
        }

        public int getRrfK() {
            return rrfK;
        }

        public void setRrfK(int rrfK) {
            this.rrfK = rrfK;
        }
    }

    public static class Worker {

        private boolean enabled = true;
        private int batchSize = 10;
        private Duration pollInterval = Duration.ofSeconds(3);
        private int maxRetries = 5;
        private Duration retryDelay = Duration.ofSeconds(30);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public Duration getPollInterval() {
            return pollInterval;
        }

        public void setPollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public Duration getRetryDelay() {
            return retryDelay;
        }

        public void setRetryDelay(Duration retryDelay) {
            this.retryDelay = retryDelay;
        }
    }
}
