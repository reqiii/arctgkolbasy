package org.arctgkolbasy.bot.controller

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.handlers.Handler
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.User
import org.springframework.stereotype.Controller

@Controller
class HelpCommand(
    val securedCommands: List<SecuredCommand>,
) : SecuredCommand({ true }) {
    override fun getCommandName(): String = "help"
    override fun handleUpdateInternal(user: User, bot: Bot, update: Update) {
        bot.sendMessage(
            ChatId.fromId(update.message!!.chat.id),
            listOf(getCommandName())
                .plus(securedCommands
                    .filter { it.securityCheck.test(user) }
                    .map { it.getCommandName() }
                ).joinToString(
                    prefix = "Available commands:\n",
                    separator = "\n",
                    transform = { "/$it" }
                )
        )
    }
}
