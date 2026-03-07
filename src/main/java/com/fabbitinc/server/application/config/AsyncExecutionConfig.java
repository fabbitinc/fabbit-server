package com.fabbitinc.server.application.config;

import com.fabbitinc.server.application.drawing.config.DrawingConverterProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncExecutionConfig {

    @Bean(name = "synthesisTaskExecutor")
    public Executor synthesisTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("synthesis-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "drawingTaskExecutor")
    public Executor drawingTaskExecutor(DrawingConverterProperties drawingConverterProperties) {
        int concurrency = Math.max(1, drawingConverterProperties.maxConcurrent());

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(concurrency);
        executor.setMaxPoolSize(concurrency);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("drawing-");
        executor.initialize();
        return executor;
    }
}
