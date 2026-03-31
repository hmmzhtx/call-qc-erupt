package com.oai.callqc.entity;



/**
 * 源码中文讲解：质检结果实体
 *
 * - 对应 qc_result 表，保存总分、各维度分数、风险等级、摘要和状态。
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

@Getter
@Setter
@Entity
@Table(name = "qc_result")
@Erupt(name = "质检结果")
public class QcResult extends BaseModel {

    @EruptField(views = @View(title = "通话ID"), edit = @Edit(title = "通话ID", notNull = true, search = @Search))
    @Column(name = "call_id", nullable = false, unique = true, length = 64)
    private String callId;

    @EruptField(views = @View(title = "总分", sortable = true), edit = @Edit(title = "总分"))
    @Column(name = "total_score")
    private Integer totalScore;

    @EruptField(views = @View(title = "合规分"), edit = @Edit(title = "合规分"))
    @Column(name = "compliance_score")
    private Integer complianceScore;

    @EruptField(views = @View(title = "服务分"), edit = @Edit(title = "服务分"))
    @Column(name = "service_score")
    private Integer serviceScore;

    @EruptField(views = @View(title = "流程分"), edit = @Edit(title = "流程分"))
    @Column(name = "process_score")
    private Integer processScore;

    @EruptField(views = @View(title = "业务分"), edit = @Edit(title = "业务分"))
    @Column(name = "business_score")
    private Integer businessScore;

    @EruptField(views = @View(title = "风险等级"), edit = @Edit(title = "风险等级", search = @Search))
    @Column(name = "risk_level", length = 16)
    private String riskLevel;

    @EruptField(views = @View(title = "是否需人工复核"), edit = @Edit(title = "是否需人工复核"))
    @Column(name = "need_manual_review")
    private Boolean needManualReview;

    @Lob
    @EruptField(views = @View(title = "AI/规则摘要"), edit = @Edit(title = "AI/规则摘要"))
    @Column(name = "summary_text")
    private String summaryText;

    @EruptField(views = @View(title = "状态"), edit = @Edit(title = "状态", search = @Search))
    @Column(name = "qc_status", length = 32)
    private String qcStatus;
}
