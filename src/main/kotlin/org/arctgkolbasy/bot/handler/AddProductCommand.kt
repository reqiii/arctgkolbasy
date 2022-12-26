package org.arctgkolbasy.bot.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.handler.AddProductCommand.Companion.ADD_PRODUCT_COMMAND
import org.arctgkolbasy.product.Product
import org.arctgkolbasy.product.ProductRepository
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.user.UserRepository
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.user.UserService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class AddProductCommand(
    userService: UserService,
    @Qualifier("currentUserHolder")
    currentUserHolder: ThreadLocal<User?>,
    val objectMapper: ObjectMapper,
    val productRepository: ProductRepository,
    val userRepository: UserRepository,
) : SecuredCommand(
    userService = userService,
    currentUserHolder = currentUserHolder,
    isStateless = false,
) {
    override fun getCommandName(): String = ADD_PRODUCT_COMMAND

    override fun checkUserAccess(user: User): Boolean = UserRoles.USER in user.roles

    override fun sessionCheck(sessionKey: String?, session: String?): Boolean = sessionKey in steps

    override fun handleUpdateInternal(user: User, bot: Bot, update: Update) = when (user.sessionKey) {
        AddProductStates.STEP_1_ENTER_NAME.step -> stepOneEnterName(update, bot, user)
        AddProductStates.STEP_2_ENTER_COST.step -> stepTwoEnterCost(update, bot, user)
        AddProductStates.STEP_3_ENTER_AMOUNT.step -> stepThreeEnterAmount(update, bot, user)
        AddProductStates.STEP_4_ENTER_IMAGE.step -> stepFourEnterImage(update, bot, user)
        else -> stepZero(bot, update, user)
    }

    private fun stepZero(bot: Bot, update: Update, user: User) {
        bot.sendMessage(update.chatIdUnsafe(), "Название продукта:")
        user.updateSession(
            AddProductStates.STEP_1_ENTER_NAME.step,
            objectMapper.writeValueAsString(AddProductSession())
        )
    }

    private fun stepOneEnterName(update: Update, bot: Bot, user: User) {
        val name = update.message()
        if (name.startsWith("/")) {
            throw IllegalArgumentException("Неправильное название. Отправь текстовое сообщение с названием")
        }
        bot.sendMessage(update.chatIdUnsafe(), "Цена:")
        user.updateSession(
            AddProductStates.STEP_2_ENTER_COST.step,
            objectMapper.writeValueAsString(AddProductSession(name = name))
        )
    }

    private fun stepTwoEnterCost(update: Update, bot: Bot, user: User) {
        val cost = try {
            val cost = update.message()
            if (cost.isEmpty()) {
                throw IllegalArgumentException("Неправильная цена. Отправь текстовое сообщение с ценой")
            }
            BigDecimal(cost)
        } catch (e: Exception) {
            throw IllegalArgumentException("Неправильная цена. Отправь текстовое сообщение с ценой", e)
        }
        val addProductSession = objectMapper.readValue<AddProductSession>(user.session!!).copy(cost = cost)
        bot.sendMessage(chatId = update.chatIdUnsafe(), text = "Количество:")
        user.updateSession(
            AddProductStates.STEP_3_ENTER_AMOUNT.step,
            objectMapper.writeValueAsString(addProductSession)
        )
    }

    private fun stepThreeEnterAmount(update: Update, bot: Bot, user: User) {
        val initialAmount = try {
            update.message().toInt()
        } catch (e: Exception) {
            throw IllegalArgumentException("Неправильное количество. Отправь текстовое сообщение с количеством", e)
        }
        val addProductSession = objectMapper.readValue<AddProductSession>(user.session!!)
            .copy(initialAmount = initialAmount)
        bot.sendMessage(chatId = update.chatIdUnsafe(), text = "Фото чека или товара:")
        user.updateSession(
            AddProductStates.STEP_4_ENTER_IMAGE.step,
            objectMapper.writeValueAsString(addProductSession)
        )
    }

    private fun stepFourEnterImage(update: Update, bot: Bot, user: User) {
        val image = update.message?.photo?.maxByOrNull { it.height + it.width }?.fileId
            ?: throw IllegalArgumentException("Неправильное фото. Отправь фото чека или товара")
        val addProductSession = objectMapper.readValue<AddProductSession>(user.session!!).copy(image = image)
        val product = productRepository.save(
            Product(
                id = -1,
                name = addProductSession.name!!,
                cost = addProductSession.cost!!,
                initialAmount = addProductSession.initialAmount!!,
                currentAmount = addProductSession.initialAmount!!,
                productImage = addProductSession.image!!,
                buyer = userRepository.findByTelegramId(user.telegramId)!!,
            )
        )
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = "Напиши на стикере id: '${product.id}'"
        )
        user.clearSession()
    }

    companion object {
        val steps: Set<String> = AddProductStates.values().map { it.step }.toSet()
        const val ADD_PRODUCT_COMMAND = "add_product"
    }
}

enum class AddProductStates(
    val step: String,
) {
    STEP_1_ENTER_NAME("${ADD_PRODUCT_COMMAND}1"),
    STEP_2_ENTER_COST("${ADD_PRODUCT_COMMAND}2"),
    STEP_3_ENTER_AMOUNT("${ADD_PRODUCT_COMMAND}3"),
    STEP_4_ENTER_IMAGE("${ADD_PRODUCT_COMMAND}4"),
}

data class AddProductSession(
    var name: String? = null,
    var cost: BigDecimal? = null,
    var initialAmount: Int? = null,
    var image: String? = null,
)
