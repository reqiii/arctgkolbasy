package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.Session
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.bot.user.emptySession
import org.arctgkolbasy.transactions.TransactionService
import org.arctgkolbasy.user.UserRepository
import org.springframework.stereotype.Controller
import java.math.BigDecimal

@Controller
class TransferMoneyCommand(
    userRepository: UserRepository,
    transactionService: TransactionService,
) : StateMachine() {

    override fun display() = false

    override fun stepZero(user: User, bot: Bot, update: Update): Session {
        val split = update.callbackQuery?.data?.trim()?.split(" ") ?: return emptySession
        if (split.size != 3) return emptySession
        val (command, username, credit) = split
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = "Твой долг перед @$username составляет ${
                credit.toBigDecimal().unaryMinus()
            }. Напиши сколько ты вернул?",
        )
        return Session(
            sessionKey = TransferMoneySteps.STEP_1_ENTER_AMOUNT.step,
            session = username,
        )
    }

    init {
        addSessionStep(TransferMoneySteps.STEP_1_ENTER_AMOUNT.step) { user, bot, update ->
            val amount = update.message().toBigDecimalOrNull()
                ?: throw IllegalArgumentException("неправильное число")
            if (amount <= BigDecimal.ZERO) throw IllegalArgumentException("неправильное число")
            val transferTo = userRepository.findByUsername(
                user.session ?: throw IllegalArgumentException("непонятно кому деньги")
            ) ?: throw IllegalArgumentException("непонятно кому деньги")
            transactionService.transferTo(
                from = user,
                to = transferTo,
                amount = amount,
            )
            transferTo.telegramChatId?.let { transferToChatId ->
                bot.sendMessage(
                    chatId = ChatId.fromId(transferToChatId),
                    text = "@${user.username} отправил тебе $amount",
                )
            }
            bot.sendMessage(
                chatId = update.chatIdUnsafe(),
                text = "Я запомнил, что ты отправил ${transferTo.username} $amount лари"
            )
            return@addSessionStep emptySession
        }
    }

    override fun getCommandName(): String = commandName

    override fun checkUserAccess(user: User): Boolean = UserRoles.USER in user.roles || UserRoles.ADMIN in user.roles

    companion object {
        const val commandName = "transfer"
    }
}

enum class TransferMoneySteps(
    val step: String,
) {
    STEP_1_ENTER_AMOUNT("${TransferMoneyCommand.commandName}1"),
}
