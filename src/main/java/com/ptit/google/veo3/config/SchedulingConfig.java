package com.ptit.google.veo3.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration class cho Scheduling và Async processing
 * 
 * FEATURES:
 * - Enable @Scheduled annotations
 * - Enable @Async annotations  
 * - Configure dedicated thread pool cho scheduled tasks
 * 
 * PERFORMANCE TUNING:
 * - Pool size: 5 threads (đủ cho các scheduled jobs hiện tại)
 * - Thread naming: 'video-scheduler-' prefix để dễ debug
 * - Graceful shutdown với await termination
 * 
 * @author System
 * @since 1.4.0
 */
@Configuration
@EnableScheduling
@EnableAsync
@Slf4j
public class SchedulingConfig {

    /**
     * TaskScheduler bean để handle các @Scheduled jobs
     * 
     * THREAD POOL CONFIGURATION:
     * - Pool size: 5 threads (có thể scale theo nhu cầu)
     * - Thread prefix: 'video-scheduler-' để dễ monitoring
     * - Daemon threads: false để đảm bảo jobs hoàn thành trước shutdown
     * - Wait for termination: 60 seconds cho graceful shutdown
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        
        // Thread pool configuration
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("video-scheduler-");
        scheduler.setDaemon(false);
        
        // Graceful shutdown configuration
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        
        // Error handling
        scheduler.setRejectedExecutionHandler((runnable, executor) -> {
            log.warn("Scheduled task rejected: {}, active threads: {}, queue size: {}", 
                    runnable.toString(), executor.getActiveCount(), executor.getQueue().size());
        });
        
        scheduler.initialize();
        
        log.info("TaskScheduler initialized with pool size: {}, thread prefix: '{}'", 
                scheduler.getPoolSize(), scheduler.getThreadNamePrefix());
        
        return scheduler;
    }
}
