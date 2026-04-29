package com.legal.chat.cache;

import com.legal.common.JsonUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 问答缓存服务（Exact + Semantic）。
 *
 * <p>命中顺序由上层控制：USER-EXACT -> TENANT-EXACT -> USER-SEMANTIC -> TENANT-SEMANTIC。</p>
 */
@Service
public class AnswerCacheService {

    private static final Duration TTL = Duration.ofDays(7);

    private static final int SEMANTIC_CANDIDATES = 200;
    private static final double SEMANTIC_THRESHOLD = 0.92d;

    private final StringRedisTemplate redis;
    private final QuestionNormalizer normalizer;
    private final QuestionSimilarityService similarityService;

    public AnswerCacheService(StringRedisTemplate redis,
                              QuestionNormalizer normalizer,
                              QuestionSimilarityService similarityService) {
        this.redis = redis;
        this.normalizer = normalizer;
        this.similarityService = similarityService;
    }

    // -------------------- Exact --------------------

    public Optional<CacheHit> getUserExact(Long tenantId, Long userId, String kbSnapshotVersion, String question) {
        String normalized = normalizer.normalize(question);
        String key = keyUserExact(tenantId, userId, kbSnapshotVersion, normalized);
        return readHit(key, "USER", "EXACT", null);
    }

    public Optional<CacheHit> getTenantExact(Long tenantId, String kbSnapshotVersion, String question) {
        String normalized = normalizer.normalize(question);
        String key = keyTenantExact(tenantId, kbSnapshotVersion, normalized);
        return readHit(key, "TENANT", "EXACT", null);
    }

    public void putUserExact(Long tenantId,
                             Long userId,
                             String kbSnapshotVersion,
                             String question,
                             AnswerCachePayload payload) {
        String normalized = normalizer.normalize(question);
        payload.setNormalizedQuestion(normalized);
        payload.setCacheScope("USER");
        payload.setCacheType("EXACT");
        payload.setCreatedAt(System.currentTimeMillis());
        String key = keyUserExact(tenantId, userId, kbSnapshotVersion, normalized);
        redis.opsForValue().set(key, JsonUtils.toJson(payload), TTL);
    }

    public void putTenantExact(Long tenantId,
                               String kbSnapshotVersion,
                               String question,
                               AnswerCachePayload payload) {
        String normalized = normalizer.normalize(question);
        payload.setNormalizedQuestion(normalized);
        payload.setCacheScope("TENANT");
        payload.setCacheType("EXACT");
        payload.setCreatedAt(System.currentTimeMillis());
        String key = keyTenantExact(tenantId, kbSnapshotVersion, normalized);
        redis.opsForValue().set(key, JsonUtils.toJson(payload), TTL);
    }

    // -------------------- Semantic (Phase2) --------------------

    public Optional<CacheHit> getUserSemantic(Long tenantId, Long userId, String kbSnapshotVersion, String question) {
        String normalized = normalizer.normalize(question);
        String candidatesKey = keyUserSemanticCandidates(tenantId, userId, kbSnapshotVersion);
        return semanticMatch(candidatesKey, scopeEntryPrefixUser(tenantId, userId, kbSnapshotVersion), normalized, "USER");
    }

    public Optional<CacheHit> getTenantSemantic(Long tenantId, String kbSnapshotVersion, String question) {
        String normalized = normalizer.normalize(question);
        String candidatesKey = keyTenantSemanticCandidates(tenantId, kbSnapshotVersion);
        return semanticMatch(candidatesKey, scopeEntryPrefixTenant(tenantId, kbSnapshotVersion), normalized, "TENANT");
    }

    public void putUserSemanticCandidate(Long tenantId,
                                        Long userId,
                                        String kbSnapshotVersion,
                                        String question,
                                        AnswerCachePayload payload) {
        String normalized = normalizer.normalize(question);
        String entryId = "e_" + UUID.randomUUID().toString().replace("-", "");
        String entryKey = keyUserSemanticEntry(tenantId, userId, kbSnapshotVersion, entryId);

        SemanticEntry entry = SemanticEntry.fromPayload(payload);
        entry.setNormalizedQuestion(normalized);
        entry.setVector(similarityService.encodeVector(similarityService.embed(normalized)));
        entry.setCreatedAt(System.currentTimeMillis());

        redis.opsForValue().set(entryKey, JsonUtils.toJson(entry), TTL);
        redis.opsForZSet().add(keyUserSemanticCandidates(tenantId, userId, kbSnapshotVersion), entryId, entry.getCreatedAt());
        redis.expire(keyUserSemanticCandidates(tenantId, userId, kbSnapshotVersion), TTL);
    }

    public void putTenantSemanticCandidate(Long tenantId,
                                          String kbSnapshotVersion,
                                          String question,
                                          AnswerCachePayload payload) {
        String normalized = normalizer.normalize(question);
        String entryId = "e_" + UUID.randomUUID().toString().replace("-", "");
        String entryKey = keyTenantSemanticEntry(tenantId, kbSnapshotVersion, entryId);

        SemanticEntry entry = SemanticEntry.fromPayload(payload);
        entry.setNormalizedQuestion(normalized);
        entry.setVector(similarityService.encodeVector(similarityService.embed(normalized)));
        entry.setCreatedAt(System.currentTimeMillis());

        redis.opsForValue().set(entryKey, JsonUtils.toJson(entry), TTL);
        redis.opsForZSet().add(keyTenantSemanticCandidates(tenantId, kbSnapshotVersion), entryId, entry.getCreatedAt());
        redis.expire(keyTenantSemanticCandidates(tenantId, kbSnapshotVersion), TTL);
    }

