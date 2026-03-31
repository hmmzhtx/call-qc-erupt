package com.oai.callqc.config;



/**
 * 源码中文讲解：异步线程池配置
 *
 * - 定义 qcTaskExecutor 线程池，供异步转写任务执行。
 * - 线程池参数来自 AsyncTaskProperties，方便后续按机器性能调优。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncTaskConfig {

    /**
     * 功能说明：创建异步任务线程池，供后台录音转写任务使用。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param properties 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    @Bean(name = "qcTaskExecutor")
    public Executor qcTaskExecutor(AsyncTaskProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getExecutorCorePoolSize());
        executor.setMaxPoolSize(properties.getExecutorMaxPoolSize());
        executor.setQueueCapacity(properties.getExecutorQueueCapacity());
        executor.setThreadNamePrefix("call-qc-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
