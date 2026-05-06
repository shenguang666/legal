package com.legal.knowledge.controller;

import com.legal.common.ApiResponse;
import com.legal.knowledge.service.DocumentAssetService;
import com.legal.security.AuthContextHolder;
import com.legal.security.AuthPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/document-assets")
public class DocumentAssetController {

    private final DocumentAssetService documentAssetService;

    public DocumentAssetController(DocumentAssetService documentAssetService) {
        this.documentAssetService = documentAssetService;
    }

    @GetMapping("/{documentId}/{assetName}")
    public ApiResponse<String> accessUrl(@PathVariable("documentId") Long documentId,
                                         @PathVariable("assetName") String assetName) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(documentAssetService.createAccessUrl(principal, documentId, assetName));
    }
}
