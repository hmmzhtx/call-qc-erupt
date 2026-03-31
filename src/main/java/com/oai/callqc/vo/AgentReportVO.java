package com.oai.callqc.vo;



/**
 * 源码中文讲解：坐席报表 VO
 *
 * - 用于返回按坐席聚合后的统计结果。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentReportVO {
    private String agentId;
    private String agentName;
    private long callCount;
    private double averageScore;
    private long highRiskCount;
}
