package com.oai.callqc.service;



/**
 * 源码中文讲解：通话导入服务接口
 *
 * - 定义导入通话主数据的能力。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import com.oai.callqc.dto.CallImportRequest;
import com.oai.callqc.entity.CallRecord;

public interface CallImportService {
    /**
     * 功能说明：导入并保存业务数据，为后续流程提供基础记录。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param request 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    CallRecord importCall(CallImportRequest request);
}
