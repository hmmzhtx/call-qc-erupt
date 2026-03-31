package com.oai.callqc.service.impl;



/**
 * 源码中文讲解：基础质检服务实现
 *
 * - 这是 1.0 版本的核心业务类，负责执行基础规则质检。
 * - 它会读取转写文本、遍历规则、生成命中明细、计算分数、生成摘要，并支持详情查询和人工复核。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import com.oai.callqc.common.BusinessException;
import com.oai.callqc.common.PageResponse;
import com.oai.callqc.dto.ManualReviewRequest;
import com.oai.callqc.entity.CallRecord;
import com.oai.callqc.entity.CallTranscript;
import com.oai.callqc.entity.QcHitDetail;
import com.oai.callqc.entity.QcResult;
import com.oai.callqc.entity.QcReview;
import com.oai.callqc.entity.QcRule;
import com.oai.callqc.enums.CallProcessStatus;
import com.oai.callqc.enums.RiskLevel;
import com.oai.callqc.repository.CallRecordRepository;
import com.oai.callqc.repository.CallTranscriptRepository;
import com.oai.callqc.repository.QcHitDetailRepository;
import com.oai.callqc.repository.QcResultRepository;
import com.oai.callqc.repository.QcReviewRepository;
import com.oai.callqc.repository.QcRuleRepository;
import com.oai.callqc.service.BasicQcService;
import com.oai.callqc.vo.QcDetailVO;
import com.oai.callqc.vo.QcListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BasicQcServiceImpl implements BasicQcService {

    private static final int TOTAL_SCORE = 100;
    private static final int COMPLIANCE_SCORE = 40;
    private static final int SERVICE_SCORE = 25;
    private static final int PROCESS_SCORE = 20;
    private static final int BUSINESS_SCORE = 15;

    private final CallRecordRepository callRecordRepository;
    private final CallTranscriptRepository callTranscriptRepository;
    private final QcRuleRepository qcRuleRepository;
    private final QcResultRepository qcResultRepository;
    private final QcHitDetailRepository qcHitDetailRepository;
    private final QcReviewRepository qcReviewRepository;
    /**
     * 执行一通电话的基础规则质检。
     *      * 核心流程：读取转写 → 读取规则 → 规则判定 → 保存命中明细 → 计算总分与风险等级 → 更新主表状态。
     */
    @Override
    public void execute(String callId) {
        CallRecord callRecord = callRecordRepository.findByCallId(callId)
                .orElseThrow(() -> new BusinessException("通话记录不存在: " + callId));
        List<CallTranscript> transcripts = callTranscriptRepository.findByCallIdOrderBySegmentIndexAsc(callId);
        if (transcripts.isEmpty()) {
            /**
             * 功能说明：封装当前类中的一个业务步骤，供上层流程按顺序调用。
             * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
             * @param "转写结果为空，无法执行质检" 方法输入参数，具体含义请结合调用场景和字段命名理解。
             */
            throw new BusinessException("转写结果为空，无法执行质检");
        }

        int totalScore = TOTAL_SCORE;
        int complianceScore = COMPLIANCE_SCORE;
        int serviceScore = SERVICE_SCORE;
        int processScore = PROCESS_SCORE;
        int businessScore = BUSINESS_SCORE;

        List<QcRule> rules = qcRuleRepository.findByEnabledTrue();
        qcHitDetailRepository.deleteByCallId(callId);
        List<QcHitDetail> hitDetails = new ArrayList<>();

        for (QcRule rule : rules) {
            RuleEvaluation hit = evaluateRule(rule, transcripts);
            if (hit == null || !hit.hit()) {
                continue;
            }

            QcHitDetail detail = new QcHitDetail();
            detail.setCallId(callId);
            detail.setRuleCode(rule.getRuleCode());
            detail.setRuleName(rule.getRuleName());
            detail.setHitFlag(true);
            detail.setSeverity(defaultString(rule.getSeverity(), "LOW"));
            detail.setDeductScore(defaultInt(rule.getDeductScore(), 0));
            detail.setEvidenceText(hit.evidenceText());
            detail.setStartMs(hit.startMs());
            detail.setEndMs(hit.endMs());
            detail.setJudgeSource("RULE");
            hitDetails.add(detail);

            int deduct = defaultInt(rule.getDeductScore(), 0);
            totalScore = Math.max(0, totalScore - deduct);
            if (rule.getRuleCode().startsWith("COMP")) {
                complianceScore = Math.max(0, complianceScore - deduct);
            } else if (rule.getRuleCode().startsWith("SERV")) {
                serviceScore = Math.max(0, serviceScore - deduct);
            } else if (rule.getRuleCode().startsWith("PROC")) {
                processScore = Math.max(0, processScore - deduct);
            } else if (rule.getRuleCode().startsWith("BIZ")) {
                businessScore = Math.max(0, businessScore - deduct);
            }
        }

        qcHitDetailRepository.saveAll(hitDetails);

        QcResult result = qcResultRepository.findByCallId(callId).orElseGet(QcResult::new);
        result.setCallId(callId);
        result.setTotalScore(totalScore);
        result.setComplianceScore(complianceScore);
        result.setServiceScore(serviceScore);
        result.setProcessScore(processScore);
        result.setBusinessScore(businessScore);
        result.setRiskLevel(resolveRiskLevel(totalScore).name());
        result.setNeedManualReview(totalScore < 80 || hitDetails.stream().anyMatch(it -> "HIGH".equalsIgnoreCase(it.getSeverity())));
        result.setSummaryText(buildSummary(callRecord, hitDetails, totalScore));
        result.setQcStatus("QC_DONE");
        qcResultRepository.save(result);

        callRecord.setProcessStatus(CallProcessStatus.QC_DONE.name());
        callRecord.setProcessMessage(result.getNeedManualReview() ? "基础质检完成，建议人工复核" : "基础质检完成");
        callRecord.setLastProcessTime(LocalDateTime.now());
        callRecordRepository.save(callRecord);
    }
    /**
     * 查询质检详情页需要的全部聚合数据。
     *      * 这里会一次性把主表、转写、命中明细、复核记录等都查出来。
     */
    @Override
    public QcDetailVO detail(String callId) {
        CallRecord callRecord = callRecordRepository.findByCallId(callId)
                .orElseThrow(() -> new BusinessException("通话记录不存在: " + callId));
        List<CallTranscript> transcripts = callTranscriptRepository.findByCallIdOrderBySegmentIndexAsc(callId);
        List<QcHitDetail> hitDetails = qcHitDetailRepository.findByCallIdOrderByIdAsc(callId);
        List<QcReview> reviews = qcReviewRepository.findByCallIdOrderByReviewedAtDesc(callId);
        long transcriptDurationMs = transcripts.stream()
                .map(CallTranscript::getEndMs)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L);
        String fullTranscript = transcripts.stream()
                .map(CallTranscript::getTranscriptText)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
        return QcDetailVO.builder()
                .callRecord(callRecord)
                .qcResult(qcResultRepository.findByCallId(callId).orElse(null))
                .transcripts(transcripts)
                .hitDetails(hitDetails)
                .reviews(reviews)
                .transcriptSegmentCount(transcripts.size())
                .hitCount(hitDetails.size())
                .highSeverityHitCount((int) hitDetails.stream().filter(item -> "HIGH".equalsIgnoreCase(item.getSeverity())).count())
                .reviewCount(reviews.size())
                .transcriptDurationMs(transcriptDurationMs)
                .fullTranscriptText(fullTranscript)
                .build();
    }
    /**
     * 提交人工复核结果。
     *      * 如果复核人员修改了分数，这里会同步更新风险等级和质检状态。
     */
    @Override
    public void review(String callId, ManualReviewRequest request) {
        QcResult result = qcResultRepository.findByCallId(callId)
                .orElseThrow(() -> new BusinessException("质检结果不存在: " + callId));
        QcReview review = new QcReview();
        review.setCallId(callId);
        review.setReviewerId(request.getReviewerId());
        review.setReviewerName(request.getReviewerName());
        review.setReviewResult(request.getReviewResult());
        review.setAdjustedScore(request.getAdjustedScore());
        review.setReviewComment(request.getReviewComment());
        review.setReviewedAt(LocalDateTime.now());
        qcReviewRepository.save(review);

        if (request.getAdjustedScore() != null) {
            result.setTotalScore(request.getAdjustedScore());
            result.setRiskLevel(resolveRiskLevel(request.getAdjustedScore()).name());
        }
        result.setQcStatus("REVIEWED");
        result.setNeedManualReview(false);
        result.setSummaryText(buildReviewedSummary(result.getSummaryText(), request));
        qcResultRepository.save(result);

        callRecordRepository.findByCallId(callId).ifPresent(record -> {
            record.setProcessStatus(CallProcessStatus.REVIEWED.name());
            record.setProcessMessage("人工复核已完成");
            record.setLastProcessTime(LocalDateTime.now());
            callRecordRepository.save(record);
        });
    }
    /**
     * 分页查询质检列表。
     *      * 当前 1.0 版本采用内存聚合方式，适合演示或小规模数据。后续可改为数据库分页 SQL。
     */
    @Override
    public PageResponse<QcListItemVO> page(int pageNo,
                                           int pageSize,
                                           String callId,
                                           String agentName,
                                           String businessLine,
                                           String processStatus,
                                           String riskLevel,
                                           Boolean needManualReview) {
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = Math.max(pageSize, 1);

        Map<String, QcResult> resultMap = qcResultRepository.findAll().stream()
                .collect(Collectors.toMap(QcResult::getCallId, item -> item, (a, b) -> b));
        Map<String, Integer> hitCountMap = new HashMap<>();
        for (QcHitDetail hit : qcHitDetailRepository.findAll()) {
            hitCountMap.merge(hit.getCallId(), 1, Integer::sum);
        }
        Map<String, Integer> reviewCountMap = new HashMap<>();
        for (QcReview review : qcReviewRepository.findAll()) {
            reviewCountMap.merge(review.getCallId(), 1, Integer::sum);
        }

        List<QcListItemVO> rows = callRecordRepository.findAll().stream()
                .sorted(Comparator.comparing(CallRecord::getStartTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(call -> {
                    QcResult result = resultMap.get(call.getCallId());
                    return QcListItemVO.builder()
                            .callId(call.getCallId())
                            .agentId(call.getAgentId())
                            .agentName(call.getAgentName())
                            .businessLine(call.getBusinessLine())
                            .skillGroup(call.getSkillGroup())
                            .durationSeconds(call.getDurationSeconds())
                            .processStatus(call.getProcessStatus())
                            .totalScore(result == null ? null : result.getTotalScore())
                            .riskLevel(result == null ? null : result.getRiskLevel())
                            .needManualReview(result == null ? null : result.getNeedManualReview())
                            .hitCount(hitCountMap.getOrDefault(call.getCallId(), 0))
                            .reviewCount(reviewCountMap.getOrDefault(call.getCallId(), 0))
                            .startTime(call.getStartTime())
                            .build();
                })
                .filter(item -> matchLike(item.getCallId(), callId))
                .filter(item -> matchLike(item.getAgentName(), agentName))
                .filter(item -> matchLike(item.getBusinessLine(), businessLine))
                .filter(item -> matchEquals(item.getProcessStatus(), processStatus))
                .filter(item -> matchEquals(item.getRiskLevel(), riskLevel))
                .filter(item -> needManualReview == null || Objects.equals(item.getNeedManualReview(), needManualReview))
                .toList();

        int fromIndex = Math.min((normalizedPageNo - 1) * normalizedPageSize, rows.size());
        int toIndex = Math.min(fromIndex + normalizedPageSize, rows.size());
        List<QcListItemVO> pageRecords = rows.subList(fromIndex, toIndex);

        return PageResponse.<QcListItemVO>builder()
                .total(rows.size())
                .pageNo(normalizedPageNo)
                .pageSize(normalizedPageSize)
                .records(pageRecords)
                .build();
    }

    /**
     * 功能说明：处理规则相关的构建、匹配或统计逻辑。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param rule 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param transcripts 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private RuleEvaluation evaluateRule(QcRule rule, List<CallTranscript> transcripts) {
        String type = defaultString(rule.getRuleType(), "KEYWORD_PRESENT").toUpperCase(Locale.ROOT);
        /**
         * 功能说明：封装当前类中的一个业务步骤，供上层流程按顺序调用。
         * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
         * @param type 方法输入参数，具体含义请结合调用场景和字段命名理解。
         */
        return switch (type) {
            case "KEYWORD", "KEYWORD_PRESENT" -> keywordPresent(rule.getExpressionText(), transcripts);
            case "KEYWORD_MISSING" -> keywordMissing(rule.getExpressionText(), transcripts);
            case "REGEX_PRESENT" -> regexPresent(rule.getExpressionText(), transcripts);
            case "CLOSING_MISSING" -> closingMissing(rule.getExpressionText(), transcripts);
            default -> keywordPresent(rule.getExpressionText(), transcripts);
        };
    }

    /**
     * 功能说明：基于关键字规则执行匹配判断。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param expressionText 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param transcripts 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private RuleEvaluation keywordPresent(String expressionText, List<CallTranscript> transcripts) {
        if (!StringUtils.hasText(expressionText)) {
            return null;
        }
        String[] words = expressionText.toLowerCase(Locale.ROOT).split("\\|");
        for (CallTranscript transcript : transcripts) {
            String text = lower(transcript.getTranscriptText());
            for (String word : words) {
                String keyword = word.trim();
                if (!keyword.isEmpty() && text.contains(keyword)) {
                    return RuleEvaluation.hit(transcript.getTranscriptText(), transcript.getStartMs(), transcript.getEndMs());
                }
            }
        }
        return null;
    }

    /**
     * 功能说明：基于关键字规则执行匹配判断。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param expressionText 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param transcripts 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private RuleEvaluation keywordMissing(String expressionText, List<CallTranscript> transcripts) {
        RuleEvaluation found = keywordPresent(expressionText, transcripts);
        if (found != null) {
            return null;
        }
        CallTranscript evidence = transcripts.isEmpty() ? null : transcripts.get(0);
        return RuleEvaluation.hit(
                "未检测到要求关键词：" + expressionText,
                evidence == null ? 0L : defaultLong(evidence.getStartMs(), 0L),
                evidence == null ? 0L : defaultLong(evidence.getEndMs(), 0L)
        );
    }

    /**
     * 功能说明：基于正则表达式规则执行匹配判断。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param expressionText 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param transcripts 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private RuleEvaluation regexPresent(String expressionText, List<CallTranscript> transcripts) {
        if (!StringUtils.hasText(expressionText)) {
            return null;
        }
        Pattern pattern = Pattern.compile(expressionText, Pattern.CASE_INSENSITIVE);
        for (CallTranscript transcript : transcripts) {
            String text = defaultString(transcript.getTranscriptText(), "");
            if (pattern.matcher(text).find()) {
                return RuleEvaluation.hit(text, transcript.getStartMs(), transcript.getEndMs());
            }
        }
        return null;
    }

    /**
     * 功能说明：检查结束语或尾部话术是否满足规则要求。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param expressionText 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param transcripts 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private RuleEvaluation closingMissing(String expressionText, List<CallTranscript> transcripts) {
        List<CallTranscript> agentSegments = transcripts.stream()
                .filter(item -> "AGENT".equalsIgnoreCase(item.getSpeakerRole()) || !StringUtils.hasText(item.getSpeakerRole()))
                .toList();
        if (agentSegments.isEmpty()) {
            return RuleEvaluation.hit("未识别到坐席结束语", 0L, 0L);
        }
        CallTranscript lastAgent = agentSegments.get(agentSegments.size() - 1);
        RuleEvaluation present = keywordPresent(expressionText, List.of(lastAgent));
        if (present != null) {
            return null;
        }
        return RuleEvaluation.hit(defaultString(lastAgent.getTranscriptText(), "") + "（末尾未检测到结束语）",
                defaultLong(lastAgent.getStartMs(), 0L),
                defaultLong(lastAgent.getEndMs(), 0L));
    }

    /**
     * 功能说明：构建当前场景需要的对象、摘要或返回结果。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param callRecord 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param hitDetails 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param totalScore 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private String buildSummary(CallRecord callRecord, List<QcHitDetail> hitDetails, int totalScore) {
        if (hitDetails.isEmpty()) {
            return String.format("通话【%s】未命中基础规则，当前总分=%d", callRecord.getCallId(), totalScore);
        }
        String issues = hitDetails.stream().map(QcHitDetail::getRuleName).collect(Collectors.joining("、"));
        return String.format("通话【%s】命中 %d 项规则：%s；当前总分=%d；%s",
                callRecord.getCallId(),
                hitDetails.size(),
                issues,
                totalScore,
                totalScore < 80 ? "建议人工复核" : "可直接进入后续处理");
    }

    /**
     * 功能说明：处理人工复核结果，并回写最终质检状态。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param originalSummary 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param request 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private String buildReviewedSummary(String originalSummary, ManualReviewRequest request) {
        String reviewText = String.format("人工复核结果=%s，复核人=%s", request.getReviewResult(), request.getReviewerName());
        if (request.getAdjustedScore() != null) {
            reviewText += "，调整后分数=" + request.getAdjustedScore();
        }
        if (StringUtils.hasText(request.getReviewComment())) {
            reviewText += "，备注=" + request.getReviewComment();
        }
        return (StringUtils.hasText(originalSummary) ? originalSummary + "；" : "") + reviewText;
    }

    /**
     * 功能说明：执行字符串匹配判断，返回是否命中。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param source 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param target 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private boolean matchLike(String source, String target) {
        if (!StringUtils.hasText(target)) {
            return true;
        }
        /**
         * 功能说明：统一处理字符串格式，降低比较时的差异。
         * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
         * @param source).contains(target.trim().toLowerCase(Locale.ROOT) 方法输入参数，具体含义请结合调用场景和字段命名理解。
         */
        return lower(source).contains(target.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * 功能说明：执行字符串匹配判断，返回是否命中。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param source 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param target 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private boolean matchEquals(String source, String target) {
        if (!StringUtils.hasText(target)) {
            return true;
        }
        /**
         * 功能说明：在输入为空时返回默认值，减少重复判空逻辑。
         * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
         * @param source 方法输入参数，具体含义请结合调用场景和字段命名理解。
         * @param "").equalsIgnoreCase(target.trim() 方法输入参数，具体含义请结合调用场景和字段命名理解。
         */
        return defaultString(source, "").equalsIgnoreCase(target.trim());
    }

    /**
     * 功能说明：统一处理字符串格式，降低比较时的差异。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param value 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private String lower(String value) {
        /**
         * 功能说明：在输入为空时返回默认值，减少重复判空逻辑。
         * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
         * @param value 方法输入参数，具体含义请结合调用场景和字段命名理解。
         * @param "").toLowerCase(Locale.ROOT 方法输入参数，具体含义请结合调用场景和字段命名理解。
         */
        return defaultString(value, "").toLowerCase(Locale.ROOT);
    }

    /**
     * 功能说明：在输入为空时返回默认值，减少重复判空逻辑。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param value 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param defaultValue 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private String defaultString(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * 功能说明：在输入为空时返回默认值，减少重复判空逻辑。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param value 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param defaultValue 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * 功能说明：在输入为空时返回默认值，减少重复判空逻辑。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param value 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param defaultValue 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private Long defaultLong(Long value, Long defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * 功能说明：解析并确定最终要使用的配置、路径、角色或业务值。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param score 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private RiskLevel resolveRiskLevel(int score) {
        if (score < 60) {
            return RiskLevel.HIGH;
        }
        if (score < 80) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private record RuleEvaluation(boolean hit, String evidenceText, Long startMs, Long endMs) {
        /**
         * 功能说明：封装当前类中的一个业务步骤，供上层流程按顺序调用。
         * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
         * @param evidenceText 方法输入参数，具体含义请结合调用场景和字段命名理解。
         * @param startMs 方法输入参数，具体含义请结合调用场景和字段命名理解。
         * @param endMs 方法输入参数，具体含义请结合调用场景和字段命名理解。
         */
        private static RuleEvaluation hit(String evidenceText, Long startMs, Long endMs) {
            /**
             * 功能说明：处理规则相关的构建、匹配或统计逻辑。
             * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
             * @param true 方法输入参数，具体含义请结合调用场景和字段命名理解。
             * @param evidenceText 方法输入参数，具体含义请结合调用场景和字段命名理解。
             * @param startMs 方法输入参数，具体含义请结合调用场景和字段命名理解。
             * @param endMs 方法输入参数，具体含义请结合调用场景和字段命名理解。
             */
            return new RuleEvaluation(true, evidenceText, startMs, endMs);
        }
    }
}
