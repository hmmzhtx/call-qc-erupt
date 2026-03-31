package com.oai.callqc.repository;

import com.oai.callqc.entity.QcTranscribeTask;
import com.oai.callqc.enums.TranscribeTaskStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 转写任务队列仓储。
 */
public interface QcTranscribeTaskRepository extends JpaRepository<QcTranscribeTask, Long> {

    Optional<QcTranscribeTask> findByCallId(String callId);

    long countByTaskStatus(TranscribeTaskStatus taskStatus);

    List<QcTranscribeTask> findByTaskStatusInAndNextRunTimeLessThanEqualOrderByPriorityNumDescIdAsc(
            Collection<TranscribeTaskStatus> statuses, LocalDateTime nextRunTime, Pageable pageable);
}
