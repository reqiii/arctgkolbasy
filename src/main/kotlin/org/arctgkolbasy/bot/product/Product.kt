package org.arctgkolbasy.bot.product

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.arctgkolbasy.bot.user.model.User
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
    val currentAmount: Int,
    @Column(name = "product_image")
    val productImage: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    val buyer: User,
) {
    constructor() : this(
        -1,
        "",
        BigDecimal.ZERO,
        -1,
        -1,
        "",
        User()
    )
}
