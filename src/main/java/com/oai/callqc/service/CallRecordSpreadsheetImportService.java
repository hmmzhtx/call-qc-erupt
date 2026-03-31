package com.oai.callqc.service;

import com.oai.callqc.vo.CallRecordImportResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通话记录表格导入服务。
 */
public interface CallRecordSpreadsheetImportService {

    /**
     * 把 Excel / CSV 表格导入为通话主表数据。
     *
     * @param file 用户上传的 Excel / CSV 文件。
     * @return 导入结果统计。
     */
    CallRecordImportResultVO importSpreadsheet(MultipartFile file);

    /**
     * 生成导入模板文件内容。
     *
     * @return UTF-8 编码的 CSV 模板字节数组。
     */
    byte[] buildTemplateBytes();
}
