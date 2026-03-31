package com.oai.callqc.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 录音 ZIP 导入结果。
 */
@Data
@Builder
public class RecordingZipImportResultVO {

    /** ZIP 文件名称。 */
    private String zipFileName;

    /** ZIP 内总条目数（包含目录和非音频文件）。 */
    private int totalEntries;

    /** 实际解压出的音频文件数量。 */
    private int extractedAudioCount;

    /** 成功匹配并回写到通话记录的数量。 */
    private int matchedCallCount;

    /** 自动加入异步转写队列的数量。 */
    private int autoQueuedCount;

    /** 未匹配到通话记录的录音文件名列表。 */
    private List<String> unmatchedFiles;

    /** 最终解压目录。 */
    private String extractedDir;
}
