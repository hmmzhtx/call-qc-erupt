package com.oai.callqc.dto;



/**
 * 源码中文讲解：本地 ASR 响应 DTO
 *
 * - 用于解析 Python/FunASR 服务返回的 JSON。
 * - 其中 segments 表示分段转写结果，后续会落库到 call_transcript 表。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

@Data
public class LocalAsrResponse {

    private Boolean success;
    private String text;
    private String message;
    private String detail;
    private List<Segment> segments;
    private Object raw;

    @Data
    public static class Segment {
        private Integer index;
        @JsonAlias({"speaker_role", "speakerRole"})
        private String speakerRole;
        private String speaker;
        @JsonAlias({"start_ms", "startMs"})
        private Long startMs;
        @JsonAlias({"end_ms", "endMs"})
        private Long endMs;
        private String text;
        private Double confidence;
    }
}
