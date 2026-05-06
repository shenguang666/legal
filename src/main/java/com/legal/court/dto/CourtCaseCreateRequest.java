package com.legal.court.dto;

import com.legal.enums.CourtUserSide;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能小法庭案件创建请求。
 */
@Data
public class CourtCaseCreateRequest {

    /** 案件标题。 */
    private String title;
    /** 用户诉讼立场。 */
    private CourtUserSide userSide;
    /** 案件简要描述。 */
    private String caseSummary;
    /** 用户核心诉求或抗辩目标。 */
    private String userObjective;
    /** 作为案件证据引用的文档ID列表。 */
    private List<Long> documentIds = new ArrayList<>();
}
