package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import jakarta.annotation.PostConstruct
import org.arctgkolbasy.bot.handler.UseProductCommand.Companion.USE_PRODUCT_COMMAND
import org.arctgkolbasy.bot.user.Session
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.bot.user.emptySession
import org.arctgkolbasy.product.Product
import org.arctgkolbasy.product.ProductRepository
import org.arctgkolbasy.product.ProductService
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull

@Component
class UseProductCommand(
    private val productRepository: ProductRepository,
    private val productService: ProductService,
) : StateMachine() {
    override fun getCommandName(): String = USE_PRODUCT_COMMAND

    override fun checkUserAccess(user: User): Boolean = UserRoles.USER in user.roles

    override fun stepZero(user: User, bot: Bot, update: Update): Session {
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = "Выбери продукт или напиши id",
            replyMarkup = chooseProductKeyboard(
                productRepository.findAllByCurrentAmountNotOrderById(0)
            ),
        )
        return Session(UseCommandSteps.STEP_1_ENTER_ID.step)
    }

    @PostConstruct
    fun initSteps() {
        addSessionStep(UseCommandSteps.STEP_1_ENTER_ID.step, this::stepOneChooseProduct)
        addSessionStep(UseCommandSteps.STEP_2_ENTER_EATEN_AMOUNT.step, this::stepTwoEnterEatenAmount)
        addSessionStep(UseCommandSteps.STEP_2_ENTER_IS_ENDED.step, this::stepTwoEnterIsEnded)
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
            return Session(sessionKey = UseCommandSteps.STEP_2_ENTER_EATEN_AMOUNT.step, session = product.id.toString())
        } else {
            bot.editMessageText(
                chatId = update.chatId(),
                messageId = update.callbackQuery?.message?.messageId,
                text = "Ты доел ${product.name}?",
                replyMarkup = isEatenKeyboard(),
            )
            return Session(sessionKey = UseCommandSteps.STEP_2_ENTER_IS_ENDED.step, session = product.id.toString())
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun stepTwoEnterEatenAmount(user: User, bot: Bot, update: Update): Session {
        val amount = update.callbackQuery?.data?.toIntOrNull()
            ?: throw IllegalArgumentException("Неправильный id")
        val productId = user.session?.toLongOrNull()
            ?: throw IllegalArgumentException("Неправильный id")
        val product = productRepository.findById(productId).getOrNull()
            ?: throw IllegalArgumentException("Продукт не найден")
        if (amount < 1 || amount > product.currentAmount) {
            throw IllegalArgumentException("Неправильное количество. Введи число от 1 до ${product.currentAmount}")
        }
        productService.consumeProduct(user.id, product.id, amount)
        bot.editMessageText(
            chatId = update.chatId(),
            messageId = update.callbackQuery?.message?.messageId,
            text = "Я запомнил, что ты ел $amount ${product.name}"
        )
        return emptySession
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun stepTwoEnterIsEnded(user: User, bot: Bot, update: Update): Session {
        val productId = user.session?.toLongOrNull()
            ?: throw IllegalArgumentException("Неправильный id")
        val product = productRepository.findById(productId).getOrNull()
            ?: throw IllegalArgumentException("Продукт не найден")
        when (update.callbackQuery?.data) {
            IS_EATEN_ANSWER_YES -> {
                productService.markProductAsEaten(productId)
                bot.editMessageText(
                    chatId = update.chatId(),
                    messageId = update.callbackQuery?.message?.messageId,
                    text = "Я запомнил, что ты доел ${product.name}",
                )
            }

            IS_EATEN_ANSWER_NO -> {
                bot.editMessageText(
                    chatId = update.chatId(),
                    messageId = update.callbackQuery?.message?.messageId,
                    text = "Я запомнил, что ты употреблял ${product.name}",
                )
            }

            else -> throw IllegalArgumentException(
                "Я тебя не понял. Ответь '$IS_EATEN_ANSWER_YES' или '$IS_EATEN_ANSWER_NO'"
            )
        }
        productService.consumeProduct(user.id, product.id, 1)
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

    private fun enterAmountKeyboard(product: Product) = InlineKeyboardMarkup.create(
        (1..product.currentAmount)
            .map { number ->
                InlineKeyboardButton.CallbackData(
                    text = number.toString(),
                    callbackData = number.toString(),
                )
            }.chunked(3)
    )

    private fun isEatenKeyboard() = InlineKeyboardMarkup.createSingleRowKeyboard(
        InlineKeyboardButton.CallbackData(
            text = IS_EATEN_ANSWER_YES,
            callbackData = IS_EATEN_ANSWER_YES,
        ),
        InlineKeyboardButton.CallbackData(
            text = IS_EATEN_ANSWER_NO,
            callbackData = IS_EATEN_ANSWER_NO,
        ),
    )

    companion object {
        const val USE_PRODUCT_COMMAND = "use"
        const val IS_EATEN_ANSWER_YES = "да"
        const val IS_EATEN_ANSWER_NO = "нет"
    }
}

enum class UseCommandSteps(
    val step: String,
) {
    STEP_1_ENTER_ID("${USE_PRODUCT_COMMAND}1"),
    STEP_2_ENTER_EATEN_AMOUNT("${USE_PRODUCT_COMMAND}2.1"),
    STEP_2_ENTER_IS_ENDED("${USE_PRODUCT_COMMAND}2.2"),
}
