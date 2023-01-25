package org.arctgkolbasy.bot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.Session
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.bot.user.emptySession
import org.arctgkolbasy.product.ProductRepository
import org.arctgkolbasy.transactions.TransactionService
import org.springframework.stereotype.Controller

@Controller
class RecalculateAllBalances(
    val productRepository: ProductRepository,
    val transactionService: TransactionService,
) : SecuredCommand {
    override fun getCommandName(): String = "recalculate_all"

    override fun checkUserAccess(user: User): Boolean = UserRoles.ADMIN in user.roles

    override fun handleUpdateInternal(user: User, bot: Bot, update: Update): Session {
        val ids = productRepository.findAll().map { it.id }
        transactionService.recalculateTransactionsForProducts(ids)
        return emptySession
    }
}
