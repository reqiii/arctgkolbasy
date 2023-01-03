package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.Update
import emoji.Emoji
import jakarta.annotation.PostConstruct
import org.arctgkolbasy.bot.user.Session
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.bot.user.emptySession
import org.arctgkolbasy.product.ProductRepository
import org.springframework.stereotype.Controller
import kotlin.jvm.optionals.getOrNull

@Controller
class BillCommand(
    val productRepository: ProductRepository,
) : StateMachine() {

    override fun getCommandName(): String = BILL

    override fun checkUserAccess(user: User): Boolean = UserRoles.USER in user.roles

    @PostConstruct
    fun init() {
        addSessionStep(BillCommandSteps.STEP_1_INPUT_ID.step, this::findBillById)
    }

    override fun stepZero(user: User, bot: Bot, update: Update): Session {
        val billPayload = update.message().substringAfter(BILL, "")
        if (billPayload.isEmpty()) {
            bot.sendMessage(update.chatIdUnsafe(), "Введи ${Emoji.ID_BUTTON.emoji} продукта:")
            return Session(BillCommandSteps.STEP_1_INPUT_ID.step)
        }
        val idBill = billPayload.toLongOrNull()
            ?: throw IllegalArgumentException("Неправильный ${Emoji.ID_BUTTON.emoji}.")
        sendBill(bot, update, idBill)
        return emptySession
    }

    private fun findBillById(user: User, bot: Bot, update: Update): Session {
        val idBill = update.message().toLongOrNull()
            ?: throw IllegalArgumentException("Неправильный ${Emoji.ID_BUTTON.emoji}")
        sendBill(bot, update, idBill)
        return emptySession
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun sendBill(bot: Bot, update: Update, idBill: Long) {
        bot.sendPhoto(
            chatId = update.chatIdUnsafe(),
            photo = TelegramFile.ByFileId(
                productRepository.findById(idBill).getOrNull()?.productImage
                    ?: throw IllegalArgumentException("Не найден продукт с таким id")
            ),
            caption = "Продукт находится в этом чеке ${Emoji.RECEIPT.emoji}\n" +
                "Вот ищи теперь${Emoji.FACE_WITH_MONOCLE.emoji}"
        )
    }

    enum class BillCommandSteps(
        val step: String,
    ) {
        STEP_1_INPUT_ID("${BILL}1"),
    }

    companion object {
        const val BILL = "bill"
    }
}
