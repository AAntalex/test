package com.antalex.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class SchedulingConfiguration {

    @Value("${test.scheduler.scheduler-thread-count}")
    public int schedulerThreadCount;

    @Bean(name = "TestTaskScheduler")
    public ThreadPoolTaskScheduler threadPoolTaskTicketScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(schedulerThreadCount);
        scheduler.setThreadNamePrefix("TEST_SCHEDULER");
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
