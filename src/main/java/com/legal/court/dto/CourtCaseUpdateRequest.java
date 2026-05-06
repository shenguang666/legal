package com.legal.court.dto;

import com.legal.enums.CourtUserSide;
import lombok.Data;

/**
 * 智能小法庭案件更新请求。
 */
@Data
public class CourtCaseUpdateRequest {

    /** 案件标题。 */
    private String title;
    /** 用户诉讼立场。 */
    private CourtUserSide userSide;
    /** 案件简要描述。 */
    private String caseSummary;
    /** 用户核心诉求或抗辩目标。 */
    private String userObjective;
}
