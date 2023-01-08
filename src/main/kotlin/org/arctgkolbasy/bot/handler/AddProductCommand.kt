package org.arctgkolbasy.bot.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.handler.AddProductCommand.Companion.ADD_PRODUCT_COMMAND
import org.arctgkolbasy.bot.user.Session
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.bot.user.emptySession
import org.arctgkolbasy.product.Product
import org.arctgkolbasy.product.ProductRepository
import org.arctgkolbasy.user.UserRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class AddProductCommand(
    val objectMapper: ObjectMapper,
    val productRepository: ProductRepository,
    val userRepository: UserRepository,
) : StateMachine() {
    override fun getCommandName(): String = ADD_PRODUCT_COMMAND

    override fun checkUserAccess(user: User): Boolean = UserRoles.USER in user.roles

    override fun stepZero(user: User, bot: Bot, update: Update): Session {
        bot.sendMessage(update.chatIdUnsafe(), "Название продукта:")
        return Session(AddProductStates.STEP_1_ENTER_NAME.step)
    }

    init {
        addSessionStep(AddProductStates.STEP_1_ENTER_NAME.step, ::stepOneEnterName)
        addSessionStep(AddProductStates.STEP_2_ENTER_COST.step, ::stepTwoEnterCost)
        addSessionStep(AddProductStates.STEP_3_ENTER_AMOUNT.step, ::stepThreeEnterAmount)
        addSessionStep(AddProductStates.STEP_4_ENTER_IMAGE.step, ::stepFourEnterImage)
    }

    private fun stepOneEnterName(user: User, bot: Bot, update: Update): Session {
        val name = update.message()
        bot.sendMessage(update.chatIdUnsafe(), "Цена:")
        return Session(
            sessionKey = AddProductStates.STEP_2_ENTER_COST.step,
            session = objectMapper.writeValueAsString(AddProductSession(name = name))
        )
    }

    private fun stepTwoEnterCost(user: User, bot: Bot, update: Update): Session {
        val cost = update.message().toBigDecimalOrNull()
            ?: throw IllegalArgumentException("Неправильная цена. Отправь текстовое сообщение с ценой")
        if (cost < 0.toBigDecimal()) {
            throw IllegalArgumentException("Отправь цену не меньше 0")
        }
        val addProductSession = objectMapper.readValue<AddProductSession>(
            user.session ?: return emptySession
        ).copy(cost = cost)
        bot.sendMessage(chatId = update.chatIdUnsafe(), text = "Количество:")
        return Session(
            sessionKey = AddProductStates.STEP_3_ENTER_AMOUNT.step,
            session = objectMapper.writeValueAsString(addProductSession)
        )
    }

    private fun stepThreeEnterAmount(user: User, bot: Bot, update: Update): Session {
        val initialAmount = update.message().toIntOrNull()
            ?: throw IllegalArgumentException("Неправильное количество. Отправь текстовое сообщение с количеством")
        if (initialAmount < 1) {
            throw IllegalArgumentException("Отправь количество не меньше 1")
        }
        val addProductSession = objectMapper.readValue<AddProductSession>(user.session!!)
            .copy(initialAmount = initialAmount)
        bot.sendMessage(chatId = update.chatIdUnsafe(), text = "Фото чека или товара:")
        return Session(
            sessionKey = AddProductStates.STEP_4_ENTER_IMAGE.step,
            session = objectMapper.writeValueAsString(addProductSession)
        )
    }

    private fun stepFourEnterImage(user: User, bot: Bot, update: Update): Session {
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
                consumers = mutableSetOf(),
            )
        )
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = "Напиши на стикере id: '${product.id}'"
        )
        return emptySession
    }

    companion object {
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
