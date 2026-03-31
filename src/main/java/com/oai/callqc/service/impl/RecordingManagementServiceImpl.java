package com.oai.callqc.service.impl;

import com.oai.callqc.common.BusinessException;
import com.oai.callqc.config.AsyncTaskProperties;
import com.oai.callqc.config.TranscriptionProperties;
import com.oai.callqc.entity.CallRecord;
import com.oai.callqc.enums.CallProcessStatus;
import com.oai.callqc.repository.CallRecordRepository;
import com.oai.callqc.service.AsyncCallProcessingService;
import com.oai.callqc.service.RecordingManagementService;
import com.oai.callqc.vo.AsyncTaskSubmitVO;
import com.oai.callqc.vo.RecordingZipImportResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 录音管理服务实现。
 *
 * <p>本版做了两个关键调整：</p>
 * <ul>
 *     <li>所有导入/上传的录音统一落到 qc.transcription.temp-dir（默认 ./temp/audio）</li>
 *     <li>新增录音 ZIP 导入，自动解压并按 recordingFileName / callId 匹配通话记录</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RecordingManagementServiceImpl implements RecordingManagementService {

    private static final Set<String> AUDIO_EXTENSIONS = new HashSet<>(Set.of(
            ".wav", ".mp3", ".m4a", ".aac", ".amr", ".ogg", ".flac", ".webm", ".mpga"
    ));

    private final CallRecordRepository callRecordRepository;
    private final AsyncTaskProperties asyncTaskProperties;
    private final AsyncCallProcessingService asyncCallProcessingService;
    private final TranscriptionProperties transcriptionProperties;

    /**
     * 上传单个录音文件，直接保存到 temp/audio，并可自动提交异步转写。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AsyncTaskSubmitVO uploadRecording(String callId, MultipartFile file) {
        CallRecord callRecord = callRecordRepository.findByCallId(callId)
                .orElseThrow(() -> new BusinessException("通话记录不存在: " + callId));
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传的录音文件不能为空");
        }

        Path storedPath = storeFile(callRecord, file);
        LocalDateTime now = LocalDateTime.now();
        String fileName = storedPath.getFileName().toString();
        callRecord.setRecordingFileName(fileName);
        callRecord.setRecordingUrl(storedPath.toAbsolutePath().toString());
        callRecord.setProcessStatus(CallProcessStatus.RECORDING_UPLOADED.name());
        callRecord.setProcessMessage("录音上传成功，已写入 temp/audio");
        callRecord.setLastProcessTime(now);
        callRecordRepository.save(callRecord);

        if (asyncTaskProperties.isEnabled() && asyncTaskProperties.isAutoSubmitAfterRecordingUpload()) {
            return asyncCallProcessingService.submitExistingRecording(callId, "录音上传成功，已自动加入转写队列");
        }

        return AsyncTaskSubmitVO.builder()
                .callId(callId)
                .processStatus(callRecord.getProcessStatus())
                .processMessage(callRecord.getProcessMessage())
                .recordingUrl(callRecord.getRecordingUrl())
                .queuedAt(now)
                .build();
    }

    /**
     * 上传录音 ZIP，自动解压到 temp/audio，并尽量自动匹配通话记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecordingZipImportResultVO importRecordingZip(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传的 ZIP 文件不能为空");
        }
        String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("recordings.zip");
        if (!originalName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new BusinessException("只支持上传 .zip 录音压缩包");
        }

        Path tempDir = prepareAudioDir();
        int totalEntries = 0;
        int extractedAudioCount = 0;
        int matchedCallCount = 0;
        int autoQueuedCount = 0;
        List<String> unmatchedFiles = new ArrayList<>();

        try (InputStream rawInputStream = file.getInputStream(); ZipInputStream zipInputStream = new ZipInputStream(rawInputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                totalEntries++;
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                String fileName = sanitizeFileName(entryName);
                if (!isAudioFile(fileName)) {
                    continue;
                }

                Path target = tempDir.resolve(fileName).normalize();
                if (!target.startsWith(tempDir)) {
                    throw new BusinessException("ZIP 中存在非法文件路径: " + entryName);
                }
                Files.copy(zipInputStream, target, StandardCopyOption.REPLACE_EXISTING);
                extractedAudioCount++;

                Optional<CallRecord> matched = matchCallRecord(fileName);
                if (matched.isPresent()) {
                    CallRecord callRecord = matched.get();
                    callRecord.setRecordingFileName(fileName);
                    callRecord.setRecordingUrl(target.toAbsolutePath().toString());
                    callRecord.setProcessStatus(CallProcessStatus.RECORDING_UPLOADED.name());
                    callRecord.setProcessMessage("录音 ZIP 导入成功，文件已写入 temp/audio");
                    callRecord.setLastProcessTime(LocalDateTime.now());
                    callRecordRepository.save(callRecord);
                    matchedCallCount++;

                    if (asyncTaskProperties.isEnabled() && asyncTaskProperties.isAutoSubmitAfterRecordingUpload()) {
                        asyncCallProcessingService.submitExistingRecording(callRecord.getCallId(), "录音 ZIP 导入成功，已自动加入转写队列");
                        autoQueuedCount++;
                    }
                } else {
                    unmatchedFiles.add(fileName);
                }
            }
        } catch (IOException e) {
            throw new BusinessException("导入录音 ZIP 失败: " + e.getMessage());
        }

        return RecordingZipImportResultVO.builder()
                .zipFileName(originalName)
                .totalEntries(totalEntries)
                .extractedAudioCount(extractedAudioCount)
                .matchedCallCount(matchedCallCount)
                .autoQueuedCount(autoQueuedCount)
                .unmatchedFiles(unmatchedFiles)
                .extractedDir(tempDir.toAbsolutePath().toString())
                .build();
    }

    /**
     * 保存单个录音文件到 temp/audio。
     */
    private Path storeFile(CallRecord callRecord, MultipartFile file) {
        try {
            Path baseDir = prepareAudioDir();
            String originalFilename = file.getOriginalFilename();
            String extension = extractExtension(originalFilename);
            String stableFileName = StringUtils.hasText(callRecord.getRecordingFileName())
                    ? callRecord.getRecordingFileName()
                    : callRecord.getCallId() + extension;
            if (!stableFileName.contains(".")) {
                stableFileName = stableFileName + extension;
            }
            Path target = baseDir.resolve(sanitizeFileName(stableFileName));
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException e) {
            throw new BusinessException("保存录音文件失败: " + e.getMessage());
        }
    }

    /**
     * 准备 temp/audio 目录。
     */
    private Path prepareAudioDir() {
        try {
            Path baseDir = Paths.get(transcriptionProperties.getTempDir());
            Files.createDirectories(baseDir);
            return baseDir.toAbsolutePath().normalize();
        } catch (IOException e) {
            throw new BusinessException("创建 temp/audio 目录失败: " + e.getMessage());
        }
    }

    /**
     * 根据录音文件名匹配通话记录。
     *
     * <p>优先按 recordingFileName 精确匹配，其次尝试把“文件名去扩展名”作为 callId 匹配。</p>
     */
    private Optional<CallRecord> matchCallRecord(String fileName) {
        Optional<CallRecord> byRecordingName = callRecordRepository.findFirstByRecordingFileName(fileName);
        if (byRecordingName.isPresent()) {
            return byRecordingName;
        }
        String baseName = fileName;
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
        }
        return callRecordRepository.findByCallId(baseName);
    }

    /**
     * 从文件名中提取扩展名。
     */
    private String extractExtension(String fileName) {
        if (StringUtils.hasText(fileName) && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.'));
        }
        return ".wav";
    }

    /**
     * 判断是否为支持的音频文件。
     */
    private boolean isAudioFile(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return AUDIO_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    /**
     * 仅保留 ZIP 条目里的文件名，防止路径穿越。
     */
    private String sanitizeFileName(String source) {
        String normalized = source.replace('\\', '/');
        int index = normalized.lastIndexOf('/');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }
}
