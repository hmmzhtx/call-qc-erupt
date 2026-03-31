package com.oai.callqc.service.impl;

import com.oai.callqc.common.BusinessException;
import com.oai.callqc.config.AsyncTaskProperties;
import com.oai.callqc.entity.CallRecord;
import com.oai.callqc.entity.QcTranscribeTask;
import com.oai.callqc.enums.CallProcessStatus;
import com.oai.callqc.enums.TranscribeTaskStatus;
import com.oai.callqc.repository.CallRecordRepository;
import com.oai.callqc.repository.QcTranscribeTaskRepository;
import com.oai.callqc.service.AsyncCallProcessingService;
import com.oai.callqc.vo.AsyncTaskSubmitVO;
import com.oai.callqc.vo.BatchAsyncTaskSubmitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 异步转写任务提交服务实现。
 *
 * 当前版本不再“提交即直接调用转写”，而是统一写入数据库任务队列，
 * 由后台调度器按固定并发数消费，避免批量提交时把本地 ASR 服务打满。
 */
@Service
@RequiredArgsConstructor
public class AsyncCallProcessingServiceImpl implements AsyncCallProcessingService {

    private final CallRecordRepository callRecordRepository;
    private final QcTranscribeTaskRepository qcTranscribeTaskRepository;
    private final AsyncTaskProperties asyncTaskProperties;

    /**
     * 提交单条异步转写任务。
     */
    @Override
    @Transactional
    public AsyncTaskSubmitVO submitExistingRecording(String callId, String submitMessage) {
        if (!asyncTaskProperties.isEnabled()) {
            throw new BusinessException("异步转写任务未启用，请检查 qc.async.enabled 配置");
        }

        CallRecord callRecord = callRecordRepository.findByCallId(callId)
                .orElseThrow(() -> new BusinessException("通话记录不存在: " + callId));
        if (!StringUtils.hasText(callRecord.getRecordingUrl()) && !StringUtils.hasText(callRecord.getRecordingFileName())) {
            throw new BusinessException("录音地址为空，无法提交异步转写");
        }

        LocalDateTime now = LocalDateTime.now();
        QcTranscribeTask task = qcTranscribeTaskRepository.findByCallId(callId)
                .orElseGet(QcTranscribeTask::new);
        task.setCallId(callId);
        task.setTaskStatus(TranscribeTaskStatus.WAITING);
        task.setPriorityNum(task.getPriorityNum() == null ? 0 : task.getPriorityNum());
        task.setRetryCount(0);
        task.setMaxRetryCount(asyncTaskProperties.getMaxRetryCount());
        task.setNextRunTime(now);
        task.setLockedAt(null);
        task.setLockedBy(null);
        task.setLastError(null);
        qcTranscribeTaskRepository.save(task);

        callRecord.setProcessStatus(CallProcessStatus.TRANSCRIBE_PENDING.name());
        callRecord.setProcessMessage(StringUtils.hasText(submitMessage) ? submitMessage : "已进入转写队列，等待处理");
        callRecord.setLastProcessTime(now);
        callRecordRepository.save(callRecord);

        return AsyncTaskSubmitVO.builder()
                .callId(callId)
                .processStatus(callRecord.getProcessStatus())
                .processMessage(callRecord.getProcessMessage())
                .recordingUrl(callRecord.getRecordingUrl())
                .queuedAt(now)
                .build();
    }

    /**
     * 批量提交异步转写任务。
     * <p>如果选中的记录已经转写完成/质检完成/已复核，则自动剔除，并在返回结果中给出提示说明。</p>
     */
    @Override
    public BatchAsyncTaskSubmitVO submitBatchExistingRecordings(List<String> callIds, String submitMessage) {
        if (callIds == null || callIds.isEmpty()) {
            throw new BusinessException("请选择至少一条通话记录");
        }
        Set<String> uniq = new LinkedHashSet<>();
        for (String callId : callIds) {
            if (StringUtils.hasText(callId)) {
                uniq.add(callId.trim());
            }
        }
        if (uniq.isEmpty()) {
            throw new BusinessException("未获取到有效的通话ID");
        }

        List<String> successCallIds = new ArrayList<>();
        List<String> failedMessages = new ArrayList<>();
        List<String> skippedMessages = new ArrayList<>();
        for (String callId : uniq) {
            try {
                CallRecord callRecord = callRecordRepository.findByCallId(callId)
                        .orElseThrow(() -> new BusinessException("通话记录不存在: " + callId));

                if (shouldSkipForBatchTranscribe(callRecord)) {
                    skippedMessages.add(buildSkipMessage(callRecord));
                    continue;
                }

                submitExistingRecording(callId, submitMessage);
                successCallIds.add(callId);
            } catch (Exception ex) {
                failedMessages.add(callId + ": " + ex.getMessage());
            }
        }

        return BatchAsyncTaskSubmitVO.builder()
                .totalCount(uniq.size())
                .successCount(successCallIds.size())
                .failedCount(failedMessages.size())
                .skippedCount(skippedMessages.size())
                .successCallIds(successCallIds)
                .failedMessages(failedMessages)
                .skippedMessages(skippedMessages)
                .build();
    }

    /**
     * 判断一条通话在“批量异步转写”场景下是否应自动剔除。
     * 只要已经有转写结果，或者流程已经走到转写完成/质检完成/复核完成，就不再重复入队。
     */
    private boolean shouldSkipForBatchTranscribe(CallRecord callRecord) {
        if (callRecord == null) {
            return true;
        }
        Integer transcriptSegmentCount = callRecord.getTranscriptSegmentCount();
        if (transcriptSegmentCount != null && transcriptSegmentCount > 0) {
            return true;
        }
        String status = callRecord.getProcessStatus();
        if (!StringUtils.hasText(status)) {
            return false;
        }
        return CallProcessStatus.TRANSCRIBED.name().equals(status)
                || CallProcessStatus.QC_DONE.name().equals(status)
                || CallProcessStatus.REVIEWED.name().equals(status);
    }

    /**
     * 组装“已自动剔除”的提示文案。
     */
    private String buildSkipMessage(CallRecord callRecord) {
        String callId = callRecord.getCallId();
        Integer count = callRecord.getTranscriptSegmentCount();
        String status = callRecord.getProcessStatus();
        if (count != null && count > 0) {
            return callId + ": 已存在 " + count + " 条转写分段，已自动跳过";
        }
        return callId + ": 当前状态为 " + status + "，已自动跳过";
    }
}
