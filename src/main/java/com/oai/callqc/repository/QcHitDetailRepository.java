package com.oai.callqc.repository;



/**
 * 源码中文讲解：命中明细 Repository
 *
 * - 提供按通话查询命中明细的能力。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import com.oai.callqc.entity.QcHitDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QcHitDetailRepository extends JpaRepository<QcHitDetail, Long> {
    /**
     * 功能说明：封装当前类中的一个业务步骤，供上层流程按顺序调用。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param callId 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    List<QcHitDetail> findByCallIdOrderByIdAsc(String callId);
    /**
     * 功能说明：封装当前类中的一个业务步骤，供上层流程按顺序调用。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param callId 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    void deleteByCallId(String callId);
}
