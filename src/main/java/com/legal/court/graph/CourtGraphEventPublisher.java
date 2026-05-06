package com.legal.court.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legal.court.entity.CourtGraphEventEntity;
import com.legal.court.mapper.CourtGraphEventMapper;
import com.legal.enums.CourtGraphEventStatus;
import com.legal.enums.CourtGraphEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 智能小法庭图谱事件发布器。
 */
@Service
@Slf4j
public class CourtGraphEventPublisher {

    private final CourtGraphEventMapper courtGraphEventMapper;
    private final ObjectMapper objectMapper;

    public CourtGraphEventPublisher(CourtGraphEventMapper courtGraphEventMapper, ObjectMapper objectMapper) {
        this.courtGraphEventMapper = courtGraphEventMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 发布图谱事件，业务代码只能通过事件入队，不能直接写 Neo4j。
     */
    @Transactional
    public CourtGraphEventEntity publish(Long tenantId,
                                         Long caseId,
                                         Long roundId,
                                         CourtGraphEventType eventType,
                                         CourtGraphEventPayload payload) {
        CourtGraphEventEntity entity = new CourtGraphEventEntity();
        entity.setTenantId(tenantId);
        entity.setCaseId(caseId);
        entity.setRoundId(roundId);
        entity.setEventType(eventType);
        entity.setPayloadJson(toJson(payload));
        entity.setStatus(CourtGraphEventStatus.PENDING);
        entity.setRetryCount(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        courtGraphEventMapper.insert(entity);
        log.info("graph.event.publish tenantId={} caseId={} roundId={} eventId={} eventType={}",
                tenantId, caseId, roundId, entity.getEventId(), eventType);
        return entity;
    }

    private String toJson(CourtGraphEventPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("图谱事件载荷序列化失败", ex);
        }
    }
}
