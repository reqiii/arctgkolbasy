package org.arctgkolbasy.bot.user

import org.arctgkolbasy.bot.user.model.User
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository: CrudRepository<User, Long> {
    fun findByTelegramId(id: Long): User?
}
