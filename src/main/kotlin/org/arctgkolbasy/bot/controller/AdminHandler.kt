package org.arctgkolbasy.bot.controller

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.bot.user.UserService
import org.springframework.stereotype.Controller

@Controller
class AdminHandler(
    userService: UserService
) : SecuredCommand(
    { user -> user.roles.contains(UserRoles.ADMIN) },
    userService
) {
    override fun getCommandName() = "admin"
    override fun handleUpdateInternal(user: User, bot: Bot, update: Update) {
        bot.sendMessage(
            chatId = ChatId.fromId(update.message!!.chat.id),
            text = "привет, великий ${user.username}"
        )
    }
}
