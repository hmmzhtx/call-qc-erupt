package com.oai.callqc.service;



/**
 * 源码中文讲解：报表服务接口
 *
 * - 定义概览报表、坐席报表和规则命中报表能力。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import com.oai.callqc.vo.AgentReportVO;
import com.oai.callqc.vo.OverviewReportVO;
import com.oai.callqc.vo.RuleHitStatVO;

import java.util.List;

public interface ReportService {
    /**
     * 功能说明：统计整体概览指标，供总览报表展示。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     */
    OverviewReportVO overview();

    /**
     * 功能说明：统计坐席维度的数据结果，供坐席报表使用。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     */
    List<AgentReportVO> agentReports();

    /**
     * 功能说明：处理规则相关的构建、匹配或统计逻辑。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     */
    List<RuleHitStatVO> ruleHitStats();
}
