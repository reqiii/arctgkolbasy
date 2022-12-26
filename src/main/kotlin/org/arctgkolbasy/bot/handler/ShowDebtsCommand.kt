package org.arctgkolbasy.bot.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.user.UserRepository
import org.arctgkolbasy.user.UserService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller
import org.arctgkolbasy.consumer.ConsumerRepository

@Controller
class ShowDebtsCommand(
    val userRepository: UserRepository,
    userService: UserService,
    private val consumerRepository: ConsumerRepository,
    @Qualifier("currentUserHolder")
    currentUserHolder: ThreadLocal<User?>,
    val objectMapper: ObjectMapper,
) : SecuredCommand(
    userService = userService,
    currentUserHolder = currentUserHolder,
    isStateless = false,
) {
    override fun getCommandName(): String = SHOW_DEBTS_COMMAND

    override fun checkUserAccess(user: User): Boolean = UserRoles.USER in user.roles

    override fun handleUpdateInternal(user: User, bot: Bot, update: Update) {

        when (update.message()) {
            SELF_DEBTS -> {
            }

            MY_DEBTORS -> {
                bot.sendMessage(
                    chatId = update.chatIdUnsafe(),
                    text = "Сейчас посмотрим" //${consumerRepository.findById(user.id)}
                )
            }
        }
    }

    private fun stepZero(update: Update, bot: Bot, user: User) {
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = "Проверить сколько:",
            replyMarkup = keyboardReplyMarkup,
        )
        user.updateSession(
            DebtsOptions.OPTION_1_SELF_DEBTS.step,
            objectMapper.writeValueAsString(null)
        )
    }

    private val keyboardReplyMarkup = KeyboardReplyMarkup(
        keyboard = listOf(
            listOf(
                KeyboardButton(SELF_DEBTS),
                KeyboardButton(MY_DEBTORS),
            )
        ),
        resizeKeyboard = true,
        oneTimeKeyboard = true,
    )

    enum class DebtsOptions(
        val step: String,
    ) {
        OPTION_1_SELF_DEBTS("${SHOW_DEBTS_COMMAND}1"),
        OPTION_2_MY_DEBTORS("${SHOW_DEBTS_COMMAND}2"),
    }

    companion object {
        val steps = DebtsOptions.values().mapTo(mutableSetOf()) { it.step }
        const val SHOW_DEBTS_COMMAND = "show_debts"
        const val SELF_DEBTS = "Я должен"
        const val MY_DEBTORS = "Мне должны"
    }

    data class ShowDebtsSession(
        val option: String,
    )
}