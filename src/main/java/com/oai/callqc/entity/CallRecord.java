package com.oai.callqc.entity;

import com.oai.callqc.handler.OpenCallImportPageOperationHandler;
import com.oai.callqc.handler.OpenQcDetailOperationHandler;
import com.oai.callqc.handler.OpenCallRecordListPageOperationHandler;
import com.oai.callqc.handler.SubmitAsyncTranscribeOperationHandler;
import com.oai.callqc.handler.BatchSubmitAsyncTranscribeOperationHandler;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;

import java.time.LocalDateTime;

/**
 * 通话主表实体。
 *
 * <p>这张表是整个质检系统的主线表：一条记录对应一通电话，后续的转写、质检、复核、报表都围绕它展开。</p>
 *
 * <p>本次针对用户提供的“初始录音记录表”进行了字段补齐，新增了：</p>
 * <ul>
 *     <li>callerNumber：主叫号码</li>
 *     <li>projectName：项目名称</li>
 *     <li>taskName：任务名称</li>
 *     <li>customerName：客户姓名</li>
 *     <li>customerStatus：客户状态</li>
 *     <li>recordingFileName：按“坐席工号_客户号码_时间戳.wav”生成或记录的录音文件名</li>
 * </ul>
 *
 * <p>这样可以和用户现有的 Excel / CSV 基础台账保持一致，也为后续按录音文件名自动匹配通话记录打基础。</p>
 */
@Getter
@Setter
@Entity
@Table(name = "qc_call_record")
@Erupt(
        name = "通话记录",
        desc = "呼叫系统录音主表，支持后台列表查看、表格导入、打开质检详情页",
        power = @Power(export = true),
        rowOperation = {
                @RowOperation(
                        title = "导入表格",
                        code = "openImportPage",
                        icon = "fa fa-upload",
                        tip = "打开通话记录表格导入页面，支持 Excel / CSV 导入",
                        callHint = "",
                        mode = RowOperation.Mode.BUTTON,
                        operationHandler = OpenCallImportPageOperationHandler.class
                ),
                @RowOperation(
                        title = "增强列表",
                        code = "openCallRecordListPage",
                        icon = "fa fa-table",
                        tip = "打开增强版通话记录列表页，适合单页 500 条时查看横向拖动条",
                        callHint = "",
                        mode = RowOperation.Mode.BUTTON,
                        operationHandler = OpenCallRecordListPageOperationHandler.class
                ),
                @RowOperation(
                        title = "异步转写",
                        code = "submitAsyncTranscribe",
                        icon = "fa fa-play-circle",
                        tip = "直接提交该通话的异步转写任务",
                        callHint = "",
                        mode = RowOperation.Mode.SINGLE,
                        operationHandler = SubmitAsyncTranscribeOperationHandler.class
                ),
                @RowOperation(
                        title = "批量异步转写",
                        code = "batchSubmitAsyncTranscribe",
                        icon = "fa fa-tasks",
                        tip = "对已选择的多条通话记录直接提交异步转写任务",
                        callHint = "",
                        mode = RowOperation.Mode.MULTI_ONLY,
                        operationHandler = BatchSubmitAsyncTranscribeOperationHandler.class
                ),
                @RowOperation(
                        title = "质检详情",
                        code = "openQcDetail",
                        icon = "fa fa-external-link",
                        tip = "在新标签页打开该通话的质检详情工作台",
                        callHint = "",
                        mode = RowOperation.Mode.SINGLE,
                        operationHandler = OpenQcDetailOperationHandler.class
                )
        }
)
public class CallRecord extends BaseModel {

    @EruptField(views = @View(title = "通话ID", width = "180px"), edit = @Edit(title = "通话ID", notNull = true, search = @Search), sort = 10)
    @Column(name = "call_id", nullable = false, unique = true, length = 64)
    private String callId;

    @EruptField(views = @View(title = "主叫号码", width = "130px"), edit = @Edit(title = "主叫号码", search = @Search), sort = 15)
    @Column(name = "caller_number", length = 32)
    private String callerNumber;

    @EruptField(views = @View(title = "客户号码", width = "130px"), edit = @Edit(title = "客户号码", search = @Search), sort = 20)
    @Column(name = "customer_phone", length = 32)
    private String customerPhone;

