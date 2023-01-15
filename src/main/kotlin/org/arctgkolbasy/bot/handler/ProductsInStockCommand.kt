package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import emoji.Emoji
import org.arctgkolbasy.bot.extensions.escapeForMarkdownV2
import org.arctgkolbasy.bot.handler.BillCommand.Companion.BILL
import org.arctgkolbasy.bot.user.Session
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.bot.user.emptySession
import org.arctgkolbasy.product.Product
import org.arctgkolbasy.product.ProductRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode

@Controller
class ProductsInStockCommand(
    val productRepository: ProductRepository,
) : SecuredCommand {
    override fun getCommandName(): String = ALL_PRODUCT

    override fun checkUserAccess(user: User): Boolean = UserRoles.USER in user.roles

    override fun handleUpdateInternal(user: User, bot: Bot, update: Update): Session {
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = productRepository.findAllByCurrentAmountNotOrderById(
                cursor = -1,
                currentAmount = 0,
                pageable = Pageable.ofSize(100)
            ).joinToString(
                prefix = "Продукты в наличии\\:\n",
                separator = "\n",
                transform = { product ->
                    getEmoji(product) +
                        "${Emoji.ID_BUTTON.emoji}*${product.id}* \\- _${product.name.escapeForMarkdownV2()}_ " +
                        "осталось\\: _${product.currentAmount}_, " +
                        getPrice(product) +
                        ", чек\\- \\/${BILL}${product.id}"
                }
            ),
            parseMode = ParseMode.MARKDOWN_V2,
        )
        return emptySession
    }

    fun getPrice(product: Product): String {
        return when (product.isDivisible()) {
            false -> "не делимый, его стоимость\\: ${
                product.cost.toString().replace(".", "\\.")
            }${Emoji.DOLLAR_BANKNOTE.emoji}"

            else -> "можно поделить\\: ${
                (product.cost.divide(product.initialAmount.toBigDecimal(), 2, RoundingMode.HALF_UP)).toString()
                    .replace(".", "\\.")
            }\\/шт, его стоимость\\: ${product.cost.toString().replace(".", "\\.")}${Emoji.DOLLAR_BANKNOTE.emoji}"
        }
    }

    fun getEmoji(product: Product): String {
        val dif = (product.currentAmount.toDouble() / product.initialAmount) * 100
        return when (dif.toInt()) {
            100 -> Emoji.GREEN_CIRCLE.emoji
            in 25..99 -> Emoji.YELLOW_CIRCLE.emoji
            else -> Emoji.RED_CIRCLE.emoji
        }
    }

    companion object {
        const val ALL_PRODUCT = "product_in_stock"
    }
}
