package com.oai.callqc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通话导入请求 DTO。
 *
 * <p>该对象既服务于单条导入接口，也服务于“Excel / CSV 批量导入”功能。</p>
 *
 * <p>为了兼容用户现有的基础台账，本次补充了和 Excel 列结构一致的字段：</p>
 * <ul>
 *     <li>callerNumber：主叫号码</li>
 *     <li>projectName：项目名称</li>
 *     <li>taskName：任务名称</li>
 *     <li>customerName：客户姓名</li>
 *     <li>customerStatus：客户状态</li>
 *     <li>recordingFileName：录音文件名</li>
 * </ul>
 */
@Data
public class CallImportRequest {

    /**
     * 通话唯一标识。
     *
     * <p>如果外部系统没有提供，服务层会自动根据“坐席工号_客户号码_开始时间”生成，
     * 例如：20260001_13543010534_20260326170407。</p>
     */
    private String callId;

    /** 主叫号码。 */
    private String callerNumber;

    /** 坐席工号。 */
    private String agentId;

    /** 坐席姓名。 */
    private String agentName;

    /** 客户内部 ID，可选。 */
    private String customerId;

    /** 客户号码。 */
    private String customerPhone;

    /** 客户姓名。 */
    private String customerName;

    /** 客户状态。 */
    private String customerStatus;

    /** 项目名称。 */
    private String projectName;

    /** 任务名称。 */
    private String taskName;

    /** 业务线。没有单独传入时，可由项目名称兜底。 */
    private String businessLine;

    /** 技能组。没有单独传入时，可由任务名称兜底。 */
    private String skillGroup;

    /** 通话类型，如 INBOUND / OUTBOUND。 */
    private String callType;

    /** 开始时间。 */
    private LocalDateTime startTime;

    /** 结束时间。 */
    private LocalDateTime endTime;

    /** 通话时长，单位秒。 */
    private Integer durationSeconds;

    /** 录音文件名，例如：20260001_13543010534_20260326170407.wav。 */
    private String recordingFileName;

    /** 录音地址。 */
    private String recordingUrl;

    /**
     * 导入完成后是否自动进入异步转写队列。
     *
     * <p>单条导入接口默认可为 true；批量表格导入一般建议为 false，避免一次性触发大量转写任务。</p>
     */
    private Boolean autoSubmitAsync = Boolean.TRUE;
}
