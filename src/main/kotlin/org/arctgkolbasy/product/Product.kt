package org.arctgkolbasy.product

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
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
    var name: String,
    @Column(name = "cost")
    var cost: BigDecimal,
    @Column(name = "initial_amount")
    val initialAmount: Int,
    @Column(name = "current_amount")
    var currentAmount: Int,
    @Column(name = "product_image")
    var productImage: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    var buyer: User,
) {
    fun isDivisible(): Boolean = initialAmount != 1
}
