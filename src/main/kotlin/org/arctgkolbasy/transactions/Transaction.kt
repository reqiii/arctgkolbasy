package org.arctgkolbasy.transactions

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table
import org.arctgkolbasy.product.Product
import org.arctgkolbasy.user.User
import java.math.BigDecimal

@Entity
@Table(name = "transactions")
@NamedEntityGraph(
    name = "Transaction.detailed",
    attributeNodes = [
        NamedAttributeNode("from"),
        NamedAttributeNode("to"),
    ],
)
class Transaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id", insertable = false)
    val id: Long,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_id", nullable = false)
    val from: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_id", nullable = false)
    val to: User,
    @Column(name = "amount")
    val amount: BigDecimal,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinTable(
        name = "products_to_transactions",
        joinColumns = [JoinColumn(name = "transaction_id")],
        inverseJoinColumns = [JoinColumn(name = "product_id")],
    )
    val product: Product? = null,
)
