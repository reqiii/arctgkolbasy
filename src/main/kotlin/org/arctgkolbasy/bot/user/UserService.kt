package org.arctgkolbasy.bot.user

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.github.kotlintelegrambot.entities.User as TgApiUser
import org.arctgkolbasy.bot.user.model.User as DbUser

@Service
class UserService(
    val userRepository: UserRepository,
    val roleRepository: RoleRepository,
){

    @Transactional
    fun getOrCreateUser(tgApiUser: TgApiUser): User {
        var user = userRepository.findByTelegramId(tgApiUser.id)
        if (user != null) {
            user.firstName = tgApiUser.firstName
            user.lastName = tgApiUser.lastName
            user.username = tgApiUser.username
        } else {
            user = userRepository.save(
                DbUser(
                    id = 0,
                    telegramId = tgApiUser.id,
                    isBot = tgApiUser.isBot,
                    firstName = tgApiUser.firstName,
                    lastName = tgApiUser.lastName,
                    username = tgApiUser.username,
                    roles = setOf(
                        roleRepository.findByRoleName(UserRoles.GUEST) ?: throw IllegalStateException("no role found"),
                    ),
                )
            )
        }
        return User(
            user.id,
            user.telegramId,
            user.isBot,
            user.firstName,
            user.lastName,
            user.username,
            user.roles.map { it.roleName }.toSet()
        )
    }
}
