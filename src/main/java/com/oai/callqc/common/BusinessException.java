package com.oai.callqc.common;



/**
 * 源码中文讲解：业务异常类
 *
 * - 用于抛出“业务可预期错误”，如通话不存在、录音为空、质检结果缺失等。
 * - 这类异常最终会被全局异常处理器转换为统一 JSON 响应。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
public class BusinessException extends RuntimeException {

    /**
     * 功能说明：封装当前类中的一个业务步骤，供上层流程按顺序调用。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param message 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    public BusinessException(String message) {
        super(message);
    }
}
