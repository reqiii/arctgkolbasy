package org.arctgkolbasy.bot.controller

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserService
import org.springframework.stereotype.Controller

@Controller
class HelloWorldCommand(
    userService: UserService
) : SecuredCommand(
    { true },
    userService
) {
    override fun getCommandName() = "hello"
    override fun handleUpdateInternal(user: User, bot: Bot, update: Update) {
        bot.sendMessage(
            chatId = ChatId.fromId(update.message!!.chat.id),
            text = "Hi there!"
        )
    }
}
