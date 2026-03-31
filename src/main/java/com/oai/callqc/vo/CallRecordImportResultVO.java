package com.oai.callqc.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 表格导入结果视图对象。
 */
@Data
@Builder
public class CallRecordImportResultVO {

    /** 导入文件名称。 */
    private String fileName;

    /** 文件中读取到的总数据行数（不含表头）。 */
    private int totalRows;

    /** 成功导入的行数。 */
    private int successCount;

    /** 失败行数。 */
    private int failedCount;

    /** 成功导入的前若干个 callId，用于页面快速确认。 */
    private List<String> importedCallIds;

    /** 失败原因列表，格式类似：第 3 行：开始时间为空。 */
    private List<String> errors;
}
