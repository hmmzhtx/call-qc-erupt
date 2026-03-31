package com.oai.callqc.controller;



/**
 * 源码中文讲解：报表控制器
 *
 * - 对外提供概览报表、坐席统计、规则命中统计等基础报表接口。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import com.oai.callqc.common.ApiResponse;
import com.oai.callqc.service.ReportService;
import com.oai.callqc.vo.AgentReportVO;
import com.oai.callqc.vo.OverviewReportVO;
import com.oai.callqc.vo.RuleHitStatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 功能说明：统计整体概览指标，供总览报表展示。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     */
    @GetMapping("/overview")
    public ApiResponse<OverviewReportVO> overview() {
        return ApiResponse.success(reportService.overview());
    }

    /**
     * 功能说明：统计坐席维度的数据结果，供坐席报表使用。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     */
    @GetMapping("/agents")
    public ApiResponse<List<AgentReportVO>> agents() {
        return ApiResponse.success(reportService.agentReports());
    }

    /**
     * 功能说明：处理规则相关的构建、匹配或统计逻辑。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     */
    @GetMapping("/rules")
    public ApiResponse<List<RuleHitStatVO>> rules() {
        return ApiResponse.success(reportService.ruleHitStats());
    }
}
