package com.oai.callqc.vo;



/**
 * 源码中文讲解：规则命中统计 VO
 *
 * - 用于规则报表中展示每条规则的命中次数和趋势。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RuleHitStatVO {
    private String ruleCode;
    private String ruleName;
    private String severity;
    private long hitCount;
    private long callCount;
}
