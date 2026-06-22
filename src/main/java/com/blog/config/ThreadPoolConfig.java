package com.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 *
 * <p>为 AI 调用等异步任务提供专用线程池，避免使用 ForkJoinPool.commonPool() 导致线程饥饿。</p>
 *
 * @author Diamond
 * @since 1.0.0
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * AI 服务专用线程池
     *
     * <p>用于执行 AI 文章生成、润色、建议等异步任务。</p>
     * <p>配置说明：</p>
     * <ul>
     *   <li>核心线程数: 5 - 保持常驻线程处理常规请求</li>
     *   <li>最大线程数: 10 - 允许在高峰期扩展线程</li>
     *   <li>队列容量: 25 - 缓冲超出核心线程数的任务</li>
     *   <li>拒绝策略: CallerRunsPolicy - 队列满时由调用线程执行，避免任务丢失</li>
     * </ul>
     *
     * @return AI 服务专用线程池执行器
     */
    @Bean("aiExecutor")
    public Executor aiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：保持常驻的线程数量
        executor.setCorePoolSize(5);
        // 最大线程数：线程池允许创建的最大线程数量
        executor.setMaxPoolSize(10);
        // 队列容量：当核心线程都在忙时，新任务会进入队列等待
        executor.setQueueCapacity(25);
        // 线程名前缀：便于日志追踪和问题排查
        executor.setThreadNamePrefix("ai-");
        // 拒绝策略：队列满且线程数达到最大值时，由调用线程执行任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 线程空闲存活时间：60秒
        executor.setKeepAliveSeconds(60);
        // 允许核心线程超时：在空闲时也可以回收核心线程
        executor.setAllowCoreThreadTimeOut(true);
        // 初始化线程池
        executor.initialize();
        return executor;
    }
}
