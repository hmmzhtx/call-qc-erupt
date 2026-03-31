package com.oai.callqc.handler;

import com.oai.callqc.entity.CallRecord;
import xyz.erupt.annotation.fun.OperationHandler;

import java.util.List;

/**
 * Erupt 通话记录列表页行按钮：直接提交异步转写。
 */
public class SubmitAsyncTranscribeOperationHandler implements OperationHandler<CallRecord, Void> {

    /**
     * 提交异步转写任务。
     * 只弹一次结果提示，不再二次确认，也不自动刷新列表。
     */
    @Override
    public String exec(List<CallRecord> data, Void eruptForm, String[] param) {
        if (data == null || data.isEmpty() || data.get(0) == null) {
            return "alert('未获取到通话记录，无法提交异步转写');";
        }
        CallRecord record = data.get(0);
        if (record.getCallId() == null || record.getCallId().isBlank()) {
            return "alert('当前通话记录缺少 callId，无法提交异步转写');";
        }
        String callId = escapeJs(record.getCallId());
        return "(function(){" +
                "fetch('/api/v1/calls/" + callId + "/transcribe/async',{method:'POST'})" +
                ".then(function(resp){return resp.json();})" +
                ".then(function(res){alert(res.message || '已提交异步转写');})" +
                ".catch(function(err){alert('提交异步转写失败：' + err);});" +
                "})();";
    }

    private String escapeJs(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
