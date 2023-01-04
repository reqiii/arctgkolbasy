package org.arctgkolbasy.bot.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.Handler
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.handler.CurrentUserHandler
import org.arctgkolbasy.bot.handler.HelpCommand.Companion.HELP_COMMAND
import org.arctgkolbasy.bot.handler.chatIdUnsafe
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BotConfig(
    val currentUserHandler: CurrentUserHandler,
    val objectMapper: ObjectMapper,
) {

    @Value("\${telegram.bot.token}")
    lateinit var botToken: String

    @Bean
    fun bot() = bot {
        token = botToken
        dispatch {
            setUpCommands()
            logUpdate()
            addHandler(currentUserHandler)
        }
    }

    private fun Dispatcher.logUpdate() {
        addHandler(
            object : Handler {
                override fun checkUpdate(update: Update) = true
                override fun handleUpdate(bot: Bot, update: Update) =
                    println("received update '${objectMapper.writeValueAsString(update)}'")
            }
        )
    }

    private fun Dispatcher.setUpCommands() {
        command("start") {
            bot.sendMessage(
                chatId = update.chatIdUnsafe(),
                text = "Привет! Используй команду /$HELP_COMMAND для получения списка доступных команд."
            )
        }
    }
}
