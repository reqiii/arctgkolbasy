package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import org.arctgkolbasy.bot.handler.UseProductCommand.Companion.USE_PRODUCT_COMMAND
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.product.ProductRepository
import org.arctgkolbasy.product.ProductService
import org.arctgkolbasy.user.UserService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull

@Component
class UseProductCommand(
    userService: UserService,
    @Qualifier("currentUserHolder")
    currentUserHolder: ThreadLocal<User?>,
    private val productRepository: ProductRepository,
    private val productService: ProductService,
) : SecuredCommand(
    userService = userService,
    currentUserHolder = currentUserHolder,
    isStateless = false,
) {
    override fun getCommandName(): String = USE_PRODUCT_COMMAND

    override fun checkUserAccess(user: User): Boolean = UserRoles.USER in user.roles

    override fun sessionCheck(sessionKey: String?, session: String?): Boolean = sessionKey in steps

    override fun handleUpdateInternal(user: User, bot: Bot, update: Update) {
        when (user.sessionKey) {
            UseCommandSteps.STEP_1_ENTER_ID.step -> stepOneChooseProduct(user, bot, update)
            UseCommandSteps.STEP_2_ENTER_EATEN_AMOUNT.step -> stepTwoEnterEatenAmount(user, bot, update)
            UseCommandSteps.STEP_2_ENTER_IS_ENDED.step -> stepTwoEnterIsEnded(user, bot, update)
            else -> stepZero(user, bot, update)
        }
    }

    private val keyboardReplyMarkup = KeyboardReplyMarkup(
        keyboard = listOf(
            listOf(
                KeyboardButton(IS_EATEN_ANSWER_YES),
                KeyboardButton(IS_EATEN_ANSWER_NO),
            )
        ),
        resizeKeyboard = true,
        oneTimeKeyboard = true,
    )

    private fun stepZero(user: User, bot: Bot, update: Update) {
        bot.sendMessage(chatId = update.chatIdUnsafe(), text = "Напиши id со стикера")
        user.updateSession(UseCommandSteps.STEP_1_ENTER_ID.step)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun stepOneChooseProduct(user: User, bot: Bot, update: Update) {
        val product = productRepository.findById(
            update.message().toLongOrNull() ?: throw IllegalArgumentException("Неправильный id, напиши число")
        ).getOrNull() ?: throw IllegalArgumentException("Продукт с этим id не найден, напиши число со стикера")
        if (product.isDivisible()) {
            bot.sendMessage(chatId = update.chatIdUnsafe(), text = "Напиши сколько ты съел?")
            user.updateSession(
                sessionKey = UseCommandSteps.STEP_2_ENTER_EATEN_AMOUNT.step,
                session = product.id.toString()
            )
        } else {
            bot.sendMessage(
                chatId = update.chatIdUnsafe(),
                text = "Ты доел ${product.name}?",
                replyMarkup = keyboardReplyMarkup,
            )
            user.updateSession(
                sessionKey = UseCommandSteps.STEP_2_ENTER_IS_ENDED.step,
                session = product.id.toString()
            )
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun stepTwoEnterEatenAmount(user: User, bot: Bot, update: Update) {
        val eatenAmount = update.message().toIntOrNull()
            ?: throw IllegalArgumentException("Неправильное количество, напиши число")
        val productId = user.session?.toLongOrNull()
            ?: return user.clearSession()
        val product = productRepository.findById(productId).getOrNull()
            ?: return user.clearSession()
        if (eatenAmount < 1 || eatenAmount > product.currentAmount) {
            throw IllegalArgumentException("Неправильное количество. Введи число от 1 до ${product.currentAmount}")
        }
        productService.consumeProduct(user.id, product.id, eatenAmount)
        bot.sendMessage(chatId = update.chatIdUnsafe(), text = "Я запомнил, что ты ел ${product.name}")
        user.clearSession()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun stepTwoEnterIsEnded(user: User, bot: Bot, update: Update) {
        val productId = user.session?.toLongOrNull() ?: return user.clearSession()
        val product = productRepository.findById(productId).getOrNull() ?: return user.clearSession()
        when (update.message()) {
            IS_EATEN_ANSWER_YES -> {
                productService.markProductAsEaten(productId)
                bot.sendMessage(
                    chatId = update.chatIdUnsafe(),
                    text = "Я запомнил, что ты доел ${product.name}",
                )
            }

            IS_EATEN_ANSWER_NO -> {
                bot.sendMessage(
                    chatId = update.chatIdUnsafe(),
                    text = "Я запомнил, что ты употреблял ${product.name}",
                )
            }

            else -> throw IllegalArgumentException("Я тебя не понял. Ответь '$IS_EATEN_ANSWER_YES' или '$IS_EATEN_ANSWER_NO'")
        }
        productService.consumeProduct(user.id, product.id, 1)
        user.clearSession()
    }

    companion object {
        val steps = UseCommandSteps.values().mapTo(mutableSetOf()) { it.step }
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
