package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import org.arctgkolbasy.bot.user.Session
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.emptySession
import org.arctgkolbasy.user.UserService
import org.springframework.stereotype.Controller

@Controller
class HelpCommand(
    val securedCommands: List<SecuredCommand>,
    val userService: UserService,
) : SecuredCommand {
    override fun getCommandName(): String = HELP_COMMAND
    override fun checkUserAccess(user: User) = true

    override fun handleUpdateInternal(user: User, bot: Bot, update: Update): Session {
        val resp = bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = "Доступные команды:",
            replyMarkup = getInlineKeyboardMarkup(user),
        )
        if (resp.isSuccess) {
            user.lastMenuMessageId?.let { bot.deleteMessage(update.chatIdUnsafe(), it) }
            userService.updateLastMenu(user, resp.get().messageId)
        }
        return emptySession
    }

    private fun getInlineKeyboardMarkup(user: User): InlineKeyboardMarkup {
        val buttons = securedCommands.filter { it.checkUserAccess(user) }
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
