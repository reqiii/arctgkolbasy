package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.user.UserService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller
import java.util.*

@Controller
class HelpCommand(
    val securedCommands: List<SecuredCommand>,
    userService: UserService,
    @Qualifier("currentUserHolder")
    currentUserHolder: ThreadLocal<User?>,
) : SecuredCommand(
    userService = userService,
    currentUserHolder = currentUserHolder
) {
    override fun getCommandName(): String = HELP_COMMAND
    override fun checkUserAccess(user: User) = true

    override fun handleUpdateInternal(user: User, bot: Bot, update: Update) {
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = "Доступные команды:",
            replyMarkup = getInlineKeyboardMarkup(user),
        )
    }

    private fun getInlineKeyboardMarkup(user: User): InlineKeyboardMarkup {
        val buttons = listOf(this)
            .plus(securedCommands.filter { it.checkUserAccess(user) })
            .map { command ->
                InlineKeyboardButton.CallbackData(
                    text = command.getCommandName()
                        .replaceFirstChar { it.uppercase() }
                        .replace("_", " "),
                    callbackData = command.getCommandName()
                        .replace("/", "")
                )
            }
        return InlineKeyboardMarkup.create(buttons.chunked(3))
    }

    companion object {
        const val HELP_COMMAND = "help"
    }
}
