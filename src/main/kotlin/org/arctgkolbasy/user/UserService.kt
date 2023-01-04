package org.arctgkolbasy.user

import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull
import com.github.kotlintelegrambot.entities.User as TgApiUser
import org.arctgkolbasy.user.User as DbUser

@Service
class UserService(
    val userRepository: UserRepository,
    val roleRepository: RoleRepository,
) {
    @Transactional
    fun getOrCreateUser(tgApiUser: TgApiUser, chatId: Long): User {
        var user = userRepository.findByTelegramId(tgApiUser.id)
        if (user != null) {
            user.firstName = tgApiUser.firstName
            user.lastName = tgApiUser.lastName
            user.username = tgApiUser.username
            user.telegramChatId = chatId
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
                    telegramChatId = chatId,
                    lastMenuMessageId = null,
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
            telegramChatId = chatId,
            lastMenuMessageId = user.lastMenuMessageId,
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

    @Transactional
    fun deleteUserRoles(username: String, role: UserRoles) {
        val user = userRepository.findByUsername(username) ?: throw IllegalArgumentException("Пользователь не найден!")
        user.roles.remove(roleRepository.findByRoleName(role) ?: throw IllegalArgumentException("Роль не найдена!"))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Transactional
    fun updateLastMenu(user: User, messageId: Long) {
        val dbUser = userRepository.findById(user.id).getOrNull()
            ?: throw IllegalArgumentException("")
        dbUser.lastMenuMessageId = messageId
    }
}
