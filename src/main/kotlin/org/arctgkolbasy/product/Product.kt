package org.arctgkolbasy.product

import jakarta.persistence.*
import org.arctgkolbasy.consumer.Consumer
import org.arctgkolbasy.transactions.Transaction
import org.arctgkolbasy.user.User
import java.math.BigDecimal

@Entity
@Table(name = "products")
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", insertable = false)
    val id: Long,
    @Column(name = "name")
    val name: String,
    @Column(name = "cost")
    val cost: BigDecimal,
    @Column(name = "initial_amount")
    val initialAmount: Int,
    @Column(name = "current_amount")
    var currentAmount: Int,
    @Column(name = "product_image")
    val productImage: String,
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "buyer_id")
    val buyer: User,
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = true)
    @MapsId("consumerId")
    val consumers: Set<Consumer>,
    @OneToMany(
        mappedBy = "product",
        targetEntity = Transaction::class,
        fetch = FetchType.LAZY
    )
    val transactions: List<Transaction>,
) {
    fun isDivisible(): Boolean = initialAmount != 1
}
