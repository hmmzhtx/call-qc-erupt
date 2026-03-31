package com.oai.callqc.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oai.callqc.common.BusinessException;
import com.oai.callqc.service.BasicQcService;
import com.oai.callqc.vo.QcDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 页面路由控制器。
 *
 * <p>负责返回 Thymeleaf 页面模板入口：</p>
 * <ul>
 *     <li>/pages/qc-detail/{callId}：质检详情工作台</li>
 *     <li>/pages/call-record-import：通话记录表格导入页</li>
 * </ul>
 */
@Controller
@RequiredArgsConstructor
public class QcPageController {

    private final BasicQcService basicQcService;
    private final ObjectMapper objectMapper;

    /**
     * 项目首页。
     *
     * <p>由于当前项目希望“免登录直接进入业务页面”，因此这里不再把根路径交给 Erupt 登录页，
     * 而是直接重定向到自定义的通话记录与录音导入页面。</p>
     *
     * @return 重定向到业务首页。
     */
    @GetMapping({"/", "/index", "/index.html"})
    public String index() {
        return "redirect:/pages/call-record-list";
    }

    /**
     * 打开质检详情页。
     *
     * @param callId 通话ID。
     * @param model  页面模型。
     * @return 模板名称。
     */
    @GetMapping("/pages/qc-detail/{callId}")
    public String detailPage(@PathVariable String callId, Model model) {
        QcDetailVO detail = basicQcService.detail(callId);
        model.addAttribute("detail", detail);
        try {
            model.addAttribute("detailJson", objectMapper.writeValueAsString(detail));
        } catch (JsonProcessingException e) {
            throw new BusinessException("质检详情页数据序列化失败: " + e.getMessage());
        }
        return "qc-detail";
    }


    /**
     * 打开增强版通话记录列表页。
     *
     * <p>该页面使用自定义表格布局，支持：
     * 单页 500 条时依然能在顶部看到横向滚动条，
     * 并可直接在操作列中触发“异步转写”和“质检详情”。</p>
     *
     * @return 模板名称。
     */
    @GetMapping("/pages/call-record-list")
    public String callRecordListPage() {
        return "call-record-list";
    }

    /**
     * 打开通话记录表格导入页。
     *
     * @return 模板名称。
     */
    @GetMapping("/pages/call-record-import")
    public String callRecordImportPage() {
        return "call-record-import";
    }
}
