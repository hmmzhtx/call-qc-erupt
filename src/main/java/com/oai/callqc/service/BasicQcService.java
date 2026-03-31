package com.oai.callqc.service;



/**
 * 源码中文讲解：基础质检服务接口
 *
 * - 定义执行质检、查询详情、分页查询和人工复核等能力。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import com.oai.callqc.common.PageResponse;
import com.oai.callqc.dto.ManualReviewRequest;
import com.oai.callqc.vo.QcDetailVO;
import com.oai.callqc.vo.QcListItemVO;

public interface BasicQcService {
    /**
     * 功能说明：执行当前业务场景的核心处理逻辑。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param callId 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    void execute(String callId);

    /**
     * 功能说明：查询并组装详情数据，供页面展示或接口返回。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param callId 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    QcDetailVO detail(String callId);

    /**
     * 功能说明：处理人工复核结果，并回写最终质检状态。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param callId 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param request 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    void review(String callId, ManualReviewRequest request);

    PageResponse<QcListItemVO> page(int pageNo,
                                    int pageSize,
                                    String callId,
                                    String agentName,
                                    String businessLine,
                                    String processStatus,
                                    String riskLevel,
                                    Boolean needManualReview);
}
