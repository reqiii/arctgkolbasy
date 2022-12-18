package org.arctgkolbasy.bot.user

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.IllegalArgumentException
import com.github.kotlintelegrambot.entities.User as TgApiUser
import org.arctgkolbasy.bot.user.model.User as DbUser

@Service
class UserService(
    val userRepository: UserRepository,
    val roleRepository: RoleRepository,
) {
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
                    id = -1,
                    telegramId = tgApiUser.id,
                    isBot = tgApiUser.isBot,
                    firstName = tgApiUser.firstName,
                    lastName = tgApiUser.lastName,
                    username = tgApiUser.username,
                    roles = mutableSetOf(
                        roleRepository.findByRoleName(UserRoles.GUEST) ?: throw IllegalStateException("no role found"),
                    ),
                    sessionKey = null,
                    session = null,
                )
            )
        }
        return User(
            id = user.id,
            telegramId = user.telegramId,
            isBot = user.isBot,
            firstName = user.firstName,
            lastName = user.lastName,
            username = user.username,
            roles = user.roles.map { it.roleName }.toSet(),
            sessionKey = user.sessionKey,
            session = user.session,
        )
    }

    @Transactional
    fun updateSession(id: Long, sessionKey: String?, session: String?) {
        userRepository.updateSession(id, sessionKey, session)
    }

    @Transactional
    fun addUserRoles(username: String, role: UserRoles) {
        val user = userRepository.findByUsername(username) ?: throw IllegalArgumentException("Пользователь не найден!")
        user.roles.add(roleRepository.findByRoleName(role) ?: throw IllegalArgumentException("Роль не найдена!"))
    }
}
