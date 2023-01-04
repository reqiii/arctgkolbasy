package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import emoji.Emoji
import org.arctgkolbasy.bot.user.Session
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.bot.user.emptySession
import org.arctgkolbasy.consumer.Consumer
import org.arctgkolbasy.consumer.ConsumerRepository
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.math.RoundingMode

@Controller
class ShowDebtsCommand(
    private val consumerRepository: ConsumerRepository,
) : StateMachine() {
    override fun getCommandName(): String = SHOW_DEBTS_COMMAND

    override fun checkUserAccess(user: User): Boolean = UserRoles.USER in user.roles

    init {
        addSessionStep(DebtsConst.STEP_0_SELECT_DEBT_TYPE.step, ::selectDebt)
    }

    override fun stepZero(user: User, bot: Bot, update: Update): Session {
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = "Выбери, чей долг ты хочешь проверить:",
            replyMarkup = keyboardReplyMarkup,
        )
        return Session(DebtsConst.STEP_0_SELECT_DEBT_TYPE.step)
    }

    private fun selectDebt(user: User, bot: Bot, update: Update): Session {
        return when (update.message()) {
            SELF_DEBTS -> selfDebts(update, bot, user)
            MY_DEBTORS -> myDebtors(update, bot, user)
            else -> throw IllegalArgumentException(
                "Я тебя не понял. Посмотреть сколько: '${SELF_DEBTS}' или '${MY_DEBTORS}'?"
            )
        }
    }

    private fun selfDebts(update: Update, bot: Bot, user: User): Session {
        val debtors = consumerRepository.findAll()
            .filter { it.consumer.id == user.id && it.product.buyer.id != user.id }
            .groupingBy { it.product.buyer.username }
            .fold(BigDecimal(0)) { total: BigDecimal, c: Consumer ->
                total + c.product.cost.divide(
                    BigDecimal(c.product.initialAmount),
                    RoundingMode.CEILING
                ) * BigDecimal(c.consumedAmount)
            }
            .map { it.key + " - " + it.value.setScale(2, RoundingMode.CEILING) }
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            debtors.joinToString(
                prefix = "${Emoji.MONEY_WITH_WINGS.emoji} Ты должен отдать ${Emoji.MONEY_WITH_WINGS.emoji}\n",
                separator = "\n",
                transform = { "@${it}" + Emoji.MONEY_BAG.emoji }
            )
        )
        return emptySession
    }

    private fun myDebtors(update: Update, bot: Bot, user: User): Session {
        val debtors = consumerRepository.findAll()
            .filter { it.product.buyer.id == user.id && it.consumer.id != user.id }
            .groupingBy { it.consumer.username }
            .fold(BigDecimal(0)) { total: BigDecimal, c: Consumer ->
                total + c.product.cost.divide(
                    BigDecimal(c.product.initialAmount),
                    RoundingMode.CEILING
                ) * BigDecimal(c.consumedAmount)
            }
            .map { it.key + " - " + it.value.setScale(2, RoundingMode.CEILING) }
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            debtors.joinToString(
                prefix = "${Emoji.DOLLAR_BANKNOTE.emoji} Ты должен получить ${Emoji.DOLLAR_BANKNOTE.emoji}\n",
                separator = "\n",
                transform = { "@${it}" + Emoji.MONEY_BAG.emoji }
            )
        )
        return emptySession
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

    enum class DebtsConst(
        val step: String,
    ) {
        STEP_0_SELECT_DEBT_TYPE("${SHOW_DEBTS_COMMAND}0"),
    }

    companion object {
        const val SHOW_DEBTS_COMMAND = "show_debts"
        private val SELF_DEBTS = "Свой долг" + Emoji.FACE_EXHALING.emoji
        private val MY_DEBTORS = "Кто мне должен" + Emoji.BEAMING_FACE_WITH_SMILING_EYES.emoji
    }
}
