package com.oai.callqc.controller;

import com.oai.callqc.common.ApiResponse;
import com.oai.callqc.common.BusinessException;
import com.oai.callqc.dto.CallImportRequest;
import com.oai.callqc.dto.TranscriptUploadRequest;
import com.oai.callqc.entity.CallRecord;
import com.oai.callqc.repository.CallRecordRepository;
import com.oai.callqc.service.AsyncCallProcessingService;
import com.oai.callqc.service.CallImportService;
import com.oai.callqc.service.CallRecordSpreadsheetImportService;
import com.oai.callqc.service.RecordingManagementService;
import com.oai.callqc.service.TranscriptService;
import com.oai.callqc.vo.AsyncTaskSubmitVO;
import com.oai.callqc.vo.BatchAsyncTaskSubmitVO;
import com.oai.callqc.vo.CallRecordImportResultVO;
import com.oai.callqc.vo.RecordingZipImportResultVO;
import com.oai.callqc.vo.TranscriptionResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 通话接入与转写控制器。
 *
 * <p>本控制器除了原有的单条导入、录音上传、同步/异步转写能力外，新增了：</p>
 * <ul>
 *     <li>表格导入接口：支持 Excel / CSV 直接导入到通话记录</li>
 *     <li>模板下载接口：方便业务同学按固定表头准备导入文件</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/calls")
@RequiredArgsConstructor
public class CallImportController {

    private final CallImportService callImportService;
    private final TranscriptService transcriptService;
    private final RecordingManagementService recordingManagementService;
    private final AsyncCallProcessingService asyncCallProcessingService;
    private final CallRecordRepository callRecordRepository;
    private final CallRecordSpreadsheetImportService callRecordSpreadsheetImportService;

    /**
     * 导入一通通话的主数据。
     *
     * @param request 通话主数据。
     * @return 保存后的主表记录。
     */
    @PostMapping("/import")
    public ApiResponse<CallRecord> importCall(@Valid @RequestBody CallImportRequest request) {
        return ApiResponse.success("导入成功", callImportService.importCall(request));
    }

    /**
     * 通过 Excel / CSV 批量导入通话记录。
     *
     * @param file 表格文件，支持 .xlsx / .xls / .csv。
     * @return 导入统计结果。
     */
    @PostMapping(value = "/import/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CallRecordImportResultVO> importCallFile(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success("表格导入完成", callRecordSpreadsheetImportService.importSpreadsheet(file));
    }

    /**
     * 下载导入模板。
     *
     * @return CSV 模板文件。
     */
    @GetMapping("/import/template")
    public ResponseEntity<Resource> downloadImportTemplate() {
        byte[] bytes = callRecordSpreadsheetImportService.buildTemplateBytes();
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=call-record-import-template.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .contentLength(bytes.length)
                .body(resource);
    }


    /**
     * 上传录音 ZIP 文件，自动解压到 temp/audio，并按录音文件名匹配通话记录。
     *
     * @param file ZIP 压缩包。
     * @return 导入结果。
     */
    @PostMapping(value = "/recordings/import/zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<RecordingZipImportResultVO> importRecordingZip(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success("录音 ZIP 导入完成", recordingManagementService.importRecordingZip(file));
    }

    /**
     * 接收外部系统直接回传的转写结果。
     *
     * @param callId  通话ID。
     * @param request 转写分段内容。
     * @return 统一返回体。
     */
    @PostMapping("/{callId}/transcripts")
    public ApiResponse<Void> uploadTranscript(@PathVariable String callId,
                                              @Valid @RequestBody TranscriptUploadRequest request) {
        transcriptService.saveTranscript(callId, request);
        return ApiResponse.success("转写结果保存成功", null);
    }

    /**
     * 对已存在录音的通话发起同步转写。
     *
     * @param callId 通话ID。
     * @return 转写结果。
     */
    @PostMapping("/{callId}/transcribe")
    public ApiResponse<TranscriptionResultVO> transcribe(@PathVariable String callId) {
        return ApiResponse.success("自动转写成功", transcriptService.transcribeByCallId(callId));
    }

