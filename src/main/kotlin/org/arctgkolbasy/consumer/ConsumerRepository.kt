package org.arctgkolbasy.consumer

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface ConsumerRepository: CrudRepository<Consumer, ConsumerId> {
    @Query("select c from Consumer c where c.product.id in :productIds")
    fun findAllByProductIds(
        @Param("productIds") productIds: List<Long>,
    ): List<Consumer>

    @Query("select c from Consumer c JOIN FETCH c.consumer where c.product.id in :productIds order by c.consumedAmount")
    fun findAllByProductIdsAndUserWithOrderBy(
        @Param("productIds") productIds: Long,
    ): List<Consumer>
}
