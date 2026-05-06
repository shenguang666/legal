package com.legal.court.graph;

import com.legal.court.dto.CourtSuggestionGap;
import com.legal.enums.CourtSuggestionSeverity;
import com.legal.enums.CourtSuggestionType;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 智能小法庭补证建议规则引擎。
 */
@Slf4j
@Service
public class CourtSuggestionRuleEngine {

    /** 诉求未闭环规则查询。 */
    private static final String CLAIM_NOT_CLOSED_QUERY = """
            MATCH (c:Claim {tenantId:$tenantId, caseId:$caseId})
            WHERE NOT EXISTS {
              MATCH (c)-[:ASSERTS_FACT|SUPPORTED_BY*1..4]->(e:Evidence {tenantId:$tenantId, caseId:$caseId})
              WHERE coalesce(e.evidenceStatus, e.status, 'ACTIVE') <> 'INVALID'
            }
            RETURN c.businessId AS claimBusinessId,
                   c.content AS claimContent,
                   c.claimType AS claimType
            LIMIT $limit
            """;

    /** 金额冲突规则查询。 */
    private static final String AMOUNT_INCONSISTENT_QUERY = """
            MATCH (claim:Claim {tenantId:$tenantId, caseId:$caseId})-[:PROVES_AMOUNT|HAS_RISK*1..2]-(a:Amount {tenantId:$tenantId, caseId:$caseId})
            WITH claim, collect(DISTINCT a) AS amounts, count(DISTINCT coalesce(toString(a.amount), a.rawText)) AS amountValueCount
            WHERE size(amounts) > 1 AND amountValueCount > 1
            RETURN claim.businessId AS claimBusinessId,
                   [x IN amounts | x.businessId] AS amountBusinessIds,
                   [x IN amounts | coalesce(toString(x.amount), x.rawText)] AS amountValues
            LIMIT $limit
            """;

    /** 关键日期缺失规则查询。 */
    private static final String KEY_DATE_MISSING_QUERY = """
            MATCH (o:Obligation {tenantId:$tenantId, caseId:$caseId})
            WHERE NOT EXISTS {
              MATCH (o)-[:PROVES_DATE|DERIVED_FROM*1..2]-(d:Date {tenantId:$tenantId, caseId:$caseId})
            }
            RETURN o.businessId AS obligationBusinessId,
                   o.content AS obligationContent,
                   o.obligorRole AS obligorRole
            LIMIT $limit
            """;

    /** 对方抗辩未被反驳规则查询。 */
    private static final String DEFENSE_NOT_REBUTTED_QUERY = """
            MATCH (d:Defense {tenantId:$tenantId, caseId:$caseId})
            WHERE NOT EXISTS {
              MATCH (:Argument {tenantId:$tenantId, caseId:$caseId})-[:CHALLENGES_ARGUMENT]->(d)
            }
            RETURN d.businessId AS defenseBusinessId,
                   d.content AS defenseContent,
                   d.targetClaimBusinessId AS claimBusinessId
            LIMIT $limit
            """;

    private final Driver driver;

    public CourtSuggestionRuleEngine(Driver driver) {
        this.driver = driver;
    }

    /**
     * 执行四类补证缺口规则，返回结构化缺口列表。
     */
    public List<CourtSuggestionGap> detectGaps(Long tenantId, Long caseId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<CourtSuggestionGap> gaps = new ArrayList<>();
        try (Session session = driver.session()) {
            Map<String, Object> params = Map.of("tenantId", tenantId, "caseId", caseId, "limit", safeLimit);
            gaps.addAll(queryClaimNotClosed(session, params));
            gaps.addAll(queryAmountInconsistent(session, params));
            gaps.addAll(queryKeyDateMissing(session, params));
            gaps.addAll(queryDefenseNotRebutted(session, params));
        }
        return gaps;
    }