    // -------------------- helpers --------------------

    private Optional<CacheHit> readHit(String key, String scope, String type, Double score) {
        String json = redis.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return Optional.empty();
        }
        AnswerCachePayload payload = JsonParser.parsePayload(json);
        if (payload == null || !StringUtils.hasText(payload.getAnswer())) {
            return Optional.empty();
        }
        return Optional.of(new CacheHit(payload, scope, type, score));
    }

    private Optional<CacheHit> semanticMatch(String candidatesKey,
                                            String entryPrefix,
                                            String normalizedQuestion,
                                            String scope) {
        // 取最近 N 条候选
        var range = redis.opsForZSet().reverseRange(candidatesKey, 0, SEMANTIC_CANDIDATES - 1);
        if (range == null || range.isEmpty()) {
            return Optional.empty();
        }

        List<Float> qv = similarityService.embed(normalizedQuestion);
        if (qv.isEmpty()) {
            return Optional.empty();
        }

        return range.stream()
                .map(entryId -> {
                    String json = redis.opsForValue().get(entryPrefix + entryId);
                    SemanticEntry entry = JsonParser.parseSemantic(json);
                    if (entry == null || !StringUtils.hasText(entry.getVector())) {
                        return null;
                    }
                    double sim = similarityService.cosine(qv, similarityService.decodeVector(entry.getVector()));
                    if (sim < SEMANTIC_THRESHOLD) {
                        return null;
                    }
                    AnswerCachePayload payload = entry.toPayload();
                    return new CacheHit(payload, scope, "SEMANTIC", sim);
                })
                .filter(hit -> hit != null && hit.getPayload() != null && StringUtils.hasText(hit.getPayload().getAnswer()))
                .max(Comparator.comparing(CacheHit::getSimilarityScore));
    }

    private String keyUserExact(Long tenantId, Long userId, String kbSnapshotVersion, String normalized) {
        return "legal:qa:cache:exact:user:" + tenantId + ":" + userId + ":" + kbSnapshotVersion + ":" + md5(normalized);
    }

    private String keyTenantExact(Long tenantId, String kbSnapshotVersion, String normalized) {
        return "legal:qa:cache:exact:tenant:" + tenantId + ":" + kbSnapshotVersion + ":" + md5(normalized);
    }

    private String keyUserSemanticCandidates(Long tenantId, Long userId, String kbSnapshotVersion) {
        return "legal:qa:cache:semantic:candidates:user:" + tenantId + ":" + userId + ":" + kbSnapshotVersion;
    }

    private String keyTenantSemanticCandidates(Long tenantId, String kbSnapshotVersion) {
        return "legal:qa:cache:semantic:candidates:tenant:" + tenantId + ":" + kbSnapshotVersion;
    }

    private String keyUserSemanticEntry(Long tenantId, Long userId, String kbSnapshotVersion, String entryId) {
        return scopeEntryPrefixUser(tenantId, userId, kbSnapshotVersion) + entryId;
    }

    private String scopeEntryPrefixUser(Long tenantId, Long userId, String kbSnapshotVersion) {
        return "legal:qa:cache:semantic:entry:user:" + tenantId + ":" + userId + ":" + kbSnapshotVersion + ":";
    }

    private String keyTenantSemanticEntry(Long tenantId, String kbSnapshotVersion, String entryId) {
        return scopeEntryPrefixTenant(tenantId, kbSnapshotVersion) + entryId;
    }

    private String scopeEntryPrefixTenant(Long tenantId, String kbSnapshotVersion) {
        return "legal:qa:cache:semantic:entry:tenant:" + tenantId + ":" + kbSnapshotVersion + ":";
    }

    private String md5(String s) {
        return DigestUtils.md5DigestAsHex(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 为避免引入额外 JSON 依赖封装，简单做一个内部 entry。
     */
    public static class SemanticEntry {
        private String normalizedQuestion;
        private String vector;
        private Long createdAt;
        private AnswerCachePayload payload;

        public static SemanticEntry fromPayload(AnswerCachePayload payload) {
            SemanticEntry e = new SemanticEntry();
            e.payload = payload;
            return e;
        }

        public AnswerCachePayload toPayload() {
            return payload;
        }

        public String getNormalizedQuestion() {
            return normalizedQuestion;
        }

        public void setNormalizedQuestion(String normalizedQuestion) {
            this.normalizedQuestion = normalizedQuestion;
        }

        public String getVector() {
            return vector;
        }

        public void setVector(String vector) {
            this.vector = vector;
        }

        public Long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Long createdAt) {
            this.createdAt = createdAt;
        }

        public AnswerCachePayload getPayload() {
            return payload;
        }

        public void setPayload(AnswerCachePayload payload) {
            this.payload = payload;
        }
    }

    /**
     * 轻量 JSON 解析器：避免修改 JsonUtils（当前仅有 toJson）。
     *
     * <p>这里使用 Jackson 的 ObjectMapper 直接解析。</p>
     */
    static class JsonParser {
        private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

        static AnswerCachePayload parsePayload(String json) {
            try {
                return MAPPER.readValue(json, AnswerCachePayload.class);
            } catch (Exception e) {
                return null;
            }
        }

        static SemanticEntry parseSemantic(String json) {
            if (!StringUtils.hasText(json)) {
                return null;
            }
            try {
                return MAPPER.readValue(json, SemanticEntry.class);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
