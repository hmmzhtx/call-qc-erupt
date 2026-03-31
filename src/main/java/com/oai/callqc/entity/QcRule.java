package com.oai.callqc.entity;



/**
 * 源码中文讲解：质检规则实体
 *
 * - 对应 qc_rule 表，是基础规则引擎的配置来源。
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
@Table(name = "qc_rule")
@Erupt(name = "质检规则")
public class QcRule extends BaseModel {

    @EruptField(views = @View(title = "规则编码"), edit = @Edit(title = "规则编码", notNull = true, search = @Search))
    @Column(name = "rule_code", nullable = false, unique = true, length = 64)
    private String ruleCode;

    @EruptField(views = @View(title = "规则名称"), edit = @Edit(title = "规则名称", notNull = true, search = @Search))
    @Column(name = "rule_name", nullable = false, length = 128)
    private String ruleName;

    @EruptField(views = @View(title = "规则类型"), edit = @Edit(title = "规则类型", search = @Search))
    @Column(name = "rule_type", length = 32)
    private String ruleType;

    @EruptField(views = @View(title = "业务线"), edit = @Edit(title = "业务线", search = @Search))
    @Column(name = "business_line", length = 64)
    private String businessLine;

    @Lob
    @EruptField(views = @View(title = "表达式"), edit = @Edit(title = "表达式"))
    @Column(name = "expression_text")
    private String expressionText;

    @EruptField(views = @View(title = "扣分值"), edit = @Edit(title = "扣分值"))
    @Column(name = "deduct_score")
    private Integer deductScore;

    @EruptField(views = @View(title = "风险等级"), edit = @Edit(title = "风险等级", search = @Search))
    @Column(name = "severity", length = 16)
    private String severity;

    @EruptField(views = @View(title = "是否启用"), edit = @Edit(title = "是否启用"))
    @Column(name = "enabled")
    private Boolean enabled;
}
