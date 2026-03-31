package com.oai.callqc.entity;



/**
 * 源码中文讲解：规则命中明细实体
 *
 * - 对应 qc_hit_detail 表，保存每一条规则命中的证据和扣分。
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
@Table(name = "qc_hit_detail")
@Erupt(name = "命中明细")
public class QcHitDetail extends BaseModel {

    @EruptField(views = @View(title = "通话ID"), edit = @Edit(title = "通话ID", notNull = true, search = @Search))
    @Column(name = "call_id", nullable = false, length = 64)
    private String callId;

    @EruptField(views = @View(title = "规则编码"), edit = @Edit(title = "规则编码", search = @Search))
    @Column(name = "rule_code", length = 64)
    private String ruleCode;

    @EruptField(views = @View(title = "规则名称"), edit = @Edit(title = "规则名称"))
    @Column(name = "rule_name", length = 128)
    private String ruleName;

    @EruptField(views = @View(title = "是否命中"), edit = @Edit(title = "是否命中"))
    @Column(name = "hit_flag")
    private Boolean hitFlag;

    @EruptField(views = @View(title = "严重等级"), edit = @Edit(title = "严重等级", search = @Search))
    @Column(name = "severity", length = 16)
    private String severity;

    @EruptField(views = @View(title = "扣分值"), edit = @Edit(title = "扣分值"))
    @Column(name = "deduct_score")
    private Integer deductScore;

    @Lob
    @EruptField(views = @View(title = "证据文本"), edit = @Edit(title = "证据文本"))
    @Column(name = "evidence_text")
    private String evidenceText;

    @EruptField(views = @View(title = "开始毫秒"), edit = @Edit(title = "开始毫秒"))
    @Column(name = "start_ms")
    private Long startMs;

    @EruptField(views = @View(title = "结束毫秒"), edit = @Edit(title = "结束毫秒"))
    @Column(name = "end_ms")
    private Long endMs;

    @EruptField(views = @View(title = "判定来源"), edit = @Edit(title = "判定来源"))
    @Column(name = "judge_source", length = 16)
    private String judgeSource;
}
