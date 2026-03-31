package com.oai.callqc.controller;

import com.oai.callqc.common.ApiResponse;
import com.oai.callqc.common.PageResponse;
import com.oai.callqc.entity.CallRecord;
import com.oai.callqc.repository.CallRecordRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 增强版通话记录列表控制器。
 *
 * <p>这个控制器为自定义“增强列表页”提供分页数据，主要解决两个问题：</p>
 * <ul>
 *     <li>原生 Erupt 表格在单页 500 条场景下，横向滚动条位于内部容器底部，不容易第一时间看到</li>
 *     <li>业务同学希望在操作列里直接触发“异步转写”，减少跳转次数</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/call-records")
@RequiredArgsConstructor
public class CallRecordListController {

    private final CallRecordRepository callRecordRepository;

    /**
     * 查询增强版通话记录分页列表。
     */
    @GetMapping("/page")
    public ApiResponse<PageResponse<CallRecord>> page(@RequestParam(defaultValue = "1") int pageNo,
                                                      @RequestParam(defaultValue = "50") int pageSize,
                                                      @RequestParam(required = false) String callId,
                                                      @RequestParam(required = false) String callerNumber,
                                                      @RequestParam(required = false) String customerPhone,
                                                      @RequestParam(required = false) String customerName,
                                                      @RequestParam(required = false) String agentName,
                                                      @RequestParam(required = false) String agentId,
                                                      @RequestParam(required = false) String businessLine,
                                                      @RequestParam(required = false) String projectName,
                                                      @RequestParam(required = false) String taskName,
                                                      @RequestParam(required = false) String processStatus) {
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.min(Math.max(pageSize, 10), 500);
        Pageable pageable = PageRequest.of(safePageNo - 1, safePageSize,
                Sort.by(Sort.Direction.DESC, "startTime", "id"));

        Page<CallRecord> page = callRecordRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            addLike(predicates, cb, root.get("callId"), callId);
            addLike(predicates, cb, root.get("callerNumber"), callerNumber);
            addLike(predicates, cb, root.get("customerPhone"), customerPhone);
            addLike(predicates, cb, root.get("customerName"), customerName);
            addLike(predicates, cb, root.get("agentName"), agentName);
            addLike(predicates, cb, root.get("agentId"), agentId);
            addLike(predicates, cb, root.get("businessLine"), businessLine);
            addLike(predicates, cb, root.get("projectName"), projectName);
            addLike(predicates, cb, root.get("taskName"), taskName);
            addLike(predicates, cb, root.get("processStatus"), processStatus);
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        return ApiResponse.success(PageResponse.from(page));
    }

    /**
     * 为可选字符串参数追加模糊查询条件。
     */
    private void addLike(List<Predicate> predicates,
                         jakarta.persistence.criteria.CriteriaBuilder cb,
                         jakarta.persistence.criteria.Path<String> path,
                         String value) {
        if (StringUtils.hasText(value)) {
            predicates.add(cb.like(path, "%" + value.trim() + "%"));
        }
    }
}
