package com.oai.callqc.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 批量异步转写提交结果。
 */
@Data
@Builder
public class BatchAsyncTaskSubmitVO {
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    /**
     * 已被判定为无需再次转写、在本次批量提交中自动剔除的数量。
     */
    private Integer skippedCount;
    private List<String> successCallIds;
    private List<String> failedMessages;
    /**
     * 被跳过的通话说明，通常用于提示“哪些记录已经转写完成/QC完成，因此未再次入队”。
     */
    private List<String> skippedMessages;
}
