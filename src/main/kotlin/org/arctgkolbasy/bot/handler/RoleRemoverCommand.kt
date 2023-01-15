package org.arctgkolbasy.bot.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Update
import org.arctgkolbasy.bot.user.Session
import org.arctgkolbasy.bot.user.User
import org.arctgkolbasy.bot.user.UserRoles
import org.arctgkolbasy.bot.user.emptySession
import org.arctgkolbasy.user.UserRepository
import org.arctgkolbasy.user.UserService
import org.springframework.stereotype.Controller

@Controller
class RoleRemoverCommand(
    val userService: UserService,
    val userRepository: UserRepository,
    val objectMapper: ObjectMapper,
) : StateMachine() {
    override fun getCommandName(): String = REMOVE_USER_ROLE

    override fun checkUserAccess(user: User): Boolean = UserRoles.ADMIN in user.roles

    init {
        addSessionStep(RemoveRoleStates.STEP_1_CHOOSE_USER.step, ::stepOneChooseUser)
        addSessionStep(RemoveRoleStates.STEP_2_REMOVE_ROLE.step, ::stepTwoRemoveRole)
    }

    override fun stepZero(user: User, bot: Bot, update: Update): Session {
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = userService.getPageFromCursor().joinToString(
                prefix = "Выбери пользователя:\n",
                separator = "\n",
                transform = { u ->
                    "/${u.username.toString()} ${
                        u.roles.joinToString(
                            prefix = "[",
                            postfix = "]"
                        ) { role -> role.roleName.name }
                    }"
                }
            )
        )
        return Session(RemoveRoleStates.STEP_1_CHOOSE_USER.step)
    }

    private fun stepOneChooseUser(user: User, bot: Bot, update: Update): Session {
        val username = update.message().replaceFirst("/", "")
        val selectedUser = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("Не найден $username")
        bot.sendMessage(
            update.chatIdUnsafe(),
            selectedUser.roles.joinToString(
                prefix = "Текущие роли пользователя ${username}:\n",
                separator = "\n",
                transform = { "/${it.roleName.name}" }
            )
        )
        return Session(
            sessionKey = RemoveRoleStates.STEP_2_REMOVE_ROLE.step,
            session = objectMapper.writeValueAsString(AddRoleSession(username = username))
        )
    }

    private fun stepTwoRemoveRole(user: User, bot: Bot, update: Update): Session {
        val role = when (update.message().replaceFirst("/", "")) {
            UserRoles.ADMIN.name -> UserRoles.ADMIN
            UserRoles.USER.name -> UserRoles.USER
            UserRoles.GUEST.name -> UserRoles.GUEST
            else -> throw IllegalArgumentException("Недопустимая роль!")
        }
        val removeRoleSession = objectMapper.readValue<RoleSetterCommand.AddRoleSession>(user.session!!)
        userService.deleteUserRoles(removeRoleSession.username, role)
        bot.sendMessage(
            chatId = update.chatIdUnsafe(),
            text = "Роль $role пользователя ${removeRoleSession.username} удалена."
        )
        return emptySession
    }

    enum class RemoveRoleStates(
        val step: String,
    ) {
        STEP_1_CHOOSE_USER("${REMOVE_USER_ROLE}1"),
        STEP_2_REMOVE_ROLE("${REMOVE_USER_ROLE}2"),
    }

    companion object {
        const val REMOVE_USER_ROLE = "remove_user_role"
    }

    data class AddRoleSession(
        val username: String,
    )
}