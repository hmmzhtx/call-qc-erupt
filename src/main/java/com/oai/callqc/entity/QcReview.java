package com.oai.callqc.entity;



/**
 * 源码中文讲解：人工复核实体
 *
 * - 对应 qc_review 表，记录人工复核历史。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "qc_review")
@Erupt(name = "人工复核")
public class QcReview extends BaseModel {

    @EruptField(views = @View(title = "通话ID"), edit = @Edit(title = "通话ID", notNull = true, search = @Search))
    @Column(name = "call_id", nullable = false, length = 64)
    private String callId;

    @EruptField(views = @View(title = "复核人ID"), edit = @Edit(title = "复核人ID"))
    @Column(name = "reviewer_id", length = 64)
    private String reviewerId;

    @EruptField(views = @View(title = "复核人姓名"), edit = @Edit(title = "复核人姓名", search = @Search))
    @Column(name = "reviewer_name", length = 64)
    private String reviewerName;

    @EruptField(views = @View(title = "复核结果"), edit = @Edit(title = "复核结果", search = @Search))
    @Column(name = "review_result", length = 32)
    private String reviewResult;

    @EruptField(views = @View(title = "调整分数"), edit = @Edit(title = "调整分数"))
    @Column(name = "adjusted_score")
    private Integer adjustedScore;

    @Lob
    @EruptField(views = @View(title = "复核备注"), edit = @Edit(title = "复核备注"))
    @Column(name = "review_comment")
    private String reviewComment;

    @EruptField(views = @View(title = "复核时间"), edit = @Edit(title = "复核时间"))
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
