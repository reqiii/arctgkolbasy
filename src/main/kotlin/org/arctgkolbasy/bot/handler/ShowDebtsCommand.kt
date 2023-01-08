package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import emoji.Emoji
import org.arctgkolbasy.bot.user.Session
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.bot.user.emptySession
import org.arctgkolbasy.consumer.Consumer
import org.arctgkolbasy.product.Product
import org.arctgkolbasy.product.ProductRepository
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.math.RoundingMode

@Controller
class ShowDebtsCommand(
    private val productRepository: ProductRepository,
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
        return when (update.callbackQuery?.data) {
            SELF_DEBTS -> selfDebts(update, bot, user)
            MY_DEBTORS -> myDebtors(update, bot, user)
            else -> throw IllegalArgumentException(
                "Я тебя не понял. Нужно выбрать доступные варианты."
            )
        }
    }

    private fun selfDebts(update: Update, bot: Bot, user: User): Session {
        val debtors = productRepository.findAll()
            .filter { product ->
                product.buyer.id != user.id && product.consumers.any {
                    it.consumer.id == user.id
                }
            }
            .groupingBy { it.buyer.username }
            .fold(BigDecimal.ZERO) { total: BigDecimal, p: Product ->
                if (p.isDivisible()) {
                    total + p.cost.divide(BigDecimal(p.initialAmount), RoundingMode.CEILING) * BigDecimal(
                        p.consumers.find { it.product.id == p.id }!!.consumedAmount
                    )
                } else {
                    if (p.consumers.isNotEmpty())
                        total + p.cost.divide(BigDecimal(p.consumers.size), RoundingMode.CEILING)
                    else
                        total + p.cost
                }
            }
            .map { it.key + " - " + it.value.setScale(2, RoundingMode.CEILING) }
        bot.editMessageText(
            chatId = update.chatIdUnsafe(),
            messageId = update.callbackQuery?.message?.messageId,
            text = debtors.joinToString(
                prefix = "${Emoji.MONEY_WITH_WINGS.emoji} Ты должен отдать ${Emoji.MONEY_WITH_WINGS.emoji}\n",
                separator = "\n",
                transform = { "@${it}" + Emoji.MONEY_BAG.emoji }
            ),
        )
        return emptySession
    }

    private fun myDebtors(update: Update, bot: Bot, user: User): Session {
        val debtors = productRepository.findAll()
            .filter { product -> product.buyer.id == user.id }
            .flatMap { it.consumers }
            .groupingBy { it.consumer.username }
            .fold(BigDecimal.ZERO) { total: BigDecimal, c: Consumer ->
                if (c.product.isDivisible() && c.consumer.id != user.id) {
                    total + c.product.cost.divide(
                        BigDecimal(c.product.initialAmount),
                        RoundingMode.CEILING
                    ) * BigDecimal(c.consumedAmount)
                } else if (c.product.consumers.isNotEmpty() && c.consumer.id != user.id) {
                    total + c.product.cost.divide(BigDecimal(c.product.consumers.size), RoundingMode.CEILING)
                } else total
            }
            .filter { it.key != user.username }
            .map { it.key + " - " + it.value.setScale(2, RoundingMode.CEILING) }
        bot.editMessageText(
            chatId = update.chatIdUnsafe(),
            messageId = update.callbackQuery?.message?.messageId,
            text = debtors.joinToString(
                prefix = "${Emoji.DOLLAR_BANKNOTE.emoji} Ты должен получить ${Emoji.DOLLAR_BANKNOTE.emoji}\n",
                separator = "\n",
                transform = { "@${it}" + Emoji.MONEY_BAG.emoji }
            ),
        )
        return emptySession
    }

    private val keyboardReplyMarkup = InlineKeyboardMarkup.createSingleRowKeyboard(
        InlineKeyboardButton.CallbackData(SELF_DEBTS, SELF_DEBTS),
        InlineKeyboardButton.CallbackData(MY_DEBTORS, MY_DEBTORS),
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
