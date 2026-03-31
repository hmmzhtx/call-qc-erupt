package com.oai.callqc.enums;



/**
 * 源码中文讲解：通话处理状态枚举
 *
 * - 定义录音处理链路中的关键状态，例如已导入、待转写、转写中、已质检、已复核等。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
public enum CallProcessStatus {
    IMPORTED,
    RECORDING_UPLOADED,
    TRANSCRIBE_PENDING,
    TRANSCRIBING,
    TRANSCRIBED,
    QC_DONE,
    REVIEWED,
    TRANSCRIBE_FAILED
}
