package com.oai.callqc.config;



/**
 * 源码中文讲解：质检配置属性
 *
 * - 映射 qc.basic.* 等质检相关配置。
 * - 当前主要用于控制“转写后是否自动执行基础质检”等行为。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "qc")
public class QcProperties {

    /**
     * 转写结果落库后自动触发基础质检
     */
    private boolean autoExecuteAfterTranscription = true;
}
