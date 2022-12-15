package org.arctgkolbasy.bot.configuration

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.handlers.Handler
import com.github.kotlintelegrambot.dispatcher.text
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BotConfig(
    val handlers: List<Handler>,
) {

    @Value("\${telegram.bot.token}")
    lateinit var botToken: String

    @Bean
    fun bot() = bot {
        token = botToken
        dispatch {
            handlers.forEach(this::addHandler)
            text { println(message.text) }
        }
    }
}
