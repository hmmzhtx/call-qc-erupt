package com.oai.callqc.service.impl;



/**
 * 源码中文讲解：转写服务实现
 *
 * - 负责调用本地免费 ASR 服务，把录音转成文本后保存到数据库。
 * - 同时兼容“外部系统直接回传转写结果”和“上传录音后现场转写”两种模式。
 * - 如果开启自动质检，还会在转写入库后直接执行基础质检。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oai.callqc.common.BusinessException;
import com.oai.callqc.config.QcProperties;
import com.oai.callqc.config.TranscriptionProperties;
import com.oai.callqc.dto.LocalAsrResponse;
import com.oai.callqc.dto.TranscriptSegmentRequest;
import com.oai.callqc.dto.TranscriptUploadRequest;
import com.oai.callqc.entity.CallRecord;
import com.oai.callqc.entity.CallTranscript;
import com.oai.callqc.enums.CallProcessStatus;
import com.oai.callqc.repository.CallRecordRepository;
import com.oai.callqc.repository.CallTranscriptRepository;
import com.oai.callqc.service.BasicQcService;
import com.oai.callqc.service.TranscriptService;
import com.oai.callqc.vo.TranscriptionResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TranscriptServiceImpl implements TranscriptService {

    private final CallRecordRepository callRecordRepository;
    private final CallTranscriptRepository callTranscriptRepository;
    private final TranscriptionProperties transcriptionProperties;
    private final QcProperties qcProperties;
    private final RestClient transcriptionRestClient;
    private final ObjectMapper objectMapper;
    private final BasicQcService basicQcService;
    /**
     * 把转写结果持久化到数据库。
     *      * 这里会先清理旧分段，再批量保存新分段，最后更新主表状态。
     *      * 如果配置了“转写后自动质检”，这里还会继续触发基础质检。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTranscript(String callId, TranscriptUploadRequest request) {
        CallRecord callRecord = callRecordRepository.findByCallId(callId)
                .orElseThrow(() -> new BusinessException("通话记录不存在: " + callId));
        callTranscriptRepository.deleteByCallId(callId);
        List<CallTranscript> entities = new ArrayList<>();
        int persistedIndex = 1;
        for (TranscriptSegmentRequest segment : request.getSegments()) {
            List<String> textChunks = splitTranscriptText(segment.getTranscriptText());
            for (String textChunk : textChunks) {
                CallTranscript entity = new CallTranscript();
                entity.setCallId(callId);
                entity.setSegmentIndex(persistedIndex++);
                entity.setSpeakerRole(segment.getSpeakerRole());
                entity.setStartMs(segment.getStartMs());
                entity.setEndMs(segment.getEndMs());
                entity.setTranscriptText(textChunk);
                entity.setConfidence(segment.getConfidence());
                entities.add(entity);
            }
        }
        callTranscriptRepository.saveAll(entities);
        callRecord.setTranscriptSegmentCount(entities.size());
        callRecord.setLastProcessTime(LocalDateTime.now());
        callRecord.setProcessStatus(CallProcessStatus.TRANSCRIBED.name());
        callRecord.setProcessMessage("转写完成，共 " + entities.size() + " 段");
        callRecordRepository.save(callRecord);

        if (qcProperties.isAutoExecuteAfterTranscription()) {
            basicQcService.execute(callId);
        }
    }
    /**
     * 按照 callId 找到录音地址，然后调用本地 ASR 服务进行同步转写。
     *      * 适合“录音已存在，只需要发起一次转写”的场景。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TranscriptionResultVO transcribeByCallId(String callId) {
        ensureEnabled();
        CallRecord callRecord = callRecordRepository.findByCallId(callId)
                .orElseThrow(() -> new BusinessException("通话记录不存在: " + callId));
        if (!StringUtils.hasText(callRecord.getRecordingUrl())) {
            /**
             * 功能说明：封装当前类中的一个业务步骤，供上层流程按顺序调用。
             * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
             * @param "录音地址为空，无法自动转写" 方法输入参数，具体含义请结合调用场景和字段命名理解。
             */
            throw new BusinessException("录音地址为空，无法自动转写");
        }

        ResolvedAudioFile resolvedAudioFile = resolveAudioFile(callRecord);
        try {
            TranscriptUploadRequest request = executeTranscription(resolvedAudioFile.path(), callId);
            saveTranscript(callId, request);
            /**
             * 功能说明：构建当前场景需要的对象、摘要或返回结果。
             * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
             * @param callId 方法输入参数，具体含义请结合调用场景和字段命名理解。
             * @param request 方法输入参数，具体含义请结合调用场景和字段命名理解。
             */
            return buildResult(callId, request);
        } finally {
            cleanup(resolvedAudioFile);
        }
    }
    /**
     * 接收上传的音频文件并立即执行同步转写。
     *      * 适合调试、临时补录或前端直接上传文件的场景。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TranscriptionResultVO transcribeByUpload(String callId, MultipartFile file) {
        ensureEnabled();
        callRecordRepository.findByCallId(callId)
                .orElseThrow(() -> new BusinessException("通话记录不存在: " + callId));
        if (file == null || file.isEmpty()) {
            /**
             * 功能说明：封装当前类中的一个业务步骤，供上层流程按顺序调用。
             * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
             * @param "上传的录音文件不能为空" 方法输入参数，具体含义请结合调用场景和字段命名理解。
             */
            throw new BusinessException("上传的录音文件不能为空");
        }

        ResolvedAudioFile resolvedAudioFile = saveUploadedFile(file, callId);
        try {
            TranscriptUploadRequest request = executeTranscription(resolvedAudioFile.path(), callId);
            saveTranscript(callId, request);
            /**
             * 功能说明：构建当前场景需要的对象、摘要或返回结果。
             * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
             * @param callId 方法输入参数，具体含义请结合调用场景和字段命名理解。
             * @param request 方法输入参数，具体含义请结合调用场景和字段命名理解。
             */
            return buildResult(callId, request);
        } finally {
            cleanup(resolvedAudioFile);
        }
    }

    /**
     * 功能说明：校验前置条件是否满足，不满足时直接终止后续流程。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     */
    private void ensureEnabled() {
        if (!transcriptionProperties.isEnabled()) {
            /**
             * 功能说明：封装当前类中的一个业务步骤，供上层流程按顺序调用。
             * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
             * @param qc.transcription.enabled" 方法输入参数，具体含义请结合调用场景和字段命名理解。
             */
            throw new BusinessException("本地 ASR 转写未启用，请先在 application.yml 中打开 qc.transcription.enabled");
        }
        if (!StringUtils.hasText(transcriptionProperties.getBaseUrl())) {
            /**
             * 功能说明：封装当前类中的一个业务步骤，供上层流程按顺序调用。
             * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
             * @param qc.transcription.base-url" 方法输入参数，具体含义请结合调用场景和字段命名理解。
             */
            throw new BusinessException("缺少本地 ASR 服务地址，请配置 qc.transcription.base-url");
        }
    }

    /**
     * 功能说明：执行当前业务场景的核心处理逻辑。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param audioPath 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param callId 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private TranscriptUploadRequest executeTranscription(Path audioPath, String callId) {
        String responseBody;
        try {
            // 优先走“本地文件路径转写”接口。
            // 这样 Java 与 Python 共用 temp/audio 目录时，不需要再做一次 multipart 上传。
            responseBody = executePathBasedTranscription(audioPath, callId);
        } catch (RestClientResponseException ex) {
            // 只有在接口不存在或请求格式不兼容时，才回退到 multipart。
            // 如果 Python 已经返回 5xx，通常说明音频文件、ffmpeg 或模型本身异常，
            // 此时继续重试 multipart 只会重复报错，反而掩盖真实原因。
            boolean shouldFallback = ex.getStatusCode().is4xxClientError();
            if (shouldFallback) {
                log.warn("本地 ASR 路径转写失败，准备回退到 multipart 上传, callId={}, status={}, body={}", callId, ex.getStatusCode(), ex.getResponseBodyAsString());
                responseBody = executeMultipartTranscription(audioPath, callId);
            } else {
                throw new BusinessException("本地 ASR 路径转写失败: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString());
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("本地 ASR 路径转写异常，准备回退到 multipart, callId={}", callId, ex);
            responseBody = executeMultipartTranscription(audioPath, callId);
        }

        LocalAsrResponse response = parseResponse(responseBody);
        return toUploadRequest(response);
    }

    /**
     * 直接把本地音频文件路径发给 Python ASR 服务。
     */
    private String executePathBasedTranscription(Path audioPath, String callId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("audio_path", audioPath.toAbsolutePath().toString());
        payload.put("hotword", transcriptionProperties.getHotword());
        try {
            return transcriptionRestClient.post()
                    .uri(transcriptionProperties.getTranscribePathByLocalFile())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            log.error("本地 ASR 路径接口调用失败, callId={}, status={}, body={}", callId, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw ex;
        } catch (Exception ex) {
            log.error("本地 ASR 路径接口调用异常, callId={}", callId, ex);
            throw new BusinessException("本地 ASR 路径转写失败: " + ex.getMessage());
        }
    }

    /**
     * 传统 multipart 上传模式。
     * 作为路径模式失败后的兼容兜底。
     */
    private String executeMultipartTranscription(Path audioPath, String callId) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", new FileSystemResource(audioPath));
        if (StringUtils.hasText(transcriptionProperties.getHotword())) {
            bodyBuilder.part("hotword", transcriptionProperties.getHotword());
        }

        try {
            return transcriptionRestClient.post()
                    .uri(transcriptionProperties.getTranscribePath())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(bodyBuilder.build())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            log.error("本地 ASR 接口调用失败, callId={}, status={}, body={}", callId, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BusinessException("本地 ASR 接口调用失败: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("本地 ASR 转写发生异常, callId={}", callId, ex);
            throw new BusinessException("本地 ASR 转写失败: " + ex.getMessage());
        }
    }

    /**
     * 功能说明：解析输入内容或第三方返回值，转换为系统内部对象。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param responseBody 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private LocalAsrResponse parseResponse(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, LocalAsrResponse.class);
        } catch (JsonProcessingException e) {
            /**
             * 功能说明：封装当前类中的一个业务步骤，供上层流程按顺序调用。
             * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
             * @param e.getMessage() 方法输入参数，具体含义请结合调用场景和字段命名理解。
             */
            throw new BusinessException("本地 ASR 返回结果解析失败: " + e.getMessage());
        }
    }

    /**
     * 功能说明：接收外部上传的数据或文件，并完成基础校验与后续处理。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param response 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private TranscriptUploadRequest toUploadRequest(LocalAsrResponse response) {
        if (response == null) {
            /**
             * 功能说明：封装当前类中的一个业务步骤，供上层流程按顺序调用。
             * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
             * @param 未返回结果" 方法输入参数，具体含义请结合调用场景和字段命名理解。
             */
            throw new BusinessException("本地 ASR 未返回结果");
        }
        if (Boolean.FALSE.equals(response.getSuccess())) {
            String msg = StringUtils.hasText(response.getDetail()) ? response.getDetail() : response.getMessage();
            /**
             * 功能说明：封装当前类中的一个业务步骤，供上层流程按顺序调用。
             * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
             * @param "未知错误") 方法输入参数，具体含义请结合调用场景和字段命名理解。
             */
            throw new BusinessException("本地 ASR 转写失败: " + (StringUtils.hasText(msg) ? msg : "未知错误"));
        }

        TranscriptUploadRequest request = new TranscriptUploadRequest();
        List<TranscriptSegmentRequest> segments = new ArrayList<>();
        Map<String, String> speakerRoleMapping = new LinkedHashMap<>();

        if (response.getSegments() != null && !response.getSegments().isEmpty()) {
            int index = 1;
            for (LocalAsrResponse.Segment segment : response.getSegments()) {
                if (!StringUtils.hasText(segment.getText())) {
                    continue;
                }
                TranscriptSegmentRequest item = new TranscriptSegmentRequest();
                item.setSegmentIndex(segment.getIndex() == null ? index : segment.getIndex());
                item.setSpeakerRole(resolveSpeakerRole(segment, speakerRoleMapping));
                item.setStartMs(segment.getStartMs() == null ? 0L : segment.getStartMs());
                item.setEndMs(segment.getEndMs() == null ? 0L : segment.getEndMs());
                item.setTranscriptText(segment.getText().trim());
                item.setConfidence(segment.getConfidence());
                segments.add(item);
                index++;
            }
        }

        if (segments.isEmpty() && StringUtils.hasText(response.getText())) {
            TranscriptSegmentRequest single = new TranscriptSegmentRequest();
            single.setSegmentIndex(1);
            single.setSpeakerRole(transcriptionProperties.getDefaultSpeakerRole());
            single.setStartMs(0L);
            single.setEndMs(0L);
            single.setTranscriptText(response.getText().trim());
            segments.add(single);
        }

        if (segments.isEmpty()) {
            /**
             * 功能说明：封装当前类中的一个业务步骤，供上层流程按顺序调用。
             * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
             * @param 转写成功但未返回有效文本，请检查音频内容或服务日志" 方法输入参数，具体含义请结合调用场景和字段命名理解。
             */
            throw new BusinessException("本地 ASR 转写成功但未返回有效文本，请检查音频内容或服务日志");
        }

        request.setSegments(segments);
        return request;
    }

    /**
     * 功能说明：解析并确定最终要使用的配置、路径、角色或业务值。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param segment 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param speakerRoleMapping 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    /**
     * 对转写文本做安全切分，兼容老库里 transcript_text 仍是 VARCHAR(255) 的情况。
     *
     * <p>优先按换行和中文/英文标点做软切分；如果仍然过长，再按固定长度硬切分。</p>
     */
    private List<String> splitTranscriptText(String transcriptText) {
        final int safeLength = 240;
        List<String> result = new ArrayList<>();
        if (!StringUtils.hasText(transcriptText)) {
            result.add("");
            return result;
        }
        String normalized = transcriptText.replace("\r", "").trim();
        if (normalized.length() <= safeLength) {
            result.add(normalized);
            return result;
        }

        StringBuilder current = new StringBuilder();
        for (char ch : normalized.toCharArray()) {
            current.append(ch);
            boolean softBreak = ch == '\n' || ch == '。' || ch == '！' || ch == '？' || ch == '；' || ch == ';' || ch == ',' || ch == '，';
            if (current.length() >= safeLength || (softBreak && current.length() >= safeLength / 2)) {
                result.add(current.toString().trim());
                current.setLength(0);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        List<String> hardened = new ArrayList<>();
        for (String item : result) {

            if (item.length() <= safeLength) {
                hardened.add(item);
                continue;
            }
            for (int i = 0; i < item.length(); i += safeLength) {
                hardened.add(item.substring(i, Math.min(i + safeLength, item.length())).trim());
            }
        }
        return hardened;
    }

    private String resolveSpeakerRole(LocalAsrResponse.Segment segment, Map<String, String> speakerRoleMapping) {
        if (StringUtils.hasText(segment.getSpeakerRole())) {
            return segment.getSpeakerRole().trim();
        }
        if (!StringUtils.hasText(segment.getSpeaker())) {
            return transcriptionProperties.getDefaultSpeakerRole();
        }
        if (!"callcenter".equalsIgnoreCase(transcriptionProperties.getSpeakerRoleMode())) {
            return "SPEAKER_" + segment.getSpeaker().toUpperCase(Locale.ROOT);
        }
        return speakerRoleMapping.computeIfAbsent(segment.getSpeaker(), key -> {
            if (speakerRoleMapping.isEmpty()) {
                return "AGENT";
            }
            if (speakerRoleMapping.size() == 1) {
                return "CUSTOMER";
            }
            return "SPEAKER_" + key.toUpperCase(Locale.ROOT);
        });
    }

    /**
     * 功能说明：构建当前场景需要的对象、摘要或返回结果。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param callId 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param request 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private TranscriptionResultVO buildResult(String callId, TranscriptUploadRequest request) {
        String preview = request.getSegments().stream()
                .limit(3)
                .map(TranscriptSegmentRequest::getTranscriptText)
                .reduce((a, b) -> a + " | " + b)
                .orElse("");
        CallRecord callRecord = callRecordRepository.findByCallId(callId).orElse(null);
        return TranscriptionResultVO.builder()
                .callId(callId)
                .segmentCount(request.getSegments().size())
                .processStatus(callRecord == null ? CallProcessStatus.TRANSCRIBED.name() : callRecord.getProcessStatus())
                .transcriptPreview(preview)
                .build();
    }

    /**
     * 功能说明：解析并确定最终要使用的配置、路径、角色或业务值。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param recordingUrl 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param callId 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private ResolvedAudioFile resolveAudioFile(CallRecord callRecord) {
        String callId = callRecord.getCallId();
        List<String> candidates = new ArrayList<>();
        if (StringUtils.hasText(callRecord.getRecordingUrl())) {
            candidates.add(callRecord.getRecordingUrl());
        }
        if (StringUtils.hasText(callRecord.getRecordingFileName())) {
            candidates.add(callRecord.getRecordingFileName());
        }

        List<String> checked = new ArrayList<>();
        for (String candidate : candidates) {
            if (!StringUtils.hasText(candidate)) {
                continue;
            }
            try {
                String normalized = normalizeRecordingLocation(candidate);
                checked.add(normalized);
                if (isRemoteUrl(normalized)) {
                    return downloadRemoteFile(normalized, callId);
                }
                Path localPath = resolveLocalPath(normalized);
                checked.add(localPath.toString());
                if (Files.exists(localPath)) {
                    return new ResolvedAudioFile(localPath, false);
                }
            } catch (Exception ignored) {
                checked.add(candidate + " (解析失败: " + ignored.getMessage() + ")");
            }
        }

        throw new BusinessException("未找到可用录音文件，请检查 recordingUrl / recordingFileName 或 temp/audio 目录。已检查: " + checked);
    }

    /**
     * 对录音地址做兼容处理。
     *
     * <p>兼容场景：</p>
     * <ul>
     *     <li>标准 http / https 地址</li>
     *     <li>file: 本地文件 URI</li>
     *     <li>Windows 盘符路径，例如 D:\recordings\a.wav</li>
     *     <li>Linux / 相对路径，例如 /data/a.wav、./uploads/a.wav</li>
     *     <li>缺少协议头的远程地址，例如 10.0.0.158:443/common/record?... 或 host:80/path</li>
     * </ul>
     */
    private String normalizeRecordingLocation(String recordingUrl) {
        if (!StringUtils.hasText(recordingUrl)) {
            throw new BusinessException("录音地址为空，无法解析");
        }
        String value = recordingUrl.trim();
        if (isRemoteUrl(value) || value.startsWith("file:")) {
            return value;
        }
        if (looksLikeWindowsPath(value) || looksLikeLocalPath(value)) {
            return value;
        }
        if (looksLikeRemoteWithoutScheme(value)) {
            String scheme = value.contains(":443/") || value.endsWith(":443") ? "https://" : "http://";
            return scheme + value;
        }
        return value;
    }

    /**
     * 判断是否为已经带协议头的远程地址。
     */
    private boolean isRemoteUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    /**
     * 判断是否像 Windows 绝对路径，例如 D:\a.wav。
     */
    private boolean looksLikeWindowsPath(String value) {
        return value.matches("^[A-Za-z]:[\\/].*");
    }

    /**
     * 判断是否像本地路径。
     */
    private boolean looksLikeLocalPath(String value) {
        return value.startsWith("/") || value.startsWith("./") || value.startsWith("../") || value.contains("\\");
    }

    /**
     * 判断是否像“缺少协议头的远程地址”。
     */
    private boolean looksLikeRemoteWithoutScheme(String value) {
        if (value.contains(" ")) {
            return false;
        }
        if (!value.contains("/") && !value.contains("?")) {
            return false;
        }
        int colonIndex = value.indexOf(':');
        if (colonIndex <= 0) {
            return false;
        }
        if (colonIndex == 1 && Character.isLetter(value.charAt(0))) {
            return false;
        }
        return true;
    }

    /**
     * 解析本地录音文件路径。
     *
     * <p>优先支持以下几种情况：</p>
     * <ul>
     *     <li>绝对路径，例如 D:\\recordings\\a.wav</li>
     *     <li>file: URI</li>
     *     <li>仅文件名，例如 20260001_13543010534_20260326170407.wav，会自动在 temp/audio 下查找</li>
     *     <li>相对路径，例如 ./temp/audio/a.wav</li>
     * </ul>
     */
    private Path resolveLocalPath(String normalized) {
        try {
            if (normalized.startsWith("file:")) {
                return Paths.get(URI.create(normalized)).toAbsolutePath().normalize();
            }
            Path path = Paths.get(normalized);
            if (path.isAbsolute()) {
                return path.toAbsolutePath().normalize();
            }

            Path configuredTempDir = prepareTempDir().toAbsolutePath().normalize();
            if (normalized.contains("/") || normalized.contains("\\") || normalized.startsWith(".")) {
                return path.toAbsolutePath().normalize();
            }
            return configuredTempDir.resolve(path.getFileName().toString()).normalize();
        } catch (IOException e) {
            throw new BusinessException("初始化临时录音目录失败: " + e.getMessage());
        }
    }

    /**
     * 下载远程录音。
     *
     * <p>这里不再使用 Java 11 HttpClient，因为某些录音系统返回的地址主机名里可能包含下划线，
     * HttpClient 对这类 URI 更严格，会直接报 unsupported URI。改用 URLConnection 可以兼容这类地址。</p>
     */
    private ResolvedAudioFile downloadRemoteFile(String url, String callId) {
        try {
            Path tempDir = prepareTempDir();
            String fileName = "call-" + callId + extractExtension(url);
            Path target = tempDir.resolve(fileName);

            URL remote = new URL(url);
            var connection = remote.openConnection();
            connection.setConnectTimeout((int) Duration.ofSeconds(transcriptionProperties.getConnectTimeoutSeconds()).toMillis());
            connection.setReadTimeout((int) Duration.ofSeconds(transcriptionProperties.getReadTimeoutSeconds()).toMillis());

            if (connection instanceof java.net.HttpURLConnection httpURLConnection) {
                httpURLConnection.setRequestMethod("GET");
                int status = httpURLConnection.getResponseCode();
                if (status >= 400) {
                    throw new BusinessException("下载录音失败，HTTP状态码=" + status);
                }
            }

            try (InputStream inputStream = connection.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new ResolvedAudioFile(target, true);
        } catch (IOException e) {
            throw new BusinessException("下载远程录音失败: " + e.getMessage());
        }
    }

    /**
     * 功能说明：接收外部上传的数据或文件，并完成基础校验与后续处理。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param file 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param callId 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private ResolvedAudioFile saveUploadedFile(MultipartFile file, String callId) {
        try {
            Path tempDir = prepareTempDir();
            String extension = extractExtension(file.getOriginalFilename());
            Path target = tempDir.resolve("upload-" + callId + extension);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            /**
             * 功能说明：解析并确定最终要使用的配置、路径、角色或业务值。
             * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
             * @param target 方法输入参数，具体含义请结合调用场景和字段命名理解。
             * @param true 方法输入参数，具体含义请结合调用场景和字段命名理解。
             */
            return new ResolvedAudioFile(target, true);
        } catch (IOException e) {
            /**
             * 功能说明：封装当前类中的一个业务步骤，供上层流程按顺序调用。
             * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
             * @param e.getMessage() 方法输入参数，具体含义请结合调用场景和字段命名理解。
             */
            throw new BusinessException("保存上传录音失败: " + e.getMessage());
        }
    }

    /**
     * 功能说明：准备目录、环境或上下文信息，确保后续处理可执行。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     */
    private Path prepareTempDir() throws IOException {
        Path dir = Paths.get(transcriptionProperties.getTempDir());
        Files.createDirectories(dir);
        return dir;
    }

    /**
     * 功能说明：从输入内容中提取关键字段，供后续逻辑使用。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param filename 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return ".wav";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    /**
     * 功能说明：清理临时文件或中间资源，避免资源泄露。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param resolvedAudioFile 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private void cleanup(ResolvedAudioFile resolvedAudioFile) {
        if (resolvedAudioFile == null || !resolvedAudioFile.cleanup() || resolvedAudioFile.path() == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolvedAudioFile.path());
        } catch (IOException e) {
            log.warn("删除临时录音文件失败: {}", resolvedAudioFile.path(), e);
        }
    }

    private record ResolvedAudioFile(Path path, boolean cleanup) {
    }
}
