package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.bot.user.UserService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller

@Controller
class HelloWorldCommand(
    userService: UserService,
    @Qualifier("currentUserHolder")
    currentUserHolder: ThreadLocal<User?>,
) : SecuredCommand(
    userService = userService,
    currentUserHolder = currentUserHolder
) {
    override fun getCommandName() = "hello"
    override fun checkUserAccess(user: User): Boolean = user.roles.contains(UserRoles.USER)
    override fun handleUpdateInternal(user: User, bot: Bot, update: Update) {
        bot.sendMessage(
            chatId = ChatId.fromId(update.message!!.chat.id),
            text = "Hi there!"
        )
    }
}