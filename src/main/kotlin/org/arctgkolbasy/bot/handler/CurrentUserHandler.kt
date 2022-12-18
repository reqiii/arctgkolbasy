package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.handlers.Handler
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class CurrentUserHandler(
    val userService: UserService,
    @Qualifier("currentUserHolder")
    val currentUserHolder: ThreadLocal<User?>,
    val handlers: List<Handler>
) : Handler {
    override fun checkUpdate(update: Update): Boolean = true

    override fun handleUpdate(bot: Bot, update: Update) {
        val user = update.message?.from?.let { userService.getOrCreateUser(it) }
        currentUserHolder.set(user)
        handlers.filter { it.checkUpdate(update) }
            .forEach {
                if (update.consumed) {
                    return@forEach
                }
                it.handleUpdate(bot, update)
            }
        currentUserHolder.set(null)
    }
}