package org.arctgkolbasy.bot.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import org.arctgkolbasy.bot.handler.UseProductCommand.Companion.USE_PRODUCT_COMMAND
import org.arctgkolbasy.bot.user.Session
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.bot.user.emptySession
import org.arctgkolbasy.consumer.ConsumerRepository
import org.arctgkolbasy.product.Product
import org.arctgkolbasy.product.ProductRepository
import org.arctgkolbasy.product.ProductService
import org.arctgkolbasy.transactions.TransactionService
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull

@Component
class UseProductCommand(
    private val productRepository: ProductRepository,
    private val consumerRepository: ConsumerRepository,
    private val productService: ProductService,
    private val transactionService: TransactionService,
    private val objectMapper: ObjectMapper,
) : StateMachine() {
    override fun getCommandName(): String = USE_PRODUCT_COMMAND

    override fun checkUserAccess(user: User): Boolean = UserRoles.USER in user.roles

    override fun stepZero(user: User, bot: Bot, update: Update): Session {
        val consumed = consumerRepository.findConsumedProductIdsByConsumerId(user.id)
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = "Выбери продукт или напиши id",
            replyMarkup = chooseProductKeyboard(
                productRepository.findAllByCurrentAmountNotOrderById(
                    cursor = -1,
                    currentAmount = 0,
                    pageable = Pageable.ofSize(100)
                ),
                consumed.mapTo(HashSet()) { it.id.productId },
            ),
        )
        return Session(UseCommandSteps.STEP_1_ENTER_ID.step)
    }

    init {
        addSessionStep(UseCommandSteps.STEP_1_ENTER_ID.step, ::stepOneChooseProduct)
        addSessionStep(UseCommandSteps.STEP_2_ENTER_EATEN_AMOUNT.step, ::stepTwoEnterEatenAmount)
        addSessionStep(UseCommandSteps.STEP_2_ENTER_IS_ENDED.step, ::stepTwoEnterIsEnded)
        addSessionStep(UseCommandSteps.STEP_3_APPROVE.step, ::stepThreeApprove)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun stepOneChooseProduct(user: User, bot: Bot, update: Update): Session {
        val product = productRepository.findById(
            update.callbackQuery?.data?.toLongOrNull() ?: throw IllegalArgumentException("Неправильный id")
        ).getOrNull() ?: throw IllegalArgumentException("Продукт с этим id не найден")
        if (product.isDivisible()) {
            bot.editMessageText(
                chatId = update.chatId(),
                messageId = update.callbackQuery?.message?.messageId,
                text = "Сколько ты съел ${product.name}?",
                replyMarkup = enterAmountKeyboard(product)
            )
            return Session(
                sessionKey = UseCommandSteps.STEP_2_ENTER_EATEN_AMOUNT.step,
                session = objectMapper.writeValueAsString(
                    UseProductSessionStep1(
                        id = product.id,
                    )
                )
            )
        } else {
            bot.editMessageText(
                chatId = update.chatId(),
                messageId = update.callbackQuery?.message?.messageId,
                text = "Ты доел ${product.name}?",
                replyMarkup = yesOrNoKeyboard,
            )
            return Session(
                sessionKey = UseCommandSteps.STEP_2_ENTER_IS_ENDED.step,
                session = objectMapper.writeValueAsString(
                    UseProductSessionStep1(
                        id = product.id,
                    )
                )
            )
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun stepTwoEnterEatenAmount(user: User, bot: Bot, update: Update): Session {
        val amount = update.callbackQuery?.data?.toIntOrNull()
            ?: throw IllegalArgumentException("Неправильное количество")
        val useProductSession: UseProductSessionStep1 = deserializeSession(user)
        val productId = useProductSession.id
        val product = productRepository.findById(productId).getOrNull()
            ?: throw IllegalArgumentException("Продукт не найден")
        if (amount < 1 || amount > product.currentAmount) {
            throw IllegalArgumentException("Неправильное количество. Введи число от 1 до ${product.currentAmount}")
        }
        bot.editMessageText(
            chatId = update.chatId(),
            messageId = update.callbackQuery?.message?.messageId,
            text = "Ты точно съел $amount ${product.name}?",
            replyMarkup = yesOrNoKeyboard,
        )
        return Session(
            sessionKey = UseCommandSteps.STEP_3_APPROVE.step,
            session = objectMapper.writeValueAsString(
                UseProductSessionStep2(
                    id = useProductSession.id,
                    productName = product.name,
                    eatenAmount = amount,
                )
            )
        )
    }

    private fun stepTwoEnterIsEnded(user: User, bot: Bot, update: Update): Session {
        val useProductSession: UseProductSessionStep1 = deserializeSession(user)
        val productId = useProductSession.id
        val product = transactionService.consumeProduct(
            productId = productId,
            consumerId = user.id,
            amountRaw = 1
        )
        when (update.callbackQuery?.data) {
            CALLBACK_DATA_YES -> {
                productService.markProductAsEaten(productId)
                transactionService.recalculateTransactionsForProducts(listOf(productId))
                bot.editMessageText(
                    chatId = update.chatId(),
                    messageId = update.callbackQuery?.message?.messageId,
                    text = "Я запомнил, что ты доел ${product.name}",
                )
            }

            CALLBACK_DATA_NO -> {
                bot.editMessageText(
                    chatId = update.chatId(),
                    messageId = update.callbackQuery?.message?.messageId,
                    text = "Я запомнил, что ты употреблял ${product.name}",
                )
            }

            else -> throw IllegalArgumentException(
                "Я тебя не понял. Ответь '$CALLBACK_DATA_YES' или '$CALLBACK_DATA_NO'"
            )
        }
        return emptySession
    }

    private fun stepThreeApprove(user: User, bot: Bot, update: Update): Session {
        val useProductSession: UseProductSessionStep2 = deserializeSession(user)
        when (update.callbackQuery?.data) {
            CALLBACK_DATA_YES -> {
                transactionService.consumeProduct(
                    productId = useProductSession.id,
                    consumerId = user.id,
                    amountRaw = useProductSession.eatenAmount
                )
                bot.editMessageText(
                    chatId = update.chatId(),
                    messageId = update.callbackQuery?.message?.messageId,
                    text = "Я запомнил, что ты съел ${useProductSession.eatenAmount} ${useProductSession.productName}",
                )
            }

            CALLBACK_DATA_NO -> {
                bot.editMessageText(
                    chatId = update.chatId(),
                    messageId = update.callbackQuery?.message?.messageId,
                    text = "Не ошибается тот кто ничего не делает",
                )
            }
        }
        return emptySession
    }

    private fun chooseProductKeyboard(product: Iterable<Product>, consumed: Set<Long>) = InlineKeyboardMarkup.create(
        product.map { p ->
            InlineKeyboardButton.CallbackData(
                text = "${emojiIfConsumed(p, consumed)}${p.id} ${p.name}",
                callbackData = p.id.toString(),
            )
        }.chunked(2)
    )

    private fun emojiIfConsumed(p: Product, consumed: Set<Long>) =
        if (p.id in consumed) "🍽 " else ""

    private fun enterAmountKeyboard(product: Product) = InlineKeyboardMarkup.create(
        (1..product.currentAmount)
            .map { number ->
                InlineKeyboardButton.CallbackData(
                    text = number.toString(),
                    callbackData = number.toString(),
                )
            }.chunked(3)
    )

    private inline fun <reified T> deserializeSession(user: User) = objectMapper.readValue<T>(
        user.session ?: throw IllegalArgumentException("Пустая сессия")
    )

    companion object {
        const val USE_PRODUCT_COMMAND = "use"
        const val CALLBACK_DATA_YES = "да"
        const val CALLBACK_DATA_NO = "нет"
        val yesOrNoKeyboard = InlineKeyboardMarkup.createSingleRowKeyboard(
            InlineKeyboardButton.CallbackData(
                text = CALLBACK_DATA_YES,
                callbackData = CALLBACK_DATA_YES,
            ),
            InlineKeyboardButton.CallbackData(
                text = CALLBACK_DATA_NO,
                callbackData = CALLBACK_DATA_NO,
            ),
        )
    }
}

open class UseProductSessionStep1(
    val id: Long,
)

open class UseProductSessionStep2(
    id: Long,
    val productName: String,
    val eatenAmount: Int,
) : UseProductSessionStep1(id = id)

enum class UseCommandSteps(
    val step: String,
) {
    STEP_1_ENTER_ID("${USE_PRODUCT_COMMAND}1"),
    STEP_2_ENTER_EATEN_AMOUNT("${USE_PRODUCT_COMMAND}2.1"),
    STEP_2_ENTER_IS_ENDED("${USE_PRODUCT_COMMAND}2.2"),
    STEP_3_APPROVE("${USE_PRODUCT_COMMAND}3"),
}
