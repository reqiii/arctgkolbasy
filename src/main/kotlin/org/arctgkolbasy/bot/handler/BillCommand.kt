package org.arctgkolbasy.bot.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.Update
import emoji.Emoji
import org.arctgkolbasy.bot.handler.ProductsInStockCommand.Companion.ALL_PRODUCT
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.product.Product
import org.arctgkolbasy.product.ProductRepository
import org.arctgkolbasy.user.UserService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller

@Controller
class BillCommand(
    userService: UserService,
    @Qualifier("currentUserHolder")
    currentUserHolder: ThreadLocal<User?>,
    val productRepository: ProductRepository,
    val objectMapper: ObjectMapper,
) : SecuredCommand(
    userService = userService,
    currentUserHolder = currentUserHolder,
    isStateless = false,
) {
    override fun getCommandName(): String = BILL

    override fun sessionCheck(sessionKey: String?, session: String?): Boolean = sessionKey in steps

    override fun checkUserAccess(user: User): Boolean = UserRoles.USER in user.roles

    override fun handleUpdateInternal(user: User, bot: Bot, update: Update) = when (user.sessionKey) {
        BillCommandSteps.STEP_1_INPUT_ID.step -> findBillById(update, bot, user)
        else -> stepOneInputIdOrSendPhoto(update, bot, user)
    }

    private fun findBillById(update: Update, bot: Bot, user: User) {
        if (update.message().startsWith("/")) {
            stepOneInputIdOrSendPhoto(update, bot, user)
        } else {
            val idBill = update.message().toLongOrNull()
                ?: throw IllegalArgumentException("Неправильный ${Emoji.ID_BUTTON.emoji}. Введи корректный:")
            if (productRepository.findById(idBill).isEmpty) {
                throw IllegalArgumentException(
                    "Нет такого продукта${Emoji.PERSON_SHRUGGING.emoji}.\n" +
                            "Введи еще раз или посмотри список продуктов${Emoji.MAGNIFYING_GLASS_TILTED_LEFT.emoji} " +
                            "/${ALL_PRODUCT}"
                )
            } else {
                sendBillAndClearSession(bot, update, idBill, user)
            }
        }
    }

    private fun stepOneInputIdOrSendPhoto(update: Update, bot: Bot, user: User) {
        if (update.message().substringAfter(BILL, "").isEmpty()) {
            bot.sendMessage(update.chatIdUnsafe(), "Введи ${Emoji.ID_BUTTON.emoji} продукта:")
            user.updateSession(
                BillCommandSteps.STEP_1_INPUT_ID.step,
                null
            )
        } else {
            val idBill = update.message().substringAfter(BILL).toLongOrNull()
                ?: throw IllegalArgumentException("Неправильный ${Emoji.ID_BUTTON.emoji}.")
            sendBillAndClearSession(bot, update, idBill, user)
        }
    }

    private fun sendBillAndClearSession(bot: Bot, update: Update, idBill: Long?, user: User) {
        bot.sendPhoto(
            chatId = update.chatIdUnsafe(),
            TelegramFile.ByFileId(
                productRepository.findById(idBill!!).get().productImage
            ),
            caption = "Продукт находится в этом чеке ${Emoji.RECEIPT.emoji}\n" +
                    "Вот ищи теперь${Emoji.FACE_WITH_MONOCLE.emoji}"
        )
        user.clearSession()
    }

    enum class BillCommandSteps(
        val step: String,
    ) {
        STEP_1_INPUT_ID("${BILL}1"),
    }

    data class InputIdSession(
        val id: Long,
    )

    companion object {
        val steps = BillCommandSteps.values().mapTo(mutableSetOf()) { it.step }
        const val BILL = "bill"
    }
}
