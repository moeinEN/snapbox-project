package com.aut.snapboxfilereader;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;



@Configuration
public class BaseConfiguration {
    private BlockingQueue<Runnable> synchronousMessagesQueueForWorkMessages = new SynchronousQueue();
    @Value("${worker.core.pool.size}")
    int workerCorePoolSize;
    @Value("${worker.max.pool.size}")
    int workerMaxPoolSize;


    @Bean("base")
    public ThreadPoolExecutor threadPoll() {
        ThreadFactory threadFactory = (new ThreadFactoryBuilder()).setNameFormat("b-p-%d").build();
        return new ThreadPoolExecutor(workerCorePoolSize, workerMaxPoolSize, 1L, TimeUnit.MINUTES, synchronousMessagesQueueForWorkMessages, threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
    }
}