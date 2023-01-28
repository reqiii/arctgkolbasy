package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import org.arctgkolbasy.bot.user.Session
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.bot.user.emptySession
import org.arctgkolbasy.transactions.Transaction
import org.arctgkolbasy.transactions.TransactionsRepository
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.math.RoundingMode

@Controller
class ShowDebtsCommand(
    private val transactionsRepository: TransactionsRepository,
) : SecuredCommand {
    override fun getCommandName(): String = SHOW_DEBTS_COMMAND

    override fun checkUserAccess(user: User): Boolean = UserRoles.USER in user.roles

    override fun handleUpdateInternal(user: User, bot: Bot, update: Update): Session {
        val fromUser = transactionsRepository.findAllFromUserId(user.id)
            .groupingBy { it.to.username }
            .fold(BigDecimal.ZERO) { total: BigDecimal, transaction: Transaction ->
                total + transaction.amount
            }
        val toUser = transactionsRepository.findAllToUserId(user.id)
            .groupingBy { it.from.username }
            .fold(BigDecimal.ZERO) { total: BigDecimal, transaction: Transaction ->
                total - transaction.amount
            }
        val (debts, creds) = (fromUser.asSequence() + toUser.asSequence())
            .groupingBy { it.key }
            .fold(BigDecimal.ZERO) { total: BigDecimal, entry: Map.Entry<String?, BigDecimal> ->
                total + entry.value
            }
            .entries
            .map { it.key to it.value.setScale(2, RoundingMode.CEILING) }
            .filter {
                it.first != user.username &&
                    it.second.setScale(2, RoundingMode.CEILING) != BigDecimal.ZERO.setScale(2, RoundingMode.CEILING)
            }
            .partition { it.second < BigDecimal.ZERO }

        val debtsSorted = debts.sortedBy { it.second }
        val debtsMessage = debtsSorted.joinToString(
            prefix = "📉 Ты должен отдать 📉\n",
            separator = "\n",
            transform = {
                "@${it.first} ${it.second.unaryMinus()}"
            }
        )
        val credsMessage = creds.sortedByDescending { it.second }.joinToString(
            prefix = "📈 Ты должен получить 📈\n",
            separator = "\n",
            transform = {
                "@${it.first} ${it.second}"
            }
        )
        val ending = "❗Чтобы отправить деньги - нажми на кнопки внизу❗"
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = sequenceOf(credsMessage, debtsMessage, ending).joinToString(separator = "\n"),
            replyMarkup = InlineKeyboardMarkup.create(
                debtsSorted.map {
                    InlineKeyboardButton.CallbackData(
                        text = "💸 @${it.first}",
                        callbackData = "${TransferMoneyCommand.commandName} ${it.first} ${it.second}",
                    )
                }.chunked(2)
            )
        )
        return emptySession
    }

    companion object {
        const val SHOW_DEBTS_COMMAND = "show_debts"
    }
}
