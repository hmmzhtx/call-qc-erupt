package com.oai.callqc.service.impl;



/**
 * 源码中文讲解：演示数据初始化实现
 *
 * - 用于项目首次启动时生成演示规则和示例通话，方便快速体验系统。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import com.oai.callqc.dto.CallImportRequest;
import com.oai.callqc.dto.TranscriptSegmentRequest;
import com.oai.callqc.dto.TranscriptUploadRequest;
import com.oai.callqc.entity.QcRule;
import com.oai.callqc.repository.QcRuleRepository;
import com.oai.callqc.service.CallImportService;
import com.oai.callqc.service.DataInitializerService;
import com.oai.callqc.service.TranscriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class DataInitializerServiceImpl implements DataInitializerService {

    private final QcRuleRepository qcRuleRepository;
    private final CallImportService callImportService;
    private final TranscriptService transcriptService;

    /**
     * 功能说明：执行初始化逻辑，准备系统运行所需的基础数据或环境。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     */
    @Override
    public void init() {
        initRules();
        initDemoCall();
    }

    /**
     * 功能说明：执行初始化逻辑，准备系统运行所需的基础数据或环境。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     */
    private void initRules() {
        if (qcRuleRepository.count() > 0) {
            return;
        }
        qcRuleRepository.save(rule("COMP_NO_RECORD_NOTICE", "未告知录音", "KEYWORD_MISSING", "客服", "录音|录制|质量监督", 5, "LOW", true));
        qcRuleRepository.save(rule("SERV_RUDE_LANGUAGE", "不文明用语", "KEYWORD_PRESENT", "客服", "你听不懂吗|烦死了|别再说了", 30, "HIGH", true));
        qcRuleRepository.save(rule("PROC_NO_SOLUTION", "未给明确方案", "KEYWORD_PRESENT", "客服", "后续再说|不知道|不清楚", 10, "MEDIUM", true));
        qcRuleRepository.save(rule("COMP_ILLEGAL_PROMISE", "违规承诺", "KEYWORD_PRESENT", "客服", "保证退款|绝对到账", 30, "HIGH", true));
        qcRuleRepository.save(rule("SERV_EMOTION_CONFLICT", "情绪冲突", "KEYWORD_PRESENT", "客服", "投诉你|我要举报|太差了", 15, "MEDIUM", true));
        qcRuleRepository.save(rule("PROC_NO_CLOSING", "缺少结束语", "CLOSING_MISSING", "客服", "感谢您的来电|祝您生活愉快|还有什么可以帮您", 5, "LOW", true));
    }

    /**
     * 功能说明：处理规则相关的构建、匹配或统计逻辑。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param code 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param name 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param type 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param line 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param expr 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param deduct 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param severity 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param enabled 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private QcRule rule(String code, String name, String type, String line, String expr, Integer deduct, String severity, Boolean enabled) {
        QcRule rule = new QcRule();
        rule.setRuleCode(code);
        rule.setRuleName(name);
        rule.setRuleType(type);
        rule.setBusinessLine(line);
        rule.setExpressionText(expr);
        rule.setDeductScore(deduct);
        rule.setSeverity(severity);
        rule.setEnabled(enabled);
        return rule;
    }

    /**
     * 功能说明：执行初始化逻辑，准备系统运行所需的基础数据或环境。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     */
    private void initDemoCall() {
        CallImportRequest request = new CallImportRequest();
        request.setCallId("demo-call-001");
        request.setAgentId("A1001");
        request.setAgentName("张三");
        request.setCustomerId("C9001");
        request.setCustomerPhone("13800000000");
        request.setBusinessLine("客服");
        request.setSkillGroup("售后组");
        request.setCallType("INBOUND");
        request.setStartTime(LocalDateTime.now().minusMinutes(10));
        request.setEndTime(LocalDateTime.now().minusMinutes(2));
        request.setDurationSeconds(480);
        request.setRecordingUrl("");
        callImportService.importCall(request);

        TranscriptSegmentRequest s1 = new TranscriptSegmentRequest();
        s1.setSegmentIndex(1);
        s1.setSpeakerRole("AGENT");
        s1.setStartMs(0L);
        s1.setEndMs(5000L);
        s1.setTranscriptText("您好，这里是客服中心，本次通话可能会录音，请问有什么可以帮您？");
        s1.setConfidence(0.98);

        TranscriptSegmentRequest s2 = new TranscriptSegmentRequest();
        s2.setSegmentIndex(2);
        s2.setSpeakerRole("CUSTOMER");
        s2.setStartMs(5001L);
        s2.setEndMs(12000L);
        s2.setTranscriptText("你们处理太慢了，我要投诉你。");
        s2.setConfidence(0.96);

        TranscriptSegmentRequest s3 = new TranscriptSegmentRequest();
        s3.setSegmentIndex(3);
        s3.setSpeakerRole("AGENT");
        s3.setStartMs(12001L);
        s3.setEndMs(18000L);
        s3.setTranscriptText("我先帮您核实订单，稍后给您明确方案。感谢您的来电。");
        s3.setConfidence(0.97);

        TranscriptUploadRequest uploadRequest = new TranscriptUploadRequest();
        uploadRequest.setSegments(Arrays.asList(s1, s2, s3));
        transcriptService.saveTranscript("demo-call-001", uploadRequest);
    }
}
