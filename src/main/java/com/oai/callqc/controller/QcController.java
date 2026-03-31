package com.oai.callqc.controller;



/**
 * 源码中文讲解：质检控制器
 *
 * - 提供手动执行质检、查看质检详情、提交人工复核、分页查询质检列表等接口。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import com.oai.callqc.common.ApiResponse;
import com.oai.callqc.common.PageResponse;
import com.oai.callqc.dto.ManualReviewRequest;
import com.oai.callqc.service.BasicQcService;
import com.oai.callqc.vo.QcDetailVO;
import com.oai.callqc.vo.QcListItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/qc")
@RequiredArgsConstructor
public class QcController {

    private final BasicQcService basicQcService;

    @PostMapping("/{callId}/execute")
    /**
     * 手动执行基础质检。
     */
    
    public ApiResponse<Void> execute(@PathVariable String callId) {
        basicQcService.execute(callId);
        return ApiResponse.success("质检执行成功", null);
    }

    @GetMapping("/{callId}/detail")
    /**
     * 查询质检详情。
     */
    
    public ApiResponse<QcDetailVO> detail(@PathVariable String callId) {
        return ApiResponse.success(basicQcService.detail(callId));
    }

    @PostMapping("/{callId}/review")
    /**
     * 提交人工复核。
     */
    
    public ApiResponse<Void> review(@PathVariable String callId, @Valid @RequestBody ManualReviewRequest request) {
        basicQcService.review(callId, request);
        return ApiResponse.success("复核成功", null);
    }

    @GetMapping("/page")
    /**
     * 分页查询质检列表。
     */
    
    public ApiResponse<PageResponse<QcListItemVO>> page(@RequestParam(defaultValue = "1") int pageNo,
                                                        @RequestParam(defaultValue = "10") int pageSize,
                                                        @RequestParam(required = false) String callId,
                                                        @RequestParam(required = false) String agentName,
                                                        @RequestParam(required = false) String businessLine,
                                                        @RequestParam(required = false) String processStatus,
                                                        @RequestParam(required = false) String riskLevel,
                                                        @RequestParam(required = false) Boolean needManualReview) {
        return ApiResponse.success(basicQcService.page(pageNo, pageSize, callId, agentName, businessLine,
                processStatus, riskLevel, needManualReview));
    }
}
