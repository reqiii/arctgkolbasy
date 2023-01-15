package org.arctgkolbasy.consumer

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import org.arctgkolbasy.product.Product
import org.arctgkolbasy.user.User
import java.io.Serializable

@Entity
@Table(name = "consumers")
class Consumer(
    @EmbeddedId
    val id: ConsumerId,
    @ManyToOne
    @JoinColumn(name = "consumer_id", nullable = false)
    @MapsId("userId")
    val consumer: User,
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @MapsId("productId")
    val product: Product,
    @Column(name = "consumed_amount")
    var consumedAmount: Int,
)

@Embeddable
data class ConsumerId(
    @Column(name = "user_id")
    val userId: Long,
    @Column(name = "product_id")
    val productId: Long,
) : Serializable
