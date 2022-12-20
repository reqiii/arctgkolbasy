package org.arctgkolbasy.bot.user

import org.arctgkolbasy.bot.user.model.User
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CrudRepository<User, Long> {
    fun findByTelegramId(id: Long): User?
    @Modifying
    @Query("update User u set u.sessionKey = :sessionKey, u.session = :session where u.id = :id")
    fun updateSession(
        @Param("id") id: Long,
        @Param("sessionKey") sessionKey: String?,
        @Param("session") session: String?,
    )
}
