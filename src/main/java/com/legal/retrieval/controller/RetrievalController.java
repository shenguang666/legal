package com.legal.retrieval.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.legal.chat.rag.ChunkRetriever;
import com.legal.chat.rag.RetrievedChunk;
import com.legal.common.ApiResponse;
import com.legal.config.RagProperties;
import com.legal.retrieval.dto.RetrievePreviewRequest;
import com.legal.retrieval.dto.RetrievePreviewResponse;
import com.legal.retrieval.dto.RetrievedChunkDto;
import com.legal.security.AuthContextHolder;
import com.legal.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SaCheckRole("ADMIN")
@Validated
@RestController
@RequestMapping("/api/retrieval")
public class RetrievalController {

    private final ChunkRetriever chunkRetriever;
    private final RagProperties ragProperties;

    public RetrievalController(ChunkRetriever chunkRetriever, RagProperties ragProperties) {
        this.chunkRetriever = chunkRetriever;
        this.ragProperties = ragProperties;
    }

    @PostMapping("/preview")
    public ApiResponse<RetrievePreviewResponse> preview(@Valid @RequestBody RetrievePreviewRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        int topK = request.getTopK() == null ? ragProperties.getTopK() : Math.max(1, request.getTopK());
        List<RetrievedChunk> hits = chunkRetriever.retrieve(principal.tenantId(), request.getQuestion(), topK);
        List<RetrievedChunkDto> chunks = hits.stream()
                .map(this::toDto)
                .toList();
        return ApiResponse.ok(new RetrievePreviewResponse(chunks));
    }

    private RetrievedChunkDto toDto(RetrievedChunk chunk) {
        RetrievedChunkDto dto = new RetrievedChunkDto();
        dto.setChunkId(chunk.getChunkId());
        dto.setDocumentId(chunk.getDocumentId());
        dto.setChunkOrder(chunk.getChunkOrder());
        dto.setSource(chunk.getSource());
        dto.setContent(chunk.getContent());
        return dto;
    }
}
