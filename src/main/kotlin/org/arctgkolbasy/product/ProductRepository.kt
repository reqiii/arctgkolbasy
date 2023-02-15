package org.arctgkolbasy.product

import org.arctgkolbasy.user.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : CrudRepository<Product, Long> {

    @Query("select p from Product p where p.id > :cursor and p.currentAmount <> :currentAmount order by p.id asc")
    fun findAllByCurrentAmountNotOrderById(
        @Param("cursor") cursor: Long,
        @Param("currentAmount") currentAmount: Int,
        pageable: Pageable
    ): List<Product>

    @Query("select p from Product p where p.id > :cursor and p.buyer = :buyer order by p.id asc")
    fun findAllProductByBuyer(
        @Param("cursor") cursor: Long,
        @Param("buyer") buyerNick: User,
        pageable: Pageable
    ): List<Product>
}
