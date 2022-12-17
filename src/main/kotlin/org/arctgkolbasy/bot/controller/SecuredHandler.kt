package org.arctgkolbasy.bot.controller

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.handlers.Handler
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserService
import java.util.function.Predicate

abstract class SecuredHandler(
    val securityCheck: Predicate<User>,
    val userService: UserService,
) : Handler {
    private val currentUserHolder: ThreadLocal<User?> = ThreadLocal()

    abstract fun checkUpdateInternal(update: Update): Boolean

    abstract fun handleUpdateInternal(user: User, bot: Bot, update: Update)

    override fun checkUpdate(update: Update): Boolean = checkUpdateInternal(update) && checkSecurityAndSetUser(update)

    protected fun checkSecurityAndSetUser(update: Update): Boolean {
        val tgApiUser = update.message?.from
        if (tgApiUser != null) {
            val currentUser = userService.getOrCreateUser(tgApiUser)
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
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            update.consume()
            currentUserHolder.set(null)
        }
    }
}
