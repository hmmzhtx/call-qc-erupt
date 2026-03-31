package com.oai.callqc.config;



/**
 * 源码中文讲解：异步任务配置属性
 *
 * - 映射 qc.async.* 配置，控制异步任务是否开启以及线程池大小等。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "qc.async")
public class AsyncTaskProperties {

    /**
     * 是否启用异步转写任务
     */
    private boolean enabled = true;

    /**
     * 导入通话且携带录音地址后，是否自动提交异步转写
     */
    private boolean autoSubmitAfterImport = true;

    /**
     * 上传录音文件后，是否自动提交异步转写
     */
    private boolean autoSubmitAfterRecordingUpload = true;

    /**
     * 本地录音文件上传目录
     */
    private String recordingUploadDir = "./uploads/recordings";

    /**
     * 线程池核心线程数
     */
    private int executorCorePoolSize = 2;

    /**
     * 线程池最大线程数
     */
    private int executorMaxPoolSize = 4;

    /**
     * 队列容量
     */
    private int executorQueueCapacity = 100;

    /**
     * 队列消费定时轮询间隔（毫秒）
     */
    private long dispatchFixedDelayMs = 2000L;

    /**
     * 每次从队列中取出的最大任务数
     */
    private int queueBatchSize = 5;

    /**
     * 队列消费者允许同时运行的最大任务数
     */
    private int maxConcurrentRunning = 2;

    /**
     * 默认最大重试次数
     */
    private int maxRetryCount = 3;

    /**
     * 重试等待秒数
     */
    private long retryDelaySeconds = 30L;

}
