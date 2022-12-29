package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.handlers.Handler
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.user.UserService

abstract class SecuredHandler(
    val isStateless: Boolean,
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
                user = currentUserHolder.get() ?: throw IllegalStateException("Illegal access"),
                bot = bot,
                update = update
            )
        } finally {
            if (isTerminal) {
                update.consume()
            }
        }
    }

    fun User.clearSession() = updateSession()

    fun User.updateSession(sessionKey: String? = null, session: String? = null) {
        userService.updateSession(id, sessionKey, session)
    }
}

fun Update.chatIdUnsafe(): ChatId = chatId()!!

fun Update.chatId(): ChatId? {
    val id = this.message?.chat?.id ?: return null
    return ChatId.fromId(id)
}

fun Update.message(): String = this.message?.text?.trim() ?: ""
