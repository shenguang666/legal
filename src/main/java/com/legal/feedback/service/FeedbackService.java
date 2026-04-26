package com.legal.feedback.service;

import com.legal.feedback.dto.FeedbackRequest;
import com.legal.feedback.entity.QaFeedbackEntity;
import com.legal.feedback.mapper.QaFeedbackMapper;
import com.legal.security.AuthPrincipal;
import com.legal.security.IdempotencyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FeedbackService {

    private final QaFeedbackMapper qaFeedbackMapper;
    private final IdempotencyService idempotencyService;

    public FeedbackService(QaFeedbackMapper qaFeedbackMapper,
                           IdempotencyService idempotencyService) {
        this.qaFeedbackMapper = qaFeedbackMapper;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public Long submit(AuthPrincipal principal, FeedbackRequest request) {
        idempotencyService.ensureUnique(principal, "feedback:submit", request.getRequestId());
        QaFeedbackEntity entity = new QaFeedbackEntity();
        entity.setMessageId(request.getMessageId());
        entity.setHelpful(request.getHelpful());
        entity.setComment(request.getComment());
        entity.setCreatedAt(LocalDateTime.now());
        qaFeedbackMapper.insert(entity);
        return entity.getFeedbackId();
    }
}
