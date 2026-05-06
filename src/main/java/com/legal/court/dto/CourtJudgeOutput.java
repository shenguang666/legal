package com.legal.court.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能小法庭 AI 法官结构化输出。
 */
@Data
public class CourtJudgeOutput {

    /** 争议焦点列表。 */
    private List<String> focusIssues = new ArrayList<>();
    /** 已采信事实列表。 */
    private List<String> acceptedFacts = new ArrayList<>();
    /** 未采信事实列表。 */
    private List<String> rejectedFacts = new ArrayList<>();
    /** 对 PartyA 不利的要点列表。 */
    private List<String> unfavorableToPartyA = new ArrayList<>();
    /** 对 PartyB 不利的要点列表。 */
    private List<String> unfavorableToPartyB = new ArrayList<>();
    /** 尚待查明的问题列表。 */
    private List<String> openQuestions = new ArrayList<>();
    /** 是否为重试失败后的回退输出。 */
    private boolean fallback;
    /** 回退或校验失败原因。 */
    private String failureReason;
}
