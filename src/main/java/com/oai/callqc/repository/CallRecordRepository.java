package com.oai.callqc.repository;

import com.oai.callqc.entity.CallRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 通话主表 Repository。
 */
public interface CallRecordRepository extends JpaRepository<CallRecord, Long>, JpaSpecificationExecutor<CallRecord> {

    /**
     * 按通话 ID 查询主表。
     */
    Optional<CallRecord> findByCallId(String callId);

    /**
     * 按录音文件名查询主表。
     *
     * <p>录音 ZIP 导入后，会优先按 recordingFileName 自动匹配通话记录。</p>
     */
    Optional<CallRecord> findFirstByRecordingFileName(String recordingFileName);

    /**
     * 按多个通话ID批量查询主表。
     */
    List<CallRecord> findByCallIdIn(List<String> callIds);
}
