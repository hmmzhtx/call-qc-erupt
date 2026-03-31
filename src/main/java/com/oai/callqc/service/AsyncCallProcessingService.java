package com.oai.callqc.service;



/**
 * 源码中文讲解：异步处理服务接口
 *
 * - 定义“提交异步转写任务”的服务边界。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import com.oai.callqc.vo.AsyncTaskSubmitVO;
import com.oai.callqc.vo.BatchAsyncTaskSubmitVO;
import java.util.List;

public interface AsyncCallProcessingService {

    /**
     * 功能说明：封装当前类中的一个业务步骤，供上层流程按顺序调用。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param callId 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param submitMessage 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    AsyncTaskSubmitVO submitExistingRecording(String callId, String submitMessage);

    /**
     * 批量提交已存在录音的异步转写任务。
     */
    BatchAsyncTaskSubmitVO submitBatchExistingRecordings(List<String> callIds, String submitMessage);
}
