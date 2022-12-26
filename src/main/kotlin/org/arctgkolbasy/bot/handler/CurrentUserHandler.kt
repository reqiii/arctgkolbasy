package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.handlers.Handler
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.user.UserService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class CurrentUserHandler(
    val userService: UserService,
    @Qualifier("currentUserHolder")
    val currentUserHolder: ThreadLocal<User?>,
    val commands: List<SecuredCommand>,
) : Handler {
    override fun checkUpdate(update: Update): Boolean = update.message?.from != null

    override fun handleUpdate(bot: Bot, update: Update) {
        val user = userService.getOrCreateUser(update.message!!.from!!)
        currentUserHolder.set(user)
        val command = commands.singleOrNull {
            update.message?.text?.startsWith("/${it.getCommandName()}") == true
        }
        if (command != null) {
            command.handleUpdateAndClearSessionIfNeeded(bot, update, user)
        } else {
            commands.filter { it.checkUpdate(update) }
                .forEach {
                    if (update.consumed) {
                        return@forEach
                    }
                    it.handleUpdateAndClearSessionIfNeeded(bot, update, user)
                }
        }
        currentUserHolder.set(null)
    }

    private fun SecuredCommand.handleUpdateAndClearSessionIfNeeded(bot: Bot, update: Update, user: User) {
        try {
            this.handleUpdate(bot, update)
        } catch (exception: Exception) {
            bot.sendMessage(
                chatId = update.chatIdUnsafe(),
                text = "Ошибка: ${exception.message}"
            )
        }
        if (this.isStateless) {
            user.clearSession()
        }
    }
}
