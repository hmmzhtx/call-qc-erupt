package com.oai.callqc.dto;



/**
 * 源码中文讲解：转写分段 DTO
 *
 * - 表示单个分段的时间范围、说话人、文本和置信度。
 * - 既可用于外部系统回传转写结果，也可由本地 ASR 调用后在内部组装。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TranscriptSegmentRequest {

    @NotNull(message = "segmentIndex不能为空")
    private Integer segmentIndex;
    @NotBlank(message = "speakerRole不能为空")
    private String speakerRole;
    @NotNull(message = "startMs不能为空")
    private Long startMs;
    @NotNull(message = "endMs不能为空")
    private Long endMs;
    @NotBlank(message = "transcriptText不能为空")
    private String transcriptText;
    private Double confidence;
}
