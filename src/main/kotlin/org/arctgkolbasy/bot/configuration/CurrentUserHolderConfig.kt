package org.arctgkolbasy.bot.configuration

import org.arctgkolbasy.bot.user.User
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CurrentUserHolderConfig {
    @Bean
    fun currentUserHolder(): ThreadLocal<User?> = ThreadLocal()
}
