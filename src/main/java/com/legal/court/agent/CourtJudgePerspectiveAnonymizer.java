package com.legal.court.agent;

import com.legal.enums.CourtCaseRole;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 智能小法庭法官视角当事人匿名化工具。
 */
@Component
public class CourtJudgePerspectiveAnonymizer {

    /**
     * 对法官输入做去用户立场化处理，统一以 PartyA / PartyB 表示双方。
     */
    public String anonymize(String text, CourtCaseRole userSide) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String result = text;
        for (Map.Entry<String, String> entry : aliases(userSide).entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * 返回前端渲染时可用于回填真实身份的映射。
     */
    public Map<String, CourtCaseRole> partyRoleMapping(CourtCaseRole userSide) {
        Map<String, CourtCaseRole> mapping = new LinkedHashMap<>();
        mapping.put("PartyA", CourtCaseRole.PLAINTIFF);
        mapping.put("PartyB", CourtCaseRole.DEFENDANT);
        return mapping;
    }

    private Map<String, String> aliases(CourtCaseRole userSide) {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("原告", "PartyA");
        aliases.put("被告", "PartyB");
        aliases.put("用户", "当事人");
        aliases.put("我方", userSide == CourtCaseRole.DEFENDANT ? "PartyB" : "PartyA");
        aliases.put("对方", userSide == CourtCaseRole.DEFENDANT ? "PartyA" : "PartyB");
        aliases.put("用户方", userSide == CourtCaseRole.DEFENDANT ? "PartyB" : "PartyA");
        return aliases;
    }
}
