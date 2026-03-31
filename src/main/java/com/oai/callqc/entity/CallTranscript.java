package com.oai.callqc.entity;



/**
 * 源码中文讲解：转写分段实体
 *
 * - 对应 qc_call_transcript 表，保存每个分段的文本、时间轴和说话人。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;

@Getter
@Setter
@Entity
@Table(name = "qc_call_transcript")
@Erupt(name = "转写分段")
public class CallTranscript extends BaseModel {

    @EruptField(views = @View(title = "通话ID"), edit = @Edit(title = "通话ID", notNull = true, search = @Search))
    @Column(name = "call_id", nullable = false, length = 64)
    private String callId;

    @EruptField(views = @View(title = "分段序号"), edit = @Edit(title = "分段序号"))
    @Column(name = "segment_index")
    private Integer segmentIndex;

    @EruptField(views = @View(title = "说话人"), edit = @Edit(title = "说话人", search = @Search))
    @Column(name = "speaker_role", length = 16)
    private String speakerRole;

    @EruptField(views = @View(title = "开始毫秒"), edit = @Edit(title = "开始毫秒"))
    @Column(name = "start_ms")
    private Long startMs;

    @EruptField(views = @View(title = "结束毫秒"), edit = @Edit(title = "结束毫秒"))
    @Column(name = "end_ms")
    private Long endMs;

    @Lob
    @EruptField(views = @View(title = "转写文本"), edit = @Edit(title = "转写文本"))
    @Column(name = "transcript_text", columnDefinition = "LONGTEXT")
    private String transcriptText;

    @EruptField(views = @View(title = "置信度"), edit = @Edit(title = "置信度"))
    @Column(name = "confidence")
    private Double confidence;
}
