package com.oai.callqc.vo;



/**
 * 源码中文讲解：质检列表行 VO
 *
 * - 用于质检列表页每一行的展示数据。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QcListItemVO {
    private String callId;
    private String agentId;
    private String agentName;
    private String businessLine;
    private String skillGroup;
    private Integer durationSeconds;
    private String processStatus;
    private Integer totalScore;
    private String riskLevel;
    private Boolean needManualReview;
    private Integer hitCount;
    private Integer reviewCount;
    private LocalDateTime startTime;
}
