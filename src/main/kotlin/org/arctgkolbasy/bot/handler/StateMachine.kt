package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.Session
import org.arctgkolbasy.bot.user.User

abstract class StateMachine : SecuredCommand {
    private val steps: MutableMap<String, (user: User, bot: Bot, update: Update) -> Session> = mutableMapOf()

    override fun sessionCheck(sessionKey: String?, session: String?) = sessionKey in steps.keys

    abstract fun stepZero(user: User, bot: Bot, update: Update): Session

    fun addSessionStep(step: String, handler: (user: User, bot: Bot, update: Update) -> Session) {
        steps[step] = handler
    }

    override fun handleUpdateInternal(user: User, bot: Bot, update: Update): Session {
        if (update.message?.text == "/${getCommandName()}" || update.callbackQuery?.data == getCommandName()) {
            return stepZero(user, bot, update)
        }
        return steps[user.sessionKey]?.invoke(user, bot, update) ?: stepZero(user, bot, update)
    }
}