package com.oai.callqc.vo;



/**
 * 源码中文讲解：异步任务提交结果 VO
 *
 * - 用于返回异步任务已入队时的状态信息。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AsyncTaskSubmitVO {
    private String callId;
    private String processStatus;
    private String processMessage;
    private String recordingUrl;
    private LocalDateTime queuedAt;
}
