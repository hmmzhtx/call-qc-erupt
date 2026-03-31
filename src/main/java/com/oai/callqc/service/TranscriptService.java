package com.oai.callqc.service;



/**
 * 源码中文讲解：转写服务接口
 *
 * - 定义保存转写结果、同步转写和上传录音转写能力。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import com.oai.callqc.dto.TranscriptUploadRequest;
import com.oai.callqc.vo.TranscriptionResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface TranscriptService {
    /**
     * 功能说明：保存当前步骤的处理结果，确保后续流程可以继续使用。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param callId 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param request 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    void saveTranscript(String callId, TranscriptUploadRequest request);

    /**
     * 功能说明：发起录音转写处理，把音频内容转换成可用的文本数据。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param callId 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    TranscriptionResultVO transcribeByCallId(String callId);

    /**
     * 功能说明：接收外部上传的数据或文件，并完成基础校验与后续处理。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param callId 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param file 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    TranscriptionResultVO transcribeByUpload(String callId, MultipartFile file);
}
