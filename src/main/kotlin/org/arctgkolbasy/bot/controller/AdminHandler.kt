package org.arctgkolbasy.bot.controller

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.springframework.stereotype.Controller

@Controller
class AdminHandler : SecuredCommand({ it.roles.contains(UserRoles.ADMIN) }) {
    override fun getCommandName() = "admin"
    override fun handleUpdateInternal(user: User, bot: Bot, update: Update) {
        bot.sendMessage(ChatId.fromId(update.message!!.chat.id), "привет, великий ${user.username}")
    }
}