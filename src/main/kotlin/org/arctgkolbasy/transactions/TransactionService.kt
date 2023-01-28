package org.arctgkolbasy.transactions

import org.arctgkolbasy.consumer.Consumer
import org.arctgkolbasy.consumer.ConsumerRepository
import org.arctgkolbasy.product.Product
import org.arctgkolbasy.product.ProductRepository
import org.arctgkolbasy.product.ProductService
import org.arctgkolbasy.user.User
import org.arctgkolbasy.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.jvm.optionals.getOrNull
import org.arctgkolbasy.bot.user.User as TgApiUser

@Service
class TransactionService(
    private val transactionsRepository: TransactionsRepository,
    private val userRepository: UserRepository,
    private val consumerRepository: ConsumerRepository,
    private val productService: ProductService,
    private val productRepository: ProductRepository,
) {

    private val log = LoggerFactory.getLogger(this::class.java)!!

    @Transactional
    fun recalculateTransactionsForProducts(
        productIds: List<Long>,
    ): List<Transaction> {
        val transactionsToOverwrite = consumerRepository.findAllByProductIds(productIds)
            .map { consumer ->
                Transaction(
                    id = -1,
                    from = consumer.consumer,
                    to = consumer.product.buyer,
                    amount = calculateAmount(consumer).unaryMinus(),
                    product = consumer.product,
                )
            }
        val transactionsToDelete = transactionsRepository.findAllByProductIds(productIds)
        transactionsRepository.deleteAllByIdInBatch(transactionsToDelete.map { it.id })
        log.info("Deleted {} transactions", transactionsToDelete.size)
        val savedTransactions = transactionsRepository.saveAll(transactionsToOverwrite)
        log.info("Saved {} transactions", savedTransactions.size)
        return savedTransactions
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Transactional
    fun consumeProduct(productId: Long, consumerId: Long, amountRaw: Int): Product {
        val product = productRepository.findById(productId).getOrNull() ?: throw IllegalStateException()
        val consumer = userRepository.findById(consumerId).getOrNull() ?: throw IllegalStateException()
        val amount = Integer.min(product.currentAmount, amountRaw)
        val consumedProduct = productService.consumeProduct(consumer, product, amount)
        if (consumedProduct.isDivisible()) {
            transactionsRepository.saveAndFlush(
                Transaction(
                    id = -1,
                    from = consumer,
                    to = product.buyer,
                    amount = calculateAmount(consumedProduct, amount).unaryMinus(),
                    product = product,
                )
            )
        } else if (consumedProduct.currentAmount == 0) {
            consumedProduct.consumers.forEach { c ->
                transactionsRepository.saveAndFlush(
                    Transaction(
                        id = -1,
                        from = c.consumer,
                        to = consumedProduct.buyer,
                        amount = calculateAmount(consumedProduct, amount).unaryMinus(),
                        product = consumedProduct,
                    )
                )
            }
        }
        return consumedProduct
    }

    private fun calculateAmount(consumer: Consumer): BigDecimal =
        if (consumer.product.isDivisible()) {
            consumer.product.cost
                .divide(consumer.product.initialAmount.toBigDecimal(), RoundingMode.CEILING)
                .multiply(consumer.consumedAmount.toBigDecimal())
        } else if (consumer.product.currentAmount == 0) {
            consumer.product.cost
                .divide(consumer.product.consumers.size.toBigDecimal(), RoundingMode.CEILING)
        } else {
            BigDecimal.ZERO
        }

    private fun calculateAmount(product: Product, consumedAmount: Int): BigDecimal =
        if (product.isDivisible()) {
            product.cost
                .divide(product.initialAmount.toBigDecimal(), RoundingMode.CEILING)
                .multiply(consumedAmount.toBigDecimal())
        } else {
            product.cost
                .divide(product.consumers.size.toBigDecimal(), RoundingMode.CEILING)
        }

    @OptIn(ExperimentalStdlibApi::class)
    fun transferTo(from: TgApiUser, to: User, amount: BigDecimal) {
        transactionsRepository.save(
            Transaction(
                id = -1,
                from = userRepository.findById(from.id).getOrNull()
                    ?: throw IllegalArgumentException("непонятно от кого"),
                to = to,
                amount = amount,
            )
        )
    }
}
