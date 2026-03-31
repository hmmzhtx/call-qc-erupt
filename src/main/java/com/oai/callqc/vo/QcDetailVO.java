package com.oai.callqc.vo;



/**
 * 源码中文讲解：质检详情 VO
 *
 * - 聚合主表、转写、命中明细、复核记录和统计指标，服务于详情页工作台。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import com.oai.callqc.entity.CallRecord;
import com.oai.callqc.entity.CallTranscript;
import com.oai.callqc.entity.QcHitDetail;
import com.oai.callqc.entity.QcResult;
import com.oai.callqc.entity.QcReview;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QcDetailVO {
    private CallRecord callRecord;
    private QcResult qcResult;
    private List<CallTranscript> transcripts;
    private List<QcHitDetail> hitDetails;
    private List<QcReview> reviews;
    private Integer transcriptSegmentCount;
    private Integer hitCount;
    private Integer highSeverityHitCount;
    private Integer reviewCount;
    private Long transcriptDurationMs;
    private String fullTranscriptText;
}
