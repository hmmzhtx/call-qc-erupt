package com.oai.callqc.handler;

import com.oai.callqc.entity.CallRecord;
import xyz.erupt.annotation.fun.OperationHandler;

import java.util.List;

/**
 * 打开增强版通话记录列表页。
 *
 * <p>由于 Erupt 原生表格在单页 500 条场景下，底部横向滚动条会落在内部滚动容器底部，
 * 用户经常需要手工滚到最底部才能看到拖动条。这个按钮用于打开自定义的增强列表页，
 * 页面会在顶部固定提供一条同步横向滚动条，并在操作列中提供“异步转写”等快捷操作。</p>
 */
public class OpenCallRecordListPageOperationHandler implements OperationHandler<CallRecord, Void> {

    /**
     * 打开增强版通话记录列表页。
     */
    @Override
    public String exec(List<CallRecord> data, Void eruptForm, String[] param) {
        return "window.open('/pages/call-record-list', '_blank');";
    }
}
