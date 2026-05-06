package com.legal.court.controller;

import com.legal.common.ApiResponse;
import com.legal.court.dto.CourtGraphSnapshotDto;
import com.legal.court.graph.CourtGraphQueryService;
import com.legal.court.service.CourtCaseService;
import com.legal.security.AuthContextHolder;
import com.legal.security.AuthPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能小法庭图谱查询 Controller。
 */
@Validated
@RestController
@RequestMapping("/api/smart-court/cases")
public class SmartCourtGraphController {

    private final CourtGraphQueryService graphQueryService;
    private final CourtCaseService courtCaseService;

    public SmartCourtGraphController(CourtGraphQueryService graphQueryService, CourtCaseService courtCaseService) {
        this.graphQueryService = graphQueryService;
        this.courtCaseService = courtCaseService;
    }

    @GetMapping("/{caseId}/graph")
    public ApiResponse<CourtGraphSnapshotDto> graph(@PathVariable Long caseId,
                                                    @RequestParam(value = "focusClaimId", required = false) String focusClaimId,
                                                    @RequestParam(value = "hops", required = false) Integer hops,
                                                    @RequestParam(value = "limit", required = false) Integer limit) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        courtCaseService.requireCase(principal, caseId);
        return ApiResponse.ok(graphQueryService.queryCaseGraph(principal.tenantId(), caseId, focusClaimId, hops, limit));
    }
}
