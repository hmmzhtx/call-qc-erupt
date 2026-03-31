package com.oai.callqc.repository;



/**
 * 源码中文讲解：规则配置 Repository
 *
 * - 提供查询启用规则列表等能力。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import com.oai.callqc.entity.QcRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QcRuleRepository extends JpaRepository<QcRule, Long> {
    /**
     * 功能说明：封装当前类中的一个业务步骤，供上层流程按顺序调用。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     */
    List<QcRule> findByEnabledTrue();
    /**
     * 功能说明：处理规则相关的构建、匹配或统计逻辑。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param ruleCode 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    Optional<QcRule> findByRuleCode(String ruleCode);
}
