package com.oai.callqc.service.impl;



/**
 * 源码中文讲解：报表服务实现
 *
 * - 负责汇总主表、质检结果、命中明细和复核记录，形成基础运营报表。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import com.oai.callqc.entity.CallRecord;
import com.oai.callqc.entity.QcHitDetail;
import com.oai.callqc.entity.QcResult;
import com.oai.callqc.repository.CallRecordRepository;
import com.oai.callqc.repository.QcHitDetailRepository;
import com.oai.callqc.repository.QcResultRepository;
import com.oai.callqc.vo.AgentReportVO;
import com.oai.callqc.vo.OverviewReportVO;
import com.oai.callqc.vo.RuleHitStatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements com.oai.callqc.service.ReportService {

    private final CallRecordRepository callRecordRepository;
    private final QcResultRepository qcResultRepository;
    private final QcHitDetailRepository qcHitDetailRepository;
    /**
     * 生成首页概览指标，如总通话数、已质检数、平均分、高风险数。
     */
    @Override
    public OverviewReportVO overview() {
        List<CallRecord> calls = callRecordRepository.findAll();
        List<QcResult> results = qcResultRepository.findAll();
        long highRisk = results.stream().filter(it -> "HIGH".equalsIgnoreCase(it.getRiskLevel())).count();
        long needManual = results.stream().filter(it -> Boolean.TRUE.equals(it.getNeedManualReview())).count();
        double avg = results.stream().map(QcResult::getTotalScore).filter(Objects::nonNull).mapToInt(Integer::intValue).average().orElse(0D);
        return OverviewReportVO.builder()
                .totalCalls(calls.size())
                .qcFinishedCalls(results.size())
                .highRiskCalls(highRisk)
                .averageScore(avg)
                .needManualReviewCalls(needManual)
                .build();
    }

    /**
     * 功能说明：统计坐席维度的数据结果，供坐席报表使用。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     */
    @Override
    public List<AgentReportVO> agentReports() {
        List<CallRecord> calls = callRecordRepository.findAll();
        Map<String, QcResult> resultMap = qcResultRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(QcResult::getCallId, item -> item, (a, b) -> b));

        Map<String, AgentAccumulator> map = new HashMap<>();
        for (CallRecord call : calls) {
            String key = call.getAgentId() == null ? "UNKNOWN" : call.getAgentId();
            AgentAccumulator acc = map.computeIfAbsent(key, k -> new AgentAccumulator(call.getAgentId(), call.getAgentName()));
            acc.callCount++;
            QcResult r = resultMap.get(call.getCallId());
            if (r != null) {
                if (r.getTotalScore() != null) {
                    acc.scoreTotal += r.getTotalScore();
                    acc.scoredCount++;
                }
                if ("HIGH".equalsIgnoreCase(r.getRiskLevel())) {
                    acc.highRiskCount++;
                }
            }
        }

        List<AgentReportVO> list = new ArrayList<>();
        for (AgentAccumulator acc : map.values()) {
            list.add(AgentReportVO.builder()
                    .agentId(acc.agentId)
                    .agentName(acc.agentName)
                    .callCount(acc.callCount)
                    .averageScore(acc.scoredCount == 0 ? 0D : (double) acc.scoreTotal / acc.scoredCount)
                    .highRiskCount(acc.highRiskCount)
                    .build());
        }
        list.sort(Comparator.comparing(AgentReportVO::getAverageScore).reversed());
        return list;
    }

    /**
     * 功能说明：处理规则相关的构建、匹配或统计逻辑。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     */
    @Override
    public List<RuleHitStatVO> ruleHitStats() {
        Map<String, RuleAccumulator> map = new LinkedHashMap<>();
        for (QcHitDetail item : qcHitDetailRepository.findAll()) {
            RuleAccumulator acc = map.computeIfAbsent(item.getRuleCode(), code -> new RuleAccumulator(
                    item.getRuleCode(), item.getRuleName(), item.getSeverity()));
            acc.hitCount++;
            acc.callIds.add(item.getCallId());
        }
        return map.values().stream()
                .map(acc -> RuleHitStatVO.builder()
                        .ruleCode(acc.ruleCode)
                        .ruleName(acc.ruleName)
                        .severity(acc.severity)
                        .hitCount(acc.hitCount)
                        .callCount(acc.callIds.size())
                        .build())
                .sorted(Comparator.comparing(RuleHitStatVO::getHitCount).reversed())
                .toList();
    }

    private static class AgentAccumulator {
        private final String agentId;
        private final String agentName;
        private long callCount;
        private long scoredCount;
        private long scoreTotal;
        private long highRiskCount;

        /**
         * 功能说明：统计坐席维度的数据结果，供坐席报表使用。
         * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
         * @param agentId 方法输入参数，具体含义请结合调用场景和字段命名理解。
         * @param agentName 方法输入参数，具体含义请结合调用场景和字段命名理解。
         */
        private AgentAccumulator(String agentId, String agentName) {
            this.agentId = agentId;
            this.agentName = agentName;
        }
    }

    private static class RuleAccumulator {
        private final String ruleCode;
        private final String ruleName;
        private final String severity;
        private long hitCount;
        private final java.util.Set<String> callIds = new java.util.HashSet<>();

        /**
         * 功能说明：处理规则相关的构建、匹配或统计逻辑。
         * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
         * @param ruleCode 方法输入参数，具体含义请结合调用场景和字段命名理解。
         * @param ruleName 方法输入参数，具体含义请结合调用场景和字段命名理解。
         * @param severity 方法输入参数，具体含义请结合调用场景和字段命名理解。
         */
        private RuleAccumulator(String ruleCode, String ruleName, String severity) {
            this.ruleCode = ruleCode;
            this.ruleName = ruleName;
            this.severity = severity;
        }
    }
}
