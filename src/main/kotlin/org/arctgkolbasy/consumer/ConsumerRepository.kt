package org.arctgkolbasy.consumer

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface ConsumerRepository : CrudRepository<Consumer, ConsumerId> {
    @Query("select c from Consumer c where c.id.userId = :consumerId")
    fun findConsumedProductIdsByConsumerId(@Param("consumerId") consumerId: Long): Set<Consumer>

    @Query("select c from Consumer c where c.product.id in :productIds")
    fun findAllByProductIds(
        @Param("productIds") productIds: List<Long>,
    ): List<Consumer>
}
