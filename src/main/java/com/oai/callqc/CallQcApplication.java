package com.oai.callqc;



/**
 * 源码中文讲解：项目启动类
 *
 * - 这是 Spring Boot 的入口类，main 方法启动整个项目。
 * - 当前类同时开启了异步任务能力，供“录音上传后异步转写/质检”场景使用。
 * - 这里新增了 @EruptScan，用于告诉 Erupt 去哪些包下扫描低代码实体与登录模型。
 *   如果缺少这个注解，访问 /erupt 或调用 Erupt 登录接口时，就会出现“Not found "@EruptScan" Annotation”错误。
 * - 这里额外显式声明了 @EntityScan 和 @EnableJpaRepositories，
 *   目的是在 Erupt + Spring Data JPA 组合下，强制把本项目的 entity / repository 包纳入扫描。
 *   这样可以避免启动时出现“Not a managed type: class com.oai.callqc.entity.CallRecord”这类实体未纳管异常。
 *
 * 阅读建议：先看该类对外暴露的方法名，再结合调用链理解它在“录音接入 → 转写 → 质检 → 复核 → 报表”中的位置。
 */
import com.oai.callqc.service.DataInitializerService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import xyz.erupt.core.annotation.EruptScan;

@SpringBootApplication
@EruptScan({"com.oai.callqc", "xyz.erupt"})
@EntityScan(basePackages = {"com.oai.callqc.entity"})
@EnableJpaRepositories(basePackages = {"com.oai.callqc.repository"})
@EnableAsync
@EnableScheduling
@RequiredArgsConstructor
public class CallQcApplication {

    /**
     * 功能说明：程序主入口，用于启动整个 Spring Boot 应用。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param args 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    public static void main(String[] args) {
        SpringApplication.run(CallQcApplication.class, args);
    }

    /**
     * 功能说明：执行初始化逻辑，准备系统运行所需的基础数据或环境。
     * 业务含义：这是当前类中的一个独立处理节点，阅读时建议结合调用链一起理解。
     * @param initializerService 方法输入参数，具体含义请结合调用场景和字段命名理解。
     */
    @Bean
    CommandLineRunner init(DataInitializerService initializerService) {
        return args -> initializerService.init();
    }
}
