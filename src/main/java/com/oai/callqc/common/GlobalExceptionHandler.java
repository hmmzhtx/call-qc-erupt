package com.oai.callqc.common;



/**
 * 源码中文讲解：全局异常处理器
 *
 * - 集中拦截控制器抛出的异常，避免接口直接返回堆栈。
 * - 把业务异常和系统异常转换成统一的失败响应，方便前端展示。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 功能说明：统一处理异常或回调结果，并返回标准化响应。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param ex 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        return ApiResponse.fail(4001, ex.getMessage());
    }

    /**
     * 功能说明：统一处理异常或回调结果，并返回标准化响应。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param ex 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "参数校验失败";
        return ApiResponse.fail(4000, msg);
    }

    /**
     * 功能说明：统一处理异常或回调结果，并返回标准化响应。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param ex 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraint(ConstraintViolationException ex) {
        return ApiResponse.fail(4000, ex.getMessage());
    }

    /**
     * 功能说明：统一处理异常或回调结果，并返回标准化响应。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param ex 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception ex) {
        return ApiResponse.fail(5000, ex.getMessage());
    }
}
