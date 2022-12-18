package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller

@Controller
class HelpCommand(
    val securedCommands: List<SecuredCommand>, userService: UserService,
    @Qualifier("currentUserHolder")
    currentUserHolder: ThreadLocal<User?>,
) : SecuredCommand(
    userService = userService,
    currentUserHolder = currentUserHolder
) {
    override fun getCommandName(): String = "help"
    override fun checkUserAccess(user: User) = true
    override fun handleUpdateInternal(user: User, bot: Bot, update: Update) {
        bot.sendMessage(
            ChatId.fromId(update.message!!.chat.id),
            listOf(getCommandName())
                .plus(securedCommands
                    .filter { it.checkUserAccess(user) }
                    .map { it.getCommandName() }
                ).joinToString(
                    prefix = "Available commands:\n",
                    separator = "\n",
                    transform = { "/$it" }
                )
        )
    }
}
