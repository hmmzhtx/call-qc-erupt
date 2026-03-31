package com.oai.callqc.vo;



/**
 * 源码中文讲解：转写结果 VO
 *
 * - 用于接口返回本次转写的摘要结果。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TranscriptionResultVO {
    private String callId;
    private Integer segmentCount;
    private String processStatus;
    private String transcriptPreview;
}
