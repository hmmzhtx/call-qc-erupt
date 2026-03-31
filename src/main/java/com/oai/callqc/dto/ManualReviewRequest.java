package com.oai.callqc.dto;



/**
 * 源码中文讲解：人工复核请求 DTO
 *
 * - 用于接收质检员复核时提交的复核人、复核结果、调整分数和备注。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ManualReviewRequest {

    @NotBlank(message = "reviewerId不能为空")
    private String reviewerId;
    @NotBlank(message = "reviewerName不能为空")
    private String reviewerName;
    @NotBlank(message = "reviewResult不能为空")
    private String reviewResult;
    private Integer adjustedScore;
    private String reviewComment;
}
