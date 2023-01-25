package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.Session
import org.arctgkolbasy.bot.user.User

interface SecuredCommand {
    fun display() = true
    fun getCommandName(): String
    fun checkUserAccess(user: User): Boolean
    fun sessionCheck(sessionKey: String?, session: String?): Boolean = false
    fun handleUpdateInternal(user: User, bot: Bot, update: Update): Session
}

fun Update.chatIdUnsafe(): ChatId = chatId()!!

fun Update.chatId(): ChatId? {
    val id = this.message?.chat?.id
        ?: this.callbackQuery?.message?.chat?.id
        ?: return null
    return ChatId.fromId(id)
}

fun Update.message(): String = this.message?.text?.trim() ?: ""
