package com.oai.callqc.handler;

import com.oai.callqc.entity.CallRecord;
import xyz.erupt.annotation.fun.OperationHandler;

import java.util.List;

/**
 * Erupt 列表顶部按钮处理器：打开“通话记录表格导入页”。
 *
 * <p>该按钮挂在 CallRecord 的 BUTTON 级操作上，不依赖具体行数据，点击后直接打开独立导入页面。</p>
 */
public class OpenCallImportPageOperationHandler implements OperationHandler<CallRecord, Void> {

    /**
     * 打开导入页。
     *
     * @param data      BUTTON 模式下通常为空列表，这里不使用。
     * @param eruptForm 当前按钮未绑定表单，因此固定为 null。
     * @param param     扩展参数，本项目未使用。
     * @return 返回给 Erupt 前端执行的 JavaScript 代码。
     */
    @Override
    public String exec(List<CallRecord> data, Void eruptForm, String[] param) {
        return "window.open('/pages/call-record-import', '_blank');";
    }
}
