package org.arctgkolbasy.bot.controller

import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.User
import java.util.function.Predicate

abstract class SecuredCommand(
    securityCheck: Predicate<User>,
) : SecuredHandler(securityCheck) {
    abstract fun getCommandName(): String
    override fun checkUpdateInternal(update: Update): Boolean = update
        .message
        ?.text
        ?.startsWith("/${getCommandName()}")
        ?: false
}
