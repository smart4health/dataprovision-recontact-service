package com.healthmetrix.recontact

import io.micrometer.core.aop.CountedAspect
import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfiguration {

    @Bean
    fun timedAspect(registry: MeterRegistry): TimedAspect = TimedAspect(registry)

    @Bean
    fun countedAspect(registry: MeterRegistry): CountedAspect = CountedAspect(registry)
}
