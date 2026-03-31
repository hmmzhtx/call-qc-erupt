package com.oai.callqc.enums;

/**
 * 转写任务队列状态。
 */
public enum TranscribeTaskStatus {
    WAITING,
    RUNNING,
    SUCCESS,
    FAILED,
    RETRY_WAIT
}
