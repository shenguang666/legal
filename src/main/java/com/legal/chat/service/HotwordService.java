package com.legal.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.legal.chat.dto.HotwordDtos;
import com.legal.chat.dto.HotwordStatusUpdateRequest;
import com.legal.chat.dto.HotwordUpsertRequest;
import com.legal.chat.entity.ChatHotwordEntity;
import com.legal.chat.mapper.ChatHotwordMapper;
import com.legal.common.AppException;
import com.legal.common.JsonUtils;
import com.legal.security.AuthPrincipal;
import com.legal.security.IdempotencyService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class HotwordService {

    private static final Duration CACHE_TTL = Duration.ofDays(1);
    private static final Duration ANSWER_CACHE_TTL = Duration.ofDays(1);
    private static final int DEFAULT_RANDOM_LIMIT = 3;
    private static final int MAX_RANDOM_LIMIT = 20;

    private final ChatHotwordMapper hotwordMapper;
    private final StringRedisTemplate redis;
    private final IdempotencyService idempotencyService;

    public HotwordService(ChatHotwordMapper hotwordMapper,
                          StringRedisTemplate redis,
                          IdempotencyService idempotencyService) {
        this.hotwordMapper = hotwordMapper;
        this.redis = redis;
        this.idempotencyService = idempotencyService;
    }

    public HotwordDtos.PageResult page(AuthPrincipal principal, int pageNo, int pageSize) {
        int safePageNo = Math.max(1, pageNo);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        Page<ChatHotwordEntity> page = hotwordMapper.selectPage(
                Page.of(safePageNo, safePageSize),
                new LambdaQueryWrapper<ChatHotwordEntity>()
                        .eq(ChatHotwordEntity::getTenantId, principal.tenantId())
                        .eq(ChatHotwordEntity::getDeleted, false)
                        .orderByDesc(ChatHotwordEntity::getEnabled)
                        .orderByDesc(ChatHotwordEntity::getWeight)
                        .orderByAsc(ChatHotwordEntity::getSortOrder)
                        .orderByDesc(ChatHotwordEntity::getUpdatedAt)
        );
        HotwordDtos.PageResult result = new HotwordDtos.PageResult();
        result.setTotal(page.getTotal());
        result.setPageNo(safePageNo);
        result.setPageSize(safePageSize);
        result.setItems(page.getRecords().stream().map(this::toItem).toList());
        return result;
    }

    @Transactional
    public HotwordDtos.Item create(AuthPrincipal principal, HotwordUpsertRequest request) {
        idempotencyService.ensureUnique(principal, "hotword:create", request.getRequestId());
        LocalDateTime now = LocalDateTime.now();
        ChatHotwordEntity entity = new ChatHotwordEntity();
        entity.setTenantId(principal.tenantId());
        entity.setDeleted(false);
        entity.setCreatedBy(principal.userId());
        entity.setUpdatedBy(principal.userId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        applyEditableFields(entity, request);
        ensureHotwordKeyAvailable(principal.tenantId(), entity.getHotwordKey(), null);
        hotwordMapper.insert(entity);
        evictHotwordCaches(principal.tenantId(), entity.getHotwordKey());
        return toItem(entity);
    }

    @Transactional
    public HotwordDtos.Item update(AuthPrincipal principal, Long hotwordId, HotwordUpsertRequest request) {
        idempotencyService.ensureUnique(principal, "hotword:update", request.getRequestId());
        ChatHotwordEntity entity = requireTenantHotword(principal, hotwordId);
        String oldHotwordKey = entity.getHotwordKey();
        applyEditableFields(entity, request);
        ensureHotwordKeyAvailable(principal.tenantId(), entity.getHotwordKey(), hotwordId);
        entity.setUpdatedBy(principal.userId());
        entity.setUpdatedAt(LocalDateTime.now());
        hotwordMapper.updateById(entity);
        evictHotwordCaches(principal.tenantId(), oldHotwordKey);
        evictHotwordCaches(principal.tenantId(), entity.getHotwordKey());
        return toItem(entity);
    }

    @Transactional
    public HotwordDtos.Item updateStatus(AuthPrincipal principal, Long hotwordId, HotwordStatusUpdateRequest request) {
        idempotencyService.ensureUnique(principal, "hotword:update-status", request.getRequestId());
        ChatHotwordEntity entity = requireTenantHotword(principal, hotwordId);
        entity.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        entity.setUpdatedBy(principal.userId());
        entity.setUpdatedAt(LocalDateTime.now());
        hotwordMapper.updateById(entity);
        evictHotwordCaches(principal.tenantId(), entity.getHotwordKey());
        return toItem(entity);
    }

    @Transactional
    public void delete(AuthPrincipal principal, Long hotwordId, String requestId) {
        idempotencyService.ensureUnique(principal, "hotword:delete", requestId);
        ChatHotwordEntity entity = requireTenantHotword(principal, hotwordId);
        entity.setDeleted(true);
        entity.setEnabled(false);
        entity.setUpdatedBy(principal.userId());
        entity.setUpdatedAt(LocalDateTime.now());
        hotwordMapper.updateById(entity);
        evictHotwordCaches(principal.tenantId(), entity.getHotwordKey());
    }

    public List<HotwordDtos.RandomItem> random(AuthPrincipal principal, Integer limit) {
        int safeLimit = limit == null ? DEFAULT_RANDOM_LIMIT : Math.max(1, Math.min(limit, MAX_RANDOM_LIMIT));
        List<HotwordDtos.RandomItem> pool = enabledPool(principal.tenantId());
        if (pool.isEmpty()) {
            return List.of();
        }
        List<HotwordDtos.RandomItem> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(safeLimit, shuffled.size()));
    }

    public Optional<String> findPresetAnswer(Long tenantId, String hotwordKey) {
        if (!StringUtils.hasText(hotwordKey)) {
            return Optional.empty();
        }
        String normalizedKey = hotwordKey.trim();
        String cacheKey = presetAnswerCacheKey(tenantId, normalizedKey);
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            return StringUtils.hasText(cached) ? Optional.of(cached) : Optional.empty();
        }
        ChatHotwordEntity entity = hotwordMapper.selectOne(
                new LambdaQueryWrapper<ChatHotwordEntity>()
                        .eq(ChatHotwordEntity::getTenantId, tenantId)
                        .eq(ChatHotwordEntity::getHotwordKey, normalizedKey)
                        .eq(ChatHotwordEntity::getDeleted, false)
                        .eq(ChatHotwordEntity::getEnabled, true)
                        .last("limit 1")
        );
        if (entity == null || !StringUtils.hasText(entity.getPresetAnswer())) {
            return Optional.empty();
        }
        String presetAnswer = entity.getPresetAnswer().trim();
        redis.opsForValue().set(cacheKey, presetAnswer, ANSWER_CACHE_TTL);
        return Optional.of(presetAnswer);
    }

    private List<HotwordDtos.RandomItem> enabledPool(Long tenantId) {
        String key = enabledCacheKey(tenantId);
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            try {
                return JsonUtils.fromJsonList(cached, HotwordDtos.RandomItem.class);
            } catch (Exception ignored) {
                redis.delete(key);
            }
        }
        List<HotwordDtos.RandomItem> items = hotwordMapper.selectList(
                        new LambdaQueryWrapper<ChatHotwordEntity>()
                                .eq(ChatHotwordEntity::getTenantId, tenantId)
                                .eq(ChatHotwordEntity::getDeleted, false)
                                .eq(ChatHotwordEntity::getEnabled, true)
                                .orderByDesc(ChatHotwordEntity::getWeight)
                                .orderByAsc(ChatHotwordEntity::getSortOrder)
                ).stream()
                .map(this::toRandomItem)
                .toList();
        redis.opsForValue().set(key, JsonUtils.toJson(items), CACHE_TTL);
        return items;
    }

    private void applyEditableFields(ChatHotwordEntity entity, HotwordUpsertRequest request) {
        if (!StringUtils.hasText(request.getContent())) {
            throw AppException.badRequest("热词内容不能为空");
        }
        String content = request.getContent().trim();
        if (content.length() > 255) {
            throw AppException.badRequest("热词内容不能超过 255 个字符");
        }
        String presetAnswer = request.getPresetAnswer().trim();
        if (presetAnswer.length() > 4000) {
            throw AppException.badRequest("预设答案不能超过 4000 个字符");
        }
        entity.setContent(content);
        entity.setHotwordKey(resolveHotwordKey(request.getHotwordKey(), content));
        entity.setPresetAnswer(presetAnswer);
        entity.setCategory(StringUtils.hasText(request.getCategory()) ? request.getCategory().trim() : null);
        entity.setWeight(request.getWeight() == null ? 0 : request.getWeight());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setEnabled(request.getEnabled() == null || Boolean.TRUE.equals(request.getEnabled()));
    }

    private String resolveHotwordKey(String requestedKey, String content) {
        String key = StringUtils.hasText(requestedKey) ? requestedKey.trim() : "hotword_" + Integer.toUnsignedString(content.hashCode(), 36);
        if (!key.matches("[A-Za-z0-9_\\-]{3,64}")) {
            throw AppException.badRequest("热词标记仅支持 3-64 位英文、数字、中划线或下划线");
        }
        return key;
    }

    private void ensureHotwordKeyAvailable(Long tenantId, String hotwordKey, Long currentHotwordId) {
        ChatHotwordEntity exists = hotwordMapper.selectOne(
                new LambdaQueryWrapper<ChatHotwordEntity>()
                        .eq(ChatHotwordEntity::getTenantId, tenantId)
                        .eq(ChatHotwordEntity::getHotwordKey, hotwordKey)
                        .eq(ChatHotwordEntity::getDeleted, false)
                        .last("limit 1")
        );
        if (exists != null && (currentHotwordId == null || !exists.getHotwordId().equals(currentHotwordId))) {
            throw AppException.badRequest("热词标记已存在");
        }
    }

    private ChatHotwordEntity requireTenantHotword(AuthPrincipal principal, Long hotwordId) {
        ChatHotwordEntity entity = hotwordMapper.selectOne(
                new LambdaQueryWrapper<ChatHotwordEntity>()
                        .eq(ChatHotwordEntity::getHotwordId, hotwordId)
                        .eq(ChatHotwordEntity::getTenantId, principal.tenantId())
                        .eq(ChatHotwordEntity::getDeleted, false)
                        .last("limit 1")
        );
        if (entity == null) {
            throw AppException.notFound("热词不存在或无权操作");
        }
        return entity;
    }

    private void evictHotwordCaches(Long tenantId, String hotwordKey) {
        redis.delete(enabledCacheKey(tenantId));
        if (StringUtils.hasText(hotwordKey)) {
            redis.delete(presetAnswerCacheKey(tenantId, hotwordKey));
        }
    }

    private String enabledCacheKey(Long tenantId) {
        return "legal:chat:hotword:enabled:" + tenantId;
    }

    private String presetAnswerCacheKey(Long tenantId, String hotwordKey) {
        return "legal:chat:hotword:answer:" + tenantId + ":" + hotwordKey;
    }

    private HotwordDtos.Item toItem(ChatHotwordEntity entity) {
        HotwordDtos.Item item = new HotwordDtos.Item();
        item.setHotwordId(entity.getHotwordId());
        item.setHotwordKey(entity.getHotwordKey());
        item.setContent(entity.getContent());
        item.setPresetAnswer(entity.getPresetAnswer());
        item.setCategory(entity.getCategory());
        item.setWeight(entity.getWeight());
        item.setSortOrder(entity.getSortOrder());
        item.setEnabled(entity.getEnabled());
        item.setCreatedAt(entity.getCreatedAt());
        item.setUpdatedAt(entity.getUpdatedAt());
        return item;
    }

    private HotwordDtos.RandomItem toRandomItem(ChatHotwordEntity entity) {
        HotwordDtos.RandomItem item = new HotwordDtos.RandomItem();
        item.setHotwordId(entity.getHotwordId());
        item.setHotwordKey(entity.getHotwordKey());
        item.setContent(entity.getContent());
        item.setCategory(entity.getCategory());
        return item;
    }
}
