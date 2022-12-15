package org.arctgkolbasy.bot

import com.github.kotlintelegrambot.Bot
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class ApplicationInitializer(
    val bot: Bot,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        bot.startPolling()
        println("Bot ${bot.getMe().get().username} started...")
    }
}
