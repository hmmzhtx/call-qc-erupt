package com.oai.callqc.service.impl;

import com.oai.callqc.config.AsyncTaskProperties;
import com.oai.callqc.entity.QcTranscribeTask;
import com.oai.callqc.enums.TranscribeTaskStatus;
import com.oai.callqc.repository.QcTranscribeTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 转写任务队列调度器。
 *
 * 定时从数据库中拉取待执行任务，并按限制并发数进行分发。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncTranscribeQueueScheduler {

    private final QcTranscribeTaskRepository qcTranscribeTaskRepository;
    private final AsyncTaskProperties asyncTaskProperties;
    private final AsyncCallProcessingWorker asyncCallProcessingWorker;

    /**
     * 固定频率扫描队列。
     */
    @Scheduled(fixedDelayString = "${qc.async.dispatch-fixed-delay-ms:2000}")
    public void dispatchTasks() {
        if (!asyncTaskProperties.isEnabled()) {
            return;
        }
        long runningCount = qcTranscribeTaskRepository.countByTaskStatus(TranscribeTaskStatus.RUNNING);
        int available = asyncTaskProperties.getMaxConcurrentRunning() - (int) runningCount;
        if (available <= 0) {
            return;
        }
        List<QcTranscribeTask> candidates = qcTranscribeTaskRepository
                .findByTaskStatusInAndNextRunTimeLessThanEqualOrderByPriorityNumDescIdAsc(
                        Set.of(TranscribeTaskStatus.WAITING, TranscribeTaskStatus.RETRY_WAIT),
                        LocalDateTime.now(),
                        PageRequest.of(0, Math.min(available, asyncTaskProperties.getQueueBatchSize())));
        for (QcTranscribeTask task : candidates) {
            if (tryLockTask(task.getId())) {
                asyncCallProcessingWorker.consumeTask(task.getId());
            }
        }
    }

    /**
     * 抢占任务，防止定时任务重复分发同一条记录。
     */
    @Transactional
    public boolean tryLockTask(Long taskId) {
        return qcTranscribeTaskRepository.findById(taskId).map(task -> {
            if (!(task.getTaskStatus() == TranscribeTaskStatus.WAITING || task.getTaskStatus() == TranscribeTaskStatus.RETRY_WAIT)) {
                return false;
            }
            task.setTaskStatus(TranscribeTaskStatus.RUNNING);
            task.setLockedBy("scheduler");
            task.setLockedAt(LocalDateTime.now());
            qcTranscribeTaskRepository.save(task);
            return true;
        }).orElse(false);
    }
}
