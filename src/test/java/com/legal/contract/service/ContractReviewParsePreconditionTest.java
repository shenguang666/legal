package com.legal.contract.service;

import com.legal.common.AppException;
import com.legal.contract.dto.ContractReviewTriggerRequest;
import com.legal.contract.entity.ContractReviewEntity;
import com.legal.contract.entity.ContractReviewTaskEntity;
import com.legal.contract.mapper.ContractReviewFieldMapper;
import com.legal.contract.mapper.ContractReviewMapper;
import com.legal.contract.mapper.ContractReviewTaskMapper;
import com.legal.contract.mapper.ContractRiskItemMapper;
import com.legal.enums.DocumentParseStatus;
import com.legal.enums.KbDocumentBizType;
import com.legal.enums.KbDocumentStatus;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.mapper.KbDocumentMapper;
import com.legal.security.AuthPrincipal;
import com.legal.security.IdempotencyService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractReviewParsePreconditionTest {

    @ParameterizedTest
    @EnumSource(value = DocumentParseStatus.class, names = {"PENDING", "PROCESSING"})
    void triggerReviewShouldRejectDocumentsStillParsing(DocumentParseStatus status) {
        ContractReviewMapper reviewMapper = mock(ContractReviewMapper.class);
        ContractReviewTaskMapper taskMapper = mock(ContractReviewTaskMapper.class);
        KbDocumentMapper documentMapper = mock(KbDocumentMapper.class);
        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        ContractReviewService service = service(reviewMapper, taskMapper, documentMapper, idempotencyService);
        when(documentMapper.selectOne(any())).thenReturn(document(status, null));

        assertThatThrownBy(() -> service.triggerReview(principal(), request()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("文档仍在解析中");

        verify(idempotencyService).ensureUnique(eq(principal()), eq("contract-review:create"), eq("req-1"));
        verify(reviewMapper, never()).insert(any(ContractReviewEntity.class));
        verify(taskMapper, never()).insert(any(ContractReviewTaskEntity.class));
    }

    @Test
    void triggerReviewShouldRejectFailedParsingWithFailureReason() {
        ContractReviewMapper reviewMapper = mock(ContractReviewMapper.class);
        ContractReviewTaskMapper taskMapper = mock(ContractReviewTaskMapper.class);
        KbDocumentMapper documentMapper = mock(KbDocumentMapper.class);
        ContractReviewService service = service(reviewMapper, taskMapper, documentMapper, mock(IdempotencyService.class));
        when(documentMapper.selectOne(any())).thenReturn(document(DocumentParseStatus.FAILED, "MinerU 解析失败"));

        assertThatThrownBy(() -> service.triggerReview(principal(), request()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("文档解析失败，无法启动天眼审查：MinerU 解析失败");

        verify(reviewMapper, never()).insert(any(ContractReviewEntity.class));
        verify(taskMapper, never()).insert(any(ContractReviewTaskEntity.class));
    }

    private ContractReviewService service(ContractReviewMapper reviewMapper,
                                          ContractReviewTaskMapper taskMapper,
                                          KbDocumentMapper documentMapper,
                                          IdempotencyService idempotencyService) {
        return new ContractReviewService(
                reviewMapper,
                mock(ContractReviewFieldMapper.class),
                mock(ContractRiskItemMapper.class),
                taskMapper,
                documentMapper,
                idempotencyService
        );
    }

    private AuthPrincipal principal() {
        return new AuthPrincipal(1L, 2L, "ADMIN");
    }

    private ContractReviewTriggerRequest request() {
        ContractReviewTriggerRequest request = new ContractReviewTriggerRequest();
        request.setRequestId("req-1");
        request.setDocumentId(10L);
        return request;
    }

    private KbDocumentEntity document(DocumentParseStatus parseStatus, String failureReason) {
        KbDocumentEntity document = new KbDocumentEntity();
        document.setDocumentId(10L);
        document.setTenantId(1L);
        document.setOwnerUserId(2L);
        document.setBizType(KbDocumentBizType.TIANYAN_REVIEW);
        document.setStatus(KbDocumentStatus.ACTIVE);
        document.setDocVersion(1);
        document.setParseStatus(parseStatus);
        document.setParseFailureReason(failureReason);
        return document;
    }
}