    private List<CourtSuggestionGap> queryClaimNotClosed(Session session, Map<String, Object> params) {
        return session.run(CLAIM_NOT_CLOSED_QUERY, params).list(record -> {
            CourtSuggestionGap gap = baseGap(CourtSuggestionType.CLAIM_NOT_CLOSED, CourtSuggestionSeverity.HIGH, record.asMap());
            gap.setClaimBusinessId(record.get("claimBusinessId").asString(null));
            gap.setMissingDescription("诉求缺少可达的有效证据链支撑");
            gap.getRecommendedMaterials().add("补充能够证明该诉求成立的合同条款、付款记录、验收记录或沟通记录");
            gap.getRelatedBusinessIds().add(gap.getClaimBusinessId());
            gap.setRationale("未发现 Claim 到有效 Evidence 的 4 跳内可达路径");
            return gap;
        });
    }

    private List<CourtSuggestionGap> queryAmountInconsistent(Session session, Map<String, Object> params) {
        return session.run(AMOUNT_INCONSISTENT_QUERY, params).list(record -> {
            CourtSuggestionGap gap = baseGap(CourtSuggestionType.AMOUNT_INCONSISTENT, CourtSuggestionSeverity.MEDIUM, record.asMap());
            gap.setClaimBusinessId(record.get("claimBusinessId").asString(null));
            gap.setMissingDescription("同一争议事项存在多个金额值且不一致");
            gap.getRecommendedMaterials().add("补充能解释金额差异的发票、流水、结算单或对账确认文件");
            gap.getRelatedBusinessIds().add(gap.getClaimBusinessId());
            gap.setRationale("规则发现同一 Claim 关联多个不同 Amount 值");
            return gap;
        });
    }

    private List<CourtSuggestionGap> queryKeyDateMissing(Session session, Map<String, Object> params) {
        return session.run(KEY_DATE_MISSING_QUERY, params).list(record -> {
            CourtSuggestionGap gap = baseGap(CourtSuggestionType.KEY_DATE_MISSING, CourtSuggestionSeverity.MEDIUM, record.asMap());
            String obligationBusinessId = record.get("obligationBusinessId").asString(null);
            gap.setGapOrRiskBusinessId(obligationBusinessId);
            gap.setMissingDescription("合同义务缺少履约、验收、通知或解除等关键日期节点");
            gap.getRecommendedMaterials().add("补充载明关键日期的合同附件、验收单、通知送达凭证或聊天记录");
            gap.getRelatedBusinessIds().add(obligationBusinessId);
            gap.setRationale("存在 Obligation 但未发现关联 Date 节点");
            return gap;
        });
    }

    private List<CourtSuggestionGap> queryDefenseNotRebutted(Session session, Map<String, Object> params) {
        return session.run(DEFENSE_NOT_REBUTTED_QUERY, params).list(record -> {
            CourtSuggestionGap gap = baseGap(CourtSuggestionType.DEFENSE_NOT_REBUTTED, CourtSuggestionSeverity.HIGH, record.asMap());
            gap.setDefenseBusinessId(record.get("defenseBusinessId").asString(null));
            gap.setClaimBusinessId(record.get("claimBusinessId").asString(null));
            gap.setMissingDescription("对方抗辩尚未被有效观点或证据反驳");
            gap.getRecommendedMaterials().add("补充能够反驳该抗辩的履约证据、沟通记录、对账凭证或反证说明");
            gap.getRelatedBusinessIds().add(gap.getDefenseBusinessId());
            gap.setRationale("未发现指向 Defense 的 CHALLENGES_ARGUMENT 关系");
            return gap;
        });
    }

    private CourtSuggestionGap baseGap(CourtSuggestionType type, CourtSuggestionSeverity severity, Map<String, Object> rawProperties) {
        CourtSuggestionGap gap = new CourtSuggestionGap();
        gap.setType(type);
        gap.setSeverity(severity);
        gap.setRawProperties(rawProperties);
        return gap;
    }
}
