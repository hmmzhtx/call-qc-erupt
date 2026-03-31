package com.oai.callqc.service.impl;

import com.oai.callqc.config.AsyncTaskProperties;
import com.oai.callqc.dto.CallImportRequest;
import com.oai.callqc.entity.CallRecord;
import com.oai.callqc.enums.CallProcessStatus;
import com.oai.callqc.repository.CallRecordRepository;
import com.oai.callqc.service.AsyncCallProcessingService;
import com.oai.callqc.service.CallImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * 通话导入服务实现。
 *
 * <p>职责：</p>
 * <ul>
 *     <li>把外部传入的通话元数据写入主表</li>
 *     <li>在缺少 callId 时自动生成稳定的唯一标识</li>
 *     <li>在缺少录音文件名时，按照“坐席工号_客户号码_时间戳.wav”自动生成</li>
 *     <li>根据配置决定是否自动提交异步转写任务</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CallImportServiceImpl implements CallImportService {

    private static final DateTimeFormatter FILE_NAME_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CallRecordRepository callRecordRepository;
    private final AsyncTaskProperties asyncTaskProperties;
    private final AsyncCallProcessingService asyncCallProcessingService;

    /**
     * 导入单条通话记录。
     *
     * @param request 外部传入的通话主数据。
     * @return 保存后的通话记录实体。
     */
    @Override
    public CallRecord importCall(CallImportRequest request) {
        fillDerivedFields(request);

        CallRecord entity = callRecordRepository.findByCallId(request.getCallId()).orElseGet(CallRecord::new);
        entity.setCallId(request.getCallId());
        entity.setCallerNumber(request.getCallerNumber());
        entity.setAgentId(request.getAgentId());
        entity.setAgentName(request.getAgentName());
        entity.setCustomerId(request.getCustomerId());
        entity.setCustomerPhone(request.getCustomerPhone());
        entity.setCustomerName(request.getCustomerName());
        entity.setCustomerStatus(request.getCustomerStatus());
        entity.setProjectName(request.getProjectName());
        entity.setTaskName(request.getTaskName());
        entity.setBusinessLine(request.getBusinessLine());
        entity.setSkillGroup(request.getSkillGroup());
        entity.setCallType(request.getCallType());
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        entity.setDurationSeconds(request.getDurationSeconds());
        entity.setRecordingFileName(request.getRecordingFileName());
        entity.setRecordingUrl(request.getRecordingUrl());
        entity.setLastProcessTime(LocalDateTime.now());

        if (StringUtils.hasText(request.getRecordingUrl())) {
            entity.setProcessStatus(CallProcessStatus.IMPORTED.name());
            entity.setProcessMessage("通话已导入，待转写");
        } else {
            entity.setProcessStatus(CallProcessStatus.IMPORTED.name());
            entity.setProcessMessage("通话已导入，等待上传录音");
        }

        CallRecord saved = callRecordRepository.save(entity);

        boolean shouldAutoSubmit = asyncTaskProperties.isEnabled()
                && asyncTaskProperties.isAutoSubmitAfterImport()
                && Boolean.TRUE.equals(request.getAutoSubmitAsync())
                && StringUtils.hasText(saved.getRecordingUrl());
        if (shouldAutoSubmit) {
            asyncCallProcessingService.submitExistingRecording(saved.getCallId(), "通话导入成功，已自动加入转写队列");
        }
        return saved;
    }

    /**
     * 补齐导入请求中的派生字段。
     *
     * <p>主要做三件事：</p>
     * <ol>
     *     <li>如果没有 callId，则自动生成 callId</li>
     *     <li>如果没有录音文件名，则自动生成 recordingFileName</li>
     *     <li>如果没有单独传 businessLine / skillGroup，则分别用项目名称 / 任务名称兜底</li>
     * </ol>
     *
     * @param request 原始导入请求。
     */
    private void fillDerivedFields(CallImportRequest request) {
        if (!StringUtils.hasText(request.getBusinessLine()) && StringUtils.hasText(request.getProjectName())) {
            request.setBusinessLine(request.getProjectName());
        }
        if (!StringUtils.hasText(request.getSkillGroup()) && StringUtils.hasText(request.getTaskName())) {
            request.setSkillGroup(request.getTaskName());
        }
        if (request.getStartTime() != null && request.getEndTime() == null && request.getDurationSeconds() != null) {
            request.setEndTime(request.getStartTime().plusSeconds(request.getDurationSeconds()));
        }
        if (!StringUtils.hasText(request.getCallId())) {
            request.setCallId(buildCallId(request));
        }
        if (!StringUtils.hasText(request.getRecordingFileName())) {
            request.setRecordingFileName(buildRecordingFileName(request));
        }
        if (StringUtils.hasText(request.getRecordingUrl())) {
            request.setRecordingUrl(normalizeRecordingUrl(request.getRecordingUrl()));
        }
    }

    /**
     * 生成通话唯一标识。
     *
     * <p>优先按照“坐席工号_客户号码_时间戳”生成，这样既和录音文件命名规范一致，也能用于幂等导入。</p>
     *
     * @param request 通话导入请求。
     * @return 生成后的 callId。
     */
    private String buildCallId(CallImportRequest request) {
        if (StringUtils.hasText(request.getAgentId())
                && StringUtils.hasText(request.getCustomerPhone())
                && request.getStartTime() != null) {
            return normalizeToken(request.getAgentId())
                    + "_"
                    + normalizeToken(request.getCustomerPhone())
                    + "_"
                    + request.getStartTime().format(FILE_NAME_TIME_FORMATTER);
        }
        if (StringUtils.hasText(request.getCallerNumber())
                && StringUtils.hasText(request.getCustomerPhone())
                && request.getStartTime() != null) {
            return normalizeToken(request.getCallerNumber())
                    + "_"
                    + normalizeToken(request.getCustomerPhone())
                    + "_"
                    + request.getStartTime().format(FILE_NAME_TIME_FORMATTER);
        }
        return "CALL_" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    /**
     * 生成录音文件名。
     *
     * <p>文件名格式：坐席工号_客户号码_时间戳.wav</p>
     *
     * @param request 通话导入请求。
     * @return 生成后的录音文件名；如果生成条件不足，则返回 callId.wav。
     */
    private String buildRecordingFileName(CallImportRequest request) {
        if (StringUtils.hasText(request.getAgentId())
                && StringUtils.hasText(request.getCustomerPhone())
                && request.getStartTime() != null) {
            return normalizeToken(request.getAgentId())
                    + "_"
                    + normalizeToken(request.getCustomerPhone())
                    + "_"
                    + request.getStartTime().format(FILE_NAME_TIME_FORMATTER)
                    + ".wav";
        }
        return request.getCallId() + ".wav";
    }

    /**
     * 清洗文件名或主键中的分隔符，避免特殊字符影响后续匹配。
     *
     * @param value 原始字符串。
     * @return 清洗后的字符串。
     */
    private String normalizeToken(String value) {
        return value == null ? "" : value.trim().replaceAll("[^0-9A-Za-z_-]", "");
    }

    /**
     * 规范化导入时的录音地址，兼容“缺少协议头的远程地址”。
     */
    private String normalizeRecordingUrl(String recordingUrl) {
        String value = recordingUrl.trim();
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("file:")) {
            return value;
        }
        if (value.matches("^[A-Za-z]:[\\/].*") || value.startsWith("/") || value.startsWith("./") || value.startsWith("../") || value.contains("\\")) {
            return value;
        }
        if (!value.contains(" ") && (value.contains("/") || value.contains("?"))) {
            int colonIndex = value.indexOf(':');
            if (colonIndex > 1 || !Character.isLetter(value.charAt(0))) {
                String scheme = value.contains(":443/") || value.endsWith(":443") ? "https://" : "http://";
                return scheme + value;
            }
        }
        return value;
    }

}
