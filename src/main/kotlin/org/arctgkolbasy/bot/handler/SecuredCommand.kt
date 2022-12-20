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
    isStateless: Boolean = true,
) : SecuredHandler(
    userService = userService,
    currentUserHolder = currentUserHolder,
    isTerminal = isTerminal,
    isStateless = isStateless,
) {
    abstract fun getCommandName(): String

    protected open fun sessionCheck(sessionKey: String?, session: String?): Boolean = false

    override fun checkUpdateInternal(update: Update, user: User): Boolean =
        (messageStartsFromCommand(update) || sessionCheck(user.sessionKey, user.session)) && (checkUserAccess(user))

    private fun messageStartsFromCommand(update: Update): Boolean = update
        .message
        ?.text
        ?.startsWith("/${getCommandName()}")
        ?: false

    abstract fun checkUserAccess(user: User): Boolean
}
