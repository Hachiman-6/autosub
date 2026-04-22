package com.djx.autosub.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolConfig {

    /**
     * 创建自定义网络调用线程池
     *
     * @return 自定义网络调用线程池
     */
    @Bean
    public ExecutorService networkRequestThreadPool() {

        int corePoolSize = 8;      // 核心线程数
        int maximumPoolSize = 16;  // 最大线程数
        long keepAliveTime = 60L;  // 非核心线程空闲存活时间
        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(100); // 有界队列

        // 自定义线程工厂，便于日志追踪
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread t = new Thread(r, "NetworkRequestPool-" + threadNumber.getAndIncrement());
                t.setDaemon(false); // 非守护线程
                if (t.getPriority() != Thread.NORM_PRIORITY) {
                    t.setPriority(Thread.NORM_PRIORITY);
                }
                return t;
            }
        };

        // 拒绝策略：由调用者线程执行
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

        return new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime, unit,
                workQueue,
                threadFactory,
                handler
        );
    }
}
