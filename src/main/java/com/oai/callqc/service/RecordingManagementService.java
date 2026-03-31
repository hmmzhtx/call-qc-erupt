package com.oai.callqc.service;

import com.oai.callqc.vo.AsyncTaskSubmitVO;
import com.oai.callqc.vo.RecordingZipImportResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 录音管理服务接口。
 */
public interface RecordingManagementService {

    /**
     * 上传单个录音文件并回写到指定通话记录。
     */
    AsyncTaskSubmitVO uploadRecording(String callId, MultipartFile file);

    /**
     * 上传录音 ZIP 文件，自动解压到 temp/audio，并按录音文件名匹配通话记录。
     */
    RecordingZipImportResultVO importRecordingZip(MultipartFile file);
}
