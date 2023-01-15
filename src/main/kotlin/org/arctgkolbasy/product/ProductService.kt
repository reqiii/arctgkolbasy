package org.arctgkolbasy.product

import org.arctgkolbasy.consumer.Consumer
import org.arctgkolbasy.consumer.ConsumerId
import org.arctgkolbasy.consumer.ConsumerRepository
import org.arctgkolbasy.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.Integer.min
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

@Service
class ProductService(
    val consumerRepository: ConsumerRepository,
    val productRepository: ProductRepository,
    val userRepository: UserRepository,
) {
    @OptIn(ExperimentalStdlibApi::class)
    @Transactional
    fun consumeProduct(userId: Long, productId: Long, consumedAmountRaw: Int): Product {
        val user = userRepository.findById(userId).getOrNull()
            ?: throw IllegalStateException("Нет пользователя с id '$userId'")
        val product = productRepository.findById(productId).getOrNull()
            ?: throw IllegalStateException("Нет продукта с id '$productId'")
        val consumerId = ConsumerId(userId, productId)
        val consumedAmount = min(product.currentAmount, consumedAmountRaw)
        val consumer = consumerRepository.findById(consumerId).getOrElse {
            consumerRepository.save(
                Consumer(
                    id = consumerId,
                    consumer = user,
                    product = product,
                    consumedAmount = 0,
                )
            )
        }
        if (product.isDivisible()) {
            consumer.consumedAmount += consumedAmount
            product.currentAmount -= consumedAmount
        } else {
            consumer.consumedAmount = 1
        }
        return product
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Transactional
    fun markProductAsEaten(productId: Long) {
        val product = productRepository.findById(productId).getOrNull()
            ?: throw IllegalStateException("Нет продукта с id '$productId'")
        if (!product.isDivisible()) {
            product.currentAmount = 0
        }
    }
}
