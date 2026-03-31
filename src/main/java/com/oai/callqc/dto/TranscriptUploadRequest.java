package com.oai.callqc.dto;



/**
 * 源码中文讲解：转写结果上传 DTO
 *
 * - 用于承载一整通电话的所有分段转写结果。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class TranscriptUploadRequest {

    @Valid
    @NotEmpty(message = "segments不能为空")
    private List<TranscriptSegmentRequest> segments;
}
