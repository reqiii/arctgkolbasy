package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.handlers.Handler
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserService

abstract class SecuredHandler(
    protected val userService: UserService,
    private val currentUserHolder: ThreadLocal<User?>,
    private val isTerminal: Boolean,
) : Handler {

    abstract fun checkUpdateInternal(update: Update, user: User): Boolean

    abstract fun handleUpdateInternal(user: User, bot: Bot, update: Update)

    override fun checkUpdate(update: Update): Boolean = checkUpdateInternal(
        update = update,
        user = currentUserHolder.get() ?: throw IllegalStateException("Illegal access")
    )

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
            if (isTerminal) {
                update.consume()
            }
            currentUserHolder.set(null)
        }
    }
}
