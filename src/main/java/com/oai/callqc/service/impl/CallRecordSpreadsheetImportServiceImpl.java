package com.oai.callqc.service.impl;

import com.oai.callqc.common.BusinessException;
import com.oai.callqc.dto.CallImportRequest;
import com.oai.callqc.entity.CallRecord;
import com.oai.callqc.service.CallImportService;
import com.oai.callqc.service.CallRecordSpreadsheetImportService;
import com.oai.callqc.vo.CallRecordImportResultVO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 通话记录表格导入服务实现。
 *
 * <p>支持两类文件：</p>
 * <ul>
 *     <li>.xlsx / .xls：Excel 表格</li>
 *     <li>.csv：逗号分隔文本</li>
 * </ul>
 *
 * <p>支持的表头和用户截图一致：</p>
 * <pre>
 * 主叫号码, 客户号码, 坐席姓名, 坐席工号, 开始时间, 通话时长,
 * 项目名称, 任务名称, 客户姓名, 客户状态, 录音地址
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class CallRecordSpreadsheetImportServiceImpl implements CallRecordSpreadsheetImportService {

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm")
    );

    private static final String[] TEMPLATE_HEADERS = {
            "主叫号码", "客户号码", "坐席姓名", "坐席工号", "开始时间", "通话时长",
            "项目名称", "任务名称", "客户姓名", "客户状态", "录音地址"
    };

    private final CallImportService callImportService;

    /**
     * 导入 Excel / CSV 文件。
     *
     * @param file 用户上传的文件。
     * @return 导入结果。
     */
    @Override
    public CallRecordImportResultVO importSpreadsheet(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        List<Map<String, String>> rows = readRows(file, originalFilename);

        List<String> importedCallIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int totalRows = rows.size();

        for (int i = 0; i < rows.size(); i++) {
            int displayRowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            if (isBlankRow(row)) {
                continue;
            }
            try {
                CallImportRequest request = convertRow(row, displayRowNumber);
                request.setAutoSubmitAsync(Boolean.FALSE);
                CallRecord saved = callImportService.importCall(request);
                importedCallIds.add(saved.getCallId());
                successCount++;
            } catch (Exception ex) {
                errors.add("第 " + displayRowNumber + " 行：" + ex.getMessage());
            }
        }

        return CallRecordImportResultVO.builder()
                .fileName(originalFilename)
                .totalRows(totalRows)
                .successCount(successCount)
                .failedCount(errors.size())
                .importedCallIds(importedCallIds.size() > 20 ? importedCallIds.subList(0, 20) : importedCallIds)
                .errors(errors)
                .build();
    }

    /**
     * 生成 CSV 导入模板。
     *
     * @return UTF-8 编码的 CSV 内容。
     */
    @Override
    public byte[] buildTemplateBytes() {
        StringBuilder builder = new StringBuilder();
        builder.append("\uFEFF");
        builder.append(String.join(",", TEMPLATE_HEADERS)).append("\n");
        builder.append("202603241,13543010534,俊才,20260001,2026-03-26 17:04:07,22,T1-1007,T1-1007,张三,失败客户,https://example.com/common/record?fileId=demo").append("\n");
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 根据文件扩展名读取数据行。
     *
     * @param file             上传文件。
     * @param originalFilename 原始文件名。
     * @return 数据行列表，每一行是“表头 -> 单元格值”的映射。
     */
    private List<Map<String, String>> readRows(MultipartFile file, String originalFilename) {
        String lower = originalFilename.toLowerCase(Locale.ROOT);
        try {
            if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
                return readExcelRows(file.getInputStream());
            }
            if (lower.endsWith(".csv")) {
                return readCsvRows(file.getInputStream());
            }
        } catch (IOException e) {
            throw new BusinessException("读取导入文件失败：" + e.getMessage());
        }
        throw new BusinessException("仅支持 .xlsx、.xls、.csv 文件导入");
    }

    /**
     * 读取 Excel 数据。
     *
     * @param inputStream Excel 输入流。
     * @return 数据行列表。
     */
    private List<Map<String, String>> readExcelRows(InputStream inputStream) {
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new BusinessException("Excel 中没有可读取的数据");
            }
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new BusinessException("Excel 缺少表头行");
            }
            List<String> headers = readHeaders(headerRow, formatter);
            List<Map<String, String>> rows = new ArrayList<>();
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c);
                    rowMap.put(headers.get(c), readCellValue(cell, formatter));
                }
                rows.add(rowMap);
            }
            return rows;
        } catch (Exception e) {
            throw new BusinessException("解析 Excel 失败：" + e.getMessage());
        }
    }

    /**
     * 读取 CSV 数据。
     *
     * @param inputStream CSV 输入流。
     * @return 数据行列表。
     */
    private List<Map<String, String>> readCsvRows(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new BusinessException("CSV 文件为空");
            }
            headerLine = removeBom(headerLine);
            List<String> headers = Arrays.stream(headerLine.split(",", -1))
                    .map(String::trim)
                    .toList();
            List<Map<String, String>> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",", -1);
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    rowMap.put(headers.get(i), i < values.length ? values[i].trim() : "");
                }
                rows.add(rowMap);
            }
            return rows;
        } catch (IOException e) {
            throw new BusinessException("解析 CSV 失败：" + e.getMessage());
        }
    }

    /**
     * 读取 Excel 表头。
     *
     * @param headerRow  表头行。
     * @param formatter  Excel 文本格式化器。
     * @return 表头列表。
     */
    private List<String> readHeaders(Row headerRow, DataFormatter formatter) {
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            headers.add(readCellValue(headerRow.getCell(i), formatter));
        }
        return headers;
    }

    /**
     * 读取单元格文本，尽量保留用户在 Excel 中看到的展示值。
     *
     * @param cell       单元格。
     * @param formatter  格式化器。
     * @return 文本值。
     */
    private String readCellValue(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return formatter.formatCellValue(cell).trim();
        }
        return formatter.formatCellValue(cell).trim();
    }

    /**
     * 把单行表格数据转换为通话导入请求。
     *
     * @param row              当前行的“表头 -> 值”映射。
     * @param displayRowNumber 页面显示的 Excel 行号，用于报错提示。
     * @return 导入请求对象。
     */
    private CallImportRequest convertRow(Map<String, String> row, int displayRowNumber) {
        CallImportRequest request = new CallImportRequest();
        request.setCallerNumber(getFirstValue(row, "主叫号码", "主叫", "主叫号"));
        request.setCustomerPhone(getFirstValue(row, "客户号码", "客户手机号", "客户电话", "被叫号码"));
        request.setAgentName(getFirstValue(row, "坐席姓名", "客服姓名", "员工姓名"));
        request.setAgentId(getFirstValue(row, "坐席工号", "工号", "坐席ID"));
        request.setProjectName(getFirstValue(row, "项目名称", "项目"));
        request.setTaskName(getFirstValue(row, "任务名称", "任务"));
        request.setCustomerName(getFirstValue(row, "客户姓名", "客户名"));
        request.setCustomerStatus(getFirstValue(row, "客户状态", "状态"));
        request.setRecordingUrl(getFirstValue(row, "录音地址", "录音URL", "录音链接"));
        request.setStartTime(parseDateTime(getFirstValue(row, "开始时间", "通话开始时间"), displayRowNumber));
        request.setDurationSeconds(parseInteger(getFirstValue(row, "通话时长", "通话时长(秒)", "时长"), displayRowNumber));

        if (!StringUtils.hasText(request.getAgentId())) {
            throw new BusinessException("坐席工号不能为空");
        }
        if (!StringUtils.hasText(request.getCustomerPhone())) {
            throw new BusinessException("客户号码不能为空");
        }
        if (request.getStartTime() == null) {
            throw new BusinessException("开始时间不能为空");
        }

        request.setCallId(buildStableCallId(request));
        request.setRecordingFileName(buildStableRecordingFileName(request));
        return request;
    }

    /**
     * 生成稳定的 callId，使同一条通话重复导入时能够走更新而不是重复插入。
     *
     * @param request 通话导入请求。
     * @return 稳定的 callId。
     */
    private String buildStableCallId(CallImportRequest request) {
        return sanitize(request.getAgentId())
                + "_"
                + sanitize(request.getCustomerPhone())
                + "_"
                + request.getStartTime().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * 按用户给定的录音命名规范生成录音文件名。
     *
     * @param request 通话导入请求。
     * @return 文件名，例如：20260001_13543010534_20260326170407.wav。
     */
    private String buildStableRecordingFileName(CallImportRequest request) {
        return buildStableCallId(request) + ".wav";
    }

    /**
     * 从一行数据里按候选表头依次取值。
     *
     * @param row     当前行。
     * @param aliases 候选表头。
     * @return 第一个非空值。
     */
    private String getFirstValue(Map<String, String> row, String... aliases) {
        for (String alias : aliases) {
            String value = row.get(alias);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * 解析日期时间字符串。
     *
     * @param value            表格中的日期时间文本。
     * @param displayRowNumber Excel 行号，用于报错。
     * @return 解析后的 LocalDateTime。
     */
    private LocalDateTime parseDateTime(String value, int displayRowNumber) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignore) {
                // 尝试下一种格式
            }
        }
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (DateTimeParseException ignore) {
            // ignore
        }
        throw new BusinessException("开始时间格式无法识别：" + value + "（行号：" + displayRowNumber + "）");
    }

    /**
     * 解析整数。
     *
     * @param value            表格中的文本。
     * @param displayRowNumber 行号，用于报错。
     * @return 整数值，允许为空。
     */
    private Integer parseInteger(String value, int displayRowNumber) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim().replace(".0", ""));
        } catch (NumberFormatException ex) {
            throw new BusinessException("通话时长不是有效数字：" + value + "（行号：" + displayRowNumber + "）");
        }
    }

    /**
     * 判断一行是否为空白行。
     *
     * @param row 当前行。
     * @return true 表示该行没有有效内容。
     */
    private boolean isBlankRow(Map<String, String> row) {
        return row.values().stream().noneMatch(StringUtils::hasText);
    }

    /**
     * 移除 UTF-8 BOM。
     *
     * @param value 原始文本。
     * @return 移除 BOM 后的文本。
     */
    private String removeBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    /**
     * 清洗主键或文件名中的特殊字符。
     *
     * @param value 原始值。
     * @return 清洗后的字符串。
     */
    private String sanitize(String value) {
        return value == null ? "" : value.trim().replaceAll("[^0-9A-Za-z_-]", "");
    }
}
