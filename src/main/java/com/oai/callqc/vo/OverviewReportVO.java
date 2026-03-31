package com.oai.callqc.vo;



/**
 * 源码中文讲解：概览报表 VO
 *
 * - 用于返回总通话数、平均分、高风险数等概览指标。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OverviewReportVO {
    private long totalCalls;
    private long qcFinishedCalls;
    private long highRiskCalls;
    private double averageScore;
    private long needManualReviewCalls;
}