    /**
     * 手动提交异步转写任务。
     *
     * @param callId 通话ID。
     * @return 异步任务提交结果。
     */
    @PostMapping("/{callId}/transcribe/async")
    public ApiResponse<AsyncTaskSubmitVO> transcribeAsync(@PathVariable String callId) {
        return ApiResponse.success("已加入异步转写队列",
                asyncCallProcessingService.submitExistingRecording(callId, "已手动提交异步转写"));
    }

    /**
     * 批量手动提交异步转写任务。
     */
    @PostMapping("/transcribe/async/batch")
    public ApiResponse<BatchAsyncTaskSubmitVO> transcribeAsyncBatch(@RequestBody List<String> callIds) {
        BatchAsyncTaskSubmitVO result = asyncCallProcessingService.submitBatchExistingRecordings(callIds, "已批量提交异步转写");
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(String.format("批量提交完成：成功 %d 条", result.getSuccessCount()));
        if (result.getSkippedCount() != null && result.getSkippedCount() > 0) {
            messageBuilder.append(String.format("，已跳过 %d 条已转写完成数据", result.getSkippedCount()));
        }
        if (result.getFailedCount() != null && result.getFailedCount() > 0) {
            messageBuilder.append(String.format("，失败 %d 条", result.getFailedCount()));
        }
        return ApiResponse.success(messageBuilder.toString(), result);
    }

    /**
     * 上传录音并立即同步转写。
     *
     * @param callId 通话ID。
     * @param file   录音文件。
     * @return 转写结果。
     */
    @PostMapping(value = "/{callId}/transcribe/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TranscriptionResultVO> transcribeByUpload(@PathVariable String callId,
                                                                 @RequestPart("file") MultipartFile file) {
        return ApiResponse.success("上传录音并转写成功", transcriptService.transcribeByUpload(callId, file));
    }

    /**
     * 上传录音并走异步链路。
     *
     * @param callId 通话ID。
     * @param file   录音文件。
     * @return 异步任务结果。
     */
    @PostMapping(value = "/{callId}/recording/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AsyncTaskSubmitVO> uploadRecording(@PathVariable String callId,
                                                          @RequestPart("file") MultipartFile file) {
        return ApiResponse.success("录音上传成功", recordingManagementService.uploadRecording(callId, file));
    }

    /**
     * 把录音以流式响应返回给浏览器，供详情页播放器播放。
     *
     * @param callId 通话ID。
     * @return 音频流。
     * @throws IOException 读取本地文件失败时抛出。
     */
    @GetMapping("/{callId}/recording/content")
    public ResponseEntity<?> recordingContent(@PathVariable String callId) throws IOException {
        CallRecord callRecord = callRecordRepository.findByCallId(callId)
                .orElseThrow(() -> new BusinessException("通话记录不存在: " + callId));
        String recordingUrl = callRecord.getRecordingUrl();
        if (recordingUrl == null || recordingUrl.isBlank()) {
            throw new BusinessException("当前通话暂无录音文件");
        }
        String normalizedLocation = normalizeRecordingLocation(recordingUrl);
        if (normalizedLocation.startsWith("http://") || normalizedLocation.startsWith("https://")) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(normalizedLocation))
                    .build();
        }
        Path path = normalizedLocation.startsWith("file:") ? Paths.get(URI.create(normalizedLocation)) : Paths.get(normalizedLocation);
        if (!Files.exists(path)) {
            throw new BusinessException("录音文件不存在: " + path);
        }
        Resource resource = new FileSystemResource(path);
        String contentType = Files.probeContentType(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
                .contentLength(Files.size(path))
                .contentType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType))
                .body(resource);
    }

    /**
     * 对录音地址做兼容处理，避免“缺少协议头的远程地址”在浏览器播放时被当成本地路径。
     */
    private String normalizeRecordingLocation(String recordingUrl) {
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
