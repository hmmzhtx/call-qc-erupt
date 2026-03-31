package com.oai.callqc.service.impl;

import com.oai.callqc.config.AsyncTaskProperties;
import com.oai.callqc.entity.CallRecord;
import com.oai.callqc.entity.QcTranscribeTask;
import com.oai.callqc.enums.CallProcessStatus;
import com.oai.callqc.enums.TranscribeTaskStatus;
import com.oai.callqc.repository.CallRecordRepository;
import com.oai.callqc.repository.QcTranscribeTaskRepository;
import com.oai.callqc.service.TranscriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 异步队列消费执行器。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AsyncCallProcessingWorker {

    private final CallRecordRepository callRecordRepository;
    private final QcTranscribeTaskRepository qcTranscribeTaskRepository;
    private final TranscriptService transcriptService;
    private final AsyncTaskProperties asyncTaskProperties;

    /**
     * 兼容旧入口，统一改为消费队列任务。
     */
    @Async("qcTaskExecutor")
    public void transcribeAndQc(String callId) {
        qcTranscribeTaskRepository.findByCallId(callId)
                .ifPresent(task -> consumeTask(task.getId()));
    }

    /**
     * 真正的队列消费逻辑。
     */
    @Async("qcTaskExecutor")
    public void consumeTask(Long taskId) {
        QcTranscribeTask task = qcTranscribeTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }
        String callId = task.getCallId();
        markCallStatus(callId, CallProcessStatus.TRANSCRIBING, "队列消费中，正在执行异步转写");
        try {
            transcriptService.transcribeByCallId(callId);
            markTaskSuccess(taskId);
            callRecordRepository.findByCallId(callId).ifPresent(record -> {
                if (record.getProcessStatus() == null
                        || CallProcessStatus.TRANSCRIBING.name().equalsIgnoreCase(record.getProcessStatus())
                        || CallProcessStatus.TRANSCRIBE_PENDING.name().equalsIgnoreCase(record.getProcessStatus())) {
                    record.setProcessStatus(CallProcessStatus.TRANSCRIBED.name());
                }
                if (record.getProcessMessage() == null || record.getProcessMessage().isBlank()) {
                    record.setProcessMessage("异步转写完成");
                }
                record.setLastProcessTime(LocalDateTime.now());
                callRecordRepository.save(record);
            });
        } catch (Exception ex) {
            log.error("异步转写失败, taskId={}, callId={}", taskId, callId, ex);
            handleTaskFailure(taskId, callId, ex);
        }
    }

    /**
     * 更新通话表中的状态与说明。
     */
    @Transactional
    protected void markCallStatus(String callId, CallProcessStatus status, String message) {
        callRecordRepository.findByCallId(callId).ifPresent(record -> {
            record.setProcessStatus(status.name());
            record.setProcessMessage(shrink(message));
            record.setLastProcessTime(LocalDateTime.now());
            callRecordRepository.save(record);
        });
    }

    /**
     * 标记任务成功。
     */
    @Transactional
    protected void markTaskSuccess(Long taskId) {
        qcTranscribeTaskRepository.findById(taskId).ifPresent(task -> {
            task.setTaskStatus(TranscribeTaskStatus.SUCCESS);
            task.setLockedAt(null);
            task.setLockedBy(null);
            task.setLastError(null);
            task.setNextRunTime(null);
            qcTranscribeTaskRepository.save(task);
        });
    }

    /**
     * 处理任务失败与重试。
     */
    @Transactional
    protected void handleTaskFailure(Long taskId, String callId, Exception ex) {
        qcTranscribeTaskRepository.findById(taskId).ifPresent(task -> {
            int nextRetry = (task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1;
            task.setRetryCount(nextRetry);
            task.setLockedAt(null);
            task.setLockedBy(null);
            String error = shrink(ex.getMessage());
            task.setLastError(error);
            if (nextRetry < task.getMaxRetryCount()) {
                task.setTaskStatus(TranscribeTaskStatus.RETRY_WAIT);
                task.setNextRunTime(LocalDateTime.now().plusSeconds(asyncTaskProperties.getRetryDelaySeconds()));
                qcTranscribeTaskRepository.save(task);
                markCallStatus(callId, CallProcessStatus.TRANSCRIBE_PENDING,
                        "转写失败，已加入重试队列(" + nextRetry + "/" + task.getMaxRetryCount() + "): " + error);
            } else {
                task.setTaskStatus(TranscribeTaskStatus.FAILED);
                task.setNextRunTime(null);
                qcTranscribeTaskRepository.save(task);
                markCallStatus(callId, CallProcessStatus.TRANSCRIBE_FAILED,
                        "异步转写失败，已达到最大重试次数: " + error);
            }
        });
    }

    /**
     * 截断超长错误信息，避免写库爆字段。
     */
    private String shrink(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 480 ? message.substring(0, 480) : message;
    }
}
