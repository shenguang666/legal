package com.legal.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.legal.common.ApiResponse;
import com.legal.knowledge.dto.ChunkDto;
import com.legal.knowledge.dto.CreateDocumentRequest;
import com.legal.knowledge.dto.DocumentDto;
import com.legal.knowledge.service.KnowledgeService;
import com.legal.security.AuthContextHolder;
import com.legal.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@SaCheckRole("ADMIN")
@Validated
@RestController
@RequestMapping("/api/knowledge/documents")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping
    public ApiResponse<DocumentDto> create(@Valid @RequestBody CreateDocumentRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(knowledgeService.createDocument(principal, request));
    }

    @PostMapping("/import")
    public ApiResponse<DocumentDto> importFile(@RequestParam("requestId") String requestId,
                                               @RequestParam("file") MultipartFile file,
                                               @RequestParam(value = "title", required = false) String title,
                                               @RequestParam(value = "source", required = false) String source,
                                               @RequestParam(value = "chunkSize", required = false ) Integer chunkSize ,
                                               @RequestParam(value = "chunkOverlap", required = false) Integer chunkOverlap) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(knowledgeService.importDocument(principal, requestId, file, title, source, chunkSize, chunkOverlap));
    }

    @GetMapping
    public ApiResponse<List<DocumentDto>> list() {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(knowledgeService.listDocuments(principal));
    }

    @GetMapping("/{id}/chunks")
    public ApiResponse<List<ChunkDto>> listChunks(@PathVariable("id") Long id) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(knowledgeService.listChunks(principal, id));
    }

    @PostMapping("/{id}/index")
    public ApiResponse<DocumentDto> index(@PathVariable("id") Long id,
                                          @RequestParam("requestId") String requestId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(knowledgeService.triggerIndex(principal, id, requestId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<DocumentDto> delete(@PathVariable("id") Long id,
                                           @RequestParam("requestId") String requestId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(knowledgeService.deleteDocument(principal, id, requestId));
    }
}
