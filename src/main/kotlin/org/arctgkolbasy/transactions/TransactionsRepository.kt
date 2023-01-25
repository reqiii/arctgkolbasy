package org.arctgkolbasy.transactions

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TransactionsRepository : JpaRepository<Transaction, Long> {
    @EntityGraph(value = "Transaction.detailed", type = EntityGraph.EntityGraphType.FETCH)
    @Query("select t from Transaction t join Product p where t.product.id in :productIds")
    fun findAllByProductIds(
        @Param("productIds") productIds: List<Long>,
    ): List<Transaction>

    @EntityGraph(value = "Transaction.detailed", type = EntityGraph.EntityGraphType.FETCH)
    @Query("select t from Transaction t where t.from.id = :fromId order by t.id")
    fun findAllFromUserId(@Param("fromId") id: Long): List<Transaction>

    @EntityGraph(value = "Transaction.detailed", type = EntityGraph.EntityGraphType.FETCH)
    @Query("select t from Transaction t where t.to.id = :toId order by t.id")
    fun findAllToUserId(@Param("toId") id: Long): List<Transaction>
}
