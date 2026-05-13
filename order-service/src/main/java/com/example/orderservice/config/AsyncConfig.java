package com.example.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 * <p>
 * 知识点：
 * 1. @EnableAsync 开启 Spring 异步支持
 * 2. 显式声明线程池 Bean，避免使用默认的 SimpleAsyncTaskExecutor（会无限创建线程）
 * 3. 线程池参数决定了高并发下的行为和资源消耗
 * </p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 订单通知专用线程池
     * 命名明确，方便监控和排查问题
     */
    @Bean("orderNotifyExecutor")
    public Executor orderNotifyExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：常驻线程数量，和 CPU 核数相关，这里设 4
        executor.setCorePoolSize(4);
        // 最大线程数：核心线程忙且队列满时，最多扩展到这个数量
        executor.setMaxPoolSize(8);
        // 任务队列容量：核心线程忙时，新任务先放入队列等待
        executor.setQueueCapacity(100);
        // 非核心线程空闲存活时间（秒）
        executor.setKeepAliveSeconds(60);
        // 线程名前缀，便于日志中区分
        executor.setThreadNamePrefix("order-notify-");
        // 拒绝策略：当队列满且线程池达到最大线程数时，由调用者线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅关闭：等待线程池中的任务执行完毕再关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 初始化线程池
        executor.initialize();
        return executor;
    }
}