package com.oai.callqc.enums;



/**
 * 源码中文讲解：风险等级枚举
 *
 * - 用于统一表示 LOW/MEDIUM/HIGH 等质检风险级别。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}
