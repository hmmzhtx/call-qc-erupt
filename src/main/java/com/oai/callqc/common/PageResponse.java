package com.oai.callqc.common;



/**
 * 源码中文讲解：分页返回结构
 *
 * - 用于列表页返回 total/pageNo/pageSize/records 结构。
 * - 质检列表分页接口会直接使用该对象。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private long total;
    private int pageNo;
    private int pageSize;
    private List<T> records;

    /**
     * 功能说明：把外部对象转换为系统统一使用的数据结构。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param page 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @return 当前方法处理后返回的业务结果对象。
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .total(page.getTotalElements())
                .pageNo(page.getNumber() + 1)
                .pageSize(page.getSize())
                .records(page.getContent())
                .build();
    }
}
