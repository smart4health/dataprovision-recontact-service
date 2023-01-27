package com.healthmetrix.recontact

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.context.event.SimpleApplicationEventMulticaster
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class TaskExecutorConfiguration {

    @Bean
    @Primary
    fun provideTaskExecutor(): TaskExecutor = ThreadPoolTaskExecutor().apply {
        setQueueCapacity(0)
        setWaitForTasksToCompleteOnShutdown(true)
        setAwaitTerminationSeconds(30)
        initialize()
    }

    @Bean("applicationEventMulticaster")
    fun provideApplicationEventMulticaster(
        taskExecutor: TaskExecutor,
    ): ApplicationEventMulticaster = SimpleApplicationEventMulticaster().apply {
        setTaskExecutor(taskExecutor)
    }
}
