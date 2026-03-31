package com.oai.callqc.handler;

import com.oai.callqc.entity.CallRecord;
import xyz.erupt.annotation.fun.OperationHandler;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Erupt 通话记录列表页批量按钮：批量提交异步转写。
 */
public class BatchSubmitAsyncTranscribeOperationHandler implements OperationHandler<CallRecord, Void> {

    @Override
    public String exec(List<CallRecord> data, Void eruptForm, String[] param) {
        if (data == null || data.isEmpty()) {
            return "alert('请至少选择一条通话记录');";
        }
        String jsonArray = data.stream()
                .filter(item -> item != null && item.getCallId() != null && !item.getCallId().isBlank())
                .map(item -> "\"" + escapeJs(item.getCallId()) + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        if ("[]".equals(jsonArray)) {
            return "alert('未获取到有效的通话ID');";
        }
        return "(function(){" +
                "fetch('/api/v1/calls/transcribe/async/batch',{method:'POST',headers:{'Content-Type':'application/json'},body:'" + jsonArray + "'})" +
                ".then(function(resp){return resp.json();})" +
                ".then(function(res){alert(res.message || '已批量提交异步转写');})" +
                ".catch(function(err){alert('批量提交异步转写失败：' + err);});" +
                "})();";
    }

    private String escapeJs(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
