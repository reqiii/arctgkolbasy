package org.arctgkolbasy.bot.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
class ExecutorConfiguration {
    @Bean
    fun executorService(): ExecutorService {
        val coresCount = Runtime.getRuntime().availableProcessors()
        return Executors.newFixedThreadPool(coresCount)
    }
}