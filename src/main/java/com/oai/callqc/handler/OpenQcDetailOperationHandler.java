package com.oai.callqc.handler;

import com.oai.callqc.entity.CallRecord;
import xyz.erupt.annotation.fun.OperationHandler;

import java.util.List;

/**
 * 源码中文讲解：Erupt 行按钮处理器（打开质检详情页）
 *
 * - 该类挂在 CallRecord 的 @RowOperation 上。
 * - 当用户在 Erupt 的“通话记录”列表页点击“质检详情”按钮时，Erupt 会调用本类的 exec 方法。
 * - 这里不做后端数据变更，只返回一段前端 JavaScript，让浏览器直接新开页面进入质检详情工作台。
 */
public class OpenQcDetailOperationHandler implements OperationHandler<CallRecord, Void> {

    /**
     * 功能说明：处理 Erupt 列表页上的“质检详情”按钮点击事件。
     * @param data 当前选中的行数据。因为按钮配置为 SINGLE，所以这里理论上只会有一条。
     * @param eruptForm 当前按钮未绑定表单，因此这里固定为 null。
     * @param param 行按钮注解上传入的扩展参数，本项目暂未使用。
     * @return 返回给 Erupt 前端执行的 JavaScript；返回 null 表示无需执行前端脚本。
     */
    @Override
    public String exec(List<CallRecord> data, Void eruptForm, String[] param) {
        if (data == null || data.isEmpty() || data.get(0) == null) {
            return "alert('未获取到通话记录，无法打开质检详情页');";
        }
        CallRecord record = data.get(0);
        if (record.getCallId() == null || record.getCallId().isBlank()) {
            return "alert('当前通话记录缺少 callId，无法打开质检详情页');";
        }
        return "window.open('/pages/qc-detail/" + escapeJs(record.getCallId()) + "', '_blank');";
    }

    /**
     * 功能说明：对拼接到 JavaScript 字符串中的文本做最基本的转义处理。
     * @param value 原始字符串。
     * @return 适合放入单引号 JavaScript 字符串中的安全值。
     */
    private String escapeJs(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
