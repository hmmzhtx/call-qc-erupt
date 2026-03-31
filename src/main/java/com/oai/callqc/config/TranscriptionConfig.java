package com.oai.callqc.config;



/**
 * 源码中文讲解：转写客户端配置
 *
 * - 注册调用本地 ASR 服务的 RestClient。
 * - 通过 timeout/baseUrl 等配置避免把调用逻辑散落在业务代码中。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties({TranscriptionProperties.class, QcProperties.class, AsyncTaskProperties.class})
public class TranscriptionConfig {

    /**
     * 功能说明：创建本地 ASR 调用客户端，统一转写服务访问入口。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param properties 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    @Bean
    public RestClient transcriptionRestClient(TranscriptionProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));
        return RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.getBaseUrl()))
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 功能说明：清理地址结尾多余的斜杠，避免 URL 拼接错误。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param baseUrl 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    private String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://127.0.0.1:18080";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
