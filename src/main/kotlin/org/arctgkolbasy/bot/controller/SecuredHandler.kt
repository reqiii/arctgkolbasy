package org.arctgkolbasy.bot.controller

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.handlers.Handler
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import java.lang.IllegalStateException
import java.util.function.Predicate
import com.github.kotlintelegrambot.entities.User as TgApiUser

abstract class SecuredHandler(
    val securityCheck: Predicate<User>,
) : Handler {
    private val currentUserHolder: ThreadLocal<User?> = ThreadLocal()

    abstract fun getCommandName(): String

    abstract fun checkUpdateInternal(update: Update): Boolean

    abstract fun handleUpdateInternal(user: User, bot: Bot, update: Update)

    override fun checkUpdate(update: Update): Boolean = checkUpdateInternal(update) && checkSecurityAndSetUser(update)

    protected fun checkSecurityAndSetUser(update: Update): Boolean {
        val tgApiUser = update.message?.from
        if (tgApiUser != null) {
            val currentUser = getOrCreateUser(tgApiUser)
            if (securityCheck.test(currentUser)) {
                currentUserHolder.set(currentUser)
                return true
            }
            return false
        }
        return false
    }

    override fun handleUpdate(bot: Bot, update: Update) {
        try {
            handleUpdateInternal(
                user = currentUserHolder.get() ?: throw IllegalStateException("insecure access"),
                bot = bot,
                update = update
            )
        } finally {
            update.consume()
            currentUserHolder.set(null)
        }
    }

    protected fun getOrCreateUser(tgApiUser: TgApiUser): User {
        return User(
            id = -1,
            isBot = false,
            firstName = tgApiUser.firstName,
            lastName = tgApiUser.lastName,
            username = tgApiUser.username,
            roles = if (tgApiUser.username == "problem_hunter") {
                listOf(UserRoles.ADMIN, UserRoles.USER)
            } else {
                listOf(UserRoles.USER)
            },
        )
    }
}
