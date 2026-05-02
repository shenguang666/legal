package com.legal.chat.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.legal.chat.dto.HotwordDtos;
import com.legal.chat.dto.HotwordStatusUpdateRequest;
import com.legal.chat.dto.HotwordUpsertRequest;
import com.legal.chat.service.HotwordService;
import com.legal.common.ApiResponse;
import com.legal.security.AuthContextHolder;
import com.legal.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/hotwords")
public class HotwordController {

    private final HotwordService hotwordService;

    public HotwordController(HotwordService hotwordService) {
        this.hotwordService = hotwordService;
    }

    @GetMapping("/random")
    public ApiResponse<List<HotwordDtos.RandomItem>> random(@RequestParam(value = "limit", defaultValue = "3") Integer limit) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(hotwordService.random(principal, limit));
    }

    @SaCheckRole("ADMIN")
    @GetMapping
    public ApiResponse<HotwordDtos.PageResult> page(@RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                                    @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(hotwordService.page(principal, pageNo, pageSize));
    }

    @SaCheckRole("ADMIN")
    @PostMapping
    public ApiResponse<HotwordDtos.Item> create(@Valid @RequestBody HotwordUpsertRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(hotwordService.create(principal, request));
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/{hotwordId}")
    public ApiResponse<HotwordDtos.Item> updateByPost(@PathVariable Long hotwordId,
                                                      @Valid @RequestBody HotwordUpsertRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(hotwordService.update(principal, hotwordId, request));
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/{hotwordId}")
    public ApiResponse<HotwordDtos.Item> update(@PathVariable Long hotwordId,
                                                @Valid @RequestBody HotwordUpsertRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(hotwordService.update(principal, hotwordId, request));
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/{hotwordId}/status")
    public ApiResponse<HotwordDtos.Item> updateStatus(@PathVariable Long hotwordId,
                                                      @Valid @RequestBody HotwordStatusUpdateRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(hotwordService.updateStatus(principal, hotwordId, request));
    }

    @SaCheckRole("ADMIN")
    @DeleteMapping("/{hotwordId}")
    public ApiResponse<Void> delete(@PathVariable Long hotwordId,
                                    @RequestParam("requestId") String requestId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        hotwordService.delete(principal, hotwordId, requestId);
        return ApiResponse.ok(null);
    }
}
