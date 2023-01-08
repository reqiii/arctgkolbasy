package org.arctgkolbasy.user

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CrudRepository<User, Long> {
    @EntityGraph(value = "User.detail", type = EntityGraph.EntityGraphType.FETCH)
    @Query("select u from User u join u.roles where u.telegramId = :telegramId")
    fun findByTelegramId(@Param("telegramId") telegramId: Long): User?

    @EntityGraph(value = "User.detail", type = EntityGraph.EntityGraphType.FETCH)
    fun findByUsername(username: String): User?

    @EntityGraph(value = "User.detail", type = EntityGraph.EntityGraphType.FETCH)
    @Query("select u from User u join u.roles where u.id > :id order by u.id asc")
    fun getPageFromCursor(@Param("id") id: Long, pageable: Pageable): List<User>

    @Modifying
    @Query("update User u set u.sessionKey = :sessionKey, u.session = :session where u.id = :id")
    fun updateSession(
        @Param("id") id: Long,
        @Param("sessionKey") sessionKey: String?,
        @Param("session") session: String?,
    )
}
