package com.legal.chat.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.chat.dto.ChatMessageDto;
import com.legal.chat.entity.ChatMessageEntity;
import com.legal.chat.mapper.ChatMessageMapper;
import com.legal.common.JsonUtils;
import com.legal.enums.ChatMessageRole;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话消息列表缓存：先读 Redis，再读 MySQL。
 *
 * <p>缓存结构使用 Redis List，每条元素为一条 ChatMessageDto 的 JSON。</p>
 * <p>一致性策略：写入/删除优先 MySQL，Redis 仅做最终一致的缓存。</p>
 */
@Service
public class ChatMessageCacheService {

    private final StringRedisTemplate redis;
    private final ChatMessageMapper chatMessageMapper;

    public ChatMessageCacheService(StringRedisTemplate redis, ChatMessageMapper chatMessageMapper) {
        this.redis = redis;
        this.chatMessageMapper = chatMessageMapper;
    }

    public List<ChatMessageDto> listMessages(Long tenantId, Long userId, String sessionId, Duration ttl) {
        String key = keyMessages(tenantId, userId, sessionId);
        Long size = redis.opsForList().size(key);
        if (size != null && size > 0) {
            List<String> raw = redis.opsForList().range(key, 0, -1);
            return parseDtoList(raw);
        }

        List<ChatMessageDto> dtos = chatMessageMapper.selectList(
                        new LambdaQueryWrapper<ChatMessageEntity>()
                                .eq(ChatMessageEntity::getSessionId, sessionId)
                                .orderByAsc(ChatMessageEntity::getCreatedAt)
                ).stream()
                .map(this::toDto)
                .toList();

        // 回填缓存（按时间升序写入）
        if (!dtos.isEmpty()) {
            List<String> jsonList = dtos.stream().map(JsonUtils::toJson).toList();
            redis.opsForList().rightPushAll(key, jsonList);
            redis.expire(key, ttl);
        }
        return dtos;
    }

    /**
     * 读取用于上下文的最近 N 条历史消息（不包含 excludeMessageId）。
     */
    public List<ChatMessageDto> recentMessages(Long tenantId,
                                               Long userId,
                                               String sessionId,
                                               int limit,
                                               Long excludeMessageId,
                                               Duration ttl) {
        if (limit <= 0) {
            return List.of();
        }
        String key = keyMessages(tenantId, userId, sessionId);
        Long size = redis.opsForList().size(key);
        if (size == null || size == 0) {
            // 尝试触发一次回填
            listMessages(tenantId, userId, sessionId, ttl);
        }
        List<String> raw = redis.opsForList().range(key, -Math.max(1, limit + 5L), -1);
        List<ChatMessageDto> dtos = parseDtoList(raw);
        if (excludeMessageId != null) {
            dtos = dtos.stream().filter(d -> d.getMessageId() == null || !excludeMessageId.equals(d.getMessageId())).toList();
        }
        // 保持原顺序，截取最后 limit 条
        if (dtos.size() > limit) {
            return dtos.subList(dtos.size() - limit, dtos.size());
        }
        return dtos;
    }

    /**
     * 写入消息后，若缓存存在则追加；缓存不存在则忽略（下次读时从 MySQL 回填）。
     */
    public void appendIfPresent(Long tenantId, Long userId, String sessionId, ChatMessageEntity entity, Duration ttl) {
        String key = keyMessages(tenantId, userId, sessionId);
        Boolean exists = redis.hasKey(key);
        if (Boolean.FALSE.equals(exists)) {
            return;
        }
        ChatMessageDto dto = toDto(entity);
        String json = JsonUtils.toJson(dto);
        redis.opsForList().rightPush(key, json);
        redis.expire(key, ttl);
    }

    private List<ChatMessageDto> parseDtoList(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<ChatMessageDto> out = new ArrayList<>();
        for (String s : raw) {
            if (!StringUtils.hasText(s)) {
                continue;
            }
            try {
                out.add(JsonUtils.fromJson(s, ChatMessageDto.class));
            } catch (Exception ignored) {
                // 单条解析失败不影响整体，触发回源时可刷新
            }
        }
        return out;
    }

    private ChatMessageDto toDto(ChatMessageEntity entity) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setMessageId(entity.getMessageId());
        dto.setRole(entity.getRole() == null ? null : entity.getRole().getCode());
        dto.setContent(entity.getContent());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private String keyMessages(Long tenantId, Long userId, String sessionId) {
        return "legal:chat:messages:" + tenantId + ":" + userId + ":" + sessionId;
    }
}
