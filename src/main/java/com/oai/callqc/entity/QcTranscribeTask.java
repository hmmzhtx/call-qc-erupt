package com.oai.callqc.entity;

import com.oai.callqc.enums.TranscribeTaskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import xyz.erupt.jpa.model.BaseModel;

import java.time.LocalDateTime;

/**
 * 转写任务队列表。
 *
 * 用于承接“批量异步转写”的入队、消费、重试与失败跟踪，避免一次性并发把本地 ASR 打满。
 */
@Getter
@Setter
@Entity
@Table(name = "qc_transcribe_task",
        uniqueConstraints = @UniqueConstraint(name = "uk_qc_transcribe_task_call_id", columnNames = "call_id"))
public class QcTranscribeTask extends BaseModel {

    @Column(name = "call_id", nullable = false, length = 128)
    private String callId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_status", nullable = false, length = 32)
    private TranscribeTaskStatus taskStatus = TranscribeTaskStatus.WAITING;

    @Column(name = "priority_num", nullable = false)
    private Integer priorityNum = 0;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "max_retry_count", nullable = false)
    private Integer maxRetryCount = 3;

    @Column(name = "locked_by", length = 64)
    private String lockedBy;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "next_run_time")
    private LocalDateTime nextRunTime;

    @Column(name = "last_error", columnDefinition = "LONGTEXT")
    private String lastError;
}
