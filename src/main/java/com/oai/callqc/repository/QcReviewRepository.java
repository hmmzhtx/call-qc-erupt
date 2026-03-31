package com.oai.callqc.repository;



/**
 * 源码中文讲解：复核记录 Repository
 *
 * - 提供按通话查询复核历史的能力。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import com.oai.callqc.entity.QcReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QcReviewRepository extends JpaRepository<QcReview, Long> {
    /**
     * 功能说明：处理人工复核结果，并回写最终质检状态。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param callId 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    List<QcReview> findByCallIdOrderByReviewedAtDesc(String callId);
}
