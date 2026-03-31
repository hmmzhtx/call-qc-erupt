package com.oai.callqc.config;



/**
 * 源码中文讲解：转写配置属性
 *
 * - 映射 qc.transcription.* 配置，例如本地 ASR 地址、接口路径、热词和默认说话人角色。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "qc.transcription")
public class TranscriptionProperties {

    /**
     * 是否启用本地 ASR 转写
     */
    private boolean enabled = false;

    /**
     * 本地 ASR 服务根地址，例如 http://127.0.0.1:18080
     */
    private String baseUrl = "http://127.0.0.1:18080";

    /**
     * 转写接口路径
     */
    private String transcribePath = "/transcribe";

    /**
     * 直接传本地文件路径给 ASR 服务时使用的接口路径。
     * 当 Java 与 Python 服务部署在同一台机器，并且共享 temp/audio 目录时，
     * 走这个接口可以绕过 multipart 上传，避免出现 400 BAD_REQUEST。
     */
    private String transcribePathByLocalFile = "/transcribe_path";

    /**
     * 健康检查接口路径
     */
    private String healthPath = "/health";

    /**
     * 录音下载及上传时使用的临时目录
     */
    private String tempDir = "./temp/audio";

    /**
     * 连接超时时间（秒）
     */
    private int connectTimeoutSeconds = 30;

    /**
     * 读取超时时间（秒）
     */
    private int readTimeoutSeconds = 600;

    /**
     * 热词，多个词用空格分隔，例如：订单号 客服 工单 售后
     */
    private String hotword = "";

    /**
     * 本地 ASR 未返回角色时使用的默认角色
     */
    private String defaultSpeakerRole = "UNKNOWN";

    /**
     * raw: 使用本地 ASR 返回的 speaker_role/speaker
     * callcenter: 按 speaker 值依次映射为 AGENT / CUSTOMER
     */
    private String speakerRoleMode = "raw";
}