    @EruptField(views = @View(title = "客户姓名", width = "120px"), edit = @Edit(title = "客户姓名", search = @Search), sort = 25)
    @Column(name = "customer_name", length = 64)
    private String customerName;

    @EruptField(views = @View(title = "客户状态", width = "120px"), edit = @Edit(title = "客户状态", search = @Search), sort = 26)
    @Column(name = "customer_status", length = 64)
    private String customerStatus;

    @EruptField(views = @View(title = "坐席姓名", width = "120px"), edit = @Edit(title = "坐席姓名", search = @Search), sort = 30)
    @Column(name = "agent_name", length = 64)
    private String agentName;

    @EruptField(views = @View(title = "坐席工号", width = "130px"), edit = @Edit(title = "坐席工号", search = @Search), sort = 40)
    @Column(name = "agent_id", length = 64)
    private String agentId;

    @EruptField(views = @View(title = "项目名称", width = "140px"), edit = @Edit(title = "项目名称", search = @Search), sort = 45)
    @Column(name = "project_name", length = 128)
    private String projectName;

    @EruptField(views = @View(title = "任务名称", width = "140px"), edit = @Edit(title = "任务名称", search = @Search), sort = 46)
    @Column(name = "task_name", length = 128)
    private String taskName;

    @EruptField(views = @View(title = "业务线", width = "120px"), edit = @Edit(title = "业务线", search = @Search), sort = 50)
    @Column(name = "business_line", length = 64)
    private String businessLine;

    @EruptField(views = @View(title = "技能组", width = "120px"), edit = @Edit(title = "技能组", search = @Search), sort = 55)
    @Column(name = "skill_group", length = 64)
    private String skillGroup;

    @EruptField(views = @View(title = "通话类型", width = "110px"), edit = @Edit(title = "通话类型", search = @Search), sort = 60)
    @Column(name = "call_type", length = 16)
    private String callType;

    @EruptField(views = @View(title = "开始时间", width = "160px", sortable = true), edit = @Edit(title = "开始时间"), sort = 70)
    @Column(name = "start_time")
    private LocalDateTime startTime;

    @EruptField(views = @View(title = "结束时间", width = "160px"), edit = @Edit(title = "结束时间"), sort = 80)
    @Column(name = "end_time")
    private LocalDateTime endTime;

    @EruptField(views = @View(title = "通话时长(秒)", width = "120px", sortable = true), edit = @Edit(title = "通话时长(秒)"), sort = 90)
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @EruptField(views = @View(title = "录音文件名", width = "220px", className = "qc-col-nowrap"), edit = @Edit(title = "录音文件名", search = @Search), sort = 95)
    @Column(name = "recording_file_name", length = 255)
    private String recordingFileName;

    @EruptField(views = @View(title = "录音地址", width = "280px", className = "qc-col-nowrap"), edit = @Edit(title = "录音地址"), sort = 100)
    @Column(name = "recording_url", length = 500)
    private String recordingUrl;

    @EruptField(views = @View(title = "客户ID", width = "110px"), edit = @Edit(title = "客户ID"), sort = 105)
    @Column(name = "customer_id", length = 64)
    private String customerId;

    @EruptField(views = @View(title = "处理状态", width = "130px"), edit = @Edit(title = "处理状态", search = @Search), sort = 110)
    @Column(name = "process_status", length = 32)
    private String processStatus;

    @EruptField(views = @View(title = "处理说明", width = "260px", className = "qc-col-nowrap"), edit = @Edit(title = "处理说明"), sort = 120)
    @Column(name = "process_message", length = 500)
    private String processMessage;

    @EruptField(views = @View(title = "最近处理时间", width = "160px", sortable = true), edit = @Edit(title = "最近处理时间"), sort = 130)
    @Column(name = "last_process_time")
    private LocalDateTime lastProcessTime;

    @EruptField(views = @View(title = "转写分段数", width = "120px", sortable = true), edit = @Edit(title = "转写分段数"), sort = 140)
    @Column(name = "transcript_segment_count")
    private Integer transcriptSegmentCount;
}
