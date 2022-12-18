package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserService
import org.springframework.beans.factory.annotation.Qualifier

abstract class SecuredCommand(
    userService: UserService,
    @Qualifier("currentUserHolder")
    currentUserHolder: ThreadLocal<User?>,
    isTerminal: Boolean = true,
) : SecuredHandler(
    userService = userService,
    currentUserHolder = currentUserHolder,
    isTerminal = isTerminal
) {
    abstract fun getCommandName(): String

    override fun checkUpdateInternal(update: Update, user: User): Boolean = update
        .message
        ?.text
        ?.startsWith("/${getCommandName()}")?.and(checkUserAccess(user))
        ?: false

    abstract fun checkUserAccess(user: User): Boolean
}
