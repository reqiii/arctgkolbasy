package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.handlers.Handler
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.Session
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.emptySession
import org.arctgkolbasy.user.UserService
import org.springframework.stereotype.Component

@Component
class CurrentUserHandler(
    val userService: UserService,
    val commands: List<SecuredCommand>,
    val helpCommand: HelpCommand,
) : Handler {
    override fun checkUpdate(update: Update): Boolean =
        update.message?.from != null || update.callbackQuery?.from != null

    override fun handleUpdate(bot: Bot, update: Update) {
        val from = update.message?.from
            ?: update.callbackQuery?.from
            ?: throw IllegalStateException("непонятно от кого апдейт")
        val user = userService.getOrCreateUser(
            tgApiUser = from,
            chatId = update.message?.chat?.id
                ?: update.callbackQuery?.message?.chat?.id
                ?: throw IllegalStateException("Непонятный id чата")
        )
        val command = commands.firstOrNull {
            it.checkUserAccess(user) && (messageStartsFromCommand(update, it) || callbackStartsFromCommand(update, it))
        } ?: commands.firstOrNull {
            it.checkUserAccess(user) && it.sessionCheck(user.sessionKey, user.session)
        } ?: helpCommand
        val session = command.safeHandleUpdate(bot, update, user)
        if (session == emptySession && command != helpCommand) {
            helpCommand.handleUpdateInternal(user, bot, update)
        }
        if (session != null) {
            userService.updateSession(
                id = user.id,
                sessionKey = session.sessionKey,
                session = session.session
            )
        }
    }

    private fun callbackStartsFromCommand(
        update: Update,
        it: SecuredCommand,
    ) = update.callbackQuery?.data?.startsWith(it.getCommandName()) == true

    private fun messageStartsFromCommand(
        update: Update,
        it: SecuredCommand,
    ) = update.message?.text?.startsWith("/${it.getCommandName()}") == true

    private fun SecuredCommand.safeHandleUpdate(bot: Bot, update: Update, user: User): Session? {
        val session = try {
            this.handleUpdateInternal(user, bot, update)
        } catch (exception: Exception) {
            exception.printStackTrace()
            bot.sendMessage(
                chatId = update.chatIdUnsafe(),
                text = "Ошибка: ${exception.message}"
            )
            null
        } finally {
            update.consume()
        }
        return session
    }
}
