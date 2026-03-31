package com.oai.callqc.common;



/**
 * 源码中文讲解：统一接口响应体
 *
 * - 所有 REST 接口优先返回该结构，便于前端统一处理 code、message、data。
 * - success 静态方法用于快速构建成功返回。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private Integer code;
    private String message;
    private T data;

    /**
     * 功能说明：快速构造成功响应对象，统一接口返回格式。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param data 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @return 当前方法处理后返回的业务结果对象。
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().code(0).message("success").data(data).build();
    }

    /**
     * 功能说明：快速构造成功响应对象，统一接口返回格式。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param message 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param data 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @return 当前方法处理后返回的业务结果对象。
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder().code(0).message(message).data(data).build();
    }

    /**
     * 功能说明：快速构造失败响应对象，统一接口返回格式。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param code 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @param message 方法输入参数，具体含义请结合调用场景和字段命名理解。
     * @return 当前方法处理后返回的业务结果对象。
     */
    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return ApiResponse.<T>builder().code(code).message(message).build();
    }
}
