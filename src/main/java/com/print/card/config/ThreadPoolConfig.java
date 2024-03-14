package com.print.card.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

//@Configuration
public class ThreadPoolConfig implements AsyncConfigurer {

    @Value("${core.pool.size}")
    private Integer corePoolSize;
    @Value("${max_pool.size}")
    private Integer maxPoolSize;
    @Value("${queue.capacity}")
    private Integer queueCapacity;
    @Value("${keep.alive.seconds}")
    private Integer keepAliveSeconds;
    @Value("thread.name.prefix")
    private String threadNamePrefix;
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize); // 核心线程数
        executor.setMaxPoolSize(maxPoolSize); // 最大线程数
        executor.setQueueCapacity(queueCapacity); // 队列容量
        executor.setKeepAliveSeconds(keepAliveSeconds); // 线程空闲时间(单位：秒)
        executor.setThreadNamePrefix(threadNamePrefix); // 线程前缀名
        executor.setWaitForTasksToCompleteOnShutdown(true); // 关闭时等待任务都完成
        executor.setAwaitTerminationSeconds(60); // 等待终止的时间(单位：秒)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 拒绝策略
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        // 你可以自定义一个AsyncUncaughtExceptionHandler来处理未捕获的异常
        return null;
    }
}