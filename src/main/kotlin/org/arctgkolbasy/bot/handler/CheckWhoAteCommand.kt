package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import org.arctgkolbasy.bot.user.Session
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.bot.user.emptySession
import org.arctgkolbasy.consumer.ConsumerRepository
import org.arctgkolbasy.product.Product
import org.arctgkolbasy.product.ProductRepository
import org.arctgkolbasy.user.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Controller
import kotlin.jvm.optionals.getOrNull

@Controller
class CheckWhoAteCommand(
    val productRepository: ProductRepository,
    val consumerRepository: ConsumerRepository,
    val userRepository: UserRepository,
) : StateMachine() {

    override fun getCommandName(): String = CHECK_ATE

    override fun checkUserAccess(user: User): Boolean = UserRoles.USER in user.roles

    override fun stepZero(user: User, bot: Bot, update: Update): Session {
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = "Выбери продукт, который ты добавил",
            replyMarkup = chooseProductKeyboard(
                productRepository.findAllProductByBuyer(
                    buyerNick = userRepository.findByTelegramId(user.telegramId)!!,
                    cursor = -1,
                    pageable = Pageable.ofSize(100)
                )
            ),
        )
        return Session(ViewEatingCommandSteps.STEP_1_ENTER_ID.step)
    }

    init {
        addSessionStep(ViewEatingCommandSteps.STEP_1_ENTER_ID.step, ::stepOneChooseProduct)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun stepOneChooseProduct(user: User, bot: Bot, update: Update): Session {

        val product = productRepository.findById(
            update.callbackQuery?.data?.toLongOrNull()
                ?: throw IllegalArgumentException("Неправильный id или вы не добавляли продуты")
        ).getOrNull() ?: throw IllegalArgumentException("Продукт с этим id не найден")

        val result =
            consumerRepository.findAllByProductIdsAndUserWithOrderBy(product.id)
                .joinToString("\n") { "${it.consumer.username} - ${it.consumedAmount}шт" }

        val username = consumerRepository.findAllByProductIdsAndUserWithOrderBy(product.id)
            .joinToString { "${it.consumer.username}\n" }

        if (result.isEmpty()) {
            bot.editMessageText(
                chatId = update.chatId(),
                messageId = update.callbackQuery?.message?.messageId,
                text = "Ваш продукт ${product.name} никто не ел",
            )
        } else {
            if (product.isDivisible()) {
                bot.editMessageText(
                    chatId = update.chatId(),
                    messageId = update.callbackQuery?.message?.messageId,
                    text = "${product.name} ели: \n${result}",
                )
            } else {
                bot.editMessageText(
                    chatId = update.chatId(),
                    messageId = update.callbackQuery?.message?.messageId,
                    text = "${product.name} ели: \n${username}",
                )
            }
        }
        return emptySession
    }

    private fun chooseProductKeyboard(product: Iterable<Product>) = InlineKeyboardMarkup.create(
        product.map { p ->
            InlineKeyboardButton.CallbackData(
                text = "${p.id} ${p.name}",
                callbackData = p.id.toString(),
            )
        }.chunked(2)
    )

    enum class ViewEatingCommandSteps(
        val step: String,
    ) {
        STEP_1_ENTER_ID("${CHECK_ATE}1"),
    }

    companion object {
        const val CHECK_ATE = "check_who_ate"
    }

}
